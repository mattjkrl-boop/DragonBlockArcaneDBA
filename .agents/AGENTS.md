# Custom Rules

- Only ever build the mod using `python build.py`. Never launch the game client automatically using `python launch.py` or any other method.
- This project is for Minecraft Java 26.2 (1.21.4). Keep this in mind for all APIs and resource structures.
- **Adding Items to the Mod**: When adding new items, you MUST follow this exact 5-step process to ensure they are registered correctly and have their textures working in 1.21.4:
  1. **Texture**: Place the item's `.png` file in `src/main/resources/assets/dragonblockarcanedba/textures/item/`.
  2. **Model**: Create a JSON file in `src/main/resources/assets/dragonblockarcanedba/models/item/` with parent `minecraft:item/generated` and mapping `layer0` to `dragonblockarcanedba:item/<item_id>`.
  3. **Item Definition (Crucial for 1.21.4)**: Create a JSON file in `src/main/resources/assets/dragonblockarcanedba/items/` wrapping the model. Example: `{"model": {"type": "minecraft:model", "model": "dragonblockarcanedba:item/<item_id>"}}`.
  4. **Java Registration**: In `DbaItems.java`, create a `ResourceKey<Item>`, create an `Item` instance using that key, add it to `CreativeTab` via `output.accept()`, and finally register it via `Registry.register(BuiltInRegistries.ITEM, ...)`.
  5. **Translation**: Add the item's in-game name to `src/main/resources/assets/dragonblockarcanedba/lang/en_us.json`.
