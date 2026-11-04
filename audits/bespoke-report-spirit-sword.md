# Bespoke Pass Phase 2 Report: Spirit Sword

Scope: item/SpiritSwordItem.java, entity/SpiritCannonBeamEntity.java,
entity/SpiritCleaveEntity.java, entity/SpiritImpaleEntity.java, and the
three renderers (SpiritCannonBeamRenderer, SpiritCleaveRenderer,
SpiritImpaleRenderer). Shared files were read only. Nothing committed.

## Counts, before and after

| Category | Before | After |
| --- | --- | --- |
| Local geometry draw helpers in the 3 renderers | 22 (beam 9, cleave 5 geometry plus v/strip plumbing, impale 8) | 0 |
| KiRenderHelper primitive calls (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) | 0 | 0 |
| Vanilla ParticleTypes references in the 7 files | 0 | 0 |
| Vanilla DustParticleOptions references | 0 | 0 |
| Vanilla SoundEvents references | 5 (item 4, cleave 1) | 0 |
| Vanilla damageSources() call sites | 6 (item 4, cleave 2 branches of one ternary) | 0 |
| DbaDamage.spirit call sites | 0 | 5 (item 4, cleave 1) |
| java.util.Random in renderers | 1 (impale) | 0 |
| BespokeGeometry builder call sites | 0 | beam 8 code sites (6 ribbons via one pose adapter helper, plus glowFan, shardFan, displacedDome calls), cleave 8, impale 20 |

Particles were already fully compliant before this pass: every emission in
the four common files goes through DbaParticles types or
LayeredParticles.dust with DbaDustOptions, so rule 2 needed no edits, only
verification by grep on both sides of the pass.

## Geometry (rule 1)

All geometry now comes from BespokeGeometry, seeded from entity synced
state: the beam and cleave entities gained a synced SEED field
(EntityDataSerializers.LONG, set from the entity UUID at spawn, exposed
as getSeed()), and the impale renderer keeps its existing seed, the
entity UUID most significant bits. Per-element transforms are done with
PoseStack.Pose.copy() plus translate and rotate inside the existing
single submit per pass; Pose.rotate post-multiplies via JOML
Matrix4f.rotate, verified against the 26.2 and 26.3 jars, so the axis
mappings below are the ones the code actually produces.

SpiritCleaveRenderer (bladeArc for the cleave, as assigned):
- Blade body: bladeArc rotated to stand in the YZ plane centered on the
  flight direction, radius = the spectacle length times 0.76, sweep 80
  degrees, warm white-gold (1.0, 0.95, 0.72), plus a nested white core arc
  at 0.84 radius and 64 degrees.
- Wake: three tornRibbons aimed back down the flight path with negative
  bend for the old downward sag. Widths 0.68, 0.48, 0.32 (the old wake
  widths doubled), lengths 1.50, 1.00, 0.55, gold, cyan, and white-gold
  layers at the old alpha shares.
- Guard horns: one gold shardFan across the blade base in the XY plane.
- Tip flash: one shardFan at the crescent front, envelope scaled by the
  unchanged tipFlash ease.
- Halo pass: one bladeArc at 1.06 radius in cyan on the additive type.
- Kept identical: FxEnvelope envelope (0.08 build, 0.30 decay) on the
  synced life, the 3 tick open ease, the 2.25 length curve, distScale.

SpiritCannonBeamRenderer (tornRibbon flow layers, glowFan apertures, as
assigned):
- Body: six tornRibbons through one private pose adapter (beamRibbon)
  that only positions the shared builder: translate to zStart, roll
  around the beam axis, turn so the ribbon runs down +Z. White core pair
  at the old 0.26 diameter rolling at the old core speed (age times 5),
  cyan flow pair at the old 0.52 diameter rolling at age times minus 8
  with opposed bends, sheath pair at the old 0.84 diameter in
  (0.05, 0.70, 1) rolling at age times 3. Alphas 0.95, 0.65, 0.30 times
  the shared envelope, the spectacle layer values.
- Orbital nodes: placement math unchanged (count via FxEnvelope.segments,
  alternating cyan and gold, nodePulse phasing, ringR pulse). Each node is
  now a glowFan aperture spun at the old ring spin, with a shardFan
  reticle inside counter spun at minus 1.4 times, radius 0.62 of the node
  radius, brightened color, the spectacle alphas (0.50 and 0.55).
- Muzzle: glowFan aperture radius 0.70 times pulse in cyan at z 0.15,
  shardFan seal radius 0.48 at z 0.20 counter spun, alphas 0.85 and 0.90.
- Terminus: displacedDome radius 0.85 times pulse bellied back toward the
  caster, gold shardFan splash star radius 1.1 times pulse on the contact
  plane, white glowFan flash radius 0.5 times pulse. Alphas as before.
- Glow pass: two beamRibbon echoes at the old prism glow alphas 0.22 and
  0.10.
- Kept identical: fadeIn smoothstep over 5 ticks, synced FADE envelope,
  drawLength retract formula, first person anchor (sideSign times 0.40,
  minus 0.46, 0.35) with zStart 0.25 and fpScale 0.5, the pulse factor.

SpiritImpaleRenderer (shardFan for the crystals, as assigned):
- Ground array: cyan crackStar rotated at age times 22 (the old outer
  ring speed), gold glowFan at the old mid radius share, white crackStar
  counter rotated at minus age times 30 with a white glowFan heart. The
  old rings rotated at 22, minus 30, and 15 degrees per tick; the white
  heart fan is static under its crack star, the one speed not carried.
- Pillar: three brokenVortexBands (white core at 0.42 radius share and
  1.05 height share, cyan shells) with twist riding age times 0.25, plus
  four tornRibbon strands orbiting at radius 1.25 times the pillar radius
  at the old helix phase speed, alternating cyan and gold.
- Swords: placement math verbatim (ring radius, drop, center bob, tips,
  vibration now from BespokeGeometry.Rng on the same seed, consumed in
  the same order). Each sword is posed hilt to tip by yaw and pitch:
  tornRibbon grip, tornRibbon blade from the guard point (0.18 of length)
  with a crossed tornRibbon web rotated 90 degrees around the blade axis,
  gold shardFan guard across the blade, shardFan pommel, and two
  tornRibbon tendrils (cyan plus white core) trailing back and rising 0.3,
  the old tendril numbers.
- Crystal perimeter: two shardFans, cyan at 0.98 of baseRadius and white
  at 0.55, replacing the 8 placed spikes. Radius rides baseRadius, so the
  fans erupt with the slam on the same curve.
- Glow pass: LayeredRender.drawVortexBands and drawAnchorGlow kept
  verbatim (already compliant); a crackStar and gold glowFan bloom echo
  replaces the old ring and cylinder halo copies.
- Kept identical: envelope (0.12 build, 0.28 decay over the 40 tick
  life), slam over 4 ticks, pulse 0.85 plus 0.15 sine, all radii shares.

## Particles (rule 2)

Zero vanilla ParticleTypes and zero DustParticleOptions before and after,
verified by grep across all 7 files. No particle edits were needed; all
emission was already DbaParticles or LayeredParticles.dust.

## Sounds (rule 3)

All call sites, volumes, and pitches unchanged. Swaps, per the DbaSounds
guide:
- Item hurtEnemy: SoundEvents.TRIDENT_THUNDER to DbaSounds.THUNDER_CRACK
  (guide: thunder to THUNDER_CRACK), and SoundEvents.BEACON_ACTIVATE to
  DbaSounds.CHARGE_RISE (guide: BEACON_ACTIVATE to CHARGE_RISE). The
  layered DbaSounds.ABILITY_CHARGE at the same spot was already custom.
- Cannon beam spawn (new call site, the beam_charge the task names):
  DbaSounds.BEAM_CHARGE at the caster, volume 1.0, pitch 1.0, played once
  when the beam entity is created in onUseTick.
- Cannon per-pulse caster resonance: SoundEvents.BEACON_ACTIVATE to
  DbaSounds.BEAM_FIRE (the beam_fire the task names), volume 1.0, pitch
  1.6 as before.
- Cannon per-target impact: SoundEvents.BEACON_POWER_SELECT to
  DbaSounds.CHARGE_FULL (guide: beacon select milestone to CHARGE_FULL),
  volume 1.2, pitch 1.5 as before.
- Cleave block impact: SoundEvents.GLASS_BREAK to DbaSounds.GLASS_CRACK
  (guide: glass to GLASS_CRACK), volume 0.8, pitch 1.35 as before.
- performSpiritSwing already used DbaSounds.BLADE_SHING; unchanged.

## Damage (rule 4), stated exactly

- Contact melee hit: unchanged. It is the item attack pipeline itself
  (the 500 damage attribute hit) and keeps the vanilla player attack
  source, the standard's honest exception. No code in hurtEnemy touches
  that hit.
- Melee percent bonus (was damageSources().magic(), 2 percent of target
  max HP): now DbaDamage.spirit(serverPlayer, serverPlayer). The helper
  supports the direct-plus-attacker form; with no separate direct entity
  in a melee swing, the attacker fills both slots, which is the same
  attribution shape the neighboring playerAttack source had.
- Melee Strength bonus (was damageSources().playerAttack(serverPlayer),
  Strength times 2.0): now DbaDamage.spirit(serverPlayer, serverPlayer).
- Cannon main pulse (was damageSources().playerAttack(player), amount
  (100 plus Spirit times 0.75) times ramp): now
  DbaDamage.spirit(beam, player), the beam entity as direct and the
  caster as attacker, as the task directed.
- Cannon percent bonus (was damageSources().magic(), 1 percent of target
  max HP per pulse, no ramp multiplier, exactly as the spectacle code had
  it): now DbaDamage.spirit(beam, player), same direct-plus-attacker
  shape as the main pulse.
- Cleave projectile hit (was playerAttack(owner) when the owner is a
  player, else magic()): now DbaDamage.spirit(this, owner) in both cases.
  The percent (2 percent of max HP) is still folded into the same single
  hurtServer amount (this.damage plus percent) as before; only the source
  changed.
- One behavior note, stated plainly: the two percent hits used to be
  unattributed magic. They now attribute to the attacker through the
  spirit type, which is what the type's player death message variant
  exists for. Amounts, caps (DbaEffectsConfig.capDamage), ramp, pulse
  cadence (every 5 ticks), and the wall occlusion ClipContext are all
  untouched.

## Verification

Targeted javac of the 7 owned files with sourcepath over both source
sets, the spectacle pass method:
- 26.2 classpath (cp262.txt): zero errors in the owned files. The only
  errors in the run are 4 lines in DbaItems.java and DbaMenus.java,
  confirmed present in the saved pre-pass sweep outputs (errors262b and
  errors262c), so they are baseline, not from this pass.
- 26.3 classpath (cp263.txt): zero errors in the owned files. The only
  errors are 11 lines in DbaItems.java, DbaMenus.java,
  DbaHostileEntity.java, FriezaSoldierEntity.java,
  NamekianWarriorEntity.java, PatrolRobotEntity.java, and
  LavaSwordGround.java, likewise confirmed in the saved baseline sweeps.
- Compile status is the javac audit only, as always in this VM; no
  Gradle build ran and none is claimed.

## Honest worse check

- The cleave's silhouette genuinely changed. bladeArc is the assigned
  builder, and it makes the projectile a standing crescent about as tall
  as the old straight blade was long, not a tapered sword profile. If it
  reads worse in game, the fallback inside this same renderer is a large
  radius, small sweep bladeArc (a nearly straight segment); nothing else
  would need to change.
- The beam lost the literal corkscrew: the four helical drill ribbons are
  replaced by crossed torn ribbons whose flow reads as seeded wander plus
  roll. The old traveling brightness band (drawPrismBeamFlow) has no
  equivalent in the ribbon builder, so flow is carried by shape and
  rotation instead of a moving highlight.
- Small ornaments were dropped where the standard bans their shapes: the
  3 orbiting white bead quads per beam node (the shard reticle carries
  the orbital read), the 4 curved muzzle needles, and the 4 gold terminus
  needles. The impale swords are crossed ribbons rather than faceted 3D
  diamond-section models, so they read flatter edge-on; the pommel is a
  shard star instead of a modeled diamond cap, and the crystal spikes are
  ground-hugging fans instead of tall individual spikes.
- Orientation math was verified against the JOML and Pose bytecode in
  both version jars, not in game. If any single element faces wrong on
  Carter's PC build, it is a one-line sign change in that renderer's
  rotate call; the risk is named here rather than hidden.

## Toolkit change requests

None. PoseStack.Pose.copy() with translate and rotate covered every
per-element transform inside the existing submits, and every needed
shape had a builder. One observation for the coordinating pass, not a
request: tornRibbon has no traveling brightness parameter, so beam flow
bands from the spectacle pass cannot be reproduced exactly; all beam
agents will hit the same tradeoff.
