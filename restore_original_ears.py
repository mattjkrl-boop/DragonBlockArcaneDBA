import json, os, shutil, zipfile

original_head_cubes = [
    {
      "origin": [
        -4.366,
        26.446,
        -2.98
      ],
      "size": [
        8.733,
        9.609,
        5.959
      ],
      "uv": {
        "east": {
          "uv": [79, 0],
          "uv_size": [22, 35]
        },
        "up": {
          "uv": [109.0, 57],
          "uv_size": [15.9, 21.6]
        },
        "north": {
          "uv": [93, 138],
          "uv_size": [16, 35]
        },
        "down": {
          "uv": [93.1, 35.4],
          "uv_size": [15.9, 21.6]
        },
        "south": {
          "uv": [109, 138],
          "uv_size": [16, 35]
        },
        "west": {
          "uv": [15, 66],
          "uv_size": [22, 35]
        }
      }
    },
    {
      "origin": [
        -4.798,
        31.346,
        -4.408
      ],
      "size": [
        9.597,
        5.36,
        8.814
      ],
      "uv": {
        "up": {
          "uv": [29.1, 33],
          "uv_size": [17.4, 32.0]
        },
        "down": {
          "uv": [29.1, 1.0],
          "uv_size": [17.4, 32.0]
        },
        "north": {
          "uv": [110.5, 79],
          "uv_size": [17.4, 19.5]
        },
        "east": {
          "uv": [125, 19],
          "uv_size": [32, 19]
        },
        "west": {
          "uv": [101, 0],
          "uv_size": [32, 19]
        },
        "south": {
          "uv": [93.1, 99],
          "uv_size": [17.4, 19.5]
        }
      }
    },
    {
      "origin": [
        -6.499,
        31.021,
        -1.642
      ],
      "size": [
        2.754,
        5.299,
        3.282
      ],
      "uv": {
        "south": {
          "uv": [73, 108],
          "uv_size": [5, 17.4]
        },
        "east": {
          "uv": [157.4, 67],
          "uv_size": [17.2, 5]
        },
        "north": {
          "uv": [157, 52],
          "uv_size": [17.4, 5]
        },
        "west": {
          "uv": [73, 125.8],
          "uv_size": [5, 17.2]
        },
        "down": {
          "uv": [59, 90.1],
          "uv_size": [5, 5.9]
        },
        "up": {
          "uv": [128.1, 100],
          "uv_size": [5.9, 5]
        }
      }
    },
    {
      "origin": [
        3.746,
        31.021,
        -1.642
      ],
      "size": [
        2.754,
        5.299,
        3.282
      ],
      "uv": {
        "north": {
          "uv": [59, 66.6],
          "uv_size": [5, 17.4]
        },
        "south": {
          "uv": [157.6, 57],
          "uv_size": [17.4, 5]
        },
        "down": {
          "uv": [114, 30],
          "uv_size": [5.9, 5]
        },
        "east": {
          "uv": [157.4, 62],
          "uv_size": [17.2, 5]
        },
        "west": {
          "uv": [128, 82],
          "uv_size": [5, 17.2]
        },
        "up": {
          "uv": [59, 84],
          "uv_size": [5, 5.9]
        }
      }
    }
]

races = [
    'android', 'arcosian', 'bio_android', 'half_saiyan', 'human',
    'majin', 'namekian', 'neo_tuffle', 'saiyan', 'tuffle',
    'universal_humanoid', 'yardrat'
]

for r in races:
    main_json_path = f'Animated/UniversalAnimations/BPM/{r}/models/main.json'
    with open(main_json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    for b in data['minecraft:geometry'][0]['bones']:
        if b['name'] == 'head':
            b['cubes'] = original_head_cubes

    with open(main_json_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

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

# Update BPM jar default
jar_path = 'run/mods/better-player-model-1.1.4-fabric-26.2.jar'
yardrat_dir = 'Animated/UniversalAnimations/BPM/universal_humanoid'
temp_jar = jar_path + '.tmp'

replacement_files = {
    'assets/better_player_model/builtin/default/models/main.json': os.path.join(yardrat_dir, 'models', 'main.json'),
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
print('Successfully restored the exact original model head and ears across all files!')
