import json, sys, os
import pathlib
ROOT=pathlib.Path(__file__).resolve().parent.parent/'src/main/resources/assets'
B=str(ROOT/'better_player_model/builtin')+'/'
O=str(ROOT/'dragonblockarcanedba/models/3d')+'/'
GROUP={'body':'body','head':'head','left_arm':'leftarm','right_arm':'rightarm','left_leg':'leftleg','right_leg':'rightleg'}
S=1/16.0
def gen(race, out):
    g=json.load(open(B+race+'/models/main.json'))['minecraft:geometry'][0]
    TW,TH=g['description']['texture_width'],g['description']['texture_height']
    bones={b['name']:b for b in g['bones']}
    def grp(n):
        while n:
            if n in GROUP: return GROUP[n]
            n=bones[n].get('parent')
        return None
    groups={}
    for b in g['bones']:
        if b['name'] in GROUP:
            gn=grp(b['name'])
        else: continue  # skip eyelids, tail, locators (rendered by dedicated layers)
        if b.get('rotation') and any(b['rotation']): print('warn rotated bone',race,b['name'])
        for c in b.get('cubes',[]):
            if c.get('rotation') and any(c['rotation']): print('warn rotated cube',race,b['name'])
            groups.setdefault(gn,[]).append(c)
    L=['# generated from better_player_model/builtin/%s/models/main.json by gen_race_obj.py'%race]
    vi=1; ti=1; nvi=[1]
    for gn in ['rightarm','body','leftarm','rightleg','head','leftleg']:
        if gn not in groups: continue
        L.append('o '+gn)
        for c in groups[gn]:
            x0,y0,z0=c['origin']; sx,sy,sz=c['size']; x1,y1,z1=x0+sx,y0+sy,z0+sz
            F={'north':[(x1,y1,z0),(x0,y1,z0),(x0,y0,z0),(x1,y0,z0)],
               'south':[(x0,y1,z1),(x1,y1,z1),(x1,y0,z1),(x0,y0,z1)],
               'east':[(x1,y1,z1),(x1,y1,z0),(x1,y0,z0),(x1,y0,z1)],
               'west':[(x0,y1,z0),(x0,y1,z1),(x0,y0,z1),(x0,y0,z0)],
               'up':[(x1,y1,z0),(x0,y1,z0),(x0,y1,z1),(x1,y1,z1)],
               'down':[(x1,y0,z1),(x0,y0,z1),(x0,y0,z0),(x1,y0,z0)]}
            for fn,q in F.items():
                f=c.get('uv',{}).get(fn)
                if not f: continue
                u,v=f['uv']; du,dv=f['uv_size']
                uvs=[(u,v),(u+du,v),(u+du,v+dv),(u,v+dv)]
                # mirror z (bedrock front is -z, OBJ front is +z) and reverse order to keep outward winding
                q=[(p[0]*S,p[1]*S,-p[2]*S) for p in q][::-1]; uvs=uvs[::-1]
                N={'north':(0,0,1),'south':(0,0,-1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}[fn]
                L.append('vn %d %d %d'%N); ni=nvi[0]; nvi[0]+=1
                idx=[]
                for p,t in zip(q,uvs):
                    L.append('v %.6f %.6f %.6f'%p); L.append('vt %.6f %.6f'%(t[0]/TW,1-t[1]/TH))
                    idx.append('%d/%d/%d'%(vi,ti,ni)); vi+=1; ti+=1
                L.append('f '+' '.join(idx))
    open(out,'w').write('\n'.join(L)+'\n')
for r in sys.argv[1:]: gen(r,O+r+'.obj')
