import json, os, math, shutil

races = [
    'android', 'arcosian', 'bio_android', 'half_saiyan', 'human',
    'majin', 'namekian', 'neo_tuffle', 'saiyan', 'tuffle',
    'universal_humanoid', 'yardrat'
]

anims = {}

# 1. EPIC IDLE (DBZ Martial Arts Stance)
idle_frames = [round(i * 0.05, 2) for i in range(41)]
anims['idle'] = {
    "animation_length": 2.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [round(2.0 + math.sin(t * math.pi) * 1.5, 3), 0.0, 0.0] for t in idle_frames},
            "position": {str(t): [0.0, round(math.sin(t * math.pi) * 0.2, 3), 0.0] for t in idle_frames}
        },
        "head": {
            "rotation": {str(t): [round(-1.5 - math.sin(t * math.pi) * 1.0, 3), 0.0, 0.0] for t in idle_frames}
        },
        "left_arm": {
            "rotation": {str(t): [round(-12.0 + math.sin(t * math.pi) * 2.5, 3), 5.0, 8.0] for t in idle_frames}
        },
        "right_arm": {
            "rotation": {str(t): [round(-12.0 + math.sin(t * math.pi) * 2.5, 3), -5.0, -8.0] for t in idle_frames}
        },
        "left_leg": {
            "rotation": {"0": [-2.0, 0.0, -2.0]}
        },
        "right_leg": {
            "rotation": {"0": [2.0, 0.0, 2.0]}
        },
        "tail": {
            "rotation": {str(t): [-22.0, round(math.sin(t * 2 * math.pi) * 10.0, 3), round(math.cos(t * 2 * math.pi) * 4.0, 3)] for t in idle_frames}
        },
        "tail1": {
            "rotation": {str(t): [-12.0, round(math.sin(t * 2 * math.pi + 0.6) * 14.0, 3), 0.0] for t in idle_frames}
        }
    }
}
anims['item_idle'] = anims['idle']

# 2. EPIC WALK (Confident Martial Arts Stride)
walk_frames = [round(i * 0.04, 2) for i in range(26)]
anims['walk'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [3.0, round(math.sin(t * 2 * math.pi) * 3.0, 3), 0.0] for t in walk_frames},
            "position": {str(t): [0.0, round(abs(math.sin(t * 2 * math.pi)) * 0.35, 3), 0.0] for t in walk_frames}
        },
        "head": {
            "rotation": {str(t): [-2.0, round(-math.sin(t * 2 * math.pi) * 2.0, 3), 0.0] for t in walk_frames}
        },
        "left_arm": {
            "rotation": {str(t): [round(-math.sin(t * 2 * math.pi) * 30.0, 3), 0.0, 5.0] for t in walk_frames}
        },
        "right_arm": {
            "rotation": {str(t): [round(math.sin(t * 2 * math.pi) * 30.0, 3), 0.0, -5.0] for t in walk_frames}
        },
        "left_leg": {
            "rotation": {str(t): [round(math.sin(t * 2 * math.pi) * 34.0, 3), 0.0, 0.0] for t in walk_frames}
        },
        "right_leg": {
            "rotation": {str(t): [round(-math.sin(t * 2 * math.pi) * 34.0, 3), 0.0, 0.0] for t in walk_frames}
        },
        "tail": {
            "rotation": {str(t): [-25.0, round(math.sin(t * 2 * math.pi) * 16.0, 3), 0.0] for t in walk_frames}
        }
    }
}
anims['walk_back'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0]}},
        "left_arm": {str(t): [round(math.sin(t * 2 * math.pi) * 22.0, 3), 0.0, 5.0] for t in walk_frames},
        "right_arm": {str(t): [round(-math.sin(t * 2 * math.pi) * 22.0, 3), 0.0, -5.0] for t in walk_frames},
        "left_leg": {str(t): [round(-math.sin(t * 2 * math.pi) * 28.0, 3), 0.0, 0.0] for t in walk_frames},
        "right_leg": {str(t): [round(math.sin(t * 2 * math.pi) * 28.0, 3), 0.0, 0.0] for t in walk_frames}
    }
}

# 3. EPIC RUN (DBZ High-Speed Saiyan Dash)
run_frames = [round(i * 0.025, 3) for i in range(21)]
anims['run'] = {
    "animation_length": 0.5,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [18.0, round(math.sin(t * 4 * math.pi) * 4.5, 3), 0.0] for t in run_frames},
            "position": {str(t): [0.0, round(abs(math.sin(t * 4 * math.pi)) * 0.6, 3), 0.0] for t in run_frames}
        },
        "head": {
            "rotation": {str(t): [-14.0, round(-math.sin(t * 4 * math.pi) * 2.5, 3), 0.0] for t in run_frames}
        },
        "left_arm": {
            "rotation": {str(t): [round(-math.sin(t * 4 * math.pi) * 60.0, 3), 0.0, 10.0] for t in run_frames}
        },
        "right_arm": {
            "rotation": {str(t): [round(math.sin(t * 4 * math.pi) * 60.0, 3), 0.0, -10.0] for t in run_frames}
        },
        "left_leg": {
            "rotation": {str(t): [round(math.sin(t * 4 * math.pi) * 65.0, 3), 0.0, 0.0] for t in run_frames}
        },
        "right_leg": {
            "rotation": {str(t): [round(-math.sin(t * 4 * math.pi) * 65.0, 3), 0.0, 0.0] for t in run_frames}
        },
        "tail": {
            "rotation": {str(t): [-35.0, round(math.sin(t * 4 * math.pi) * 22.0, 3), 0.0] for t in run_frames}
        }
    }
}

# 4. EPIC FLY (Authentic DBZ Ki Flight)
fly_frames = [round(i * 0.05, 2) for i in range(21)]
anims['fly'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [45.0 + math.sin(t * 2 * math.pi) * 2.0, 0.0, 0.0] for t in fly_frames},
            "position": {str(t): [0.0, math.sin(t * 2 * math.pi) * 0.3, 0.0] for t in fly_frames}
        },
        "head": {
            "rotation": {str(t): [-38.0 - math.sin(t * 2 * math.pi) * 1.5, 0.0, 0.0] for t in fly_frames}
        },
        "left_arm": {
            "rotation": {str(t): [40.0 + math.sin(t * 2 * math.pi) * 3.0, 0.0, 14.0] for t in fly_frames}
        },
        "right_arm": {
            "rotation": {str(t): [40.0 + math.sin(t * 2 * math.pi) * 3.0, 0.0, -14.0] for t in fly_frames}
        },
        "left_leg": {
            "rotation": {str(t): [-12.0 + math.sin(t * 2 * math.pi) * 2.0, 0.0, -3.0] for t in fly_frames}
        },
        "right_leg": {
            "rotation": {str(t): [-12.0 - math.sin(t * 2 * math.pi) * 2.0, 0.0, 3.0] for t in fly_frames}
        },
        "tail": {
            "rotation": {str(t): [-50.0, math.sin(t * 2 * math.pi) * 8.0, 0.0] for t in fly_frames}
        }
    }
}

# 5. EPIC FLURRY PUNCH RUSH (extra3 - Dragon Ball Rapid Fire Fists)
flurry_frames = [round(i * 0.025, 3) for i in range(25)]
anims['extra3'] = {
    "animation_length": 0.6,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [6.0, round(math.sin(t * 10 * math.pi) * 14.0, 3), 0.0] for t in flurry_frames},
            "position": {str(t): [0.0, round(abs(math.sin(t * 10 * math.pi)) * 0.15, 3), 0.0] for t in flurry_frames}
        },
        "left_arm": {
            "rotation": {str(t): [round(-75.0 + math.sin(t * 10 * math.pi) * 45.0, 3), 10.0, 0.0] for t in flurry_frames}
        },
        "right_arm": {
            "rotation": {str(t): [round(-75.0 - math.sin(t * 10 * math.pi) * 45.0, 3), -10.0, 0.0] for t in flurry_frames}
        },
        "left_leg": {"rotation": {"0": [-10, 0, -5]}},
        "right_leg": {"rotation": {"0": [10, 0, 5]}}
    }
}

# 6. EPIC POWER-UP CHARGE SHOUT (extra5 & shout - Super Saiyan Ki Blast)
charge_frames = [round(i * 0.025, 3) for i in range(41)]
anims['shout'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {
            "rotation": {str(t): [12.0 + (1.2 if int(t*40)%2==0 else -1.2), 0.0, 0.0] for t in charge_frames},
            "position": {str(t): [0.0, -1.8 + (0.1 if int(t*40)%2==0 else -0.1), 0.0] for t in charge_frames}
        },
        "head": {"rotation": {"0": [-28, 0, 0], "0.5": [-32, 0, 0], "1.0": [-28, 0, 0]}},
        "left_arm": {"rotation": {"0": [25, 0, 30], "0.5": [30, 0, 35], "1.0": [25, 0, 30]}},
        "right_arm": {"rotation": {"0": [25, 0, -30], "0.5": [30, 0, -35], "1.0": [25, 0, -30]}},
        "left_leg": {"rotation": {"0": [-12, 0, -12]}},
        "right_leg": {"rotation": {"0": [-12, 0, 12]}}
    }
}
anims['extra5'] = anims['shout']

# 7. EPIC CROSS GUARD (extra4 & block)
anims['block'] = {
    "animation_length": 1.0,
    "loop": True,
    "bones": {
        "body": {"rotation": {"0": [6, 0, 0]}},
        "left_arm": {"rotation": {"0": [-75, 30, 40]}},
        "right_arm": {"rotation": {"0": [-75, -30, -40]}},
        "left_leg": {"rotation": {"0": [-6, 0, -5]}},
        "right_leg": {"rotation": {"0": [8, 0, 5]}}
    }
}
anims['arm_parry'] = anims['block']
anims['sword_parry'] = anims['block']
anims['axe_parry'] = anims['block']
anims['extra4'] = anims['block']

# 8. EPIC MARTIAL ARTS STRIKES
anims['attack'] = {
    "animation_length": 0.4,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.15": [4, -28, 0], "0.4": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.15": [-95, -15, -10], "0.4": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.15": [20, 15, 10], "0.4": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0], "0.15": [-15, 0, 0], "0.4": [0, 0, 0]}},
        "left_leg": {"rotation": {"0": [0, 0, 0], "0.15": [15, 0, 0], "0.4": [0, 0, 0]}}
    }
}
anims['swing_hand'] = anims['attack']
anims['punch'] = anims['attack']
anims['use_tool'] = anims['attack']
anims['use_item'] = anims['attack']
anims['use_mainhand'] = anims['attack']

anims['cross_punch_right'] = {
    "animation_length": 0.5,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.2": [5, -40, 0], "0.5": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [-20, 0, 0], "0.2": [-100, -15, 0], "0.5": [-20, 0, 0]}},
        "left_arm": {"rotation": {"0": [-55, 20, 20], "0.2": [-70, 30, 25], "0.5": [-55, 20, 20]}}
    }
}
anims['cross_punch_left'] = {
    "animation_length": 0.5,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.2": [5, 40, 0], "0.5": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [-20, 0, 0], "0.2": [-100, 15, 0], "0.5": [-20, 0, 0]}},
        "right_arm": {"rotation": {"0": [-55, -20, -20], "0.2": [-70, -30, -25], "0.5": [-55, -20, -20]}}
    }
}

anims['kick_right'] = {
    "animation_length": 0.6,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.25": [-10, 30, -10], "0.6": [0, 0, 0]}},
        "right_leg": {"rotation": {"0": [0, 0, 0], "0.25": [95, -10, 15], "0.6": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.25": [-55, 15, 25], "0.6": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.25": [30, -15, -30], "0.6": [0, 0, 0]}}
    }
}
anims['extra2'] = anims['kick_right']

anims['kick_left'] = {
    "animation_length": 0.6,
    "loop": False,
    "bones": {
        "body": {"rotation": {"0": [0, 0, 0], "0.25": [-10, -30, 10], "0.6": [0, 0, 0]}},
        "left_leg": {"rotation": {"0": [0, 0, 0], "0.25": [95, 10, -15], "0.6": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.25": [-55, -15, -25], "0.6": [0, 0, 0]}},
        "left_arm": {"rotation": {"0": [0, 0, 0], "0.25": [30, 15, 30], "0.6": [0, 0, 0]}}
    }
}

# 9. EMOTES
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

# 10. SNEAK, JUMP, FALL, SIT, SWIM
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
anims['sneak_walk'] = anims['sneak_idle']

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

full_anim_data = {
    "format_version": "1.8.0",
    "animations": anims
}

for r in races:
    dst_path = f'Animated/UniversalAnimations/BPM/{r}/animations/main.animation.json'
    with open(dst_path, 'w', encoding='utf-8') as f:
        json.dump(full_anim_data, f, indent=2)

    builtin_dst = f'src/main/resources/assets/better_player_model/builtin/{r}'
    custom_dst = f'run/config/better_player_model/custom/{r}'
    src_dir = f'Animated/UniversalAnimations/BPM/{r}'
    
    if os.path.exists(builtin_dst):
        shutil.rmtree(builtin_dst)
    shutil.copytree(src_dir, builtin_dst)
    
    if os.path.exists(custom_dst):
        shutil.rmtree(custom_dst)
    shutil.copytree(src_dir, custom_dst)

print('Successfully created epic DBZ animations across all 12 races!')
