# Entity Objects Pass: TimeShatter conversion report

Owner slot: TimeShatter (Whis Staff's TimeShatter set). Spec:
audits/entity-audit.md section 6, contract audits/ENTITY-STANDARD.md.
HEAD 6155dda, no commits made.

## What moved

The frozen fan of five solid temporal glass shards is no longer drawn
by client/render/TimeShatterRenderer.java. It now exists as five real
FragmentEntity instances in FROZEN mode, hung server side by
item/WhisStaffItem.java at the moment each TimeShatterEntity spawns,
and it ends the way Carter named: at tick 36 every fragment releases,
drops under gravity, lands (with the foundation's SHARD_CHIP landing
puff), rests 10 ticks, fades over 12, and is gone by about tick 60
plus fall time, instead of vanishing with the host.

Replication of the drawn fan, point by point:

- Count and mode: 5 fragments per shatter, MODE_FROZEN, spawned at the
  host center, owner set to the caster.
- Angles: java.util.Random over the host seed with the 0xFA17 salt,
  angle_i = (i / 5) * 2 PI + (nextFloat - 0.5) * 0.55, consumed in the
  renderer's exact order. The salt constant moved from the renderer
  into WhisStaffItem (SHATTER_FAN_ANGLE_SALT), as the audit required.
- Mesh seeds: per-spoke seed = hostSeed ^ (0x5A1D + i * 131), re-homed
  as SHATTER_FAN_SEED_SALT. The old burst call's per-index seed salt
  adds 0 * 0x9E37 at index zero, so the synced fragment seed produces
  the identical ComplexGeometry shard silhouette the renderer drew.
  The int truncation is exact: the salt never reaches bit 31, so the
  sign-extended long the fragment renderer rebuilds from the synced
  int equals the long the burst call consumed.
- Sizes: unit = host scale * 0.90 (the old fanUnit). Length and girth
  roll server side over the shardBurst classes (55 percent at 0.30 to
  0.55 of unit, 30 percent at 0.55 to 1.05, 15 percent at 1.05 to
  1.60; girth 0.30 to 0.44 of length), mirroring the foundation's own
  private roll, seeded per fragment from its mesh seed.
- Colors: root 0xFFFFFF, tip 0x65D8FF, the two-tone glass split,
  unchanged.
- Release: RELEASE_AT preset to the host's lifetime via
  host.getMaxLifetime() (36). Rest ticks 10, fade 12 (foundation
  constant), per the audit.
- Plane: the drawn fan lived on each client's camera plane. The
  entity fan is world-anchored on the plane perpendicular to the
  caster's look direction at the cast moment (right = look cross
  world up, plane up = right cross look), which the item knows server
  side. The basis is built so the caster sees the same chirality the
  billboard showed: angle zero on the caster's right, positive angles
  rising. The audit records this trade as accepted.

The two renderer-only seed constants (0xFA17, 0x5A1D) now live server
side in WhisStaffItem; grep finds neither salt, nor any shard,
shardBurst, or submitSolid call, left in TimeShatterRenderer.

## Spawn sites (all three, confirmed in code)

Every TimeShatterEntity construction in the mod is in WhisStaffItem;
all three sites now call spawnShatterFan immediately after the host
is added:

1. hurtEnemy (left-click melee hit): host at the target's center,
   scale 1.2, caster is the attacking ServerPlayer.
2. performTemporalStrike (the staff's own strike): the stepped chain
   of three hosts at distances 1.2, 2.6, 4.2 with scales 0.8, 1.05,
   1.4. Each host in the chain gets its own five fragments with its
   own seed and its own fan unit (0.72, 0.945, 1.26), so a full strike
   hangs 15 fragments across the three shatters.
3. applyStaffHit (the extended strike's landed hit): host at the
   target's center, scale 1.2.

No client-side spawning anywhere; the renderer only lost calls.

## Host and renderer: what did not change

- entity/TimeShatterEntity.java: untouched. The 36 tick life, the
  synced SEED roll, the synced scale, and the discard beat are exactly
  as at HEAD; the spec put the spawn on the item, so the host needed
  no bookkeeping.
- TimeShatterRenderer: only the fan left. The shader crackFan on the
  shatter plane, the SHARD_CHIP chips along the crack spokes, the
  glass dust streams (including the gravity scale 0.1 suspension
  wiring), the plasma and soul core, and the color-split ghost streams
  are byte-for-byte the HEAD code paths. The renderer vertex worst
  case drops from 2232 to 792 (the crackFan alone), still class M.
- Host damage, cooldowns, Ki costs, timings, and synced fields:
  identical. The WhisStaffItem diff adds spawn calls, one helper, two
  imports, and four constants; the three host constructions keep
  their exact arguments (sites 2 and 3 now hold the instance in a
  local so its seed can be read, nothing else). Fragments contain no
  damage calls and deal no damage in any mode.

## Counts before and after

- ComplexGeometry.shard / shardBurst calls in TimeShatterRenderer:
  5 (plus their submitSolid wrapper) before, 0 after. Merge gate 1
  satisfied for this host.
- FragmentEntity.hang calls in WhisStaffItem: 0 before, 5 per shatter
  from 3 spawn sites after.
- Entities per shatter: 1 before, 6 after (host plus 5 fragments).
  Worst case in one area: chained strikes and melee hits are bounded
  by the foundation area cap of 256 per 96 blocks; the authored
  cadence (5 per shatter) sits far under it.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
src/main/java:src/client/java, private /tmp tag ts03be6479) over the
two owned files, WhisStaffItem.java and TimeShatterRenderer.java:

- cp262: 4 errors, all baseline closure kinds (DbaItems x2,
  DbaMenus x2). Zero in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity kinds). Zero in owned
  files.

git diff --check clean on both owned files. Added-lines em dash scan
(byte level, UTF-8 E2 80 94 and E2 80 93): zero. No Gradle build run
or claimed; the in-game look is unverified until Carter's PC test.

## Change requests (foundation gaps, filed not fixed)

1. FragmentRenderer's FROZEN growth scales the drawn length only.
   The drawn fan it replaced rolled girth from the already grown
   length each frame, so its shards thickened along with the travel
   curve; fragment shards keep their spawn girth (times the scale-in)
   while the length grows to 1.40. Applying the same growth factor to
   drawGirth in FragmentRenderer would close the gap. Foundation
   file, read-only for this pass.
2. FragmentRenderer applies the five tick spawn scale-in to FROZEN
   fragments on top of the growth curve, while the standard's text
   describes scale-in as applying except for FROZEN. The first two or
   three ticks therefore read a touch smaller than the drawn fan's
   0.55 growth floor. One-line foundation decision either way.
3. FragmentEntity.hang takes explicit length and girth, so each host
   re-implements the size class roll (the foundation's lengthFor and
   girthFor are private). A public static roll helper on
   FragmentEntity would keep all seven hosts on one implementation
   instead of seven mirrors that can drift.

## Honest worse check

- The accepted trade, stated plainly: the fan is world-anchored now,
  not billboarded. From directly behind or edge-on, the five shards
  read as a thin plane of glass where the billboard always turned to
  face you. From the caster's viewpoint, the common case since they
  just cast it forward, the fan matches the old read by construction,
  and every other player now sees the same physical fan instead of
  their own private camera plane. This is the physical truth the pass
  exists for, and the audit recorded it in advance.
- The drawn fan jittered each shard inside a 14 degree cone and
  offset its origin by up to 0.12 of unit; the audit spec replaces
  that with the exact center and the exact fan axis, so the entity
  fan reads marginally more regular, a clean five-spoke star, than
  the jittered drawing did. Small, and it is the spec's own choice,
  but it is the first thing I would soften if Carter dislikes it in
  game (the fix would be an audit change, not a local tweak).
- The girth growth gap in change request 1 makes late-hang shards
  read slightly slimmer relative to their length than the drawn fan
  at the same age. Subtle at these sizes; named here so the PC test
  can judge it with the cause attached.
- Nothing else regressed that I can find: the crack fan, chips, dust,
  and core are untouched code paths, and the fragments add the drop,
  landing, and rest the host never had. Nothing was reverted.
