# Lava Block Art Report

Scope: the look of MagmaCrustBlock and MiniVolcanoBlock only. Files changed
are their blockstates, block models, and block textures, plus the new
generator at tools/gen_lava_block_textures.py. No Java was touched:
LavaField, damage, timings, stage schedule, and mechanics are exactly as
they were. No code hook turned out to be necessary, so there are no
change requests.

## What was wrong

Textures (all 32 by 32 before):

- The molten top was generic orange cloud noise. No crack network, no
  crust rafts, no convection structure. It read as a lava emoji, not a
  surface.
- The cooling and dark tops drew their cracks as dotted one-pixel chains
  on muddy brown noise. The dots are the worst part: at block scale they
  read as perforated stitching, not fractured rock.
- The sides were a soft vertical gradient with a thin hot rim at the top.
  The model sampled the bottom band of the side texture, so most of what
  a player saw was featureless dark smear.
- The volcano had one rock texture for everything: flat brown noise with
  no strata, no ash, no facet shading, no difference between the sunny
  side, the shadowed side, and the crater.

Models:

- The crust was one flat rectangular slab, 16 by 6 by 16, identical in
  all three stages. Stage changes only swapped the texture. A flat box
  top with a repeating texture is why the fields of crust read as a
  stamped pattern.
- The volcano was four centered square tiers stacked like a wedding
  cake, all one texture, with a flat square of molten texture on top
  instead of a crater. No rim, no depression, no channel, no asymmetry.

## Textures rebuilt

New generator: tools/gen_lava_block_textures.py, in the style of
tools/gen_no_shapes_textures.py (seeded per texture name, numpy and PIL,
periodic noise so edges tile). All block textures are now 64 by 64.

Method: periodic fractal noise, two scales of Voronoi cell networks
whose edge-distance fields drive the cracks, heat ramps for lava and
glow, basalt ramps for rock, ash and oxide patches, and pore speckle.
Cracks are built as a dark groove with width plus a hot core and a red
halo whose strength depends on stage, never as a dotted line. Two
measured fixes worth recording: blurring the edge-distance field made
grooves merge into black blobs at crack junctions (one patch measured
54 down to 11 luminance), so the raw distance fields are used and the
finish grain is kept off the glow; and the cooled groove is narrow and
shallow because clusters of small cells put most pixels near a boundary.

Per stage:

- Stage 0 molten top: convection heat field with bright flow channels
  between cells, dark basalt rafts floating in it with heat-lit rims,
  and small bright bubbles in the open lava.
- Stage 0 plate: the raised rafts as their own texture: dark basalt
  whose fractures are mostly cold, with only short stretches still
  showing heat.
- Stage 1 cooling top: dark basalt plates with a continuous glowing
  crack network (dark groove, orange core, red halo, brown rim light on
  the plate edges).
- Stage 1 plate: the same family, cooler: crack glow cut to about a
  third so raised plates read as further cooled than the surface
  between them.
- Stage 2 dark top and plate: near-black basalt with visible grain,
  bedding, and ash; most seams are dark grooves and only separated
  stretches still hold embers. The plate field is compressed upward so
  no cell renders as a flat black void next to its neighbours.
- Sides, all stages: full block-height cross-sections with bedding,
  vertical streaks, and a heat profile in the band the models sample:
  stage 0 spills bright drips down from the surface line, stage 1 keeps
  a dull red rim and sparse cracks, stage 2 is charcoal with faint
  gated embers. Each of the four directions is a different seed, and
  the base model uses a different texture per direction.

Volcano textures: a basalt rock sheet with chunk facets, groove cracks,
bedding, ash patches, and oxide staining, in four variants (main, east,
west, dark for shadowed and inner faces), an ash-heavy sheet for
terraces and the rim top, a crater sheet (live lava pool with a broken
dark crust edge, mapped on the depressed pool surface), and a channel
sheet: a continuous lava flow with a pale core, orange mid, red halo,
and dark solidified banks, drawn in world coordinates so the model
strips sample one continuous run.

## Models rebuilt

Magma crust (stage0, stage1, stage2 models):

- A 2-high base slab carries the stage top texture; on top of it sit
  separate raised plates at different heights (stage 0: five plates
  plus one rotated, tops 4.4 to 5.8; stage 1: eight plates, tops 5.0 to
  6.0; stage 2: nine plates, tops 5.5 to 6.0, near full coverage). The
  visual maximum is still 6, matching the shape the block class
  declares. Hot stages are mostly open surface with floating plates;
  the cooled stage is mostly plate with narrow seams. That progression
  is the stage read the old single slab never had.
- Every face UV is mapped in world block coordinates: a plate samples
  the crop of the plate sheet where it actually sits, so plates and
  gaps form one continuous surface instead of each element repeating
  the whole texture.
- The blockstate now lists four rotations (0, 90, 180, 270) per stage,
  chosen per position by the game, so a field of crust does not stamp
  the same arrangement in every cell.

Mini volcano (29 elements, was 4):

- Stepped cone built from offset, asymmetric tiers instead of centered
  squares, with four buttress rocks breaking the profile on different
  sides and ash terraces on the ledges.
- A real crater: four uneven rim walls (top at 15) around stepped inner
  walls (top at 14) around a depressed lava pool (top at 13.2), the
  pool using the crater texture and the inner walls the dark rock.
- A lava channel down the south face: seven thin strips, each sitting
  just proud of the tier behind it from the crater rim to the base,
  sampling the channel sheet at their world position so the flow reads
  as one continuous run. The last strip protrudes past the base front
  rather than sitting coplanar with it, to avoid z-fighting.

## Verification

- All blockstate and model JSON parses.
- Every texture named in every model exists on disk; every face alias
  resolves; every UV is inside 0 to 16; no element has to below from.
- Blockstate stage mapping checked explicitly: stage=0, stage=1, and
  stage=2 each map only to their own stage model, in all four
  rotations.
- All 28 generated textures verified at 64 by 64.
- Textures and both models were visually reviewed from rendered
  sheets and a flat-shaded model render during the work; three
  generations of the crack and groove treatment were rejected and
  redone based on those reviews (dotted glow, junction blobs, and the
  cooled plate void).

## Honest self-assessment

Better by a large margin, and most of the way to professional within
what Minecraft block models can express. The crust now reads as cracked
lava at all three stages and changes structure as it cools, not just
color. The volcano reads as a small volcano: cone, crater, pool,
channel, ash.

What still keeps it short of fully professional, stated plainly:

- The volcano silhouette is still stepped tiers. Block models are
  cuboids, so a smooth cone does not exist in this format; the offset
  tiers, buttresses, and texture work carry the read, and at a glance
  it is still visibly built from boxes.
- Crack networks are Voronoi-straight segments. Real cooling cracks
  curve more. The groove, core, and halo treatment hides most of it,
  but a long look at the cooling top shows the geometry of the method.
- This was verified from texture sheets and a flat render, not in
  game. Lighting, the block's light levels (15, 9, 2 by stage), and
  neighbouring terrain will change the final read, and that check
  happens on Carter's PC like everything else in this pass.
