import json
import os

weapons = ['dimensional_sword', 'power_pole', 'devil_trident', 'sickle_of_sorrow', 'spirit_sword', 'bansho_fan', 'hollows_edge', 'whis_staff']
base_dir = r"src\main\resources\assets\dragonblockarcanedba\items"

for w in weapons:
    path = os.path.join(base_dir, w + ".json")
    data = {
        "model": {
            "type": "minecraft:special",
            "base": f"dragonblockarcanedba:item/{w}",
            "model": {
                "type": "dragonblockarcanedba:procedural_weapon",
                "weapon": w
            }
        }
    }
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
    print(f"Updated {w}.json")
