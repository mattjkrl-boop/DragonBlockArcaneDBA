# Attack Upgrade Pass, Phase 1: Attack Complexity Audit and Upgrade Plan

Carter's playtest correction (Nov 4, 2026): "The attacks need to use
more shapes and whatnot like complex things not boring stuff." Plus
his follow-up the same day: "U can do like walls coming out and up
out of the ground and slamming down or a wall pushing out could do
whatever maybe realistic spikes enter out of ur body space or
something." Base: HEAD 8594bf8 plus the Phase 1 glow fix (same
commit). Sources: the current renderers under
src/client/java/com/dragonblockarcanedba/client/render/, the complex
pass reports (audits/complex-report-*.md), and the entity pass state.
This audit is a plan only: beyond the glow fix, no code changes here.

## Scope and method

In scope: everything the player sees when attacking with the 22
weapons: swing and trail visuals, projectile bodies and trails, beam
bodies, impact bursts, slam compositions, deployed shard visuals, and
the hit moment on targets. Out of scope: auras, forms, menus, status
effects, ambient fields. Border calls: Valor Field is a defense and
is excluded (Grand Sword is rated on its attacks); Dabura's eclipse
domain and Cursed Blade's abyssal domain are channeling attack bodies
and are included; Dread Gaze is a debuff visual and is excluded;
Saber Dodge Spark is mobility, excluded.

Ratings judge the PIECES layer first (authored ComplexGeometry:
branchedLightning, shard and shardBurst, fracturePlates, crackFan,
tendril and tendrilFan, flameTongues, plus real FragmentEntity and
DebrisEntity pieces), because pieces are what Carter found boring.
Sprite and material layers count second. THIN means the attack's body
is one or two ribbon calls or filler sprites with no solid geometry.
OK means a real pieces layer exists but at the complex pass's cautious
counts. RICH means layered pieces at class L weight; RICH attacks get
polish only, no count inflation.

Every spec below keeps the standing rules: no damage, cooldown, cost,
timing, mechanic, or synced field changes; pieces ride the existing
distance scaling so vertex budgets stay sane; physical matter (stone,
earth, metal, crystal, glass) draws through the new world-lit path
ComplexGeometry.submitSolidLit from the Phase 1 glow fix, never the
emissive type.

## The new family: walls and spikes (Carter, Nov 4, 2026)

Two new ComplexGeometry builders are specced for Phase 2 foundation
work, in the existing pattern (seeded, deterministic, size classes,
drawn through submit calls):

- rockWall(seed, origin, facing, length, height, palette): a row of
  interlocking irregular slabs along a line or shallow arc, built on
  fracturePlates technology grown vertical: 6 to 14 slabs per wall,
  each slab an irregular closed plate 0.7 to 1.6 blocks wide with
  seeded tilt, lean, and height variance, cracks between slabs shown
  by gaps and offset faces, not by texture. Drawn through
  submitSolidLit in ground-sampled stone colors, or through
  submitSolid only where the material itself is hot (magma seams are
  separate thin energy insets, never the slab bodies).
- spikeEruption(seed, center, layout, count, palette): realistic
  tapered spikes: bent, irregular closed spindles (the shard builder's
  technology at 3 to 6 times the size, with a mid-bend and surface
  facets), in line, ring, scatter, or body-space layouts. Lengths 1.2
  to 3.2 blocks in three classes. Stone, obsidian, iron, and crystal
  variants draw through submitSolidLit; only genuinely hot or magical
  cores use submitEnergy insets.

Animation is renderer-side over the host's existing envelope, three
beats: RISE (the wall or spikes translate up out of the ground over 6
to 10 ticks with an ease-out, shedding DUST material and SHARD_CHIP
sprites and a DebrisSpawner burst of 6 to 12 real chunks at the base),
HOLD (2 to 6 ticks at full height, dust settling), SLAM or SETTLE
(walls slam down over 3 to 4 ticks with a crackFan at the base and a
second debris burst; spikes either retract fast or crack and remain
as fading pieces for the host's remaining life, per weapon below).
These shapes are presentation layered on existing damaging beats:
the wall rises where the slam already lands, the spikes erupt where
the hit already happens. Where a spec says a shape is the visual body
of an existing beat, the beat's damage and timing are untouched; the
shape only gives the beat a body.

### Wall and spike assignments at a glance

WALLS: Ox King's Ax (fissure wall, rise and slam), War Mace (crater
rim wall ring, rise and slam inward), Z Sword (gravity curtain wall,
lifted then dropped), Lava Sword (basalt slab wall at the field edge),
Grand Sword (pushing debris-and-steel front on the Crescent Wave),
Bansho Fan (pushing debris front on the shockwave).
SPIKES: Devil Trident (brimstone spikes in the slam ring), Evil Spear
(dark iron spikes at the Hell Hunt impact and out of the space around
the caster on charge), Spike Flail (iron spike ring at the full-spin
beat), Cursed Blade (obsidian spikes on Ground Shatter), Dabura's
Sword (eclipse stone spikes at the domain rim, modest), Spirit Sword
(soul crystal spikes on the Cleave impact), Whis Staff (temporal
glass spikes on the Temporal Rift, modest).
NO WALL OR SPIKE SPEC: Katana, Saber, Brave Sword, Azure Dragon
Sword, Dimensional Sword, Hollow's Edge, Sickle of Sorrow, Power
Pole, Blaster Gun. Their identities are speed, storm, void, or tech;
ground architecture would fight the read. They get piece
multiplication inside their own themes instead. Carter can veto any
line of this spread; it is a proposal built from each weapon's
identity, not a promise.

## Worst offenders (biggest Phase 2 rebuilds)

1. SPIRIT CLEAVE (Spirit Sword). The thinnest attack in the mod: its
   entire pieces layer is two tendril ribbons in one submitEnergy
   call (SpiritCleaveRenderer.java line 147), and its sprite layers
   are all soft filler (soul, vapor, one SPARK, one WISP_CURL stream;
   12 emit calls total, the lowest count of any attack renderer).
   There is no solid geometry and no impact composition at all. The
   weapon whose sibling beam is class L has a basic cleave made of
   two ribbons and mist.
2. GRAND CRESCENT WAVE (Grand Sword). The sword's signature ranged
   attack is two tendril ribbons (GrandCrescentWaveRenderer.java
   lines 153 and 159) wrapped in plasma and vapor filler (17 emit
   calls). No shards, no plates, no crack, nothing solid for the eye
   to hold, on the attack that is supposed to feel like a thrown
   bulwark of valor.
3. BLASTER BOLT (Blaster Gun). The gun's every-shot attack: two
   tendril ribbons (BlasterBoltRenderer.java line 175) with the body
   carried by plasma sprites. The class L Erasure Cannon proves the
   kit can do better; the shot the player sees a hundred times a
   fight is the boring one.

Honorable mention: SkyCracks (Cursed Blade) thinned out when its
shards became real FragmentEntity rain in the entity pass; the
renderer is down to two crackFans and filler. The pieces exist, but
they left the tear heart empty. Its spec below refills the heart.

## Per weapon audit and upgrade spec

### 1. Katana

Today: KatanaChargeRenderer and KatanaAimGuideRenderer (class M,
one tendrilFan each, sprite support), HeavenSplitterRenderer (class
L, crackFan plus tendril, 38 emit calls, the kit's rich piece),
SwiftCrescentRenderer (class M: ONE tendril arc carries the slash,
lines 124 to 125, over a rich plasma and spark sprite stack).
Rating: OK overall; Swift Crescent is pieces-THIN.
Upgrade spec: Swift Crescent doubles its pieces: add a second offset
tendril arc on the opposite lip (seeded apart, 60 percent alpha of
the main arc), a shardBurst of 5 to 7 small steel-bright shards along
the arc's leading edge at the swing's peak tick (submitSolidLit,
silver gradient, size class S), and a short crackFan of 3 branches
at the arc's ground contact when the swing is aimed low. Heaven
Splitter: add a shardBurst crown of 6 shards above the split point
and a second crackFan mirrored at half radius; counts otherwise
unchanged. Charge and Aim Guide: add 3 orbiting shard pieces
(submitSolidLit) to the charge fan so the charge reads as gathering
steel, not only light. No wall or spike spec: speed and precision
identity. Must not change: all swing timings, the aim guide's
telegraph read, Heaven Splitter's split geometry positions.

### 2. Z Sword

Today: ZChargeRenderer (class M, tendrilFan), ZGravitySlamRenderer
(class L, fracturePlates plus crackFan, 33 emit calls),
ZShockwaveRenderer (class M, plates plus crackFan, 32 emit calls),
DelayedLaunchRenderer (class M, crackFan plus tendrilFan: debris
hangs, then drops). Rating: OK, slam-leaning RICH.
Upgrade spec: WALL. Delayed Launch and Gravity Slam gain a gravity
curtain: a rockWall of 8 to 10 slabs (ground-sampled palette,
submitSolidLit) that the gravity beat lifts out of the ground along
the slam's front edge during the existing wind-up (RISE stretched to
the launch delay, slabs tilting as they hang), then drops with the
slam (SLAM beat: crackFan full radius at the base, DebrisSpawner
burst 10 to 14 chunks, DUST ring). The wall is the visual body of
the existing launch-and-drop beat; nothing about the delay, radius,
or damage changes. Z Shockwave: plates count up by half (one more
plate ring at 1.3 times radius, smaller class), crackFan branches
plus 2. Z Charge: add 4 suspended shard pieces above the blade
(submitSolidLit stone) that drop and shatter (SHARD_CHIP burst) when
the charge releases. Must not change: DelayedLaunch timing and its
debris field counts, gravity slam radius and falloff.

### 3. Saber

Today: SaberSlashRenderer (class M, 3 tendrils, 34 emit calls),
SaberDimensionalLineSlashRenderer (class M, 2 tendrils, 36 emit
calls), SaberVoidTearRenderer (class M, shard plus tendril, 36 emit
calls; terrain-free by identity). Rating: OK; every piece is a
ribbon, solids nearly absent.
Upgrade spec: no wall or spikes (void-speed identity). Slash gains a
solid edge: a shardBurst of 4 to 6 thin blade shards
(submitSolidLit, pale steel) flying the slash arc's plane at the
swing tick, plus a second tendril pair crossing the first at a
seeded offset so the cut reads woven. Line Slash: multiply tendrils
2 to 4 with staggered phase, and add a crackFan of 4 short branches
in the slash plane at the line's midpoint (in-air crack, void style,
submitEnergy). Void Tear: shard count 1 to 3 (one large, two small
flanking), plus ASH_FLAKE and SHARD_CHIP sprite layers doubled at
the tear mouth. Must not change: the terrain-free rule (no ground
pieces, no debris), dodge timings, tear placement.

### 4. Brave Sword

Today: BraveChargeRenderer (tendrilFan), BraveCrossSlashRenderer
(2 tendrils, 32 emit calls), BraveRushTrailRenderer (3 tendrils),
BraveShockwaveRenderer (class L, plates plus crackFan, 32 emit
calls), BraveSlashRenderer (2 tendrils, 26 emit calls). Rating: OK.
Upgrade spec: no wall or spikes; Brave is holy momentum. Cross Slash
gains a pieces cross: two shardBursts of 4 shards each along both
slash planes at the crossing tick (submitSolidLit, bright steel with
gold tips), so the X has a solid heart. Slash: tendrils 2 to 3 plus
a shardBurst of 3 at the arc apex. Rush Trail: add a trailing
shardBurst shed (2 shards every 6 ticks, small class) so the rush
leaves steel, not only light. Shockwave (already L): plates stay,
add one flameTongues ring in brave gold at the plate rim, counts
otherwise unchanged. Charge: tendrilFan strands plus 2. Must not
change: cross slash timing, rush trail lifetime, shockwave radius.

### 5. Grand Sword

Today: GrandBladeShardRenderer (class M, real shard entities with
steel shard bodies, now world-lit after the glow fix, plus gold
tendril wakes), GrandClashSparkRenderer (class M, tendrilFan, 20
emit calls), GrandCrescentWaveRenderer (class M, THIN, worst
offender 2). Valor Field excluded (defense). Rating: THIN on the
signature attack, OK elsewhere.
Upgrade spec: REBUILD the Crescent Wave. The wave gains a body in
three layers: (a) a pushing front: a low rockWall arc of 6 to 8
steel-and-stone slabs (submitSolidLit, steel palette with gold
insets on submitEnergy) traveling at the wave's leading edge, slabs
leaning into the push, shedding SHARD_CHIP and dust as it goes: the
wall pushing outward, riding the existing wave entity and its exact
path and speed; (b) a shardBurst escort of 6 to 8 valor shards
(submitSolidLit) orbiting the front; (c) the two existing tendrils
stay as the wave's energy skin, plus one flameTongues row along the
front's top edge. Clash Spark: tendrilFan strands doubled, plus a
shardBurst of 5 at the clash point. Blade Shard visuals: flight
tendril wake strands 1 to 2. Must not change: wave speed, width,
damage, and lifetime; shard counts and swarm behavior; the stasis
system (untouched, out of scope).

### 6. Azure Dragon Sword

Today: AzureLightningRenderer (class M telegraph, crackFan plus
tendrilFan) feeding the shared BoltRenderer body (branchedLightning
with 51 emit calls, RICH), AzureRushTrailRenderer (3 tendrils),
AzureSonicQuakeRenderer (class L, plates plus crackFan),
AzureStormRenderer (2 tendrilFan, 30 emit calls),
AzureTempestChannelRenderer (1 tendril, 30 emit calls),
AzureTornadoRenderer (class L, tendrilFan plus real DebrisSpawner
fields). Rating: RICH overall; Tempest Channel is the thin sibling.
Upgrade spec: no wall or spikes (storm identity). Tempest Channel:
tendril 1 call to a tendrilFan of 5 strands, plus a ring of 6 small
shard pieces (submitSolidLit, storm-glass blue) orbiting the channel
at waist height. Storm: tendrilFan strands plus 2 per fan, and a
shardBurst shed at the storm's rim every 10 ticks (4 shards, small).
Sonic Quake: second crackFan at 0.6 radius with reversed fork
direction for a woven crack read. Rush Trail: add SPARK_STREAK layer
doubled; pieces unchanged. Lightning telegraph: crackFan branches
plus 2. Must not change: bolt damage path (byte-identical branch
math stays), tornado debris counts, channel timing.

### 7. Dabura's Sword

Today: DarknessBladeRenderer (class M, tendrilFan on the veil type),
DarknessChargeRenderer (tendril), DarknessDomainRenderer (class L,
crackFan plus tendril, 33 emit calls; the eclipse domain is the
channeling attack body), DarknessShatterRenderer (plates plus
crackFan, 29 emit calls), DarknessWaveRenderer (tendril on veil).
Rating: OK.
Upgrade spec: SPIKES, modest. The domain gains an eclipse spike
rim: a spikeEruption ring of 10 to 14 dark stone spikes
(submitSolidLit, near-black basalt palette with violet energy seams
on submitVeil insets) rising at the domain's rim over the existing
channel build-up (RISE matched to the channel envelope), holding
while the domain holds, and cracking away (SHARD_CHIP burst,
retract) when it ends. The spikes are the rim's body; the domain's
projectile-slowing and damage behavior are untouched. Darkness Wave:
tendril 1 to tendrilFan of 4. Darkness Blade: add 3 shard pieces
(submitSolidLit, dark steel) riding the blade arc. Shatter: plates
plus one ring at 1.25 radius, smaller class. Must not change: domain
radius growth, slow factor, channel timing.

### 8. Cursed Blade

Today: AbyssalDomainRenderer (class L, crackFan plus tendril),
CurseChainRenderer (tendril, 25 emit calls),
CurseGroundShatterRenderer (plates plus crackFan, 23 emit calls),
CurseLightningRenderer (branchedLightning, 20 emit calls),
CurseTelegraphRenderer (crackFan plus tendrilFan),
SkyCracksRenderer (2 crackFans, 16 emit calls, plus FragmentEntity
rain from the entity pass). Rating: OK; SkyCracks heart is empty.
Upgrade spec: SPIKES on Ground Shatter: a spikeEruption scatter of
8 to 12 obsidian spikes (submitSolidLit, black glass palette) bursting
up through the shatter disc on the hit beat (fast RISE 4 ticks, no
hold, crack-and-fade settle), with the existing plates pushed to the
rim. SkyCracks: refill the tear heart with a hanging shardBurst of
5 shards (submitSolid, curse violet glass: an energy material, so
the emissive path stays correct here) that pulse with the bolt-shed
beat the rain already uses, plus crackFan branches plus 2 per fan.
Chain: tendril doubled into a paired chain (two offset strands) and
a shardBurst of 3 at the anchor point. Abyssal Domain: crackFan
branches plus 3. Must not change: SkyCracks rain counts and cadence,
chain timing, domain behavior, bolt path.

### 9. Hollow's Edge

Today: HollowAfterimageRenderer (2 tendrils plus tendrilFan on veil,
33 emit calls), HollowChargeRenderer (tendrilFan),
HollowRushTrailRenderer (4 tendrils, 37 emit calls),
VoidRiftRenderer (2 tendrils plus the FragmentEntity ring from the
entity pass), VoidSlashRenderer (2 tendrils, 21 emit calls).
Rating: OK; Void Slash is THIN-leaning.
Upgrade spec: no wall or spikes (void identity; its solids are
fragments already). Void Slash: tendrils 2 to 4 in two crossing
pairs, plus a shardBurst of 4 void shards (submitSolid, near-black
violet) along the slash plane at the swing tick. Rush Trail: add a
SHARD_CHIP shed layer and one extra tendril pair at half alpha.
Afterimage: tendrilFan strands plus 2. Void Rift: rim tendrils 2 to
3, and the fragment ring gains a second inner ring of 3 smaller
fragments (FragmentEntity.hold, same release path) if the merge's
ring read in the PC test is sparse; renderer-side fallback is a
shardBurst of 3 at the mouth. Must not change: rift drag, growth,
implosion timing, fragment release rules.

### 10. Dimensional Sword

Today: DimensionalSlashRenderer (class L, 3 tendrils, 26 emit
calls), DimensionalWarpRiftRenderer (crackFan plus tendril, plus
FragmentEntity bursts from the entity pass). Rating: OK.
Upgrade spec: no wall or spikes (space-cutting identity). Slash:
tendrils 3 to 5 with seeded phase offsets, plus a crackFan of 5
branches in the slash plane at the cut's center (in-air dimensional
crack, submitEnergy) and a shardBurst of 4 crystal shards
(submitSolid, warp violet glass) at the cut's ends. Warp Rift:
crackFan branches plus 2, tendrils 1 to 2, and the destination rift
gains a mirrored crackFan so arrival reads as violent as departure.
Must not change: warp distances, rift lifetimes, fragment counts
(4 per rift stands).

### 11. Spirit Sword

Today: SpiritCannonBeamRenderer (class L, tendril plus tendrilFan),
SpiritCleaveRenderer (class M, THIN, worst offender 1),
SpiritImpaleRenderer (2 shard calls plus tendril, 17 emit calls;
its shards became a real impale composition in the bespoke pass).
Rating: THIN on the basic attack, OK on the beam.
Upgrade spec: REBUILD the Cleave. (a) Pieces body: the two tendrils
become a tendrilFan of 5 strands (soul cyan and white), so the cleave
has a woven body. (b) SPIKES: on the cleave's hit beat, a
spikeEruption fan of 5 to 7 soul crystal spikes (submitSolidLit,
pale cyan glass palette) erupts from the ground at the hit point in
the swing's direction (fast RISE 4 ticks, HOLD 3, crack-and-fade),
giving the hit a physical moment it has never had. (c) Sprite
layers: add SPARK_STREAK and SHARD_CHIP bursts at the hit point and
double the soul streams. (d) A shardBurst of 4 soul shards
(submitSolidLit) rides the arc's leading edge. Impale: shard count
per impale plus 1 size class up on the main shard, tendril wake
strands doubled. Cannon Beam: polish only (tendrilFan strands plus
1, core stays). Must not change: cleave arc, range, and cooldown;
impale timing; beam damage path and width.

### 12. Evil Spear

Today: EvilSpearChargeRenderer (flameTongues plus tendrilFan, 28
emit calls), EvilSpearProjectileRenderer (flameTongues plus 2
tendrils, 29 emit calls), HellHuntImpactRenderer (crackFan plus
tendrilFan, 42 emit calls). Dread Gaze excluded (debuff). Rating:
OK, sprite-strong, pieces-moderate.
Upgrade spec: SPIKES, two homes. (a) Hell Hunt impact: a
spikeEruption ring of 8 to 10 dark iron spikes (submitSolidLit,
blackened steel) bursting from the ground around the hunted target
on the impact beat (RISE 5 ticks, HOLD 4, retract), inside the
existing crackFan: the hunt made physical. (b) Charge: Carter's
body-space beat: 6 shorter spikes (1.2 to 1.8 blocks) erupting out
of the space around the caster's body at full charge, angled outward
like a burst of spearheads (spikeEruption body-space layout,
submitSolidLit), holding while the charge holds and retracting on
release. Projectile: flameTongues rows plus 1, plus a shardBurst
escort of 3 iron shards at launch. Must not change: charge timing
and release rules, hunt targeting and damage, projectile speed.

### 13. Devil Trident

Today: DevilSlamShockwaveRenderer (class L: fracturePlates,
crackFan, flameTongues, 2 solid submits, 51 emit calls; the effect
built from Carter's target picture), TridentProngRenderer (shard,
flameTongues, tendril), TridentShardRenderer (real shard entities
with shard bodies and tendril wakes). Rating: RICH. The flagship
stays the flagship; spec is additive only.
Upgrade spec: SPIKES. The slam gains a brimstone spike ring: a
spikeEruption ring of 10 to 12 spikes (submitSolidLit, blackstone
and basalt palette; magma seams as thin submitEnergy insets only)
erupting between the fracture plates and the crack web on the slam
beat (RISE 6 ticks staggered by ring position, HOLD while the seal
holds, then crack and settle as fading pieces). Sizes deliberately
inside the seal radius like the plates (0.90) so the composition
stays layered, not crowded. Prong: flameTongues rows plus 1 at the
prongs. Shards: no change (the swarm is lore and already real).
Must not change: slam damage and falloff, crater zoning, shard swarm
behavior, deploy and recall rules.

### 14. Sickle of Sorrow

Today: DimensionalRiftRenderer (tendrils plus the FragmentEntity
burst and a 60-piece DebrisSpawner field from the host),
SorrowSlashRenderer (2 tendrils, 26 emit calls). Rating: OK; the
slash is THIN-leaning.
Upgrade spec: no wall or spikes (rift identity). Sorrow Slash:
tendrils 2 to 4 (two pairs, seeded phase apart), plus a shardBurst
of 5 sorrow shards (submitSolid, violet crystal) along the arc and a
crackFan of 3 short branches at the arc's low point when grounded.
Rift: the entity-pass burst gains a second authored beat at peak
tear size (the follow-up the entity pass already flagged): 4 more
FragmentEntity bursts at tearY peak, same palette and rules, so the
grown tear is not emptier than the young one. Renderer tendrils plus
1 strand. Must not change: rift growth curve, debris field count,
fragment lifetimes.

### 15. Ox King's Ax

Today: KingsSlamRenderer (class L, plates plus crackFan, full
disc), OxChargeRenderer (tendrilFan), OxFissureRenderer (2 crackFans
plus plates, 17 emit calls), OxShockwaveRenderer (class L, plates
plus crackFan, wired to real PermanentDestruction). Rating: RICH on
slams, OK on the fissure.
Upgrade spec: WALL, the flagship wall. The fissure gains a rising
rock wall: a rockWall of 9 to 12 slabs (ground-sampled palette,
submitSolidLit) erupting along the fissure line as the fissure runs
(RISE staggered slab by slab, 5 to 8 ticks each, DUST and debris at
every base), HOLD 4 ticks at full height (2.2 to 3.0 blocks), then
the whole wall SLAMS down flat (3 ticks) with a full-length
crackFan, a DebrisSpawner burst of 12 to 16 chunks, and the existing
fissure damage beat unchanged underneath: the wall is the fissure's
body, rising and falling on the fissure's own timing. Shockwave:
plates count plus one inner ring; Kings Slam: crackFan branches
plus 3 and a spikeEruption scatter of 6 short rock spikes at the
disc rim (submitSolidLit) on the slam beat. Charge: tendrilFan
strands plus 2 and EMBER_FRAGMENT layer doubled. Must not change:
destruction tiers, caps, and protected-block rules (byte-level
sacred), fissure path and timing, slam radii.

### 16. Power Pole

Today: PowerPoleExtensionRenderer (2 tendrils, 29 emit calls),
PowerPoleImpactRenderer (ONE crackFan carries the whole hit, 33
sprite emit calls), PowerPoleWhirlwindRenderer (tendrilFan plus
debris field). Rating: OK; the Impact is pieces-THIN.
Upgrade spec: no wall or spikes (the pole is the shape). Impact
rebuild: the crackFan gains a fracturePlates ring of 6 to 8 plates
at the hit radius (submitSolidLit, ground palette), a shardBurst of
5 stone chips (submitSolidLit) at the contact point, and a second
crackFan at half radius with reversed forks; sprite counts stay.
Extension: tendrils 2 to 3 with a braided phase, plus SPARK_STREAK
shed along the shaft at extension speed. Whirlwind: tendrilFan
strands plus 2; the spin-speed strobing question from the complex
pass stays a PC-test call, counts here do not touch spin rates.
Must not change: extension length and timing, whirlwind parry
window, impact radius.

### 17. Whis Staff

Today: TemporalRiftRenderer (3 inward tendrils plus the
FragmentEntity burst from the entity pass),
TimeShatterRenderer (crackFan plus the FragmentEntity fan; the fan
is the pieces layer and it is real). Rating: OK by design; its
pieces are entities, which is the point of the last pass.
Upgrade spec: SPIKES, modest, crystal variant. Temporal Rift gains
a temporal glass spike ring: a spikeEruption ring of 7 to 9 glass
spikes (submitSolidLit, white-to-ice palette matching the rift's
fragments) rising slowly at the rift's rim across its first 20 ticks
(slow RISE matched to the rift's build), holding, and dropping as
real FragmentEntity bursts when the reversal beat fires, reusing the
entity pass's fall and rest read. TimeShatter: crackFan branches
plus 2 and a second smaller crackFan counter-rotated; the fragment
fan is unchanged (its regularity question is a PC-test call).
Must not change: rift ticks (peak, reversal), fragment counts and
release rules, shatter timing (RELEASE_AT 36 stands).

### 18. Bansho Fan

Today: BanshoCycloneRenderer (class L, tendrilFan, real debris
fields), BanshoGaleRenderer (3 tendrils, 18 emit calls),
BanshoShockwaveRenderer (plates plus crackFan, 45 sprite emit
calls), BanshoWindProjectileRenderer (2 tendrils, 23 emit calls).
Rating: OK to RICH.
Upgrade spec: PUSH WALL on the Shockwave: the shockwave's front
becomes a visible pushing wall of the ground itself: a rockWall arc
of 7 to 9 slabs (ground-sampled, submitSolidLit) at the wavefront,
leaning hard into the travel direction, riding the shockwave's
existing expansion (the wall pushes outward with the front, slabs
crumbling progressively: rear slabs drop and shatter into
DebrisSpawner chunks as the front passes). This is Carter's wall
pushing out, made of the terrain the wind is shoving. Gale:
tendrils 3 to 5. Wind Projectile: tendrils 2 to 3 plus a
shardBurst of 3 wind-glass shards (submitSolid, pale) at the core.
Cyclone: no change (already debris-rich). Must not change:
shockwave expansion speed and radius, projectile pathing, cyclone
debris counts.

### 19. Blaster Gun

Today: BlasterBoltRenderer (class M, THIN, worst offender 3),
ErasureCannonBeamRenderer (class L, crackFan plus 2 tendrils plus
tendrilFan, 34 emit calls), ErasureChargeOrbRenderer (tendrilFan,
29 emit calls). Rating: THIN on the everyday shot, RICH on the
cannon.
Upgrade spec: REBUILD the Bolt body. (a) A solid core: a small
shardBurst-shaped casing of 4 interlocked plates (submitSolid, gun
palette: dark casing, hot core inset on submitEnergy) replaces the
bare plasma ball as the bolt's body, spinning slowly in flight.
(b) The two tendrils become a tendrilFan of 4 strands as the wake.
(c) A SPARK_STREAK shed layer along the path, and on impact a
shardBurst of 5 casing fragments (submitSolid) plus the existing
burst, so hits land with pieces. Charge Orb: tendrilFan strands
plus 2 and an orbiting shard pair (submitSolid). Erasure Cannon:
polish only (crackFan branches plus 2 at the impact). No wall or
spikes: tech identity. Must not change: bolt speed, size, damage,
fire rate; cannon channel and beam path.

### 20. Lava Sword

Today: LavaBlobRenderer (flameTongues), LavaCleaveRenderer
(flameTongues plus tendril), LavaEruptionRenderer (class L,
crackFan plus flameTongues), LavaGeyserRenderer (flameTongues),
LavaMeteorRenderer (class L, plates plus crackFan plus flameTongues,
the kit's other flagship). Rating: RICH.
Upgrade spec: WALL. The Lava Field gains a basalt wall: when a field
is established, a rockWall of 7 to 10 basalt slabs (submitSolidLit,
dark basalt; magma seams as submitEnergy insets between slabs)
rises along one field edge over 10 ticks (slow RISE, EMBER_FRAGMENT
shed), stands while the field lives, and crumbles into the field
(DebrisSpawner burst, crust conversion as today) when the field
ends. The wall is set dressing on the field's existing lifetime; it
damages nothing and blocks nothing (no collision, presentation
only). Cleave: flameTongues rows plus 1 and a shardBurst of 4
basalt shards (submitSolidLit) on the hit beat. Eruption: crackFan
branches plus 2. Meteor and Geyser: unchanged. Must not change:
lava placement budgets, crust settling rules, the is_fire design
call (still Carter's), meteor counts.

### 21. Spike Flail

Today: SpikeFlailSpinRenderer (2 tendrils, 57 sprite emit calls:
the sprite-heaviest M in the set), SpikeFlailSwingRenderer (3
tendrils, 27 emit calls). Rating: OK sprites, pieces all ribbons.
Upgrade spec: SPIKES, name identity. At the spin's full-speed beat,
an iron spike ring erupts: a spikeEruption ring of 9 to 11 iron
spikes (submitSolidLit, dark iron) bursting from the ground at the
spin's radius (fast RISE 4 ticks, HOLD while full speed lasts,
retract on spin-down), timed to the existing spin envelope; the ring
is presentation on the spin's existing damaging life. Swing: add a
shardBurst of 4 iron shards (submitSolidLit) at the swing's apex
and a third tendril pair on the chain line. Spin: tendrils stay 2
(the strobing caution from earlier passes applies: do not multiply
spin visuals), sprite layers trimmed by a quarter to make room for
the ring's read. Must not change: spin radius, speed curve, parry
and damage behavior.

### 22. War Mace

Today: WarMaceRiseRenderer (class M, plates plus crackFan, 34 emit
calls), WarMaceSmashRenderer (class L, plates plus crackFan, 58
sprite emit calls, the crater flagship, wired to real destruction).
Rating: RICH.
Upgrade spec: WALL. The smash gains a crater rim wall: a rockWall
ring of 10 to 13 slabs (ground-sampled, submitSolidLit) around the
crater rim that rises with the smash's telegraph (RISE over the
rise renderer's existing build), holds through the impact frame,
then SLAMS down and inward (3 ticks) as the crater forms, ending as
a DebrisSpawner burst of 12 chunks plus the existing plate and crack
composition. The wall gives the crater a before-and-after: ground
heaves up, then the mace brings it down. Rise renderer: plates plus
one ring, crackFan branches plus 2. Smash: sprite counts trimmed
slightly (58 is the mod's highest) to keep the wall readable; pieces
counts otherwise unchanged. Must not change: crater radius, cap,
destruction tier, smash timing.

## Phase 2 execution notes

- Foundation first: rockWall and spikeEruption builders land in
  ComplexGeometry with the standard's gates (seeded, size classes,
  vertex budgets measured), plus per-layout unit checks, before any
  weapon owner touches them. Both builders need a lit draw entry:
  they emit through submitSolidLit for physical palettes and keep a
  submitEnergy inset variant for seams and cores.
- Order: worst offenders first (Spirit Cleave, Grand Crescent Wave,
  Blaster Bolt), then wall and spike flagships (Ox, War Mace, Z
  Sword, Devil Trident), then the multiplication specs.
- Clutter flags: Spike Flail spin and Power Pole whirlwind keep
  their spin visuals unmultiplied (strobing risk already on record);
  War Mace Smash and Devil Slam are at sprite ceilings and trade
  sprite count for pieces rather than adding both.
- Every Phase 2 owner re-checks the no-glow rule: any new physical
  piece drawn on the emissive path is a merge-gate failure, same as
  a damage change.
- Verification per owner and at merge follows the standing pattern:
  targeted javac during the pass, dual sweeps at merge, honest worse
  checks, no Gradle claim, PC test is the first real look.
