import json, os, math, shutil, zipfile

races = [
    'android', 'arcosian', 'bio_android', 'half_saiyan', 'human',
    'majin', 'namekian', 'neo_tuffle', 'saiyan', 'tuffle',
    'universal_humanoid', 'yardrat'
]

# Clean DBZ Universal Animation Library
anims = {}

# 1. IDLE (Upright martial arts breathing stance)
anims['idle'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(round(i*0.1, 2)): [round(math.sin(i*0.1*math.pi)*1.2, 3), 0.0, 0.0] for i in range(21)},
            "position": {str(round(i*0.1, 2)): [0.0, round(math.sin(i*0.1*math.pi)*0.15, 3), 0.0] for i in range(21)}
        },
        "head": {
            "rotation": {str(round(i*0.1, 2)): [round(-math.sin(i*0.1*math.pi)*1.0, 3), 0.0, 0.0] for i in range(21)}
        },
        "left_arm": {
            "rotation": {str(round(i*0.1, 2)): [round(math.sin(i*0.1*math.pi)*2.0, 3), 0.0, 3.0] for i in range(21)}
        },
        "right_arm": {
            "rotation": {str(round(i*0.1, 2)): [round(math.sin(i*0.1*math.pi)*2.0, 3), 0.0, -3.0] for i in range(21)}
        },
        "left_leg": {"rotation": {"0": [0.0, 0.0, 0.0]}},
        "right_leg": {"rotation": {"0": [0.0, 0.0, 0.0]}},
        "tail": {
            "rotation": {str(round(i*0.1, 2)): [-20.0, round(math.sin(i*0.1*2*math.pi)*8.0, 3), 0.0] for i in range(21)}
        },
        "tail1": {
            "rotation": {str(round(i*0.1, 2)): [-10.0, round(math.sin(i*0.1*2*math.pi+0.5)*12.0, 3), 0.0] for i in range(21)}
        }
    }
}
anims['item_idle'] = anims['idle']

# 2. WALK (Natural upright stride)
walk_frames = {}
for i in range(21):
    t = round(i * 0.05, 2)
    s = math.sin(t * 2 * math.pi)
    walk_frames[str(t)] = s

anims['walk'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {t: [2.0, round(s * 2.0, 3), 0.0] for t, s in walk_frames.items()},
            "position": {t: [0.0, round(abs(s) * 0.25, 3), 0.0] for t, s in walk_frames.items()}
        },
        "head": {
            "rotation": {t: [-1.0, round(-s * 1.5, 3), 0.0] for t, s in walk_frames.items()}
        },
        "left_arm": {
            "rotation": {t: [round(-s * 26.0, 3), 0.0, 3.0] for t, s in walk_frames.items()}
        },
        "right_arm": {
            "rotation": {t: [round(s * 26.0, 3), 0.0, -3.0] for t, s in walk_frames.items()}
        },
        "left_leg": {
            "rotation": {t: [round(s * 30.0, 3), 0.0, 0.0] for t, s in walk_frames.items()}
        },
        "right_leg": {
            "rotation": {t: [round(-s * 30.0, 3), 0.0, 0.0] for t, s in walk_frames.items()}
        },
        "tail": {
            "rotation": {t: [-25.0, round(s * 15.0, 3), 0.0] for t, s in walk_frames.items()}
        }
    }
}
anims['walk_back'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0]}},
        "left_arm": {t: [round(s * 20.0, 3), 0.0, 3.0] for t, s in walk_frames.items()},
        "right_arm": {t: [round(-s * 20.0, 3), 0.0, -3.0] for t, s in walk_frames.items()},
        "left_leg": {t: [round(-s * 25.0, 3), 0.0, 0.0] for t, s in walk_frames.items()},
        "right_leg": {t: [round(s * 25.0, 3), 0.0, 0.0] for t, s in walk_frames.items()}
    }
}

# 3. RUN (Fast martial arts forward sprint)
run_frames = {}
for i in range(16):
    t = round(i * 0.0333, 3)
    s = math.sin((t / 0.5) * 2 * math.pi)
    run_frames[str(t)] = s

anims['run'] = {
    "animation_length": 0.5,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {t: [14.0, round(s * 4.0, 3), 0.0] for t, s in run_frames.items()},
            "position": {t: [0.0, round(abs(s) * 0.5, 3), 0.0] for t, s in run_frames.items()}
        },
        "head": {
            "rotation": {t: [-10.0, round(-s * 2.0, 3), 0.0] for t, s in run_frames.items()}
        },
        "left_arm": {
            "rotation": {t: [round(-s * 50.0, 3), 0.0, 8.0] for t, s in run_frames.items()}
        },
        "right_arm": {
            "rotation": {t: [round(s * 50.0, 3), 0.0, -8.0] for t, s in run_frames.items()}
        },
        "left_leg": {
            "rotation": {t: [round(s * 55.0, 3), 0.0, 0.0] for t, s in run_frames.items()}
        },
        "right_leg": {
            "rotation": {t: [round(-s * 55.0, 3), 0.0, 0.0] for t, s in run_frames.items()}
        },
        "tail": {
            "rotation": {t: [-30.0, round(s * 20.0, 3), 0.0] for t, s in run_frames.items()}
        }
    }
}

# 4. FLY (DBZ Flight pose - streamlined body, slight arm tuck)
anims['fly'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [40.0, 0.0, 0.0], "0.5": [42.0, 0.0, 0.0], "1.0": [40.0, 0.0, 0.0]}},
        "head": {"rotation": {"0": [-35.0, 0.0, 0.0], "0.5": [-37.0, 0.0, 0.0], "1.0": [-35.0, 0.0, 0.0]}},
        "left_arm": {"rotation": {"0": [35.0, 0.0, 15.0], "0.5": [38.0, 0.0, 18.0], "1.0": [35.0, 0.0, 15.0]}},
        "right_arm": {"rotation": {"0": [35.0, 0.0, -15.0], "0.5": [38.0, 0.0, -18.0], "1.0": [35.0, 0.0, -15.0]}},
        "left_leg": {"rotation": {"0": [-10.0, 0.0, -3.0], "0.5": [-12.0, 0.0, -3.0], "1.0": [-10.0, 0.0, -3.0]}},
        "right_leg": {"rotation": {"0": [-10.0, 0.0, 3.0], "0.5": [-12.0, 0.0, 3.0], "1.0": [-10.0, 0.0, 3.0]}},
        "tail": {"rotation": {"0": [-45.0, 0.0, 0.0], "0.5": [-48.0, 0.0, 0.0], "1.0": [-45.0, 0.0, 0.0]}}
    }
}

# 5. SWIM
anims['swim'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [75.0, 0.0, 0.0], "0.5": [80.0, 0.0, 0.0], "1.0": [75.0, 0.0, 0.0]}},
        "head": {"rotation": {"0": [-55.0, 0.0, 0.0]}},
        "left_arm": {"rotation": {"0": [40.0, 0.0, 20.0], "0.5": [-30.0, 0.0, 25.0], "1.0": [40.0, 0.0, 20.0]}},
        "right_arm": {"rotation": {"0": [-30.0, 0.0, -25.0], "0.5": [40.0, 0.0, -20.0], "1.0": [-30.0, 0.0, -25.0]}},
        "left_leg": {"rotation": {"0": [10.0, 0.0, 0.0], "0.5": [-10.0, 0.0, 0.0], "1.0": [10.0, 0.0, 0.0]}},
        "right_leg": {"rotation": {"0": [-10.0, 0.0, 0.0], "0.5": [10.0, 0.0, 0.0], "1.0": [-10.0, 0.0, 0.0]}}
    }
}

# 6. SNEAK / CROUCH
anims['sneak_idle'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [20.0, 0.0, 0.0]}, "position": {"0": [0.0, -3.0, 0.0]}},
        "head": {"rotation": {"0": [-15.0, 0.0, 0.0]}},
        "left_arm": {"rotation": {"0": [-20.0, 0.0, 10.0]}},
        "right_arm": {"rotation": {"0": [-20.0, 0.0, -10.0]}},
        "left_leg": {"rotation": {"0": [-20.0, 0.0, -5.0]}},
        "right_leg": {"rotation": {"0": [20.0, 0.0, 5.0]}}
    }
}
anims['sneaking'] = anims['sneak_idle']
anims['sneak'] = anims['sneak_idle']
anims['sneak_walk'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [22.0, 0.0, 0.0]}, "position": {"0": [0.0, -3.0, 0.0]}},
        "head": {"rotation": {"0": [-18.0, 0.0, 0.0]}},
        "left_arm": {t: [round(-s * 15.0 - 15.0, 3), 0.0, 10.0] for t, s in walk_frames.items()},
        "right_arm": {t: [round(s * 15.0 - 15.0, 3), 0.0, -10.0] for t, s in walk_frames.items()},
        "left_leg": {t: [round(s * 25.0, 3), 0.0, -3.0] for t, s in walk_frames.items()},
        "right_leg": {t: [round(-s * 25.0, 3), 0.0, 3.0] for t, s in walk_frames.items()}
    }
}

# 7. JUMP & FALL
anims['jump'] = {
    "animation_length": 0.8,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [5.0, 0.0, 0.0]}},
        "left_arm": {"rotation": {"0": [-45.0, 0.0, 15.0]}},
        "right_arm": {"rotation": {"0": [-45.0, 0.0, -15.0]}},
        "left_leg": {"rotation": {"0": [20.0, 0.0, -5.0]}},
        "right_leg": {"rotation": {"0": [35.0, 0.0, 5.0]}}
    }
}
anims['jump_start'] = anims['jump']
anims['running_jump'] = anims['jump']
anims['fall'] = {
    "animation_length": 0.8,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [-5.0, 0.0, 0.0]}},
        "left_arm": {"rotation": {"0": [-80.0, 0.0, 25.0]}},
        "right_arm": {"rotation": {"0": [-80.0, 0.0, -25.0]}},
        "left_leg": {"rotation": {"0": [-10.0, 0.0, -5.0]}},
        "right_leg": {"rotation": {"0": [-10.0, 0.0, 5.0]}}
    }
}
anims['land'] = {
    "animation_length": 0.4,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [15.0, 0.0, 0.0], "0.4": [0.0, 0.0, 0.0]}, "position": {"0": [0, -2, 0], "0.4": [0, 0, 0]}},
        "left_leg": {"rotation": {"0": [10.0, 0.0, 0.0], "0.4": [0.0, 0.0, 0.0]}},
        "right_leg": {"rotation": {"0": [10.0, 0.0, 0.0], "0.4": [0.0, 0.0, 0.0]}}
    }
}

# 8. ATTACKS & STRIKES
anims['attack'] = {
    "animation_length": 0.5,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.2": [5, -25, 0], "0.5": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.2": [-90, -20, -10], "0.5": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.2": [20, 15, 10], "0.5": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0], "0.2": [-15, 0, 0], "0.5": [0, 0, 0]}},
        "left_leg": {"rotation": {"0": [0, 0, 0], "0.2": [15, 0, 0], "0.5": [0, 0, 0]}}
    }
}
anims['swing_hand'] = anims['attack']
anims['punch'] = anims['attack']
anims['use_tool'] = anims['attack']
anims['use_item'] = anims['attack']
anims['use_mainhand'] = anims['attack']

anims['cross_punch_right'] = {
    "animation_length": 0.6,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.25": [5, -35, 0], "0.6": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [-20, 0, 0], "0.25": [-95, -15, 0], "0.6": [-20, 0, 0]}},
        "left_arm": {"rotation": {"0": [-60, 20, 20], "0.25": [-70, 30, 25], "0.6": [-60, 20, 20]}}
    }
}
anims['cross_punch_left'] = {
    "animation_length": 0.6,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.25": [5, 35, 0], "0.6": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [-20, 0, 0], "0.25": [-95, 15, 0], "0.6": [-20, 0, 0]}},
        "right_arm": {"rotation": {"0": [-60, -20, -20], "0.25": [-70, -30, -25], "0.6": [-60, -20, -20]}}
    }
}

anims['kick_right'] = {
    "animation_length": 0.7,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.3": [-10, 25, -10], "0.7": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0], "0.3": [90, -10, 15], "0.7": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.3": [-50, 15, 25], "0.7": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.3": [30, -15, -30], "0.7": [0, 0, 0]}}
    }
}
anims['kick_left'] = {
    "animation_length": 0.7,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.3": [-10, -25, 10], "0.7": [0, 0, 0]}},
        "left_leg": {"rotation": {"0": [0, 0, 0], "0.3": [90, 10, -15], "0.7": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.3": [-50, -15, -25], "0.7": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.3": [30, 15, 30], "0.7": [0, 0, 0]}}
    }
}

# 9. GUARD / PARRY / BLOCK
anims['block'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [5, 0, 0]}},
        "left_arm": {"rotation": {"0": [-70, 30, 35]}},
        "right_arm": {"rotation": {"0": [-70, -30, -35]}},
        "left_leg": {"rotation": {"0": [-5, 0, -3]}},
        "right_leg": {"rotation": {"0": [5, 0, 3]}}
    }
}
anims['arm_parry'] = anims['block']
anims['sword_parry'] = anims['block']
anims['axe_parry'] = anims['block']

# 10. WEAPONS (Sword & Axe)
anims['sword_idle'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [2, 0, 0]}},
        "right_arm": {"rotation": {"0": [-30, -10, -10]}},
        "left_arm": {"rotation": {"0": [0, 0, 5]}},
        "left_leg": {"rotation": {"0": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0]}}
    }
}
anims['sword_walk'] = anims['walk']
anims['sword_draw'] = anims['attack']
anims['sword_sheathe'] = anims['attack']
anims['axe_idle'] = anims['sword_idle']
anims['axe_walk'] = anims['walk']
anims['axe_draw'] = anims['attack']
anims['axe_sheathe'] = anims['attack']

# 11. SIT / RIDE / SLEEP / DEATH
anims['sit'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0]}, "position": {"0": [0, -7, 0]}},
        "left_leg": {"rotation": {"0": [80, 0, -5]}},
        "right_leg": {"rotation": {"0": [80, 0, 5]}},
        "left_arm": {"rotation": {"0": [-10, 0, 5]}},
        "right_arm": {"rotation": {"0": [-10, 0, -5]}}
    }
}
anims['ride'] = anims['sit']
anims['riding'] = anims['sit']
anims['ride_idle'] = anims['sit']
anims['sitting_idle'] = anims['sit']
anims['sit_down_to_drive'] = anims['sit']
anims['sitting_eat'] = anims['sit']
anims['get_up_from_sitting'] = anims['idle']
anims['get_up_from_crouch'] = anims['idle']
anims['get_up_from_lying_down'] = anims['idle']

anims['sleep'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [90, 0, 0]}, "position": {"0": [0, -22, 0]}},
        "left_leg": {"rotation": {"0": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0]}}
    }
}
anims['sleep_idle'] = anims['sleep']

anims['death'] = {
    "animation_length": 2.0,
    "loop": "hold_on_last_frame",
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "1.0": [90, 0, 0]}, "position": {"0": [0, 0, 0], "1.0": [0, -20, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "1.0": [40, 0, 20]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "1.0": [40, 0, -20]}}
    }
}

anims['eat'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "head": {"rotation": {"0": [0, 0, 0], "0.5": [10, 0, 0], "1.0": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [-60, -20, -10], "0.5": [-75, -25, -15], "1.0": [-60, -20, -10]}}
    }
}

# 12. EMOTES (extra0 to extra7 & named emotes)
anims['dance'] = {
    "animation_length": 1.2,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [0, 0, -8], "0.3": [0, 0, 8], "0.6": [0, 0, -8], "0.9": [0, 0, 8], "1.2": [0, 0, -8]}, "position": {"0": [0, 0, 0], "0.3": [0, 0.8, 0], "0.6": [0, 0, 0], "0.9": [0, 0.8, 0], "1.2": [0, 0, 0]}},
        "head": {"rotation": {"0": [0, 0, 6], "0.3": [0, 0, -6], "0.6": [0, 0, 6], "0.9": [0, 0, -6], "1.2": [0, 0, 6]}},
        "left_arm": {"rotation": {"0": [-50, 0, 60], "0.3": [-80, 0, 30], "0.6": [-50, 0, 60], "0.9": [-80, 0, 30], "1.2": [-50, 0, 60]}},
        "right_arm": {"rotation": {"0": [-80, 0, -30], "0.3": [-50, 0, -60], "0.6": [-80, 0, -30], "0.9": [-50, 0, -60], "1.2": [-80, 0, -30]}},
        "left_leg": {"rotation": {"0": [0, 0, -6], "0.3": [12, 0, 0], "0.6": [0, 0, -6], "0.9": [12, 0, 0], "1.2": [0, 0, -6]}},
        "right_leg": {"rotation": {"0": [12, 0, 0], "0.3": [0, 0, 6], "0.6": [12, 0, 0], "0.9": [0, 0, 6], "1.2": [12, 0, 0]}}
    }
}
anims['extra0'] = anims['dance']

anims['wave'] = {
    "animation_length": 1.5,
    "loop": True,
    "bones": {
        "head": {"rotation": {"0": [0, 8, 0], "0.75": [0, -8, 0], "1.5": [0, 8, 0]}},
        "right_arm": {"rotation": {"0": [-115, 0, -25], "0.25": [-125, 0, -45], "0.5": [-115, 0, -25], "0.75": [-125, 0, -45], "1.0": [-115, 0, -25], "1.25": [-125, 0, -45], "1.5": [-115, 0, -25]}},
        "left_arm": {"rotation": {"0": [0, 0, 4], "1.5": [0, 0, 4]}}
    }
}
anims['extra1'] = anims['wave']

anims['extra2'] = anims['kick_right']

anims['extra3'] = {
    "animation_length": 0.8,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [4, -12, 0], "0.2": [4, 12, 0], "0.4": [4, -12, 0], "0.6": [4, 12, 0], "0.8": [4, -12, 0]}},
        "left_arm": {"rotation": {"0": [-85, 8, 0], "0.2": [-15, 15, 15], "0.4": [-85, 8, 0], "0.6": [-15, 15, 15], "0.8": [-85, 8, 0]}},
        "right_arm": {"rotation": {"0": [-15, -15, -15], "0.2": [-85, -8, 0], "0.4": [-15, -15, -15], "0.6": [-85, -8, 0], "0.8": [-15, -15, -15]}}
    }
}

anims['extra4'] = anims['block']

anims['shout'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {str(round(i*0.05, 2)): [10.0 + (1.0 if i%2==0 else -1.0), 0.0, 0.0] for i in range(41)}, "position": {"0": [0, -1.2, 0], "1.0": [0, -1.4, 0], "2.0": [0, -1.2, 0]}},
        "head": {"rotation": {"0": [-25, 0, 0], "1.0": [-30, 0, 0], "2.0": [-25, 0, 0]}},
        "left_arm": {"rotation": {"0": [20, 0, 25], "0.5": [25, 0, 30], "1.0": [20, 0, 25], "1.5": [25, 0, 30], "2.0": [20, 0, 25]}},
        "right_arm": {"rotation": {"0": [20, 0, -25], "0.5": [25, 0, -30], "1.0": [20, 0, -25], "1.5": [25, 0, -30], "2.0": [20, 0, -25]}},
        "left_leg": {"rotation": {"0": [-8, 0, -8]}},
        "right_leg": {"rotation": {"0": [-8, 0, 8]}}
    }
}
anims['extra5'] = anims['shout']

anims['talk'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "head": {"rotation": {"0": [0, 4, 0], "0.5": [4, -4, 0], "1.0": [-2, 4, 0], "1.5": [3, 0, 0], "2.0": [0, 4, 0]}},
        "right_arm": {"rotation": {"0": [-25, 0, -10], "0.5": [-40, 8, -18], "1.0": [-15, -4, -8], "1.5": [-35, 4, -15], "2.0": [-25, 0, -10]}},
        "left_arm": {"rotation": {"0": [0, 0, 4], "1.0": [-12, 0, 8], "2.0": [0, 0, 4]}}
    }
}
anims['extra6'] = anims['talk']

anims['zombie_walk'] = {
    "animation_length": 1.2,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [8, 0, 0]}},
        "left_arm": {"rotation": {"0": [-85, 0, 0], "0.6": [-90, 0, 0], "1.2": [-85, 0, 0]}},
        "right_arm": {"rotation": {"0": [-85, 0, 0], "0.6": [-90, 0, 0], "1.2": [-85, 0, 0]}},
        "left_leg": {"rotation": {"0": [20, 0, 0], "0.6": [-20, 0, 0], "1.2": [20, 0, 0]}},
        "right_leg": {"rotation": {"0": [-20, 0, 0], "0.6": [20, 0, 0], "1.2": [-20, 0, 0]}}
    }
}
anims['extra7'] = anims['zombie_walk']

full_anim_data = {
    "format_version": "1.8.0",
    "animations": anims
}

for r in races:
    dst_path = f'Animated/UniversalAnimations/BPM/{r}/animations/main.animation.json'
    with open(dst_path, 'w', encoding='utf-8') as f:
        json.dump(full_anim_data, f, indent=2)

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

# Update BPM jar default main.animation.json
jar_path = 'run/mods/better-player-model-1.1.4-fabric-26.2.jar'
yardrat_dir = 'Animated/UniversalAnimations/BPM/universal_humanoid'
temp_jar = jar_path + '.tmp'

replacement_files = {
    'assets/better_player_model/builtin/default/animations/main.animation.json': os.path.join(yardrat_dir, 'animations', 'main.animation.json'),
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
print('Successfully sanitized all animation clips across all 12 races!')
