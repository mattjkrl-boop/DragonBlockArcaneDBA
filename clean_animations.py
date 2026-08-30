import json, os, math

races = [
    'android', 'arcosian', 'bio_android', 'half_saiyan', 'human',
    'majin', 'namekian', 'neo_tuffle', 'saiyan', 'tuffle',
    'universal_humanoid', 'yardrat'
]

# Load existing universal humanoid animations
src_anim_path = 'Animated/UniversalAnimations/BPM/universal_humanoid/animations/main.animation.json'
with open(src_anim_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

anims = data.get('animations', {})

# Clean idle animation: natural breathing upright DBZ martial arts stance
idle_frames = {}
for i in range(31):
    t = round(i * 0.1, 2)
    s = math.sin(t * math.pi)
    t_str = str(t)
    idle_frames[t_str] = s

anims['idle'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {t: [round(s * 1.2, 3), 0.0, 0.0] for t, s in idle_frames.items()},
            "position": {t: [0.0, round(s * 0.15, 3), 0.0] for t, s in idle_frames.items()}
        },
        "head": {
            "rotation": {t: [round(-s * 1.0, 3), 0.0, 0.0] for t, s in idle_frames.items()}
        },
        "left_arm": {
            "rotation": {t: [round(s * 2.0, 3), 0.0, 3.0] for t, s in idle_frames.items()}
        },
        "right_arm": {
            "rotation": {t: [round(s * 2.0, 3), 0.0, -3.0] for t, s in idle_frames.items()}
        },
        "left_leg": {
            "rotation": {"0": [0.0, 0.0, 0.0]}
        },
        "right_leg": {
            "rotation": {"0": [0.0, 0.0, 0.0]}
        },
        "tail": {
            "rotation": {t: [-20.0, round(math.sin(float(t) * 2 * math.pi) * 8.0, 3), 0.0] for t in idle_frames.keys()}
        },
        "tail1": {
            "rotation": {t: [-10.0, round(math.sin(float(t) * 2 * math.pi + 0.5) * 12.0, 3), 0.0] for t in idle_frames.keys()}
        }
    }
}

# Clean walk animation: natural stride
walk_frames = {}
for i in range(21):
    t = round(i * 0.05, 2)
    t_str = str(t)
    s = math.sin(t * 2 * math.pi)
    c = math.cos(t * 2 * math.pi)
    walk_frames[t_str] = (s, c)

anims['walk'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {t: [4.0, round(s * 3.0, 3), 0.0] for t, (s, c) in walk_frames.items()},
            "position": {t: [0.0, round(abs(s) * 0.3, 3), 0.0] for t, (s, c) in walk_frames.items()}
        },
        "head": {
            "rotation": {t: [-3.0, round(-s * 2.0, 3), 0.0] for t, (s, c) in walk_frames.items()}
        },
        "left_arm": {
            "rotation": {t: [round(-s * 28.0, 3), 0.0, 4.0] for t, (s, c) in walk_frames.items()}
        },
        "right_arm": {
            "rotation": {t: [round(s * 28.0, 3), 0.0, -4.0] for t, (s, c) in walk_frames.items()}
        },
        "left_leg": {
            "rotation": {t: [round(s * 32.0, 3), 0.0, 0.0] for t, (s, c) in walk_frames.items()}
        },
        "right_leg": {
            "rotation": {t: [round(-s * 32.0, 3), 0.0, 0.0] for t, (s, c) in walk_frames.items()}
        },
        "tail": {
            "rotation": {t: [-25.0, round(s * 15.0, 3), 0.0] for t, (s, c) in walk_frames.items()}
        }
    }
}

# Clean run animation: fast sprint
run_frames = {}
for i in range(16):
    t = round(i * 0.0333, 3)
    t_str = str(t)
    phase = (t / 0.5) * 2 * math.pi
    s = math.sin(phase)
    run_frames[t_str] = s

anims['run'] = {
    "animation_length": 0.5,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {t: [16.0, round(s * 5.0, 3), 0.0] for t, s in run_frames.items()},
            "position": {t: [0.0, round(abs(s) * 0.6, 3), 0.0] for t, s in run_frames.items()}
        },
        "head": {
            "rotation": {t: [-12.0, round(-s * 3.0, 3), 0.0] for t, s in run_frames.items()}
        },
        "left_arm": {
            "rotation": {t: [round(-s * 55.0, 3), 0.0, 12.0] for t, s in run_frames.items()}
        },
        "right_arm": {
            "rotation": {t: [round(s * 55.0, 3), 0.0, -12.0] for t, s in run_frames.items()}
        },
        "left_leg": {
            "rotation": {t: [round(s * 60.0, 3), 0.0, 0.0] for t, s in run_frames.items()}
        },
        "right_leg": {
            "rotation": {t: [round(-s * 60.0, 3), 0.0, 0.0] for t, s in run_frames.items()}
        },
        "tail": {
            "rotation": {t: [-30.0, round(s * 20.0, 3), 0.0] for t, s in run_frames.items()}
        }
    }
}

# Emotes: extra0 to extra7
# extra0: Dance
anims['extra0'] = {
    "animation_length": 1.2,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [0, 0, -10], "0.3": [0, 0, 10], "0.6": [0, 0, -10], "0.9": [0, 0, 10], "1.2": [0, 0, -10]}, "position": {"0": [0, 0, 0], "0.3": [0, 1, 0], "0.6": [0, 0, 0], "0.9": [0, 1, 0], "1.2": [0, 0, 0]}},
        "head": {"rotation": {"0": [0, 0, 8], "0.3": [0, 0, -8], "0.6": [0, 0, 8], "0.9": [0, 0, -8], "1.2": [0, 0, 8]}},
        "left_arm": {"rotation": {"0": [-60, 0, 80], "0.3": [-90, 0, 40], "0.6": [-60, 0, 80], "0.9": [-90, 0, 40], "1.2": [-60, 0, 80]}},
        "right_arm": {"rotation": {"0": [-90, 0, -40], "0.3": [-60, 0, -80], "0.6": [-90, 0, -40], "0.9": [-60, 0, -80], "1.2": [-90, 0, -40]}},
        "left_leg": {"rotation": {"0": [0, 0, -10], "0.3": [15, 0, 0], "0.6": [0, 0, -10], "0.9": [15, 0, 0], "1.2": [0, 0, -10]}},
        "right_leg": {"rotation": {"0": [15, 0, 0], "0.3": [0, 0, 10], "0.6": [15, 0, 0], "0.9": [0, 0, 10], "1.2": [15, 0, 0]}}
    }
}

# extra1: Wave
anims['extra1'] = {
    "animation_length": 1.5,
    "loop": True,
    "bones": {
        "head": {"rotation": {"0": [0, 10, 0], "0.75": [0, -10, 0], "1.5": [0, 10, 0]}},
        "right_arm": {"rotation": {"0": [-120, 0, -30], "0.25": [-130, 0, -60], "0.5": [-120, 0, -30], "0.75": [-130, 0, -60], "1.0": [-120, 0, -30], "1.25": [-130, 0, -60], "1.5": [-120, 0, -30]}},
        "left_arm": {"rotation": {"0": [0, 0, 5], "1.5": [0, 0, 5]}}
    }
}

# extra2: Kick
anims['extra2'] = {
    "animation_length": 1.0,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.2": [-10, 30, -15], "0.4": [-5, 45, -25], "0.7": [0, 15, -5], "1.0": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0], "0.2": [40, -15, 20], "0.4": [95, -30, 35], "0.7": [30, -10, 10], "1.0": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.4": [-60, 20, 40], "1.0": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.4": [40, -20, -50], "1.0": [0, 0, 0]}}
    }
}

# extra3: Punch Flurry
anims['extra3'] = {
    "animation_length": 0.8,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [5, -15, 0], "0.2": [5, 15, 0], "0.4": [5, -15, 0], "0.6": [5, 15, 0], "0.8": [5, -15, 0]}},
        "left_arm": {"rotation": {"0": [-85, 10, 0], "0.2": [-20, 20, 20], "0.4": [-85, 10, 0], "0.6": [-20, 20, 20], "0.8": [-85, 10, 0]}},
        "right_arm": {"rotation": {"0": [-20, -20, -20], "0.2": [-85, -10, 0], "0.4": [-20, -20, -20], "0.6": [-85, -10, 0], "0.8": [-20, -20, -20]}}
    }
}

# extra4: Parry / Guard
anims['extra4'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [5, 0, 0], "0.5": [7, 0, 0], "1.0": [5, 0, 0]}},
        "left_arm": {"rotation": {"0": [-70, 35, 45], "0.5": [-72, 35, 45], "1.0": [-70, 35, 45]}},
        "right_arm": {"rotation": {"0": [-70, -35, -45], "0.5": [-72, -35, -45], "1.0": [-70, -35, -45]}},
        "left_leg": {"rotation": {"0": [-5, 0, -5]}},
        "right_leg": {"rotation": {"0": [10, 0, 5]}}
    }
}

# extra5: Super Saiyan Power-Up Shout
anims['extra5'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {str(round(i*0.05, 2)): [12.0 + (1.5 if i%2==0 else -1.5), 0.0, 0.0] for i in range(41)}, "position": {"0": [0, -1.5, 0], "1.0": [0, -1.8, 0], "2.0": [0, -1.5, 0]}},
        "head": {"rotation": {"0": [-30, 0, 0], "1.0": [-35, 0, 0], "2.0": [-30, 0, 0]}},
        "left_arm": {"rotation": {"0": [25, 0, 35], "0.5": [30, 0, 40], "1.0": [25, 0, 35], "1.5": [30, 0, 40], "2.0": [25, 0, 35]}},
        "right_arm": {"rotation": {"0": [25, 0, -35], "0.5": [30, 0, -40], "1.0": [25, 0, -35], "1.5": [30, 0, -40], "2.0": [25, 0, -35]}},
        "left_leg": {"rotation": {"0": [-10, 0, -15]}},
        "right_leg": {"rotation": {"0": [-10, 0, 15]}}
    }
}

# extra6: Talk
anims['extra6'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "head": {"rotation": {"0": [0, 5, 0], "0.5": [5, -5, 0], "1.0": [-3, 5, 0], "1.5": [4, 0, 0], "2.0": [0, 5, 0]}},
        "right_arm": {"rotation": {"0": [-30, 0, -15], "0.5": [-50, 10, -25], "1.0": [-20, -5, -10], "1.5": [-45, 5, -20], "2.0": [-30, 0, -15]}},
        "left_arm": {"rotation": {"0": [0, 0, 5], "1.0": [-15, 0, 10], "2.0": [0, 0, 5]}}
    }
}

# extra7: Zombie Walk
anims['extra7'] = {
    "animation_length": 1.2,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [10, 0, 0]}},
        "left_arm": {"rotation": {"0": [-85, 0, 0], "0.6": [-90, 0, 0], "1.2": [-85, 0, 0]}},
        "right_arm": {"rotation": {"0": [-85, 0, 0], "0.6": [-90, 0, 0], "1.2": [-85, 0, 0]}},
        "left_leg": {"rotation": {"0": [25, 0, 0], "0.6": [-25, 0, 0], "1.2": [25, 0, 0]}},
        "right_leg": {"rotation": {"0": [-25, 0, 0], "0.6": [25, 0, 0], "1.2": [-25, 0, 0]}}
    }
}

data['animations'] = anims

for r in races:
    dst_path = f'Animated/UniversalAnimations/BPM/{r}/animations/main.animation.json'
    with open(dst_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

print('Successfully cleaned and standardized animations across all 12 races!')
