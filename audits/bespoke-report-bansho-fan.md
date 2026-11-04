# Bespoke Pass Phase 2 Report: Bansho Fan

Scope: item/BanshoFanItem.java, entity/BanshoCycloneEntity.java,
entity/BanshoGaleEntity.java, entity/BanshoShockwaveEntity.java,
entity/BanshoWindProjectileEntity.java, and the four matching renderers.
Base: HEAD 2ef870b. No mechanics, numbers, timings, colors, or envelopes
changed anywhere in this conversion. BanshoShockwaveEntity needed no edits
at all (see below), so the WindChargeMixin constructor contract is
untouched.

## Before and after counts

Vanilla ParticleTypes references: 0 before, 0 after.
The fan was already fully on DbaParticles (WIND, KI_MOTE) and
LayeredParticles after Phase 1, so there was nothing to convert. Verified
by grep over all nine files: zero ParticleTypes references, and zero
KiRenderHelper primitive draws (drawColoredBox, drawCylinder, drawDisc,
drawSphere, drawHelix) before and after.

Vanilla SoundEvents call sites: 9 before, 0 after.
- BanshoFanItem: PLAYER_ATTACK_SWEEP became DbaSounds.SWING_FAN (the melee
  impact slot; it still plays paired with the bespoke SLASH_WHOOSH at the
  same site). WIND_CHARGE_THROW became DbaSounds.SWING_FAN (the barrage
  cast slot; its WIND_GUST pairing was already bespoke).
- BanshoCycloneEntity: ejection WIND_CHARGE_BURST became WIND_GUST; the
  howl pair ELYTRA_FLYING became FLIGHT_WIND and WIND_CHARGE_BURST became
  WIND_GUST.
- BanshoGaleEntity: the wall burst WIND_CHARGE_BURST became WIND_GUST.
- BanshoWindProjectileEntity: timeout WIND_CHARGE_BURST became WIND_GUST;
  impact WIND_CHARGE_BURST became WIND_GUST and ELYTRA_FLYING became
  FLIGHT_WIND.
Volumes and pitches are unchanged at every site, per the DbaSounds swap
guide (wind bursts to WIND_GUST or SWING_FAN, flight to FLIGHT_WIND).

Vanilla damage sources: 4 call sites before, 0 after.
- Cyclone shred: playerAttack / mobAttack / magic ternary became
  DbaDamage.tempest(this, owner).
- Gale hits: playerAttack / magic became DbaDamage.tempest(this, owner).
- Barrage projectiles: playerAttack / mobProjectile / generic became
  DbaDamage.tempest(this, owner).
- Melee bonus hit in BanshoFanItem: converted to DbaDamage.tempest(player).
  Stated plainly per the brief: BESPOKE-STANDARD rule 4 maps ability damage
  layered on a melee hit to the theme family (its own example list names
  bonus hits), so the Strength x 1.5 bonus now uses the tempest type. The
  amount is identical (same capDamage call, same figure). The plain melee
  hit itself stays vanilla player attack exactly as the standard requires;
  it never appeared as a damageSources call (it is the item attack path in
  hurtEnemy). In the three entity conversions the ability entity is the
  direct entity and the owner is the attacker, matching the DbaDamage
  factory contract; kill credit and amounts are unchanged.

Local regular-geometry helpers: 19 before, 0 after.
- CycloneRenderer: drawLash, lashSpan, drawRotatingRing, drawQuad (4).
- GaleRenderer: drawRib, drawArc, drawRibbon (3). fanPoint stays: it is
  path math, not a draw helper.
- ShockwaveRenderer: drawLash, lashSpan, drawRingZ, drawBeveledShockRing,
  drawFlatRing, drawQuad, drawTriangle (7).
- WindProjectileRenderer: drawLash, lashSpan, drawRingZ, drawQuad,
  drawTriangle (5).

Vanilla weather: none before, none after. The cyclone still calls
AtmosphereServer.begin with Kind.RAIN, which after the Phase 1 rework is
the mod's own tracked storm volume with custom rain, never WeatherData.
The stale "real rain" wording in the item header and the cyclone comment
was corrected to "the mod's own rain".

BanshoShockwaveEntity: no changes. It already used only DbaParticles.WIND,
has no sounds and no damage, and both constructors plus setOrientToImpact
are byte-identical, so WindChargeMixin is unaffected.

## Builders used, per renderer

Seeds: every render state now captures a seed from the entity id in
extractRenderState. The id arrives in the spawn packet, so it is synced
state and every client draws the same torn silhouette for the same entity.
Per-element variation XORs small offsets onto that seed. Gale, Shockwave,
and WindProjectile states also capture entity position for
LayeredRender.distScale, matching the cyclone's existing pattern.

CycloneRenderer: LayeredRender.drawVortexBands and drawAnchorGlow kept
(already bespoke after Phase 1, signatures unchanged). New:
brokenVortexBand for the foot suction skirt (twist carries the old ring's
rotation) and for the crown outflow band; glowFan for the pale eye;
branch chains (3 strands x 8 segments through the original helix points)
for the triple helical ribbons, keeping the base-to-top color gradient;
bladeArc (two nested arcs per blade, posed vertical in the tangent plane)
for the 8 orbiting blades. Growth, radius, height, and envelope math
unchanged; the mid band still lands the outer band exactly on the effect
radius.

GaleRenderer: tornRibbon only. Each of the 9 ribs is one posed submit
(rotated so the ribbon runs along the rib's fanned direction) with a wide
faint ribbon and a narrow bright core ribbon. The 3 arcs are each 4
tornRibbon segments chorded along the original fanPoint curve (leading
edge, fold, wake). Open ease, spread, lift, fade, and tip dissolve timing
are the original numbers.

ShockwaveRenderer: cone mode is two plumes rotated onto the shot axis
(outer emerald surge, pale core surge) replacing the 32 segment ruled cone
shells, a shardFan at the mouth for the jade lip, a second shardFan
mid-cone for the forward needles (needleReach falloff curve unchanged),
and a glowFan for the muzzle flare. Radial mode is a glowFan core, two
stacked brokenVortexBands for the expanding wall (emerald base band, pale
crest band at the old crest height), a center plume for burst height, and
6 bladeArc crescents on the original rotating angles. The 14 tick progress
and exit fade envelope are unchanged.

WindProjectileRenderer: two plumes rotated onto the flight axis and spun
at the original 45 degrees per tick form the drill body (emerald outer,
bright spindle); bladeArc pairs (dark edge arc, bright core arc) posed in
the plane across the flight axis for the 4 orbiting scythes on the
original counter-rotation; tornRibbon segments chorded along the original
helix curve (2 strands x 5 segments, yaw then pitch per chord) for the
wrap; two glowFans facing backward for the wake; two branch calls (wide
sheath, white core) for the luminous tip spike. Age envelope and the 0.55
first-person owner scale are unchanged.

## Verification

Targeted javac (JDK 25) of the nine files via sourcepath, on both audit
classpaths (cp262.txt and cp263.txt): all nine files compiled and produced
class files on both. Zero errors in any Bansho file on either classpath.
The only errors reported are pre-existing baseline kinds in shared files
outside this scope, pulled in transitively (26.2: DbaItems CreativeModeTab
protected access, DbaMenus MenuType private access; 26.3: the same plus
DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity,
PatrolRobotEntity, and the known LavaSwordGround BlockState kind).
Full-tree sweeps on both classpaths also showed zero Bansho errors; the
26.3 full sweep currently aborts early on another weapon's in-progress
parse error (an invalid hex literal in DarknessShatterRenderer), so the
targeted compile above is the verification of record for 26.3.
Grep verification over the nine files: SoundEvents 0, ParticleTypes 0,
vanilla damageSources calls 0, local draw helpers 0, KiRenderHelper
primitive draws 0, vanilla weather calls 0.

## Shared change requests

None. The Phase 1 toolkit covered every shape this weapon needed
(brokenVortexBand, tornRibbon, plume, shardFan, glowFan, bladeArc,
branch). No toolkit gap was found.

## Honest worse check

Four trade-offs, none judged worse overall, none reverted:
- Cyclone ribbons: branch chains are visibly more jagged than the old
  smooth ruled ribbons; they read as torn energy strands. The funnel wall
  (the unchanged LayeredRender bands) still carries the smooth storm read,
  and torn is the bespoke house style, so this is a style shift, not a
  loss. Recorded because Carter, not the toolkit, owns that call.
- Shockwave radial crescents: bladeArc lies flat on the ground plane; the
  old crescents rose in a hump up to 0.85 blocks. The new center plume
  carries the burst height instead, but the crescents themselves lost
  their vertical arc. This is the one real silhouette loss in the pass.
- Gale ribs: tornRibbon takes one color per run, so the old continuous
  base-to-tip color gradient per rib is now two flat tones (wide ribbon
  plus bright core). Motion and timing are identical; the gradient is
  coarser.
- Wind projectile drill: the plume body bulges mid-length by its profile
  instead of tapering straight to a point; the branch tip spike restores
  the point read. The drill reads more organic and less machined than
  before.
