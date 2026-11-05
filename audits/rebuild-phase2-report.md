# REBUILD PHASE 2 REPORT: the player animation layer

Date: Nov 4, 2026. Status: complete, committed locally in chunks,
pushed to origin/main (authorized by Carter). Compile check remains
waived by Carter's standing rule; verification is the dual javac
audit sweep against the Phase 1 baselines. The in-game look is
unverified until Carter's PC test, same as always.

## Audit first: what player animation infrastructure exists at 28f3aea

Everything below was re-derived from the current code, per the
clean slate rule.

- Player rendering is the vanilla AvatarRenderer and PlayerModel.
  PlayerEntityRendererMixin adds DBA render layers. A race either
  uses the cuboid model or a custom OBJ mesh (Custom3DModelLayer)
  whose limbs are posed from the same ModelPart transforms, so one
  pose system drives both body types.
- The animation engine is BedrockAnimationRegistry plus
  BedrockAnimationApplier (client/render/animation). It parses
  Blockbench and Bedrock .animation.json files (player, per-race,
  and forms files) into per-bone rotation and position keyframe
  tracks and samples them with binary search and linear
  interpolation. This is the same JSON keyframe family Better
  Combat builds on.
- BedrockAnimationApplier.apply is a priority cascade, invoked
  from Custom3DModelLayer.submit for every rendered player. In
  order: death, sleeping, the Azure rush pose, the form transform
  clip, emotes, crawl, swim, climb, fly, ride, airborne, crouch,
  block, item use, the vanilla-swing attack clip, locomotion,
  idle. The first state that matches owns the whole body at weight
  1 after the bind pose is restored.
- Form transformation sequences are the transform clip entry in
  that cascade: the applier watches each player's form id, and on
  a change plays the matching transform clip before anything below
  it. That is the sequence the priority rule protects.
- Cross-player pose state has a precedent: emotes are a string on
  the player (PlayerStatsAccessor, common code), broadcast to
  nearby clients and read by the applier. PlayerKiBroadcastPayload
  is the entity-id S2C precedent; PlayerLookup.tracking is the
  viewer-scoping precedent (MobPhysics).
- GeckoLib is in the mod (weapon item renderers, dragons, hostile
  entities, armor) but is not used for the player at all. There is
  no GeckoLib player rig.
- What did not exist: any client notion of a cast, any ability
  clip set, and any charge visual beyond the Phase 1 server motes
  in AbilityService.

Conclusion: no new library dependency is needed or added. Phased
ability animation is a time-mapping layer over the existing
sampler plus one sync signal, and that is what was built.

## The design

### Schema (ability/AbilityDef.java)

The reserved string field became an object, parsed and validated
like everything else: animation.id (required, must be a Bedrock
clip name: dotted lowercase tokens), animation.charge (optional,
same check), animation.strikeAt (0.05 to 0.95, default 0.5),
animation.followTicks (0 to 200, default 10), animation.channel
(default false). Unknown fields inside the block are rejected by
name. A malformed id or an out-of-range phase value throws naming
the exact field (animation.id, animation.strikeAt, and so on).
The old bare-string form now fails loudly as "field animation is
required and must be an object" instead of being silently carried.
Abilities without the block parse with a null spec and cast
exactly as they did in Phase 1. The record component is now
AnimationSpec; the only constructor caller was the parser itself.

### Sync (network/CastSyncPayload.java, AbilityService)

One payload, three actions, sent by AbilityService to the caster
and every player tracking them, only for abilities whose data
carries an animation block:

- START (player entity id, ability id, cast start game tick),
  broadcast where the cast registers in onPress.
- FIRE (fire game tick), broadcast at the end of fire(), the same
  tick the carrier spawns and the fire sound plays.
- CANCEL, broadcast at every existing transition that drops a
  cast without firing: a new press replacing a charge, a release
  after the weapon changed, the per-tick weapon check, and the
  definition-vanished path (that one ungated, because the def
  whose animation flag would gate it is precisely what is gone;
  clients tracking nothing ignore it).

Why each is necessary and why nothing more is sent: the wind-up
precedes any carrier entity, so observers cannot derive a start
from anything else server-visible. The fire tick of a
charge-release cast is the release moment, which observers
cannot predict; pinning the pose's strike frame to it requires
the tick itself. A charge can be held indefinitely, so a dropped
charge needs an explicit end or the pose would hold forever.
Durations, phases, and power are never sent: clients re-derive
them from the synced definition and the two ticks. Poses are
server-authoritative with no client prediction, the same latency
profile the carrier spawns already have.

Server behavior is otherwise untouched: the AbilityService diff
is additive broadcasts at existing transitions (the one modified
line captures the return of the existing CASTS.remove). No
damage, cost, cooldown, lockout, or carrier code changed, and no
timing boundary moved: fire() still runs on the same ticks.

### The layer (client/ability/CastAnimations.java, applier slot)

CastAnimations holds one entry per casting player and maps level
game time (the server clock, mirrored by the client level) onto
the ability's clip:

- Wind-up gear: with a charge clip, that clip plays across
  startup on a smoothstep, deepening as the cast's power curve
  grows. Without one, the release clip's own segment before its
  strike frame plays across startup the same way.
- Strike: the clip's strike frame (strikeAt of its length) is
  pinned to the broadcast fire tick.
- Second gear: the rest of the clip plays across followTicks
  plus recovery on a cubic ease-out, fast at the gear change and
  settling through the follow-through into recovery.
- Channel variant: the pose holds just past the strike frame,
  with a small sway, for the whole active phase, then settles
  across recovery.
- A FIRE with no matching START (an observer who came into range
  mid-cast) reconstructs the entry from the fire tick minus the
  definition's startup. A wind-up entry that outlives startup by
  3600 ticks without a fire or cancel is dropped as a safety net.
  A clip name that does not resolve warns once and yields no
  pose; presentation fails soft, gameplay never does.

Priority: BedrockAnimationApplier gained one slot, directly
below the form transform clip and above emotes. A cast pose
therefore overrides emotes, item use, the attack swing,
locomotion, and idle, and never overrides death, sleep, the
Azure rush pose, or a form transformation, all of which return
earlier in the cascade. A new cast replaces the old entry in the
store, so a cast interrupts recovery cleanly by construction.
The head uses the applier's default path (clip head motion added
onto vanilla look), so a caster keeps aim control while posing.

### The generic animation set

assets/dragonblockarcanedba/animations/abilities.animation.json,
loaded by BedrockAnimationRegistry with the other player files.
Six clips, all parameterized by the ability's phase durations,
none by weapon:

- charge_stance: the two-handed gather. Arms draw in and up to
  meet over the chest while the torso leans in and the legs
  brace; the depth is the whole clip, so a 6-tick startup and a
  60-tick charge play the same gather at different speeds. Its
  final frame is the gathered pose the release clips strike
  from.
- horizontal_release: torso twist winds the striking arm across
  the body, sweeps it through, settles.
- overhead_release: arms arch up and back, chop down, settle.
- thrust: arm cocks back with the torso twisted away, drives to
  full extension with a lunge, settles.
- channel_raise: both arms rise up and out to a full raise, hold
  with a sway, lower. The hold segment is what the channel flag
  rides.
- point_slam: the striking arm rises, slams down to point at the
  ground ahead, holds the point, releases.

Every release clip places its strike frame at exactly half its
length, which is the schema's default strikeAt, so data only
overrides strikeAt when a future clip deviates. These are
first-pass authored data; retuning them is a JSON edit, no code.

### Charge read (client/render/ability/ChargeRingRenderer.java)

While any player winds up, a ground ring at their feet is drawn
from the same cast store, so every viewer agrees: the radius
grows with the charge fraction (0.55 to 2.3 blocks, times the
definition's visual scale), the rim color forms the outer band,
and two dashed mid-color arcs inside counter-rotate at a rate
that follows the power curve, which is the vortex read. At full
charge a bright core ring pulses. Distance behavior comes from
the ability's own caps (outer ring only past fullDetailDistance,
nothing past cullDistance). The Phase 1 server motes stay as the
decay layer around the posed caster and the ring.

### Keyframe firing alignment

Nothing server-side was re-timed. The charge sound already plays
at press; the carrier spawn and fire sound already happen inside
fire(). The FIRE broadcast leaves in the same call carrying the
same game tick, and each client pins the clip's strike frame to
that tick. Pose, carrier, and sound therefore land on the same
tick by construction, for the caster and for observers alike.
Hit sounds remain the carriers' own, at hit time, as in Phase 1.

## Proof attacks, before and after

### Z Shockwave: charge read, PARTIAL becomes PARTIAL-STRONG

- Before: the 20-tick charge read as gold material motes
  gathering at the caster while the body played locomotion or
  idle, and the release had no body motion at all.
- After: charge_stance winds across the 20 ticks, deepening on
  the power curve; the ground ring grows under the caster with
  counter-rotating arcs that spin faster as power rises, and the
  white core ring pulses at full charge. On release,
  horizontal_release's strike frame lands on the fire tick, the
  arm sweep follows through across follow 10 plus recovery 6 on
  the ease-out snap, and the AbilityService motes still shed
  around the caster as decay.
- Against the picture (z-sword.png): the card's step 1 is a
  golden vortex coiling flat around the player like a Saturn
  ring. The build now has the posed gather plus a ground vortex
  of light in the same colors at growing scale. What it does not
  have is the raised, torn-edged ring at waist height: that is a
  primitive (a flat torn ring mesh) and belongs to Phase 4, as
  scoped. The charge read is honest progress toward the card,
  not the card.

### Cursed Tempest: cast raise, new

- Before: the 10-tick startup was invisible; the cloud simply
  appeared, and the caster's body never acknowledged the cast.
- After: channel_raise lifts both arms across the 10-tick
  startup, the strike frame lands on the fire tick together with
  the thunder crack and the cloud spawn, the arms hold through
  the 10 follow ticks, then lower across the 10 recovery ticks.
- Deliberate non-behavior: the Tempest's active is 300 ticks and
  the pose does not ride it (channel flag off). The storm is the
  carrier's; the raise is the cast's. A genuinely channeled
  ability (the Dabura eclipse in the design record) sets channel
  true in its data when Phase 6 authors it, and the same clip
  holds for its whole channel with no code change.

## Verification

- Dual sweeps at the finished tree: 120 kinds on 26.2, 126 on
  26.3; comm -13 against the saved baselines
  (kinds-rebuild-base262.txt, kinds-rebuild-base263.txt) prints
  NOTHING on both classpaths. The new code adds zero new error
  kinds.
- Schema harness (the real AbilityDef parser, run standalone):
  14 of 14 checks pass. Both proof definitions parse with their
  animation blocks and expected defaults; a definition without
  the block parses with a null spec; a bad animation id, a bad
  charge name, strikeAt 1.5, followTicks -1, an unknown field
  inside the block, and the retired string form are each rejected
  with an error naming the exact field.
- All 570 resource JSONs parse (569 from Phase 1 plus the new
  abilities.animation.json).
- git diff --check clean. Byte-level em dash and en dash scan
  over all 13 changed and new files: zero.
- Split source sets hold: CastSyncPayload and the service hooks
  are common code with no client references; the store, sampler,
  ring renderer, and applier slot are client-only.

## What Phase 3 inherits

- The tick discipline: server game time, broadcast at the
  transition, reconstructed locally. The seeded effect packets
  should ride the same clock and the same transitions (start,
  fire, hit); CastAnimations' entry store is the shape to mirror
  for per-player effect state.
- The strikeAt convention: strike frames live at a declared
  fraction of a clip, defaulting to one half. Particle recipes
  keyed to the strike can assume the same fraction.
- The caps-in-data distance model, proven by the ring renderer,
  is the culling contract the universal particle class should
  consume per effect.
- The charge store's power curve (chargeInfo) is the exact input
  charge-time particle recipes need, already computed once for
  poses and rings.
- Known limits Phase 3 does not have to solve but should know:
  poses are third-person and observer visuals (first person
  shows the ring and hears the sounds; the local body is not
  rendered, the same limit the whole Bedrock system has); there
  is no root motion and no swing trail capture yet (later
  research steps, unchanged); the six clips are first-pass data
  and will be retuned against Carter's PC test without code.
