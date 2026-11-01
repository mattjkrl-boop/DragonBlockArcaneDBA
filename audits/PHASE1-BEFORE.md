# Phase 1 Before Audit: Weapons (Nov 1, 2026)

Taken at HEAD ac66e3a, before any Phase 1 code or asset change. Method per BUILD-VERIFY.md: read the files, inventory behavior, check facts from the files themselves. This is an audit, not a compile. The build has never passed.

## Inventory (verified from files, not docs)

All 19 weapons have: an item class with Left and Right abilities, a GeckoLib geo model, a flat item texture, a glowmask texture, and an item definition JSON that renders the flat model in GUI/fixed/ground and the GeckoLib model in hand.

| Weapon | Item class lines | Geo model | Procedural model | _3d wiring | Swing trail |
|---|---|---|---|---|---|
| azure_dragon_sword | 371 | 1 bone, 70 cubes, 64x64 | no | no | yes, 0x3FA9FF |
| bansho_fan | 244 | 1 bone, 73 cubes, 64x64 | yes | yes | no |
| blaster_gun | 286 | 1 bone, 37 cubes, 64x64 | no | no | no |
| brave_sword | 369 | 1 bone, 70 cubes, 64x64 | no | no | yes, 0xFFD54A |
| cursed_blade (CurseBladeItem) | 369 | 1 bone, 92 cubes, 64x64 | yes | no | yes, 0x8A2BE2 |
| dabura_sword | 330 | 1 bone, 72 cubes, 64x64 | no | no | yes, 0xC23BFF |
| devil_trident | 323 | 1 bone, 63 cubes, 64x64 | yes | yes | yes, 0xFF2244 |
| dimensional_sword | 174 | 1 bone, 79 cubes, 64x64 | yes | yes | yes, 0x9B6BFF |
| evil_spear | 314 | 1 bone, 58 cubes, 64x64 | no | no | yes, 0xB0003A |
| grand_sword | 293 | 1 bone, 78 cubes, 64x64 | no | no | yes, 0xFFB347 |
| hollows_edge | 392 | 1 bone, 69 cubes, 64x64 | yes | no | yes, 0x5A5A7A |
| katana | 440 | 1 bone, 65 cubes, 64x64 | no | no | yes, 0xFFFFFF |
| ox_kings_ax | 424 | 1 bone, 74 cubes, 64x64 | no | no | yes, 0xFF6A2B |
| power_pole | 232 | 1 bone, 55 cubes, 64x64 | yes | yes | no |
| saber | 964 | 1 bone, 69 cubes, 64x64 | no | no | yes, 0x66CCFF |
| sickle_of_sorrow | 221 | 1 bone, 62 cubes, 64x64 | yes | no | yes, 0x8B0000 |
| spirit_sword | 258 | 1 bone, 67 cubes, 64x64 | yes | no | yes, 0x7DF9FF |
| whis_staff | 229 | 1 bone, 59 cubes, 64x64 | yes | no | no |
| z_sword | 316 | 1 bone, 70 cubes, 64x64 | yes | no | yes, 0xBFE6FF |

Trail colors are from SwingTrailRenderer.colorFor. Bansho Fan, Blaster Gun, Power Pole, and Whis Staff return 0 (no trail).

## Findings that Phase 1 must fix

1. Every geo model is a single bone. No separate grip, blade, guard, or glow bones, so no part-level animation or glow pulsing is possible today. All weapons share one animation file (dba_weapon.animation.json) through DbaGeoWeaponRenderers.
2. Three rendering paths coexist: GeckoLib in hand for all 19, a code-built procedural model (ProceduralWeaponRenderer + WeaponModelRegistry) for 10, and the _3d item swap (ItemModelResolverMixin + DbaConfig.use3dWeapons) wired for only 4 (bansho_fan, devil_trident, dimensional_sword, power_pole). Which path a player sees depends on the weapon and a config flag. Phase 1 picks one standard and retires the rest cleanly.
3. Textures are 64x64 for every geo model. That caps detail for the overhaul; the race models moved to a 512 atlas for a reason. Phase 1 concept sheets set the new texture budget per weapon before model work starts.
4. SaberItem is 964 lines, roughly double to quadruple every other weapon class. Any Saber visual change touches the largest and riskiest file in the set; it goes last in the weapon order, not first.
5. Nothing in the weapon visuals is broken on its face: all geo JSON parses, all textures and glowmasks exist, all item JSONs exist. The overhaul is a quality and consistency job, not a repair job. Ability logic, damage, and cooldowns stay untouched in Phase 1.

## Phase 1 order (proposed)

Start with katana (clean mid-size class, white trail, iconic), then z_sword, brave_sword, grand_sword, azure_dragon_sword, dabura_sword, cursed_blade, hollows_edge, saber last among blades; then evil_spear, devil_trident, power_pole, sickle_of_sorrow, ox_kings_ax; then dimensional_sword, spirit_sword, whis_staff, bansho_fan, blaster_gun. Concept sheet first for each, per the standing visual workflow.
