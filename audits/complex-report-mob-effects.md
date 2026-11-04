# Complex Effects Redo, Phase 2 Report: Mob Ability Effects

Owner slot: Mob Ability Effects (all class S, no geometry anywhere
in this slot, as assigned). Base: HEAD 9178eac. Files edited, and
only these three:

- src/main/java/com/dragonblockarcanedba/entity/FriezaSoldierEntity.java
- src/main/java/com/dragonblockarcanedba/entity/SaibamanEntity.java
- src/client/java/com/dragonblockarcanedba/client/fx/StaggerFx.java

No commits. No damage number, cooldown, cost, timing, mechanic,
or synced data field changes. Mob AI, attack behavior, the
self-destruct damage and timing, and the BossStaggerPayload
network shape are all untouched. The d74f13a destruction wiring
is not touched. Split source sets stay clean: the two entity
files are common code, emit only through the DbaParticles types
they already had access to via ServerLevel.sendParticles, and
reference no client class; StaggerFx is client code and emits
through the MaterialEmitters path it already used.

Every pre-existing material emission line is kept verbatim as
the filler layer. The only additions are the assigned complex
sprite layers below, at the existing feedback points.

## Frieza Soldier ranged hit feedback (class S)

Complex sprite layer added: SPARK_STREAK, no pieces.

- One new ServerLevel.sendParticles call with
  DbaParticles.SPARK_STREAK, placed directly after the existing
  PLASMA material call in aiStep and using the exact same hit
  point (target.getX(), target.getEyeY(), target.getZ()), the
  same count (5), the same spread (0.2, 0.25, 0.2), and the same
  speed (0.02) as that plasma feedback.
- Total complex sprites per shot: 5, inside the class S burst
  budget of 12.
- The VAPOR call after it, the 40 tick shot cooldown, the 5.0f
  ki damage, the swing, the sound, and the range and line of
  sight checks are unchanged. SPARK_STREAK is a fixed style
  sprite (white-gold streaks), so it adds no new theme color;
  the gold plasma theme color is unchanged.

## Saibaman self-destruct burst (class S)

Complex sprite layer added: WISP_CURL and ASH_FLAKE, no pieces.

- Two new ServerLevel.sendParticles calls at the end of the
  existing burst() emission block, before discard(): one with
  DbaParticles.WISP_CURL (count 6) and one with
  DbaParticles.ASH_FLAKE (count 6). Both use the exact same
  burst point (this.getX(), this.getY() + 0.6, this.getZ()),
  spread (0.4, 0.4, 0.4), and speed (0.04) as the material
  burst calls above them.
- Total complex sprites per burst: 6 plus 6 equals 12, at the
  class S burst budget of 12 and not over it.
- The physical slime chunks stay physical and are not
  replaced: the BlockParticleOption call with
  DbaParticles.BLOCK_CHUNKS and Blocks.SLIME_BLOCK (count 18)
  is unchanged, as are the green VAPOR (10) and DUST (8)
  material calls, the 8.0f burst damage, the 2.5 inflate area,
  the health and target trigger, and the discard timing.

## Hostile boss stagger feedback (class S, StaggerFx)

Complex sprite layer added: SPARK_STREAK and SHARD_CHIP burst,
no pieces.

- Two new MaterialEmitters.burst calls in emitImpact, after
  the existing dust and plasma bursts, both through
  MaterialEmitters.Preset.fixed with DbaParticles.SPARK_STREAK
  (spread 0.26) and DbaParticles.SHARD_CHIP (spread 0.20), both
  with Vec3.ZERO base velocity and both sourced from the same
  MaterialEmitters.sphereVolume(center, radius) as the
  existing bursts, so the sprites land at the same center and
  radius as the current feedback.
- Counts scale with the payload strength the same way the
  existing bursts do: each is 3 plus round(2 times strength),
  capped at 6 with Math.min, so the combined complex burst is
  6 to 10 at the strengths the server sender produces (0.35 to
  1.2) and can never exceed 12 for any payload value. That is
  the class S burst budget.
- Neutral color, as assigned: Preset.fixed carries no theme
  color argument, and both sprite types have fixed style
  colors (white-gold streaks, stone gray chips), so the new
  layer introduces no theme. The BossStaggerPayload shape is
  unchanged, no theme field was added, and the deferred
  payload theme extension stays deferred. The existing dust
  (0xC2B49A) and plasma (0xFFCA6A) bursts, the strength and
  progress store, and the tick logic are unchanged.

## Verification

Targeted javac with JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
cp from ~/workspace/dba-audit-libs/cp262.txt and cp263.txt,
sourcepath src/main/java:src/client/java, compiling only the
three owned files. Baseline logs were captured before the edits
(/tmp/dba-mob-effects-baseline262.log and
/tmp/dba-mob-effects-baseline263.log) and post-edit logs after
(/tmp/dba-mob-effects-262.log and /tmp/dba-mob-effects-263.log,
output dirs /tmp/dba-mob-effects-262 and /tmp/dba-mob-effects-263).
Full logs were read and diffed against baseline:

- 26.2 classpath (cp262.txt): 4 errors, identical to baseline.
  All four are outside the owned files: DbaItems
  CreativeModeTab Output protected access (two) and DbaMenus
  MenuType constructor and MenuSupplier private access (two),
  reached through the sourcepath. Zero errors in the three
  owned files, and the post-edit log is byte identical to the
  baseline log. All three owned class files were produced
  (FriezaSoldierEntity, SaibamanEntity, StaggerFx).
- 26.3 classpath (cp263.txt): 10 errors, the same error kinds
  as baseline, zero new error kinds and zero errors
  attributable to these edits. The set is: the same DbaItems
  (two) and DbaMenus (two) kinds as 26.2, DbaHostileEntity
  swinging and attackAnim cannot find symbol (two),
  NamekianWarriorEntity hurtMarked cannot find symbol (one),
  PatrolRobotEntity swinging and attackAnim cannot find
  symbol (two), and the pre-existing FriezaSoldierEntity kind
  (one): no suitable method found for swing(InteractionHand)
  at the swing call in aiStep. That FriezaSoldier kind is the
  baseline kind named in the assignment; it exists in the
  baseline log at line 51 and in the post-edit log at line 52,
  shifted by exactly the one added DbaParticles import line.
  The log diff between baseline and post-edit 26.3 is that
  single line number and nothing else. The new SPARK_STREAK
  sendParticles line in FriezaSoldierEntity produced no error
  on either classpath, and SaibamanEntity and StaggerFx have
  zero errors on both classpaths.
- git diff --check is clean on the three owned files. Added
  lines scan: zero em dashes in the code, comments, and this
  report. No client class is referenced from either entity
  file. No ComplexGeometry call was added anywhere in this
  slot, as assigned (class S, no pieces).

## Foundation change requests

None. The Phase 1 foundation covered this slot completely:
the five complex sprite types are registered in the common
DbaParticles, ServerLevel.sendParticles accepts them directly
for the common entity files, and MaterialEmitters.Preset.fixed
plus burst covered the client file. No private emitter, no
private toolkit, and no workaround was needed.

## Honest worse check

Frieza Soldier: the risk is spark streaks reading as a second,
busier hit flash on a shot that fires every 40 ticks. The
streaks are only 5 sprites at the exact plasma point, count,
spread, and speed, so they sit inside the flash that already
exists rather than forming a separate effect, and their
white-gold fixed color is close to the existing 0xFFD98A
plasma. Verdict: not worse than the particles only version.

Saibaman: the risk is the pale WISP_CURL and gray ASH_FLAKE
sprites diluting the green plant burst read. The green is
still carried by the unchanged slime chunks (18), green vapor
(10), and green dust (8); the 12 neutral sprites are the ash
and curl debris a bursting body would throw, at the same
point and spread, so they read as remains inside the burst
rather than a recolor of it. The total added count is at the
budget cap but well under the 36 particles the burst already
emitted. Verdict: not worse.

StaggerFx: the risk is shard chips implying a physical armor
break on bosses whose stagger is a rock and dip animation.
The chips are stone gray, capped at 6, mixed with an equal
number of spark streaks inside the same sphere volume as the
existing dust burst, which already reads as impact debris;
no new color enters, so the neutral heavy hit read is kept.
Counts grow with strength only as far as the existing dust
and plasma counts already do. Verdict: not worse.
