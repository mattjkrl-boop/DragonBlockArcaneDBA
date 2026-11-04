# Complex Effects Redo, Phase 2 Report: Dabura's Sword

Owner slot: Dabura's Sword (darkness smoke, hellish plasma, soul vapor)
Base: HEAD 9178eac. Files edited: only the five assigned renderers under
src/client/java/com/dragonblockarcanedba/client/render/. No commits.
Themes, colors, damage numbers, cooldowns, costs, timing, mechanics, and
synced data fields are identical to HEAD in every file. The existing
material emission is untouched and remains as the filler layer in all
five effects. No calls to drawColoredBox, drawCylinder, drawDisc,
drawSphere, or drawHelix were added (scan of the five files: zero).

## Per effect

### DarknessBladeRenderer (class M)

- Pieces layer: one tendrilFan of 3 tendrils through
  ComplexGeometry.submitVeil with ComplexTextures.TENDRIL, rooted at the
  blade's upper end (local y 1.55) and streaming upward as the wake
  behind the falling blade, hellish purple at the root (0x8C0DF2) fading
  to dark violet at the tips (0x2A0045), peak alpha 0.92, envelope equal
  to the existing 3 tick fade-in. Estimated 1,152 vertices per submit at
  distScale 1.0 (3 tendrils x 24 spine samples x 16 vertices), inside
  the class M budget of 2,500.
- Complex particle layer: EMBER_FRAGMENT (Preset.fixed, tip spark
  source, 30 per second, budget 36) and SPARK_STREAK at the leading tip
  (Preset.fixed, tip spark source, 22 per second, budget 28). Both stay
  under the 40 per second continuous cap.
- Filler: the seven existing material emitters, unchanged.

### DarknessChargeRenderer (class M)

- Pieces layer: five individual tendril calls inside one
  ComplexGeometry.submitVeil submission with ComplexTextures.TENDRIL.
  tendrilFan only fans outward, so the inward strands are authored per
  strand: each is rooted on the gathering ring at baseRadius(state) x
  1.05 (the same charge driven radius the emission uses), its control
  point is swung tangentially so the strand curls as it is pulled in,
  and it ends at the core (local y 0.85). Colors run near black at the
  rim (0x1F0040) to hellish pink at the core (0xF21459), peak alpha
  0.92, envelope 0.30 plus 0.70 x charge, matching the emission
  envelope. Estimated 1,920 vertices per submit (5 x 384), inside the
  class M budget of 2,500.
- Complex particle layer: WISP_CURL (Preset.fixed, sphere gather
  source, 28 per second, budget 34).
- Filler: the seven existing material emitters, unchanged.

### DarknessDomainRenderer (class L)

- Pieces layer, two builder types: (1) six rim tendrils through
  ComplexGeometry.submitVeil with ComplexTextures.TENDRIL, rooted at
  the base rim, climbing to 62 percent of the dome height along the old
  dome profile and curling inward, dark at the root (0x1E0033) to
  purple at the tip (0x8C0DF2), peak alpha 0.90, envelope equal to the
  synced fade. (2) crackFan at the domain base through
  ComplexGeometry.submitEnergy, at the full synced radius, hot pink at
  the center (0xF21459) cooling to hellish purple (0x8C0DF2), peak
  alpha 0.95, so the energy pieces carry the shader treatment the class
  L rule requires. Estimated vertices per submit: about 2,304 for the
  rim tendrils plus about 500 to 650 for the crack fan, about 2,900
  total, inside the class L budget of 6,000.
- Complex particle layer: ASH_FLAKE falling inside the volume
  (Preset.fixed, dome volume source, downward preset velocity, 32 per
  second, budget 38).
- Filler: the eight existing material emitters, unchanged. Lightning
  stays with the real BoltEntity strikes, untouched.

### DarknessShatterRenderer (class M)

- Pieces layer, two builder types: (1) crackFan as the shatter web
  itself through ComplexGeometry.submitEnergy, at the live expanding
  shockRadius(state), hot pink at the center (0xF21459) cooling to
  purple (0xA605F2), peak alpha 0.95. (2) fracturePlates through
  ComplexGeometry.submitSolid at 0.85 x the shock radius with 0.38
  lift, dark treatment: purple hot at the center (0x4C1D95) to near
  black at the rim (0x12001F), peak alpha 0.95, envelope equal to the
  existing fade x build envelope. Estimated vertices per submit: about
  500 to 650 for the crack fan plus about 480 for the plates, about
  1,100 total, inside the class M budget of 2,500.
- Complex particle layer: SHARD_CHIP (Preset.fixed, outward ground
  source, 30 per second, budget 36).
- Filler: the seven existing material emitters, unchanged.

### DarknessWaveRenderer (class M)

- Pieces layer: three tendril strands inside one
  ComplexGeometry.submitVeil submission with ComplexTextures.TENDRIL,
  spanning the wave front as its body. The strands are built on the
  same forward, right, and up basis vectors the emission samples, at
  three lanes across the front, endpoints at the rim (plus and minus
  waveSpan(state)) and control points bowed 0.65 forward, so the front
  bulges the way the emission volume does. Colors run dark purple at
  one rim (0x3B0764) to hellish purple at the other (0xA605F2), peak
  alpha 0.92, envelope equal to waveFade(state), so the pieces open
  over 5 ticks and die at tick 60 with the wave. Estimated 1,152
  vertices per submit (3 x 384), inside the class M budget of 2,500.
- Complex particle layer: EMBER_FRAGMENT shed from the front
  (Preset.fixed, front source, 28 per second, budget 34).
- Filler: the five existing material emitters, unchanged. The first
  person owner scaling still applies, because the strands derive their
  span and height from waveSpan(state) and waveHeight(state).

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the five owned
files, output under private /tmp tags dabura-sword-262 and
dabura-sword-263:

- cp262.txt (26.2): 4 errors total, zero in the owned files. All four
  are the baseline kinds elsewhere in the closure: Output has protected
  access in CreativeModeTab (x2, DbaItems), and MenuType MenuSupplier
  private access (x2, DbaMenus). Class files for all five renderers and
  their render states were produced.
- cp263.txt (26.3): 10 errors total, zero in the owned files. Baseline
  kinds elsewhere: the same 4 access errors as 26.2, plus cannot find
  symbol (x5) and no suitable method found for swing(InteractionHand)
  in the hostile entity files (DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity). Class files for all five
  renderers and their render states were produced.
- git diff --check on the five owned files: clean.
- Added lines em dash scan on the five owned files: zero.
- Plain primitive scan on the five owned files: zero calls.

## Foundation change requests

None required. Every assigned builder, submit helper, canvas texture,
and complex sprite type was used exactly as shipped.

One optional observation for the merge, not a blocker: tendrilFan only
fans outward from its origin, so an inward gathering fan (the charge
pattern in this slot, and likely the other charge owners) has to be
authored as per strand tendril calls inside one submission. That works
fine and stays in budget. If several owners did the same, a shared
inward fan variant would save repetition, but nothing in this slot
needs it to ship.

## Honest worse check

- DarknessBlade: not worse. The particles only version had no wake
  structure at all; the veil fan adds a tearing silhouette above the
  blade. One real limit: the fan is rotationally symmetric around the
  fall axis while the filler emission swirls with age, so the strands
  do not visibly spin with the swirl. The read is a trailing wake, not
  a rotating one, and that still beats round particles alone.
- DarknessCharge: not worse. The strands are seeded and static in curl
  shape; their motion comes from the ring radius growing with charge
  and from the filler streaming inward between them. At very low
  charge the envelope (0.30) keeps them faint by design, the same
  envelope the emission already used, so the charge still reads as
  building rather than starting at full strength.
- DarknessDomain: not worse, with one sparsity caveat. Six rim strands
  around a radius 8 dome are accents on the boundary, not a wall; that
  is deliberate, because the dome volume filler already fills the
  interior and a denser rim would blow the vertex budget and hide the
  crack fan. The base crack fan is the strong new read here.
- DarknessShatter: not worse. The solid dark plates sit over the
  energy web and partially cover its center at peak radius; that
  layering is the intended ground broke over glowing cracks read, and
  the web stays visible in the gaps the plate fan leaves (about 15
  percent of plates are missing by toolkit design, plus the radial
  gaps between plates).
- DarknessWave: the closest call in this slot, still not worse. Veil
  strands are alpha blended, so against a bright sky the dark purple
  body reads weaker than an additive piece would. I kept the veil path
  anyway: the wave's theme is darkness smoke, an additive body would
  turn it back into a glow wave, and the hellish plasma filler already
  supplies the glow behind the strands. Against the particles only
  version, which had no body at all, three torn strands spanning the
  full front are a clear gain. If in game testing shows the strands
  washing out at midday, the fix inside the standard is a darker root
  color on the same veil submission, not a path change.
