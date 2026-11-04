# Complex Effects Report: Z Sword Slot

Owner slot: Z Sword (theme: gravity plasma, stone dust, steel sparks).
Base: HEAD 9178eac, Phase 1 foundation. Files edited: only the five
assigned renderers under src/client/java/com/dragonblockarcanedba/client/render/.
No commits. No damage number, cooldown, cost, timing, mechanic, or
synced data field changed. The d74f13a destruction wiring is untouched.
The existing material emission in every file is kept in full as the
filler layer; nothing was traded against it.

## Per effect

### ZChargeRenderer (class M)

- Pieces layer: ComplexGeometry.tendrilFan through
  ComplexGeometry.submitEnergy with ComplexTextures.TENDRIL. Three
  seeded clusters sit on the charge ring (positions derived from the
  entity seed plus a slow age based rotation) and each fans two
  strands inward toward the core point at center plus one block, the
  same point the filler emission already converges on. Gold at the
  root, bright gold at the tip, peak alpha 0.95.
- Complex particle layer: SPARK_STREAK through
  MaterialEmitters.Preset.fixed on the existing funnel volume source,
  continuous at 28 per second with a 36 per second budget, inside the
  40 per second continuous cap.
- Vertex budget: six tendrils at 24 spine segments and two crossed
  quads per segment is about 2304 vertices, inside the class M 2500
  budget, and it scales down with the distance scale.

### ZGravitySlamRenderer (class L)

- Pieces layer, two builders as the class L rule requires:
  ComplexGeometry.fracturePlates through submitSolid (stone dust hot
  color over dark stone, radius follows the growing shock radius,
  lift scales with power) and ComplexGeometry.crackFan through
  submitEnergy (pale gold hot at the center running out to violet at
  the tips, alpha 0.95, envelope tied to fade and the strike
  envelope). These are the fast near field fragments and energy
  structure; the entity already spawns a DebrisSpawner field for the
  big physical chunks, and that is untouched.
- Complex particle layer: a one shot burst fired once per emission
  instance (once per entity and seed) while progress is under 0.55:
  SHARD_CHIP burst of 36 and SPARK_STREAK burst of 20 through
  MaterialEmitters.burst, thrown outward and up from inside the
  growing shock radius. Combined 56, under the class L 60 burst
  budget.
- Vertex budget: plates are about 48 vertices per plate over at most
  ten plates, cracks at most about 790 vertices, combined roughly
  1270, well inside the class L 6000 budget.

### ZShockwaveRenderer (class M)

- Pieces layer: ComplexGeometry.fracturePlates small through
  submitSolid (radius capped at 3.4 blocks or 0.36 of the wave span,
  stone dust over dark stone) plus ComplexGeometry.crackFan through
  submitEnergy (near white core color at the center running out to
  gold, alpha 0.95). Both sit at the origin in the local ground
  plane, ride the strike envelope for the first 18 ticks, and fade
  out completely by about tick 40, while the wave itself lives
  longer on its filler emission.
- Complex particle layer: SHARD_CHIP through
  MaterialEmitters.Preset.fixed on the existing lower edge dust
  source, continuous at 22 per second with a 30 per second budget.
- Vertex budget: plates plus cracks are roughly 1200 vertices,
  inside the class M 2500 budget.

### ZStanceAuraRenderer (class M)

- Pieces layer: ComplexGeometry.tendrilFan through
  ComplexGeometry.submitVeil with ComplexTextures.TENDRIL: six
  strands rising from the base in a 38 degree cone, length equal to
  the existing funnel height, deep violet at the root to pale violet
  at the tip. Peak alpha 0.78, which is the standard's allowed lower
  peak for wisps and veils, not a body faintness fake; the envelope
  breathes with the stance's existing pulse.
- Complex particle layer: WISP_CURL through
  MaterialEmitters.Preset.fixed on the existing well volume source,
  continuous at 24 per second with a 32 per second budget.
- Vertex budget: six tendrils at the toolkit segment count is about
  2304 vertices, inside the class M 2500 budget.

### DelayedLaunchRenderer (class M)

- Pieces layer: ComplexGeometry.crackFan through submitEnergy for
  the whole countdown, a telegraph web on the ground inside the
  contracting radius, gold hot at the center running out to violet,
  alpha 0.92, envelope rising with countdown progress. In the launch
  window (the last tenth of the countdown) a
  ComplexGeometry.tendrilFan lash fans upward from the center
  through submitEnergy with ComplexTextures.TENDRIL, violet at the
  root to gold at the tip, alpha 0.95, envelope ramping over that
  last tenth.
- Complex particle layer: SPARK_STREAK at launch, a one shot burst
  of 24 through MaterialEmitters.burst, fired once per emission
  instance on the first submit inside the launch window, under the
  class M 30 burst budget.
- Vertex budget: the crack web is at most about 790 vertices and
  the lash is four tendrils at about 1536, combined about 2326 in
  the launch window, inside the class M 2500 budget.

## Cross cutting checks

- Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere,
  or drawHelix in all five files (grep, no matches).
- No PLASMA material emission is the sole or dominant layer of any
  effect here anymore: every class M and L effect carries a pieces
  layer and a complex sprite layer over the kept filler.
- Every energy piece goes through submitEnergy or submitVeil; every
  solid piece (the fracture plates) goes through submitSolid.
- Body peak alphas are 0.92 to 0.96. The only lower peak is the
  stance wisp veil at 0.78, which the standard explicitly allows
  for wisps and veils.
- All piece seeds come from the entity's synced seed field, with
  fixed per effect salts, exactly as the existing emission does.
- Added lines em dash scan: clean (checked for the em dash and en
  dash characters in all five files). git diff --check on the tree
  state at verification time: clean.

## Verification

Targeted javac with JDK 25 (~/workspace/tools/jdk-25.0.5+8/bin/javac),
sourcepath src/main/java:src/client/java, compiling only the five
owned files, private output tags /tmp/zsword-262 and /tmp/zsword-263:

- 26.2 classpath (cp262.txt): zero errors in owned files. The only
  errors in the closure are the known baseline kinds elsewhere:
  DbaItems CreativeModeTab output access (2) and DbaMenus MenuType
  access (2), 4 errors total, none in this slot. Class files for all
  five renderers and their inner classes were produced.
- 26.3 classpath (cp263.txt): zero errors in owned files. Baseline
  errors elsewhere: the same DbaItems (2) and DbaMenus (2), plus
  DbaHostileEntity swinging and attackAnim (2), FriezaSoldierEntity
  swing signature (1), NamekianWarriorEntity hurtMarked (1), and
  PatrolRobotEntity swinging and attackAnim (2), 10 errors total,
  none in this slot. Class files for all five renderers were
  produced.

## Foundation change requests

No blocking gaps. Nothing private was built; all geometry and all
complex sprites go through the shared foundation as shipped.

One optional extension worth considering at the merge:
ComplexGeometry.tendril and tendrilFan take no caller phase
parameter, unlike flameTongues which takes swayPhase. Strand
silhouettes are therefore static for a given seed, and motion has to
come from the envelope, the origin, or the fan rotation. ZCharge
rotates its cluster origins with age and ZStance breathes its veil
envelope with the existing pulse, which covers this slot, but a
phase argument that advances the curl offsets would let stance and
charge strands visibly writhe instead of only breathing. Not
required by any gate in the standard.

## Honest worse check

- ZCharge: low risk. The tendrils converge on the same core point as
  the filler, the count is small, and the width tops out at 0.24.
  The one clutter risk is at full charge, where strands, core glints,
  and streaks all overlap near the core; the strand alpha envelope
  follows the same baseAlpha as the filler, so nothing new outlives
  or outshines the existing read. Not worse than the particles only
  version, which had no structure at all.
- ZGravitySlam: low risk. Plate positions scale with the same shock
  radius the filler uses and the facet pattern is stable frame to
  frame because the seed is fixed, so the fan reads as ground
  heaving outward rather than flickering. The plates fade with the
  slam envelope instead of persisting; the permanent crater from
  the destruction pass is what persists, which is the correct
  division. Not worse.
- ZShockwave: this is the one real risk in the slot, stated plainly.
  The plates and crack web are drawn in the unrotated local ground
  plane at the entity origin, and the origin travels with the wave.
  For a ground hugging launch this reads as the ground fracturing
  where the wave breaks loose, which is what the audit ordered. For
  a steeply pitched or airborne launch, the same pieces would read
  as a small fracture fan travelling with the wave for its first
  40 ticks. The mitigations in place are the small capped radius
  and the hard fade by about tick 40. I judge it better than the
  particles only version in the normal case and acceptable in the
  pitched case, but if in game testing shows pitched launches are
  common, the fix is to gate the pieces layer on the entity being
  near the ground, which needs a synced or extracted ground height
  and is a merge decision, not something to improvise here.
- ZStanceAura: small risk. The veil strands are seeded and static
  in silhouette (see the change request above), so against the
  swirling filler they could read as slightly frozen on a long
  hold. The pulse breathing on the envelope and the WISP_CURL layer
  moving through the same volume keep it from reading as a static
  prop, and it is strictly more structured than the particles only
  version. Not worse overall.
- DelayedLaunch: low risk with one timing caveat. The crack web
  contracts with the same radius as the filler, so the telegraph
  stays honest about the footprint timing. The lash and the streak
  burst depend on the renderer receiving a submit during the last
  tenth of the countdown; for very short delays that window can be
  a single tick. The burst flag fires on the first qualifying
  submit, so any frame in the window triggers it, and if the entity
  is removed exactly at the launch tick the gravity slam effect
  that follows carries the impact anyway. Not worse.

Nothing in this slot reads worse than the pre redo version in a way
that needs a revert. The ZShockwave pitched launch caveat above is
the only item I would put in front of Carter at the merge.
