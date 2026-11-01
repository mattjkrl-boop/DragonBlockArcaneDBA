# Realism Pass V2 (Nov 1, 2026, Carter feedback: good but too blocky; not round, realistic)

What changed across all 9 rebuilt swords (tools/build_swords.py is now the single builder, katana included via the new tsuba guard style; tools/build_katana.py is superseded):
- Proportions slimmed toward real weapons: thinner blades and grips, smaller pommels, thinner guards.
- Blades are 7 finer segments instead of 4 chunks, with a two-step tip taper, so curves and tapers step in small increments.
- Every blade segment carries two angled bevel facet boxes hugging the faces at the cutting edge, the way a real edge bevel catches light, instead of one flat slab side.
- Textures are 256 atlases with painted detail: a fuller groove with a bright ridge, edge highlights, fine scratches, brushed metal streaks on guards, crosshatched wrap shading on grips, speckle wear. The blade length gradient was softened after a render showed segment banding.
- Renderer upgraded to per-pixel texture mapping (tools/render3d.py) so the painted detail itself is verifiable here, not just silhouettes: renders/3d/katana_v1_v2_compare.png is the side by side.

Still Minecraft geometry (flat angled planes, no curves), which is the point: realistic reads as correct proportions, faceted bevels, and surface detail, not roundness.
