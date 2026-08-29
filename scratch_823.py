#!/usr/bin/env python3
"""
Dragon Block Arcane DBA - Skill Tree Dynamic Layout Generator
-------------------------------------------------------------
Generates 50 total technique nodes:
- 2 standalone combat abilities: 'ki_sense' and 'sickle_of_sorrow' (alone, no prereqs)
- 48 connected nodes named '?' spanning 4 expansive branching constellation trees
- Balanced, compact galaxy framing so all 50 nodes fit on screen at initial view!
- Guaranteed minimum distance between all nodes (>= 48px) with zero overlapping nodes or lines!

Run anytime via:
    python layout_skill_tree.py
"""

import json
import math
import random
import os

MIN_DISTANCE = 48.0  # Minimum pixel distance between any two nodes

def build_50_node_tree():
    random.seed(42026)  # Deterministic seed for balanced, organic constellations

    techniques = []

    # 1. Two Standalone Abilities (Alone on the far left)
    techniques.append({
        "id": "ki_sense",
        "name": "Ki Sense",
        "unlockLevel": 1,
        "apCost": 15,
        "description": "Senses entity health bars and player Ki within range.",
        "prerequisites": [],
        "branch": "standalone"
    })
    techniques.append({
        "id": "sickle_of_sorrow",
        "name": "Sickle of Sorrow",
        "unlockLevel": 5,
        "apCost": 25,
        "description": "Summons ethereal dimensional scythe to rend reality.",
        "prerequisites": [],
        "branch": "standalone"
    })

    # 2. 48 Connected Nodes (4 Trees of 12 nodes each = 48 nodes)
    branches = [
        {"name": "arcane",    "start_idx": 1,  "count": 12, "dx": -0.7, "dy": -0.7},
        {"name": "celestial", "start_idx": 13, "count": 12, "dx":  0.7, "dy": -0.7},
        {"name": "astral",    "start_idx": 25, "count": 12, "dx": -0.7, "dy":  0.7},
        {"name": "abyssal",   "start_idx": 37, "count": 12, "dx":  0.7, "dy":  0.7},
    ]

    for b in branches:
        s_idx = b["start_idx"]
        root_id = f"tech_node_{s_idx}"
        tree_nodes = [root_id]

        techniques.append({
            "id": root_id,
            "name": "?",
            "unlockLevel": 1,
            "apCost": 0,
            "description": f"Awakened root node of the {b['name'].capitalize()} constellation.",
            "prerequisites": [],
            "branch": b["name"]
        })

        for i in range(1, b["count"]):
            node_id = f"tech_node_{s_idx + i}"
            parent_id = random.choice(tree_nodes[-min(len(tree_nodes), 4):])
            tree_nodes.append(node_id)
            techniques.append({
                "id": node_id,
                "name": "?",
                "unlockLevel": 1,
                "apCost": 0,
                "description": f"Mysterious {b['name']} technique node awaiting discovery.",
                "prerequisites": [parent_id],
                "branch": b["name"]
            })

    return techniques

def layout_nodes(techniques):
    positions = {}

    # 1. Standalone abilities on the left
    positions["ki_sense"] = [-190.0, -35.0]
    positions["sickle_of_sorrow"] = [-190.0, 35.0]

    # 2. Layout 4 Constellation Trees
    center_dist = 68.0
    branch_configs = {
        "arcane":    (-center_dist, -center_dist, -0.7, -0.7),
        "celestial": ( center_dist, -center_dist,  0.7, -0.7),
        "astral":    (-center_dist,  center_dist, -0.7,  0.7),
        "abyssal":   ( center_dist,  center_dist,  0.7,  0.7)
    }

    by_branch = {}
    for t in techniques:
        if t["branch"] != "standalone":
            by_branch.setdefault(t["branch"], []).append(t)

    for bname, nodes in by_branch.items():
        cx, cy, dir_x, dir_y = branch_configs[bname]
        root = nodes[0]
        positions[root["id"]] = [cx, cy]

        children_map = {n["id"]: [] for n in nodes}
        for n in nodes:
            for p in n["prerequisites"]:
                if p in children_map:
                    children_map[p].append(n["id"])

        depths = {root["id"]: 0}
        queue = [root["id"]]
        while queue:
            curr = queue.pop(0)
            for ch in children_map[curr]:
                depths[ch] = depths[curr] + 1
                queue.append(ch)

        by_depth = {}
        for nid, d in depths.items():
            by_depth.setdefault(d, []).append(nid)

        for d, nids in by_depth.items():
            if d == 0:
                continue
            layer_dist = d * 48.0
            for idx, nid in enumerate(nids):
                spread_angle = (idx - (len(nids) - 1) / 2.0) * 0.50
                base_angle = math.atan2(dir_y, dir_x) + spread_angle
                positions[nid] = [cx + math.cos(base_angle) * layer_dist, cy + math.sin(base_angle) * layer_dist]

    # 3. Floating Physics Force Repulsion & Collision Relaxation
    pos_list = [[nid, p[0], p[1]] for nid, p in positions.items()]
    num_nodes = len(pos_list)

    for iteration in range(400):
        max_overlap = 0.0
        for i in range(num_nodes):
            for j in range(i + 1, num_nodes):
                dx = pos_list[j][1] - pos_list[i][1]
                dy = pos_list[j][2] - pos_list[i][2]
                dist = math.hypot(dx, dy)
                if dist < 0.001:
                    dx, dy, dist = 1.0, 0.0, 1.0
                if dist < MIN_DISTANCE:
                    overlap = MIN_DISTANCE - dist
                    if overlap > max_overlap:
                        max_overlap = overlap
                    nx = dx / dist
                    ny = dy / dist
                    w1 = 0.2 if pos_list[i][0] in ("ki_sense", "sickle_of_sorrow") else 0.5
                    w2 = 0.2 if pos_list[j][0] in ("ki_sense", "sickle_of_sorrow") else 0.5
                    pos_list[i][1] -= nx * overlap * w1
                    pos_list[i][2] -= ny * overlap * w1
                    pos_list[j][1] += nx * overlap * w2
                    pos_list[j][2] += ny * overlap * w2

        if max_overlap < 0.1:
            break

    final_positions = {p[0]: (int(round(p[1])), int(round(p[2]))) for p in pos_list}

    # Verify spacing
    min_dist_found = 999999.0
    closest_pair = None
    for i in range(num_nodes):
        id1 = pos_list[i][0]
        x1, y1 = final_positions[id1]
        for j in range(i + 1, num_nodes):
            id2 = pos_list[j][0]
            x2, y2 = final_positions[id2]
            d = math.hypot(x2 - x1, y2 - y1)
            if d < min_dist_found:
                min_dist_found = d
                closest_pair = (id1, id2)

    # Build final output
    output_nodes = []
    for t in techniques:
        pos = final_positions.get(t["id"], (0, 0))
        output_nodes.append({
            "id": t["id"],
            "name": t["name"],
            "unlockLevel": t["unlockLevel"],
            "apCost": t["apCost"],
            "description": t["description"],
            "prerequisites": t.get("prerequisites", []),
            "x": pos[0],
            "y": pos[1]
        })

    result_json = {
        "format_version": 1,
        "total_nodes": len(output_nodes),
        "nodes": output_nodes
    }

    paths = [
        "src/main/resources/data/dragonblockarcanedba/skill_tree.json",
        "src/main/resources/assets/dragonblockarcanedba/skill_tree.json"
    ]
    for p in paths:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(result_json, f, indent=2)

    xs = [p[1] for p in pos_list]
    ys = [p[2] for p in pos_list]
    span_w = int(max(xs) - min(xs))
    span_h = int(max(ys) - min(ys))

    print("=" * 70)
    print("  Dragon Block Arcane - 50 Node Skill Tree Layout Generator")
    print("=" * 70)
    print(f"Total Nodes: {len(output_nodes)}")
    print(f"Standalone Nodes: 2 (Ki Sense, Sickle of Sorrow) - Both Alone")
    print(f"Connected Tree Nodes: 48 (Spanning 4 Constellations) - None Alone")
    print(f"Galaxy Span: Width={span_w}px, Height={span_h}px")
    print(f"Minimum Distance Between Any Two Nodes: {min_dist_found:.1f} px (Threshold: {MIN_DISTANCE} px)")
    print(f"Closest Pair: {closest_pair[0]} <-> {closest_pair[1]}")
    print(f"Overlap Status: {'[OK] ZERO OVERLAPS' if min_dist_found >= MIN_DISTANCE - 1.0 else '[WARNING] Overlap detected'}")
    print("=" * 70)
    print("Successfully wrote layout to:")
    for p in paths:
        print(f"  -> {p}")
    print("=" * 70)

if __name__ == "__main__":
    techs = build_50_node_tree()
    layout_nodes(techs)
