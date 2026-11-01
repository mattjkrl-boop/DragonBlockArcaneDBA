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

## Addendum: display contexts, other players, and first-person visibility (Nov 1, 2026)

Carter's Phase 1 requirement (Nov 1, 2026): advanced looks and visuals when holding the item in first and third person, when using the item in first and third person, and everything, plus what other people see when you use it, plus making sure you can still see when you use the weapon. Audited against the code the same day:

### How display contexts work today
- Every weapon item JSON selects the flat 2D model for gui, fixed, and ground only. Every other context (firstperson_righthand, firstperson_lefthand, thirdperson_righthand, thirdperson_lefthand, head, on_shelf) falls through to the same GeckoLib model with no per-context split, tuning, or transforms.
- All 19 geo base models are parent handheld with zero display blocks. The display transforms that exist sit in a few flat models (azure_dragon_sword, cursed_blade, saber share near identical copy-paste values at scale 2.5 or 1.5; the katana flat model has none and uses parent generated). Those hand-context transforms can never apply in hand, because in hand the geo fallback renders instead. Net: there is no working first person vs third person tuning for the 3D weapons today.
- One shared animation file for all 19 weapons: idle (3s gentle sway), swing (0.35s loop), charge (0.4s shake loop), all on the single weapon bone. Same in both hands, all contexts, all players.

### What other people see today (multiplayer gap, the big one)
- DbaGeoWeaponRenderers.state reads only the local player (Minecraft.getInstance().player) and receives only the Item, never the holder. DbaGeoWeapon.Hooks.STATE is a per-item global, and the AnimatableInstanceCache is cached per Item, not per holder or stack.
- Consequences: on your screen, another player's weapon animates from YOUR state, not theirs. If you charge, every rendered copy of that item charges. If they charge and you do not, you see their weapon idle. Weapon idle, swing, and charge animation is effectively broken for remote players.
- What does work for remote players: swing trails (SwingTrailRenderer iterates level.players()), and ability visuals (beams, waves, charge orbs are server entities with SynchedEntityData, e.g. KatanaChargeEntity syncs charge ratio and caster id, rendered on every client).

### Can you still see when you use it today
- No system guarantees it. There is no first-person weapon visibility rule: no FP-specific scale, offset, or in-frame check for weapons while using or charging.
- Partial precedents exist to generalize: DbaConfig.firstPersonTransparency plus EntityRenderDispatcherMixin handle the player's own body in first person, and some rush trail renderers scale down for the first-person owner (AzureRushTrailRenderer uses 0.45x in FP). Charge and aura effects anchored at the caster have no equivalent FP treatment, so a charge orb or aura can fill the first-person view, and the using pose plus the tiny shared shake loop is all the feedback the holder gets.

### Phase 1 standard added by this requirement
Every weapon must pass all of these, in the before/after audit for its change:
1. Holding, first person: tuned transform and framing, weapon clearly visible and detailed, not the third person framing reused.
2. Holding, third person (self and other players): tuned transform, correct grip, scale, and glow.
3. Using, first person: swing and charge/use animation distinct from third person where it helps, weapon stays in frame for the whole use, charge and aura effects are FP-scaled or offset so the player can still see the target and surroundings.
4. Using, third person (self and other players): swing and charge/use animation driven by the actual holder's synced state, per holder, not per item and not from the local player. Fixing the STATE hook and the per-item cache is part of Phase 1, not a later phase.
5. Everything else: GUI, ground, fixed, head, offhand, and on_shelf contexts all get the same quality pass; offhand use is detected, not just main hand.
6. Per-weapon animations replace the one shared idle/swing/charge file, on a multi-bone model (grip, blade, guard, glow bones minimum) so states can actually differ.
