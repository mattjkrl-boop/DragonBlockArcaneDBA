# Complex Effects Redo, Phase 2 Report: Devil Trident

Owner slot: Devil Trident. Files edited, and only these:
src/client/java/com/dragonblockarcanedba/client/render/DevilSlamShockwaveRenderer.java,
src/client/java/com/dragonblockarcanedba/client/render/TridentProngRenderer.java,
src/client/java/com/dragonblockarcanedba/client/render/TridentShardRenderer.java.
No entity code touched. DevilsHandsEffect.java not touched. No commits.
Themes, colors, damage, cooldowns, costs, timing, mechanics, and synced
fields are identical to HEAD 9178eac. The slam hit timing (tick 8) and its
zoned brimstone conversions are entity and destruction code and were not
touched; renderer only. The d74f13a destruction wiring is untouched.

## DevilSlamShockwaveRenderer (class L)

Pieces layer, three builder types, all seeded from the entity UUID seed
already in the render state:

- fracturePlates through submitSolid: a brimstone fan at 0.90 of the seal
  radius, lift 0.42, gradient PLATE_HOT 0x8A2E10 at the center to
  PLATE_DARK 0x1E0D08 at the rim, peak alpha 0.97, envelope is the
  existing fade. Sized at 0.90 deliberately: plates reach at most 1.10
  times their fan radius, so the fan stays inside the hit edge (see the
  worse check on telegraph honesty below).
- crackFan through submitEnergy: a magma web at 0.95 of the seal radius,
  MAGMA_HOT 0xFFCE7A at the center running to the existing HOT 0xFF381A
  at the tips, peak alpha 0.95, envelope is the existing fade.
- flameTongues through submitEnergy with ComplexTextures.FLAME_TONGUE:
  9 tongues rising from the crater ring area, base disc 1.05 times the
  plate fan radius, height 1.5 plus a quarter of the existing pillar
  height math, TONGUE_HOT 0xFFF0C0 at the base to HOT at the tip, peak
  alpha 0.95, sway phase from age so the tongues move.

Vertex estimate at distScale 1.0: plates about 410 (10 plates, seeded
gaps, at most 48 vertices each), crackFan about 560 (9 to 11 cracks plus
forks), flameTongues 432 (9 tongues, 6 segments). Total about 1400,
inside the class L budget of 6000. No shardBurst geometry was added: the
audit row for this effect names exactly these three builders, and the
entity already spawns a DebrisSpawner field server side for the big
physical chunks, so mesh shards would duplicate the debris.

Complex particle layer, one shot bursts through MaterialEmitters.burst
with Preset.fixed, guarded once per slam inside the existing Emission
tick, inside the class L burst budget of 60 per burst event:

- At ground contact (age 1.0, the same tick the server side ground burst
  and debris field fire): EMBER_FRAGMENT burst of 30 and SHARD_CHIP
  burst of 24, sampled across the seal disk with outward and upward
  velocities.
- After, at age 16.0 as the slam settles: ASH_FLAKE burst of 20, sampled
  across the disk with a gentle upward drift.

Filler layer: the existing material emission is unchanged (dust disk,
flame disk, dark and hot plasma disks, brimstone smoke disk, core
cluster, flame, plasma, and ember column, follow dust front). PLASMA is
no longer the body of the effect; the plates, cracks, and tongues carry
the silhouette and the emission fills between them.

## TridentProngRenderer (class M)

Pieces layer, per needle, at the existing formation offsets and radii,
seeded from the entity UUID seed:

- shard through submitSolid: one shard mesh per needle as the prong
  head, drawn along the flight direction from behind the needle center
  so the tip lands ahead of it, length 1.10 times the needle radius
  factor, girth 0.34 times it, gradient from the existing SHELL
  0x5C050A root to the existing CORE 0xF53D14 tip, peak alpha 0.97,
  envelope is the existing intro fade and post-impact dissolve.
- flameTongues through submitEnergy with ComplexTextures.FLAME_TONGUE:
  3 tongues at each needle base, radius 0.24 and height 0.55 times the
  needle radius factor, white hot 0xFFF0C0 base to CORE tip, peak alpha
  0.95, sway phase from age with a per needle phase offset.
- tendril through submitEnergy with ComplexTextures.TENDRIL: one torn
  wake per needle, from the needle base curving back 1.25 blocks behind
  it with an age based sway, width 0.26 times the radius factor, CORE at
  the root to SHELL at the tip, peak alpha 0.90, drawn at 0.7 of the
  distance scale to hold the budget.

Vertex estimate at distScale 1.0: 3 shards at 288 is 864, 3 tendrils at
reduced segment scale about 770, 9 flame tongues about 430. Total about
2070, inside the class M budget of 2500.

Complex particle layer: a continuous EMBER_FRAGMENT emitter per needle
at the needle base source, Preset.fixed with spread 0.08, rate 18 per
second, inside the 40 per second continuous budget, ticked at 0.8 of
the existing envelope.

Filler layer unchanged: plasma head clusters, head flames, plasma
trails, material embers at the bases.

## TridentShardRenderer (class M)

Pieces layer, seeded from the entity UUID seed:

- shard through submitSolid: one shard mesh as the shard body, a jagged
  hellfire fragment restoring the silhouette the no-shapes report
  flagged as lost. Length 1.25 times the existing sizeVar, scaled by
  the existing body pulse and spawn scale-in, girth 0.40 times sizeVar,
  gradient from the existing SHELL 0x57050A root to CORE 0xF23812 tip,
  peak alpha 0.97, envelope is the spawn scale.
- tendril through submitEnergy with ComplexTextures.TENDRIL: one torn
  wake from behind the body curving back 1.65 body lengths with an age
  based sway, width 0.30 times sizeVar, CORE at the root to SHELL at
  the tip, peak alpha 0.90.

Vertex estimate at distScale 1.0: shard 288 plus tendril 384 is about
670, inside the class M budget of 2500.

Complex particle layer: a continuous EMBER_FRAGMENT emitter on the
existing trail source, Preset.fixed with spread 0.07, rate 22 per
second, inside the continuous budget, ticked at 0.8 of the existing
envelope.

Filler layer unchanged: plasma head cluster, head flame, plasma trail,
material embers.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling the three owned
files, private output tags /tmp/dba-deviltrident-262 and
/tmp/dba-deviltrident-263. Full logs read, not summaries.

- cp262.txt (26.2): 4 errors, all baseline kinds outside this slot:
  DbaItems.java line 535, CreativeModeTab Output protected access (2),
  and DbaMenus.java line 16, MenuType constructor and MenuSupplier
  private access (2). Zero errors in the owned files. All owned class
  files produced, including the Emission inner classes.
- cp263.txt (26.3): 10 errors, all baseline kinds outside this slot: the
  same 4 as 26.2, plus DbaHostileEntity.java and PatrolRobotEntity.java
  swinging and attackAnim cannot find symbol (4),
  FriezaSoldierEntity.java swing method signature (1), and
  NamekianWarriorEntity.java hurtMarked cannot find symbol (1). Zero
  errors in the owned files. All owned class files produced.

Other checks: zero drawColoredBox, drawCylinder, drawDisc, drawSphere,
or drawHelix calls in the owned files (grep found none). git diff
--check clean on the three files. No em dashes in the edited files or
in this report. Every energy and fire piece goes through submitEnergy
with the matching canvas texture where the builder samples one; every
solid piece goes through submitSolid. Peak alphas on shader pieces are
0.90 to 0.95 and on solid pieces 0.97, with color carrying faintness
(dark shell roots), per the alpha-as-life rule.

## Foundation change requests

None. ComplexGeometry, ComplexTextures, MaterialEmitters, and
DbaParticles as shipped in Phase 1 covered every piece this slot needed.
No private toolkit, emitter, or texture was built.

One observation for the merge, not a request: flameTongues always rises
along local +Y. For the flying TridentProng that means the base flames
lick upward off the needle heads rather than streaming backward along
the flight line. The tendril wake carries the backward streaming read,
so the composition still works, but if a later pass wants
direction-aligned tongues, that is a shared toolkit variant (a base
plane or axis parameter on flameTongues), not something to fork per
owner.

## Worse check

Devil Slam seal edge, the known flag: in the particles-only version the
seal edge read as a density edge, a soft falloff in particle count at
the hit radius with no hard structure at the rim. The pieces fix this
directly. The fracture plates end at seeded, varied radii with real
skirt thickness, so the rim reads as broken ground ending, not as
particles thinning out. The crack web runs under the plates and stops
at varied lengths, giving a second, glowing edge inside the plate rim.
The edge is now the most structured part of the effect instead of the
least. One honest cost: plates and cracks are drawn in the local XZ
plane at the entity Y, so on sloped or stepped ground they can clip
into a rise or hover over a dip, where the old particle disk conformed
sample by sample. This is the same posture as every other ground slam
in this pass (ZGravitySlam and siblings), the real crater and brimstone
conversions the entity leaves are actual blocks that do conform, and
the filler disk still samples the same area, so the net read is
stronger than the particles-only version. Not worse overall.

Telegraph honesty, checked while sizing the pieces: at full size the
plate fan would have reached about 1.16 times the damage radius (plates
reach 1.10 times their fan radius, and the fan was first sized at the
1.05 seal radius). That would have drawn a picture wider than the
damage, the exact failure the renderer's own comments warn against. The
fan is sized at 0.90 of the seal radius and the crack web at 0.95, so
every piece stays inside the hit edge. Recorded here because the merge
should not re-size these back up without redoing this math.

TridentProng: the shard heads are spindles, widest near the middle
ring, while the original needles were slimmer. At 0.34 girth against
1.10 length they read as chunky hellfire barbs rather than thin
needles. That matches the reference picture's heavy, jagged trident
better than the old crescents did, and the dense plasma cluster still
sits at each head center, so the formation read (three points, center
largest) is unchanged. The one real risk is the +Y flame tongues noted
above reading as upward licks on a downward flying shot; the tongues
are short (0.55 times the radius factor) and the tendril wake dominates
the motion read. Judged not worse than the particles-only version,
where the heads had no silhouette at all.

TridentShard: the mesh restores a hard silhouette where the
particles-only version was a round cluster with no shape, which was
the flagged loss. The risk is the opposite one: a solid faceted body
could read too heavy next to the plasma cluster that still surrounds
it. The gradient root is the dark shell color and the cluster envelope
keeps its pulse, so the mesh sits inside the glow rather than replacing
it. Judged an improvement, and the closest of the three to the
pre-redo version's intent at higher piece quality.

Nothing in this slot reads worse than the particles-only state in a way
that needs a revert. The two watch items for in-game review are the
slam plates on sloped ground and the prong tongues on steep downward
shots.
