# Batch 1 After Audit: Z Sword, Brave Sword, Grand Sword, Azure Dragon Sword (Nov 1, 2026)

Built by tools/build_swords.py (spec driven, same 5 bone structure as the katana template), concepts in concepts/ for all four, previews in renders/. All four added to the MIGRATED set in DbaGeoWeaponRenderers.

Old vs new, measured from the actual files (old read from git HEAD):
- z_sword: 1 bone, 70 cubes, 64 atlas -> 5 bones, 18 cubes, 128 atlas
- brave_sword: 1 bone, 70 cubes, 64 -> 5 bones, 21 cubes, 128
- grand_sword: 1 bone, 78 cubes, 64 -> 5 bones, 24 cubes, 128
- azure_dragon_sword: 1 bone, 70 cubes, 64 -> 5 bones, 21 cubes, 128
The old cube counts are not detail: they are 1 pixel tall slices extruding the flat inventory sprite, so the old in-hand model is a flat cardboard cutout, invisible edge-on, with no separate grip, guard, or glow geometry. The new cubes are volumetric parts with real thickness, wider guards, pommels, and a separate glow edge bone per weapon.

Design locks from concepts: Z Sword wide pale blade, double gold bar guard, long brown two-hand grip, gold ball pommel, slower swing (0.45). Brave Sword bright blade with gold glow edge, gold wing guard, red grip, gold pommel with glowing red gem, standard swing (0.35). Grand Sword massive dark iron blade, orange glow edge plus ember crack glow cubes on the faces, bronze plate guard with rivets, black cord grip, spiked pommel, slowest swing (0.5). Azure Dragon Sword curved azure blade with cyan edge, teal dragon wing guard, scale textured teal grip, glowing blue orb pommel, fastest swing (0.3).

Safety checks run before this commit:
- All 4 asset files (geo, animation, atlas, glowmask) verified present for every MIGRATED weapon, katana included. Flat item JSONs untouched, so GUI, ground, and frame looks are unchanged for all 19 weapons.
- Non-migrated weapons are byte-identical in behavior: same shared animations, same sprite texture path, only the renderer class changed underneath them (plus the foundation per-holder state fix, which is strictly more correct than the local-player global it replaced).
- Animation names in the new files (idle, swing, charge) match what the DbaGeoWeapon controller requests.
- Reversibility: each weapon is independent. Removing its id from MIGRATED and restoring its geo file from git reverts it alone; nothing else depends on the new assets.

Known unverified: in-game appearance. Previews are flat projections from the generator (rotations flattened, so wing guards read blockier than the 3D data). First in-game screenshots remain the proof step for framing and glow, with tuning values isolated in DbaWeaponRenderer.
