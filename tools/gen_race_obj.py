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
        if gn=='head':
            for seg in race_features(race):
                vi=emit_segment(L,seg,vi,nvi)
            ti=vi
    open(out,'w').write('\n'.join(L)+'\n')

# ---- race-specific head features -------------------------------------------
# Features are tapered boxes chained along a 3D polyline, in bedrock pixel space
# (front of the face is -z). They are appended to the head group so they follow
# head rotation. Every face samples an 8x8 shade patch painted into an unused
# corner of the race atlas (FEATURE_UV) that is flagged as skin in the race mask,
# so the features are tinted with the player's skin color like the rest of the body.
import math
FEATURE_UV=(224,224)   # top-left of the 32x8 shade strip in the 256px atlas
SHADES={'light':0,'mid':1,'dark':2,'darkest':3}
def _sub(a,b): return [a[i]-b[i] for i in range(3)]
def _add(a,b): return [a[i]+b[i] for i in range(3)]
def _mul(a,k): return [x*k for x in a]
def _cross(a,b): return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def _norm(a):
    l=math.sqrt(sum(x*x for x in a)) or 1.0
    return [x/l for x in a]
def chain(points,t0,t1,shade0,shade1,overlap=0.35):
    """Tapered segments along points, thickness t0 -> t1, shade switches at the last segment."""
    segs=[]; n=len(points)-1
    for i in range(n):
        a,b=points[i],points[i+1]
        d=_norm(_sub(b,a))
        a=_sub(a,_mul(d,overlap)); b=_add(b,_mul(d,overlap))
        ta=t0+(t1-t0)*i/n; tb=t0+(t1-t0)*(i+1)/n
        segs.append((a,b,ta,tb,shade1 if i==n-1 else shade0))
    return segs
def mirror_x(segs):
    return [([-a[0],a[1],a[2]],[-b[0],b[1],b[2]],ta,tb,sh) for a,b,ta,tb,sh in segs]
def bulb(c,r,shade):
    return [([c[0],c[1]-r,c[2]],[c[0],c[1]+r,c[2]],2*r,2*r,shade)]
def race_features(race):
    if race=='arcosian':
        # Frieza-style horns: out of the sides of the skull, sweeping outward then curving up and slightly back
        pts=[[4.2,34.6,-0.6],[6.2,35.0,-0.4],[7.6,35.8,0.0],[8.4,37.2,0.4],[8.6,38.9,0.9],[8.2,40.4,1.4]]
        right=chain(pts,2.3,0.6,'dark','darkest')
        return right+mirror_x(right)
    if race=='namekian':
        # two thin antennae from the forehead, leaning forward and out, with a small bulb at each tip
        pts=[[1.3,35.6,-4.0],[1.6,37.2,-4.6],[2.1,38.8,-5.0],[2.7,40.2,-5.1],[3.3,41.3,-4.9]]
        ant=chain(pts,0.75,0.55,'mid','mid')+bulb([3.4,41.6,-4.85],0.5,'light')
        # pointed ears: tip rising out of each side ear block
        ear=chain([[6.2,33.6,-0.1],[7.3,35.0,0.3],[8.0,36.6,0.6]],1.6,0.5,'mid','dark')
        segs=ant+ear
        return segs+mirror_x(segs)
    if race=='majin':
        # Buu-style head tendril: thick root on top of the head, rising then curling back and down
        pts=[[0,36.0,0.4],[0,38.2,0.8],[0,40.0,1.8],[0,41.0,3.4],[0,40.9,5.1],[0,40.0,6.4],[0,38.6,7.0]]
        return chain(pts,3.0,1.1,'mid','light')+bulb([0,38.2,7.0],0.65,'light')
    return []
def emit_segment(L,seg,vi,nvi):
    a,b,ta,tb,shade=seg
    d=_norm(_sub(b,a))
    up=[0,1,0] if abs(d[1])<0.9 else [1,0,0]
    s1=_norm(_cross(d,up)); s2=_norm(_cross(s1,d))
    def ring(c,t):
        h=t/2
        return [_add(c,_add(_mul(s1,h*i),_mul(s2,h*j))) for i,j in ((-1,-1),(1,-1),(1,1),(-1,1))]
    A=ring(a,ta); B=ring(b,tb)
    quads=[[A[0],A[1],B[1],B[0]],[A[1],A[2],B[2],B[1]],[A[2],A[3],B[3],B[2]],[A[3],A[0],B[0],B[3]],
           [A[3],A[2],A[1],A[0]],[B[0],B[1],B[2],B[3]]]
    centre=_mul(_add(a,b),0.5)
    px,py=FEATURE_UV[0]+8*SHADES[shade]+1,FEATURE_UV[1]+1
    uvs=[(px,py),(px+6,py),(px+6,py+6),(px,py+6)]
    for q in quads:
        q=[[p[0]*S,p[1]*S,-p[2]*S] for p in q]
        c=[sum(p[i] for p in q)/4 for i in range(3)]
        n=_norm(_cross(_sub(q[1],q[0]),_sub(q[2],q[0])))
        out=_sub(c,[centre[0]*S,centre[1]*S,-centre[2]*S])
        # emit both windings: the body cubes mix windings, so stay visible whether or not culling is on
        n=_norm(_cross(_sub(q[1],q[0]),_sub(q[2],q[0])))
        out=_sub(c,[centre[0]*S,centre[1]*S,-centre[2]*S])
        if sum(n[i]*out[i] for i in range(3))<0: n=_mul(n,-1)
        for qq,tt,nn in ((q,uvs,n),(q[::-1],uvs[::-1],n)):
            L.append('vn %.5f %.5f %.5f'%tuple(nn)); ni=nvi[0]; nvi[0]+=1
            idx=[]
            for p,t in zip(qq,tt):
                L.append('v %.6f %.6f %.6f'%tuple(p)); L.append('vt %.6f %.6f'%(t[0]/256,1-t[1]/256))
                idx.append('%d/%d/%d'%(vi,vi,ni)); vi+=1
            L.append('f '+' '.join(idx))
    return vi

# ---- Blockbench source -------------------------------------------------------
BB=pathlib.Path(__file__).resolve().parent.parent/'assets/blockbench/races'
def gen_bbmodel(race,out):
    """Export the OBJ from assets/blockbench/races/<race>.bbmodel (Bedrock-format project).
    Blockbench stores bedrock x negated; convert back, then use the same face layout as gen()."""
    m=json.load(open(BB/(race+'.bbmodel')))
    TW,TH=m['resolution']['width'],m['resolution']['height']
    els={e['uuid']:e for e in m['elements']}
    groups={}
    def walk(node,gn):
        if isinstance(node,str):
            e=els[node]
            if gn and e.get('type','cube')=='cube': groups.setdefault(gn,[]).append(e)
            return
        name=node['name']
        own=GROUP.get(name)
        # only cubes owned directly by a rig bone; eyelids, tail, locators have dedicated layers
        for ch in node.get('children',[]):
            walk(ch, own if isinstance(ch,str) else None)
    for r in m['outliner']: walk(r,None)
    L=['# exported from assets/blockbench/races/%s.bbmodel by gen_race_obj.py'%race]
    vi=1; nvi=[1]
    N={'north':(0,0,1),'south':(0,0,-1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}
    for gn in ['rightarm','body','leftarm','rightleg','head','leftleg']:
        if gn not in groups: continue
        L.append('o '+gn)
        for e in groups[gn]:
            if e.get('rotation') and any(e['rotation']): print('warn rotated cube',race,e['name'])
            inf=e.get('inflate',0)
            x0,x1=-e['to'][0]-inf,-e['from'][0]+inf
            y0,y1=e['from'][1]-inf,e['to'][1]+inf
            z0,z1=e['from'][2]-inf,e['to'][2]+inf
            F={'north':[(x1,y1,z0),(x0,y1,z0),(x0,y0,z0),(x1,y0,z0)],
               'south':[(x0,y1,z1),(x1,y1,z1),(x1,y0,z1),(x0,y0,z1)],
               'east':[(x1,y1,z1),(x1,y1,z0),(x1,y0,z0),(x1,y0,z1)],
               'west':[(x0,y1,z0),(x0,y1,z1),(x0,y0,z1),(x0,y0,z0)],
               'up':[(x1,y1,z0),(x0,y1,z0),(x0,y1,z1),(x1,y1,z1)],
               'down':[(x1,y0,z1),(x0,y0,z1),(x0,y0,z0),(x1,y0,z0)]}
            feature=e.get('color')==3
            for fn,q in F.items():
                f=e['faces'].get(fn)
                if not f or f.get('texture') is None: continue
                u0,v0,u1,v1=f['uv']
                uvs=[(u0,v0),(u1,v0),(u1,v1),(u0,v1)]
                q=[(p[0]*S,p[1]*S,-p[2]*S) for p in q][::-1]; uvs=uvs[::-1]
                variants=[(q,uvs,N[fn])]
                if feature:  # double-sided like the chained features (mixed culling on body cubes)
                    variants.append((q[::-1],uvs[::-1],tuple(-x for x in N[fn])))
                for qq,tt,nn in variants:
                    L.append('vn %d %d %d'%nn); ni=nvi[0]; nvi[0]+=1
                    idx=[]
                    for p,t in zip(qq,tt):
                        L.append('v %.6f %.6f %.6f'%p); L.append('vt %.6f %.6f'%(t[0]/TW,1-t[1]/TH))
                        idx.append('%d/%d/%d'%(vi,vi,ni)); vi+=1
                    L.append('f '+' '.join(idx))
    open(out,'w').write('\n'.join(L)+'\n')

for r in sys.argv[1:]:
    bbp=BB/(r+'.bbmodel')
    bbj=json.load(open(bbp)) if bbp.exists() else {}
    if bbj.get('dba_exporter') or bbj.get('meta',{}).get('model_format')=='free':
        print('skip',r,'(exported by',bbj.get('dba_exporter','its own generator')+')'); continue
    (gen_bbmodel if bbp.exists() else gen)(r,O+r+'.obj')
