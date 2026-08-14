# Custom Rules

- Only ever build the mod using `python build.py`. Never launch the game client automatically using `python launch.py` or any other method.
- This project is for **Minecraft Java 26.2** using **Fabric** modloader. The old 1.x.x versioning (e.g., 1.21.4) was retired — Minecraft now uses year-based versioning (26.2 = 2026, update 2). Keep this in mind for all APIs and resource structures.
- **Adding Items to the Mod**: When adding new items, you MUST follow this exact 5-step process to ensure they are registered correctly and have their textures working in MC 26.2:
  1. **Texture**: Place the item's `.png` file in `src/main/resources/assets/dragonblockarcanedba/textures/item/`.
  2. **Model**: Create a JSON file in `src/main/resources/assets/dragonblockarcanedba/models/item/` with parent `minecraft:item/generated` and mapping `layer0` to `dragonblockarcanedba:item/<item_id>`.
  3. **Item Definition (Crucial for 26.2)**: Create a JSON file in `src/main/resources/assets/dragonblockarcanedba/items/` wrapping the model. Example: `{"model": {"type": "minecraft:model", "model": "dragonblockarcanedba:item/<item_id>"}}`.
  4. **Java Registration**: In `DbaItems.java`, create a `ResourceKey<Item>`, create an `Item` instance using that key, add it to `CreativeTab` via `output.accept()`, and finally register it via `Registry.register(BuiltInRegistries.ITEM, ...)`.
  5. **Translation**: Add the item's in-game name to `src/main/resources/assets/dragonblockarcanedba/lang/en_us.json`.

- **Rendering Architecture (26.2 vs 1.21.4)**: Minecraft 1.21.4 and 26.2 have significantly different rendering architectures. 26.2 has a newer rendering architecture explicitly separating extraction/preparation and drawing, and moves toward OpenGL/Vulkan compatibility. Code/tutorials for 1.21.4 custom rendering (using old MatrixStack/VertexConsumer approaches) generally cannot be directly ported to 26.2. When building custom renderers (e.g., procedural Java geometry for weapons), build directly against **26.2's rendering API** and do not rely on 1.21.4 rendering framework assumptions.

- **Swarm Entity/Weapon Framework**: When building weapons or abilities that deploy multiple persistent entities (like floating swords, shields, or shards) around the player, use the `SwarmHelper` framework to ensure proper state management and prevent logout exploits.
  - Make the entity implement `com.dragonblockarcanedba.entity.ITrackedSwarmEntity`.
  - When the item deploys the swarm, call `SwarmHelper.deploySwarm(stack, count, startingHealth)` to initialize the `swarmHealths` NBT tag.
  - In the entity's damage/destruction logic (e.g. `hurtServer` or collision), update its health to the player's weapon NBT by calling `SwarmHelper.updateSwarmHealth(player, WeaponClass.class, this.getSwarmIndex(), newHealth)`. Do this right before `this.discard()` if health reaches 0.
  - In the entity's `tick()`, if the owner is offline (`owner == null`), dead, or too far away (> 64 blocks), call `this.discard()` *without* updating health. This gracefully despawns them to prevent chunk floating/lag.
  - In the item/ability's tick loop (like `manageShardSwarm`), use `SwarmHelper.getMissingEntities(stack, activeEntities)` to find what shards are legitimately missing (despawned) and respawn them with their exact saved health.

- **Rapid-Fire Weapons (Holding Left-Click)**: When creating weapons or items that use continuous/rapid-fire left-click logic (by holding down the attack key), you must block vanilla block breaking so the player doesn't accidentally mine blocks while spamming the attack. Do this by adding a check in `AttackBlockCallback` in `DragonBlockArcaneDBA.java` to return `InteractionResult.FAIL` if the player is holding the rapid-fire weapon. Do NOT override `canAttackBlock` directly in the item class, as the signature changes across mappings and can cause build failures.

- **Look at other complete weapons/items/mobs to see power scale of how good and advanced looking to make it**