# Bespoke Report: Dimensional Sword

Phase 2 conversion of the Dimensional Sword to the Bespoke Pass standard
(audits/BESPOKE-STANDARD.md), on top of Phase 1 commit 2ef870b. Files owned
and touched: entity/DimensionalSlashEntity.java,
entity/DimensionalWarpRiftEntity.java,
client/render/DimensionalSlashRenderer.java,
client/render/DimensionalWarpRiftRenderer.java. item/DimensionalSwordItem.java
needed no edits (already fully compliant, see counts). Not committed.

## Before and after counts

| Category | Before | After |
|---|---|---|
| Local regular-geometry draw helpers (both renderers) | 16 (6 slash, 10 rift) | 0 |
| KiRenderHelper primitive draw calls (drawColoredBox and kin) | 0 (helpers were hand-rolled; the audit's 7 hits were kiRenderType, kiAdditiveType and constant reads, which stay legal) | 0 |
| Vanilla ParticleTypes / DustParticleOptions references | 0 (already on DbaParticles and LayeredParticles after Phase 1) | 0 |
| Vanilla SoundEvents references | 11 (4 slash entity, 7 rift entity) | 0 |
| Vanilla ability damage source chains (damageSources playerAttack / mobAttack / mobProjectile / magic) | 2 (slash onHitEntity, rift doBurst) | 0 |
| BespokeGeometry builder calls | 0 | 25 (12 slash renderer, 13 rift renderer) |

Every category is at zero. The one retained vanilla element is the standard's
own exception: the plain sword melee swing keeps the vanilla player attack
source. The item never names a damage source for melee (vanilla default),
and that is unchanged.

## Geometry: what was rebuilt on what

Seeds: both entities gained a synced SEED data field, rolled once at spawn
from the level random and read by the renderers (getSeed). The rift renderer
previously derived its seed from the entity UUID; it now uses the synced
field like every other converted weapon. All shapes below are deterministic
per entity and identical on every client.

DimensionalSlashRenderer (was: hand-rolled crescent strips, ribbon strips,
concentric octahedron rings):
- bladeArc x3 for the crescent (outer violet 0.65/0.05/0.95, core magenta
  1.0/0.10/0.35, white tear 1/1/1) plus a fourth bladeArc on the additive
  render type for the halo. The builder's flat XZ arc is posed into the
  blade's XY plane (Z +90 then X -90) and flattened (scale 1.22, 0.50) to
  keep the old wide, shallow crescent proportions.
- displacedDome x3 for the singularity: magenta shell and white core domes
  facing forward, void black dome facing back, replacing the octahedron.
- brokenVortexBand x2 as the counter-rotating accretion rings around the
  singularity, at the old rings' speeds (age x 22 and age x -30 degrees,
  passed as the twist).
- shardFan x2 for the tip spurs, stood up into the blade plane.
- tornRibbon x3 for the trailing fissures, posed to stream backward (-Z),
  the center one longest, as before.
- crackStar x1 in the blade plane as the lingering tear mark, added to the
  kept LayeredRender.drawVortexBands linger block.

DimensionalWarpRiftRenderer (was: 8 trench boxes, 8 rim slabs, 24 orbiting
pyramids, hand-rolled tear quads, tilted octahedron and rings):
- crackStar x1 for the fissure trenches (same violet, radius 1.02 x rift
  radius).
- shardFan x1, near-black violet, for the levered rim slabs.
- brokenVortexBand x2 in the base pose: a low ground band turning at the
  old astrolabe ring's speed, and a tall orbit band (y 0.5 to 2.3) whose
  radius follows the old orbiting pyramids' spiral exactly (origin 0.85 to
  0.35 of radius, destination 0.30 to 0.95, eased by progress), twisting at
  age x 0.12.
- tornRibbon x8 for the tear itself: the old four layers (violet corona,
  magenta slit, black horizon, white seam, same colors, widths and alphas)
  drawn on two crossed planes, ribbons running upward from the corona base
  with lengths scaled so each layer keeps its old span relative to tearH.
- tornRibbon x1 for the tear wake, streaming backward and pinching with
  the shut factor, same guard conditions as before.
- displacedDome x3 for the singularity pearl (magenta front, white core
  offset forward, void back), replacing the octahedron.
- brokenVortexBand x2 tilted plus and minus 38 degrees around the tear as
  the gyroscopic rings, twisting at the old rings' speeds (age x 24 and
  age x -19 degrees).
- Kept as Phase 1 rebuilt them: LayeredRender.drawAnchorGlow and
  LayeredRender.drawVortexBands, same arguments.

The rift renderer's ease() shaping helper stays; it is lifecycle math, not
geometry. All lifecycle numbers are untouched: slash envelope (age, 100,
0.05, 0.30), linger 14 ticks with the 1.0 + 0.22 swell, first-person
scale-in, plus/minus 35 degree tilt, spin wobble; rift envelope (age, life,
0.20, 0.35), 6 tick entrance ease, destination 0.6 + 0.7 and origin
1.0 - 0.4 expansion, pulse 0.88 + 0.12 sin(age x 0.5), tear open and shut
shaping from progress 0.55, all tearH and tearW formulas.

## Particles

No changes needed and none made. Both entities already emitted only
DbaParticles types (KI_MOTE, VOID_SMOKE, SPARK) and LayeredParticles dust
(which Phase 1 moved onto DbaDustOptions). Verified zero vanilla particle
references across all five files after the conversion.

## Sounds (same call sites, volumes, pitches)

DimensionalSlashEntity:
- ILLUSIONER_CAST_SPELL (1.8, 1.3) to DbaSounds.CAST_DARK
- RESPAWN_ANCHOR_SET_SPAWN (1.2, 1.6) to DbaSounds.CHARGE_DARK
- RESPAWN_ANCHOR_DEPLETE on entity hit (1.8, 1.2) to DbaSounds.RELEASE_DARK
- RESPAWN_ANCHOR_DEPLETE on block scar (1.4, 1.5) to DbaSounds.RELEASE_DARK

DimensionalWarpRiftEntity:
- ENDERMAN_TELEPORT (2.0, 0.7) to DbaSounds.TELEPORT_WHOOSH (guide: teleport)
- RESPAWN_ANCHOR_SET_SPAWN (1.8, 0.6) to DbaSounds.CHARGE_DARK
- PORTAL_TRAVEL (1.5, 1.4) to DbaSounds.RIFT_OPEN (guide: portal to
  RIFT_OPEN / RIFT_AMBIENT; RIFT_AMBIENT is a looping ambient event, so the
  one-shot travel cue takes RIFT_OPEN; the charge cue above takes
  CHARGE_DARK, keeping the spawn stack at four distinct events as before)
- WARDEN_SONIC_BOOM (2.2, 0.8) to DbaSounds.BOOM_SONIC (guide: sonic boom)
- RESPAWN_ANCHOR_DEPLETE (2.0, 0.6) to DbaSounds.RELEASE_DARK
- GENERIC_EXPLODE (1.5, 0.5) to DbaSounds.BOOM_BIG (guide: explosion)
- ENDERMAN_TELEPORT at warp landing (1.2, 0.8) to DbaSounds.TELEPORT_WHOOSH
- Already in the bank and kept: DARK_PULSE, WIND_GUST.

CAST_DARK, CHARGE_DARK and RELEASE_DARK for the anchor and illusioner cues
follow the same dark-magic mapping the audit recorded for the Phase 1
baseline files (illusioner cast to CAST_DARK in DimensionalRiftEntity).

## Damage

- Slash hit: DbaDamage.voidDmg(this, owner). The (direct, attacker) factory
  form anchors on the slash with the owner as attacker, and falls back to
  the direct entity when the owner is gone, covering the old magic()
  fallback case.
- Both rift bursts (origin and destination run the same doBurst):
  DbaDamage.voidDmg(this, owner), same fallback behavior.
- Damage numbers unchanged: slash 750 + Strength x 3, origin burst
  400 + Strength x 2, destination burst 900 + Strength x 3, all still
  through DbaEffectsConfig.capDamage. Melting applications, friction and
  air drag modifiers, debris counts, scar sizes and durations, the warp
  validation order (landing validated first, then Ki drain, then spawn),
  the teleport timing on the origin burst tick, RIFTED 60 ticks: all
  untouched, verified by diff review of the two entity files.

## Verification

Targeted javac (JDK 25, -sourcepath closure over the five owned files):
- cp262.txt: zero errors in the five owned files. The closure now also
  reaches shared registry files through DbaDamage, and shows 4 errors in
  item/DbaItems.java (line 535) and inventory/DbaMenus.java (line 16).
  Compiling those same files from a pristine 2ef870b worktree produces the
  identical 4 errors, so they are pre-existing baseline errors, not from
  this conversion.
- cp263.txt: zero errors in the five owned files. The closure shows 11
  errors, all in shared files (DbaItems, DbaMenus, DbaHostileEntity,
  FriezaSoldierEntity, NamekianWarriorEntity, PatrolRobotEntity,
  LavaSwordGround). A pristine 2ef870b worktree compile of those files
  produces the identical 11 errors. Pre-existing baseline, unchanged.
- Baseline for comparison: the identical five-file targeted compile run
  before the conversion (at 2ef870b) also had zero errors in the owned
  files. Its closure showed 4 errors on 262 (the same DbaItems and
  DbaMenus registry errors) and 16 on 263, all in shared files (the same
  registry and hostile-entity kinds, plus BanshoCycloneEntity and
  HollowsEdgeItem, other weapons' files that were in flight in the tree
  at the time). No new error kinds were introduced by this conversion.
- Compile check remains waived per Carter's standing decision (Gradle
  cannot run in this VM); this is the javac audit, not a Gradle pass.

## Honest worse check

Two judgment calls to flag, neither a clear regression:
- The crescent is now a true blade arc: its band is thicker at the belly
  and its edges are nicked and noise-profiled instead of smooth. It reads
  more like a torn crescent and less like the old smooth ribbon; the width
  and height were matched to the old proportions with the pose flattening.
  If Carter prefers the slimmer old profile, that is a change request
  against bladeArc's fixed inner radius (0.5 x radius), not something the
  renderer can tune: noted below.
- The rift's orbiting pyramids and rim slabs are now flat bespoke shards
  and bands (shardFan, brokenVortexBand). They keep the old motion, radii
  and colors but lose the pyramids' 3D bulk. The toolkit has no bespoke 3D
  shard volume builder; noted as a change request below.

No part of the conversion was reverted; nothing reads as broken or emptier
than the spectacle version. The tear itself is arguably stronger: ribbons
fray and decay along their length where the old quads were flat.

## Change requests (toolkit gaps, not worked around)

1. BespokeGeometry: a 3D shard or debris volume builder (displaced
   tetra/prism fan with height) would let the rift's orbiting shards and
   rim slabs keep their 3D bulk instead of going flat.
2. BespokeGeometry.bladeArc: the inner radius is fixed at 0.5 x radius,
   which sets a minimum crescent band thickness. A band-thickness parameter
   would let thin, rapier-style slashes match their old profiles exactly.
