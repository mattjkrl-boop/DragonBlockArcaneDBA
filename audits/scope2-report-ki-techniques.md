# Scope 2 Report A1: Ki Techniques

Agent A1, Ki techniques area. Base HEAD ef5ad02. All work is in the 18 owned
files only. No shared file was edited, no commit was made, and no other
agent's area was touched (Lava Sword, Evil Spear and Dread files untouched).

## Before and after counts (owned files only, grep measured)

| Category | Before | After |
| --- | --- | --- |
| Vanilla SoundEvents call sites | 23 | 0 |
| Vanilla damage source lines (damageSources()) | 6 | 0 |
| DustParticleOptions constructions | 4 | 0 |
| KiRenderHelper primitive calls (drawCylinder, drawDisc, drawSphere, drawHelix) | 16 | 0 |
| Local vertex-emitting geometry helpers | 5 | 0 |
| Direct addVertex lines in owned renderers | 16 | 0 |
| Entities with a synced SEED field | 0 | 7 |
| Em dashes in owned files | 5 (pre-existing) | 0 |

Notes on the counts:

- Sound call sites by file: handler 14, KiBarrier 3, KiDiskEntity 4,
  KiBlastEntity 2. The two SoundEvents imports (handler, KiBarrier) were
  removed as now unused.
- Dust constructions by file: KiBarrier 2, KiDiskEntity 2, plus the
  KiDiskEntity DustParticleOptions import, swapped to DbaDustOptions. The
  scope 2 audit's tree-wide count of 5 included one construction in
  LivingEntityMixin, which the foundation pass already converted; in this
  area the true before count is 4 constructions.
- Primitive calls by renderer: beam 2, laser 2, disk 2, explosion 3,
  spiral 3, barrier 4.
- Local helpers removed: KiBlastRenderer's drawLaserBoltBody,
  drawHelicalVortex, drawRibbonSegment, drawResonanceRing, and
  KiBarrierRenderer's drawInvertedSphere. The pose-glue helpers that
  replaced them only copy, translate, and rotate a pose and then call a
  BespokeGeometry builder; they emit no vertices.

## Sounds: vanilla to DbaSounds

Every volume and pitch is byte-identical to the original call. Layered
DbaSounds calls that already existed were left exactly as they were.

KiTechniqueHandler:

- fireBlast: BLAZE_SHOOT (1.2, 1.6) to FIRE_SHOT; GENERIC_EXPLODE (0.6, 1.8) to BOOM_BIG.
- fireBarrage: BLAZE_SHOOT (0.7, jittered pitch) to FIRE_SHOT, per pellet; FIREWORK_ROCKET_BLAST (1.2, 1.7) to BOOM_BIG.
- fireBeam: BEACON_ACTIVATE (1.8, 1.4) to KAMEHAMEHA_CHARGE; WARDEN_SONIC_BOOM (1.5, 1.8) to BOOM_SONIC; GENERIC_EXPLODE (1.2, 0.8) to KAMEHAMEHA_FIRE. This wires both bank Kamehameha events into the beam technique per decision D1; they previously had zero call sites.
- fireDisk: TRIDENT_THROW (1.5, 1.6) to DbaSounds.TRIDENT_THROW; ELYTRA_FLYING (1.2, 1.8) to FLIGHT_WIND.
- fireLaser: BEACON_DEACTIVATE (1.4, 2.0) to ZAP_DARK; GUARDIAN_ATTACK (1.2, 1.8) to LIGHTNING_ZAP.
- fireExplosion: GENERIC_EXPLODE (3.0, 0.6) to BOOM_BIG; WARDEN_SONIC_BOOM (2.5, 0.5) to BOOM_SONIC; RESPAWN_ANCHOR_DEPLETE (2.0, 0.6) to RELEASE_BLAST.

KiBarrier:

- start: BEACON_ACTIVATE (0.9, 1.7) to CHARGE_RISE.
- absorb tick: BEACON_DEACTIVATE (0.45, 1.8) to SHIELD_CLANK.
- drop: BEACON_DEACTIVATE (0.7, 1.2) to FIZZLE.

KiDiskEntity:

- onHitEntity: PLAYER_ATTACK_SWEEP (1.5, 1.5) to SWING_LIGHT; ANVIL_LAND (0.8, 1.8) to SLAM_THUD.
- onHitBlock: GENERIC_EXPLODE (0.8, 1.6) to BOOM_BIG; ITEM_BREAK (1.0, 1.2) to ITEM_SHATTER.

KiBlastEntity explode():

- GENERIC_EXPLODE (0.8, 1.4) to BOOM_BIG; LIGHTNING_BOLT_IMPACT (0.5, 1.6) to LIGHTNING_ZAP.

Judgment calls, flagged honestly:

- Disk sweep went to SWING_LIGHT, not the guide's SLASH_WHOOSH: the very
  next line already layers SLASH_WHOOSH at the identical volume and pitch,
  so the guide mapping would have played one event twice in the same tick.
  SWING_LIGHT keeps the two-layer whoosh texture distinct.
- Laser charge BEACON_DEACTIVATE to ZAP_DARK: a thin dark zap fits the eye
  laser better than the shield or fizzle families.
- Explosion RESPAWN_ANCHOR_DEPLETE to RELEASE_BLAST: the anchor drain was
  the "energy letting go" layer under the two booms.
- Barrier absorb BEACON_DEACTIVATE to SHIELD_CLANK: the absorb tick is a
  shell taking a hit, and drop already had FIZZLE as its semantic in the
  plan, so the two beacon deactivates split into clank (hit) and fizzle
  (fall).

## Damage: vanilla to DbaDamage.ki

All six sites, amounts and capDamage wrapping unchanged:

- Handler beam hit: indirectMagic(player, player) to DbaDamage.ki(player, player).
- Handler laser hit: indirectMagic(player, player) to DbaDamage.ki(player, player).
- Handler explosion area: indirectMagic(player, player) to DbaDamage.ki(player, player).
- Handler explosion self damage: magic() to DbaDamage.ki(player). Same no-attacker shape, now on the ki family instead of generic magic.
- KiDiskEntity hit: indirectMagic(this, getOwner()) to DbaDamage.ki(this, getOwner()).
- KiBlastEntity explode: indirectMagic(this, getOwner()) to DbaDamage.ki(this, getOwner()).

No new damage family was created, per decision D1. Every damage number,
cooldown, Ki cost, radius, and lifetime in the area is unchanged; only the
DamageSource objects changed.

## Dust: DustParticleOptions to DbaDustOptions

Same colors and scales, same counts, spreads, and speeds:

- KiBarrier start burst: (color & 0xFFFFFF, 1.3f).
- KiBarrier absorb burst: (shell color & 0xFFFFFF, 1.1f).
- KiDiskEntity trail: (getColor(), 0.7f).
- KiDiskEntity block burst: (getColor(), 2.0f).

DbaDustOptions renders through DbaParticles.THEME_DUST, the mod's own
themed dust, instead of the vanilla dust particle.

## Synced seeds

No Ki entity had a synced SEED field. Each of the seven visual entities
now has one, following the BoltEntity pattern: an INT EntityDataAccessor
named SEED, defined in defineSynchedData with default 0, a getSeed()
getter, and a random value set at spawn.

- KiBeamEntity, KiLaserEntity, KiSpiralBeamEntity: set in the
  (Level, owner, length, color) spawn constructor from
  level.getRandom().nextInt().
- KiBarrierEntity: set in its (Level, ServerPlayer, color, usedPercent)
  spawn constructor.
- KiBlastEntity, KiDiskEntity, KiExplosionEntity: these are only ever
  constructed through the (EntityType, Level) constructor and configured
  through setters by the handler, so the seed is set in that constructor.

Constructor signatures are unchanged everywhere, so all existing callers
(handler, Whis Staff style weapon code, registrations) compile as before.
No NBT was added: Ki entities persist nothing today, and that behavior is
unchanged.

## Renderers rebuilt on BespokeGeometry

Each render state gained a seed field extracted from entity.getSeed(), and
each submit computes LayeredRender.distScale from the camera state.
Lifecycle alpha math (beam fade over 20 ticks, explosion ease and fade,
barrier breathe) is preserved exactly and passed through as builder alpha
or envelope.

- KiBeamRenderer: tornRibbon. Crossed ribbon pairs in a pose-glue helper
  (roll around the beam axis, then turn the ribbon run onto +Z). Sheath
  width 0.44 x scale (the old sheath cylinder diameter) in the theme color
  at alpha 0.45, rolls 0 and 90; core width 0.14 x scale (old core
  diameter) in white at 0.95, rolls 45 and 135.
- KiLaserRenderer: tornRibbon, same helper shape. Sheath width
  0.12 x scale at 0.4, core width 0.04 x scale at full alpha.
- KiSpiralBeamRenderer: tornRibbon core (crossed pair, white, width
  0.16 x scale) plus brokenVortexBand. The twin helixes are now a stack of
  band segments along the beam: segment count from FxEnvelope.segments,
  each segment rolled by the old phase (age x 0.35 rad) plus its share of
  the old 3.5 total turns, at the old helix radius 0.28 x scale and the
  old 0.85 alpha.
- KiDiskRenderer: bladeArc plus shardFan. Face and rim are each three
  full blade arcs offset by 120 degrees, so one arc's pinched seam is
  always covered by another arc's swell. Face radius 0.61 x scale in the
  theme color at 0.9, rim radius 0.72 x scale in white at full alpha
  (radii reduced because a blade arc's outer edge reaches past its radius
  parameter). A shardFan at 0.85 x scale bursts teeth past the rim. The
  spin pose (24 degrees per tick) is unchanged.
- KiExplosionRenderer: displacedDome plus plume plus brokenVortexBand.
  Shell is a dome pair (base pose plus an XP 180 flip) at the old shell
  alpha 0.55, core a dome pair at 0.38 x radius in white at 0.9, a plume
  (radius 0.5 x R, height 0.9 x R, alpha 0.35) rising through the center,
  and the ground ring as a flat broken band at the old animated ring
  radius and the old ring alpha curve. The ease, fade, and early return
  are untouched.
- KiBlastRenderer: tornRibbon plus shardFan plus brokenVortexBand, all
  four local helpers deleted. Sheath is three crossed ribbons at rolls 0,
  60, 120, full width at the nose (old radius x 2) at 0.85, spanning nose
  to tail so the ribbon taper becomes the tail fade. Core is three crossed
  ribbons in warm white (1, 1, 0.95) at full alpha over the old core
  length. A shardFan flares at 0.8 x head length. The helical wake is two
  band segments winding down the tail with the old age x 0.5 rad rotation
  plus 1.5 turns, radius and alpha tapering as the old helix did. The
  resonance ring is one flat broken band at the old outer ring radius and
  0.75 alpha. The pulse (1 + 0.12 sin(age x 0.9)) and the spin pose are
  unchanged.
- KiBarrierRenderer: displacedDome plus brokenVortexBand,
  drawInvertedSphere deleted. Shell is a dome pair at the old shell alpha
  with the old breathe radius; the builders emit double-sided faces, so
  the wearer still sees the shell from inside without the separately wound
  copy. Glow is a dome pair at 0.62 x radius. The flat rim is a broken
  band at 0.97 x radius in white at the old rim alpha; the vertical rim is
  the same band stood into the spin pose (1.7 x spin) in the glow color.
  The 0.95 translate and spin rates are unchanged.

## Change requests for shared files

None. KiRenderHelper, BespokeGeometry, and LayeredRender already expose
everything this area needed; no toolkit gap was found. KiTechnique.java
and KiTechniqueType.java contained no vanilla references at all and were
not part of the conversion (they only received the em dash cleanup below).

## Em dash cleanup

Five pre-existing em dash occurrences sat in owned files (two player
message literals in the handler, three comment lines in KiTechnique.java
and KiTechniqueType.java). All five are now plain hyphens. No owned file
contains an em dash.

## Verification

Targeted javac audit only; no Gradle build was run or claimed.

- Command shape: javac with the audit classpath file, sourcepath covering
  src/main/java and src/client/java, the 18 owned files as roots.
- 26.2 (cp262.txt): zero errors naming any owned file. The only errors in
  the closure are 4 pre-existing baseline lines in non-owned files
  (DbaItems CreativeModeTab Output access, DbaMenus MenuType access).
- 26.3 (cp263.txt): zero errors naming any owned file. The only errors
  are 10 lines of the same pre-existing baseline kinds (DbaItems, DbaMenus,
  and the mob entity kinds in DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity), all outside this area.
- Class files were produced for all 18 owned files on both classpaths,
  confirming the owned files themselves compiled.
- Post-edit greps over the 18 owned files: SoundEvents 0, damageSources()
  0, DustParticleOptions 0, KiRenderHelper primitive calls 0, addVertex 0,
  local trig loops 0, em dashes 0.

## Honest worse check

Nothing mechanical changed: damage, costs, cooldowns, radii, lifetimes,
and hit logic are identical, so nothing plays worse. Visually, four reads
changed character and are stated plainly rather than hidden:

- Beam and laser tips now fade: tornRibbon tapers width and alpha toward
  the far end where the old cylinders were uniform to a hard end. It reads
  as energy dissipating, but it is a silhouette change.
- The spiral's two distinct thin helix strands merge into one continuous
  broken band sheath. It is fuller and more bespoke, but the literal
  double-helix read is gone.
- The disk loses its clean center hole: bladeArc's inner edge sits at
  half its radius parameter, so the face now starts near 0.31 x scale
  instead of 0.12 x scale, and the rim is three overlapping arcs with
  seeded nicks rather than one perfect annulus.
- The barrier loses the old double-wall shimmer: the separately wound
  inner shell at 0.985 x radius is replaced by the builders' double-sided
  faces on one displaced surface. From inside it is still fully visible;
  the faint second layer is gone.
- The blast loses its faceted diamond body and the explicit hot-white
  nose cone gradient; crossed torn ribbons carry the nose instead, and
  the resonance ring and explosion ground ring are broken, jittered bands
  rather than clean annuli.

No regression was found that would justify reverting any file: every
replacement keeps the original size, color, alpha envelope, and motion,
and the seeded bespoke read is the point of the pass.
