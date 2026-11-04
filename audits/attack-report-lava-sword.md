# Attack Upgrade Pass: Lava Sword owner report

Owner slot: Lava Sword (basalt field wall). Spec:
audits/attack-complexity-audit.md section 20, contract
audits/attack-foundation-addendum.md. HEAD ccfebf2, no commits made.
Files changed: exactly three, all client side renderers.
LavaEruptionRenderer.java and LavaCleaveRenderer.java edited,
LavaFieldWallRenderer.java new. No file under src/main was touched:
no damage number, cooldown, cost, timing, mechanic, synced field,
lava placement budget, crust settling rule, or damage tag changed.
The lava family's lack of is_fire is untouched, as specced.

## The field finding

The audit says "the Lava Field" and the task said to find its
renderer and lifecycle. The lifecycle exists; the renderer does not.
entity/LavaField.java is a server side block engine: it places real
magma crust blocks on a SavedData backed schedule and settles them
cell by cell. It has no entity and no client renderer. The field's
envelope is its POOL stage schedule, STAGE_TICKS {60, 120, 200}: a
pool field lives 200 ticks from placement. The ability that
establishes a pool field is the eruption (LavaField.crustArc along
its moving lip, budget 8 pools), so the wall hangs on the eruption's
sighting and runs on the field's 200 tick clock, drawn by a new
client side lingering visual system in the AuraRenderer pattern
(lazy LevelRenderEvents.COLLECT_SUBMITS hook, camera relative pose,
per wall records keyed by entity id). The wall is set dressing on
that lifetime: it damages nothing, blocks nothing, has no
collision, and exists on no server.

## LavaFieldWallRenderer (new): the basalt wall

Beats, all on the field's existing envelope:

- RISE, field ticks 0 to 10: rockWall riseFactor runs 0 to 1
  (smoothstepped, the builder eases again per the addendum's owner
  pattern). Shed from the client tick hook: 2 basalt BLOCK_CHUNKS
  plus 1 EMBER_FRAGMENT per tick sampled along the wall line, rock
  chips and seam embers working out as the ridge heaves up.
- STAND, ticks 10 to 194: riseFactor 1, sinkFactor 0, envelope 1.
- SETTLE, ticks 194 to 200: sinkFactor runs 0 to 1 over 6 ticks and
  the envelope fades over the last 4, so the ridge sinks back into
  the field as the crust settles. Shed: 3 basalt BLOCK_CHUNKS per
  tick off the face, plus 1 EMBER_FRAGMENT every other tick while
  the seam is open. Records prune at age 202.

Form, per Carter's steering (rocky mountain heaving out of the
ground, jagged and clustered, fewer and bigger pieces) and his
realism bar (physical believability over game shapes):

- 7 slabs, the spec's low end: wall length 8.5 gives one slab per
  1.15 blocks, each slab 0.7 to 1.6 blocks wide after the builder's
  boundary jitter, so the ridge is a handful of big interlocking
  plates, not a row of bricks.
- Height 2.4 nominal, slab tops running 1.7 to 2.8 blocks on the
  builder's seeded 0.70 to 1.16 variance, with its seeded tilt,
  lean, face offset, and bulge: the skyline is uneven by
  construction, no two slabs alike.
- The wall stands on the field's front edge and follows its arc:
  length 8.5 at the eruption's final reach radius 6.5 (reachAt at
  end of life, 1.1 + 5.4), arc set to length over radius (about
  1.31 radians), facing back toward the eruption origin. The
  builder's arc circle then centers on the eruption origin, so the
  ridge curves along the same lip the pools were laid on.
- Grounded, not placed: the base height is the client heightmap
  (MOTION_BLOCKING) at the wall center, and the builder buries the
  slabs a further half block (baseY -0.45), so the ridge reads as
  ground that rose, with no daylight under it on flat or gently
  sloped fields.
- Material: dark basalt, root 0xFF17120E to tip 0xFF4A4238, drawn
  through submitSolidLit ONLY, with the packed light captured from
  the eruption's render state. The slab bodies never touch the
  emissive path (merge gate held).
- Magma seams: one thin tendril inset along the front face (width
  0.14, magma orange root to pale hot tip, TENDRIL canvas through
  submitEnergy), bowed to the wall's arc and sitting 0.34 inside
  the edge radius so the slab bulge swallows it in places: a seam
  that opens and closes along the face instead of a stripe painted
  on it. It fades in over the back half of the rise, and its
  vertical offset mirrors the builder's emergence drop term for
  term, so it rides the slabs down exactly on the settle. It is
  the only energy on the wall; there is no seam geometry in the
  slab bodies.

Counts: before, 0 wall pieces anywhere. After: per standing field,
7 slabs at 72 vertices is 504, plus the vein tendril at about 384,
for roughly 890 vertices in one submitSolidLit and one submitEnergy
call. Birth time derives from the eruption's synced seed and age,
so every client builds the identical wall on the identical clock,
and a wall first seen mid flight still runs on the field's clock.
Repeat sightings of the same eruption (same entity id and seed)
are ignored; the map is capped at 48 walls with stale records
pruned first.

## LavaEruptionRenderer: crackFan branches plus 2, wall feed

- Added the sighting call: every eruption rendered reports itself
  to LavaFieldWallRenderer.noteField with its position, rotation,
  age, seed, and render state light coords. No other behavior in
  this renderer changed; its emission system is byte-identical.
- crackFan: 1 fan before, 2 layered fans after. The builder
  derives its crack count from the seed (9 to 11 cracks, no count
  parameter exists), so the plus 2 branches are realized as a
  second inner fan at 0.55 of the radius and 0.60 alpha under the
  main fan, same pose: the web's core gains a second layer of
  shorter branches while the outer web is unchanged.
- Pieces vertex estimate at distScale 1.0: about 1100 before
  (9 tongues 432, one crackFan 560 to 700), about 1550 after
  (inner fan adds about 450). Class L budget 6000, ample headroom.

## LavaCleaveRenderer: tongue rows plus 1, basalt shard burst

- flameTongues rows: 1 row of 4 tongues before, 2 rows of 4 after.
  The added row trails the contact point along the wave body
  (offset travelDir times -0.30, radius 0.50, height 0.62 versus
  the lead row's 0.55 and 0.85), same palette and sway, so the
  crescent carries two rows of flame over its tendril body.
- shardBurst: 0 before, 4 basalt shards after, drawn at the
  leading contact point (travelDir times 0.55) with the travel
  direction as burst axis, 50 degree spread, unit 0.55, for the
  whole flight: the cleave's hit beat is its travel, and the
  shards are the rock the wave throws ahead of itself. Palette
  root 0xFF1B1512 to tip 0xFF57493A (dark basalt to worn grey
  brown), through submitSolidLit with the render state's light
  coords. Physical pieces on the emissive path: none.
- Pieces vertex estimate at distScale 1.0: about 580 before (one
  tendril 384, four tongues 192), about 1930 after (second tongue
  row 192, four shards at about 290 each is 1160). Class M budget
  2500, inside with headroom; distance scaling steps the burst
  count down to 1 at far range.

## Unchanged, verified

LavaMeteorRenderer and LavaGeyserRenderer: untouched, per spec
(flagships). LavaBlobRenderer: untouched (not in the spec). All
five lava entities, LavaField, LavaSwordGround, and LavaSwordItem:
untouched. Meteor counts, pool budgets (eruption 8, meteor 5,
cleave wave 44), channel carving, volcano rules, crust stage
schedule and settle conversions (POOL to basalt, CHANNEL to basalt
with the quarter magma centerline, WAVE vanishing) are exactly as
at HEAD, because no server file was edited at all.

## Change requests and spec gaps (foundation was read-only)

1. The crumble burst is client particles, not a DebrisSpawner
   burst. The spec's settle beat names DebrisSpawner, but that is
   a server side spawner of real DebrisEntity chunks, and the wall
   has no server presence; a renderer cannot call it. There is
   also no single server moment to hook: LavaField settles per
   cell, so "the field ends" is a 12 tick wide window server side,
   not an event. The settle therefore sheds basalt BLOCK_CHUNKS
   client side, the same chunk visual LavaField itself uses for
   its crust stage changes. If the merge wants real debris
   entities at the settle, it needs a server hook designed at the
   merge level; flagged, not improvised.
2. Per slab ground following needs builder support. The wall
   grounds on one heightmap sample at its center; on a field whose
   front edge crosses a steep step, individual slab bases can
   float or bury by the local variance (the builder's half block
   burial absorbs gentle slopes). rockWall takes a single origin,
   so per slab base offsets are a foundation change.
3. True per gap magma seams need builder support. Slab boundary
   positions are internal to rockWall and jittered per seed, so
   the owner cannot address the gaps; the seam is the face vein
   described above. If the merge's rockWall irregularity upgrade
   exposes boundary positions, the vein should be replaced by
   thin insets in the actual gaps.
4. crackFan branch count is seed derived (see the eruption
   section). A count parameter on crackFan would make the plus 2
   literal; the layered fan is the owner side realization.
5. Sighting dependency: a wall is established on a client only if
   that client renders the eruption at least once, the same
   limitation class as every renderer fed visual in the mod. An
   eruption fired entirely off screen leaves its pools standing
   without a wall on that client. A server driven alternative
   would need a field entity, which is an entity pass decision,
   not an owner one.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
sourcepath src/main/java:src/client/java) over the three owned
files, full logs read, never summaries:

- cp262: 4 errors, all standing baseline closure kinds (DbaItems
  x2, DbaMenus x2). Zero in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero in
  owned files.

git diff --check clean. Byte level em dash scan (UTF-8 E2 80 94
and E2 80 93): zero on the added lines of both edited renderers,
zero in the new file, zero in this report. No commits; HEAD
remains ccfebf2 for my work (other owners' uncommitted changes
share the tree; the three files above are the only ones mine).
No Gradle build run or claimed; the in-game look is unverified
until Carter's PC test.

## Honest worse check

- Sightline trade, named plainly: a ridge 1.7 to 2.8 blocks tall
  stands at the field's far edge for up to 200 ticks. From the
  caster the pools stay in front of it and fully visible, so the
  field's own read is untouched, but a target standing just past
  the field is partly hidden behind the ridge for up to 10
  seconds. That is the accepted cost of set dressing on a damaging
  field, and the wall sits on the far edge rather than the near
  edge for exactly this reason. First thing for Carter's eye.
- Settle skew: pools are laid across the eruption's 12 ticks and
  settle at placement plus 200, so the lip's last cells settle up
  to about 12 ticks after the wall finishes sinking at tick 200.
  The bulk of the field, laid from tick 1, settles on the wall's
  clock. The wall does not visibly outlive or predate the field's
  mass; the tail is a handful of crust cells. Accepted, named.
- The vein is one tendril: where a slab's bulge is shallow it sits
  slightly proud and can read as a molten stripe rather than an
  inset, especially mid rise before the face has fully emerged.
  Its fade in window (back half of the rise) and 0.34 inset were
  chosen against exactly that read. If it still reads as a stripe
  in game, the fix is form (thinner, lower, or removed), not
  count, and the merge's realism review should judge it.
- Cleave shard burst strobing risk: the burst is static geometry
  redrawn at the moving head each frame, the kit's standard burst
  pattern, and the head moves 1.35 blocks per tick. The shards are
  chunky (lengths to about 0.9 blocks) and dark against the bright
  flame body, which should hold the read; named as the cleave's
  watch item for the PC test.
- Eruption inner crackFan: the web's core is denser than before.
  Alpha 0.60 and the 0.55 radius keep it subordinate to the main
  fan; if the center reads busy in game, lowering the inner
  layer's alpha is a one line tune.
- Nothing was reverted. Every change stayed inside its class
  budget, no host lost a layer, and the two flagship renderers
  were not touched at all.
