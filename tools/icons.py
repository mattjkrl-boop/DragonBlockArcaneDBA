"""Normalize DBA item icons to 64x64 with a shared outline style, and draw redrawn icons."""
from PIL import Image, ImageDraw, ImageFilter
import glob, os, math
D='src/main/resources/assets/dragonblockarcanedba/textures/item/'
S=64
def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(255,)
def outline(im,col=(20,16,24,255)):
    a=im.split()[3].point(lambda v:255 if v>40 else 0)
    grown=a.filter(ImageFilter.MaxFilter(3))
    o=Image.new('RGBA',im.size,col); base=Image.new('RGBA',im.size,(0,0,0,0)); base.paste(o,mask=grown)
    base.alpha_composite(im); return base
def bar(d,p0,p1,w,c,hi=1.35,lo=0.6):
    # shaded thick line: dark rim, mid, highlight stripe
    d.line([p0,p1],fill=shade(c,lo),width=w+2); d.line([p0,p1],fill=c,width=w)
    if w>=3: 
        dx,dy=p1[0]-p0[0],p1[1]-p0[1]; L=math.hypot(dx,dy) or 1; nx,ny=-dy/L,dx/L; o=w/4
        d.line([(p0[0]+nx*o,p0[1]+ny*o),(p1[0]+nx*o,p1[1]+ny*o)],fill=shade(c,hi),width=max(1,w//3))
def new(): im=Image.new('RGBA',(S,S),(0,0,0,0)); return im,ImageDraw.Draw(im)
def blaster():
    im,d=new(); g=(92,98,110,255); dark=(48,52,62,255)
    d.rounded_rectangle([10,18,50,32],4,fill=g,outline=dark)          # body
    d.rectangle([50,21,60,28],fill=(70,74,86,255),outline=dark)        # barrel
    d.rectangle([58,22,62,27],fill=(40,200,255,255))                   # emitter
    d.polygon([(18,32),(30,32),(26,52),(14,52)],fill=(60,64,74,255),outline=dark) # grip
    d.rectangle([22,33,30,40],outline=dark)                            # trigger guard
    for x in (36,40,44): d.rectangle([x,20,x+1,24],fill=(255,120,40,255))  # heat vents
    d.line([(12,20),(48,20)],fill=(150,156,170,255)); d.rectangle([14,23,34,27],fill=(30,180,240,255))  # energy cell
    return im
def piccolo(part):
    im,d=new(); purple=(98,52,150,255); white=(236,236,240,255); bl=(110,70,40,255)
    if part=='helmet':
        d.ellipse([12,14,52,48],fill=white); d.rectangle([12,32,52,50],fill=(0,0,0,0))
        d.pieslice([12,14,52,54],180,360,fill=white); d.rectangle([12,32,52,40],fill=shade(white,0.8))
        d.ellipse([28,8,36,16],fill=white); d.line([(16,22),(48,22)],fill=shade(white,1.0)); d.arc([14,16,50,50],200,250,fill=(255,255,255,255),width=2)
    if part=='chestplate':
        d.polygon([(14,10),(50,10),(56,20),(52,56),(12,56),(8,20)],fill=purple)
        d.polygon([(4,8),(26,8),(20,20),(2,20)],fill=white); d.polygon([(38,8),(60,8),(62,20),(44,20)],fill=white)  # pauldrons
        d.rectangle([12,40,52,44],fill=(220,40,50,255)); d.line([(32,12),(32,40)],fill=shade(purple,0.7))
        d.line([(16,14),(16,52)],fill=shade(purple,1.3),width=2)
    if part=='leggings':
        d.rectangle([14,8,50,14],fill=(220,40,50,255)); d.polygon([(14,14),(31,14),(29,58),(16,58)],fill=purple); d.polygon([(33,14),(50,14),(48,58),(35,58)],fill=purple)
        d.line([(19,16),(19,56)],fill=shade(purple,1.3),width=2); d.line([(38,16),(38,56)],fill=shade(purple,1.3),width=2)
    if part=='boots':
        for x in (8,36):
            d.rectangle([x+2,16,x+16,42],fill=bl); d.polygon([(x+2,42),(x+16,42),(x+22,52),(x,52)],fill=shade(bl,0.8)); d.line([(x+5,18),(x+5,40)],fill=shade(bl,1.4),width=2)
    return im
def power_pole():
    im,d=new(); bar(d,(10,54),(54,10),6,(200,30,30,255))
    for p in ((8,56),(52,12)): d.ellipse([p[0]-4,p[1]-4,p[0]+4,p[1]+4],fill=(230,180,40,255),outline=(120,80,10,255))
    return im
def whis_staff():
    im,d=new(); bar(d,(14,58),(46,16),4,(225,225,235,255))
    d.ellipse([38,4,58,24],outline=(200,160,40,255),width=3)          # ring
    d.ellipse([43,9,53,19],fill=(80,200,255,255),outline=(20,90,160,255))  # orb
    d.ellipse([45,11,48,14],fill=(255,255,255,255))
    d.rectangle([12,56,17,60],fill=(200,160,40,255))
    return im
def shard(c):
    im,d=new(); pts=[(32,4),(44,26),(36,60),(22,34)]; d.polygon(pts,fill=c)
    d.polygon([(32,4),(36,60),(22,34)],fill=shade(c,0.7)); d.line([(32,8),(40,26)],fill=(255,255,255,220),width=2)
    d.polygon([(46,30),(54,40),(48,52),(42,40)],fill=shade(c,1.1)); return im
REDRAW={'blaster_gun':blaster,'piccolo_outfit_helmet':lambda:piccolo('helmet'),'piccolo_outfit_chestplate':lambda:piccolo('chestplate'),
'piccolo_outfit_leggings':lambda:piccolo('leggings'),'piccolo_outfit_boots':lambda:piccolo('boots'),'power_pole':power_pole,'whis_staff':whis_staff,
'ki_shard':lambda:shard((90,200,255,255)),'gold_ki_shard':lambda:shard((245,200,60,255))}
THIN={'devil_trident'}
for p in sorted(glob.glob(D+'*.png')):
    n=os.path.basename(p)[:-4]
    if n in REDRAW: im=REDRAW[n]()
    else:
        im=Image.open(p).convert('RGBA')
        if im.size[0]>S:
            if n in THIN:
                a=im.split()[3].filter(ImageFilter.MaxFilter(int(im.size[0]/S*2)|1)); rgb=im.filter(ImageFilter.MaxFilter(int(im.size[0]/S*2)|1)); rgb.putalpha(a); im=rgb
            bb=im.getbbox(); im=im.crop(bb); m=max(im.size); sq=Image.new('RGBA',(m,m),(0,0,0,0)); sq.paste(im,((m-im.size[0])//2,(m-im.size[1])//2)); 
            im=sq.resize((S-4,S-4),Image.LANCZOS); c=Image.new('RGBA',(S,S),(0,0,0,0)); c.paste(im,(2,2)); im=c
            im.putalpha(im.split()[3].point(lambda v:255 if v>110 else 0))
        elif im.size[0]<S: im=im.resize((S,S),Image.NEAREST)
    im=outline(im); im.save(p)
print('ok')
