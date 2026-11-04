# ENTITY STANDARD: the shared FragmentEntity contract

Binding for the Entity Objects Pass. Carter's rule: pieces that detach
and travel through the world on their own path are real entities, not
renderer decoration. Phase 2 owners convert the seven hosts in
audits/entity-audit.md to this contract and are judged by the merge
gates at the bottom of this file. Per-host numbers live in the audit;
this file is the shared contract every conversion follows.

## The entity

entity/FragmentEntity.java, registered in DbaEntities as FRAGMENT
(id "fragment", MobCategory.MISC, sized 0.5 x 0.5, clientTrackingRange
10, updateInterval 1, the DebrisEntity registration shape). It extends
Projectile and sets noPhysics = true, following DebrisEntity and
TridentShardEntity, the two fragment-class entities already in the mod.
Renderer: client/render/FragmentRenderer.java, registered beside the
DEBRIS renderer in DragonBlockArcaneDBAClient. No lang entry (the
debris and trident shard precedent: effect entities carry no name).

Fragments deal NO damage in any mode, are not pickable, and cannot be
hurt (hurtServer returns false). They exist to be physically true, not
to change any fight.

## Synced data (the whole contract a client ever sees)

- SEED (INT): the ComplexGeometry mesh seed. Spawn helpers derive it
  with the shardBurst salt, seed = baseSeed + index * 0x9E37L, so a
  fragment draws the identical silhouette the renderer burst drew.
- ROOT_ARGB (INT) and TIP_ARGB (INT): the theme gradient, root to tip,
  exactly the colors the host renderer used.
- LENGTH (FLOAT) and GIRTH (FLOAT): the shard's size in blocks, rolled
  server side over the shardBurst class ranges (about 55 percent chips
  at 0.30 to 0.55 of unit, 30 percent shards at 0.55 to 1.05, 15
  percent chunks at 1.05 to 1.60; girth 0.30 to 0.44 of length) and
  synced, so size is world truth.
- MODE (INT): 0 BALLISTIC, 1 BURST, 2 FROZEN, 3 FIELD_HELD.
- FADE_START (INT): entity age at which the fade window opened, or -1.
  The ValorFieldEntity FADE_START precedent: the server stamps it, the
  renderer fades over FADE_TICKS, the server discards at the end.
- RELEASE_AT (INT): for held modes, the entity age at which the hold
  ends. FROZEN spawns with it preset; FIELD_HELD spawns with -1 and
  release() stamps it. -1 in FIELD_HELD means still held.

Everything else (gravity scale, drag, rest ticks, life ticks, spin,
holder, anchor offset) is server-only state set by the spawn helpers.
The client never simulates fragment motion; position and rotation are
server truth at updateInterval 1, like every other effect entity here.

## Behavior modes: four values, two physics paths, one held path

BALLISTIC and BURST share one physics path (below). They differ only in
tuning defaults set by their spawn helpers: BALLISTIC is the long
trajectory (rain from height, full fall, land, rest 30, fade), BURST is
the short scatter (gentle initial speed, higher drag, host-scaled
gravity, short rest). Keeping them as separate mode values keeps each
host's spec legible; the standard records the collapse honestly rather
than pretending four physics engines exist.

FROZEN and FIELD_HELD share one held path: no motion, no gravity, until
release, then the same ballistic path as above with zero initial
velocity. They differ in what holds them:
- FROZEN holds a fixed world position. Release comes at RELEASE_AT
  (preset at spawn) or when the host calls release() early.
- FIELD_HELD holds an anchor offset from a holder entity: every tick
  the server sets the fragment position to holder position plus the
  anchor offset, so the fragment follows a moving field for real. Its
  holder is stored as a UUID and resolved server side each tick, the
  ValorFieldEntity suspended-projectile pattern. If the holder cannot
  be resolved (removed, discarded, unloaded), the fragment releases
  that tick and drops from where it hangs. That orphan rule is a safety
  net; hosts still call release() at the authored moment (field fade,
  rift implosion) so timing is exact, not net-dependent.

Held fragments may spin: a server-side spin in degrees per tick applied
to yRot during the held phase only (Valor dome shards 1.72, marker
shards 2.86, everything else 0 unless the audit says so).

## Physics (server, per tick, ballistic path)

The DebrisEntity idiom, verbatim in spirit:
- vel.y = max(vel.y - 0.045 * gravityScale, -1.8); vel.x and vel.z are
  multiplied by drag (0.99 ballistic default, 0.96 burst default).
- Position integrates by setPos(position + vel), noPhysics throughout.
- Heading follows velocity while speed squared exceeds 1e-4, using the
  TridentShardEntity updateHeading math, so a falling fragment points
  along its fall.
- Ground test: the block below (next y - 0.06) has a non-empty
  collision shape and vel.y <= 0, exactly DebrisEntity's landing test.
  On landing: velocity zeroes, the fragment rests where it lies, and
  the server sends a two-count SHARD_CHIP puff at the landing point.
- No horizontal collision (the DebrisEntity precedent). Fragments
  falling beside a wall can clip its edge; documented, accepted, and
  shared with every debris piece already in the game.

## Lifetime and fade rules

- lifeTicks is set at spawn by the helper (per host, from the audit).
  The fade window opens at the FIRST of: rest elapsed after landing
  (land tick + restTicks), or age reaching lifeTicks - FADE_TICKS.
  When it opens, the server stamps FADE_START with the current age.
- The server discards at FADE_START + FADE_TICKS. FADE_TICKS is 12 for
  every fragment (the DebrisEntity FADE_TICKS precedent), a constant,
  not a per-fragment tuning.
- HARD_LIFE_TICKS = 2400 is the absolute backstop: at that age the
  fragment fades and discards no matter what state it is in, so no
  fragment can outlive two minutes under any bug or missed release.
- Held fragments do not age out from under their host: helpers set
  lifeTicks beyond the longest honest hold (Void Rift: channel cap +
  implosion + fallout; Valor Field: the 2400 backstop governs, with
  release driven by the field, not by the clock).

## Caps

Enforced inside the spawn helpers, so no owner can exceed them by
accident:
- MAX_PER_CALL = 64 fragments from one helper call.
- MAX_IN_AREA = 256 fragments alive within a 96 block box around the
  spawn center; helpers count with getEntitiesOfClass and spawn only
  what fits. Every audit host's worst case sits far under this (the
  channeling SkyCracks case peaks around 30).
Hosts additionally follow their audit counts exactly. A host never
respawns fragments to top up a visual; counts are authored, not
adaptive.

## Chunk and restart safety

FragmentEntity overrides shouldBeSaved() to return false. Fragments
are never written to chunk data, are removed when their chunk unloads,
and cannot survive a restart, a relog, or a dimension hop. There is no
SavedData, no static registry, and no cross-tick bookkeeping anywhere
in the foundation. A crash mid-effect loses the fragments and nothing
else; hosts re-derive everything from their own state if they run
again. This matches the directive for the mod's effect entities:
non-persistent, discarded on chunk unload.

## Spawn API (static, on FragmentEntity, server-guarded)

All helpers return the number spawned (or the entity for single
spawns), refuse silently past the caps, and do nothing on the client.

- burst(level, owner, center, axis, spreadDeg, unit, count, burstSeed,
  rootArgb, tipArgb, speedMin, speedMax, gravityScale, lifeTicks,
  restTicks): cone scatter matching shardBurst's distribution (cone
  angle by sqrt roll, origin jitter within unit * 0.12 perpendicular,
  per-fragment seeds by the 0x9E37 salt). Mode BURST.
- rain(level, owner, center, discRadius, clusterCounts, unit, hostSeed,
  rootArgb, tipArgb): clustered fall for SkyCracks. Cluster c base seed
  = hostSeed ^ (0x5A9D + c * 131), cluster centers at seeded disc
  offsets up to discRadius, initial velocity (0.03 jitter, -0.35, 0.03
  jitter), lifeTicks 220, restTicks 30. Mode BALLISTIC.
- hang(level, owner, pos, axisDir, length, girth, seed, rootArgb,
  tipArgb, releaseAtAge): one FROZEN fragment. releaseAtAge < 0 holds
  until the host calls release().
- hold(level, owner, holder, anchorOffset, axisDir, length, girth,
  seed, rootArgb, tipArgb, spinDegPerTick): one FIELD_HELD fragment
  anchored to holder.
- release(): instance method; stamps RELEASE_AT with the current age.
  Idempotent.

Owners set the fragment owner to the host's caster, so systems that
exempt the caster's own projectiles (Valor Field stasis, Void Rift
loose pulls) exempt these fragments too.

## Renderer rules

- One ComplexGeometry.shard per fragment through submitSolid, seed from
  synced SEED, gradient from synced ROOT_ARGB to TIP_ARGB, alpha 0.95
  (the hosts' own peak). The shard origin sits at -axis * LENGTH * 0.35
  in local space so the body centers on the entity position, the
  TridentShardRenderer centering choice.
- Axis from the entity's synced rotation (directionFromRotation of
  xRot, yRot), never from a client-side guess.
- Spawn scale-in: length and girth scale by 0.5 + 0.5 * min(1, age / 5)
  (the TridentShardRenderer spawn read), except FROZEN below.
- FROZEN growth rule: while age < RELEASE_AT, the drawn length also
  scales by 0.55 + 0.85 * cubicOut(age / RELEASE_AT), saturating at
  1.40, and holds 1.40 after release. This reproduces TimeShatter's
  travel-curve growth exactly, with RELEASE_AT as the window. No other
  mode applies growth.
- Fade: once FADE_START >= 0, envelope = 1 - clamp01((age -
  FADE_START) / FADE_TICKS). Before that the envelope is 1 (times the
  scale-in, which is size, not alpha).
- The fragment renderer emits no particles. Hosts keep their own
  layers; per-fragment emission, if an owner ever wants it, is a host
  budget decision made in the open, not a foundation default.

## Merge gates for Phase 2

1. Zero ComplexGeometry.shard and shardBurst calls remain in the seven
   hosts' renderers (SkyCracksRenderer, TemporalRiftRenderer,
   DimensionalRiftRenderer, VoidRiftRenderer,
   DimensionalWarpRiftRenderer, TimeShatterRenderer,
   ValorFieldRenderer). Crack fans, fracture plates, tendrils, and
   every other builder stay; only shard pieces convert.
2. Per host, fragment count, size unit, colors, seed family, mode, and
   spawn beat match audits/entity-audit.md exactly.
3. All fragment spawns come from the server code paths the audit names.
   No client-side spawning, no renderer-side entity creation.
4. No damage change anywhere: fragments contain no damage calls, and
   host and item diffs show damage numbers, cooldowns, costs, timings,
   mechanics, and synced fields identical except the added spawn and
   release calls and the host bookkeeping they need.
5. Fragment totals per host stay at or under the audit caps in the
   worst case (channeling tempest overlap, chained TimeShatters, a
   long Valor Field with marker recycling).
6. Dual javac sweeps against the Phase 1 baseline: comm -13 empty on
   both classpaths. All resource JSONs parse. git diff --check clean.
   Added-lines em dash scan zero.
7. Host renderers keep every non-shard layer byte-for-byte in behavior:
   removing the shard calls must not move a tendril, a crack fan, or an
   emission line.
