# Bespoke Pass Phase 2 Report: Brave Sword

Weapon agent report for the Brave Sword, against HEAD 2ef870b (Bespoke Pass
Phase 1). Binding rules: audits/BESPOKE-STANDARD.md. Files touched: only the
11 Brave files listed below. Nothing committed; the coordinating pass owns
commits. HeroicFocusEffect and everything under effect/ untouched. No sky
charge classes exist anywhere in the repo (searched), so there were no sky
charge pieces to convert.

Files in scope:

- item/BraveSwordItem.java
- entity/BraveChargeEntity.java (no changes needed, see below)
- entity/BraveSlashEntity.java
- entity/BraveCrossSlashEntity.java
- entity/BraveRushTrailEntity.java
- entity/BraveShockwaveEntity.java
- client/render/BraveChargeRenderer.java
- client/render/BraveSlashRenderer.java
- client/render/BraveCrossSlashRenderer.java
- client/render/BraveRushTrailRenderer.java
- client/render/BraveShockwaveRenderer.java

## Before and after counts

| Category | Before | After |
| --- | --- | --- |
| KiRenderHelper primitive calls (drawColoredBox, drawCylinder, drawDisc, drawSphere, drawHelix) | 0 | 0 |
| Local draw helpers in the 5 renderers | 18 | 0 vertex emitting helpers |
| Vanilla ParticleTypes call sites | 2 | 0 |
| Vanilla SoundEvents call sites | 8 | 0 |
| Vanilla ability damage source constructions | 12 across 5 files | 0 |
| DbaDamage.valor call sites | 0 | 6 |

Local draw helpers deleted, per renderer: BraveChargeRenderer 4
(drawRotatingRing, draw3DShard, drawOctahedron, drawQuad),
BraveCrossSlashRenderer 5 (drawCrossBlade, drawRolledCrescent,
drawRolledSpur, rolledVertex, drawGeometricStarburst),
BraveRushTrailRenderer 3 (drawTriangle, drawQuad, drawRing),
BraveShockwaveRenderer 5 (drawValorSpikes, drawStarburstCore,
drawBeveledShockRing, drawFlatRing, drawTriangle), BraveSlashRenderer 1
(drawHeroCut). Two pose and composition helpers remain
(BraveCrossSlashRenderer.drawCross, BraveRushTrailRenderer.sheet); neither
emits a vertex, both only position the pose stack and call BespokeGeometry
builders, which is the standard's composition model rather than a local
geometry helper.

Damage constructions before, counted individually: item finisher 1
(playerAttack), slash ternary 3 (playerAttack, mobProjectile, generic),
cross slash ternary 3, rush trail 2 (playerAttack, mobAttack), shockwave
ternary 3. After: zero. Every ability hit goes through DbaDamage.valor with
the ability entity as the direct entity and the owner as the attacker
(valor(this, owner) form; the item finisher uses valor(player, player)).
With no owner, valor anchors on the direct entity, so behavior for
ownerless projectiles matches the old generic fallback in reach, with the
valor type instead. The plain melee weapon hit is untouched: it is the item
attack attribute path (800 damage, attack speed -1.8), not an ability
damage source, exactly as the standard prescribes.

## Geometry conversion

Every renderer seed comes from the entity UUID (most significant bits XOR
least significant bits), extracted in extractRenderState into a new seed
field on each render state. The UUID is part of the spawn packet, so it is
synced state and every client draws identical torn edges, fans, and bands
for the same entity. No renderer reads a client local random for geometry.

Builders used per renderer:

- BraveSlashRenderer: bladeArc in three nested layers (gold 0.90, cyan
  0.95, white 1.0 alpha, all at the spectacle fade envelope), radius
  span times 1.35 with sweep 140 degrees, recentered back by span times
  0.55 so the crescent straddles the flight line like the old cut.
  shardFan times two (white over warm gold) for the strike flash at the
  belly, radius 0.38 and 0.24 times the strike bump.
- BraveCrossSlashRenderer: bladeArc times three layers per blade, two
  blades rolled plus and minus 45 degrees in the blade plane, replayed on
  the additive type at 1.16 times radius and ALPHA_OUTER as the spectacle
  pass set. shardFan tip fans at the four arm ends (where the old rolled
  spurs sat) and a two tone shardFan starburst (gold 1.15 times scale
  times strike, cyan at 0.62 of it) in the strike window.
- BraveChargeRenderer: glowFan times three for the ground runes at the old
  ring radii, heights, and alphas (gold 1.15, cyan 0.82, white-gold 0.50
  times baseRadius), plus a fourth glowFan on the additive pass at the old
  halo ring radius and alpha. shardFan times two per converging shard
  (gold body, cyan heart), same orbit radius, speed, bob, and count math
  as the old prisms, each fan aimed at the core point (0, 0.9, 0).
  displacedDome times two per core layer (one flipped 180 degrees about X
  for the underside), three layers at the old octahedron size factors
  1.0, 0.58, 0.22, speeds 22, -32, 55 degrees per tick, colors, and
  alphas, plus a 1.25 times dome pair on the additive pass for the old
  halo octahedron. tornRibbon times two per strand (strand color plus
  white core at the old 0.28 width ratio) for the twin streamers, peeling
  up off the ground ring at the old helix base points and spin rate.
  LayeredRender.drawAnchorGlow kept verbatim (already bespoke).
- BraveRushTrailRenderer: tornRibbon sheets in four planes crossed about
  the flight axis (horizontal, vertical, plus and minus 45 degrees), two
  ribbons per sheet. They carry the old cone layers and streamlines: gold
  body at the old cone alpha ratio (0.72, or 0.32 for the first person
  owner), cyan inner at 0.85 length and 0.84 width, white-gold core beams
  at 0.72 length, and the gold and cyan streamline strands at the old
  0.20 times scale width with white cores at the old 0.28 ratio.
  brokenVortexBand for the strike wake at the tip, radius coneBase times
  (0.55 plus 0.65 times strike), alpha strike times 0.85, as the old ring.
- BraveShockwaveRenderer: brokenVortexBand times four: gold wisp band
  ahead of the front at ALPHA_OUTER (the old flat wisp ring), gold crest
  band at full crest height, cyan harmonic band at 0.96 radius and 0.55
  crest height, white core band at the old core ring radius. crackStar
  times two for the ground work under the front (gold at the front
  radius, cyan at 0.7 of it). shardFan for the epicenter burst (gold over
  cyan, pose spun at the old starburst rate of 14 degrees per tick) and
  one shardFan per valor spike, stood in the radial plane with the same
  count (segments 12 down to 6), rotation rate 0.045, and height formula
  as the old spike triangles, center lifted by 0.45 times height so the
  fan crests where the old tips did. LayeredRender.drawAnchorGlow kept
  verbatim.

All lifecycle math is unchanged: envelope fractions, draw-out windows,
strike windows (0.36 slash, 0.28 to 0.62 cross, 0.34 rush), the shockwave
sine expansion ease matching the entity's front radius, first person
owner scaling on the rush trail, and every shouldRender overload.

## Particle conversion

- BraveSwordItem assault feedback: ParticleTypes.CRIT to
  DbaParticles.CRIT_SPARK, count 8, spread 0.3, speed 0.1, unchanged.
- BraveSlashEntity on-hit sparks: ParticleTypes.CRIT to
  DbaParticles.CRIT_SPARK, count 10, spread 0.3 / 0.5 / 0.3, speed 0.15,
  unchanged.
- Everything else in these files already used DbaParticles or
  LayeredParticles (which returns DbaDustOptions). No DustParticleOptions
  anywhere in Brave files, before or after.

## Sound conversion

All volumes and pitches are byte identical to the spectacle pass. Where a
vanilla sweep was already paired with a DbaSounds.SLASH_WHOOSH at the same
volume and pitch, the whoosh stays and the vanilla half is swapped per the
DbaSounds guide by weight, so the two layer design survives with both
layers bespoke:

| Site | Before (vanilla layer) | After |
| --- | --- | --- |
| Finisher hit | PLAYER_ATTACK_SWEEP 2.0 / 0.6 | SWING_HEAVY 2.0 / 0.6 |
| Finisher boom | GENERIC_EXPLODE 1.8 / 1.2 | BOOM_HOLY 1.8 / 1.2 |
| Heavy assault hit | PLAYER_ATTACK_SWEEP 1.5 / 1.4 | SWING_HEAVY 1.5 / 1.4 |
| Normal assault hit | PLAYER_ATTACK_SWEEP 1.2 / pitch rises with combo | SWING_LIGHT, same |
| Empty swing | PLAYER_ATTACK_SWEEP 1.0 / 1.35 | SWING_LIGHT 1.0 / 1.35 |
| Charge tick (every 20 ticks) | BEACON_ACTIVATE 0.9 / 0.8 plus ratio times 0.5 | CHARGE_RISE, same |
| Release | PLAYER_ATTACK_SWEEP 2.0 / 0.7 | SWING_HEAVY 2.0 / 0.7 |
| Rush trail shockwave trigger | GENERIC_EXPLODE 2.0 / 0.9 | BOOM_HOLY 2.0 / 0.9 |

## Numbers and mechanics check

Every spectacle number was re-read in the current code and is unchanged:
melee 800 and -1.8 attack speed; assault 400 plus Strength times 2.0,
heavy every 5th at 1.5 times; slash projectile speed 1.8, life 24;
finisher 900 plus Strength times 4.0; cross slash life 40, splash total
450 plus Strength times 2.0 in four 25 percent pulses at ticks 8, 14, 20,
26, radius 3.5 times scale 1.25, 24 debris at the first pulse; charge Ki
drain 15 percent per minute held to 100 ticks, ratio with no floor; dash
8 plus ratio times 16 blocks; dash damage 600 plus ratio times 1400 plus
Strength times 4.0 times ratio, resolved at rush trail tick 6 of 30;
shockwave radius 5 plus ratio times 3.5, damage dash total times 0.4 at
ring contact, life 44, 28 debris, magma scar disk radius 2 at chance 0.6
restoring after 300 ticks; right click cooldown 100 ticks; charge fade
out 10 ticks through the synced FADE factor. BraveChargeEntity needed no
edits: no vanilla particles, sounds, or damage, and it already syncs
CHARGE_RATIO, CASTER_ID, and FADE.

## Verification

Targeted javac with JDK 25 (workspace tools jdk-25.0.5+8) over the 11
Brave files, sourcepath covering both source roots:

- Classpath cp262.txt: 0 errors in Brave files. 4 errors total in the
  compile, all pre-existing baseline kinds in shared files not touched by
  this pass (DbaItems creative tab access, DbaMenus MenuType access).
- Classpath cp263.txt: 0 errors in Brave files. 11 errors total, again
  all in untouched shared or other-weapon files (DbaItems, DbaMenus,
  DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity,
  PatrolRobotEntity, LavaSwordGround).

No new error kinds attributable to Brave on either classpath. No Gradle
build was run or claimed, per the standing rule for this VM. Grep audit
over all 11 files: zero ParticleTypes, zero SoundEvents, zero
damageSources(), zero DustParticleOptions, zero KiRenderHelper primitive
draw calls, zero em dashes.

## Toolkit gaps and shared change requests

No blocking gap. One optional request for the coordinating pass to weigh:
BespokeGeometry has no cone builder and no angular prism or octahedron
builder. The rush trail's Mach cones are approximated by crossed torn
ribbon sheets and the charge core's octahedra by displaced dome pairs.
Both read well as bespoke redesigns, but if an exact cone or octahedron
silhouette is ever wanted back, that is a shared toolkit addition, not
something a weapon agent should hand roll locally.

## Honest worse check

Nothing was reverted; every swap follows the standard's own mapping. The
deliberate changes a reviewer should know about:

- Slash and cross slash silhouette: bladeArc is a deeper crescent moon
  band than the old shallow parabola cuts. Radius and recentering keep
  the footprint comparable (cross span still grows to 4.6 times scale,
  additive copy at 1.16 times), but the cut reads bolder and thicker,
  less razor thin. This is the directed bespoke redesign (the task names
  bladeArc for slashes), flagged rather than hidden.
- Charge core: the angular octahedron is now a rounded displaced dome
  shell. Sizes, speeds, colors, layering, and alphas are the old ones;
  the silhouette is softer. If Carter prefers the spiky core, that needs
  the toolkit request above, not a local helper.
- Charge shards: the old prisms graded gold to cyan along their length
  with a white core strip. The fans grade radially instead (gold fan,
  smaller cyan fan inside it). Placement, count, orbit, and aim are
  unchanged.
- Rush trail: crossed sheets are thinner than the solid cone from some
  angles. Four planes (horizontal, vertical, two diagonals) plus the
  core beams keep the volume read close to the old nested cones.
- Shockwave spikes: fans radiate in the radial plane rather than a
  single up triangle; the lowest spikes clip at the ground plane, which
  reads as ground burst. Crest heights match the old tip heights.
- Sounds: sites that paired a vanilla sweep with SLASH_WHOOSH now pair a
  bespoke swing variant with the same whoosh. Slightly denser than
  deleting the layer, but it preserves the spectacle pass sound design
  with zero vanilla, and no volume or pitch moved.

Damage amounts, timings, radii, counts, and cooldowns are numerically
identical everywhere; the only damage change is the type (valor), which
is the point of the pass.
