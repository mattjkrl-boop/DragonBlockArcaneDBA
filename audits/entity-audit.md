# Entity Objects Pass: Phase 1 Audit (at ef14656)

Carter's directive (Nov 4, 2026): pieces that detach and travel through the
world on their own path should be real entities, the way the Devil Trident's
flying shards already are, not renderer-drawn decoration. His bar: if it
needs a massive change, do the massive change, and it should be perfect no
matter what. It ships perfect or it does not ship.

This audit covers the seven hosts whose renderer pieces convert to the
shared FragmentEntity built in this phase. Every number below was read
from the post-redo code at ef14656: the host entity, its renderer, and its
spawn sites. The binding contract for the conversion is
audits/ENTITY-STANDARD.md. The foundation report is
audits/entity-phase1-report.md.

Damage check, done first because it gates everything: NONE of the seven
hosts' renderer shards carry a damage role. In every host the damage lives
in the item or the host entity (SkyCracks sheds visual-only BoltEntity
strikes and says so in its own javadoc; Temporal Rift lands its burst at
PEAK_TICK; Void Rift channels and bursts for its own damage; Dimensional
Rift ticks and bursts; Warp Rift bursts at its burst tick; TimeShatter is
pure presentation over the Whis Staff hit; Valor Field pressure damage
and the suspended projectiles' own damage are the field's, never the
decorative shards'). FragmentEntity therefore deals no damage in any
mode, and no host damage number changes anywhere in this pass.

A note on seeds that applies to all seven: the renderer shard meshes are
seeded, and ComplexGeometry.shardBurst gives burst shard i the mesh seed
(burstSeed + i * 0x9E37L). The spawn helpers in FragmentEntity set each
fragment's synced SEED with the same salt pattern from the same base
seeds the renderers use today, so a converted fragment draws the identical
mesh silhouette the renderer burst drew for that index. Length and girth
are rolled server side (java.util.Random) over the same class ranges
shardBurst uses (about 55 percent chips at 0.30 to 0.55 of unit, 30
percent shards at 0.55 to 1.05, 15 percent chunks at 1.05 to 1.60; girth
0.30 to 0.44 of length) and synced as LENGTH and GIRTH floats, so size is
world truth now, not a per-client roll.

## 1. Cursed Blade, SkyCracks falling shards

Host: entity/SkyCracksEntity.java (100 tick life, follows its caster at
caster Y + 55 capped at 280, sheds a visual BoltEntity every 20 ticks at
tickCount % 20 == 10). Spawned by item/CurseBladeItem.java while
channeling the Cursed Tempest, a fresh host every 80 ticks at the caster.
Renderer: client/render/SkyCracksRenderer.java.

Renderer composition today: three ComplexGeometry.shardBurst calls inside
one submitSolid, at billboard-plane spots (6, 4), (-8, -2), (1, -9), with
counts 2, 2, 1 (5 shards total), axis (0, -1, 0), spread 38 degrees, unit
1.05, root color 0x14030A, tip color 0x5A1230, alpha 0.95. The burst
origins cycle downward: drop = (age * 0.35 + cluster * 2.37) % 7.0, so
each cluster sinks 7 blocks at 0.35 blocks per tick and respawns, a
20 tick cycle. The billboard plane is per-client camera space; that is
exactly what the conversion removes.

Entity version must match: 5 shards per cycle in 3 clusters with the
same per-cluster counts and the same mesh seeds (cluster base seed =
hostSeed ^ (0x5A9D + cluster * 131), shard seed = base + index * 0x9E37),
unit 1.05 size classes, colors 0x14030A root and 0x5A1230 tip, solid
through submitSolid at alpha 0.95, initial downward speed 0.35 blocks
per tick (the renderer's drop rate becomes the spawn velocity).

What changes on purpose: positions become world truth. Clusters spawn in
a horizontal disc under the crack plane instead of on a camera plane:
cluster centers at seeded disc offsets up to radius 9.5 (the renderer
spots reach about 9.4), shards fall the full height to the ground under
gravity instead of cycling in a 7 block band, land, rest 30 ticks, fade
12, and are gone. The fall is the point of this host (Carter: shards fall
from sky rifts across a wide span on independent trajectories).

Spawn point: server side, SkyCracksEntity.tick, on the same beat as the
bolt shed (tickCount % 20 == 10), FragmentEntity.rain with the host at
its current position, disc radius 9.5, 3 clusters of 2, 2, 1, unit 1.05,
host seed, colors above. Owner: the cracks' owner (the caster), so other
systems treat the shards as the caster's own pieces.

Counts and caps: 5 per 20 ticks per host, 25 over one host's life. Fall
time from 55 blocks up is roughly 40 to 60 ticks, so steady state is
about 10 to 15 alive per host; up to 2 hosts overlap while channeling
(hosts spawn every 80 ticks, live 100). Per-host guidance cap 40 alive;
the helper's per-call cap (64) and area cap (256 within 96 blocks) sit
far above this host's needs.

Host keeps drawing: both crackFans (the tear plane itself), the plasma
path emission, wisp curls, soul stream, and the darkness disc. Only the
shardBursts leave the renderer.

## 2. Whis Staff, Temporal Rift glass shards

Host: entity/TemporalRiftEntity.java (life RIFT_LIFETIME_TICKS = 100 from
item/WhisStaffItem.java, radius AOE_RADIUS = 12 synced, seed synced as a
long, tear heart at radius * 1.05 * 0.42 = 5.29 blocks up at radius 12).
The host already spawns a real DebrisSpawner field of 60 pieces at tick
2; the glass shards are a separate, smaller set. Renderer:
client/render/TemporalRiftRenderer.java.

Renderer composition today: one shardBurst inside submitSolid at local
(0, tearY, 0) where tearY = radius * 1.05 * 0.42, axis (0, 1, 0), spread
150 degrees (a near-spherical fan), unit max(0.5, radius * 0.09) = 1.08
at radius 12, count 5, burst seed = riftSeed ^ 0x7E42, root 0x2F6DB3, tip
0xDDF7FF, alpha 0.95. The whole pieces pose rotates slowly
(rotationY(age * 0.4 degrees)). The shards hang at the tear heart for the
rift's life; they do not fall in the renderer.

Entity version must match: 5 fragments, same burst seed family
(riftSeed ^ 0x7E42, shard seeds + index * 0x9E37), same unit formula,
same colors, spawned at the heart (host position + (0, radius * 0.441,
0)) in the same 150 degree upward cone.

What changes on purpose: the fragments burst outward for real (Carter:
shards burst outward from the rift and travel). BURST mode with a gentle
initial speed (0.10 to 0.30) and gravity scale 0.3: the time suspension
field justifies the slow fall, and the pieces drift out, stall, sink,
land or expire near the rift's own 100 tick life (lifeTicks 110, rest
10, fade 12). The slow pose rotation of the fake fan is not reproduced;
real pieces with real positions replace it.

Spawn point: server side, TemporalRiftEntity.tick at tickCount == 2, the
same guarded beat as the DebrisSpawner field (alongside the existing
debrisSpawned flag discipline), FragmentEntity.burst with the values
above. Owner: the rift's caster.

Counts and caps: 5 per rift, once. No respawn.

Host keeps drawing: the three inward tendrils, wisp curl stream, plasma
and soul filler, glass dust, mist boundary, darkness core, and the real
DebrisSpawner field (already entities, untouched).

## 3. Sickle of Sorrow, Dimensional Rift shard burst

Host: entity/DimensionalRiftEntity.java (MAX_LIFETIME 60, radius expands
3 to 15 over 20 ticks, collapse at tick 50). No synced seed: the renderer
derives seed = entity id * 2654435761L. Renderer:
client/render/DimensionalRiftRenderer.java; fissure rig height =
min(8, 2.5 + radius * 0.4) * pulse, tear heart at rig height * 0.5, which
is 4.0 at full radius.

Renderer composition today: one shardBurst inside submitSolid at local
(0, tearY, 0), axis (0, 1, 0), spread 100 degrees, unit 0.80, count 5,
burst seed = rendererSeed ^ 0x5A17, root 0x24053D, tip 0xB84DFF, alpha
0.95, following the lifecycle fade.

Entity version must match: 5 fragments, burst seed family
(rendererSeed ^ 0x5A17) with rendererSeed = (long) host id * 2654435761L
recomputed server side from the host's entity id (identical on server
and client, since entity ids are synced), unit 0.80, same colors, cone
spread 100 degrees around up.

Spawn point: server side, DimensionalRiftEntity.tick at tickCount == 2
(the cast already spawns its debris field early; fragments ride the same
beat), at the heart height for the radius at that tick (rig height
formula at radius about 3.3 gives tearY about 1.9). BURST mode, initial
speed 0.10 to 0.30, gravity scale 0.5, lifeTicks 70 (the host dies at 60;
fragments settle and fade just after), rest 8, fade 10.

What changes on purpose: the fake fan sits at a tearY recomputed from the
current radius every frame, so it rides the column as the rift grows
from 3 to 15. Fragments spawn once at the early heart and drift; they do
not track the growing column. The tendrils and the column emission keep
carrying the growth read. Spec records this trade explicitly.

Counts and caps: 5 per rift, once.

Host keeps drawing: the three inward tendrils, all material emission,
and its DebrisSpawner field (already entities, untouched).

## 4. Hollow's Edge, Void Rift shard ring

Host: entity/VoidRiftEntity.java (radius synced, 2.5 at cast from
item/HollowsEdgeItem.java, channel up to MAX_CHANNEL_TICKS 240, then a
30 tick implosion through triggerImplosion; IS_IMPLODING and
IMPLODE_START_TICK synced). No synced seed: the renderer derives seed
from the entity UUID's most significant bits. Renderer:
client/render/VoidRiftRenderer.java.

Renderer composition today: five shardBurst calls of count 1 inside
submitSolid, at rim anchors in the camera billboard plane: anchor angle
per index, origin at (cos * belly * band * 1.16, sin * halfH * band *
1.05, 0) on the mouth ellipse, direction radial in the plane, spread 24
degrees, unit = radius * 0.38, per-shard seed = riftSeed ^ (0xE11 +
index * 131), root 0x2E004D, tip 0x9905F2, alpha 0.95. The shards follow
the shard collapse window: during implosion their envelope dies by
implodeT / 0.70 of the 30 tick collapse.

Entity version must match: 5 fragments, same per-shard seeds, same size
formula (unit radius * 0.38), same colors. The ring becomes world space:
anchors on a horizontal ring around the rift center at radius * 1.16,
heights staggered by the same sine pattern across radius * 0.5, each
fragment's axis radial (outward in the ring plane, tilted by its anchor
sine). The billboard plane is per-client; the world ring is the honest
form of the same composition, and every client now sees the same ring.

Mode: FROZEN (hang at the rim for the channel, however long it runs),
released by the host. VoidRiftEntity.triggerImplosion() is the release
event: the host keeps references to its five fragments (the same
bookkeeping pattern as ValorFieldEntity's suspended map) and calls
release() on each when the implosion triggers, including the auto
implode at MAX_CHANNEL_TICKS, since that path also calls
triggerImplosion. Released fragments drop under gravity, land, rest 8,
fade 12. If the host is discarded without imploding (chunk unload takes
the fragments too; caster interruption paths discard through the same
entity), the fragments' own hard life still bounds them (lifeTicks set
at spawn to MAX_CHANNEL_TICKS + IMPLOSION_TICKS + 120).

Spawn point: server side, VoidRiftEntity on its first tick (the
spawnBurstDone beat), FragmentEntity.hang per anchor (5 calls) or a
small loop in the host. Owner: the rift's caster.

Counts and caps: 5 per rift, once.

Host keeps drawing: the two inward tendrils, ASH_FLAKE rim layer, all
material emission, and its DebrisSpawner waves (already entities).

## 5. Dimensional Sword, Warp Rift shard fan

Host: entity/DimensionalWarpRiftEntity.java (two instances per warp from
item/DimensionalSwordItem.java: origin life ORIGIN_LIFE 34, destination
life DEST_LIFE 46, radius synced 3.0 origin and 4.5 destination, seed
synced as an int). Renderer:
client/render/DimensionalWarpRiftRenderer.java.

Renderer composition today: one shardBurst inside submitSolid at local
(0, 1.4, 0), axis (0, 1, 0), spread 68 degrees, unit 0.70, count 4, burst
seed = riftSeed ^ 0x5A17, root 0x1A0533, tip 0xE8CCFF, alpha 0.95, inside
the tear geometry envelope window.

Entity version must match: 4 fragments per rift (both origin and
destination rifts draw the same fan today, so both spawn them), same
seed family and unit, same colors, cone spread 68 degrees around up
from the heart at host position + (0, 1.4, 0).

Mode: BURST, initial speed 0.12 to 0.32, gravity scale 0.6, lifeTicks
matched to the host (origin 34 + fallout: 60; destination 46 + fallout:
72), rest 8, fade 10. The fan hangs at the tear in the renderer for the
rift's whole short life; the entity version scatters, sinks, and settles
inside roughly the same window.

Spawn point: server side, DimensionalWarpRiftEntity on its first tick.
Owner: the rift's owner (the warping player).

Counts and caps: 4 per rift, 8 per warp, once.

Host keeps drawing: the two inward tendrils, the ground crackFan,
WISP_CURL layer, and all material emission.

## 6. TimeShatter frozen shards

Host: entity/TimeShatterEntity.java (life 36 ticks fixed, scale synced,
seed synced as an int; spawned by item/WhisStaffItem.java at impact
points, scale 1.2 typical with stepped variants). Renderer:
client/render/TimeShatterRenderer.java.

Renderer composition today: five shardBurst calls of count 1 inside
submitSolid, one per spoke, on the camera billboard plane. Spoke angles
come from java.util.Random(hostSeed ^ 0xFA17): angle_i = (i / 5) * 2 PI
+ (nextFloat - 0.5) * 0.55, consumed in order. Per-spoke burst seed =
hostSeed ^ (0x5A1D + i * 131). Unit (fanUnit) = hostScale * 0.90, spread
14 degrees, root 0xFFFFFF, tip 0x65D8FF, alpha 0.95. The fan is frozen:
angles never read age. Size breathes on the host travel curve: drawn
length multiplier = hostScale growth (0.55 + travel * 0.85) with travel
the cubic-out of age / 36, saturating at 1.40.

Entity version must match: 5 fragments, FROZEN mode, spawned at the host
center with axes along the five fan angles. Because the renderer fan
lives on a per-client camera plane, the entity fan needs a world plane:
the plane perpendicular to the caster's look direction at the spawn
moment, which the item knows server side (the shatter lands on the
caster's target). Phase 2 passes the plane basis (or equivalently the
host's facing) at spawn; each fragment's axis = the fan angle applied in
that plane, its position the host center. Fan angles replicate exactly:
the server rolls java.util.Random(hostSeed ^ 0xFA17) in the same order
(both sides are java.util.Random, whose sequence is specified and
stable). Per-fragment mesh seed = hostSeed ^ (0x5A1D + i * 131), then
the burst salt (+ 0 * 0x9E37 for the single shard) applies. Fragment
LENGTH base = fanUnit * class roll (the standard class ranges), and the
renderer applies the FROZEN growth rule from the standard (length
multiplier 0.55 + 0.85 * cubicOut(age / RELEASE_AT), saturating at
1.40), so the growth read survives the conversion exactly.

Release: RELEASE_AT preset to 36, the host's life. At tick 36 the host
discards and every fragment drops under gravity in the same tick, lands,
rests 10, fades 12, and is gone by about tick 60 plus fall time. This is
the conversion Carter named: they hang for real and drop when the
effect ends instead of vanishing.

Spawn point: server side, from item/WhisStaffItem.java at each
TimeShatterEntity spawn site (three sites, all constructing the host
with level, owner, pos, scale), immediately after the host is added:
FragmentEntity.hang x5 with the host's seed and scale, plane from the
player's look direction. Alternatively the host's first tick can spawn
them if the plane is stored on the host; the item-side spawn is the
spec because the item owns the facing at cast. Owner: the caster.

Counts and caps: 5 per shatter. The Whis left click can chain shatters
(stepped scales at one site); the area cap covers pileups.

Host keeps drawing: the shader crackFan on the shatter plane, SHARD_CHIP
chips along the crack spokes, glass dust streams, plasma and soul core,
and the color-split ghost streams. Only the five solid shards leave.

## 7. Grand Sword, Valor Field suspended shards

Host: entity/ValorFieldEntity.java (FIELD_RADIUS 9, follows the caster,
open-ended life ending through beginFadeOut and a 12 tick fade; its
projectile stasis system stores each suspended projectile's original
velocity and no-gravity flag, resumes them when they leave the radius
or when the field fades, detonates them on contact, and discards them
after 200 suspended ticks). Spawned by item/GrandSwordItem.java.
Renderer: client/render/ValorFieldRenderer.java.

Renderer composition today, inside one submitSolid:
(a) Three dome shards: index i at angle i * 2.35 + 0.6, ring radius
FIELD_RADIUS * (0.34 + 0.13 * i) = 3.06, 4.23, 5.40, height 1.9 + i *
0.85 plus a slow bob sin(age * 0.07 + i * 2.1) * 0.30, axis spinning
slowly (spin = age * 0.03 + i * 1.9, dir = (cos * 0.45, -0.35, sin *
0.45) normalized), drawn with ComplexGeometry.shard length 1.15, girth
0.30, seeds SEED_DOME + i * 104729, root 0x767F8C, tip 0xFFD833, alpha
0.95.
(b) Up to two marker shards at the first two suspended projectiles:
position = projectile position relative to the field + (0, 0.55, 0),
axis spinning at age * 0.05 + i * 2.4, shard length 0.75, girth 0.20,
seeds SEED_RING_OUTER + i * 31337, root 0x8A93A6, tip 0xFFF3B0.

Entity version must match: the same counts (3 dome, up to 2 markers),
sizes, colors, and seed families. Mode FIELD_HELD for both sets:
- Dome fragments: spawned by the field on its first full tick (the
  tickCount == 1 beat that already plays the spawn sound), anchored to
  the field with anchor offsets taken from the formulas above at spawn
  (angle, ring radius, base height without the bob; the bob and spin
  are presentation the fragment carries: spinDeg 1.72 per tick about Y
  server side (0.03 rad), and the renderer bob is dropped; the
  anchor follow replaces it, since the field moves with the caster and
  the bob was a +/- 0.30 read on a moving dome). Held while the field
  is up. Released from beginFadeOut (the field calls release() on its
  fragments on the same path that resumes suspended projectiles), then
  they drop, land, rest 10, fade 12, roughly inside the field's own
  dissipation.
- Marker fragments: spawned by the field at the moment it suspends a
  projectile, capped at 2 alive (the renderer's cap), anchored to that
  projectile with offset (0, 0.55, 0), spinDeg 2.86 per tick (0.05 rad).
  Released when their projectile leaves the suspended map for any
  reason: resumed (radius exit or field fade), detonated on contact, or
  discarded by the 200 tick stasis timer. The field already iterates
  every one of those paths in its tick; Phase 2 hooks the release call
  into the same removals, and the foundation's orphan check (holder
  removed) is the safety net when a path is missed.

Spawn point: server side, ValorFieldEntity.tick (dome set at tick 1,
markers inside the suspension loop). Owner: the caster, so the field's
own stasis scan (which skips projectiles owned by the caster) never
tries to suspend its own fragments, and Void Rift style loose-entity
pulls that exempt the owner's projectiles also exempt these.

Counts and caps: 3 + 2 per field, once per field life for the dome set;
markers recycle as projectiles come and go, never more than 2 alive.

Host keeps drawing: the six rim tendrils, all emission layers including
the stasis plasma at suspended positions, and the spark streak bursts.
Only the five shard meshes leave the renderer. The suspended
projectiles themselves are already entities and stay exactly as they
are; this conversion adds the fragments beside them and touches no
stasis behavior, timing, or damage.

## Spawn ownership summary

| Host | Spawn owner (server) | Mode | Count | Cadence |
|---|---|---|---|---|
| SkyCracks | SkyCracksEntity.tick | BALLISTIC (rain) | 5 (2/2/1) | every 20 ticks |
| Temporal Rift | TemporalRiftEntity.tick, tick 2 | BURST | 5 | once |
| Dimensional Rift | DimensionalRiftEntity.tick, tick 2 | BURST | 5 | once |
| Void Rift | VoidRiftEntity first tick | FROZEN, host release | 5 | once |
| Warp Rift | DimensionalWarpRiftEntity first tick | BURST | 4 per rift | once |
| TimeShatter | WhisStaffItem at host spawn | FROZEN, RELEASE_AT 36 | 5 | per shatter |
| Valor Field | ValorFieldEntity.tick | FIELD_HELD | 3 + up to 2 | once + on suspension |

All counts are far under the foundation caps (64 per call, 256 per 96
block area). No host spawns fragments from the client, ever; renderers
only lose their shard calls in Phase 2.
