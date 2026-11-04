# Entity Objects Pass: SkyCracks (Cursed Blade) conversion report

Owner slot: SkyCracks. Host: entity/SkyCracksEntity.java. Renderer:
client/render/SkyCracksRenderer.java. Spec: audits/entity-audit.md
section 1, contract: audits/ENTITY-STANDARD.md. HEAD at start: 6155dda.
No commits made.

## What moved

The falling shards left the renderer and became real FragmentEntity
instances, spawned server side in rain mode.

SkyCracksEntity.tick (server only, as before): inside the existing
bolt-shed beat (tickCount % 20 == 10), right after the BoltEntity spawn,
one added call:

    FragmentEntity.rain(serverLevel, this.getOwner(), this.position(), 9.5f,
        new int[]{2, 2, 1}, 1.05f, getSeed(), 0x14030A, 0x5A1230);

- Owner is the host's owner (the caster), so systems that exempt the
  caster's own pieces exempt these fragments too.
- hostSeed is the host's synced SEED, the same value the renderer fed
  its bursts. The helper derives cluster base seeds as
  hostSeed ^ (0x5A9D + c * 131) and per-shard mesh seeds as
  base + index * 0x9E37, the identical chain the renderer's shardBurst
  calls used, so each fragment draws the same silhouette its renderer
  index drew.
- Colors are the renderer's exact pair: root 0x14030A, tip 0x5A1230.
  Unit 1.05, the renderer's size unit, with size classes rolled server
  side by the helper over the shardBurst ranges and synced as LENGTH
  and GIRTH.
- The helper gives the fall the renderer's own drop rate as spawn
  velocity: (0.03 jitter, -0.35, 0.03 jitter), BALLISTIC physics,
  lifeTicks 220, restTicks 30, fade 12, per the standard's rain spec.

SkyCracksRenderer: the whole submitSolid shard block is deleted (3
shardBurst calls, counts 2, 2, 1). The class javadoc and the
pieces-layer comment now state that the shards are host-rained
FragmentEntity pieces and are not drawn here. Nothing else in the
renderer moved: both crackFans, the wisp curl layer, the plasma path
emissions, the soul stream, the darkness disc, and the envelope and
flicker math are byte-for-byte in behavior what they were. A grep of
the renderer finds no ComplexGeometry.shard, shardBurst, or submitSolid
call left; the only "shard" mentions are the two explanatory comments.

No other file was edited. FragmentEntity and FragmentRenderer are
untouched. CurseBladeItem is untouched.

## Counts before and after

Before: 5 shard meshes drawn per frame per client (2, 2, 1 across 3
bursts), positioned on that client's billboard plane, cycling inside a
7 block band at 0.35 blocks per tick, never landing, identical count
for every viewer but at camera-relative positions.

After: 0 shards in the renderer. 5 real fragments per 20 ticks per
host, 25 over one host's 100 tick life, spawned in a horizontal disc of
radius 9.5 under the crack plane, falling the full height to the
ground, landing, resting 30 ticks, fading over 12. Fall time from 55
blocks up is roughly 40 to 60 ticks, so steady state is about 10 to 15
alive per host; up to 2 hosts overlap while channeling (a fresh host
every 80 ticks, 100 tick life), worst case about 30 alive. That sits
under the audit's per-host guidance cap of 40 and far under the
foundation caps (64 per call, 256 within 96 blocks), which the helper
enforces itself.

## Identity checks

- Damage: unchanged. The host still deals none (its bolts were already
  visual only, damage lives in CurseBladeItem), and fragments deal no
  damage in any mode by the foundation contract. No damage number,
  cooldown, cost, timing, or mechanic line differs in either file.
- Synced data: no field added, removed, or repurposed on the host. The
  render state still reads only tickCount and the synced seed.
- Themes and colors: exact (see above). Host life (100), follow height
  (caster Y + 55 capped at 280), bolt cadence, bolt colors, and bolt
  size are untouched.

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java,
private /tmp tags f096c889) over SkyCracksEntity.java and
SkyCracksRenderer.java:

- cp262: 4 errors total, all baseline closure kinds (DbaItems x2,
  DbaMenus x2). Zero errors in SkyCracksEntity, SkyCracksRenderer, or
  FragmentEntity.
- cp263: 10 errors total, exactly the documented baseline set
  (DbaItems x2, DbaMenus x2, DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero in owned
  files.

git diff --check on both files: clean. Added-lines em dash scan
(byte-level): zero. No Gradle build run or claimed; the in-game look
is unverified until Carter's PC test.

## Change requests against the foundation

None. The rain helper covered the audit spec exactly as written:
cluster count array, disc radius, the 0x5A9D seed family, the -0.35
spawn fall speed matching the old drop rate, rest 30 and life 220.
No gap was found and no workaround was needed.

## Honest worse check

Two real changes in read, both consequences of the conversion itself
(the audit accepted the first on purpose):

1. The clusters are world-anchored now, not billboard-anchored. The
   old shards hung in clusters near the tear heart on the camera
   plane, so the tear always looked populated at its center. The new
   shards spawn in a disc under the host and immediately fall the
   full height, so at any moment most of them are strung out down the
   fall column and the tear heart itself reads emptier, carried by
   the crack fans and plasma paths alone. That is the physical truth
   Carter asked for (shards fall from sky rifts on independent
   trajectories), and the landing and resting on real ground is new
   information the billboard version could never show. But a player
   watching the tear itself, not the ground, sees less shard presence
   than before.
2. The old band cycled the three clusters at staggered phases
   (cluster * 2.37 offsets), a continuous staggered rain illusion.
   The new rain fires all 5 shards on one beat every 20 ticks, so
   the fall comes in pulses. Fall time (40 to 60 ticks) overlaps two
   to three pulses in the column, so it never reads empty, but a
   watching player may notice the pulse beat.

Nothing else regressed: per-index silhouettes, sizes, colors, counts,
and cadence match the audit spec exactly, and the renderer's remaining
layers are untouched.
