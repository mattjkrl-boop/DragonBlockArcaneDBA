# Entity and Effects Visual Pass, After (Nov 1, 2026)

Direct follow-up to the four before audits (entity-visual-audit-group-a.md
through group-d.md, 73 entity and renderer pairs). Carter asked for the
entities the weapons use to be changed to look better, and for the effects
too. This pass worked the ranked worst lists from all four groups plus the
honorable mentions worth fixing. Verification: dual javac audit sweeps on the
26.2 and 26.3 classpaths after each group, zero new error kinds against the
pre-pass baseline both times. No Gradle build (waived by Carter).

## The pattern the audits found

The meshes were already strong almost everywhere. The recurring defects were
endings and flatness, in five families:

1. Broken fade formula: `1 - age / (lifetime + age)` can never reach 0 when
   lifetime is the fixed total (it only decrements server side), stranding
   four effects at about half alpha when they vanished.
2. No fade envelope at all on long lived effects (storm 11 s, tornado up to
   10 s, sky cracks 5 s, dimensional slash up to 5 s): full alpha pops at
   both ends.
3. Lifetime mismatches between renderer and entity: effects kept damaging
   while invisible (Grand Crescent Wave 30 ticks, Hollow Afterimage 20).
4. Frozen or single plane renders: the Dabura finisher blades had zero age
   animation; two clash bursts and the Power Pole impact lived in one plane
   and vanished edge-on.
5. Almost no entity emitted its own particles.

## Fixes by group

### Group A (Katana, Z Sword, Saber, Brave Sword, Grand Sword)
- DelayedLaunchRenderer: was a literal no-op, the only FLAT in the mod. It
  now draws a telegraph: a violet gravity ring that contracts from 5 blocks
  to 1.2 over the 40 tick delay, plus a tighter gold ring and 4 rune ticks
  that spin faster and pulse harder as the launch nears.
- BraveSlashRenderer: the extracted age was ignored. The slash now grows
  0.85 to 1.15 and fades quadratically over its 40 tick flight.
- ZShockwaveRenderer: no lifetime fade existed. Fade in over 3 ticks, out
  quadratically over the last 25 of its 80 ticks, threaded through all 8
  top level draw calls.
- GrandCrescentWaveRenderer: hardcoded maxLife 50 corrected to the entity's
  real 80, ending up to 30 ticks of invisible piercing damage.
- HollowAfterimageRenderer: maxLife 80 corrected to 100. The body is now
  tinted by the caster's synced SKIN_COLOR (extracted for months, never
  used) and a hair cap box is drawn in the synced HAIR_COLOR, so the ghost
  actually resembles the player who left it.
- SaberVoidTearRenderer: the dark halo quad passed alpha 0 on all vertices
  (dead geometry); it now renders at fade x 0.55 so the bright slit has its
  dark rim back.
- SaberDodgeSparkRenderer and GrandClashSparkRenderer: single plane bursts
  are submitted a second time rotated 90 degrees, so they read from any
  angle.
- KatanaChargeRenderer, ZChargeRenderer, BraveChargeRenderer: spawn fade-in
  over the first 4 ticks on the shared base alpha.
- GrandBladeShardEntity: the header promised golden shatter particles that
  were never emitted; trip and direct-hit discards now burst CRIT plus DBA
  SPARK.

### Group B (Azure, Dabura, Curse, Hollow's Edge, Dimensional)
- DarknessShatterRenderer and CurseGroundShatterRenderer: broken fade
  formula replaced with progress over the fixed total (40 and 35), reaching
  exactly 0 at discard.
- AzureStormRenderer: fade envelope over the 220 tick life (in 20, out 40)
  threaded through ground bands, both domes, rain shafts, the eye column,
  and the lightning arcs. AzureStormEntity now emits ambient WIND sheets
  every 4 ticks and SPARK at the eye every 7 (its particle imports sat
  unused before).
- AzureTornadoRenderer: same envelope shape (in 10, out 20) against the
  synced giant flag (60 or 200 ticks), threaded through rings, funnel,
  ribbons, shards, and crown. AzureTornadoEntity emits WIND around the
  funnel every 3 ticks.
- SkyCracksRenderer: the envelope is folded into the existing pulse (in 10,
  out 20 over the 100 tick life), so the 960 block sky quad no longer snaps
  on and off.
- DarknessBladeRenderer: the finisher blades were the only fully frozen
  render in the group. They now roll around their fall axis at 18 degrees
  per tick and fade in over 3 ticks. DarknessBladeEntity streams VOID_SMOKE
  and SPARK while falling.
- DimensionalSlashRenderer: fade in over 4 ticks, out from tick 70 to the
  100 tick cap, threaded through crescents, spurs (helper now takes the
  fade), octahedron, rings, and ribbons.
- DarknessWaveRenderer: fade out over the last 10 of its 40 ticks.
- AzureTempestChannelRenderer: spawn fade-in over 5 ticks on the base alpha.
- VoidRiftEntity and VoidRiftRenderer: implosion phase was keyed to total
  age modulo 10, so the collapse started mid-animation depending on when
  release happened. A synced IMPLODE_START_TICK is now set in
  triggerImplosion and both the collapse scale and burst rings phase from
  it.

### Group C (Spirit, Evil Spear, Devil Trident, Sickle, Ox)
- HellHuntImpactRenderer and DevilSlamShockwaveRenderer: same broken fade
  formula, same fix (35 tick totals). The Hell Hunt step impact and the
  trident deploy slam no longer pop out at half alpha.
- OxShockwaveRenderer: the 0.2 alpha floor is gone. The renderer recomputes
  the entity's real maxTicks from the synced charge and sub-wave flag and
  fades fully out across the final 30 percent.
- OxFissureEntity orientation: OxShockwaveEntity now sets each fissure's
  yaw radially from the slam center, OxFissureRenderer aligns the trench to
  it (all 6 fissures used to point the same way), and the renderer adds 2
  jittered branch cracks off the main line.
- TridentShardRenderer: the existing pulse now scales the whole blade
  (0.93 to 1.0 breathing) instead of only the fuller alpha, plus a spawn
  scale-in from half size over 5 ticks.
- SorrowSlashRenderer (flying wave): grows 0.7 to 1.3, rolls slowly in its
  tilt direction, and fades in over 3 ticks. The impact burst variant is
  unchanged.
- SpiritCannonBeamRenderer: spawn fade-in over 3 ticks on the prism layers,
  helices, and rings.
- EvilSpearProjectileRenderer: the spear body dissolves over the final 5
  ticks of its 40 tick flight (trail and wake rings keep their own cycles).
- DimensionalRiftEntity: ambient SPARK and VOID_SMOKE around the core every
  8 ticks (it previously emitted particles only on a burst hit).

### Group D (Power Pole, Whis, Bansho, Blaster)
- TimeShatterRenderer: the flat dial and spoke layer is submitted under the
  conjugated camera orientation (a true billboard), so the shatter reads
  from every angle; the 3D shards and core stay in world space.
- BanshoWindProjectileRenderer: age envelope (in 2, out over the last 5 of
  50) on every part. BanshoWindProjectileEntity now fires a WIND puff and a
  soft burst sound on the range-cap timeout path, matching the hit path's
  feedback.
- ErasureChargeOrbRenderer: base alpha is charge driven (0.10 at empty to
  1.0 at full) and the core octahedron starts faint, so the orb
  materializes as it charges instead of appearing at 0.75 alpha.
- BanshoShockwaveEntity and Renderer: projectile impacts set a new synced
  ORIENT_TO_IMPACT flag, and radial mode then applies the cone mode's
  yaw/pitch rotation, so hits on flying targets burst around the hit
  instead of drawing a flat ground ring in mid-air. Ground melee bursts are
  unchanged.
- PowerPoleImpactRenderer: split into planar (rings, starburst) and volume
  (dome, shards) geometry; the planar set is submitted twice, the second
  copy rotated 90 degrees, giving the burst volume from any viewpoint.

## Deliberately not changed

- Event driven constructs with no fixed lifetime (Valor Field dome, both
  channel domains, the charge auras of Evil Spear and Ox, Curse Chain) can
  fade in, and the charge family now does, but a client side fade out
  cannot know the release tick in advance. Their endings are masked by the
  release effects themselves (storm spawn, slam, field burst). Left as is
  rather than faking it.
- KiBlastEntity (the shared trident laser bolt) pops on hit, but the hit is
  the effect: it explodes with sound at the same tick. Left as is.
- No renderer was switched to the additive glow type. The translucent
  emissive look is consistent across all 73 pairs; mixing blend modes per
  effect would need an in-game look pass to judge, which this sandbox
  cannot do.
