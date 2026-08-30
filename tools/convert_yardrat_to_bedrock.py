import json
import os
import numpy as np

def get_face_normal(verts, vkeys):
    p0 = np.array(verts[vkeys[0]])
    p1 = np.array(verts[vkeys[1]])
    p2 = np.array(verts[vkeys[2]])
    n = np.cross(p1 - p0, p2 - p0)
    norm = np.linalg.norm(n)
    return n / norm if norm > 1e-6 else np.array([0, 0, 0])

def classify_dir(n):
    dirs = {
        'north': np.array([0, 0, -1]),
        'south': np.array([0, 0, 1]),
        'west':  np.array([-1, 0, 0]),
        'east':  np.array([1, 0, 0]),
        'up':    np.array([0, 1, 0]),
        'down':  np.array([0, -1, 0]),
    }
    best_d = None
    best_dot = -999
    for dname, dvec in dirs.items():
        dot = np.dot(n, dvec)
        if dot > best_dot:
            best_dot = dot
            best_d = dname
    return best_d

def convert_yardrat():
    bbmodel_path = r'BlockBench Models\yardrat.bbmodel'
    with open(bbmodel_path, 'r', encoding='utf-8') as f:
        bb = json.load(f)

    # Scaling and translation factors
    SCALE = 12.0 / 43.57
    CENTER_X = -3.325
    CENTER_Z = 1.665
    FLOOR_Y = 12.67963

    def tr_x(x): return round((x - CENTER_X) * SCALE, 3)
    def tr_y(y): return round((y - FLOOR_Y) * SCALE, 3)
    def tr_z(z): return round((z - CENTER_Z) * SCALE, 3)

    bones = [
        {
            "name": "root",
            "pivot": [0, 0, 0]
        },
        {
            "name": "body",
            "parent": "root",
            "pivot": [0, 24, 0],
            "cubes": []
        },
        {
            "name": "head",
            "parent": "body",
            "pivot": [0, 24, 0],
            "cubes": []
        },
        {
            "name": "left_arm",
            "parent": "body",
            "pivot": [5, 22, 0],
            "cubes": []
        },
        {
            "name": "leftitem",
            "parent": "left_arm",
            "pivot": [6, 15, 1],
            "locators": { "lead": [6, 15, 1] }
        },
        {
            "name": "LeftHandLocator",
            "parent": "left_arm",
            "pivot": [6, 15, 1]
        },
        {
            "name": "right_arm",
            "parent": "body",
            "pivot": [-5, 22, 0],
            "cubes": []
        },
        {
            "name": "rightitem",
            "parent": "right_arm",
            "pivot": [-6, 15, 1],
            "locators": { "lead": [-6, 15, 1] }
        },
        {
            "name": "RightHandLocator",
            "parent": "right_arm",
            "pivot": [-6, 15, 1]
        },
        {
            "name": "left_leg",
            "parent": "root",
            "pivot": [1.9, 12, 0],
            "cubes": []
        },
        {
            "name": "right_leg",
            "parent": "root",
            "pivot": [-1.9, 12, 0],
            "cubes": []
        }
    ]

    bone_map = {b['name']: b for b in bones}

    # Extract cubes for each element
    for el in bb['elements']:
        name = el['name']
        verts = el['vertices']
        faces = el['faces']

        target_bone_name = {
            'rightarm': 'right_arm',
            'leftarm': 'left_arm',
            'rightleg': 'right_leg',
            'leftleg': 'left_leg',
            'body': 'body',
            'head': 'head'
        }.get(name)

        if not target_bone_name:
            continue

        target_bone = bone_map[target_bone_name]

        # Group faces into components/cubes
        from collections import defaultdict
        adj = defaultdict(set)
        for fid, f in faces.items():
            for vid in f['vertices']:
                adj[vid].add(fid)

        components = []
        visited = set()
        for fid in faces.keys():
            if fid in visited: continue
            comp = set()
            queue = [fid]
            visited.add(fid)
            while queue:
                curr = queue.pop(0)
                comp.add(curr)
                for vid in faces[curr]['vertices']:
                    for nbr in adj[vid]:
                        if nbr not in visited:
                            visited.add(nbr)
                            queue.append(nbr)
            components.append(comp)

        for comp in components:
            c_verts = set()
            for fid in comp:
                c_verts.update(faces[fid]['vertices'])

            coords = [verts[v] for v in c_verts]
            xs = [tr_x(c[0]) for c in coords]
            ys = [tr_y(c[1]) for c in coords]
            zs = [tr_z(c[2]) for c in coords]

            min_x, max_x = min(xs), max(xs)
            min_y, max_y = min(ys), max(ys)
            min_z, max_z = min(zs), max(zs)

            size_x = max(0.01, round(max_x - min_x, 3))
            size_y = max(0.01, round(max_y - min_y, 3))
            size_z = max(0.01, round(max_z - min_z, 3))

            cube_uv = {}
            for fid in comp:
                f = faces[fid]
                n = get_face_normal(verts, f['vertices'])
                d = classify_dir(n)
                uvs = list(f['uv'].values())
                us = [p[0] for p in uvs]
                vs = [p[1] for p in uvs]
                cube_uv[d] = {
                    "uv": [round(min(us), 1), round(min(vs), 1)],
                    "uv_size": [round(max(us) - min(us), 1), round(max(vs) - min(vs), 1)]
                }

            # Fill in any missing directions with dummy uv
            for d in ['north', 'south', 'east', 'west', 'up', 'down']:
                if d not in cube_uv:
                    cube_uv[d] = {"uv": [0, 0], "uv_size": [1, 1]}

            target_bone['cubes'].append({
                "origin": [min_x, min_y, min_z],
                "size": [size_x, size_y, size_z],
                "uv": cube_uv
            })

    output_geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": "geometry.universal_humanoid",
                    "texture_width": 256,
                    "texture_height": 256,
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0]
                },
                "bones": bones
            }
        ]
    }

    out_main = r'Animated\UniversalAnimations\BPM\universal_humanoid\models\main.json'
    os.makedirs(os.path.dirname(out_main), exist_ok=True)
    with open(out_main, 'w', encoding='utf-8') as f:
        json.dump(output_geo, f, indent=2)

    print(f"Successfully exported Yardrat geometry to: {out_main}")

    # Copy texture.png to textures/default.png
    import shutil
    src_tex = r'BlockBench Models\texture.png'
    dst_tex = r'Animated\UniversalAnimations\BPM\universal_humanoid\textures\default.png'
    shutil.copyfile(src_tex, dst_tex)
    print(f"Successfully copied Yardrat texture to: {dst_tex}")

if __name__ == '__main__':
    convert_yardrat()
