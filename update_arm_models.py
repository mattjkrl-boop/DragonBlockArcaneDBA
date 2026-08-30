import json, os, shutil, zipfile

left_arm_bone = {
    "name": "left_arm",
    "parent": "root",
    "pivot": [5, 22, 0],
    "cubes": [
        {
            "origin": [3.913, 14.552, -2.062],
            "size": [3.661, 12.067, 4.123],
            "uv": {
                "south": {"uv": [0, 101], "uv_size": [13, 44]},
                "north": {"uv": [138, 119], "uv_size": [13, 44]},
                "east": {"uv": [45, 101], "uv_size": [15, 44]},
                "west": {"uv": [128, 75], "uv_size": [15, 44]},
                "up": {"uv": [141, 132], "uv_size": [13, 15]},
                "down": {"uv": [114, 19], "uv_size": [13, 15]}
            }
        }
    ]
}

right_arm_bone = {
    "name": "right_arm",
    "parent": "root",
    "pivot": [-5, 22, 0],
    "cubes": [
        {
            "origin": [-7.573, 14.552, -2.062],
            "size": [3.661, 12.067, 4.123],
            "uv": {
                "down": {"uv": [101, 19], "uv_size": [13, 15]},
                "up": {"uv": [154, 132], "uv_size": [13, 15]},
                "south": {"uv": [60, 108], "uv_size": [13, 44]},
                "north": {"uv": [125, 119], "uv_size": [13, 44]},
                "west": {"uv": [30, 101], "uv_size": [15, 44]},
                "east": {"uv": [15, 101], "uv_size": [15, 44]}
            }
        }
    ]
}

races = [
    'android', 'arcosian', 'bio_android', 'half_saiyan', 'human',
    'majin', 'namekian', 'neo_tuffle', 'saiyan', 'tuffle',
    'universal_humanoid', 'yardrat'
]

for r in races:
    arm_path = f'Animated/UniversalAnimations/BPM/{r}/models/arm.json'
    arm_data = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.{r}_arm",
                    "texture_width": 256,
                    "texture_height": 256
                },
                "bones": [
                    {
                        "name": "root",
                        "pivot": [0, 0, 0]
                    },
                    right_arm_bone,
                    left_arm_bone
                ]
            }
        ]
    }
    with open(arm_path, 'w', encoding='utf-8') as f:
        json.dump(arm_data, f, indent=2)

    # Sync to resources and config
    builtin_dst = f'src/main/resources/assets/better_player_model/builtin/{r}'
    custom_dst = f'run/config/better_player_model/custom/{r}'
    src_dir = f'Animated/UniversalAnimations/BPM/{r}'
    
    if os.path.exists(builtin_dst):
        shutil.rmtree(builtin_dst)
    shutil.copytree(src_dir, builtin_dst)
    
    if os.path.exists(custom_dst):
        shutil.rmtree(custom_dst)
    shutil.copytree(src_dir, custom_dst)

# Update BPM jar default arm.json
jar_path = 'run/mods/better-player-model-1.1.4-fabric-26.2.jar'
yardrat_dir = 'Animated/UniversalAnimations/BPM/universal_humanoid'
temp_jar = jar_path + '.tmp'

replacement_files = {
    'assets/better_player_model/builtin/default/models/arm.json': os.path.join(yardrat_dir, 'models', 'arm.json'),
}

with zipfile.ZipFile(jar_path, 'r') as zin, zipfile.ZipFile(temp_jar, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename in replacement_files:
            src_file = replacement_files[item.filename]
            with open(src_file, 'rb') as f:
                zout.writestr(item.filename, f.read())
        else:
            zout.writestr(item, zin.read(item.filename))

shutil.move(temp_jar, jar_path)
print('Successfully updated arm.json with both right and left hands across all 12 races!')
