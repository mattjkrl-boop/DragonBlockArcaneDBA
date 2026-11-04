# Bespoke Report: Power Pole

Phase 2 conversion of the Power Pole to audits/BESPOKE-STANDARD.md, on top of
the spectacle pass state. Files owned and edited: item/PowerPoleItem.java,
entity/PowerPoleWhirlwindEntity.java, client/render/PowerPoleWhirlwindRenderer.java,
client/render/PowerPoleExtensionRenderer.java,
client/render/PowerPoleImpactRenderer.java. The extension and impact entities
needed no edits (see Seeds). No SpiralImpactEntity exists in the tree; the
only spiral entity is KiSpiralBeamEntity, which is not a Power Pole entity
and was not touched. No shared file was edited. Not committed.

## Before and after counts (the 7 owned files)

| Category | Before | After |
| --- | --- | --- |
| Vanilla SoundEvents call sites | 5 (item 4, whirlwind entity 1), plus 2 imports | 0 |
| Vanilla ParticleTypes references | 0 | 0 |
| DustParticleOptions references | 0 | 0 |
| KiRenderHelper primitive draws (box, cylinder, disc, sphere, helix) | 0 | 0 |
| Local geometry draw helpers | 16 (whirlwind 3, extension 7, impact 6) | 0 |
| Hand-rolled segment loops emitting vertices | all 3 renderers | 0 |
| Ability damage via vanilla damageSources() | 2 (both playerAttack) | 0 |
| Client-side random in renderers | 1 (impact, seeded from entity UUID) | 0 |

Notes on the zeros that were already zero: the spectacle pass had already
moved this weapon's particles onto DbaParticles (the entities emit WIND,
KI_MOTE, and SPARK, and the item's ground burst goes through the shared
LayeredParticles, which Phase 1 converted), and the renderers never called
the KiRenderHelper shape primitives. The geometry debt was entirely local:
equal-segment rings, fans, cones, domes, and shafts built by per-renderer
helper methods. Three orchestration helpers remain (extension
drawShaftRibbons and drawWakeRibbons, impact drawPlanarBurst). They emit no
vertices themselves; they only pose the stack and call BespokeGeometry,
which is the standard's one shared foundation rule, not a private builder.

## Geometry: builders used

All seeds are hashes of synced entity state, as the standard allows (a
synced seed field or a hash of synced state), computed in
extractRenderState from values already synced today: caster id, range,
cone angle, max length, impact scale, yaw, and pitch. No entity data
fields were added, no client random, no wall clock. The same seed draws
the same shape on every client and holds still frame to frame.

Whirlwind renderer:
- Cone sheaths (cyan-white core, emerald pressure): stacked
  BespokeGeometry.brokenVortexBand calls in an axial frame (pose turned
  so builder height runs down the cone axis), 6 bands per sheath, radii,
  twists, and the 3-stop gradients evaluated at band middles from the
  same stops the old 12-tier loops used.
- Helical ribbons: narrow brokenVortexBand streaks at the old helix
  radius factor (0.85), alternating cyan and emerald, twist racing ahead
  down the cone as the old ribbons did.
- Crescent cutters: BespokeGeometry.bladeArc, an emerald arc with a
  bright core arc inside it, placed by the unchanged orbit math (same
  blade count 10, same length, fade, and alpha formulas).
- Shock rings and the mouth ring: short brokenVortexBand calls with the
  same travel timing, radii, and alphas.
- Origin disc: LayeredRender.drawAnchorGlow (a glowFan), same radius and
  alpha.
- The LayeredRender.drawVortexBands block at the cone mouth is untouched.

Extension renderer:
- Shaft: crossed BespokeGeometry.tornRibbon pairs in the old 3-stop
  crimson gradient bands, plus a thinner bright core pair. Ribbon width
  is set 1.15x the old shaft diameter because tornRibbon jitters its
  edges inward; the core pair is 1.3x the old core diameter for the same
  reason.
- Pale gold sheath: one brokenVortexBand at the old sheath radius and
  alpha.
- Gold caps and collars: brokenVortexBand collars at the old cap radius
  and z ranges. The two thin bevel rings are subsumed by the collars.
- Dragon coils: 4 stepped brokenVortexBand slices at the old coil
  radius, twist advancing slice by slice with the old age term.
- Dome tip: BespokeGeometry.displacedDome with a brokenVortexBand lip
  ring at the old radius, color, and alpha.
- Mach cones: BespokeGeometry.plume stacks at the same slide timing,
  radius shrink, and alpha curve.
- Tip wake: crossed tornRibbon pair, same length, color, and alpha as
  the old wake cone.
- Tip glint: two BespokeGeometry.shardFan bursts (wide gold, tight
  bright core) with the old pulse radius and alpha.

Impact renderer:
- Shock rings: brokenVortexBand bands in the hit plane (gold band, bright
  rim band, and the crimson annulus as two bands), same radii from the
  unchanged ease-out expansion, same alphas.
- Compression dome: BespokeGeometry.displacedDome at the old dome radius
  and color, alpha at the old latitude average.
- Starburst: two shardFan bursts (gold, bright core) replacing the 8
  hand-built needles, same radius envelope and alpha.
- Shards: 16 small shardFan bursts. The scatter draws come from
  BespokeGeometry.Rng on the state seed, in the same order and ranges as
  the old draws (yaw, pitch, speed, size, gold-or-crimson), with the
  same flight easing, distance, shrink, and fade.
- The planar burst is still drawn twice, once facing the strike and once
  turned 90 degrees, for side volume.

The lifecycle math is untouched everywhere: the whirlwind envelope still
mirrors PowerPoleWhirlwindEntity.currentEnvelope phases (build 10, decay
24, swell), the extension ease-out, hold, and retract still reach alpha
exactly 0 at the tick 12 discard, and the impact still uses the entity's
maxLifetime with the smoothstep fade and the age over 8 ease-out.

## Particles

Zero before, zero after, explained above. No emission code changed.

## Sounds

All 5 vanilla call sites swapped per the DbaSounds guide, positions,
volumes, and pitches unchanged:
- Spin spawn (item) and sustained gust (whirlwind entity):
  PLAYER_ATTACK_SWEEP to DbaSounds.FLIGHT_WIND, per the brief. The
  bespoke WIND_GUST layered under both is unchanged.
- Poke throw: TRIDENT_THROW.value() to DbaSounds.TRIDENT_THROW.
- Poke impact: ANVIL_LAND to DbaSounds.SLAM_THUD, HEAVY_CORE_HIT to
  DbaSounds.HEAVY_IMPACT.

## Damage

The audit separates the two ability hits, so the split is applied:
- Whirlwind spin hits: DbaDamage.tempest(player). The wind family for a
  wind move; the 750 / max(1, distance / 3) formula, the per-target
  10 tick gate, and the active-window rule (age at least BUILD_TICKS and
  envelope above 0.25) are unchanged.
- Poke hit: DbaDamage.crush(player). The 750 flat damage, POLE_STUN
  application, cooldown, and stamina costs are unchanged.
- Plain melee: PowerPoleItem.hurtEnemy stays empty; the base item attack
  keeps the vanilla player attack path, the standard's honest exception.

## Targeted javac verification

Targeted javac of the 7 owned files plus their transitive dependencies
(JDK 25, -Xmaxerrs 10000) against both audit classpaths:
- 26.2 classpath: 4 errors, ZERO in Power Pole files. The 4 are in
  item/DbaItems.java (CreativeModeTab.Output protected access) and
  inventory/DbaMenus.java (MenuType constructor and MenuSupplier private
  access), shared registry files pulled in transitively.
- 26.3 classpath: 11 errors, ZERO in Power Pole files. The 11 are the
  same DbaItems and DbaMenus kinds plus known 26.3 baseline kinds in
  entity/DbaHostileEntity.java, entity/FriezaSoldierEntity.java,
  entity/NamekianWarriorEntity.java, entity/PatrolRobotEntity.java
  (removed LivingEntity fields and swing signature), and
  entity/LavaSwordGround.java (BlockState.blocksMotion, the pre-existing
  26.3 kind recorded in audits/SPECTACLE-FINAL-AUDIT.md).
None of the erroring files were edited by this pass and none of the
errors reference Power Pole code, so the conversion introduces zero new
error kinds on either classpath. This is the dual javac audit method,
not a Gradle build (the compile check remains waived per Carter's
direction; the sandbox cannot run the daemon).

## Honest worse check

Two honest trade-offs, both accepted rather than reverted:
- The extension shaft is now crossed torn ribbons instead of a solid
  18-sided prism. It reads as a striking pole with a ripped energy edge
  rather than a machined cylinder. This is the standard's intended look
  (the toolkit has no prism builder), the silhouette and gradient bands
  are preserved, and the width compensation above keeps its presence.
  If Carter prefers the solid read, that is the shaft builder change
  request below.
- The impact dome is a full displaced hemisphere where the old dome was
  squashed to half height. It reads slightly taller; the radius, color,
  and fade timing are the same.

Nothing else reads worse: the whirlwind keeps its three-layer cone, the
crescents keep their orbit and fade, the impact keeps its ring radii
progression and double-plane volume.

## Shared change requests

1. A straight prism or shaft builder in BespokeGeometry (seeded, slight
   facet jitter) if a solid machined shaft read is wanted for the pole
   and similar physical weapon parts. The crossed tornRibbon build is
   compliant today; this is a look upgrade, not a gap that blocks the
   standard.
2. A squashed dome option (height scale parameter) on displacedDome
   would let flat compression domes keep their exact old profile.
