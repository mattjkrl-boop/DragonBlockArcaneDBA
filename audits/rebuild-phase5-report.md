# Rebuild Phase 5 report: the feedback service

Phase 5 of the from-scratch rebuild: one feedback service for
abilities. Data-authored screen shake in the Iron's Spells
pattern (server-synced, distance falloff, arbitration), domain
fog for clouds in the Cataclysm pattern (client-side from synced
carrier state), and a deliberate decision on hit-stop. Structure
per audits/REBUILD-ARCHITECTURE.md, build order from the visual
research report Section 4. Derived fresh from the code at HEAD
5a16292 per the clean slate rule.

## The audit: what already existed at HEAD

Screen shake already existed, in a pre-rebuild shape:

- network/ScreenShakePayload: (intensity, durationTicks,
  hitstopTicks), registered in DbaNetwork, received in
  DragonBlockArcaneDBAClient, which forwards to
  client/fx/ScreenShakeHandler.trigger and DbaPostFx.onShake.
- Exactly two senders, both direct per-player: movement landings
  (MovementPhysics.onLand, duration 7, hitstop 0) and the
  MobPhysics finisher (duration 6, hitstop 3, killer only). No
  origin, no radius, no falloff anywhere: a shake reached one
  named player at full strength.
- ScreenShakeHandler 2.0: a SINGLE active shake; a newcomer
  replaced it only if stronger than the incumbent's decayed
  amplitude, otherwise it was dropped (two live sources could
  not coexist, so a per-strike shake under a fire shake simply
  vanished). Decay was trauma squared: amplitude scales with
  (remaining/duration)^2. Presentation: layered sine sway plus
  a directional kick on spawn, sampled-before-aging so the
  first tick is full strength; a hitstop hold that freezes the
  camera at a fixed offset while shakes do not age; a global
  scale from DbaEffectsConfig.screenShake (0 to 4). Applied by
  client/mixin/CameraShakeMixin at Camera.alignWithEntity TAIL.
  The stagger and tumble systems turned out to be entity
  render-state systems (mob stagger states, a tumble pose
  renderer), not camera hooks; the established camera pattern
  is this handler plus its mixin, and Phase 5 kept it.

Fog: nothing at HEAD touched render fog. A bespoke-era
AtmosphereServer / AtmosphereClient / AtmospherePayload trio
survives, orphaned by the strip (AtmosphereServer.begin has no
callers): a payload-synced post-process gloom grade plus rain,
not FogRenderer fog. Not used: domain fog needed no server
state beyond what carriers already sync. The vanilla seam is
FogRenderer.setupFog, whose signature and FogData shape (six
public distance fields plus a color vector) are byte-identical
in the 26.2 and 26.3 jars (verified with javap against both),
so one mixin method serves both versions, which the codebase's
compat gating supports (DbaMixinPlugin gates only the named
version-specific entries; everything else applies on both).

Carrier clocks, for the hit-stop audit: renderers sample
EntityRenderState.ageInTicks (entity tickCount plus partial
tick, assigned by vanilla's EntityRenderDispatcher). Carrier
owner identity is a server-only field by deliberate Phase 1
design; the synced contract is ability id, seed, power, and
nothing else. CastAnimations samples poses as a pure function
of (startTick, fireTick, gameTime plus partial).

## What landed

Schema (ability/AbilityDef.java):

- A top-level "feedback" block: optional sub-objects onFire,
  onCarrierHit, onCarrierEnd. Each is a shake: strength
  (required, above 0, capped at MAX_SHAKE_STRENGTH 3.0),
  durationTicks (default 10, 1 to MAX_SHAKE_DURATION 40),
  radius (default 16, 1 to MAX_SHAKE_RADIUS 64). Unknown
  events and unknown fields fail the load naming the exact
  field (feedback.onCarrierHit.radius style), matching the
  Phase 3 fx parser's conventions. Caps are feel-safety: no
  authored shake can exceed 3 degrees or 2 seconds.
- The cloud block gains an optional "atmosphere" object:
  tint (hex color, required), strength 0 to 1 (default 0.6),
  density 0 to 1 (default 0.5). Errors name
  cloud.atmosphere.tint and friends.

Shake service:

- ability/FeedbackMath.falloff(distanceSq, radius): 1 at or
  below the origin, 0 at or beyond the radius, quadratic
  between (1 - d^2/r^2). Pure, no state; radius 0 means 1
  (direct). Harness-proven: endpoints, monotonic, strictly
  interior, deterministic.
- ability/AbilityFeedbackService: fire() from AbilityService
  beside the FIRE fx send; carrier() from the carriers beside
  the CARRIER_HIT and CARRIER_END fx sends (wave aggregated
  hit tick, cloud strike at the strike point, projectile
  entity and block hits, base and projectile expiry ends).
  One ScreenShakePayload per event per player inside the
  radius (PlayerLookup.around), NOT gated on fx recipes: a
  definition may author feedback with no fx and still shake.
- ScreenShakePayload extended with (ox, oy, oz, radius);
  codec extended in matching order. The two legacy senders
  pass the receiver's own position and radius 0 and keep their
  exact old behavior (server decides one player, full
  strength, hitstop hold intact). Reuse over replace: one
  payload, one client store, one arbitration.
- ScreenShakeHandler 3.0: a store of up to 8 shakes, each with
  its own origin, radius, duration, elapsed, kick, and phase
  seeds. Each client tick samples every shake's effective
  amplitude (intensity x falloff from the viewer's own
  position x squared decay of remaining life) and the
  strongest drives the unchanged 2.0 sway, kick included.
  Sampling still happens before aging, preserving the exact
  full-strength first tick the old handler produced for the
  legacy sends. The hitstop hold keeps its semantics (camera
  pinned at the holding shake's effective offset, shakes do
  not age, then resume), and a public viewerPos supplier seam
  exists for the harness (defaults to the local player).

Fog (client/fx/DomainFog + client/mixin/FogRendererMixin):

- DomainFog re-evaluates on the client tick: every rendered
  CloudCarrierEntity whose definition authors atmosphere
  contributes influence = lifecycle envelope (FxEnvelope over
  the carrier's tickCount: grows in, eases out across the end)
  times a spatial falloff (1 inside the CURRENT radiusAt
  radius, to 0 at 1.6 radii).
- Winner rule, documented here and in the architecture doc:
  the highest influence-times-strength score wins outright.
  One domain's tint, strength, and density apply whole; colors
  never blend between domains. A smoothed weight moves at
  0.05 per tick in and 0.02 out, so domains breathe rather
  than pop.
- The mixin post-processes FogRenderer.setupFog's returned
  FogData: all six distance fields scale by
  max(0.12, 1 - weight * density * 0.85); the fog color's rgb
  lerps toward the tint by weight * strength (alpha untouched).
  Disconnect resets the weight. No new packets: everything
  derives from the synced carrier state and the synced
  definitions. DomainFog.init() registers beside
  ClientAbilityFx.init().

## Hit-stop: designed, deferred (not built)

The bundled bar was strict: client-local, caster-only, opt-in,
no server timing change, no effect on other players, covering
BOTH the cast animation clock and the carrier presentation
clock. Half of that is clean in this codebase and half is not.

Clean half: the cast pose. CastAnimations is a pure function
of server ticks; a per-local-player lag could freeze the
follow-through segment for a few ticks on a confirmed hit.

Not clean: the carrier clock. (1) A renderer cannot tell "the
local player's carrier" from any other: owner identity is not
synced, deliberately (Phase 1 minimal contract), and matching
by spawn time or position is a heuristic that mis-binds under
multiplayer and lag. (2) Carrier presentation age is the
entity's shared client tickCount, the same clock vanilla
interpolation and the render state read; it is not a
per-viewer value that can be scaled for one observer's copy of
one entity without rewriting renderer age reads per entity,
and freezing age while position interpolation continues tears
the visual. (3) Worst, slowing or freezing a carrier's visuals
while the server keeps applying damage from its real position
and live radius breaks the wave and cloud contract the
rebuild set in Phase 1: what you see is what can hit you.

The research context agrees with restraint: none of Better
Combat, Epic Fight, or SlashBlade ships hit-stop at all, and
the one shake system the field does well (Iron's) treats
feedback as synced data, not clock games. The existing
hitstopTicks camera hold (MobPhysics finishers) is a different
mechanism and was left exactly as is, not relabelled.

The designed shape, recorded for Phase 6 or later: an optional
"hitStopTicks" int (cap 4) in the feedback block; on a
carrier_hit, the server sends the CASTER only a direct packet;
the client freezes the local CastAnimations sample clock for
that many ticks (the post-strike segment stretches, the entry
ends late by the same count, no other player's screen changes).
The carrier half becomes buildable only if the project accepts
one of: a synced owner field on carriers (a contract change
needing its own review), or the pose-only freeze as an
accepted compromise. Neither was forced in this phase.

## Proof values (data only)

- Z Shockwave: onFire strength 0.9, durationTicks 12, radius
  20; onCarrierHit strength 1.5, durationTicks 14, radius 16.
  You fire a ring the size of a house: a firm kick on fire,
  a heavier thud where it lands hits.
- Cursed Tempest: onCarrierHit 0.45 / 8 / 14. Strikes land
  every 16 ticks at full tilt, so each is a nudge, well under
  the shockwave's fire shake; overlapping strikes refresh the
  store entry rather than stacking to sickness (the store
  takes the strongest, it does not add).
- Cursed Tempest atmosphere: tint #3B1163 (deep violet),
  strength 0.65, density 0.5. The domain darkens and thickens
  the air while the storm lives, easing with its envelope.

## Gates

- Schema and behavior harness (targeted-source-closure
  compile and run, /tmp/FeedbackHarness.java): 45 checks, 0
  failures. Covers the feedback accept and reject matrix with
  exact field names (unknown event, unknown shake field,
  missing strength, strength above cap, duration above cap,
  radius above cap), authored proof values and defaults, the
  atmosphere matrix (missing tint, bad hex, strength and
  density bounds, defaults), FeedbackMath properties
  (endpoints, direct mode, monotonicity, interior bounds,
  determinism), and handler behavior (an out-of-radius shake
  never moves the camera, a direct shake moves it and settles
  to zero on the old schedule, the hitstop hold freezes the
  offset across ticks then releases).
- Dual javac sweeps against the rebuild baselines: 124 kinds
  on 26.2 (baseline 120) and 130 on 26.3 (baseline 126).
  Stated plainly: the literal comm -13 gate is NOT empty, and
  the reason is mechanical, not code. All 4 added kinds on
  each classpath are in the one new mixin file
  (client/mixin/FogRendererMixin.java): "cannot find symbol"
  plus the three org.spongepowered package errors. Every one
  of the 14 pre-existing mixin files contributes the identical
  4 kinds to the baseline, because the sweep classpath carries
  no Mixin jars (see the Phase 3 report); they are environment
  artifacts every mixin in this codebase produces. The
  message-level comparison (file prefixes stripped) is EMPTY
  on both classpaths: Phase 5 introduced zero new error
  messages, and every non-mixin file added in this phase
  (schema, service, handler, fog, carriers, payload) added
  zero kinds. The alternative (folding the fog hook into
  CameraShakeMixin as a second @Mixin target) was rejected:
  that class @Shadows Camera members that do not exist on
  FogRenderer, and a mixin misapplication fails at game
  startup, invisible to these sweeps; a dedicated mixin class
  matching the codebase's one-class-per-target pattern is the
  safer engineering call. The literal result and this
  reasoning are recorded here rather than silently redefining
  the gate.
- All 571 resource JSONs parse; the two edited ability files
  round-tripped byte-stable in the files' established compact
  format.
- Behavior audit over the full diff: server changes are the
  payload codec extension, the two legacy senders passing
  origin and radius 0, and 7 added feedback send calls sitting
  beside the existing fx sends on the same ticks. No damage,
  cost, cooldown, carrier movement, targeting, or hit timing
  lines changed. Client changes are the handler store, the
  fog pair, one receiver call, one init call, one mixins.json
  line.
- git diff --check clean. Em dash and en dash scan over all
  19 changed and new files: zero hits (the two legacy hits in
  untouched DbaHudOverlay.java remain, out of scope as
  before).

## What Phase 6 inherits

- The feedback block and atmosphere are available to all 22
  attacks as data: fire and hit shakes per weapon, domain fog
  for any cloud (Valor Field's gold dome wants a warm tint;
  the gravity wells want dark ones), with the caps keeping
  authoring honest. Hit-stop stays deferred on the terms
  above; if a weapon's picture demands it (an execution
  finisher is the candidate), it is the pose-only compromise
  or a contract change, decided weapon by weapon with Carter.
- The Phase 4 open calls carry forward unchanged: wave
  knockback plus a ground decal path for rings (CrackDecal
  waits), and cloud lift and drag movement control for the
  carriers themselves.
- Carrier primitives the roster will force, now with their
  feedback story already settled: the beam carrier (Spirit
  Cannon, Erasure Cannon) needs a fire shake at the muzzle
  event and per-tick-held presentation; ground chains and
  walls (TornWall, ChainRun consumers) want carrier_hit shakes
  at each link or segment rather than one blob origin;
  CrackDecal ground scars pair naturally with onCarrierEnd
  shakes for slam landings; Devil Trident's swarm wants tiny
  per-shard hits and a big deploy and recall pair; Valor
  Field's projectile stasis is still the one mechanic with no
  carrier support at all.
