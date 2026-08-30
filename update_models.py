import json, os

head_cubes_yardrat = [
    {
      "origin": [-4.366, 26.446, -2.98],
      "size": [8.733, 9.609, 5.959],
      "uv": {
        "east": {"uv": [79, 0], "uv_size": [22, 35]},
        "up": {"uv": [109.0, 57], "uv_size": [15.9, 21.6]},
        "north": {"uv": [93, 138], "uv_size": [16, 35]},
        "down": {"uv": [93.1, 35.4], "uv_size": [15.9, 21.6]},
        "south": {"uv": [109, 138], "uv_size": [16, 35]},
        "west": {"uv": [15, 66], "uv_size": [22, 35]}
      }
    },
    {
      "origin": [-4.798, 31.346, -4.408],
      "size": [9.597, 5.36, 8.814],
      "uv": {
        "up": {"uv": [29.1, 33], "uv_size": [17.4, 32.0]},
        "down": {"uv": [29.1, 1.0], "uv_size": [17.4, 32.0]},
        "north": {"uv": [110.5, 79], "uv_size": [17.4, 19.5]},
        "east": {"uv": [125, 19], "uv_size": [32, 19]},
        "west": {"uv": [101, 0], "uv_size": [32, 19]},
        "south": {"uv": [93.1, 99], "uv_size": [17.4, 19.5]}
      }
    },
    {
      "origin": [-9.2, 30.5, -1.5],
      "size": [5.0, 2.5, 3.2],
      "pivot": [-4.3, 31.0, 0.0],
      "rotation": [-15.0, -15.0, 28.0],
      "uv": {
        "south": {"uv": [73, 108], "uv_size": [5, 17.4]},
        "east": {"uv": [157.4, 67], "uv_size": [17.2, 5]},
        "north": {"uv": [157, 52], "uv_size": [17.4, 5]},
        "west": {"uv": [73, 125.8], "uv_size": [5, 17.2]},
        "down": {"uv": [59, 90.1], "uv_size": [5, 5.9]},
        "up": {"uv": [128.1, 100], "uv_size": [5.9, 5]}
      }
    },
    {
      "origin": [4.2, 30.5, -1.5],
      "size": [5.0, 2.5, 3.2],
      "pivot": [4.3, 31.0, 0.0],
      "rotation": [-15.0, 15.0, -28.0],
      "uv": {
        "north": {"uv": [59, 66.6], "uv_size": [5, 17.4]},
        "south": {"uv": [157.6, 57], "uv_size": [17.4, 5]},
        "down": {"uv": [114, 30], "uv_size": [5.9, 5]},
        "east": {"uv": [157.4, 62], "uv_size": [17.2, 5]},
        "west": {"uv": [128, 82], "uv_size": [5, 17.2]},
        "up": {"uv": [59, 84], "uv_size": [5, 5.9]}
      }
    }
]

tail_bones = [
    {
        "name": "tail",
        "parent": "body",
        "pivot": [0.0, 13.0, 2.0],
        "rotation": [-25.0, 0.0, 0.0],
        "cubes": [
            {
                "origin": [-1.0, 12.0, 2.0],
                "size": [2.0, 2.0, 4.0],
                "uv": {"north": {"uv": [0, 0], "uv_size": [2, 2]}, "south": {"uv": [4, 0], "uv_size": [2, 2]}, "east": {"uv": [2, 0], "uv_size": [4, 2]}, "west": {"uv": [6, 0], "uv_size": [4, 2]}, "up": {"uv": [2, 2], "uv_size": [2, 4]}, "down": {"uv": [4, 2], "uv_size": [2, 4]}}
            }
        ]
    },
    {
        "name": "tail1",
        "parent": "tail",
        "pivot": [0.0, 13.0, 6.0],
        "rotation": [-15.0, 0.0, 0.0],
        "cubes": [
            {
                "origin": [-1.0, 12.0, 6.0],
                "size": [2.0, 2.0, 4.0],
                "uv": {"north": {"uv": [0, 0], "uv_size": [2, 2]}, "south": {"uv": [4, 0], "uv_size": [2, 2]}, "east": {"uv": [2, 0], "uv_size": [4, 2]}, "west": {"uv": [6, 0], "uv_size": [4, 2]}, "up": {"uv": [2, 2], "uv_size": [2, 4]}, "down": {"uv": [4, 2], "uv_size": [2, 4]}}
            }
        ]
    },
    {
        "name": "tail2",
        "parent": "tail1",
        "pivot": [0.0, 13.0, 10.0],
        "rotation": [30.0, 0.0, 0.0],
        "cubes": [
            {
                "origin": [-1.0, 12.0, 10.0],
                "size": [2.0, 2.0, 4.0],
                "uv": {"north": {"uv": [0, 0], "uv_size": [2, 2]}, "south": {"uv": [4, 0], "uv_size": [2, 2]}, "east": {"uv": [2, 0], "uv_size": [4, 2]}, "west": {"uv": [6, 0], "uv_size": [4, 2]}, "up": {"uv": [2, 2], "uv_size": [2, 4]}, "down": {"uv": [4, 2], "uv_size": [2, 4]}}
            }
        ]
    },
    {
        "name": "tail3",
        "parent": "tail2",
        "pivot": [0.0, 14.0, 14.0],
        "rotation": [40.0, 0.0, 0.0],
        "cubes": [
            {
                "origin": [-0.9, 13.0, 14.0],
                "size": [1.8, 1.8, 3.5],
                "uv": {"north": {"uv": [0, 0], "uv_size": [2, 2]}, "south": {"uv": [4, 0], "uv_size": [2, 2]}, "east": {"uv": [2, 0], "uv_size": [4, 2]}, "west": {"uv": [6, 0], "uv_size": [4, 2]}, "up": {"uv": [2, 2], "uv_size": [2, 4]}, "down": {"uv": [4, 2], "uv_size": [2, 4]}}
            }
        ]
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
            b['cubes'] = head_cubes_yardrat

    data['minecraft:geometry'][0]['bones'] = [
        b for b in data['minecraft:geometry'][0]['bones']
        if not b['name'].startswith('tail')
    ]

    if r in ['saiyan', 'half_saiyan', 'arcosian']:
        data['minecraft:geometry'][0]['bones'].extend(tail_bones)

    with open(main_json_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

print('Successfully updated models/main.json across all 12 races with angled ears and tails!')
