# Bespoke Pass Phase 2 Report: Blaster Gun

Scope: item/BlasterGunItem.java, item/BlasterAmmoItem.java (audit only),
entity/BlasterBoltEntity.java, entity/ErasureCannonBeamEntity.java,
entity/ErasureChargeOrbEntity.java, and the three renderers
(BlasterBoltRenderer, ErasureCannonBeamRenderer, ErasureChargeOrbRenderer).
Base: commit 2ef870b (Bespoke Pass Phase 1). No commit made by this agent.
All spectacle-pass numbers, heat rules, cooldowns, and mechanics are
unchanged; the diffs to the item and the entities touch only sound ids,
particle ids, damage source construction, and the new synced seed field.

## Before and after counts

| Category | Before | After |
| --- | --- | --- |
| Vanilla ParticleTypes references (owned files) | 2 | 0 |
| Vanilla SoundEvents references (owned files) | 10 | 0 |
| Vanilla damageSources() constructions | 3 | 0 |
| KiRenderHelper primitive draws (renderers) | 0 | 0 |
| Local regular-geometry helpers (renderers) | 20 | 0 |
| DustParticleOptions references | 0 | 0 |

Before detail: BlasterGunItem had 6 SoundEvents lines (DISPENSER_FAIL x3,
GENERIC_EXPLODE, BLAZE_SHOOT, BEACON_AMBIENT). BlasterBoltEntity had
ParticleTypes.EXPLOSION x1, SoundEvents x2 (FIRE_EXTINGUISH,
GENERIC_EXPLODE), and 1 vanilla DamageSource ternary. ErasureCannonBeamEntity
had ParticleTypes.EXPLOSION_EMITTER x1, SoundEvents x2
(LIGHTNING_BOLT_THUNDER, GENERIC_EXPLODE), and 2 vanilla DamageSource
ternaries. ErasureChargeOrbEntity was already at zero on every category.
The renderers called no KiRenderHelper primitives, but all three were
built from local regular-geometry helpers, which rule 1 bans: the beam
renderer had drawFlowBeam (12-sided banded tubes), a uniform 4-helix
ribbon loop, drawPlaneRing, drawMuzzleEmitter, drawTerminusImpact
(16-segment dome and fins), drawLash, lashSpan, and drawQuad; the orb
renderer had rotVertex, drawRotatingOctahedron, drawGeodesicSphere
(latitude-longitude), drawPlaneRing, drawAxisRing, drawTriangle,
drawLash, lashSpan, and drawQuad; the bolt renderer had drawPlasmaBolt
(12-sided cone, body, tapered tail), drawLash, lashSpan, drawTriangle,
and drawQuad. All deleted.

## Rule 1: geometry

All weapon geometry now comes from client/render/ki/BespokeGeometry.
Each of the three entities gained a synced INT SEED (same pattern as
BoltEntity: defined in defineSynchedData, set from level random in the
spawn constructor, read through getSeed()), and each render state
carries it. Every builder call is seeded from that value XOR a per-part
constant, so the silhouette is frame-stable and identical on every
client. Ribbon and fan planes are posed by rotating the pose stack once
per submit (spin on the beam or flight axis, then the builder's local
axes onto the target frame).

ErasureCannonBeamRenderer:
- Beam body: tornRibbon in 7 crossed planes. White core in 2 planes
  (width 0.55 x baseRadius, the old 0.28 core radius as a diameter),
  cyan mid in 3 planes (width 1.45 x baseRadius, the old 0.72 tube),
  dark outer in 2 planes (width 1.72 x baseRadius, the old 0.86 tube).
  Mid and outer layers take a slight seeded bend (0.12, 0.20) so the
  beam reads as torn energy. Envelope is FxEnvelope, same as before.
- Aperture stations: glowFan irregular fans at the 0.25, 0.50, 0.75
  fractions with the old ring radius falloff (2.3 minus frac x 0.7) and
  the old alternating spin; the far fan still drops when distScale is
  below 0.55.
- Muzzle: glowFan emitter plus a shardFan of compression blades.
- Terminus: displacedDome (radius 1.2 + charge x 0.8, impact pulse as
  before) with a white core dome at 0.35 radius inside it, a shardFan
  splash, and a glowFan splash. The old lash fins are gone.

BlasterBoltRenderer:
- Body: tornRibbon in 3 crossed planes, 3 layers each (soft sheath at
  1.22 x radius and alpha 0.22, body at alpha 0.88, white core at 0.38 x
  radius), spinning at the old body rate of age x 20 degrees. Lengths
  derive from the unchanged head and tail lengths, so the bolt keeps
  its old extent and the torn taper replaces the cone-plus-cylinder
  profile.
- Nose: shardFan flare across the flight axis where the cone was.
- Fins: BespokeGeometry.branch ribbons at the old 4 fin positions,
  swept from the nose out to the old fin span (radius x 2.3) and back
  to the old rear anchor, edge color outside plus a thin white core
  pass, spinning at the old fin rate of age x 12 degrees.
- Tail: a counter-spinning shardFan (age x -30, the old core rate).
- Heat palette, pulse, first-person scale, and the impact collapse
  math (impactT over 5 ticks, cubic ease, boltDie smoothstep, radius
  x (1 - ease x 0.62), head x (1 - ease x 0.70), tail x (1 - ease
  x 0.20)) are copied unchanged and feed the ribbon lengths and the
  shared die envelope.

ErasureChargeOrbRenderer:
- ORB CORE CHOICE: the rotating octahedron (a regular solid) is
  replaced by a small displacedDome pair, one cap up and one flipped,
  forming an irregular noise-lumpy energy ball that still turns with
  age at the old core rate (age x 36), wrapped in a shardFan cluster
  in three planes as a jagged crystalline cage. A displaced dome was
  chosen over a pure shard cluster for the core itself because the
  dome pair keeps a readable bright center mass, the role the
  octahedron's lit faces played; the shards alone read as debris.
- Shells: displacedDome pairs at the old shell radii (0.82, 0.48, and
  the wispy 1.18 outer at the standard 0.18 band alpha),
  counter-rotating per layer at the old shell rates. The 3-stop color
  journey survives as layer colors: mid shell whitens toward red-hot
  past charge 0.75, inner shell pale cyan, outer shell cyan.
- Gyroscope rings: brokenVortexBand in the same three planes (muzzle
  XY, horizontal XZ, side ZY), same radii (ringR, 0.94, 0.88 of it,
  tightening with charge as before), spin carried by the band twist
  at the old rates (22, -16, 27 degrees per tick of age).
- Intake spikes: BespokeGeometry.branch, 8 spikes on the old spiral
  (radius 2.3 - ease x 0.95 of orbRadius, speed 30 + charge x 60),
  each with a thin white core pass, replacing drawLash.

## Rule 2: particles

- BlasterBoltEntity overcharged detonation: ParticleTypes.EXPLOSION
  to DbaParticles.BLAST.
- ErasureCannonBeamEntity endpoint detonation:
  ParticleTypes.EXPLOSION_EMITTER to DbaParticles.BLAST.
- Everything else was already DbaParticles (SPARK, KI_MOTE, EMBER) and
  LayeredParticles dust, which returns DbaDustOptions after Phase 1.

## Rule 3: sounds (volumes and pitches unchanged everywhere)

- BlasterGunItem out-of-ammo and fizzle paths: DISPENSER_FAIL x3 to
  DbaSounds.FIZZLE.
- BlasterGunItem overcharge vent: GENERIC_EXPLODE to
  DbaSounds.RELEASE_BLAST (the bank's energy-release entry; the small
  DbaImpactFx.heavy at the same beat is unchanged).
- BlasterGunItem barrage shot: BLAZE_SHOOT to DbaSounds.FIRE_SHOT
  (guide entry for fire shots).
- BlasterGunItem charge hum: BEACON_AMBIENT to DbaSounds.BEAM_CHARGE
  (guide entry for beams); the layered DbaSounds.ABILITY_CHARGE call
  beside it is untouched.
- BlasterBoltEntity block impact: FIRE_EXTINGUISH to
  DbaSounds.EXTINGUISH. Overcharged detonation: GENERIC_EXPLODE to
  DbaSounds.BOOM_BIG.
- ErasureCannonBeamEntity fire beat: LIGHTNING_BOLT_THUNDER to
  DbaSounds.BEAM_FIRE (guide entry for beams). The vanilla thunder was
  storm ambience borrowed by a weapon; storm thunder belongs to the
  atmosphere system under rule 5. DbaSounds.LIGHTNING_ZAP at the same
  beat is untouched. Endpoint detonation: GENERIC_EXPLODE to
  DbaSounds.BOOM_BIG.

## Rule 4: damage

- BlasterBoltEntity hit damage and ErasureCannonBeamEntity pulse and
  endpoint damage now use DbaDamage.ki(this, owner as LivingEntity),
  replacing the playerAttack / mobProjectile / generic ternaries. The
  ki damage type JSON mirrors the player attack shape per the Phase 1
  standard, and the ability entity stays the direct entity, so
  attribution and knockback behavior carry over.
- The gun's plain melee hit is untouched and stays the vanilla player
  attack source, by design.

## BlasterAmmoItem audit

Plain Item subclass, 3-line constructor, zero references of any audited
kind before or after. No change made; git confirms the file is
unmodified.

## Verification

Targeted javac (JDK 25, -sourcepath over both source sets) of the 8
owned files against both audit classpaths:
- cp262.txt (26.2): zero errors in owned files.
- cp263.txt (26.3): zero errors in owned files.
The only errors in either run sit in non-owned dependency files pulled
in through the sourcepath, and every one is a pre-existing baseline
kind: DbaItems (CreativeModeTab Output protected access), DbaMenus
(MenuType private access) on both versions, plus the known 26.3
API-removal kinds in DbaHostileEntity, FriezaSoldierEntity,
NamekianWarriorEntity, PatrolRobotEntity, and LavaSwordGround (the
recorded BlockState.blocksMotion kind) on 26.3. None of those files
were touched by this agent. The full-tree sweep comparison against the
57902e1 baseline counts (132 kinds on 26.2, 141 on 26.3) belongs to
the parent merge pass.
Grep verification on the owned files after conversion: ParticleTypes
0, SoundEvents 0, damageSources() 0, KiRenderHelper primitive draws 0,
local regular helpers 0, DustParticleOptions 0. No em dashes in any
added line (the 4 em dashes in the main-source class headers pre-date
this pass and were left as Carter's rule requires).

## Honest worse check

- Beam body: crossed torn ribbons replace round tubes, so at a frozen
  frame the cross-section reads as torn sheets rather than a cylinder.
  That is the bespoke look the standard asks for, widths were matched
  to the old tube diameters, and the core plus mid plus outer layering
  keeps the volume. Not worse overall; different by design. The old
  helical strand layer has no direct counterpart; its motion is
  carried by the bent mid ribbons and the spinning aperture fans.
- Bolt nose: the old smooth needle cone becomes a ribbon start covered
  by a shard flare, so the first tenth of a block reads wider and
  flatter than the old point. In flight at bolt speed this is a torn
  flare, not a defect, but it is the one silhouette change worth
  naming. Nothing was reverted.
- Orb: the octahedron's hard facets and the geodesic shells' banded
  journey are gone by rule; the dome shells are smoother than the old
  faceted spheres at rest, and the shard cage carries the jaggedness
  instead. The color journey is preserved per layer.
- Sound identity: the fire beat loses real thunder. BEAM_FIRE plus the
  retained LIGHTNING_ZAP keeps a heavy beam boom with an electric
  edge; if Carter wants thunder back it must come through the storm
  system, not a weapon file.
- In-game look and feel are unverified by construction, as with the
  whole pass: Gradle cannot run in this VM, the compile check is
  waived per Carter's standing decision, and Carter's PC build is the
  first real look. No build pass is claimed.

## Shared change requests

None required. Two toolkit notes for the parent, neither a blocker:
- glowFan and brokenVortexBand take no envelope parameter, so the
  envelope was baked into alpha at those call sites (beam aperture
  fans, orb rings). An envelope overload would tidy this across
  weapons but changes nothing visually.
- branch() draws a single color per call, so two-tone fins and spikes
  (edge color plus white core) need two calls per segment. Works fine;
  noted for other agents doing lash-style geometry.
