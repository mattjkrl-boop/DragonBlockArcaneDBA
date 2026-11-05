# REBUILD PHASE 1 REPORT: data-driven foundation, three carriers, two proof attacks

Date: Nov 4, 2026. Status: complete, committed locally in chunks,
pushed to origin/main (authorized by Carter). Compile check remains
waived by Carter's standing rule; verification is the dual javac
audit sweep, exactly like every prior pass. The in-game look is
unverified until Carter's PC test, same as always.

## Step 0: the fresh-start base

- The strip was committed exactly as it stood: c680c07 "Fresh
  start: strip all weapon abilities, effects, and visuals (designs
  preserved in brain)". Tree clean after; origin/main was still
  9834fb3 at commit time.
- Fresh sweep baseline measured at c680c07: 120 error kinds on
  26.2, 126 on 26.3 (matches the expected post-strip counts).
  Persistent copies: ~/workspace/dba-audit-libs/
  kinds-rebuild-base262.txt and kinds-rebuild-base263.txt. All of
  Phase 1 is measured against these.

## What was built

### Architecture (audits/REBUILD-ARCHITECTURE.md)
The full rebuild architecture concretized for this codebase:
ability data schema, the three Phase 1 carriers with their synced
fields, input routing, damage and cost flow through existing Ki and
stat systems, phase gates for later work, and the hard rules (no
per-weapon Java, geometry as hero with particles as decay only,
caps in data, physical matter world-lit only, one damage path, one
cost path).

### Data layer (src/main/java/com/dragonblockarcanedba/ability/)
- AbilityDef.java: the schema as one validated record. parse()
  rejects unknown fields by name, checks every range, and throws
  errors that name the exact bad field. Damage types map onto the
  existing DbaDamage family; materials map onto DbaMaterialOptions
  ids; colors parse from #RRGGBB / #AARRGGBB; kiPrice() combines
  flat and percent-of-max costs.
- AbilityRegistry.java: abilities by id, and item id plus slot to
  ability id bindings. One registry filled two ways (server loader,
  client sync), so both sides run identical data.
- AbilityLoader.java: datapack loader in the RaceLoader pattern
  (data/dragonblockarcanedba/abilities/*.json). Bad files log a
  loud error with the filename and reason, are skipped, and never
  break the rest. Also validates that the named weapon item exists.
- AbilityDamage.java: the single damage path. amount = (base +
  stat x perStat) x charge power x falloff, then the existing
  DbaEffectsConfig damage cap. Stats read through PlayerStats-
  Accessor. Radial helper for carrier area damage.
- AbilityService.java: the single execution path. One active cast
  per player; press checks lockout, cooldown, and Ki (fizzle sound
  when short); charge-release casts scale power from hold time the
  SERVER measured; Ki is checked at press and paid at fire;
  recovery lockout then cooldown are set from data; carriers spawn
  from data only.

### Network (network/)
- C2SWeaponClickPayload.java: rebuilt click payload, 3 varints
  (button, action, heldTicks). Carries no ability id; the server
  re-derives the ability from the held item.
- AbilitySyncPayload.java: server-to-client copy of every accepted
  definition's raw JSON, sent on datapack sync.
- DbaNetwork.java and DragonBlockArcaneDBA.java: registrations and
  the receiver that hands clicks to AbilityService.

### Carriers (entity/, client/render/ability/)
- CarrierEntity.java: shared base for Wave and Cloud. Syncs only
  ability id, seed, power (plus owner uuid). Invulnerable, never
  saved (id, seed, power written to NBT defensively and defs
  re-resolved on load).
- WaveCarrierEntity.java + WaveCarrierRenderer.java: travels
  straight, ground-snapped by the heightmap; server damages each
  entity once as the band reaches it, with data falloff; client
  draws a ground-anchored arc band in three stacked strips (rim
  base, mid body, bright core middle, fading crown) plus a ground
  ring, and sheds capped material particles as decay.
- CloudCarrierEntity.java + CloudCarrierRenderer.java: stationary
  domain; radius follows the shared radiusAt formula from
  radiusStart to radiusEnd over its life; server schedules
  telegraphed strikes strictly one at a time (target point chosen
  telegraphTicks early, synced; strike lands as an area hit plus
  the hit sound). Client draws the perimeter rim ring, two nested
  translucent dome shells, interior seeded branched lightning that
  every client agrees on, the telegraph pillar and ground ring,
  the strike bolt with a 3-tick flash, and capped interior weather
  particles.
- ProjectileCarrierEntity.java + ProjectileCarrierRenderer.java:
  a ThrowableProjectile with data gravity, direct damage plus
  optional area burst on entity or block hit. Client casing from
  data: "lit" draws a world-lit physical shard (world-lit rule),
  "energy" draws an additive shard, "none" draws none; the energy
  core shard and a capped particle wake always render.
- DbaEntities.java: the three carrier types registered
  (ability_wave, ability_cloud, ability_projectile).

### Client wiring (client/ability/, DragonBlockArcaneDBAClient.java)
- ClientAbility.java: receives AbilitySyncPayload, re-parses every
  definition with the shared parser into the shared registry.
- WeaponInputHandler.java: watches attack and use keys while the
  main hand holds a bound weapon; sends press on the rising edge
  and release with counted hold ticks on the falling edge; silent
  in screens, and sends the pending release if a screen opens
  mid-hold.
- Client init registers the three carrier renderers and starts the
  ability client layer.

### Proof attacks (data only, zero weapon-specific Java)
- data/.../abilities/z_shockwave.json: Z Sword left on the Wave
  carrier. 20-tick golden charge (chargeRelease, min power 0.4),
  wave speed 1.7, radius 7, height 3.4, 110-degree arc, 900 +
  0.5 x Strength ki-type damage with linear falloff to 40 percent,
  10 percent of max Ki, 30-tick cooldown after recovery.
- data/.../abilities/cursed_tempest.json: Cursed Blade right on
  the Cloud carrier. Radius 5 growing to 15 (the design's 10 to 30
  blocks read as diameter), 300-tick life, strikes every 16 ticks
  with a 10-tick telegraph and 2.6 strike radius, 650 + 0.4 x
  Spirit curse damage per strike with linear falloff to 50
  percent, 15 percent of max Ki, 100-tick cooldown.
- Both files were run through the real AbilityDef parser and
  registry in a standalone harness: both parse, register, and bind
  (z_sword/LEFT, cursed_blade/RIGHT), and the validator correctly
  rejects a bad stat name, a bad slot, and an unknown field, each
  with a message naming the problem.

## Verification

- Dual sweeps at the finished tree: 120 kinds on 26.2, 126 kinds
  on 26.3; comm -13 against the Step 0 baselines prints NOTHING on
  both classpaths. The new code adds zero new error kinds.
- All 569 resource JSONs parse (567 pre-existing plus the 2 proof
  files); the 2 proof files additionally pass the loader's own
  validation (harness above).
- git diff --check clean. Em dash scan over every new and edited
  file: zero.
- Split source sets hold: the common carrier and ability code
  references no client classes; all rendering lives behind the
  client entrypoint.
- No per-weapon Java exists for either proof attack; the weapon
  item classes stayed the plain stripped items. Behavior comes
  purely from the registry binding by item id.

## Proof attacks vs. their approved pictures

Judged per Carter's relaxed guidance: general direction, best
effort, match level stated plainly.

### Z Shockwave (z-sword.png concept + cool card): PARTIAL
- Strong: the choreography (charge, release, massive wave), the
  color language (white core, gold body, orange rim), the
  silhouette family (a horizontal rolling wall of gold energy),
  ground anchoring (the band rides the terrain), enemies damaged
  as the wave passes them.
- Partial: the charge. The card's charge is flat golden vortex
  rings coiling around the player like a Saturn ring; this phase's
  charge is gold material motes gathering at the caster, played by
  the server as a stand-in until the animation layer and ring
  primitives exist.
- Weak: no knockback or throw (the card hurls enemies and blocks;
  the wave damages but does not push), no scorched glowing path
  left behind (no ground decal system yet), no flying debris, and
  the band is a clean gradient wall where the card's wall has
  turbulent scrolling texture. Band height 3.4 blocks is close to
  the card's roughly two-times-player wall but slightly small.

### Cursed Tempest (cursed-blade.png concept + cool card): PARTIAL, the stronger of the two
- Strong: domain scale is exact (grows 10 to 30 blocks across),
  the defined ground rim ring, strikes landing throughout the
  area, telegraphed strikes with a visible pillar and ground ring
  before each bolt (the card's step 2), forked lightning geometry
  inside the domain, the dark violet storm palette.
- Partial: the two-hue lightning. The card's signature is violet
  and crimson bolts side by side; here ambient and strike bolts
  are violet and white (mid and core), and crimson (rim) shows in
  the rim ring, the dome tint, and the telegraph ring rather than
  in the bolts themselves. Splitting bolt hues by strike seed is a
  small renderer change once Carter wants it.
- Weak: the dome shell is two smooth translucent spheres where
  the card shows a swirling cloud vortex mass (needs the scrolling
  texture work of the primitive kit phase); enemies are not lifted
  or dragged by the storm (the Cloud carrier has no movement
  control this phase); the concept picture's rain reads here as
  drifting dark motes; no floating debris.

## Design decisions the schema forced

1. Cloud radius semantics: the design record says "10 to 30
   blocks" for the Tempest; the data reads that as diameter and
   stores radius 5 to 15. Recorded here so a future retune is a
   one-line data change, not an argument.
2. Damage numbers: the design record carries numbers for only a
   few weapons (Devil Trident's full set). Z Shockwave and Cursed
   Tempest needed numbers anyway, so they were set inside the
   mod's existing family scale (900 and 650 base). They are tuning
   values in data, flagged as such, and Carter can retune them
   without touching code.
3. Ki pricing model: both proofs use percent-of-max Ki (10 and 15
   percent) because DBA's Ki pool grows enormously with forms and
   a flat cost goes meaningless at one end or the other. The
   schema supports flat, percent, or both summed.
4. One registry, two fill paths: the architecture draft named a
   separate client cache; the implementation fills the same
   AbilityRegistry on the client from the sync payload instead.
   Fewer moving parts, same parser, identical data. The
   architecture document was corrected to match.
5. Cloud strikes are sequential: one pending strike at a time,
   because the telegraph sync is five fields for a single strike.
   Parallel strikes would need a synced strike list; the damage
   rate difference is absorbed by strikeInterval in data.
6. Owner-only exemption: carriers never damage their owner. Team,
   ally, and party exemptions do not exist as a DBA concept in
   code yet, so none was invented; the exemption list is one
   parameter away in AbilityDamage.applyArea when the game grows
   one.
7. Wave hit model: damage lands once per entity when the band
   reaches it, with falloff by distance from the wave's center,
   not from the band's leading edge. Simpler, predictable, and it
   reads correctly for a ground wave.
8. Hold time is measured by the server: the payload's heldTicks is
   advisory; the cast service times the charge from its own press
   registration, so a lying or lagging client cannot inflate power.
9. Charge visuals are server-sent material motes this phase. That
   is a deliberate placeholder, called out in AbilityService, and
   it is the first thing the animation layer replaces.
10. Ability entities refuse to save. If one is persisted anyway
    (chunk edge cases), its NBT carries id, seed, and power, and
    it re-resolves its definition or discards itself.
11. Sound ids are data strings resolved against the sound
    registry at play time; a bad id in data is silently skipped by
    the registry lookup rather than crashing a cast. The one
    hardcoded sound is the Ki-shortage fizzle in AbilityService,
    which is a system sound, not a weapon sound.

## What Phase 2 (animation layer) hooks into

- The phases are already the contract: startup, active, recovery
  exist in every definition, and AbilityService fires exactly at
  the startup-to-active boundary, which is where the animation
  layer's keyframe events will take over effect timing.
- The schema reserves the "animation" field (currently null,
  ignored by the parser's known-field list once Phase 2 defines
  its shape).
- AbilityService's cast record (player, ability, start tick) and
  its power curve are the server-side charge state the client pose
  layer will mirror; the sync payload is the natural channel for a
  later "who is charging what" packet if poses need cross-player
  visibility.
- The input handler's press and release edges are where swing
  poses and trail capture (Better Combat style, per the research)
  attach; the release edge already carries hold duration.
- The feedback service (research step 5) hooks the same two
  moments: fire (shake on release of big attacks) and carrier hit
  application (AbilityDamage.apply is the single choke point for
  hit feedback), with ScreenShakePayload already in the codebase.

## Remaining honest gaps

- Only 2 of 44 attacks exist as data. Authoring the rest is the
  all-attacks phase, on this same foundation.
- The Projectile carrier has no proof attack yet; it is built and
  sweep-clean but unproven by data. A Blaster or Power Pole attack
  is the natural first user.
- No animation, no swing trails, no ground decals, no screen shake
  wiring, no movement control (lift, drag, knockback) in any
  carrier yet. Each is a named later phase in the research build
  order, and the schema fields for caps and phases already assume
  them.
- In-game look of both proof attacks is unverified until Carter
  tests on his PC.
