# Model Quality Report: attack models raised to the character standard

Date: Nov 5, 2026. Directive (Carter): "Make sure the models are as good as the
character models." Base: dfae211 (the models pass). This pass benchmarked the
five attack models (celestial_sword, sky_blade, azure_dragon, spectral_spear,
time_shard) against the selectable character and form models in this repo,
scored them with rendered previews, and rebuilt every one that fell short
through its generator script in tools/. Asset-only pass: no Java and no
ability data changed.

## 1. The benchmark: what the character models actually are

Measured from the current tree, not from impressions. Three named references:

- Saiyan race body (assets/.../models/entity/better_player_model/saiyan.geo.json):
  the standard player body used by the selectable races and forms. 15 bones,
  16 cubes (bodies are deliberately lean; the detail lives in the texture and
  the race meshes), 256x256 texture, and the race OBJ meshes beside it run
  912 to 7272 vertices (saiyan.obj 1818 faces, frieza.obj 7272). Character
  textures run about 111 texels per block of model height.
- Spirit Sword item model (assets/.../models/item/spirit_sword.json): the
  direct comparator for the attack swords, a repo-authored weapon at the
  character standard. 52 cubes for a roughly 2.2 block weapon (12 to 24 cubes
  per block across the repo's authored item swords, which run 27 to 52 cubes),
  about a third of its cubes rotated, 256x256 texture (about 110 texels per
  block), about 300 unique painted colors.
- Shenron (assets/.../models/entity/shenron.geo.json): the dragon comparator.
  60 cubes, 54 bones, 256x256 texture, and a 20 second serpent clip with 3564
  keyframes over 24 bones (178 keyframes per second).

Animation standard from the character clips: idle runs 81 keyframes over 2.4
seconds across 8 bones; the charge and shout clips run 248 keyframes in 1.0
second across 7 bones. Character animation is dense, eased, and layered with
secondary motion; it is not a handful of pose keys.

Painting standard: character and item textures are painted, not filled:
multi-step shading gradients, edge highlights, panel and seam lines, formed
faces and hair, clothing layers. Glowmasks are used for energy accents on the
forms and Shenron; physical surfaces stay on the world-lit texture.

The bar, in numbers: authored weapons carry roughly 12 to 24 cubes per block
of length with about a third rotated; textures are 256x256 at item scale with
hundreds of painted colors and visible shading structure; clips carry tens to
hundreds of keyframes with secondary motion.

## 2. Audit of the five, before (all previews in renders/model-quality/before/)

Rendered through tools/preview_models.py, a deterministic preview renderer
written for this pass (correct Bedrock pivot semantics; the older
tools/render3d.py visibly explodes chained hierarchies like the dragon's, so
it was not used for judgment). Reference renders of the Saiyan body, the
Spirit Sword item, and Shenron were produced through the same renderer in
renders/model-quality/before/ref_*.png for an apples to apples comparison.

- celestial_sword: FAIL. 17 cubes (4.1 per block), 4 rotated, 128x128 texture
  (31 texels per block), clips animated 1 bone with 3 to 6 keyframes. In the
  render the blade read as a pale slab with a plain cross and a sticker gem.
- sky_blade: FAIL. 17 cubes, 128x128, clips on 1 bone. Its silhouette and rune
  language were the closest of the fails, but the surface was flat and thin.
- spectral_spear: FAIL, the hardest. 10 cubes (1.9 per block), 128x128; the
  head rendered as a chunky orange paddle on a stick.
- time_shard: FAIL. 10 cubes, ZERO rotated, 64x64 texture; read as a few
  tumbling ice blocks, no crystal structure.
- azure_dragon: PARTIAL. 50 cubes (7 per block against Shenron's 8.1), 44
  percent rotated, 256x256 already, swim at 180 keyframes over 20 bones; short
  on bones (23 vs 54), head anatomy, fin membranes, and clip density.

## 3. Upgrades (all through the generator scripts; outputs regenerated)

### celestial_sword (Spirit Impale, six at a time): 17 to 46 cubes
Ridge spine added inside a two-rail blade sandwich with diamond bevel diamonds
under each face, continuous saber taper from guard to a forged five-cube
point, structured collar rings, a three-layer guard with fluted quillon
terminals and a forked center, and a crown-set gem pommel. Texture 128 to 256
with structured blade shading (rail bands, diamond facets, fracture flecks);
the palette was deliberately posterized to the flat family style the
character textures use (683 down to 404 unique colors, on purpose). Clips:
descend 3 to 17 keyframes, hold 6 to 26, release 4 to 14, now across 4 to 5
bones with counter-sway and pommel lag. Silhouette unchanged: same 66-unit
span, point down.

### sky_blade (Eclipse Domain, fourteen at a time): 17 to 44 cubes
Beveled central fuller plate carrying the rune column, diamond cross-section
edge rails, a three-step tip cluster with angled facets, plated quillon lobes
on a new guard_stone bone, and a pommel core with cap. Texture 128 to 256,
363 to 2822 unique colors (fine stone grain and rune detail). Fall 5 to 30
keyframes, shatter 6 to 42, now across all 6 bones: on shatter the blade kicks
to -19 degrees, the tip to +48, and the guard stone pops out and spins, so the
break reads as real separation.

### spectral_spear (Evil Impale): 10 to 34 cubes
The paddle is gone: the head is now a five-segment leaf blade with a raised
midrib, in the weapon family's orange with a dark socket, a 45-degree diamond
collar ring, langet straps, shaft wrap bands, and a butt spike. Texture 128
to 256, 498 to 1300 colors (hammered metal grain, ember mottling). Fly clip 6
to 21 keyframes across 4 bones with shaft flex and flame-tongue flutter. Span
unchanged at exactly 84 units, tip at +Z.

### time_shard (Time Shatter): 10 to 22 cubes
Rebuilt along each shard's own axis: three graduated segments per shard with
alternating bevel collars and chamfered tips, 16 of 22 cubes rotated (was 0).
The five shards keep their exact fan directions and scatter distances (cluster
span 22.7 units against 22.5 before). Texture 64 to 128, 69 to 379 colors
(faceted ice painting with frost speckle). Hang clip 23 to 53 keyframes with
per-shard tremble phase offsets, fall 36 to 54; durations untouched.

### azure_dragon (Dragon Rush): 50 to 84 cubes, 23 to 32 bones
Head rebuilt: angled brow ridge plates hooding painted eye sockets, armored
cheek plates, enlarged nostrils, a dorsal muzzle ridge, and a teeth row
behind the fangs on the existing jaw bone. Whiskers gained a third segment on
new tip bones (only the true tips glow). Every body segment gained a dorsal
ridge plate and a pale belly plate with seam painting, so the side view reads
as plated segments instead of stacked blocks. Each dorsal fin gained a pair of
membrane panels on their own bones with a ribbed gradient membrane painting,
and the tail fan gained a third, horizontal membrane. Texture rebuilt with
filled scallop scale cells in three tints, dark outlines, and segment borders
(139 to 228 colors, still 256x256). Swim and idle clips 180 to 377 keyframes:
sampling densified, the jaw now opens and closes once per loop, and fin
membranes, whisker tips, and the tail membrane carry phase-lagged secondary
motion behind the untouched traveling wave. Length 114.8 to 117.2 units, head
still at +Z, segment pivots and spacing unchanged.

## 4. After scores

Every model now sits at the benchmark on its axes: cube density per block
(celestial 11.2, sky blade 12.5, dragon 11.5 per block against the item bar of
12 to 24 and Shenron's 8.1; the spear at 6.5 per block is a genuinely thin
5.25-block polearm, dense where it matters, at the head), rotated share
roughly a third or better, 256x256 textures on the four large pieces (128 on
the small shard cluster), painted shading structure throughout, glowmasks
still energy-only, and clips with secondary motion in the character style.
One honest texture note: the big pieces are physically larger than held items
(a 4-block sword), so texels per block run about 62 against the held items'
110 even at the same 256 resolution; at the distances these pieces are seen,
the renders show the painting reads. After previews:
renders/model-quality/after/ (three_quarter, side, front for all five).

Per-instance cost for multiples: celestial_sword 46 cubes (6 swords = 276),
sky_blade 44 (14 blades = 616), time_shard 22 per cluster piece set. All sane
for the spawn counts the data uses.

Already at the bar and untouched: none of the five fully met it; the dragon
was closest and still gained anatomy, membranes, and clip density. The
models judged not to need authored models in the models pass (swarm shards,
spin shards, sword rain tips, spikes, afterimages) were not re-opened; this
pass covered the five authored models.

## 5. Gates

- tools/check_models.py: PASS, ends with ALL MODEL REFERENCES RESOLVE (bones
  and cubes per model as upgraded: 46, 44, 84, 34, 22).
- Roster data untouched and re-verified: 54 ability files parse, 44 distinct
  bound (weapon, slot) pairs across 22 weapons, every weapon with left and
  right.
- All 632 resource JSONs under src/main/resources parse.
- Asset-only: git shows changes only in the five models' geo, animation, and
  texture files, their five generator scripts, and the new preview tool. No
  Java and no data changed, so the dual javac sweeps were not required and
  none were run.
- git diff --check clean; em dash scan zero on all changed and new files.

## 6. Honest lines

No Gradle build ran (the sandbox cannot); nothing here is compile-proven
beyond the JSON and reference checks above, and these are asset files, which
carry no compile risk of their own. The previews are deterministic geometry
renders from tools/preview_models.py, NOT in-game screenshots: lighting,
glowmask bloom, and scale against the world are only proven in Carter's PC
test. Carried PC-test checks: azure dragon fin membranes and escort height on
the first Dragon Rush, spear and dragon forward axis (+Z), sky blade fall
speed against the finale strikes, the six-sword ring read at radius 2.6, and
the time shard cluster's scale read in the Temporal Rift.
