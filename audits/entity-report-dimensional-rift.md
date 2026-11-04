# Entity Objects Pass: Dimensional Rift (Sickle of Sorrow) conversion report

Owner slot: Dimensional Rift. Spec: audits/entity-audit.md section 3.
Contract: audits/ENTITY-STANDARD.md. HEAD at start: 6155dda. No commits.

## What moved

The tear's shard burst is no longer drawn by the client renderer. It is
5 real FragmentEntity instances in BURST mode, spawned server side by
the host, and drawn by the shared fragment renderer.

### entity/DimensionalRiftEntity.java (+22 lines, one added block)

A new fragment beat inside the server branch of tick(), at
tickCount == 2, on the same early beat the debris field rides:

- Spawn center: the rift position plus the tear heart height for the
  radius at that tick, tearY = min(8.0, 2.5 + radius * 0.4) * 0.5.
  Radius at tick 2 is about 3.34, so tearY is about 1.92, matching the
  audit's "about 1.9".
- FragmentEntity.burst(serverLevel, owner, center, axis (0, 1, 0),
  spread 100 degrees, unit 0.80, count 5, burstSeed (below),
  root 0x24053D, tip 0xB84DFF, speed 0.10 to 0.30, gravity scale 0.5,
  lifeTicks 70, restTicks 8). Owner is the rift's owner (the caster),
  per the standard.
- Burst seed: the renderer seed ^ 0x5A17L, where the renderer seed is
  recomputed server side from the same state the client derives it
  from: (long) id * 2654435761L ^ (long) (x * 64.0) * 40503L ^
  (long) (z * 64.0) * 0x9E3779B9L. FragmentEntity.burst derives each
  fragment's mesh seed as burstSeed + index * 0x9E37L, exactly the
  salt ComplexGeometry.shardBurst used, so every fragment draws the
  identical silhouette the renderer burst drew.
- Nothing else in the entity changed: damage (burst 500 at tick 20,
  tick damage 60), Melting and Sorrow Rift application, vortex pull,
  collapse fling at tick 50, debris field, ground scarring, sounds,
  lifetime 60, and both synced fields (RADIUS, IS_COLLAPSING) are
  byte-identical outside the added block. Fragments carry no damage
  calls (foundation guarantee, and none added here).

### client/render/DimensionalRiftRenderer.java (shard block removed)

- The ComplexGeometry.submitSolid shardBurst call (5 shards at local
  (0, tearY, 0), axis up, spread 100, unit 0.80, root 0x24053D, tip
  0xB84DFF, alpha 0.95) is deleted. A fresh grep finds zero
  ComplexGeometry.shard and shardBurst calls in this renderer
  (merge gate 1 for this host).
- Everything else is behavior-identical: the three inward tendrils
  (same seeds, rim anchors, control points, colors 0x9900E6 to
  0xE633FF, distScale 0.70 factor), all eight material emitters
  including the WISP_CURL layer, the rig bookkeeping, envelope, pulse,
  and prune logic are untouched. The submit comment and the class
  javadoc were updated to record the move and the new vertex
  accounting (tendrils alone: 816 vertices worst case, inside the
  class M budget).

## Counts before and after

- Renderer-drawn shard pieces: 1 shardBurst call, 5 shards, 1440
  vertices per frame -> 0 calls, 0 shards, 0 vertices.
- Real fragment entities: 0 -> 5 per rift, spawned once at tick 2,
  never respawned. Host worst case is 5, far under the standard's
  area cap, matching the audit cap exactly.

## Spec resolutions (no foundation change needed)

- Audit says "fade 10"; the standard fixes FADE_TICKS at 12 for every
  fragment as a constant, and the foundation implements 12. The
  constant governs. With lifeTicks 70 the fade opens at age 58 at the
  latest and the server discards by age 70, so the audit's intent
  (fragments settle and fade just after the host dies at 60) holds.
- The audit names rendererSeed as the entity id term alone. The
  renderer's actual seed also XORs position terms. I recomputed the
  full renderer seed server side (position is server authoritative
  and the rift never moves), because the standard's silhouette
  identity rule is the point of the seed family. Recorded here so the
  merge sees the choice in the open.

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java,
both owned files, private /tmp tags dimrift-80d865fd):

- cp262: 4 errors total, all the standing baseline kinds in
  item/DbaItems.java (x2) and inventory/DbaMenus.java (x2). Zero
  errors in owned files.
- cp263: 10 errors total, exactly the standing baseline set
  (DbaItems x2, DbaMenus x2, DbaHostileEntity x2,
  PatrolRobotEntity x2, FriezaSoldierEntity x1,
  NamekianWarriorEntity x1). Zero errors in owned files.
- git diff --check on both files: clean.
- Added-lines em dash scan on both files: 0.
- No Gradle build was run; compile evidence is the javac audit
  above, per this mod's standing verification rule. In-game look is
  unverified until Carter's PC test.

## Change requests

None. The burst helper covered the spec exactly (cone distribution,
origin jitter, per-fragment seed salt, size classes, caps), and the
two spec tensions above resolved inside the standard without
foundation edits. entity/FragmentEntity.java and
client/render/FragmentRenderer.java were not touched.

## Honest worse check

- The accepted trade, recorded in the audit and real: the old fan
  rode the tear, recomputed from the current radius every frame, so
  at full size it sat at tearY 4.0 in a 15 block rift. The fragments
  spawn once at the tick 2 heart (tearY about 1.92) and drift and
  fall from there; they do not climb with the column. In the first
  second the burst therefore reads lower and more scattered than
  the fan did at peak. The tendrils and the fissure column emission
  still grow to full height and carry the growth read, and the
  fragments become falling and resting physical shards, which the
  fan never was. Net: a deliberate identity change, not a silent
  loss, but the peak-size tear will look emptier of solid pieces
  than before. If Carter judges the peak read too empty in game,
  the fix inside the standard is a second authored burst beat, not
  a renderer fan; that is a spec change for the merge or Carter,
  not something I improvised.
- The old shards faded with the rift's lifecycle envelope,
  including through the collapse (ticks 50 to 60). Fragments
  outlive the host by up to 10 ticks as settled shards fading on
  the fragment clock. That is the audit's stated intent (lifeTicks
  70, "settle and fade just after"), and it reads as debris
  settling after a collapse, but it is a visible timing difference
  worth naming.
- Burst arrangement is statistically, not literally, the old fan:
  the helper reproduces shardBurst's cone distribution with its
  own seeded Random, so directions and size rolls differ from the
  renderer's exact arrangement. Only the per-fragment mesh
  silhouettes are seed-identical. The old fan also varied its draw
  count with distance; the fragments are always 5 in the world.
  Not perceivable as a downgrade, recorded for completeness.
- Spawn height uses the unpulsed rig formula (the audit's about
  1.9 figure). The renderer's tearY also carried a pulse factor
  (0.85 to 1.0), so the fan sometimes sat up to about 0.3 blocks
  lower than the spawn height the fragments now use. Sub-block,
  not a read problem.
