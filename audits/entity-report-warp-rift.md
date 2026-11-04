# Entity Objects Pass: Warp Rift (Dimensional Sword) conversion report

## What moved

The Warp Rift's shard fan is no longer drawn. `DimensionalWarpRiftRenderer`
held one `ComplexGeometry.shardBurst` call inside `submitSolid` (4 shards at
local (0, 1.4, 0), axis up, spread 68 degrees, unit 0.70, burst seed
`riftSeed ^ 0x5A17`, root 0x1A0533, tip 0xE8CCFF, alpha 0.95). That call is
deleted. `DimensionalWarpRiftEntity` now spawns the fan as real
`FragmentEntity` pieces server side on its first tick (`tickCount == 1`, the
same beat as the spawn audio cue), via `FragmentEntity.burst` with the audit
spec exactly:

- center: host position + (0, 1.4, 0), the tear's heart
- axis (0, 1, 0), spread 68 degrees, unit 0.70, count 4
- burst seed `getSeed() ^ 0x5A17L` (same seed family; the helper salts each
  fragment's mesh seed with the standard 0x9E37 derivation)
- root 0x1A0533, tip 0xE8CCFF
- mode BURST, speed 0.12 to 0.32, gravity scale 0.6, rest 8
- lifeTicks 60 for the origin rift (host 34 plus fallout), 72 for the
  destination rift (host 46 plus fallout)
- owner: the rift's owner, so caster-exempting systems exempt these too

Both rifts spawn their own fan: the origin rift is constructed by the item
and the destination rift inside the origin's `doBurst`, and both run the
same first-tick spawn, giving 4 fragments per rift and 8 per warp, once,
matching the audit counts and caps. Fragments deal no damage (foundation
contract) and no host damage, cooldown, cost, timing, mechanic, or synced
field changed: the host diff is one added spawn block and its comment.

## Counts before and after

- `ComplexGeometry.shard` / `shardBurst` calls in DimensionalWarpRiftRenderer:
  1 before, 0 after (merge gate 1 satisfied for this host).
- Renderer-drawn shards: 4 before, 0 after.
- FragmentEntity spawns: 0 before, 4 per rift (8 per warp) after.
- The `tearGeoEnv` local existed only to gate the fan and was removed with
  it. The class javadoc and the pieces-layer comment were updated to say
  two builders and to record where the fan went; the vertex accounting now
  reads tendrils 768 plus crack fan about 450, worst case near 1220.
- Everything else in the renderer is untouched: both inward tendrils, the
  ground crackFan, the WISP_CURL layer, and every MaterialEmitters line are
  byte-for-byte as they were (the diff shows only the fan block, the one
  variable, and comments).

## Spec note (fade length)

The audit lists "fade 10" for this host. ENTITY-STANDARD fixes FADE_TICKS
at 12 for every fragment as a foundation constant, explicitly not
per-fragment tuning, and the foundation is read-only in this pass. The
conversion follows the standard: fragments fade over 12 ticks. Recorded
here so the merge sees the discrepancy was noticed and resolved in favor
of the binding standard, not overlooked.

## Change requests

None against `entity/FragmentEntity.java` or its renderer. The burst
helper covered the spec as written (cone, seed family, size classes,
gravity scale, rest, per-host life) with no workaround needed.

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java, private
/tmp tags warp7bb65205) over the two owned files:

- cp262: 4 errors total, all in the known baseline closure files
  (DbaItems.java, DbaMenus.java). Zero errors in owned files.
- cp263: 10 errors total, all in the known baseline closure files (the
  same two plus DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity). Zero errors in owned files.

`git diff --check` clean on both files. Added-lines em dash scan: 0.
No commits made.

## Honest worse check

- The read changes by design, per the audit: the drawn fan hung at the
  tear, shaped by the tear open-and-shut envelope for the rift's whole
  life. The entity fan scatters on tick 1, sinks under 0.6 gravity, lands,
  rests 8, and can outlive the rift inside its fallout window (fragment
  life 60/72 against host life 34/46). That is the specified conversion,
  not a drift from it.
- One genuine risk: appearance abruptness. The drawn fan faded in with
  the tear envelope over the first fifth of the rift's life; fragments
  appear on tick 1 at full presence, softened only by the foundation's
  spawn scale-in (0.5 to 1.0 over 5 ticks). In practice the fan sits
  inside the tear volume on the same tick as the teleport whoosh, the
  vortex pull, and the heart cluster, so the pop is masked. If it reads
  abrupt in game, the fix belongs in the foundation's scale-in, not in
  this host.
- Fragments now land with the foundation's two-count SHARD_CHIP landing
  puff, which the drawn fan never had. It is physical feedback for a
  physical landing and reads as an improvement, but it is a small visual
  addition and is recorded as one.
- Per-shard sizes are rolled server side over the standard class ranges
  instead of the drawn burst's unit scaling; silhouettes match by the
  shared seed family. This is the same trade every burst conversion in
  this pass makes.
- Nothing was reverted, and no part of the conversion reads worse in a
  way that can be fixed inside this host's scope.
