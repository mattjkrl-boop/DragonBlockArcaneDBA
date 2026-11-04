# Complex Effects Redo, Phase 2 Report: Bansho Fan

Owner slot: Bansho Fan. Base HEAD 9178eac. Files edited, and only these:
BanshoCycloneRenderer.java, BanshoGaleRenderer.java,
BanshoShockwaveRenderer.java, BanshoWindProjectileRenderer.java, all in
src/client/java/com/dragonblockarcanedba/client/render/. No commits.
Themes, colors, damage, cooldowns, costs, timing, mechanics, and synced
fields are unchanged. The BanshoShockwaveEntity constructors, the
WindChargeMixin compatibility, and the cyclone entity DebrisSpawner
wiring are untouched. Every piece seed derives from the same entity id
based seed the existing emission already used. The existing material
emission is kept in full as the filler layer in all four files.

## BanshoCyclone (class L)

- Pieces layer: ComplexGeometry.tendrilFan, six tall wind strands (the
  full fan budget for one builder type, which meets the class L rule),
  in a 32 degree cone around the column axis, length at 1.10 times the
  cyclone's current grown height, root 0x26FFA6 to tip 0xCFFFEF, peak
  alpha 0.92. Submitted through ComplexGeometry.submitEnergy with the
  ComplexTextures.TENDRIL canvas, so the strands carry the shader
  dissolve and fresnel treatment. The fan rotates around the axis with
  age at 2.0 degrees per tick, a surface speed at the column wall that
  matches the filler tangential velocity (0.11 to 0.15 per tick), on a
  copied pose inside the piece, following the AzureTornado precedent.
- Complex particle layer: ASH_FLAKE through MaterialEmitters.Preset.fixed
  at 24 per second (cap 40) sampled through the whole column volume, and
  SHARD_CHIP through Preset.fixed at 20 per second (cap 40) sampled in
  the column wall band so the chips orbit with the flow. Both under the
  40 per second continuous budget.
- Vertex budget: 6 tendrils times 24 samples times 16 vertices is 2304
  at distance scale 1.0, inside the class L budget of 6000.

## BanshoGale (class M)

- Pieces layer: three ComplexGeometry.tendril strands laid along the
  existing fanPoint rib curves, computed by the same helper and the same
  spread, open, and lift state the emission samples: the center rib at
  width 0.34 (root 0xD8FFF2 to tip 0x26FFA6, alpha 0.95) and the flanking
  ribs at side plus and minus 2.3 at width 0.20 (root 0x26FFA6 to tip
  0x9FFFE0, alpha 0.90). Submitted through submitEnergy with the TENDRIL
  canvas, under a pose rotation of yaw then pitch matching the frame
  the renderer's own right, up, and forward helpers build.
- Complex particle layer: WISP_CURL through Preset.fixed at 28 per
  second (cap 40), shed across the full fan surface sample.
- Vertex budget: 3 tendrils times 24 samples times 16 vertices is 1152,
  inside the class M budget of 2500.

## BanshoShockwave (class M)

- Pieces layer: two builders. ComplexGeometry.crackFan (hot 0xCFFFEF to
  cool 0x26FFA6, alpha 0.95, through submitEnergy) at 1.05 times the
  wave's current radius, and ComplexGeometry.fracturePlates (hot
  0x9FFFE0 to dark 0x2F6B4F, alpha 0.96, through submitSolid) at 0.92
  times the current radius for radial waves and 0.85 times the cone
  radius for cone waves, at a light 0.12 lift so they read as wind
  scour lifting the surface rather than a slam crater. Radial waves
  draw flat in the world XZ plane; cone and orient-to-impact waves draw
  under the same yaw and pitch rotation the emission frame uses. Radii
  follow the same progress driven fields (currentRadius, coneRadius)
  the emission already reads, so the pieces grow with the wave.
- Complex particle layer: SHARD_CHIP through Preset.fixed at 16 per
  second (cap 30), a few chips riding the wave front, cone sample for
  cone waves and radial front sample for radial waves.
- Vertex budget: the crack fan stays under its 900 vertex budget from
  the standard and the plates under about 480, total under 1400, inside
  the class M budget of 2500.

## BanshoWindProjectile (class M)

- Pieces layer: two ComplexGeometry.tendril strands as a double spiral
  wake. Both run from the head at local z 0.72 to the tail at local z
  minus 2.05; their bezier control points sit on opposite sides of the
  axis at radius 0.30 and rotate with the same age times 0.80 spin phase
  the body sampling already uses, so the pair twists around the axis as
  one wake. Width 0.26, root 0xD8FFF2 to tip 0x26FFA6, alpha 0.92,
  through submitEnergy with the TENDRIL canvas, under the yaw then pitch
  pose rotation matching the emission frame. All lengths and widths carry
  the existing first-person owner scale of 0.55. These are torn, pinched
  tendril strands, not the deleted clean helix mesh.
- Complex particle layer: WISP_CURL through Preset.fixed at 24 per
  second (cap 40), shed along the existing dust trail sample behind the
  projectile.
- Vertex budget: 2 tendrils times 24 samples times 16 vertices is 768,
  inside the class M budget of 2500.

## Gates checked locally

- Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix in the four files (grep clean).
- No PLASMA emission added or promoted; in Gale and WindProjectile the
  pre-existing plasma filler is unchanged and is no longer the dominant
  read because the tendril body now carries the shape. Cyclone and
  Shockwave have no plasma body layer at all beyond the pre-existing
  radial center and cone core filler, unchanged.
- Every energy piece goes through submitEnergy with the TENDRIL canvas
  (cyclone, gale, projectile) or submitEnergy on the white canvas
  (shockwave crackFan); the one solid piece (shockwave plates) goes
  through submitSolid.
- Peak alphas on shader pieces are 0.90 to 0.95; no low constant alpha
  is used to fake faintness. Faintness differences between strands use
  color (emerald versus pale mint), per the standard.
- git diff --check clean on the four files; added lines scanned for em
  dashes, zero found.

## Verification

Targeted javac with JDK 25 at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the four owned
files, output to private /tmp tags (bansho-out262, bansho-out263, logs
bansho-javac262.log and bansho-javac263.log). Full logs read.

- 26.2 classpath (cp262.txt): 4 errors, all pre-existing baseline kinds
  outside this slot: DbaItems.java CreativeModeTab Output protected
  access (2) and DbaMenus.java MenuType private access (2). Zero errors
  in any owned file, and class files for all four renderers were
  emitted.
- 26.3 classpath (cp263.txt): 10 errors, all pre-existing baseline kinds
  outside this slot: the same DbaItems and DbaMenus errors, plus
  DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, and
  PatrolRobotEntity symbol and signature errors. Zero errors in any
  owned file, and class files for all four renderers were emitted.

## Foundation change requests

None required. All four conversions were built with the shipped
builders and submit helpers only, and no private toolkit was added.

One optional observation for the merge, not a request that blocks this
slot: tendrilFan always fans from a single origin point, so the cyclone
strands share the base center point and lean outward, rather than
starting distributed around the column wall ring. A ring origin variant
of tendrilFan in the shared toolkit would let column effects start
their strands on the wall itself. The current fan still reads as
strands around the column because the 32 degree cone puts most of each
strand's length against the wall.

## Honest worse check

- Cyclone: the main risk is that additive emerald strands read as glow
  rather than mist. Mitigated by the tendril canvas dissolve, the torn
  pinch profile, and the rotation rate matched to the filler flow, and
  the same submitEnergy treatment is what the already accepted Azure
  Tornado conversion uses for the same kind of column. Judged better
  than the particles-only version, which had no structure at all.
- Gale: the tendrils cover three of the nine ribs, so the fan reads as
  three torn strands inside the spray rather than a solid sheet. That
  matches the audit wording (tendril along the gale path) and the filler
  still carries the surface. Not worse than before.
- Shockwave: this is the riskiest conversion in the slot. Solid plates,
  even at 0.12 lift and in pale mint to dark green, are physical objects
  in a wind effect; at the first ticks, when the radius is near its 0.4
  minimum, the plates are small and could read as a tight cluster of
  green shards at the origin rather than scoured ground. The crackFan
  web carries the scour read at those sizes and the plates grow with
  the wave, so on balance it is judged not worse than the
  particles-only version, but the merge should look at the first two
  ticks in game before accepting the plate layer, and dropping the
  plates (keeping crackFan plus chips) is the fallback if they read as
  floating debris. The cone orientation is the second risk: the fan and
  plates sit in the plane facing the cone direction at the origin,
  which reads as a launch burst web; if it reads wrong in game, scoping
  the pieces to radial and orient-to-impact waves only is a one block
  change in this file.
- WindProjectile: the double strand shares the head and tail points, so
  the wake pinches at both ends by construction; the tendril pinch
  windows already tear the strands mid-length, so the silhouette stays
  irregular. The spin phase is the same fast 0.80 radians per tick the
  body sampling used before this pass, so the motion character is
  unchanged from what Carter already had. Not worse than before.
