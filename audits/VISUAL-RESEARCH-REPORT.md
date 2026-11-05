# DBA Visual Research Report

Prepared Nov 4, 2026, before any rebuild work. Carter stripped every weapon visual and
ability from DBA for a fresh start; the attack designs are saved in
brain mods/DBA-ATTACK-DESIGNS.md. This report covers three research streams: the
cool-bar pictures Carter supplied, decompiled study of eleven weapon and ability mods,
and web best practices for Minecraft mod visuals. Nothing in the mod was modified.
Decompiled sources live in ~/workspace/mod-research/decompiled/, jars in
~/workspace/mod-research/jars/, full researcher notes in ~/workspace/mod-research/notes/
(fabric-batch.md, animation-batch.md, spectacle-batch.md, best-practices.md).

## Headline findings

1. Nobody in the top tier builds effects the way old DBA did. All eleven mods studied
   converge on the same shape: a small number of generic, parameterized effect carriers
   (one wave entity, one cloud entity, one universal particle class, one beam drawer)
   driven by data, instead of one bespoke renderer per ability. Old DBA had 73 ability
   entity classes and a hand-rolled renderer for each. Spell Engine plus Wizards ships
   25 spells in 51 Java classes; DBA had 466 Java files for its whole mod and still
   hand-built every effect.
2. The biggest single gap is animation, not particles. Old DBA swung weapons with the
   vanilla arm swing and one shared three-loop animation file on a single bone. Better
   Combat, Epic Fight, SlashBlade, Spell Engine, Soulslike, and Mowzie's all drive
   attacks from real animation data with phased timing (windup, contact, recovery)
   stored in data, and effects fire on animation keyframe events, not on click input.
3. Trails: the best-looking ones are captured from the weapon's actual motion. Epic
   Fight samples its weapon joint transform three times per tick and meshes a real
   ribbon. Better Combat cheats cheaper: a flipbook slash sprite particle placed by
   data and spawned exactly on the hit tick. Old DBA's ribbon profiles were a middle
   version of this, parameterized but not motion captured.
4. Custom shaders are nearly absent from the top tier. Of eleven mods, only Mahou
   Tsukai ships real custom GLSL. Everyone else fakes glow with additive render types,
   white cores, halo shells, and UV-scrolled textures on vanilla shader pipelines.
   Old DBA's custom shader detour (dissolve and fresnel films that ended up adopted by
   zero effects) is confirmed as a dead end by the entire field.
5. Feedback (hit-stop, screen shake) is rarer than expected and always systematized
   when present. None of the three combat-animation mods uses hit-stop or impact
   shake at all. Iron's Spells has the one proper shake system found: server-synced
   shake data with nearest-shake-wins arbitration and distance falloff. Cataclysm and
   Mowzie's share a shake entity pattern. Old DBA had fragments of this (a finisher-only
   hitstop, basic yaw and pitch shake, a clash camera pressure trick) but never one
   system.
6. The cool-bar pictures and the mod research agree with each other: one huge
   silhouette per ability, geometry as the hero, particles as detail and decay,
   physical debris kept dark and world-lit, and a three-beat structure of telegraph,
   working parts, payoff.

# Section 1: The cool-bar pictures

Carter's nine AI infographic cards are copied to ~/workspace/dba-cool-reference/:
hollows-edge.png, cursed-blade.png, dabura-sword.png, grand-sword.png,
azure-dragon-sword.png, brave-sword.png, saber.png, z-sword.png, katana.png. Each card
shows the weapon's left click and right click at full expression in a night scene,
with a three-step strip underneath showing the effect building. Analysis per picture,
then the shared traits, then honest translation limits.

## Shared traits (the bar, distilled)

- Scale: signature effects run 5x to 15x the player's size. The Katana Heaven Splitter
  wall is about 10x player height. The tempest domes span 20 to 30 blocks. Even dash
  chains draw arcs 3x to 5x player size. Nothing in the bar is player-sized or subtle.
- One silhouette per ability: a vortex funnel, a dome with an eclipse disc, a cross, a
  dragon, a wall of torn slabs, a spiral cyclone. Detail hangs off the silhouette; it
  never competes with it.
- Color language: near-black night scene; effects are one or two saturated hues with
  white-hot cores, colored mids, and edges fading to transparent or dark. The read is
  bright core, colored mid, dark rim, which is the inverse of flat fullbright.
- Ground contact: every big effect touches terrain with a glowing ring, lit fissures,
  erupting rock, or a light pool. Nothing floats unanchored.
- Dark physical debris: real rock chunks fly or orbit in almost every panel, world-lit
  and dark against the bright energy. The contrast is what makes the energy read as
  light instead of paint.
- Bodies sell it: enemies are lifted, dragged, lined up, frozen inside glass cubes,
  petrified gray. The pictures show what abilities do to things in the scene.
- Path of motion: afterimage chains and arc trails show where the attack traveled,
  not just where it landed.
- Three-beat choreography: every card's strip is form, grow, execute. First step is
  small and readable (a ring, a stance, a domain outline); the third is the payoff.

## Picture by picture

### Hollow's Edge (hollows-edge.png)
Left, Hollow Rush: a chain of full-body afterimages along a curved dash path, each
ghost trailing a thin violet crescent; the read is path plus rhythm, five poses on one
arc. Right, Void Rift: a huge swirling funnel with a black core, violet lightning
crawling over the funnel surface, an orbit ring of dark rock fragments at its mouth,
enemies and blocks pulled in, a glowing ring scar on the ground; roughly 12 blocks
wide and 8 tall. Translation: the painterly funnel swirl cannot be literal; the closest
build is a layered broken vortex band mesh with scrolling texture, a black core disc,
and real orbiting fragment entities. Afterimages need player-model ghost rendering,
not particles.

### Cursed Blade (cursed-blade.png)
Left, Curse Chain: chunky chains with individually readable links, white-hot cores,
wrapping targets twice and orbiting as a stacked double ring, several targets chained
at once, chains curving with sag like real chain under tension. Right, Cursed Tempest:
a dark storm dome with a defined rim ring, violet and crimson lightning forking inside
(the two-hue lightning is the signature), enemies lifted, debris floating, ground ring
lit. Translation: chain links need instanced link geometry along a catenary curve; a
flat ribbon will never read as chain. The soft cloud dome becomes a displaced dome
shell with scrolling noise alpha plus interior lightning geometry.

### Dabura Sword (dabura-sword.png)
Left, Abyssal Slash: three overlapping crescent waves, each about 4x player height,
rolling low along the ground, dark violet bodies with blinding white leading edges,
debris kicked up along the path. Right, Eclipse Domain: a translucent violet dome
capped by a solid black eclipse disc ringed in magenta; petrified enemies stand gray
and frozen inside; the finisher strip shows sky blades as falling light columns and
the ground converting to dark obsidian with glowing cracks. Translation: petrification
reads through a stone material swap on the mob itself, not an effect around it. The
eclipse disc is a flat black circle with a rim treatment, cheap, and it carries the
whole read.

### Grand Sword (grand-sword.png)
Left, Grand Cyclone: a towering double-helix spiral vortex about 10 blocks tall,
lifting zombies and full rock chunks, stacked ring shockwaves at the base, blade
shards visible in the flow. Right, Valor Field: a glassy translucent dome, and the
detail that sells it: every frozen projectile is encased in its own visible glass
cube. Arrows outside still fly and burn. Translation: the glass cubes are plain
translucent geometry, cheap and exact. The dome's glass read wants a fresnel-style
rim brightening rather than flat alpha. The cyclone's weight needs real lifted debris
entities; particles alone will not sell it.

### Azure Dragon Sword (azure-dragon-sword.png)
Left, Dragon Rush: the player flies inside a giant translucent energy dragon, head
alone about 5x the player, body a streaming ribbon of fins and light; the slam crater
throws real rocks and leaves a glowing ring scar with radiating cracks. Right, Call
of the Tempest: a full tornado from sky to ground ringed by dozens of forked lightning
bolts, mobs and blocks airborne in the funnel. Translation: the dragon is the hardest
image in the set. A literal fluid dragon is impossible; the closest builds are a
modeled segmented aura that follows the flight path with an additive flowing texture,
or a ribbon body with a modeled head. The tornado is the vortex problem at larger
scale: layered scrolling bands, lifted debris entities, branched lightning geometry.

### Brave Sword (brave-sword.png)
Left, Assault: gold and cyan afterimage clones trail the combo, slash arcs are thick
ribbons with white cores, and the finisher is a giant X cross of pure light that fills
the screen. The cross is the silhouette; everything else is lead-in. Right, Heroic
Dash: a long ground-hugging trail of gold and cyan streaks over concentric glowing
ground rings, ending in a radial burst of dense light rays with enemies thrown
outward. Translation: the cross is two crossed ribbon planes with a hot core, huge
and brief, very achievable. The etched concentric ground rings argue for a ground
decal system in the rebuild.

### Saber (saber.png)
Left, Blitz Flurry: cold blue-white palette, long speed streaks, the player at three
points along a blink chain, and a finisher shown as a single straight beam line slash
through targets that have been snapped into a row. Right, Flash Step: tall vertical
torn-rift shards, black slabs with violet glowing cracked edges, standing in the world
like broken mirrors, the player arcing between two of them. Translation: the rift
shards are flat displaced planes with black interiors and emissive edge texture; the
darkness is the point and brightness belongs only at the rim. Speed reads from streaks
stretched along motion, never from dot particles.

### Z Sword (z-sword.png)
Left, Z Shockwave: golden vortex rings coil flat around the player like Saturn rings,
then a massive horizontal wave band with a white core rolls across the terrain,
throwing debris and enemies and scorching a glowing path. Right, Katchin Weight: the
gravity well hangs in the sky as a giant violet vortex disc with light beams pulling
mobs and blocks upward into it, while below the ground tears open into a grid of
canyon fissures with violet light in the cracks. Translation: the wave band is a
scrolling-textured wall mesh. The sky disc is a vortex texture on a ring mesh plus
additive light shafts. The fissures want ground crack decals with emissive centers,
plus real displacement where destruction applies.

### Katana (katana.png)
Left, Flashdraw: a low iaido stance, then one clean curved violet-white crescent
wrapping through a line of five zombies. A single elegant arc, thin and bright, with
almost no particle dressing anywhere. Right, Heaven Splitter: a wall of torn reality
about 10x player height, jagged black crystalline slabs split by a blinding white
violet core beam, violet cracks webbing the slabs, and the wall lingers standing after
the cut. Translation: the wall is displaced slab geometry with a dark stony material,
an emissive crack texture, and one additive core plane; achievable with geometry,
impossible with particles. This card is the restraint lesson: one perfect shape beats
a dense effect.

## What the pictures demand that Minecraft cannot do literally

- Volumetric softness (smoke domes, fluid dragon, rift funnels): Minecraft geometry is
  hard-edged. Translation: layered shells with scrolling noise alpha, gradient fades,
  and rim brightening get most of the read; the silhouette does the rest.
- Photographic bloom on the white-hot cores: without a post chain, fake it with
  additive halo shells and gradient textures (which is what every studied mod does),
  or add a real bloom pass (which none of them ship).
- Cloth-like flow (chain drape, dragon body): translation is segmented geometry
  following math curves, updated per frame: catenaries for chains, path ribbons for
  the dragon.
- Filmic particle density with depth of field: the cards are actually not that
  particle-dense. Their density lives in geometry and debris. Translation: fewer,
  bigger shaped pieces plus a modest spark layer.

# Section 2: Decompiled mod study

All eleven target mods were obtained and decompiled. Method note: the Modrinth API
host refused direct requests from this VM, so jars came from equally official routes:
Modrinth CDN file URLs, official GitHub releases, the official CurseForge download
endpoint, and CurseMaven, with versions verified against each jar's own metadata.
Decompilers: Vineflower 1.12.0 (Fabric and animation batches) and CFR 0.152
(spectacle batch, after Vineflower runs were killed by VM memory limits). Full notes
per batch are in ~/workspace/mod-research/notes/. Misses: none of the eleven. Two
version caveats: Epic Fight was studied at its current NeoForge 1.21.1 build, and
SlashBlade Resharped was studied mainly at Forge 1.20.1 (a NeoForge 1.21.1 jar was
also obtained). Nothing was copied into DBA; this is technique study only.

## Fabric batch

### Simply Swords (Fabric, MC 1.21.1, mod 1.62.0)
Structure: one item class per unique weapon (about 35), which is the closest structure
to old DBA, but the visuals cost almost nothing. The whole mod has exactly one custom
particle class; everything else is vanilla particles spawned server side from shared
status effect bases and static helpers. Its signature trick: OrbitingEffect advances
an angle per tick with the effect amplifier doubling as particle count and stack
height, so orbiting weapon auras are a status effect, not a renderer. Animation is
delegated entirely: a weapon_attributes JSON per weapon declares the Better Combat
swing animation, hitbox, upswing hit timing, and swing sound, with about 35
PlayerAnimator JSON files that do nothing without Better Combat installed.
Techniques old DBA lacked: animation and hit timing declared in data; status effects
as visual carriers; near-zero custom particles as a deliberate budget. Cost: low code,
but a hard dependency does the heavy lifting.

### Marium's Soulslike Weaponry (Fabric, MC 1.21.1, mod 1.4.1)
Structure: weapons are thin. Moonveil's item class is 42 lines and mostly calls one
method handing over three ability records; about 100 composable ability classes are
grouped by trigger interface, with tuning stored as record components. Effect entities
are generic and parameterized: one wave entity syncs a texture id, rotation, and Y
offset as data trackers, forces full block light, renders on a translucent emissive
layer, and scales its model from its bounding box, so one class serves many weapons.
A flame pillar entity does the same from a synced radius. GeckoLib carries bosses,
projectiles, and 14 item renderers, with a library glowmask layer producing weapon
glow in four renderers. Of 11 particle types, 8 are a custom sprite bound to the
vanilla flame factory; only 3 particle classes are bespoke. It also spends feedback
budget on HUD overlays for posture, bleed, and frost buildup.
Techniques old DBA lacked: composable ability records instead of per-weapon classes;
one parameterized wave and pillar entity family; GeckoLib item rendering with library
glow layers; vanilla particle factory reuse. Cost: moderate framework design, very
cheap per weapon after.

### Spell Engine and Wizards, RPG Series (Fabric, MC 26.3, Spell Engine 1.10.9, Wizards 3.1.4)
These target the same Minecraft generation DBA does (26.3), and they are the
architectural extreme of the whole study. Wizards ships 25 spells in 51 Java classes
because a spell is one JSON document: cast, release, target, delivery, impacts, cost,
per-stage animations, start, loop, release and impact sounds, and ParticleGroups
(batches defined by shape, anchor, alignment, motion, color, and fractional emission
rate). One projectile entity syncs five values (spell id, behaviour, target id, item
model id, scale); its renderer assembles composites of flat models with orientation
modes (billboard, towards motion) and spin, all from data, with trails emitted client
side. One cloud entity covers every area spell; Fire Wall is five staggered cloud
placements in a single file. One universal particle class of 361 lines implements
every motion preset, facing mode (camera, upright, ground flat, velocity aligned),
attachment, ground hugging, and opacity and scale easing; about 70 particle types are
just texture entries on it, including some 30 generated area ring sizes.
The network shape matters as much as the visuals: the server sends one compact recipe
packet per effect to tracking players only, and each client reconstructs the particles
locally; the JSON keeps a client_data split so presentation never touches the server
copy. Beams have no entity at all: a renderer raycasts per frame from synced casting
state and draws three nested scrolling shells at 1x, 1.5x at 75 percent alpha, and 2x
at 33 percent alpha. Render pipelines are assembled from vanilla snippets with a small
set of light emission modes (none, glow, radiate, glow translucent) and shader-pack
aware fallbacks. No custom GLSL anywhere. Player animation is a three-layer
PlayerAnimator stack with priorities (release 950, casting 900, misc 200), a speed
modifier stretching clips to match cast duration, mirroring, and a look-pitch
adjustment, all synced through casting state.
Techniques old DBA lacked: fully data-driven abilities with a client and server data
split; a universal particle class plus recipe packets; client-predicted beams and
model effects with no server entity; layered, time-stretched player animation.
Cost: high once (this is a framework, not a mod feature), near zero per spell after.

## Animation batch

### Better Combat (Fabric, MC 26.2, mod 3.2.2; Player Animation Library 1.2.5 by ZigyTheBird)
This is DBA's exact floor version, 26.2. Animations are JSON keyframe files in an
emote-derived format; weapon behavior is data-driven attribute JSON listing attacks
with hitbox shape, angle, and upswing fraction. Its core feel trick is two-gear
playback: the swing plays slowly through the upswing gear, then snaps to a fast
downswing gear, and the hit fires exactly at the gear change, so timing, animation,
and damage share one number. Trails are not ribbons: a layered flipbook slash sprite
particle with a 6 tick life, double sided, with a per-color glow flag, placed by data
and scheduled to spawn on the hit tick. There is no hit-stop and no screen shake in
the mod at all. One attack animation packet per swing re-derives the animation and
its particles on every observing client.
Techniques old DBA lacked: phased swing timing in data; two-speed animation playback
as the feel carrier; hit-tick-scheduled slash sprites instead of continuous trails.
Cost: low to moderate; the library carries the animation system.

### Epic Fight (NeoForge, MC 1.21.1, mod 21.17.3.1)
A custom engine: its own armature with dedicated weapon joints (toolR, toolL),
animations stored as baked 4x4 matrix keyframes in JSON, and attacks defined as phase
records in seconds (anticipation, pre-delay, contact, recovery) with joint-attached
colliders and per-phase stun, impact, sound, and particle properties. Its trails are
the real thing: a trail particle samples the weapon joint's world transform at
partial ticks 0, 0.5, and 1.0, builds trail edges, Bezier-interpolates between them,
and meshes a fading textured strip, so the ribbon is the blade's actual swept volume.
It also has authored root motion read from the root joint (lunges are animation data,
not velocity code), a third-person lock-on camera API, and mesh-snapshot afterimage
particles for dashes (the dash ghosts in Carter's pictures are this technique).
Techniques old DBA lacked: joint-captured ribbon trails; phase-record attack data
with per-phase properties; root motion; afterimage mesh snapshots. Cost: very high,
an engine, not a feature; DBA should adopt the techniques, not the engine.

### SlashBlade Resharped (Forge, MC 1.20.1, mod 1.9.65)
Motions are VMD files (MikuMikuDance format) at 30 fps driving frame-ranged combo
state graphs with input buffering, timeout branches, and recovery states. The same
motion data animates the blade's OBJ model in hand, with optional player-body playback
through KosmX's animator. Surprise finding: this version has no ribbon trail class at
all (only a legacy unreferenced trail model). Its slash is a synced effect entity: an
OBJ crescent with a quartic alpha curve, a 135 degree sweep, UV scrolling, and up to
four render passes gated by a Concentration Rank, where the dark core pass only
appears at the highest rank. Summoned swords, Drive, and Judgement Cut are likewise
entities with OBJ renderers and almost no particles. The rank-layered slash is a
directly relevant idea for DBA: the same slash effect gains layers as the player's
combo rank rises, so spectacle scales with play, not with settings.
Techniques old DBA lacked: one motion source animating both body and weapon; combo
state graphs in data-like form; a slash effect whose render passes unlock with rank.
Cost: high content pipeline (authoring motions), moderate code.

## Spectacle batch

### Iron's Spells 'n Spellbooks (NeoForge, MC 1.21.1, mod 3.14.8)
No custom GLSL at all. Beams are segmented wiggle chains with UV scrolling drawn by a
shared rendering helper; its Sunbeam is a 250 block beam with eased scaling. A Black
Hole spell is a seeded-random swirl. One invisible generic area entity spawns its own
client particles scaled by area and clamped, so area spells need no per-spell effect
code. It holds the study's one proper camera shake system: shake data is synced from
the server and managed client side with nearest-shake-wins arbitration, distance
squared falloff, and a 20 tick fade. Glow is the vanilla energy swirl shader plus
additive render types plus GPU UV-offset texturing.
Techniques old DBA lacked: shared beam and area-effect infrastructure; a real shake
system with arbitration and falloff; UV-offset texture animation as the motion source.
Cost: moderate, concentrated in helpers every spell reuses.

### L_Ender's Cataclysm (NeoForge, MC 1.21.1, mod 3.24)
Also zero custom GLSL, in a 70 MB jar. Its spectacle vocabulary is a handful of tiny
invisible synced effect entities reused everywhere: a screen shake entity with 127
spawn sites, a sky color entity that lerps fog color during boss events, plus wave,
void vortex, and lightning storm entities. Lightning comes from one reusable branching
fractal bolt library. Bosses are animated by code keyframes, not GeckoLib. Its Ignis
swing uses non-billboarded quads with Bezier-sampled trails (authored swing planes,
not camera-facing billboards, which is why its slashes keep their shape from every
angle). Render types recombine vanilla shaders (beacon beam, eyes, lightning) with
tweaks like a color-write-only beam that occludes without glowing.
Techniques old DBA lacked: atmosphere as an effect (fog color as a boss weapon);
tiny reusable effect entities instead of per-ability classes; a fractal lightning
library; fixed-plane swing quads. Cost: low per effect, the discipline is the cost.
Provenance note: Mowzie's shake entity and solar beam renderer are near-identical to
Cataclysm's, a shared lineage, so those count as one data point with two tunings.

### Mowzie's Mobs (NeoForge, MC 1.21.1, mod 1.8.1)
The most transferable framework idea in the whole study: abilities are sectioned
objects (startup, active, recovery) and each ability type is constructed with its
GeckoLib animation, so gameplay phase and animation are literally one synced object;
older bosses use animation-tick timelines to the same end. Its particle system is
component based with keyframed property tracks and ribbon sub-particles. Ground
effects (fissures, pillars, spikes) are chains of animated piece entities rather than
one mesh. A Solar Beam is a billboarded cross-plane entity; a Supernova attack animates
its model through 16 textures per tick. It also carries a full GeckoLib player rig
with a first-person variant, relevant to DBA's display standard for weapon use in
first and third person.
Techniques old DBA lacked: ability phase and animation as one object; component
particles with keyframed tracks; ground effects as piece-entity chains. Cost: moderate
framework, cheap per ability after.

### Mahou Tsukai (NeoForge, MC 1.21.1, mod 1.36.25)
The only mod of the eleven with real custom shaders: fog, clip, fullbright, and
passthrough programs, including a depth-buffer-copy soft particle fog shader, and
composable render type wrappers (glow, fog, clip, tint, no-depth) that are cached.
Its particles are not rendered as particles at all: they are data objects drawn as
procedural sphere, ring, and spiral meshes by one central level-render dispatch, with
per-type distance culling at 128, 64, and 60 blocks. Spell circles are composited at
runtime into dynamic textures with six layered sine oscillators; its signature beam
is a six-stage construct 512 blocks tall; another attack runs ten counter-rotating
rune spirals. Notably, it is the only spectacle mod with no camera shake at all.
Techniques old DBA lacked: procedural mesh particles with distance culling; runtime
composited effect textures; composable render type wrappers. Cost: the highest of the
batch; this is a rendering research project, and only its culling and composition
ideas are worth importing at DBA's scale.

# Section 3: Best practices from the web

Full research with about 100 citations is in ~/workspace/mod-research/notes/
best-practices.md. The load-bearing findings:

## Geometry is the hero, particles are detail
Vanilla-style particles are client-only billboarded sprites sharing one global cap
(about 4000 alive; new spawns evict the oldest) with a short render radius, so a
30 block domain filled with particles literally evicts itself. Rings, waves,
crescents, rift walls, and shard silhouettes should be vertex geometry drawn through
render layers and world render events; particles are for sparks, embers, and decay.
Sources: Fabric documentation on creating particles
(https://docs.fabricmc.net/develop/rendering/particles/creating-particles), Minecraft
Wiki particle documentation (https://minecraft.wiki/w/Particles_(Bedrock_Edition)).

## Lifecycle and layering beat particle count
Strong spell VFX is structured as anticipation, release, travel or active phase,
impact or resolve, and decay, with a big, medium, small shape hierarchy and a hero
shape that still reads as a thumbnail. Value contrast comes before color. Layers are
offset in time; if every layer starts and stops together the effect reads as one blob.
Stylized in practice means designed meshes, masks, and flipbooks, not piles of soft
round particles. Sources: a Minecraft spell VFX engineering skill writeup
(https://github.com/gustavaopere/minecraft-mod-factory/blob/HEAD/art/skills/minecraft-spell-vfx-engineering/SKILL.md)
and a VFX fundamentals reference
(https://github.com/eminfsert/unityeffectdesigner/blob/HEAD/plugins/unity-vfx-designer/skills/vfx-fundamentals/SKILL.md).

## Rendering choices decide the look
Additive blending is the hot energy core, but it vanishes against bright daylight and
needs a dark alpha companion shape; translucent is for smoke and waves; solid or
cutout world-lit rendering is for physical matter. Additive passes should not write
depth. Translucent objects must sort back to front, and intersecting translucents
cannot be sorted correctly at all, a limitation Mojang's own 26.3 snapshot notes on
order-independent transparency acknowledge
(https://www.minecraft.net/en-us/article/minecraft-26-3-snapshot-2). Core shader
background: https://minecraft.wiki/w/Core_shader. Most mods ship no custom core shader
and no bloom; they fake glow with additive passes, white cores, and halo shells. Real
post-processing on Fabric runs through libraries such as Satin and its managed shader
effects, with lazy initialization and resolution reload handling
(https://github.com/Ladysnake/Satin).

## Trails need paired points
A ribbon trail is built from paired samples (hilt and tip, or origin and extent)
captured each tick, kept in a short history of about ten samples, subdivided with a
Catmull-Rom curve, and meshed as one triangle strip. Swing-plane-fixed ribbons suit
physical slashes; camera-facing ribbons are for energy only, because camera-facing
blade trails rotate wrongly and wide billboards artifact. Sources: Stride engine
ribbon documentation
(https://doc.stride3d.net/4.1/en/manual/particles/ribbons-and-trails.html) and a
classic ribbon implementation thread
(http://forums.ogre3d.org/viewtopic.php?f=2&t=59364&start=0). Epic Fight's decompiled
trail (Section 2) is the Minecraft-native proof of the same technique.

## Animation events are the clock
GeckoLib triggered animations handle one-shots; its keyframe trigger system can fire
sound, particle, and custom events on exact frames, which is how release, trail,
impact, and sound should be scheduled: on the contact frame, not on the click
(https://github.com/bernie-g/geckolib/wiki/Keyframe-Triggers-(Geckolib4)). Easing is
not decoration: the library ships 30 plus easing functions and linear motion is the
one that reads as cheap. Player-body animation belongs to the player animator layer
stack (JSON-driven, layered by priority); GeckoLib is for entities and constructs.
Upstream KosmX PlayerAnimator is marked no longer updated
(https://github.com/KosmX/minecraftPlayerAnimator), which is why Better Combat now
ships its own maintained fork (the ZigyTheBird library in Section 2); the rebuild
should build on the maintained line.

## Multiplayer: the server decides, clients build
Particles do not exist on the server at all. The server either calls sendParticles on
the server level (simple, chatty) or, the better fit at DBA's scale, sends one compact
packet per effect (origin, direction, size, seed, type) to tracking players and lets
each client expand it deterministically; this is exactly Spell Engine's recipe packet
pattern from Section 2. Two rules: never double-spawn from client prediction plus
server authority, and never let client-only classes load on a dedicated server.
Entity-based effects should interpolate previous to current state the way vanilla
display entities do. Sources: MinecraftForge documentation on particles
(https://github.com/MinecraftForge/Documentation/blob/1.20.1/docs/gameeffects/particles.md)
and the display entity data format (https://minecraft.wiki/w/Display/ED/Block_Display).

## Game feel is local and layered
Hit-stop of about 40 to 80 ms (2 to 5 frames at 20 ticks per second) applied locally,
screen shake scaled by trauma squared with smooth noise and a reduce-shake option,
and sound layered in three bands (low thump, mid whoosh, high crackle) with 5 to 15
percent pitch variation, all timed to the same impact frame. Sources: a game feel
principles reference
(https://github.com/jangles-byte/atelier/blob/HEAD/skills/motion/references/principles.md)
and an indie game feel guide (https://gamedo.live/news/game-feel-juice-guide-indie-devs/).
Section 2 adds the calibration: the top combat mods mostly skip hit-stop and shake,
so these are seasoning at exact frames, not a foundation.

## Budgets that follow from all of the above
Fill perimeters, not volumes, for domains (a ring of effects at the rim reads bigger
than a filled disc at a tenth of the cost). Shorten lifetimes before raising counts.
Scale emission by distance to the viewer and cull whole effect layers past set ranges,
the way Mahou Tsukai culls its mesh particles per type. Every effect gets a hard cap
decided when it is designed, not after it lags.

# Section 4: The payoff, how the rebuild should be architected

First, an honest correction to the framing the research started from. Old DBA was not
primitive across the board. The brain record and the audits show it already had
profile-driven two-tone ribbon swing trails (WeaponTrailProfiles), a finisher-only
hitstop, basic yaw and pitch camera shake plus a clash camera-pressure trick, three
post shaders with form signature grades and a shockwave distortion chain, and a
procedurally synthesized sound bank of 76 events. What it never had was a system:
those were fragments bolted per weapon or per pass, while the ability layer underneath
stayed 73 hand-rolled entity and renderer pairs. Every pass (spectacle, bespoke,
no-shapes, complex, entity, attack upgrade) reworked the same bespoke layer and the
result still dissatisfied. The research says the layer itself was the problem.

## The new architecture, technique by technique

1. Abilities as data, carriers as code (from Spell Engine, Soulslike, Mowzie's).
   Define each attack once, as data: phases with durations (anticipation, active,
   recovery), its effect groups per phase, its sounds per beat, its animation id, its
   cost and cooldown. Ship a small set of generic carriers that interpret the data:
   one wave entity, one cloud or domain entity, one projectile family, one beam
   drawer, one ground-effect piece chain. A new weapon attack becomes a data entry
   plus at most one new carrier behavior, never a new entity plus renderer pair.
   Mowzie's adds the binding rule: the phase object and the animation ship together,
   so gameplay timing and visual timing cannot drift apart.

2. A real player animation layer (from Better Combat and Spell Engine).
   Adopt the maintained player animator line, with a priority layer stack (release
   over casting over misc), clips stretched to cast or charge duration, and two-gear
   swing playback (slow windup, fast strike, hit at the gear change). Effects fire
   from animation keyframe events. This is the single largest visual upgrade
   available: Carter's pictures all show posed bodies mid-technique, and old DBA's
   shared one-bone loop could never produce that. It also fixes the known defect in
   the record that remote-player weapon animation was broken.

3. One universal particle class, texture entries for variety (from Spell Engine).
   Replace per-effect particle classes with one class implementing motion presets,
   facing modes (camera, upright, ground flat, velocity aligned), ground hugging,
   attachment, and size and opacity easing, with per-type JSON or registry entries
   supplying sprite, color ramp, and preset. Generated ring sprites at many radii
   (Spell Engine ships about 30) cover ground rings and domain rims as data.

4. Geometry heroes, built from a small primitive kit (from Epic Fight, Cataclysm,
   Iron's, and the cool-bar pictures). Keep DBA's instinct for authored geometry, but
   build it as reusable primitives with scrolling textures and gradient fades:
   crescent and wave bands, vortex band stacks, dome shells with rim treatment,
   torn-slab walls, chain link instancing on catenary curves, ground crack decals with
   emissive centers, and fixed-plane swing quads. One kit, parameterized, instead of
   per-ability geometry code. Physical pieces (debris, fragments, glass cubes around
   frozen projectiles, petrification material swaps) stay world-lit solids, which
   matches both Carter's standing no-glow rule for physical matter and the pictures.

5. Trails captured from the weapon (from Epic Fight; cheap mode from Better Combat).
   Sample the held weapon's transform during swings and mesh a fading ribbon strip;
   that replaces profile-faked trails as the primary swing visual. Where a motion
   capture trail is overkill (fast small slashes), use Better Combat's flipbook slash
   sprite spawned on the hit tick. SlashBlade contributes the progression idea: slash
   render passes unlock with combo rank, so long fights literally look better than
   short ones.

6. Beams and domains as client reconstructions (from Spell Engine and Iron's).
   Beams get no server entity: synced caster state plus a client raycast and three
   nested scrolling shells. Domains and areas get one generic area entity that owns
   its rim, its interior weather of effects, and its own area-scaled, capped
   particles. Domains fill their perimeter, not their volume.

7. One feedback system (from Iron's and Cataclysm, calibrated by the animation batch).
   A single shake service: server-synced shake events, nearest wins, distance falloff,
   short fades, plus atmosphere hooks in the same service (fog color shifts for
   eclipse-class events, from Cataclysm's sky color entity). Hit-stop stays narrow:
   local, 2 to 5 frames, on finishers and heavy impacts only, which is where old DBA
   already had it; the research warns against spreading it everywhere. Sounds fire
   from the same phase data as the visuals, layered in three bands with pitch
   variation; DBA's synthesized sound bank carries over as an asset.

8. One effect packet protocol (from Spell Engine and the multiplayer best practices).
   The server sends a compact effect recipe (type, origin, direction, size, seed,
   phase timings) to tracking players; clients build the full visual locally and
   deterministically from the seed. No server-side particle spawning for weapon
   effects, no per-particle network traffic, identical views for caster and observers,
   and nothing to desync when an effect is purely cosmetic. Synced entity state stays
   reserved for things with gameplay presence (domains that drag, fields that freeze
   projectiles, fragments that land).

9. Distance and budget discipline (from Mahou Tsukai and the budget research).
   Per-effect caps in the data definition; per-type cull distances for effect layers;
   emission scaled by viewer distance; silhouette layers (the hero geometry) are the
   last thing culled, sparkle layers the first. Mahou's shaders themselves are not
   worth importing; its culling table is.

## What to abandon from the old build

- The per-ability bespoke renderer class. This is the core abandonment; every studied
  mod points the other way.
- Particles as the body of an effect (the no-shapes pass's material-particle oceans,
  and any return to them). Particles are detail and decay only.
- Custom GLSL film shaders as an effect strategy (the dissolve and fresnel pass that
  zero effects adopted). The field ships additive pipelines on vanilla snippets; DBA
  should too. Post-processing stays limited to the form grades and shockwave
  distortion DBA already had, treated as seasoning.
- Pre-authored tendril and curve collections as the main shape language (the complex
  pass's tendrils, fans, and plates per ability). The primitive kit plus scrolling
  textures replaces them with fewer, bigger, parameterized shapes, which is also
  Carter's stated realism bar.
- Hand-tuned per-weapon copies of shared behavior (each weapon's own drain, upkeep,
  and upkeep drain keyed to weapon names; the strip already exposed how fragile that
  was when two status effects lost their Ki drain because it was keyed to weapon
  names). Shared behavior lives in the carriers and the data schema, once.

## What to keep from the old build

- The design record itself: the 22 attack identities in DBA-ATTACK-DESIGNS.md and the
  nine cool-bar pictures as the approval gate. Every studied mod's discipline starts
  from a fixed identity per ability; DBA's picture-gate workflow already does this.
- Physical pieces as real entities where they detach, travel, land, or must match for
  every player (the FragmentEntity and DebrisEntity judgment from the entity pass),
  now spawned by carriers from data instead of by per-weapon code.
- Permanent destruction with protection rules and caps (PermanentDestruction), which
  no studied mod matches and which the pictures demand (fissures, craters, torn
  ground). It stays a server system the carriers call.
- The world-lit rule for physical matter, and energy-only emissive: confirmed by the
  pictures (dark debris against bright energy) and by Soulslike and Cataclysm's light
  discipline.
- The synthesized sound bank, now fired from phase data instead of scattered calls.
- GeckoLib for weapon and construct models, with glowmask layers (Soulslike proves
  the pattern at DBA's own stack).
- Dual-version discipline (26.2 floor, 26.3 audited) and the javac sweep verification
  method; Spell Engine and Wizards shipping on 26.3 confirms the target is current.

## Mapping the bar to the build (spot checks)

- Hollow's Edge afterimages: mesh-snapshot ghosts (Epic Fight technique) spawned from
  the dash phase data; Void Rift: vortex band primitive plus fragment ring carrier
  plus ground ring decal.
- Dabura eclipse: dome primitive plus a black disc with rim treatment; petrification
  as a material swap state on the mob, scheduled in the domain's phase data.
- Grand Sword Valor Field: dome primitive with rim brightening; frozen projectiles
  get a glass cube from the primitive kit, attached by the field carrier (the picture
  detail that sells the whole ability is also the cheapest to build).
- Katana Heaven Splitter: torn-slab wall primitive with emissive crack texture and an
  additive core plane, lingering as a short-lived carrier entity. Flashdraw: one
  crescent from the kit, fired on the animation contact frame, nothing else. The
  restraint is the spec.
- Azure dragon: the one place the research says to spend real money: a segmented
  GeckoLib aura model following the rush path (Soulslike and Mowzie's prove GeckoLib
  constructs at this scale), not a particle trick.
- Z Sword Katchin Weight: sky disc as a ring mesh with a vortex texture, light shafts
  as additive planes, fissures as the crack decal plus PermanentDestruction where the
  design calls for real tearing.

## Suggested build order when Carter says go

1. The data schema and carriers (wave, cloud, projectile, beam drawer, ground piece
   chain) with two proof attacks end to end.
2. The animation layer and keyframe event wiring, with the swing trail capture.
3. The universal particle class and the effect packet protocol.
4. The primitive geometry kit and ground decals.
5. The feedback service (shake, atmosphere hooks, narrow hit-stop).
6. All 22 attacks as data on top, picture-gated per weapon as before.

## Honest limits of this research

Decompiled code shows structure and technique, not frame timing feel; several mods
were studied one Minecraft generation behind DBA's target (1.21.1 and 1.20.1 builds),
so specific API calls will differ even where techniques transfer directly. Mahou
Tsukai and Cataclysm code was read through CFR output rather than Vineflower for
memory reasons. Feel claims (hit-stop durations, shake tuning) come from the web
research and the one shake system found in code, not from playing these mods, and
nothing here was tested in game. The next verification step is a two-attack prototype
built on the new architecture and judged on Carter's PC against the pictures.
