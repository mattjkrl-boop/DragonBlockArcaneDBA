# Attack Upgrade Pass: Spirit Sword owner report

Owner slot: Spirit Sword (audit section 11, worst offender 1).
HEAD ccfebf2, no commits made. Files changed: exactly three,
SpiritCleaveRenderer.java, SpiritImpaleRenderer.java,
SpiritCannonBeamRenderer.java. No entity, item, or foundation file
was touched. All changes are presentation only: no damage, cooldown,
cost, timing, mechanic, or synced field changed anywhere, and the
cleave arc, range, impale timing, and beam damage path and width
are byte-stable in behavior (the renderers only read state the
entities already publish).

## SpiritCleaveRenderer: full rebuild (was THIN, worst offender 1)

Before: the entire pieces layer was two tendril ribbons in one
submitEnergy call (768 piece vertices), and the sprite layers were
six emit calls of soft filler (soul, vapor, one SPARK, one WISP_CURL
stream). No solid geometry, no impact composition at all.

After:

- Pieces body: the two tendrils are replaced by one tendrilFan of 5
  strands (soul cyan root to white tip) woven along the flight line
  through submitEnergy with the tendril canvas: 5 tendrils at 384
  vertices, 1920 of the 2500 class M budget for that submit.
- Leading edge shards: a shardBurst of 4 soul shards (unit 0.52 of
  the arc length) rides the arc's leading edge point, drawn through
  submitSolidLit with the render state's lightCoords because crystal
  is physical matter: 4 shards at 288 vertices, 1152 of 2500.
- Impact spikes: a spikeEruption FAN of 5 soul crystal spikes
  (submitSolidLit, pale cyan glass: deep saturated base 0xFF3FA7C6
  where the crystal is thick to near clear 0xFFEFFBFF at the tips)
  erupts from the ground at the hit point in the swing's direction.
  Per Carter's steering during this pass, the set is composed as a
  tight cluster of fewer, bigger pieces rather than a wide uniform
  fan: count 5, radius 1.05 so the spikes bunch at the hit point,
  sizeScale 0.85 so individual spikes run about 1.0 to 2.7 blocks
  with the builder's three uneven length classes, bent spines, and
  per-spike rise stagger doing the irregularity. 5 spikes at 384
  vertices, 1920 of 2500.
- The hit beat is real, not invented: SpiritCleaveEntity already
  syncs LIFE, and the server sets it to the stop tick plus FADE_TICKS
  when the flight ends, so the renderer derives the stop tick as
  life minus FADE_TICKS and the stop point as position plus motion
  times the remaining flight. Factors follow the addendum contract:
  RISE over 4 ticks ending at the hit, HOLD 3 ticks, then a
  crack-and-fade settle through the envelope with sinkFactor held at
  0. If a wall ends the flight early, the synced LIFE snaps and the
  renderer restarts the rise at the true hit point instead of popping
  in fully risen. The ground under the stop point is found by a
  client side collision scan (one block above to ten below); a swing
  that stops over open air grows no spikes.
- Sprite layers: all three soul streams doubled in rate and budget
  (215 to 430 and 260 to 520 on the core volume, 175 to 350 and 220
  to 440 on the path, 165 to 330 and 200 to 400 on the tip flash),
  and two new bursts fire at the hit point across the hit beat:
  SPARK_STREAK at 120 per second and SHARD_CHIP at 90 per second,
  ramping in over the last tick and a half of flight and settling
  with the spike envelope. Emit calls: 6 before, 8 after.
- Total pieces after: 1920 (energy) + 1152 (lit shards) + 1920 (lit
  spikes) = 4992 vertices across three submits, each submit inside
  its class M budget.

## SpiritImpaleRenderer: additive upgrade

Before: 5 shards (1 main + 4 ring) at 288 vertices (1440) plus 2
tendril wake strands at 384 (768), 2208 piece vertices total.

After:

- Shard count per impale plus 1: the ring goes from 4 shards to 5
  (angle divisor updated to match). Shards now 6 at 288, 1728 on the
  solid submit.
- Main shard one size class up: length times 1.22 and girth times
  1.45 against the pillar dimensions it had, so the central crystal
  stands stouter and spears above the particle pillar crown. The
  wake tendrils now key their control and top points to the main
  shard's new length and girth so the composition stays coherent.
- Tendril wake strands doubled: 2 to 4, spaced at quarter turns.
  Wakes now 4 at 384, 1536 on the energy submit.
- Total pieces after: 1728 + 1536 = 3264, each submit inside the
  class M budget (2500 per submit per the addendum accounting).
- Judgment call, flagged for the merge: the impale shards stay on
  their existing submitSolid path. They are a pre-existing soul
  energy composition (soul gradient spikes inside a soul pillar),
  the audit spec for the impale names no relight, and the Phase 1
  lit rule governs physical palettes. The one added ring shard
  extends that same existing composition rather than introducing a
  new physical piece. Every genuinely new physical piece in this
  slot (the cleave's crystal spikes and leading edge shards) went
  through submitSolidLit as required. If the merge wants the impale
  shards relit too, it is a one-line submit swap in this file.

## SpiritCannonBeamRenderer: polish only

The terminus tendrilFan goes from 6 strands to 7, per spec. Core
chain unchanged. Vertex count: core at most 3072 plus fan 7 at 384
(2688), 5760 of the 6000 class L budget. Beam damage path, width,
envelope, first person anchor, and fade retract untouched.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
sourcepath src/main/java:src/client/java) over the three edited
files, run against the final file contents, full logs read line by
line (never summaries):

- cp262: 4 errors, all baseline closure kinds (DbaItems x2,
  DbaMenus x2). Zero in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero in owned
  files.

An earlier compile round during this slot returned the identical
baseline-only sets; the final round covered the post-steering file
contents. git diff --check clean. Byte-level added-lines em dash
scan (UTF-8 E2 80 94 and E2 80 93) over all three files: zero.
No Gradle build run or claimed; the in-game look is unverified
until Carter's PC test.

## Change requests

None against the foundation. The spikeEruption and tendrilFan
signatures covered the spec as written. Note for the merge: Carter's
steering says spikeEruption gains a signature-compatible
irregularity upgrade at the merge (clustered placement, spacing and
height jitter). The cleave call site is ready for it as written:
count 5, FAN layout, radius 1.05, sizeScale 0.85, standard factor
arguments, no local workarounds to unwind.

## Honest worse check

- The cleave body changed character. The old two tendrils bowed to
  one side as a curved arc with a gold second strand; the tendrilFan
  is symmetric around the flight line and fans from a single rear
  anchor, so the body now reads as a woven straight blade. That
  matches the entity's own design note (a straight divine blade, not
  a crescent), and the gold survives in the vapor stream, but the
  one-sided bow is gone. This is the one deliberate trade in the
  rebuild and the first thing to judge in game.
- Spike beat on early wall hits: the full RISE 4, HOLD 3, settle
  recipe plays exactly only on a full flight, because the host's
  total life is a fixed 19 ticks and its timing may not change. When
  a wall stops the blade early, the rise restarts at the true hit
  point but only the 5 fade ticks remain, so the rise plays clipped
  and the hold and settle compress into the fade. The eruption still
  lands on the hit; the full beat does not complete. On full flights
  the spikes also begin rising during the last 4 ticks of travel at
  the predicted stop point, so the ground starts splitting a moment
  before the blade arrives. That telegraph is the cost of fitting
  the specced beat inside the host's fixed lifetime without touching
  entity timing.
- The clustered spikes interpenetrate each other near the hit point
  where their bases bunch. That is deliberate (real crystal growth
  interpenetrates, and Carter asked for a clustered eruption over a
  uniform bar), but it is a read to confirm on the PC test.
- The impale's main crystal now crowns about a fifth taller than
  the soul particle pillar it rises from, with the wakes following
  it. That is the specced size class step; if the pillar was the
  point of the composition, the crown is the change to judge.
- Doubled soul streams make the cleave noticeably denser at point
  blank range. Rates and budgets were doubled exactly per spec, so
  the density is the spec's intent, stated here so the PC test can
  judge it with the cause attached.
- Nothing was reverted. No part of this slot shipped silently: the
  trades above are the complete list I know of.
