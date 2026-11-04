# Entity Objects Pass: Void Rift (Hollow's Edge) conversion report

Owner slot: Void Rift. Spec: audits/entity-audit.md section 4, contract:
audits/ENTITY-STANDARD.md. HEAD at start: 6155dda. No commits made.
Files changed: exactly two.

- src/main/java/com/dragonblockarcanedba/entity/VoidRiftEntity.java
- src/client/java/com/dragonblockarcanedba/client/render/VoidRiftRenderer.java

## What moved

The shard ring at the rift mouth is no longer renderer decoration. It
is five FragmentEntity instances, spawned server side, that hang in
world space for the channel and drop for real at implosion.

Renderer (VoidRiftRenderer.java):

- Deleted all five ComplexGeometry.shardBurst calls (count 1 each,
  inside submitSolid) and the shardPieceEnv envelope local and the
  bandL capture that only they used. Gate grep: zero
  ComplexGeometry.shard and shardBurst references remain in this file.
- Everything else is untouched in behavior: the two inward tendrils,
  the ASH_FLAKE rim layer, all seven MaterialEmitters streams (halo,
  inward, throat, rim, seam, and the soul stream at the seven lip
  crystal anchors), the full implosion alpha choreography, and the
  distance scaling. The pieces-layer vertex budget drops from 2208
  (shards 1440 plus tendrils 768) to 768 (tendrils only). Class doc
  updated to record the conversion.

Host (VoidRiftEntity.java):

- New server-only bookkeeping: a List of FragmentEntity, the same
  pattern family as ValorFieldEntity's suspended map.
- spawnRimShards runs on the first tick, inside the existing
  spawnBurstDone beat, server side only (tick returns before it on the
  client). Five FragmentEntity.hang calls, one per anchor.
- triggerImplosion() now calls release() on each fragment after its
  existing work. That single hook covers every implosion path: the
  item release in HollowsEdgeItem, the auto implode at
  MAX_CHANNEL_TICKS, and any future caller. release() is idempotent.
- No damage number, cooldown, cost, timing, mechanic, or synced field
  changed. The host diff is only the added spawn beat, the release
  calls, and the bookkeeping they need (merge gate 4).

Match data, per the audit:

- Count: 5, once. Mode: FROZEN via hang, host release.
- Anchor angle: i * 2 PI / 5 + 0.35, the renderer's own formula.
- Placement: horizontal ring around the rift center at radius * 1.16;
  height sin(angle) * radius * 0.5; axis radial in the ring plane
  (cos, sin across XZ) tilted vertically by sin(angle). Billboard rim
  anchors become world-anchored planes of position, the trade the
  audit records and accepts.
- Unit: radius * 0.38 at spawn-time radius (2.5 at cast, unit 0.95).
- Seeds: per-shard (int) (entity UUID most significant bits ^ (0xE11
  + i * 131)), the exact burst seeds the renderer passed.
  shardBurst at count 1 draws its single shard with mesh seed equal
  to the burst seed, and FragmentRenderer draws with the synced seed
  directly, so each fragment's silhouette is the same seed's
  silhouette the renderer drew.
- Colors: root 0x2E004D, tip 0x9905F2, identical. Alpha 0.95 is
  FragmentRenderer's constant for every fragment, matching.
- Lifetime: rest 8 after landing, fade 12 (the FADE_TICKS constant),
  per the audit. Owner: the rift's caster, which also means the
  rift's own pullLooseEntities exempts them as owner projectiles, so
  the channel and the implosion suction never eat the ring early.
- Sizes: rolled server side with java.util.Random seeded per shard,
  replicating FragmentEntity's class ranges exactly (55 percent unit
  * 0.30 to 0.55, 30 percent unit * 0.55 to 1.05, 15 percent unit *
  1.05 to 1.60; girth length * 0.30 to 0.44). The old renderer's
  sizes came from ComplexGeometry's own Rng after its cone rolls, so
  individual sizes are distribution-identical, not value-identical;
  the silhouettes, which are seed-driven, are value-identical.

## Counts before and after

- Renderer-drawn shard pieces per rift: 5 before, 0 after.
- FragmentEntity instances per rift: 0 before, 5 after (once, no
  top-up, far under every cap in the standard).
- Host keep-drawing list from the audit, all still drawn: two inward
  tendrils, ASH_FLAKE rim layer, all material emission, DebrisSpawner
  waves (already entities, untouched).

## Verification

Targeted javac (JDK 25, sourcepath src/main/java:src/client/java,
private /tmp tags vr-262-8070d867 and vr-263-8070d867) over the two
owned files:

- cp262: 4 errors total, all standing baseline kinds (DbaItems x2,
  DbaMenus x2). Zero errors in owned files.
- cp263: 10 errors total, exactly the documented baseline set (the
  four above plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors in
  owned files.
- git diff --check clean on both files. Added-lines em dash scan:
  zero. No resource files touched. No Gradle build run or claimed
  (the standing VM waiver); the in-game look is unverified until
  Carter's PC test.

## Change requests (foundation, read-only for this owner)

1. hang() life versus the audit's life figure. Audit section 4
   states the fragments' spawn lifeTicks as MAX_CHANNEL_TICKS +
   IMPLOSION_TICKS + 120 = 390. hang() derives life as (releaseAtAge
   > 0 ? releaseAtAge : 600) + 300, which is 900 for the
   host-release pattern, and a held FROZEN fragment never consults
   lifeTicks in tick() at all, so an orphaned ring (host discarded
   without imploding) is bounded only by HARD_LIFE_TICKS 2400, not
   390. Safe today (the backstop discards at 2400), just longer
   lived than the audit states. Suggested fix: an explicit
   lifeTicks parameter on hang(), and held fragments fading at
   lifeTicks even while held.
2. FragmentRenderer FROZEN growth keys off any RELEASE_AT > 0. The
   growth rule was designed for a preset RELEASE_AT (TimeShatter).
   When a host calls release(), RELEASE_AT becomes the current age,
   the growth branch flips straight to 1.40, and a host-released
   shard steps from 1.0 to 1.40 drawn length on the release frame.
   Suggested fix: gate growth on RELEASE_AT having been preset at
   spawn, not merely positive. In this conversion the step lands
   inside the implosion burst frame, so it reads as collapse, but
   the foundation should not rely on that cover for every host.

## Honest worse check

1. The rift moves and grows while it channels; the ring does not.
   This is the slot's real risk. HollowsEdgeItem drags a channeling
   rift toward the crosshair (0.35 lerp per tick, target 14 blocks
   out) and grows its radius from 2.5 to as much as 9.0 over 100
   ticks. The drawn ring rode the mouth through all of that, since
   its anchors recomputed every frame from the current position and
   radius. The fragments spawn once, at the opening position, at the
   opening radius, and hang there. Drag the rift away or grow it and
   the ring stays behind at opening size until release: detached
   from the tear in the common case of a long, repositioned channel.
   The conversion follows the audit spec to the letter (FROZEN,
   first-tick spawn, no tracking), and the audit records the same
   family of trade as accepted for Dimensional Rift, but for Void
   Rift the movement is larger and player-driven, so I am naming it
   as this slot's first thing to judge in game. If it reads wrong,
   the remedy already exists in the foundation: FIELD_HELD via
   FragmentEntity.hold with the rift as holder and the same anchor
   offsets would follow the rift for real (radius growth would
   still not rescale the ring). That is a mode change against audit
   section 4 and merge gate 2, so it is the merge's call, not this
   owner's.
2. Implosion choreography changed in kind. The drawn shards died
   in place on the shard window (envelope gone by implodeT / 0.70,
   still hanging at the rim). The fragments drop at the trigger
   moment and fall through the 30 tick collapse, then rest 8 and
   fade. The rim empties by falling instead of by shrinking away.
   That is the physical trade the whole pass is buying; recorded
   plainly rather than dressed up.
3. The world ring is flatter and wider than the mouth it rings.
   The drawn anchors sat on the tall mouth ellipse (belly-limited
   width, heights near plus or minus radius * 0.96 at full band,
   band pulsing). The audit's world ring is circular: radius * 1.16
   across in every direction, heights only plus or minus radius *
   0.5. At cast radius it spans 5.8 blocks across and 2.5 high,
   where the drawn anchors spanned about 2.7 across and 4.8 high.
   The audit prescribes this form (the billboard plane is
   per-client; the world ring is the honest version, and every
   client now sees the same ring), but the silhouette against the
   tall tear is different and lower. Accepted per audit; flagged
   so the merge hears it from this slot too.
4. Release-frame length step, change request 2 above: drawn length
   steps 1.0 to 1.40 when release() stamps RELEASE_AT. Masked by
   the burst in practice; noted, not hidden.
5. Spawn read: fragments scale in over 5 ticks at the opening
   position, where the drawn shards faded in with the tear's 20
   tick envelope. Slightly more abrupt on the first ticks. Minor.

Nothing was reverted, and nothing here was shipped silently: items
1 and 3 are the audit's own accepted trades applied to a host whose
channel moves more than the audit text dwells on, and item 1 names
its remedy for the merge to judge.
