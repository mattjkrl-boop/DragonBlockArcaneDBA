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
- animation (string, optional): reserved for Phase 2, the animation
  clip id for this attack. Parsed and carried now so data written
  this phase never needs a schema change when the layer lands.
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
