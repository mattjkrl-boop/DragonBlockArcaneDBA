# Entity Objects Pass: Temporal Rift (Whis Staff) conversion report

Slot: Whis Staff, Temporal Rift glass shards. Host entity/TemporalRiftEntity.java,
renderer client/render/TemporalRiftRenderer.java. Spec: audits/entity-audit.md
section 2, contract audits/ENTITY-STANDARD.md. HEAD at start 6155dda, no commits.

## What moved

The 5 temporal glass shards at the rift's tear heart stopped being a
renderer-drawn ComplexGeometry.shardBurst and became real FragmentEntity
instances, spawned server side in BURST mode.

- entity/TemporalRiftEntity.java (+12 lines, one block): inside the existing
  tick 2 debris guard (the `!debrisSpawned && tickCount >= 2` block, same flag
  discipline as the DebrisSpawner field), one FragmentEntity.burst call with
  the audit values. Nothing else in the file changed.
- client/render/TemporalRiftRenderer.java: the submitSolid shardBurst block
  (5 shards) is deleted, along with its now-unused shardUnit local. Comments
  updated to record the conversion and the new vertex accounting (tendrils
  only, 816 worst case). Nothing else in the file changed.

## Spec match

| Value | Audit | Shipped |
|---|---|---|
| Count | 5, once, no respawn | 5, inside the one-shot debris guard |
| Spawn beat | server, tick at tickCount == 2, debris guard beat | same guard block as DebrisSpawner.spawnField |
| Center | host position + (0, radius * 0.441, 0) | center.add(0, radius * 0.441, 0) |
| Cone | axis (0, 1, 0), spread 150 degrees | axis (0, 1, 0), spread 150f |
| Seed family | riftSeed ^ 0x7E42, shard seeds + index * 0x9E37 | burstSeed = getSeed() ^ 0x7E42L; helper applies the 0x9E37 salt |
| Unit | max(0.5, radius * 0.09), 1.08 at radius 12 | Math.max(0.5f, radius * 0.09f) on the synced radius |
| Colors | root 0x2F6DB3, tip 0xDDF7FF | identical ints in the burst call |
| Mode / tuning | BURST, speed 0.10 to 0.30, gravityScale 0.3, lifeTicks 110, restTicks 10, fade 12 | identical; fade is the foundation FADE_TICKS constant |
| Owner | the rift's caster | the tick owner (getOwner(), re-fetched from CASTER_ID if needed) |
| Damage | none | fragments deal no damage; host damage code untouched |

Counts before and after: renderer-drawn shard pieces 5 per frame to 0;
FragmentEntity instances 0 to 5 per rift, once per rift.

## Untouched, verified in the diff

- The three inward tendrils: same seeds (pseed ^ (0x7EA5 + i * 131)), same
  rim/heart/control math, same submitEnergy call, still inside the same
  rotationY(age * 0.4 degrees) pose. The pose rotation lines are byte
  identical; only the shard submission between pushPose and the tendril
  submission is gone, so no tendril moved.
- All eight material emitters (plasma core, plasma field, soul, wisp curl,
  glass volume, ground glass, mist, darkness core) and the emission tick:
  untouched.
- The DebrisSpawner field of 60 real debris pieces: untouched, still spawned
  on the same beat just above the new call.
- Host mechanics: PEAK_TICK damage burst, REVERSAL_TICK rewind, vortex drift,
  layered server particles, sounds, lifetime countdown: untouched.
- Host synced data: no new accessors, no changed fields. The burst reads the
  existing synced SEED and RADIUS.
- TimeShatter files: not touched (different owner's slot).

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java) over both
owned files, private /tmp tags (7836b7ac):

- cp262: 4 errors total, all pre-existing baseline kinds in other files
  (item/DbaItems.java x2, inventory/DbaMenus.java x2). Zero errors in owned
  files.
- cp263: 10 errors total, the documented baseline set (DbaItems x2, DbaMenus
  x2, entity/PatrolRobotEntity x2, entity/DbaHostileEntity x2,
  entity/NamekianWarriorEntity x1, entity/FriezaSoldierEntity x1). Zero errors
  in owned files.
- git diff --check clean. Added-lines em dash scan: zero.
- Merge gate 1 for this host: zero ComplexGeometry.shard and shardBurst calls
  remain in TemporalRiftRenderer (grep confirms; the only remaining
  ComplexGeometry call is tendril, which stays by the standard).

## Change requests

None. The foundation burst helper covered the audit spec exactly (cone
distribution, seed salt, size classes, caps, landing puff, fade discipline).
No foundation gap was found while converting this host.

## Honest worse check

Three real differences, stated plainly:

1. The read at the heart changes, by the audit's design. The old fan hung at
   the tear heart for the rift's whole life, turning slowly with the pose.
   The fragments spawn at the full heart height (radius * 0.441) at tick 2
   and immediately drift outward and sink under 0.3 gravity; they do not
   hang at the heart for 100 ticks. This is the conversion's authored intent
   (shards burst outward and travel) and the audit already records the lost
   pose rotation as an accepted trade, with server spin and real positions
   carrying the read instead. It is still the most visible change in this
   slot: if the hanging fan was the preferred look, this slot is where that
   shows.
2. Spawn size timing. The old burst followed the renderer's build-phase
   radius unfold, so its unit grew from 0.5 toward 1.08 and its heart height
   rose over roughly the first 15 ticks. The fragments spawn at full unit
   and full height at tick 2, softened only by the foundation's 5 tick spawn
   scale-in. A slightly earlier, fuller presence during the build phase is
   possible; it lands while the field's own envelope is still ramping, so
   the net pop risk is low.
3. Fade timing is fragment-local now, not envelope-locked. A fragment that
   lands early rests 10 ticks and fades even if the rift still has life
   left, while the old fan faded exactly with the rift envelope. With spawn
   speed 0.10 to 0.30, gravity 0.3, and a 5.29 block start height at radius
   12, most fragments are still airborne or freshly landed near the rift's
   100 tick end, and lifeTicks 110 caps the tail, so the divergence window
   is small.

Nothing was reverted; no part of this conversion reads as a plain regression
against the standard. The one deliberate look change (item 1) is the audit's
own call, recorded there and here.
