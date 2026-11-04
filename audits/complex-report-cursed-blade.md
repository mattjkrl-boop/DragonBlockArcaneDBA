# Complex Effects Redo, Phase 2 Report: Cursed Blade

Owner slot: Cursed Blade (theme: curse plasma, darkness smoke, lightning plasma, ground dust).
Base: HEAD 9178eac. Files edited: only the six assigned renderers under
`src/client/java/com/dragonblockarcanedba/client/render/`. No commits.
All existing material emission kept as filler, unchanged in color, rate, and seed.
No damage number, cooldown, cost, timing, mechanic, or synced field changed.
Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or drawHelix in the six files.
Every energy piece goes through ComplexGeometry.submitEnergy (tendrils with ComplexTextures.TENDRIL),
every solid piece through submitSolid, and every shader piece is fed a peak alpha of 0.90 to 0.98
with the lifecycle value passed as the envelope, never a low constant alpha.
All complex sprite layers are continuous emitters through the existing emit helper at 28 to 36
per second, inside the 40 per second per emitter budget.

## Per effect

### AbyssalDomainRenderer (class L)

- Pieces, builder type one: crackFan base web through submitEnergy, radius 1.02 times the domain
  radius, hot 0xFFF2F7 at the heart to cool 0xF20D33 at the tips, peak alpha 0.95, envelope is the
  existing build times fade alpha, seeded from the entity UUID seed.
- Pieces, builder type two: ten tendrils through submitEnergy on the TENDRIL canvas, one per rim
  sector, from the boundary circle at ground level, control point pushed outward at a third of the
  storm height, tip leaning inward at about half the storm height, root 0x8C1AF2 to tip 0xF20D33,
  peak alpha 0.92. That is two builder types, so the class L rule is met.
- Complex particles: ASH_FLAKE through Preset.fixed, slow downward velocity, sampled in the same
  sphere volume as the standing emission, 34 per second.
- Vertex estimate at distScale 1.0: crackFan about 620, tendrils about 272 each at the reduced
  detail factor (17 spine samples), total about 3,340 of the 6,000 class L budget.

### CurseChainRenderer (class M)

- Pieces: one tendril through submitEnergy on the TENDRIL canvas, drawn in the renderer's own
  local space under the same scale, yaw, and pitch pose transform the old link geometry used.
  The bezier runs from the first link center through the middle link center to the last, so on
  the bind arc it follows the arc across the target and in flight it runs straight down the
  trail, which is the taut low-curl read the audit asked for. Root 0xA60DD9 to tip 0xF20D33,
  peak alpha 0.95 times the existing pulse, envelope is the existing fade in times fade out.
- Complex particles: SPARK_STREAK through Preset.fixed at the world link centers, 30 per second.
- Vertex estimate: one tendril at 24 spine samples, 384 vertices of the 2,500 class M budget.

### CurseGroundShatterRenderer (class M)

- Pieces, energy: crackFan through submitEnergy at 1.02 times the shatter radius, hot 0xFFF2F7
  to cool 0xF20D33, peak alpha 0.95, envelope is the existing corrected fade.
- Pieces, solid: fracturePlates through submitSolid at the full shatter radius, lift 0.38,
  hot 0xB3122E at the center to dark 0x241419 at the rim, peak alpha 0.95.
- Complex particles: SHARD_CHIP through Preset.fixed along the existing seeded fissure paths,
  small upward velocity, 34 per second.
- Vertex estimate: crackFan about 620 plus plates about 480 at most, total about 1,100 of 2,500.

### CurseLightningRenderer (class M)

- Pieces: branchedLightning through submitEnergy in curse colors, near-white core 0xFFF0FA and
  the existing sheath color (0xCC0A66 normal, 0x8C0AF2 rare) as the glow, width 0.50, peak alpha
  0.98, envelope is the existing flash alpha. The bolt runs in local space from the cloud point
  to the same travelling ground end the emission path uses, so the 2.5 tick stab is shared, and
  it is seeded from the entity UUID. Detail is passed at 0.8 of the distance factor, which runs
  the trunk at four displacement levels and keeps the whole bolt inside budget on every seed.
- Complex particles: SPARK_STREAK through Preset.fixed at the strike point, 36 per second.
- Vertex estimate: about 53 path segments typical (under the 130 segment branchedLightning cap),
  about 1,700 vertices of the 2,500 class M budget.

### CurseTelegraphRenderer (class M)

- Pieces, energy: crackFan warning web through submitEnergy at the full marker radius, hot
  0xFFF2F7 to cool 0xF20D33, peak alpha 0.92, envelope is the existing accelerating pulse.
- Pieces, gather: six one-count tendrilFan calls through submitEnergy on the TENDRIL canvas, one
  from each of six rim points, aimed at the center and rising slightly, spread 16 degrees, length
  0.95 times the radius, root 0xF20D33 to tip 0x8C1AF2, peak alpha 0.90, detail at 0.66 of the
  distance factor so the fan plus the web stay in budget.
- Complex particles: SPARK_STREAK through Preset.fixed at the existing rim source, 28 per second.
- Vertex estimate: crackFan about 620 plus six tendrils at 16 samples, about 256 each, total
  about 2,150 of 2,500.

### SkyCracksRenderer (class M)

- Pieces, crack plane: two crackFans through submitEnergy on a billboarded pose, with a copied
  pose rotated minus ninety degrees about X so the crackFan ground plane becomes the billboard
  plane, the same orientation the old crack stars used. Wide web: radius 21, hot 0xFFF7F7 to cool
  0xF20D33, peak alpha 0.95. Tighter web inside it: radius 13, hot 0xE9D5FF to cool 0x8C1AF2,
  peak alpha 0.92 at 0.7 of the distance factor. Envelope for both is the existing lifecycle
  envelope times the electrical flicker.
- Pieces, falling shards: three shardBurst calls through submitSolid, five dark shards total
  (counts 2, 2, 1), root 0x14030A to tip 0x5A1230, peak alpha 0.95. Burst origins sit on the
  billboard plane at seeded plane offsets (6, 4), (-8, -2), and (1, -9), converted into entity
  local space through the camera basis already computed for the emission paths. The bursts aim
  down in world space at 38 degrees of spread, and each origin cycles downward with age at 0.35
  blocks per tick over a 7 block cycle with its own phase, so the shards fall and respawn higher
  instead of hanging as a static cluster.
- Complex particles: WISP_CURL through Preset.fixed along the existing fracture paths, 28 per
  second.
- Vertex estimate: crackFans about 620 plus about 350, shards 5 times 288 equals 1,440, total
  about 2,410 of 2,500. This is the tightest budget in the slot; see the worse check.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac, sourcepath
src/main/java:src/client/java, compiling only the six owned files, private output dirs
/tmp/dba-cursed-blade-262 and /tmp/dba-cursed-blade-263.

- 26.2 classpath (cp262.txt): 4 errors, all baseline in files outside this slot
  (item/DbaItems.java protected access, inventory/DbaMenus.java private access). Zero errors in
  the six owned files, and all six owned class files were produced.
- 26.3 classpath (cp263.txt): 10 errors, all baseline outside this slot (the same DbaItems and
  DbaMenus errors plus entity/DbaHostileEntity.java, FriezaSoldierEntity.java,
  NamekianWarriorEntity.java, and PatrolRobotEntity.java symbol and signature errors). Zero
  errors in the six owned files, and all six owned class files were produced.
- git diff --check on the six files: clean. Em dash scan of the six files and of this report:
  zero. Primitive scan (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) in the
  six files: zero call sites.

## Foundation change requests

No private toolkit was built. Four gaps were worked around inside the shipped toolkit and are
filed here for the merge:

1. branchedLightning generates its own displaced path and cannot accept a caller-supplied trunk
   polyline. CurseLightning already owns a seeded emission path, so the geometry bolt and the
   plasma filler bolt are two different jagged paths between the same endpoints (see the worse
   check). A variant that takes a prebuilt trunk polyline, or returns its trunk points, would
   let emission and geometry share one path.
2. shardBurst takes a single origin. Shards falling from a wide crack plane need scattered
   origins; SkyCracks works around it with three small bursts. A scatter variant (an origin
   list, or a disc or plane scatter radius) would serve every rift style effect.
3. tendrilFan fans outward from one origin into a cone and has no gather form. CurseTelegraph
   works around it with six one-count fans from rim points aimed at the center. A variant that
   takes explicit from and to pairs would express gathers, lash rings, and rim effects directly.
4. crackFan and fracturePlates draw only in the local XZ plane. SkyCracks reorients through a
   pose rotation, which works and matches the old crack star orientation, but a plane or normal
   parameter on crackFan would make sky, wall, and ceiling crack effects first class.

## Worse check

Honest per effect, against both the particles-only version now in the tree and the pre-redo
version where one exists.

- AbyssalDomain: better than particles-only, which had no boundary or ground structure at all.
  One real weakness: the rim tendrils are seeded static silhouettes. The tendril builder has no
  caller phase input, so the strands do not sway with age; only the envelope and the moving
  emission around them animate. Ten frozen strands at the rim could read as spikes if the domain
  is stared at. Judged acceptable, not worse overall: the volume emission still churns, and a
  structured rim is what the effect lacked. If the merge wants motion here, tendril needs a
  phase parameter (related to request 3).
- CurseChain: better than particles-only, which had no continuous chain body. The builder's two
  pinch windows collapse the strand to 15 percent width at two points; on a chain that reads
  plausibly as segmentation between links, but it could also read as the chain nearly breaking.
  The strand is also thinner than the old torus link chain from the pre-redo version. On balance
  it restores the one thing the effect is named for, a visible chain from end to end of the
  path, so it ships.
- CurseGroundShatter: better than particles-only on both counts that matter, glowing cracks and
  lifted ground are now geometry instead of dust points. The plates appear at full lift on the
  first frame, because fracturePlates takes a constant lift and this effect's envelope is a
  fade-out only, exactly as its emission already behaved. For a 35 tick impact effect that pop
  is the impact; not judged worse.
- CurseLightning: the one genuine visual risk in the slot. Because of foundation gap 1, the
  geometry bolt and the emission bolt are different jagged paths with the same endpoints, so
  the strike can read as two close parallel bolts instead of one bolt with a glow sheath. In
  practice the effect already had a twin strike landing beside the trunk, so a second nearby
  discharge is inside its existing language, and the geometry bolt is the brighter, structured
  one. Judged better than particles-only, but if Carter reads it as a double bolt in game, this
  is the piece the merge should revert or fix via change request 1, and I am flagging it rather
  than hiding it.
- CurseTelegraph: better than particles-only; the warning web is now a real crack fan, which is
  what a telegraph needs to read at a glance. Weakness: the gather strands do not tighten as
  progress advances, because their length is fixed; the tightening read still comes from the
  shrinking emission source, as before. The strands add rim structure the old version lacked.
- SkyCracks: judged honestly, as instructed, since the previous pass flagged this as the softest
  conversion. The crack plane is a clear improvement: two layered crackFans with forks and
  pinches read as a torn rift, where the particles-only version smeared plasma points along
  paths and read as a glow cloud. Two honest limits. First, crackFan cracks all radiate from
  the center, so the plane reads as a radial fan with wandering and forks, less web-like than
  the pre-redo mains plus web network; the tighter violet fan restores some of that layering
  but not all. Second, the falling shard layer is thin: five shards in three clusters against
  a 42 block wide rift, because the class M vertex budget is mostly spent on the two crackFans,
  and each shard's 7 block fall cycle wraps with a small pop at respawn. Even so, the shards
  are dark accents under a bright crack plane and the thinness reads as sparse debris, not as
  a missing layer. Overall this is not worse than the particles-only version and is closer to
  the pre-redo read; it ships, with the shard count and the radial fan noted as the two places
  a future budget or builder change (requests 2 and 4) would improve it first.

No effect in this slot is shipped silently worse. The two flagged risks are CurseLightning's
possible double bolt read and SkyCracks' thin shard layer; both are stated above with the
foundation change that would remove them.
