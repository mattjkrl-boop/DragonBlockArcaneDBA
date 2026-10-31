import numpy as np, subprocess, sys, os, wave
SR=44100; rng=np.random.default_rng(7)
def t(d): return np.arange(int(SR*d))/SR
def env(n,a=0.01,r=0.3):
    e=np.ones(n); na=max(1,int(a*SR)); nr=max(1,int(r*SR)); e[:na]=np.linspace(0,1,na); e[-nr:]*=np.linspace(1,0,nr); return e
def noise(d): return rng.standard_normal(int(SR*d))
def lp(x,k): 
    k=max(1,int(k)); return np.convolve(x,np.ones(k)/k,'same')
def sweep(f0,f1,d,shape='sin'):
    f=np.geomspace(f0,f1,int(SR*d)); ph=2*np.pi*np.cumsum(f)/SR
    return np.sin(ph) if shape=='sin' else 2*(ph/(2*np.pi)%1)-1
def mix(*xs):
    n=max(len(x) for x in xs); o=np.zeros(n)
    for x in xs: o[:len(x)]+=x
    return o
def boom(d=1.2,f=55):
    x=t(d); return np.sin(2*np.pi*f*x*np.exp(-x*2))*np.exp(-x*3)
SOUNDS={}
d=1.6; SOUNDS['transform_generic']=mix(sweep(80,900,d)*env(int(SR*d),0.6,0.4)*0.5, lp(noise(d),3)*np.linspace(0.05,0.5,int(SR*d))*env(int(SR*d),0.1,0.3), np.pad(boom(1.0,60),(int(SR*1.2),0))*0.9)
def hurt(f0,d=0.35,grit=0.3):
    x=t(d); v=np.sign(np.sin(2*np.pi*f0*x*(1-0.3*x/d)))*0.4+np.sin(2*np.pi*f0*2*x)*0.3
    return (lp(v,4)+lp(noise(d),2)*grit)*env(len(x),0.005,0.25)
SOUNDS['saiyan_hurt']=hurt(210); SOUNDS['namekian_hurt']=hurt(140,0.4,0.2); SOUNDS['arcosian_hurt']=hurt(320,0.3,0.4)
d=2.5; n=int(SR*d); SOUNDS['space_pod_launch']=mix(lp(noise(d),6)*np.linspace(0.2,1,n)*env(n,0.3,0.8), sweep(60,400,d,'saw')*0.25*env(n,0.5,0.8), boom(1.5,45)*0.8)
d=3.0; n=int(SR*d); x=t(d); SOUNDS['kamehameha_charge']=mix(sweep(120,700,d)*0.35*(1+0.3*np.sin(2*np.pi*7*x)), sweep(240,1400,d)*0.15, lp(noise(d),2)*0.15*np.linspace(0,1,n))*env(n,0.2,0.2)
d=2.2; n=int(SR*d); SOUNDS['kamehameha_fire']=mix(lp(noise(d),3)*0.8*np.exp(-t(d)*1.2), sweep(900,90,d,'saw')*0.3*np.exp(-t(d)*1.5), boom(1.6,50))
# extra layered ability sounds (batch 3)
def impact(f=50,d=1.5,crack=0.6):
    return mix(boom(d,f), lp(noise(0.25),1)*crack*np.exp(-t(0.25)*20), lp(noise(d),40)*0.4*np.exp(-t(d)*2.5))
def whoosh(d=0.45,k=8):
    n=int(SR*d); return lp(noise(d),k)*np.sin(np.linspace(0,np.pi,n))**2*0.9
def charge_loop(f=150,d=2.0):
    x=t(d); return (np.sin(2*np.pi*f*x)*0.4+np.sin(2*np.pi*f*1.5*x)*0.2+lp(noise(d),3)*0.1)*(1+0.25*np.sin(2*np.pi*6*x))
def hum(d=2.0):
    x=t(d); return np.sin(2*np.pi*95*x)*0.4+np.sin(2*np.pi*190*x+np.sin(2*np.pi*3*x))*0.25
def zap(d=0.6):
    n=int(SR*d); s=np.sign(rng.standard_normal(n))*(rng.random(n)<0.08); return lp(s,2)*0.8*np.exp(-t(d)*5)+sweep(2000,200,d)*0.2*np.exp(-t(d)*6)
def laser(d=0.35): return sweep(1800,300,d,'saw')*0.4*np.exp(-t(d)*8)+lp(noise(d),2)*0.1*np.exp(-t(d)*10)
SOUNDS.update({'heavy_impact':impact(),'slash_whoosh':whoosh(),'blade_shing':mix(whoosh(0.3,4)*0.6,sweep(3000,2600,0.8)*0.3*np.exp(-t(0.8)*5)),
'ability_charge':charge_loop(),'saber_hum':hum(),'lightning_zap':zap(),'blaster_shot':laser(),'time_stop':mix(sweep(1200,60,1.5)*0.4*np.exp(-t(1.5)*1.5),lp(noise(1.5),30)[::-1]*0.4),
'dark_pulse':mix(boom(1.4,38)*0.9,sweep(300,80,1.4,'saw')*0.15*np.exp(-t(1.4)*2)),'wind_gust':whoosh(0.9,25)})
out=sys.argv[1]; os.makedirs(out,exist_ok=True)
for k,v in SOUNDS.items():
    v=v/ (np.max(np.abs(v))+1e-9)*0.89; w=f'/tmp/{k}.wav'
    with wave.open(w,'wb') as f: f.setnchannels(1);f.setsampwidth(2);f.setframerate(SR);f.writeframes((v*32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',w,'-c:a','libvorbis','-q:a','5',f'{out}/{k}.ogg'],check=True)
print(len(SOUNDS))
