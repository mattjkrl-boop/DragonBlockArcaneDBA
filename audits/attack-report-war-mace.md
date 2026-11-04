# Attack Upgrade Pass, Phase 2 Owner Report: War Mace (crater rim wall)

Spec: audits/attack-complexity-audit.md section 22. Builder contract:
audits/attack-foundation-addendum.md. Base HEAD ccfebf2. No commits.
Files changed, all War Mace only:

- src/client/java/com/dragonblockarcanedba/client/render/WarMaceSmashRenderer.java
- src/client/java/com/dragonblockarcanedba/client/render/WarMaceRiseRenderer.java
- src/main/java/com/dragonblockarcanedba/entity/WarMaceSmashEntity.java

No damage number, crater radius, crater cap (220), destruction tier,
smash timing, cooldown, cost, or synced field changed anywhere.
PermanentDestruction is untouched. Every new physical piece draws
through submitSolidLit; nothing new sits on the emissive path.

## WarMaceSmashRenderer: the crater rim wall

Before: 2 piece calls (fracturePlates about 480 vertices, crackFan
under 900), 15 material emitters, one 48-chip burst on the first
frame. Pieces total under about 1400 of the class L 6000 budget.

After: 4 piece calls. The plates and crack fan are byte-identical;
the wall adds 2 rockWall calls, 12 slabs total (inside the spec's 10
to 13), 864 vertices, for a pieces total under about 2250.

Composition (built to Carter's steering and quality notes: jagged
clustered rock heaving out of the ground, not a neat ring of even
slabs; fewer, bigger, deliberate pieces):

- Main arc: 6 slabs, length 7.2 (about 1.2 blocks per slab, chunky
  rather than fence-like), centered 0.20 radians off the caster's
  forward at the rim radius (4.0 normal, 5.0 heavy, the outer edge of
  the fracture ring the entity converts). Height 1.8 (2.1 heavy);
  with the builder's seeded top variance the tops run about 1.3 to
  2.1 (1.5 to 2.4 heavy), so the profile is an uneven mountain line,
  not a parapet.
- Cluster arc: 6 slabs, length 5.6, centered at -0.95 radians at 0.80
  of the rim radius, height 1.3 (1.55 heavy). It shoulders inside the
  main arc's flank, lower and closer in, so the rim reads as rock
  that heaved in two uneven pushes. Combined coverage is about 167
  degrees of the rim; the wall is a clustered arc group, not a full
  ring (see the worse check).
- Both segments face the crater center; the builder's arc parameter
  (length over radius) wraps them around the rim circle exactly.
- Palette is ground-sampled: the map color of the block under the
  impact point (grass samples as its dirt body), desaturated a third
  toward its own luminance so it reads as earth and stone, root
  darkened to 0.45 for the buried base. Drawn through submitSolidLit
  with the frame's light coords: the wall is lit by the world like
  the ground it came out of, dark at night, shaded on its faces.
- Beat, on the addendum's factor contract over the composition's own
  age: riseFactor smoothsteps over the first 30 percent of life
  (3 ticks normal, about 4 heavy), HOLD at full height through age
  life * 0.55 (the envelope peak), then sinkFactor runs the SLAM
  over 3 ticks. The wall draws inside the same groundNear gate as
  the plates, so a falling heavy smash shows no floating wall.
- The end of the slam is the entity burst below, so the wall ends
  as rubble, not as a fade.

Sprite trims (spec: trim slightly, 58 emit calls was the mod's
highest, keep the wall readable). Emitter count unchanged at 15;
rates cut about a tenth on the six largest filler emitters (values
before the heavy multiplier): disc dust 230 to 205, rim dust A 200
to 180, rim dust B 115 to 104, shell dust 165 to 148, core plasma
120 to 108, plume dust 150 to 135. First-frame SHARD_CHIP burst 48
to 42. No layer removed, no other sprite touched.

## WarMaceSmashEntity: the slam-end debris burst

A renderer cannot call the server-side DebrisSpawner, so the burst
the spec ends the wall with lives here, as presentation only. A
grounded doImpact arms a one-time burst: at tick ceil(life * 0.55
+ 3) (tick 9 normal, tick 11 heavy, matching the renderer's slam
end age) the entity fires DebrisSpawner.spawnField with exactly 12
chunks, radius 3.4 (4.2 heavy), rise 1.8, orbit 22, palette sampled
from the ground by the spawner itself. The check sits after the
impact block in tick(), so a final-tick impact still gets its burst
on the same tick. Ungrounded (air) impacts arm nothing. The crater
call, the fracture disk, the impact debris field (24 or 36 chunks),
all damage code, and all timing are byte-identical to HEAD.

## WarMaceRiseRenderer: plates ring and crackFan branches

Before: 2 piece calls (fracturePlates radius 1.05 about 480
vertices, crackFan radius 1.35 with 9 base branches). After: 3
piece calls.

- Added one fracturePlates ring: radius 1.62, lift 0.09, GHOST_MID
  to the same dark earth tone, alpha 0.88, its own seed salt. The
  base now reads as two uneven heave rings (inner fan higher,
  outer ring lower and wider). About 480 more vertices; worst case
  pieces total about 2000 of the class M 2500 budget.
- crackFan branches plus 2: the builder has no branch count
  parameter; it derives the count from distScale (segments(9,
  distScale, 6) plus a seeded 0 to 2). This call now passes
  distScale * 1.22, giving 11 base branches at full detail instead
  of 9, scaling down with distance like everything else. See
  change request 1 for the side effect.

Sprites in the rise: unchanged.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
src/main/java:src/client/java) over the three owned files, FULL
logs read, never summaries:

- cp262: 4 errors, all standing baseline kinds (DbaItems x2,
  DbaMenus x2). Zero errors in owned files. This also confirms the
  BlockState.getMapColor and MapColor.col calls compile against
  the real 26.2 jar.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors
  in owned files.

One comment-only edit to the Rise renderer's class doc landed after
the javac runs (sentence placement, no code tokens); the scoped
checks below were re-run after it. git diff --check clean on the
owned files; byte-level em dash scan on owned added lines: zero.
No Gradle build run or claimed; the in-game look is unverified
until Carter's PC test.

## Change requests (foundation is read-only)

1. crackFan has no explicit branch count. Branches plus 2 was
   achieved by inflating the call's distScale, which also raises
   the per-crack walk segments from 6 to 7 at full detail, so
   existing cracks walk one segment longer than before. An
   explicit branch-count parameter (or an overload) would make
   the audit's recurring "branches plus N" specs exact.
2. rockWall's sink is vertical only. The audit says the wall
   slams "down and inward"; the builder has no radial component,
   so the inward read here comes from the arc-centered placement
   (the slabs sink around the bowl) and the rim burst, not from
   the geometry moving inward. A radial shrink on sink, or the
   merge's planned irregularity upgrade, could close this.
3. Timing note, not a gap: the ground smash has no pre-impact
   telegraph entity (its impact resolves server side on tick 1),
   so the wall's RISE keys to the renderer composition's build
   rather than to a pre-impact window. The before-and-after reads
   inside the composition's own life: heave, hold at the peak,
   slam, rubble. If Carter wants a true pre-impact telegraph for
   the ground smash, that is a new entity beat, which the spec's
   "smash timing unchanged" rule forbids in this pass.

## Honest worse check

- Coverage: the wall wraps about 167 degrees of the rim (front and
  one flank), not the full circle. That is the deliberate result
  of the 10 to 13 slab spec plus Carter's clustered-not-neat
  steering: 12 big slabs cannot ring a 25-block circumference
  without turning into small even fence pieces, which is exactly
  what he rejected. The back rim keeps the plate and crack
  composition it already had. Flagged for the merge realism
  review as the composition's main judgment call; nothing about
  the plates or cracks got worse.
- Heavy falls: the wall shares the plates' groundNear gate, so
  on a short fall the RISE can be partly compressed (the wall is
  already partway up when it first draws). The ground smash, the
  common case, plays the full beat. Same trade the plates already
  lived with; not new.
- The Rise crackFan side effect in change request 1 is real: the
  rise's existing cracks are slightly longer-walked at full
  detail than at HEAD. It reads as more crack, in theme, and the
  branches spec demanded touching this call; named here so the
  merge can judge it with the cause attached.
- The wall palette samples the single block under the impact
  center; on mixed ground the whole wall takes that one block's
  tone. Real heaved rims do read as one soil type per spot, and
  the alternative (per-slab sampling) is builder work. Acceptable.
- Nothing was reverted. No pre-existing layer was removed, no
  count reduced beyond the specced sprite trims, and the weapon's
  damage, crater, and timing are untouched.
