# Rebuild Models Pass Report

Base: 71c1fd3 (the completed data-driven rebuild). Directive: Carter,
Thu Nov 5, 2026: models and animations are authored here as files;
anything that needs a model gets one; no more shipping signature
pieces as volumes because "the kit has no model."

## Pipeline audit (derived from code at 71c1fd3)

- Geo models live at
  assets/dragonblockarcanedba/geckolib/models/entity/<name>.geo.json,
  format_version 1.12.0, identifier
  geometry.dragonblockarcanedba.<name>, texture_width/height equal
  to the PNG size. Textures at textures/entity/<name>.png; a
  <name>_glowmask.png renders emissive through GeckoLib's
  AutoGlowingGeoLayer (the route the weapon renderers already use),
  so energy parts can glow while the base texture stays world-lit.
- Animations are GeckoLib .animation.json files (format 1.10.0) at
  geckolib/animations/entity/<name>.animation.json, driven by an
  AnimationController on a GeoEntity. (Player poses are a separate
  Bedrock engine and were not touched.)
- The load-bearing API fact, verified against the shipped GeckoLib
  jar: one GeoModel subclass can resolve model, texture, and
  animation paths from a name stashed in the render state
  (DataTicket), so ONE dynamic model plus ONE renderer serves every
  present and future modeled piece named in data. No per-weapon
  renderer exists or will be added.

## Inventory: what was a stand-in, what was not

IN (a model fixes it):
1. Spirit Impale's six celestial swords: were a pillar of wisp fx
   and a damage volume. The design and the approved picture call
   for six swords that descend, plant in a ring, and hold.
2. Eclipse Domain's sky blades: the finale was strikes on points.
   The picture calls for blades raining from the darkened sky.
3. Azure Dragon Rush's dragon: the research report named a
   segmented GeckoLib dragon aura on the flight path "the one
   place worth real money"; the rush shipped as a plain wave band.
4. Evil Impale's spear: the final audit recorded it as "an
   energy-cased shard, not a spear model"; the design says a huge
   flaming spectral spear.
5. Time Shatter's glass: the design freezes time-fracture
   fragments in the air and drops them on release; shipped as
   shard-chip particles.

OUT (judged not stand-ins):
- Devil Trident swarm shards: rated STRONG in the final audit;
  real entities with a deliberate world-lit shard plus energy
  core look, not a volume pretending to be an object.
- Grand Cyclone spin shards: rated STRONG; the picture reads as
  fast debris orbiting a vortex, which the recipe already sells.
- Katchin Weight sword rain: the final audit judged it picture
  dressing, not the design (the design spends weight stacks on
  slams and zones, which are modeled in data already).
- evil_spikes body-space spikes: SpikeCluster kit geometry by
  design, grown from the target's body space, not a missing model.
- Flashdraw and Hollow's Edge afterimages: player ghosts, handled
  by the animation layer, not objects.
- Meteor casing crack texture and Valor Field's hex-cell dome
  pattern: texture-level detail on pieces that already read; not
  modeled objects.

## Models authored (all files, all here)

- celestial_sword (Spirit Impale): 17 cubes, 6 bones
  (root/blade/blade_tip/guard/hilt/gem), 128x128 texture, built
  pointing down with the origin at the plant tip, about 4.1 blocks
  tall. Near-white blade, cyan fuller, teal guard and hilt, gold
  trim; glowmask on blade edges, fuller, and gem. Clips: descend
  (0.8s loop, faint sway), hold (2.4s loop, breathing bob and yaw),
  release (0.5s, flare and lift, hold on last frame). Six of them
  descend over 9 ticks into a ring of radius 2.6 around the summon
  point and hold for the cloud's 50-tick life.
- sky_blade (Eclipse Domain finale): 17 cubes, 5 bones, 128x128,
  a heavier, broader sibling (base width 7 units) with a jagged
  back edge, dark violet-black steel and blackened bronze;
  glowmask only on the violet-magenta runes and guard stone.
  Clips: fall (0.4s loop vibration), shatter (0.35s burst, hold on
  last frame). Fourteen rain across the finale radius at the
  finale tick, spinning at 25 degrees per tick, timed with the
  finale's six damage strikes.
- azure_dragon (Dragon Rush): 50 cubes, 23 bones, 256x256. Head
  with snout, jaw, swept horns, whiskers, and mane; eight chained
  body segments (each parented to the previous so the chain
  undulates), dorsal fins on segments 1/3/5, clawed arms under
  segment 1, tail fin. Painted azure scales, pale belly, glowmask
  on eyes, fin centers, horn and whisker tips, dorsal stripe.
  Clips: swim (1.6s loop, per-segment phase-offset sine travel,
  20 bones animated) and idle (3.2s, half amplitude). One dragon
  at scale 1.35 escorts the caster for the rush's 60 ticks,
  leading the wave.
- spectral_spear (Evil Impale): 10 cubes, 7 bones, 128x128, about
  5.25 blocks long along Z (tip forward), scorched iron shaft,
  molten orange-gold head and flame tongues; glowmask on the flame
  set and head edges. Clip: fly (0.6s loop, flame scale pulse).
  It flies the cast line as a bolt at the projectile's own speed
  (2.6), so the thrown spear is a spear; the energy projectile
  still flies with it and reads as its burning wake.
- time_shard (Time Shatter): 10 cubes, 7 bones (a cluster core
  plus six shards on their own pivots), 64x64, pale ice facets;
  glowmask on cores and facet highlights. Clips: hang (1.2s loop
  tremble), fall (0.5s loop), shatter (0.4s outward scatter, hold
  on last frame). Three clusters hang frozen at the hit point for
  the 36-tick freeze, then drop and shatter on the ground, the
  design's freeze-and-release beat in geometry.

Generator scripts for all five sets live in tools/ (gen_*.py),
painting textures from the same layout constants the geometry
uses, so a set can be regenerated or adjusted in one place.

## Generic wiring (no per-weapon Java)

- AbilityDef gains an optional "models" block (ModelSpec):
  model, count, formation (ring/rain/escort/point), behavior
  (descend/fall/escort/bolt/hang), trigger (fire/finale), radius,
  height, scale, lifetimeTicks, descendTicks, fallSpeed, spin,
  hangTicks, speed, forward, up, and animSpawn/animHold/animEnd
  clip names. Unknown fields are rejected by name; every value is
  range-checked; clip and model names are validated like all other
  names in the schema. A lifetime left at 0 derives from the def
  (carrier active window, hang time plus drop, or a fall safety
  cap) in the spawner.
- ModelDisplayEntity (entity package): a plain Entity implementing
  GeoEntity with a synced contract of model name, three clip
  names, scale, and an animation phase byte (spawn/hold/end) the
  server sets from the behavior clock, so every observer plays
  the same clip on the same tick. Server behaviors: descend
  (eased plant onto the heightmap), fall (accelerating rain,
  spin, ground shatter window), hang (static, then fall), bolt
  (flies the cast velocity, discards on block or lifetime), escort
  (rides the owner's look line with per-index trailing). It deals
  no damage, is not pickable, refuses hurtServer, never saves.
- Registered once as DbaEntities.MODEL_DISPLAY. Spawning is
  centralized in ability/ModelDisplaySpawner, called from the one
  carrier spawn path in AbilityService.spawnCarrier (fire trigger,
  so casts, summons, moves, and channels dress alike; Spirit
  Impale arrives through its summon) and from
  CloudCarrierEntity.onExpire (finale trigger).
- Client: ModelDisplayGeoModel (dynamic paths from the synced
  name) and ModelDisplayRenderer (GeoEntityRenderer plus
  AutoGlowingGeoLayer, per-entity scale from the synced value),
  registered beside the carrier renderers.
- Behavior and numbers are unchanged everywhere: same damage,
  ticks, costs, and carriers. The models are presentation riding
  on the same events.

## Gates

- Model checker (tools/check_models.py, new): every geo parses,
  every animation bone exists in its geometry, texture and
  glowmask exist at the resolved paths and match the declared
  size, and every clip named in data exists in the model's file.
  Result: ALL MODEL REFERENCES RESOLVE (5 sets, 5 defs).
- Schema harness: 12 of 12 (five good blocks parse; unknown
  field, bad behavior/formation/trigger, bad model name, count
  over cap, and bad clip name each rejected naming the field).
- Roster harness: 54 defs parse, 44 bound weapon/slot pairs,
  5 models defs. Unchanged from the rebuild close.
- Javac sweeps: message-level comparison against the rebuild
  baselines is empty on both the 26.2 and 26.3 classpaths, and
  zero error kinds name any new or edited file on either. (One
  real cross-version break was caught and fixed during the pass:
  the invulnerability flag setter is renamed between 26.2 and
  26.3, so the display entity relies on its hurtServer refusal
  instead of the flag.)
- All 633 resource JSONs parse. git diff --check clean. Em dash
  scan of all new and edited files: zero.

## Updated picture ratings

- Spirit Impale: PARTIAL-STRONG to STRONG. The six swords are
  now the picture: modeled celestial blades descending into
  their seal ring and holding over the impale zone.
- Eclipse Domain: PARTIAL to PARTIAL-STRONG. The finale now
  rains fourteen modeled sky blades with the execute; the domain
  body itself remains shell, fog, and pull as before.
- Dragon Rush: PARTIAL to PARTIAL-STRONG. The rush now carries
  its dragon; the wave band is still the visible damage body
  under it.
- Evil Impale: PARTIAL to PARTIAL-STRONG (not in the required
  three, improved anyway): the thrown piece is a modeled spear.
- Time Shatter: PARTIAL to PARTIAL-STRONG: frozen modeled glass
  clusters hang, drop, and shatter on the release beat.

## Standing honest lines

- No Gradle build ran in this VM (sandbox loopback restriction);
  verification is the dual javac sweep plus the harnesses above.
  Compile check remains waived, never passed, per Carter's word.
- The in-game look is unverified until Carter's PC test. Named
  checks for that test: bolt and escort forward-axis orientation
  (the spear and the dragon are authored tip/head toward +Z; if
  either reads backward in game it is a model-side flip, not a
  code change), the dragon's escort height against the rush pose,
  sky blade fall speed against the finale strikes, and whether
  six planted swords at ring radius 2.6 read as a seal around the
  3.2-radius zone or want the wider ring.
- Counts stay sane by design: worst added scene is the Eclipse
  finale's 14 blades (238 cubes total) plus 6 swords (102) if
  both overlap; geometry, not particles, carries these pieces.
