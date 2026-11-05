# Rebuild Phase 6 plan: the full roster, 22 weapons, 44 attacks

Written at HEAD 4a9fc6b from a fresh read of the code (AbilityDef,
AbilityService, the three carriers, AbilityDamage, DbaEffects,
PermanentDestruction, the kit), the phase 1 to 5 reports, and the
binding design record (brain mods/DBA-ATTACK-DESIGNS.md). Rule of
the phase, unchanged from the whole rebuild: abilities are data,
carriers and modules are code, no weapon owns Java. A mechanic
enters as a generic module with data parameters or not at all.

## 1. What the roster forces (the generic module list)

Reading all 44 designs against the Phase 1 to 5 system, the gaps
are exactly these, and each is built ONCE, centrally:

- M1 Beam carrier (Spirit Cannon, Erasure Cannon, Power Pole
  extension): a CarrierEntity that rides the owner's eye and look,
  applies the ability damage along a clipped segment on a data
  interval, renders through BeamDrawer (energy) or a lit cylinder
  (solid). Channel behavior comes from M6.
- M2 Caster movement (move block): dash, chain (teleport hops
  through up to N targets), step (short teleport), warp (long
  teleport to look point), launch (vertical), slam (dive and fire
  on landing), rush (sustained steered flight that fires its
  carrier on landing). Timed modes run as a per-player runtime in
  AbilityService; when a def has both a move and a carrier, the
  carrier fires when the timed move completes (instant modes fire
  it immediately, post-move). Step and warp with a cloud carrier
  also echo the cloud at the departure point (Flash Step rifts at
  both ends, from one composition rule).
- M3 Melee block: an instant cone hit at the fire tick (range,
  arc; single closest target, or all in radius at arc 360), the
  damage path's melee kind. Feeds M5 and M9.
- M4 On-hit application (onHit block): effect id, duration,
  amplifier, lifesteal fraction, scoped any/melee/carrier,
  applied at the one damage choke point. Plus the swing block:
  a passive effect a weapon's vanilla melee applies, hooked in
  the surviving AttackEntityCallback (Dimensional Melting).
- M5 Summons: a def may spawn another def's carrier at fire, at
  full power, at a critical-window release, at a melee hit, or
  at a carrier's first hit (at caster, at look target, or at the
  hit position). Summon-only defs set "bind": false. This one
  mechanism covers the Spirit Impale, the Time Shatter burst,
  the Evil Spear body spikes, the Ox titan crags, the Bansho
  cyclone and shockwave, and the Warp destination dressing.
- M6 Aura block: active while the def's cast is held (charge or
  channel): radius, signed pull, projectile deflection with
  ownership claim, optional periodic damage at a damage scale,
  optional self effect. Grand Cyclone's spin and Ox Brace's
  repulsion stance are the same module with different signs.
- M7 Channels: sustain (one beam carrier alive while held,
  draining Ki percent per second; release, empty Ki, weapon
  swap, or active cap ends it) and autoFire (repeated projectile
  spawns on an interval while held, heat accumulating to an
  overcharged shot at data power and AoE scale). Blaster left
  is autoFire; Spirit Cannon is sustain.
- M8 Cloud field modules: follow (none, caster, entity anchor),
  signed pull, vortex (lift plus swirl), projectile stasis
  (freeze with velocity restore, or per-tick velocity scale),
  inside effects on an interval, inside damage on an interval,
  chains (latched set; the renderer draws ChainRun to latched
  entities), freeze window (insiders velocity-zeroed plus
  temporal stasis), a one-shot rewind (see M11), an on-spawn
  burst, and a finale (damage scale, radius scale, signed
  knockback, slam-down, extra strikes). Valor Field, Eclipse,
  both Tempests, both Rifts, Katchin, both chain attacks, and
  the Lava field are all parameterizations of this one carrier.
- M9 Wave modules: knockback (out plus up), projectile deflect
  (scale, claim), self-launch on first hit (War Mace relaunch).
- M10 Terrain (PermanentDestruction): one block, five events:
  spawn (crater or place disk at the carrier), finale (at a
  cloud's end), impact (projectile hit), trail (wave lays a
  block line as it travels), field (a cloud converts its floor
  over time and crusts the ring when it ends). All through
  PermanentDestruction's protection and caps.
- M11 Rewind service: a light global position history (every
  living entity within 48 blocks of a player, sampled every 4
  ticks, 120 ticks kept). The Temporal Rift cloud records
  nothing itself; at its rewind moment it sends its current
  insiders back by the data's rewindTicks. Position only:
  health, effects, and inventory are NOT rewound, named here
  and in the final audit.
- M12 Cast variants and state: sneakVariant and airVariant
  ability ids resolved at press; charges (multi-use with regen,
  Flash Step); combo counting with a carrier armed on
  completion (Brave Assault); a critical release window with
  power scale and a summon (Ox); rooted charges (Ox left);
  volley spawns (projectile count and spread, Tempest
  Barrage).
- M13 Swarm carrier (Devil Trident): a host entity planted at
  the deploy point running a shared health budget, N shard
  entities on the projectile family pattern (orbit, seek,
  intercept, return), target assignment and projectile
  interception from the host, Ki drain per minute, and the
  toggle state machine in AbilityService (deploy, recall with
  its cooldown, starvation with its longer cooldown, recall
  lockout, left-click target marking while deployed). The one
  dedicated carrier the roster genuinely forces.
- M14 Presentation: geometry.ground (TornWall walls, CrackDecal
  scars, a new seeded SpikeCluster kit builder) consumed by the
  wave and cloud renderers at the carrier anchor; ChainRun
  links in the cloud renderer when the chains module is
  present; beam and swarm renderers; data-driven item
  tooltips (slot label from lang, ability name from the def,
  through the client tooltip callback reading the synced
  registry).

## 2. Schema additions (validated by name, like every block)

Top level, all optional: bind (bool, default true),
sneakVariant and airVariant (ability id strings), rooted
(bool), charges (1 to 16, default 1), chargeRegenTicks,
move, melee, onHit, swing, summons (list), aura, sustain,
autoFire, combo, criticalWindow, volley, terrain.
Carrier enum gains beam, swarm, none; the matching carrier
blocks are beam and swarm.

- move: mode (dash, chain, step, warp, launch, slam, rush),
  distance (0 to 128), maxDistance (warp, 16 to 1024),
  targets (chain, 1 to 16), range (chain, 4 to 64),
  stepTicks (chain, 1 to 20), damageOnPass (bool).
- melee: range (1 to 12, default 4), arcDegrees (10 to 360,
  default 90).
- onHit: effect (id, optional), durationTicks, amplifier,
  healFraction (0 to 1), appliesTo (any, melee, carrier).
  Rejected when it would do nothing (no effect, no heal).
- swing: effect (required), durationTicks, amplifier.
- summons entries: ability (id, required), at (caster, look,
  hit), when (fire, fullPower, critical, meleeHit, carrierHit),
  once (bool, default true).
- aura: radius (1 to 32), pull (-4 to 4), deflect (bool),
  deflectScale (0.1 to 4), damageInterval (0 = off),
  damageScale (0 to 2), selfEffect (id), selfEffectDuration.
- sustain: kiPercentPerSecond (0 to 100).
- autoFire: intervalTicks, heatPerShot, overchargeHeat
  (0 = off), overchargePower, overchargeAoeScale.
- combo: hits, windowTicks, carrierOnComplete (bool).
- criticalWindow: fromTicks, toTicks, powerScale,
  summonAbility (id, optional).
- volley: count (1 to 16), spreadDegrees (0 to 180),
  delayTicks.
- terrain: event (spawn, finale, impact, trail, field),
  radius (0.5 to 32), cap (1 to 4096), craterDepth (0 to 2),
  place (block id, optional), crust (block id, optional).
- beam: length (1 to 128), radius (0.1 to 8),
  damageInterval (1 to 100), form (energy, solid),
  endBurstRadius (0 to 16).
- swarm: count, budget, shardBase, shardPerStat, shardStat,
  slamRadius, interceptRadius, assignRadius,
  drainPercentPerMinute, recallCooldownTicks,
  starveCooldownTicks, lockoutTicks.

Cloud block additions: follow (none, caster, entity), anchor
gains "entity", pull (-4 to 4), vortex {lift, swirl}, stasis
{mode freeze or slow, factor}, insideEffect {effect,
durationTicks, intervalTicks}, insideDamageInterval, chains
{maxLinks}, freezeTicks, rewindTicks, onSpawnBurst
{damageScale, knockback}, finale {damageScale, radiusScale,
knockback, slamDown, strikes}.

Wave block additions: knockback {out, up}, deflect {scale,
claim}, selfLaunch.

Geometry block addition: ground {wallSlabs (0 to 16),
wallHeight, wallArc, crackCount (0 to 16), crackRadius,
spikes (0 to 24), spikeRadius, spikeHeight}.

Cross-reference rule: the parser validates identifier FORMAT
only (so the standalone harness keeps working with no game
registries). AbilityLoader, in the real registry environment,
validates resolution after load (effect ids, summon ids,
variant ids, critical summons, terrain block ids) and logs a
loud error naming the def and the reference; execution treats
an unresolved reference as a no-op.

## 3. The roster (44 attacks)

Damage tuning: the design record's numbers are used verbatim
where given (marked *). Elsewhere the proof scale holds:
heavy singles 700 to 1000 base, medium 450 to 700, fast or
repeated 250 to 450, damage-over-time ticks 100 to 300, Ki
mostly percent of max (8 to 25). All tuning is data and is
flagged as tuning in the final audit.

1. Katana L Flashdraw: carrier none; chargeRelease (startup
   30, minPower 0.5); move chain (targets 5, range 26,
   stepTicks 2, damageOnPass); damage 550 + 0.5 Dexterity,
   void; animation charge_stance into thrust.
2. Katana R Heaven Splitter: wave (speed 0.35, radius 9,
   height 14, arc 25, active 40); geometry.ground wall on the
   wave (10 slabs, height 9) plus cracks; damage 850 +
   0.5 Strength, void.
3. Z Sword L Z Shockwave: EXISTS (proof).
4. Z Sword R Katchin Weight: cloud anchor target (radius
   6 to 9, active 80), pull 1.6, finale (damageScale 1.6,
   radiusScale 1.6, slamDown 1.6, knockback -0.4), terrain
   finale crater (radius 9, depth 0.5, cap 320), ground
   cracks; damage 800 gravity; point_slam.
5. Saber L Blitz Flurry: carrier wave fired after the chain;
   move chain (targets 5, range 26, stepTicks 2,
   damageOnPass); wave (speed 2.2, radius 8, height 3,
   arc 40); damage 500 + 0.5 Dexterity, void.
6. Saber R Flash Step: charges 3, regen 100; move step
   (distance 9); cloud (radius 2.5, active 100, pull 0.5)
   echoed at departure by the step composition rule;
   damage 0 (a movement tool; the rift's pull is the effect).
7. Brave L Assault: melee (range 4.5, arc 120); combo
   (hits 10, window 200, carrierOnComplete); wave finisher
   (arc 360, radius 5.5, speed 1.2, height 3); damage 420 +
   0.45 Strength, valor; fast phases.
8. Brave R Heroic Dash: move dash (distance by power 8 to
   24 via chargeRelease startup 16, damageOnPass); wave
   ring at arrival (arc 360, radius 4.5, speed 1.4);
   damage 550 + 0.5 Strength, valor.
9. Grand L Grand Cyclone: chargeRelease (startup 100);
   aura during hold (radius 7, pull 1.8, deflect claim
   scale 1.4, damageInterval 10 at scale 0.2); wave on
   release (speed 1.9, radius 8, height 4.2, arc 120);
   damage 900 + 0.55 Strength, valor.
10. Grand R Valor Field: cloud follow caster (radius 7,
    active 200), stasis freeze; atmosphere warm gold;
    damage 0; Ki 12 percent.
11. Azure L Dragon Rush: move rush (active 60); wave
    carrier on landing (arc 360, radius 7, speed 1.6,
    height 3); damage 650 + 0.5 Strength, tempest;
    flight_wind sound family.
12. Azure R Call of the Tempest: cloud (radius 5 to 15,
    active 300), strikes (interval 12, telegraph 6),
    vortex (lift 0.8, swirl 1.6), atmosphere blue; damage
    600 + 0.45 Spirit, tempest.
13. Dabura L Abyssal Slash: chargeRelease wave (speed 1.8,
    radius 7.5, height 3.6, arc 100); onHit petrification
    (40 ticks, appliesTo any); damage 700 + 0.5 Strength,
    darkness.
14. Dabura R Eclipse Domain: cloud (radius 4 to 15,
    active 300, follow caster), animation channel; pull
    0.8; stasis slow 0.05; insideEffect petrification
    (40, every 20); finale (damageScale 2.0, strikes 6);
    terrain finale place obsidian (radius 12, cap 220);
    atmosphere dark violet; damage 800 + 0.5 Spirit,
    darkness.
15. Cursed L Curse Chain: cloud anchor entity, follow
    entity (radius 6, active 160), chains maxLinks 10,
    insideDamageInterval 12, pull 0.3; damage 300 +
    0.35 Spirit per tick, curse; chain_rattle sounds.
16. Cursed R Cursed Tempest: EXISTS (proof).
17. Hollow L Hollow Rush: carrier none; move chain
    (targets 4, range 24, stepTicks 2, damageOnPass);
    damage 520 + 0.5 Dexterity, void.
18. Hollow R Void Rift: cloud anchor target (radius 2.5
    to 7.5, active 160), pull 2.2, vortex swirl 1.0,
    finale (damageScale 1.3, knockback -1.2); ground
    spikes ring read via recipes plus VortexBand charge
    geometry is not applicable (noted); damage 550 +
    0.45 Spirit, void.
19. Dimensional L Dimensional Slash: projectile (speed
    2.4, gravity 0, energy casing, lengthScale 1.6);
    swing effect melting (100 ticks); damage 480 +
    0.45 Dexterity, void.
20. Dimensional R Warp Rift: move warp (maxDistance
    1024); cloud at destination (radius 5, active 60),
    onSpawnBurst (damageScale 1.0, knockback 1.2);
    damage 600 + 0.5 Spirit, void.
21. Spirit L Spirit Swing: projectile (speed 2.0, energy
    casing, lengthScale 2.2, aoe 2); melee (range 4.5)
    with summons meleeHit to spirit_impale; damage 620 +
    0.5 Spirit, spirit.
    Summon def spirit_impale (bind false): cloud (radius
    3.2, active 50), onSpawnBurst (damageScale 1.0),
    damage 520 + 0.45 Spirit, spirit.
22. Spirit R Spirit Cannon: beam (length 30, radius 0.9,
    damageInterval 4, form energy); sustain
    (kiPercentPerSecond 5); damage 130 + 0.18 Spirit per
    tick, spirit; animation channel.
23. Evil L Evil Impale: chargeRelease projectile (speed
    2.6, aoe 3, energy casing, lengthScale 2.6); summons
    fullPower to evil_spikes at caster; damage 780 +
    0.5 Strength, hellfire.
    Summon def evil_spikes (bind false): cloud (radius
    2.6, active 22), ground spikes (6, ring radius 1.1,
    height 1.9) (the design's body-space burst read as a
    tight ground ring; named in the audit), damage 0.
24. Evil R Hell Hunt: cloud anchor entity, follow entity
    (radius 3.5, active 50), chains maxLinks 3,
    insideDamageInterval 8, finale (damageScale 2.2);
    ground cracks; damage 380 + 0.4 Strength per tick,
    hellfire.
25. Devil R Deploy (swarm carrier): swarm block with the
    design's numbers verbatim *: count 10, budget 15000,
    shard 100 + 0.5 Strength, slamRadius 6,
    interceptRadius 12, assignRadius 40,
    drainPercentPerMinute 20, recallCooldown 600,
    starveCooldown 3600, lockout 20; slam damage 750*
    falloffFloor 0.4; terrain spawn crater (radius 5,
    depth 0.7, cap 260).
26. Devil L Prong Shot: projectile (speed 3.0, energy
    casing); damage 750* total (base 750, no stat),
    cooldownTicks 10*, kiCost 60 flat (the design's
    discrete drain flattened to the standard at-fire
    payment; named in the audit). While a swarm is
    deployed, AbilityService routes this slot to target
    marking instead (the swarm module's rule).
27. Sickle L Sorrow Slash: melee (range 4.5, arc 100);
    damage 750* + 3.0* Strength; onHit melting (120)
    plus healFraction 0.15 (Soul Rend), appliesTo melee;
    type spirit.
28. Sickle R Dimensional Rift: cloud anchor target
    (radius 2.5 to 7.5, active 140), vortex (lift 0.4,
    swirl 2.0), insideDamageInterval 10, finale
    (damageScale 1.5, knockback -1.5, strikes 3);
    damage 320 + 0.35 Spirit per tick, void.
29. Ox L Ox King's Strike: rooted chargeRelease (startup
    200, minPower 0.3); wave ring (arc 360, radius 24,
    speed 2.4, height 4); summons fullPower to
    ox_fissures at caster; damage 1000 + 0.6 Strength,
    crush; heavy feedback.
    Summon def ox_fissures (bind false): cloud (radius
    8, active 80), terrain spawn place magma (radius 8,
    cap 160), ground cracks, damage 250 lava per
    insideDamageInterval 10.
30. Ox R Ox Brace / King's Slam: chargeRelease (startup
    300 max hold); aura during hold (radius 12, pull
    -2.4 (repulsion), selfEffect ox_brace); criticalWindow
    280 to 300 ticks (14 to 15 s) with powerScale 1.5 and
    summonAbility ox_crags; wave ring on release (arc
    360, radius 9, speed 1.8); damage 800 + 0.55
    Strength, crush.
    Summon def ox_crags (bind false): cloud (radius 5,
    active 60), ground spikes (7, radius 4.2, height 3),
    ground cracks, terrain spawn place magma (radius 5,
    cap 60), insideDamageInterval 10, damage 300 lava.
31. Power Pole L Wind Spin: wave (speed 0.9, radius 25
    reach read as a long slow band, height 6, arc 35),
    deflect (scale 2.2, claim false), knockback
    (out 0.8, up 0.5); damage 380 + 0.4 Strength,
    tempest.
    (Reach note: the wave travels; total reach is radius
    plus travel over active. active 24 gives about 21
    blocks of travel plus the 25 radius box; the audit
    names the approximation.)
32. Power Pole R Extension: beam (length 30, radius
    0.35, damageInterval 100 (one application), form
    solid, endBurstRadius 3); onHit pole_stun (30);
    damage 620 + 0.5 Strength, crush; active 10 (a jab,
    not a channel).
33. Whis L Time Shatter melee: melee (range 4.5);
    damage 350* + 2.0* Dexterity, temporal; summons
    meleeHit to time_shatter at hit.
    Summon def time_shatter (bind false): cloud (radius
    2.2, active 36), freezeTicks 36, damage 0, glass fx.
34. Whis R Temporal Rift: cloud (radius 12, active
    170), freezeTicks 60 (the design's 3 s), rewindTicks
    100 (the design's 5 s); atmosphere pale blue;
    damage 0; time_stop sound.
35. Bansho L Gale Force: wave (speed 2.0, radius 6,
    height 3.5, arc 60), knockback (out 1.4, up 1.1);
    summons carrierHit to bansho_cyclone at hit (once);
    damage 520 + 0.45 Strength, tempest.
    Summon def bansho_cyclone (bind false): cloud
    (radius 6, active 120), vortex (lift 1.2, swirl
    2.4), insideDamageInterval 12, damage 260 +
    0.3 Strength, tempest; heavy shard_chip debris
    recipes (the 64-piece field read).
36. Bansho R Tempest Barrage: projectile volley (count
    5, spreadDegrees 36), speed 2.2, energy casing;
    summons fire to bansho_shockwave at caster; damage
    340 + 0.35 Dexterity per bolt, tempest;
    cooldownTicks 40 (the design's 2 s).
    Summon def bansho_shockwave (bind false): wave
    (arc 55, radius 5, speed 1.6, height 2.5), damage
    300 + 0.3 Strength, tempest.
37. Blaster L Barrage: autoFire (intervalTicks 3,
    heatPerShot 1, overchargeHeat 12, overchargePower
    2.5, overchargeAoeScale 2); projectile (speed 3.2,
    aoe 0 (2.2 when overcharged via the scale), energy
    casing); kiCost 12 per shot; damage 210 + 0.25
    Dexterity, ki.
38. Blaster R Erasure Cannon: chargeRelease (startup
    30); beam (length 48, radius 1.1, damageInterval 4,
    form energy, endBurstRadius 3), active 20; damage
    700 + 0.5 Spirit per tick, ki; beam_fire sound.
39. Lava L Lava Cleave: wave (speed 1.5, radius 6.5,
    height 3.2, arc 80), terrain trail place lava
    (cap 120); damage 640 + 0.5 Strength, lava;
    sneakVariant volcano.
    Summon-variant def volcano (bind false): cloud at
    caster (radius 10, active 120), terrain field place
    lava (cap 200), insideDamageInterval 8, finale
    (damageScale 1.5), ground cracks and spikes;
    damage 380 lava per tick.
40. Lava R Lava Eruption: cloud at caster (radius 6.5,
    active 200), terrain field place lava (cap 260)
    with crust basalt; insideDamageInterval 10;
    damage 300 + 0.35 Strength, lava; sneakVariant
    meteor.
    Variant def meteor (bind false): projectile (speed
    1.4, gravityScale 3, aoe 6, lit casing), terrain
    impact crater (radius 5, depth 1, cap 300); damage
    900 + 0.55 Strength, lava.
41. Spike Flail L Wide Swing: melee (range 4,
    arcDegrees 150); damage 560 + 0.5 Strength, crush;
    (the design's facing lock is a one-line root on a
    6-tick startup: rooted true, startup 6.)
42. Spike Flail R Spin Disc: chargeRelease (startup 32,
    the design's spin-up); wave ring (arc 360, radius
    9, speed 2.0, height 2); damage 480 + 0.45 Strength
    scaled by power, crush; charge_tick orbiting shard
    recipes read as the spinning ball.
43. War Mace L Crater Smash: wave ring (arc 360, radius
    4.5, speed 1.3, height 2.5), selfLaunch 1.1,
    terrain spawn crater (radius 4, depth 0.8, cap
    220); damage 720 + 0.55 Strength, crush;
    airVariant falling_smash.
    Variant def falling_smash (bind false): move slam;
    wave ring (radius 5.5, speed 1.4), selfLaunch 1.3,
    terrain spawn crater (radius 5, depth 1, cap 300);
    damage 880 + 0.6 Strength, crush.
44. War Mace R Rising Blow: melee (range 4.5) plus move
    launch (height 1.6); damage 540 + 0.5 Strength,
    crush. (The chain into the falling smash is the
    airVariant on the left slot, which is where the
    design's chain physically happens.)

Unbound summon and variant defs: spirit_impale,
evil_spikes, time_shatter, ox_fissures, ox_crags,
bansho_cyclone, bansho_shockwave, volcano, meteor,
falling_smash: 10 extra files. Total ability files: 44
bound attacks minus the 2 existing proofs (their slots are
in the 44) plus 10 unbound = 52 files at the end (42 new
bound/variant files to author plus edits to the 2 proofs
only if a rating fix needs it).

## 4. Build order inside the phase

1. Foundation A (common code): schema, registry bind flag,
   loader cross-validation, damage on-hit path, terrain,
   rewind service, service runtime (spawn extraction, move,
   aura, channels, charges, combo, critical, variants,
   summons, melee, swarm toggle), carrier extensions
   (cloud modules, wave modules, beam entity, swarm
   entities), entities registration, swing hook.
2. Sweeps plus a schema harness after Foundation A (the
   harness proves every new block accepts its valid form
   and rejects malformed forms naming the field).
3. Foundation B (client): SpikeCluster kit builder,
   geometry.ground parsing is Foundation A (common);
   wave and cloud renderer ground draws, cloud ChainRun
   links, beam renderer, swarm renderers, renderer
   registrations, tooltip callback.
4. Sweeps again.
5. Authoring owners per weapon group: JSON files only,
   each owner also reports its weapons' picture ratings.
6. Full-roster harness: every file parses through the real
   loader path, binds (or is intentionally unbound), and
   every cross-reference resolves inside the file set.
   Particle worst-case estimate for a stacked scene.
7. Picture pass consolidation, central data fixes,
   REBUILD-FINAL-AUDIT.md, commits, push.

## 5. Known honest limits (carried into the final audit)

- Rewind is position-only (M11). Freeze is velocity zeroing
  plus the stasis effect; mob AI timers are not paused.
- The Temporal Rift's drawn glass ring from the old build
  has no kit equivalent in this phase's scope; the rift
  reads through its dome, fog, and recipes.
- Heaven Splitter's wall is a wave-anchored TornWall draw;
  it lasts while the wave lives (40 ticks), not a lingering
  physical wall.
- Evil Spear body spikes read as a tight ground ring
  (spikes erupt around the caster's position), not
  free-floating spearheads.
- Spirit Impale's six swords have no model in the kit;
  the pillar reads through dome, disc, and burst.
- Grand Cyclone's spin shards are recipes, not thrown
  projectile entities; the aura's deflect and pull are
  real.
- Power Pole Wind Spin reach is wave travel plus radius,
  an approximation of the 25-block cone.
- Prong shot Ki pricing is flattened to at-fire payment.
- Hit-stop stays deferred on Phase 5 terms; no attack in
  this roster was judged to demand the pose-only
  compromise.
