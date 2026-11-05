# DBA Rebuild Architecture (Phase 1)

Written Nov 4, 2026, as step 1 of the rebuild Carter approved from
audits/VISUAL-RESEARCH-REPORT.md (Section 4 and its build order). This
document concretizes that research for this codebase. The one-sentence
version: abilities are data, carriers are code, and no weapon will ever
own Java again.

## The core rule

Every weapon attack is one JSON file. The file names the weapon item,
the click slot, a generic carrier, phase timings, damage, cost,
cooldown, and visual parameters. A small set of generic carrier
entities (this phase: Wave, Cloud, Projectile) interprets the data at
runtime. Weapon item classes stay the plain items the strip left
behind; they gain behavior purely because the ability registry binds
ability ids to their item id. Adding attack 23 means writing JSON,
not code. A genuinely new mechanic means adding one carrier behavior,
once, shared by every weapon that uses it.

## Where things live

- Ability data: src/main/resources/data/dragonblockarcanedba/abilities/
  One file per attack. File name becomes the ability id:
  abilities/z_shockwave.json is dragonblockarcanedba:z_shockwave.
- Schema and parser: ability/AbilityDef.java (one record, parsed and
  validated in one place, used by the server loader and by the client
  sync receiver, so both sides read identical definitions).
- Registry: ability/AbilityRegistry.java (ability id to definition;
  item id plus click slot to ability id).
- Loader: ability/AbilityLoader.java, a datapack reload listener in
  the RaceLoader pattern (FileToIdConverter.json("abilities")),
  registered next to the race and form loaders in the main class.
- Execution: ability/AbilityService.java (server): cast state,
  charges, cooldowns, Ki checks, carrier spawning, sound firing.
- Damage: ability/AbilityDamage.java: the one place ability damage is
  computed and applied, through util/DbaDamage types and
  DbaEffectsConfig.capDamage.
- Carriers: entity/WaveCarrierEntity.java,
  entity/CloudCarrierEntity.java, entity/ProjectileCarrierEntity.java
  on a shared entity/CarrierEntity.java base; registered in
  DbaEntities as ability_wave, ability_cloud, ability_projectile.
- Renderers: client/render/ability/ (one generic renderer per
  carrier, parameterized entirely from the definition the client
  registry holds; see the sync note under Input routing).
- Input: network/C2SWeaponClickPayload.java (client to server) and
  network/AbilitySyncPayload.java (server to client definitions and
  bindings), wired in DbaNetwork; the client side lives in
  client/ability/ (sync receiver, input handler).

## The ability schema

Fields, with the validation the loader applies. Anything invalid is a
loud log error naming the file and the field, and that one ability is
skipped; the rest of the set still loads. Unknown top-level fields are
rejected, so typos fail loudly instead of silently doing nothing.

- weapon (string, required): the item id this attack belongs to,
  e.g. "dragonblockarcanedba:z_sword". Must resolve to a registered
  item at load time.
- slot (string, required): "left" or "right". One ability per weapon
  per slot; a duplicate binding is an error and the later file loses.
- name (string, required): display name, used in logs and later UI.
- carrier (string, required): "wave", "cloud", or "projectile" this
  phase. Later carriers (beam, ground chain) join the same enum.
- phases (object, required): startup, active, recovery in ticks, each
  0 to 72000. startup is the cast or charge time before the carrier
  fires. active is the carrier's lifetime. recovery is the post-fire
  lockout before the same player can start any other ability. These
  three numbers are the contract the Phase 2 animation layer will
  read; see the hooks section below.
- animation (object, optional; shape defined by Phase 2): the cast
  animation for this attack. id (required) names the release clip;
  charge (optional) names a separate wind-up clip played across
  startup instead of the release clip's own wind-up segment;
  strikeAt (0.05 to 0.95, default 0.5) is the fraction of the
  release clip where the strike frame sits, and that frame lands
  exactly on the fire tick; followTicks (0 to 200, default 10)
  caps the follow-through after the strike (the carrier's active
  lifetime is not a body pose); channel (default false) holds the
  pose just past the strike frame for the whole active phase
  instead. Clip ids are Bedrock animation names (dotted lowercase
  tokens); a malformed id or an out-of-range value is rejected at
  parse time naming the field. The generic clip set lives in
  assets/dragonblockarcanedba/animations/abilities.animation.json
  (animation.ability.*). Abilities without the block cast exactly
  as before, with no pose. See the Phase 2 section below.
- chargeRelease (boolean, default false): when true, the attack
  charges while the button is held and fires on release, with power
  scaled by held fraction (see minPower). When false, pressing starts
  the cast and the carrier fires automatically when startup elapses.
- minPower (number 0 to 1, default 0.4): the power floor for a
  chargeRelease attack released instantly. Power scales damage and
  visual scale, never duration.
- kiCost (number >= 0, default 0) and kiCostPercent (number 0 to 100,
  default 0): the Ki price is kiCost plus kiCostPercent percent of
  the caster's max Ki, using the existing Ki store on
  PlayerStatsAccessor (dba$addKi and friends). Percent exists
  because several saved designs price attacks in percent (the Devil
  Trident's drain, percent-of-current costs).
- cooldownTicks (int >= 0, default 0): per-ability cooldown after
  firing, on top of the recovery lockout.
- damage (object, required): base (number >= 0), stat (one of
  strength, dexterity, spirit, willpower, defense, none), perStat
  (number, default 0), type (a DbaDamage family: ki, void, curse,
  darkness, hellfire, tempest, spirit, temporal, gravity, lava,
  valor, crush), falloff (none or linear), falloffFloor (0 to 1,
  default 0.4, the rim fraction from the Devil Trident design).
  Amount = (base + stat value * perStat) * power * falloff factor,
  then DbaEffectsConfig.capDamage, applied with hurtServer. Stat
  values come from PlayerStatsAccessor, so ability damage scales
  with the same progression as everything else in the mod.
- sounds (object, optional): charge, fire, hit; each a sound event
  id string resolved against the sound registry at fire time (the
  synthesized bank registers there, so data names sounds like
  "dragonblockarcanedba:charge_rise"). Missing or unknown ids are
  silent, never an error.
- visual (object, required): core, mid, rim colors as hex strings
  ("#RRGGBB" or "#AARRGGBB"); scale (number > 0, default 1);
  particleMaterial (a DbaMaterialOptions material name: plasma,
  smoke, vapor, dust, mist, soul, darkness, glass_dust) used for the
  decay layer; particleRate (number >= 0, default 1) scaling decay
  emission. The color language is the pictures' language: bright
  core, colored mid, dark rim.
- caps (object, optional): maxParticlesPerSecond (int, default 400)
  bounding the renderer's decay emitters for this effect;
  fullDetailDistance (default 48) and cullDistance (default 128):
  beyond full detail the decay layer thins by the shared FxEnvelope
  distance scale; beyond cull the decay layer stops. Hero geometry
  is never culled by these; it is the silhouette and it is the last
  thing that may disappear (it disappears only when the entity
  itself leaves tracking range).
- Carrier block (exactly one, matching carrier):
  - wave: speed (blocks per tick, > 0), radius (> 0, the band's
    half-width and damage reach), height (> 0), arcDegrees (10 to
    360, the band's sweep around the travel direction; 360 is a full
    ring shockwave).
  - cloud: radiusStart, radiusEnd (> 0; the domain grows from one to
    the other across active), height (> 0), anchor ("caster" or
    "target"; target means the block the caster is looking at within
    40 blocks, falling back to the caster), strikeInterval (ticks
    between lightning strikes, 0 disables strikes), telegraphTicks
    (warning time before each strike, < strikeInterval),
    strikeRadius (> 0, damage radius of one strike).
  - projectile: speed (> 0), gravityScale (default 1; 0 flies
    straight), aoeRadius (>= 0; 0 is a direct hit only), casing
    ("lit", "energy", or "none": a world-lit physical casing, an
    emissive energy body, or an invisible carrier with only a core),
    lengthScale (> 0, default 1).

## The carriers and their synced state

Synced entity data is deliberately minimal and identical in shape
across carriers: ABILITY_ID (string), SEED (int), POWER (float).
Everything else a renderer needs comes from the definition, which
the client holds through the sync payload, so a carrier never grows
per-ability synced fields. The one exception proves the rule: the
Cloud additionally syncs its next strike (STRIKE_POS as three
floats, STRIKE_TICK as an int game time, STRIKE_SEED as an int),
because a telegraph is gameplay information every observer must see
identically, and a single pending strike keeps that to five fields.
Cloud strikes are scheduled strictly sequentially for exactly this
reason.

Carrier behavior, server side:

- Wave: spawns at the caster's feet facing the caster's yaw, ground
  anchored (its Y snaps to the motion-blocking heightmap as it
  travels), advances at wave.speed for phases.active ticks. It
  damages each living entity once, when the entity first comes
  within wave.radius of the wave's center, with linear falloff from
  the center out to the radius. The owner is exempt from their own
  carriers' damage. Team and ally rules are a later refinement,
  noted in the phase report.
- Cloud: stationary at its anchor for phases.active ticks. Radius
  interpolates radiusStart to radiusEnd linearly over its life; the
  renderer and the strike picker read the same formula, so what you
  see is what can hit you. Strikes: at each strikeInterval, pick a
  victim (a random living entity inside the radius, never the
  owner) or, if none, a random point in the radius; telegraph for
  telegraphTicks via the synced strike fields; on the strike tick,
  damage everything within strikeRadius of the point with the
  ability's damage and linear falloff.
- Projectile: a ThrowableProjectile in the KiBlastEntity pattern.
  Direct hit applies the ability damage; if aoeRadius > 0, the hit
  (entity or block) also applies it in the radius with falloff, and
  the carrier dies on any hit. Max flight is phases.active ticks.

All three carriers refuse to save to disk (transient effects), carry
their ability id in NBT if the game saves them anyway, and resolve
their definition from the registry by id on both spawn and load, so
a datapack reload that changes numbers applies to the next cast
without any code knowing.

## Input routing

One payload for both buttons: C2SWeaponClickPayload(button, action,
heldTicks), button 0 left / 1 right, action 0 press / 1 release.
The client input handler (client/ability/WeaponInputHandler.java)
watches the attack and use keys while the main hand holds an item
the synced bindings say has that slot bound; it sends press on the
rising edge and release with the locally counted held ticks on the
falling edge, and stays silent in screens and menus. The server
never trusts the payload beyond the button: it re-derives the
ability from the item actually in the player's main hand through
AbilityRegistry, so a forged or stale payload can only ever fire
what the held weapon legitimately has.

Left click still performs the vanilla melee swing as well; that is
correct by the mod's own damage doctrine (the plain melee hit is
the game's weapon hit; abilities layer on top). The old build's
deleted input state machine is not mourned: two keys, two edges,
one payload.

Definitions reach clients through AbilitySyncPayload, sent on
datapack sync (join and /reload) by the server: each definition's
raw JSON, re-parsed on the client by the same AbilityDef parser
into the same AbilityRegistry (one registry, two fill paths: the
datapack loader on the server, the sync payload on the client;
bindings are derived from the definitions, so they travel for
free). Renderers and the input handler read only the registry.
No definition, no visuals and no payloads: fail closed, never
guess.

## Casting, costs, and cooldowns

AbilityService holds two server maps: one active cast per player,
and per-player per-ability cooldown end times, both keyed by UUID
against level game time.

- Press with startup 0: fire immediately.
- Press with startup > 0 and chargeRelease false: the cast is
  registered; AbilityService's server tick fires it when startup
  elapses. The caster's charge is visible this phase through sparse
  server-sent material particles in the ability's mid color (the
  honest Phase 1 stand-in until the animation layer owns charge
  poses).
- Press with chargeRelease true: the charge registers; release
  fires with power = clamp(heldTicks / startup, minPower, 1). A
  charge cancels silently if the weapon leaves the main hand, and
  starting any other ability cancels the current charge.
- Ki is checked at press (no Ki, no cast, a fizzle sound) and
  deducted at fire. Cooldown (recovery lockout for all abilities,
  plus this ability's cooldownTicks) starts at fire. If the
  definition vanishes in a reload mid-cast, the cast is dropped.

## Hard rules (from the research, binding on every later phase)

1. No per-weapon Java for ability behavior. If a design seems to
   need it, the answer is a carrier parameter or a new shared
   carrier, never a weapon class.
2. Geometry is the hero; particles are decay only. A carrier's
   silhouette is authored geometry (this phase: the wave band, the
   cloud ring and dome, the projectile casing and core). Particles
   shed, trail, and dissipate; they never form the body.
3. Physical matter is world-lit, always. The projectile's lit
   casing draws through the lit path with the entity's real light
   sample. Only energy materials (cores, bands, bolts) may be
   emissive.
4. Caps and distance behavior live in the data (caps block above),
   not in renderer folklore.
5. Effects fire at phase boundaries. This phase the boundaries are
   press, fire (startup end or release), strike, hit, and expire.
   Phase 2 hangs animation keyframes on exactly these.
6. One damage path (AbilityDamage), one cost path (the Ki store),
   one cooldown store (AbilityService). Nothing per weapon.
7. The server decides; clients reconstruct. Synced state stays at
   the minimum above; everything visual is derived from the
   definition plus seed, identically for caster and observers.

## Phase 2 hooks this phase deliberately leaves

- phases.startup/active/recovery are the animation phase contract
  (anticipation, active, recovery in the research's terms). The
  animation layer will stretch clips to these numbers and fire its
  keyframe events at the same boundaries AbilityService fires
  carriers at today; the executor already centralizes those
  moments, so Phase 2 changes where the fire call is triggered
  from, not what firing does.
- The reserved animation field names the clip per attack in data.
- The charge state in AbilityService (who is charging, since when,
  at what power) is exactly the state a charge pose and a two-gear
  swing need; it is exposed through the service, not per weapon.
- ScreenShakePayload already exists in the network package; the
  feedback service (research step 5) will hang off the same fire
  and hit events, with shake parameters joining the schema then,
  not now.
- The primitive kit (research step 4) will replace this phase's
  honest simple carrier geometry (band, dome, casing) behind the
  same renderer classes; the schema's visual block is where kit
  parameters will land.

## Phase 2: the animation layer (landed)

The layer is built on the mod's existing Bedrock animation engine
(BedrockAnimationRegistry and BedrockAnimationApplier, the same
JSON keyframe system that runs locomotion, emotes, and form
transform clips), not on a new library. How it fits:

- Priority: BedrockAnimationApplier gained one slot, directly
  below the form transform clip and above emotes. A cast pose
  overrides emotes, swings, item use, locomotion, and idle; it
  never overrides death, sleep, the Azure rush pose, or a form
  transformation, all of which return earlier in the cascade.
- Sync: AbilityService broadcasts CastSyncPayload (entity id,
  ability id, action, game tick) to the caster and every tracking
  player at the three cast transitions: start (a cast registers),
  fire (in fire(), the same tick the carrier spawns and the fire
  sound plays), and cancel (charge replaced, weapon swapped, or
  definition removed). Only abilities with an animation block
  produce traffic. Poses are server-authoritative; there is no
  client prediction, matching how carriers already appear.
- Sampling: client/ability/CastAnimations.java holds one entry per
  casting player and maps game time onto the clip: smoothstep
  across startup for the wind-up gear (a charge clip when the
  data names one, else the release clip's pre-strike segment),
  then a cubic ease-out across followTicks plus recovery for the
  strike snap and settle, the second gear. Channel animations
  hold just past the strike frame across active instead. A fire
  signal with no matching start (observer arrived mid-cast) is
  reconstructed from the definition's startup.
- Keyframe firing alignment: nothing new fires server-side. The
  charge sound already plays at press, the fire sound and carrier
  spawn already happen in fire(); the pose's strike frame is
  pinned to the broadcast fire tick, so pose, carrier, and sound
  share one tick by construction.
- Charge read: client/render/ability/ChargeRingRenderer.java
  draws a ground ring for every winding-up player from the same
  cast store: radius grows with charge fraction, brightness and
  the counter-rotating dashed arcs follow the cast's power curve,
  colors come from the definition's visual block, distance
  behavior from its caps block. The server-sent charge motes in
  AbilityService remain as the decay layer around it.
- Generic set: charge_stance (two-handed gather), overhead_release,
  horizontal_release, thrust, channel_raise, point_slam. Every
  release clip places its strike frame at half its length, the
  schema's default strikeAt; durations come from each ability's
  phases, never from the clip, so one clip serves a 6-tick cast
  and a 60-tick cast alike.

## Phase 3: the universal particle system (landed)

One registered particle type carries every ability effect; the
server never streams particles; clients expand small seeded
packets into the full effect locally.

- Universal class: particle type
  dragonblockarcanedba:ability_fx (DbaParticles.ABILITY_FX), a
  complex type whose options record (particle/AbilityFxOptions)
  carries entry index, ARGB color, size start and end, lifetime,
  gravity, drag, and a seed. The client particle
  (client/particle/AbilityFxParticle) is a SingleQuadParticle that
  reads everything from the options: lit entries (shard chip, ash
  flake, smoke puff) shade with world light on the translucent
  layer and collide with the ground; additive entries (spark
  streak, wisp curl, ember fragment, soft mote) render full bright
  on the no-depth-write additive layer. Spin comes from the seed,
  never from shared random state, so it is behavior-neutral
  between clients.
- Entry table: particle/AbilityFxEntries is the one place entry
  names meet sprites: 7 entries of 8 flipbook frames each (56
  slots in assets particles/ability_fx.json), reusing the sprite
  families the material set established (generic geometry chips,
  spark streaks, energy shards, ki motes, smoke). Data names
  entries; ids are validated at parse time.
- Recipes in data: the schema gained an "fx" list. Each recipe is
  keyed to one event (cast_start, fire with "strike" as an
  accepted alias, carrier_tick, carrier_hit, carrier_end,
  charge_tick) with an entry, a shape (point, ring, disc, cone,
  sphere_shell, band), a count (burst events) or rate per second
  (continuous events), speed, spread, cone angle, an optional
  radius override, size and lifetime ranges with an end-size
  scale, gravity, drag, an alpha override, a color reference
  (core, mid, rim, or a literal hex), a hard per-burst cap, and a
  cap group. The parser rejects unknown events, entries, shapes,
  fields, and out-of-range values by name, like every other block.
- Caps: every recipe respects the ability's caps block twice
  over. Rates and burst counts scale by a deterministic distance
  factor (1 inside fullDetailDistance, falling linearly to 0.35 at
  cullDistance, 0 beyond), the effects density setting, and the
  definition's particleRate; and a shared client budget
  (client/fx/FxBudget) enforces maxParticlesPerSecond per ability
  and per cap group across every route, so a recipe can ask for
  more than the cap but never emit more.
- Seeded packets: network/AbilityFxPayload carries ability id,
  event, origin, direction, seed, power, and a repeat count
  (nothing else; a burst is one small packet). ability/
  AbilityFxService sends it server-side only when the definition
  actually has recipes for the event, to the anchor's tracking
  players plus the anchor player. Clients resolve the definition
  from the synced registry and expand through client/fx/
  AbilityFxExpander: a splitmix stream keyed on the packet seed,
  per-particle seeds offset by a fixed stride, a fixed draw order
  per shape, and positions and velocities derived only from the
  seed, the definition, and the packet. Same seed plus same
  definition plus same event gives the same emission parameter
  list on every client; proven by harness (two independent
  expansions of the Cursed Tempest strike burst are record-equal;
  a different seed differs).
- Routes, and why: burst events (cast_start, fire, carrier_hit,
  carrier_end) travel by packet because they are one-shot moments
  whose exact seed matters for cross-client agreement (the cloud
  strike burst reuses the strike's already-synced seed, so burst
  and bolt describe the same strike). carrier_tick stays
  carrier-local in the renderers: the carrier entity is already
  synced, its continuous shed needs no cross-client identity, and
  a packet per tick would be pure waste. charge_tick is
  client-local from the Phase 2 cast store: every client already
  knows who is charging and how far along, so the server sends no
  charge particles at all (the Phase 1 server-sent charge motes
  are deleted).
- Defaults: ability/AbilityFxDefaults mirrors the old Phase 1
  hardcoded sheds as code-level default carrier_tick recipes
  (wave band shed, cloud ground puff plus drift, projectile
  cluster plus wake band plus a capped spark trickle), used only
  when a definition authors no carrier_tick recipes of its own.
  The two proof attacks author theirs; their numbers are in the
  phase report.
- What did not migrate: the carriers' geometry (wave band, cloud
  dome shells, telegraph, strike bolt, projectile casing and
  core) is geometry, not particles, and stays in the renderers
  for the Phase 4 kit. The projectile's server-side burst() hit
  flash stays server-sent material particles this phase: it is
  the carrier's own impact flash on the shared material system,
  not an ability recipe emission, and converting it belongs with
  the Phase 4 pass over carrier presentation. Non-ability users
  of the material system (auras, weather, StaggerFx, the wish
  ritual) are untouched by design.

## Phase 4: the primitive geometry kit (landed)

- The kit lives in client/render/kit/ and is the only shape
  language the ability renderers speak from this phase on. Every
  builder is seeded and deterministic (SeedHash noise and hashes,
  never shared random state), documents its vertex budget in its
  javadoc, takes its colors as the definition's core/mid/rim, and
  returns the vertex count it emitted. The emission hand-off is
  KitEmit, a thin wrapper over KiRenderHelper.vertex, so the
  world-lit path (submitSolidLit) applies to kit pieces exactly
  as to the older ComplexGeometry builders: physical variants are
  world-lit, always; energy variants go through submitEnergy.
- The builders: CrescentBand (the wave band: the Phase 1
  three-strip profile in tables, plus seeded turbulence that
  breathes each column's radius and tears the top edge; at
  turbulence 0 the emitted vertices are the Phase 1 band's,
  position for position and color for color). VortexBand (a
  raised ring of torn energy slabs with real gaps, per-slab tilt,
  jitter, and facet shade; rotation is a caller parameter).
  DomeShell (hemisphere layers only, the buried lower half of the
  old spheres is no longer emitted; scrolling alpha bands when
  swirlSpeed is nonzero, rim brightening toward the equator when
  rimGlow is nonzero, up to 3 nested layers; at the defaults the
  layers sit at the old spheres' radii and alphas). TornWall
  (clustered world-lit rock slabs with a grouped ridge profile
  and per-slab ground sampling through a caller sampler).
  ChainRun (instanced flat-ring links along a sagging catenary,
  alternating roll per link). CrackDecal (emissive ground crack
  fan with an explicit crack count, the answer to the old
  crackFan branch gap). BeamDrawer (an entity-less beam: seeded
  wiggle chain, crossed-ribbon core and halo shells, end taper).
  PlateCasing (four flat sabot plates in two length classes, the
  answer to the flat plate builder gap).
- Schema: the definition gains an optional "geometry" block with
  four surfaces (wave: turbulence, baseRing; cloud: shellLayers
  1 to 3, swirlSpeed, shellAlpha, rimGlow, strikeHueSplit;
  charge: raisedRing, ringHeight, ringWidth; projectile:
  casingForm shard or plate), parsed into AbilityDef.GeometrySpec
  and validated like every other block: unknown blocks, unknown
  fields, wrong types, and out-of-range values are rejected
  naming the exact field (geometry.wave.turbulence style).
  GeometrySpec.DEFAULT reproduces the Phase 1 to 3 rendering, so
  an ability without the block renders exactly as before.
- Integration is behind the existing renderers, presentation
  only: WaveCarrierRenderer draws its band through CrescentBand
  and gates its base ring on wave.baseRing; CloudCarrierRenderer
  draws its domain through DomeShell and splits its synced strike
  bolt hue by strike seed when cloud.strikeHueSplit is set (the
  bolt's glow takes the rim hue on odd strike seeds, so every
  observer sees the same strike in the same hue); the
  ChargeRingRenderer adds the raised VortexBand when
  charge.raisedRing is set, lifting and brightening with the
  charge fraction, seeded by the caster's UUID so every observer
  tears the same ring; ProjectileCarrierRenderer swaps its casing
  body to PlateCasing when projectile.casingForm is plate (the
  inset hot core is unchanged). TornWall, ChainRun, CrackDecal,
  and BeamDrawer have no consumers yet by design: Phase 6 authors
  the wall, chain, crack, and beam attacks on them, and the
  Phase 4 harness proves their budgets and determinism now.
- The projectile burst() question left open by Phase 3 is closed
  by the code, not by preference: burst() is and always was a
  damage-only method (AoE application on hit) at every rebuild
  head; no server-sent material particle flash exists anywhere in
  the projectile hit path. The visual impact flash already rides
  the Phase 3 recipe route (the carrier_hit seeded packet, fired
  on the same tick), so there was nothing to fold into the kit.
  burst() stays a damage method; the recipe system owns the
  projectile's impact presentation.
- Proof upgrades, data only: Z Shockwave authors wave turbulence
  0.45 and a raised charge ring (height 1.15, width 0.26); Cursed
  Tempest authors a 2-layer shell with swirl 1.2, rim glow 0.7,
  and strike hue split. Picture verdicts live in the phase
  report.

## Phase 5: the feedback service (landed)

One service owns how abilities feel in the camera and the air:
data-authored screen shake with real arbitration, domain fog,
and a deliberately narrow answer on hit-stop.

- Shake, Iron's Spells style. The schema gains a top-level
  "feedback" block: one optional shake per event (onFire,
  onCarrierHit, onCarrierEnd), each {strength (required, up to
  the hard cap 3.0), durationTicks (default 10, cap 40), radius
  (default 16, cap 64)}. The caps live in the parser
  (AbilityDef.MAX_SHAKE_*), so data cannot make players sick.
  ability/AbilityFeedbackService sends ONE ScreenShakePayload
  per event to the players inside the radius, on the same tick
  the event already happens on (called beside the AbilityFxService
  sends in AbilityService.fire and the carriers' hit and expiry
  paths). The service is gated on the data, not on fx recipes.
- The payload grew an origin and a radius. ScreenShakePayload
  was already the mod's one shake channel (registered, received,
  config-scaled); Phase 5 extended it rather than adding a
  second channel, because arbitration only works if every shake
  competes in one store. Radius 0 means a direct shake with no
  falloff: the two legacy senders (MovementPhysics landings,
  the MobPhysics finisher) pass the receiver's own position and
  radius 0, byte-for-byte the feel they had. The codec is
  mod-internal on both ends, so the format change is safe.
- The client store (client/fx/ScreenShakeHandler, 3.0) holds up
  to 8 active shakes. Per tick, each shake's effective amplitude
  is intensity times FeedbackMath.falloff (distance-squared:
  full at the origin, quadratically to zero at the radius; pure
  math, harness-proven) times its trauma-squared decay, and the
  strongest drives the existing sway (layered sines plus a
  directional kick, sampled before aging, so a fresh shake's
  first tick is full strength, the old sequence). This replaces
  2.0's replace-or-ignore rule: a weaker shake arriving during a
  stronger one now waits and can surface as the winner decays,
  which is what nearest-wins arbitration means with several live
  sources. The config scale (DbaEffectsConfig.screenShake) still
  multiplies the final offsets, so players can turn all of it
  down or off. The handler's hitstop camera hold (the MobPhysics
  finishers' freeze frame) is untouched and is NOT ability
  hit-stop; see below.
- Domain fog, Cataclysm style. The cloud block gains an optional
  "atmosphere" object {tint (hex, required), strength (0 to 1,
  default 0.6), density (0 to 1, default 0.5)}. Clients evaluate
  it entirely from the carrier's already-synced state: no new
  packets. client/fx/DomainFog walks the rendered entities each
  client tick; a domain's influence is its lifecycle envelope
  (FxEnvelope over the carrier's age: in with growth, out with
  the end) times a spatial factor (full inside the live
  radiusAt radius, feathering to zero at 1.6 radii). With
  several domains live, one wins: the highest
  envelope-times-spatial-times-strength score, and its tint,
  strength, and density apply whole (colors never blend). A
  smoothed weight (about a second in, about two and a half out)
  carries transitions. client/mixin/FogRendererMixin
  post-processes the FogData at FogRenderer.setupFog's return
  (same signature on 26.2 and 26.3): fog distances scale by
  1 - weight * density * 0.85 (floored at 0.12) and the fog
  color lerps toward the tint by weight * strength. This is
  separate from the orphaned bespoke-era AtmosphereServer /
  AtmosphereClient storm system (payload-driven rain and post
  grade gloom, no senders left at this head); domains do not
  revive it, because the carrier state already carries
  everything the fog needs.
- Hit-stop: designed, NOT built. The research is blunt that the
  field mostly skips it (none of Better Combat, Epic Fight, or
  SlashBlade uses any), and the bundled definition (a
  caster-local scale of the animation AND carrier presentation
  clocks on a confirmed hit) does not fit this codebase's clock
  structure. The cast pose half would work: CastAnimations
  samples are a pure function of server ticks, so a per-local-
  player lag applied only to the follow-through segment is a
  small contained change. The carrier half does not: carrier
  render age is the entity's own tickCount (shared client
  simulation state, sampled into EntityRenderState.ageInTicks),
  the carrier's owner is deliberately not synced (the Phase 1
  contract: ability id, seed, power, nothing else), and slowing
  a carrier's presentation would desync its visuals from the
  positions and radii the server is applying damage from, which
  the wave and cloud contracts explicitly forbid (what you see
  is what can hit you). The designed field, for when Phase 6 or
  later wants to pay for it properly: "hitStopTicks" (int, cap
  4) inside the feedback block, fired by a caster-only direct
  packet at the carrier_hit moment, freezing the local player's
  CastAnimations sample clock for that many ticks (post-strike
  segment only, entry ends late by the same count). Unblocking
  the carrier half needs a deliberate contract change (a synced
  owner on carriers) plus per-renderer age rewriting; neither
  was forced here. The full reasoning is in the Phase 5 report.
- Proof wiring: Z Shockwave authors onFire 0.9 / 12 ticks /
  radius 20 and onCarrierHit 1.5 / 14 / 16. Cursed Tempest
  authors onCarrierHit 0.45 / 8 / 14 (strikes land every 16
  ticks, so the per-strike shake stays seasoning) and a violet
  atmosphere (tint #3B1163, strength 0.65, density 0.5). Values
  are conservative on purpose; feel is tuned after Carter's PC
  test, in data.
