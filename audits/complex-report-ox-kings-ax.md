# Complex Effects Redo, Phase 2 Report: Ox King's Ax

Owner slot: Ox King's Ax (theme: ground dust, stone debris, shock vapor, embers).
Base: HEAD 9178eac. Files edited, all under
`src/client/java/com/dragonblockarcanedba/client/render/`:
KingsSlamRenderer.java, OxChargeRenderer.java, OxFissureRenderer.java,
OxShockwaveRenderer.java, OxStanceAuraRenderer.java. No other files
touched. FissureStunEffect and OxBraceEffect were not edited; they belong
to the Status Effects slot in this pass. No commits made.

Every effect keeps its existing material emission exactly as the filler
layer: same presets, colors, rates, budgets, sources, envelopes, growth
curves, and lifetimes as HEAD. Pieces are seeded from the entity UUID
seed already carried in each render state, the same seed the emission
uses. Peak alphas on all shader pieces are 0.88 to 0.95; solid pieces go
through submitSolid, energy pieces through submitEnergy. No
drawColoredBox, drawCylinder, drawDisc, drawSphere, or drawHelix call
exists in any owned file (grep verified, zero hits).

## KingsSlam (class L, two builders, full disc)

- Pieces layer: fracturePlates through submitSolid (seed salt 0x9A7E,
  radius follows the existing shock growth curve up to the entity radius,
  lift 0.42, gradient from the slam hot color, 0xFF8C1F normal or
  0xFFD24A flawless, at the center to dark stone 0x4A3826 at the rim,
  alpha 0.95, envelope is the existing fade). crackFan through
  submitEnergy (seed salt 0xC4A9, radius 0.94 of the disc radius, hot
  0xFFE9B0 at the center to the slam hot color at the tips, alpha 0.95).
  Estimated worst case vertex total is about 1,300 of the 6,000 class L
  budget: plates are capped by the builder at 10 plates and the crack fan
  at 11 cracks. The entity's server side DebrisSpawner field is
  untouched, as required.
- Complex particle layer: SHARD_CHIP, large. One burst of 48 at impact
  (class L burst budget 60) fired once per emission build, plus a
  continuous chip emitter across the disc at 30 per second, budget 44
  (continuous cap 40 per second).
- Filler: unchanged (dust, vapor, plasma, ember, block chunks, flawless
  geysers).

## OxCharge (class M)

- Pieces layer: tendrilFan through submitEnergy with
  ComplexTextures.TENDRIL. Three clusters sit on the charge ring
  (radius and rotation follow the existing charge state) and each fans
  two strands inward toward the brace point above the center, dust brown
  0xA0825F at the root to plasma orange 0xFF7A1A at the tip, width
  0.11 plus 0.10 times charge, alpha 0.90. Six tendrils at the toolkit
  segment count is about 2,304 vertices worst case, inside the 2,500
  class M budget, and it is the only geometry in this effect.
- Complex particle layer: SHARD_CHIP on the existing gathering source,
  so chips are pulled inward with the plasma on the same inward velocity
  field, 26 per second, budget 40.
- Filler: unchanged (plasma, vapor, dust, ember above 25 percent charge,
  block chunks).

## OxFissure (class M)

- Pieces layer: the pose is rotated by the fissure yaw with the same
  rotation the bespoke build used, so local +Z runs along the trench.
  crackFan through submitEnergy is laid twice, centered 0.95 blocks
  either side of the origin along the line, radius 1.35 each, hot
  0xFFD98A to plasma orange 0xFF6A12, alpha 0.95, so the glowing web runs
  the length of the 4.2 block trench as its core instead of a radial star
  at one point. fracturePlates through submitSolid at the origin, radius
  1.55, lift 0.26, stone gradient 0x8A6A44 to 0x3A2C1E, alpha 0.95.
  Estimated worst case vertex total is about 2,050 of the 2,500 class M
  budget (two crack fans at up to about 790 vertices each, plates about
  480).
- Complex particle layer: SHARD_CHIP sampled along the existing crack
  line source, 24 per second, budget 36.
- Filler: unchanged (two plasma streams, dust, ember, block chunks, all
  on the crack line).

## OxShockwave (class L, two builders)

- Pieces layer: fracturePlates through submitSolid at the origin
  (radius 2.6 plus 1.8 times charge, scaled by the sub-wave factor, lift
  0.30 plus 0.28 times charge, gradient 0xFF8C1F to dark stone 0x4A3826,
  alpha 0.95). crackFan through submitEnergy spreading with the wave
  (radius 0.85 of the live wave radius, minimum 1.2, hot 0xFFE9B0 to
  0xFF8C1F, alpha 0.95). The geometry envelope is the existing end fade
  times the crest factor, so the pieces rise and settle with the wave.
  Estimated worst case vertex total is about 1,300 of the 6,000 class L
  budget. The entity's server side debris is untouched.
- Complex particle layer: SHARD_CHIP. One burst of 40 at the origin as
  the wave leaves it (class L burst budget 60), plus a continuous chip
  emitter on the existing crest ring source at 30 per second, budget 44.
- Filler: unchanged (dust, vapor, plasma, ember, block chunks on the
  crest).

## OxStanceAura (class M)

- Pieces layer: tendrilFan through submitEnergy with
  ComplexTextures.TENDRIL: five dust strands rising from the caster,
  spread 34 degrees around up, length 3.6 times the existing pulse,
  width 0.17, dust brown 0xA0825F at the root to the stance body color at
  the tip (0xFF7A1A normal, 0xFFEB59 at peak, matching the existing
  plasma colors), alpha 0.88. About 1,920 vertices worst case, inside
  the 2,500 class M budget.
- Complex particle layer: ASH_FLAKE on the existing gathering source in
  the body volume, 22 per second, budget 36.
- Filler: unchanged (plasma, vapor, boundary plasma, dust, ember, peak
  burst). No avatar, band, or dome was reintroduced.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
-sourcepath src/main/java:src/client/java, compiling the five owned
files, output to private /tmp tags ox262 and ox263. Full logs read
(/tmp/ox262.log, /tmp/ox263.log).

- cp262.txt (26.2): zero errors in owned files. The log's only errors
  are the known baseline kinds elsewhere in the closure: DbaItems
  CreativeModeTab Output protected access (2) and DbaMenus MenuType
  private access (2). All five owned renderers produced class files.
- cp263.txt (26.3): zero errors in owned files. Baseline errors only:
  DbaItems (2), DbaMenus (2), DbaHostileEntity swinging and attackAnim
  (2), FriezaSoldierEntity swing signature (1), NamekianWarriorEntity
  hurtMarked (1), PatrolRobotEntity swinging and attackAnim (2). All
  five owned renderers produced class files.
- git diff --check on the five owned files: clean. Added lines scanned
  for em dashes: zero (checked by character scan of all five files).

## Foundation change requests

None required. ComplexGeometry, ComplexTextures, MaterialEmitters, and
DbaParticles as shipped in Phase 1 covered every builder and sprite this
slot needed, including offset crack fans via pose translation, which the
shared submit helpers already support.

One observation for the merge, not a blocker: crackFan always fans
radially from its local origin, so a line shaped crack (OxFissure) needs
two translated fans to read as a line. That works inside the standard
and the budget, but if another line effect appears, a future toolkit
variant that walks cracks along a caller supplied axis would do it in
one call.

## Honest worse check

- KingsSlam and OxShockwave: better, not worse. The pre-redo read was a
  flat disc of glow with nothing structural in it; plates plus glowing
  cracks give the slam a broken-ground body that matches the permanent
  destruction the entity already leaves. One risk: the fracture plates
  are drawn in the local ground plane at the entity origin, so on steep
  slopes the plate skirts can float or clip exactly like any ground
  decal does. The slam entities spawn on the struck ground, so this is
  the same limitation the filler disc already had, not a new one.
- OxCharge: better. Inward torn strands make the gathering direction
  legible, which round plasma motes never showed. The strands are thin
  (0.11 to 0.21 wide) and brown at the root, so at long distance they
  will read faint; that is honest faintness through color, and the
  filler still carries the effect at range.
- OxFissure: the one real risk in this slot. Two radial crack fans
  overlapped along the line read as a glowing web concentrated near the
  two fan centers, so the middle and the ends of the trench get denser
  crack coverage than a single hand walked crack would. It still reads
  clearly as a glowing fissure core inside the trench footprint, and it
  is strictly more structured than the particles-only version, whose
  crack was only a line of glow sprites. I judge it better than both the
  particles-only version and the pre-redo version, but it is the first
  effect I would re-check in game; if the merge wants a single fan at
  the origin instead, that is a two line change and loses the line
  coverage.
- OxStanceAura: better, with a caveat. Five rising strands give the
  stance a body without reintroducing the rejected titan avatar or dome.
  The caveat: strand length is fixed at the 3.6 block body height while
  the denial field is 12 blocks, so the pieces layer says nothing about
  the field edge. That matches the audit row (the boundary stays sensed
  through the sparse filler motes, as before) and is not a regression.
