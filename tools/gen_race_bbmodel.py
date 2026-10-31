"""Author Blockbench (.bbmodel, Bedrock format) race projects.

The base rig (bones, pivots, cubes, UVs) comes from
better_player_model/builtin/<race>/models/main.json so bone names and pivots stay
identical to the animations. Race-specific geometry is added as extra named cubes.
The written project opens directly in Blockbench (File > Open) and is the source
gen_race_obj.py exports the runtime OBJ from.

Usage: python3 tools/gen_race_bbmodel.py saiyan
"""
import json, sys, uuid, base64, pathlib
REPO = pathlib.Path(__file__).resolve().parent.parent
ASSETS = REPO / 'src/main/resources/assets'
OUT = REPO / 'assets/blockbench/races'
FEATURE_UV = (224, 224)   # skin-tinted shade strip (flagged red in <race>_mask.png)
SHADES = {'light': 0, 'mid': 1, 'dark': 2, 'darkest': 3}

def U(): return str(uuid.uuid4())

def shade_faces(shade):
    px, py = FEATURE_UV[0] + 8 * SHADES[shade] + 1, FEATURE_UV[1] + 1
    return {f: {'uv': [px, py, px + 6, py + 6], 'texture': 0}
            for f in ('north', 'south', 'east', 'west', 'up', 'down')}

# ---- race features, bedrock pixel space (front of body is -z) --------------
# box(bone, name, x0,y0,z0, x1,y1,z1, shade, mirror)
def features(race):
    F = []
    def box(bone, name, a, b, shade, mirror=False):
        F.append((bone, name, a, b, shade))
        if mirror:
            F.append((bone.replace('right', 'left') if 'right' in bone else bone,
                      name.replace('right', 'left'), [-b[0], a[1], a[2]], [-a[0], b[1], b[2]], shade))
    if race == 'saiyan':
        # Reference: Saiyans look like Human-type Earthlings with a muscular,
        # well-defined build, a strong brow, black spiky hair and a brown monkey tail.
        # Hair (DbaHairRenderer) and tail (TrailingTailLayer + tail bones) have their
        # own layers, so the mesh adds the build: pecs, deltoids, abs, traps, brow.
        box('body', 'right_pec', [-3.7, 21.4, -2.55], [-0.15, 25.2, -2.0], 'light', True)
        for i, y in enumerate((18.2, 16.2)):
            box('body', 'right_ab%d' % i, [-2.0, y, -2.35], [-0.2, y + 1.7, -2.0], 'mid', True)
        box('body', 'trap_back', [-3.2, 22.6, 2.0], [3.2, 26.2, 2.5], 'mid')
        box('right_arm', 'right_deltoid', [-7.85, 23.4, -2.35], [-3.85, 26.95, 2.35], 'light', True)
        box('right_arm', 'right_bicep', [-7.1, 19.6, -2.35], [-4.4, 22.8, -2.0], 'light', True)
        box('head', 'brow_ridge', [-3.6, 31.0, -3.45], [3.6, 31.75, -2.9], 'dark')
    return F

def build(race):
    src = ASSETS / f'better_player_model/builtin/{race}/models/main.json'
    g = json.load(open(src))['minecraft:geometry'][0]
    TW, TH = g['description']['texture_width'], g['description']['texture_height']
    elements, groups, children = [], {}, {}
    order = []
    for b in g['bones']:
        p = b.get('pivot', [0, 0, 0]); r = b.get('rotation', [0, 0, 0])
        grp = {'name': b['name'], 'origin': [-p[0], p[1], p[2]], 'rotation': [-r[0], -r[1], r[2]],
               'color': 0, 'uuid': U(), 'export': True, 'mirror_uv': False, 'isOpen': False,
               'locked': False, 'visibility': True, 'autouv': 0, 'children': []}
        groups[b['name']] = (grp, b.get('parent')); order.append(b['name'])
        for c in b.get('cubes', []):
            o, s = c['origin'], c['size']; cr = c.get('rotation', [0, 0, 0]); cp = c.get('pivot', p)
            faces = {}
            for fn, f in c['uv'].items():
                u, v = f['uv']; du, dv = f['uv_size']
                faces[fn] = {'uv': [u, v, u + du, v + dv], 'texture': 0}
            el = {'name': b['name'], 'box_uv': False, 'rescale': False, 'locked': False,
                  'render_order': 'default', 'allow_mirror_modeling': True,
                  'from': [-(o[0] + s[0]), o[1], o[2]], 'to': [-o[0], o[1] + s[1], o[2] + s[2]],
                  'autouv': 0, 'color': 0, 'inflate': c.get('inflate', 0),
                  'rotation': [-cr[0], -cr[1], cr[2]], 'origin': [-cp[0], cp[1], cp[2]],
                  'faces': faces, 'type': 'cube', 'uuid': U()}
            elements.append(el); grp['children'].append(el['uuid'])
        for lname, lpos in (b.get('locators') or {}).items():
            loc = {'name': lname, 'position': [-lpos[0], lpos[1], lpos[2]], 'rotation': [0, 0, 0],
                   'ignore_inherited_scale': False, 'type': 'locator', 'uuid': U()}
            elements.append(loc); grp['children'].append(loc['uuid'])
    for bone, name, a, b2, shade in features(race):
        grp = groups[bone][0]
        el = {'name': name, 'box_uv': False, 'rescale': False, 'locked': False,
              'render_order': 'default', 'allow_mirror_modeling': True,
              'from': [-b2[0], a[1], a[2]], 'to': [-a[0], b2[1], b2[2]], 'autouv': 0, 'color': 3,
              'inflate': 0, 'rotation': [0, 0, 0], 'origin': list(grp['origin']),
              'faces': shade_faces(shade), 'type': 'cube', 'uuid': U()}
        elements.append(el); grp['children'].append(el['uuid'])
    roots = []
    for n in order:
        grp, parent = groups[n]
        (groups[parent][0]['children'] if parent else roots).append(grp)
    tex = ASSETS / f'dragonblockarcanedba/textures/entity/player/{race}_base.png'
    model = {
        'meta': {'format_version': '4.10', 'model_format': 'bedrock', 'box_uv': False},
        'name': race, 'model_identifier': g['description']['identifier'].replace('geometry.', ''),
        'visible_box': [g['description'].get('visible_bounds_width', 3), g['description'].get('visible_bounds_height', 4),
                        g['description'].get('visible_bounds_offset', [0, 1.5, 0])[1]],
        'variable_placeholders': '', 'variable_placeholder_buttons': [], 'timeline_setups': [],
        'unhandled_root_fields': {}, 'resolution': {'width': TW, 'height': TH},
        'elements': elements, 'outliner': roots,
        'textures': [{'path': '', 'name': f'{race}_base.png', 'folder': 'entity/player',
                      'namespace': 'dragonblockarcanedba', 'id': '0', 'width': TW, 'height': TH,
                      'uv_width': TW, 'uv_height': TH, 'particle': False, 'use_as_default': True,
                      'render_mode': 'default', 'render_sides': 'auto', 'frame_time': 1,
                      'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False,
                      'visible': True, 'internal': True, 'saved': True, 'uuid': U(),
                      'source': 'data:image/png;base64,' + base64.b64encode(tex.read_bytes()).decode()}],
    }
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f'{race}.bbmodel').write_text(json.dumps(model, indent=1))
    print('wrote', OUT / f'{race}.bbmodel', len(elements), 'elements')

if __name__ == '__main__':
    for r in sys.argv[1:]: build(r)
