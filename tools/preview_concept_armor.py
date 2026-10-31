"""Offline render of the actual exported Azure armor cubes and UV atlases."""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image,ImageDraw,ImageFont
def rotation(axis,degrees):
    a=math.radians(degrees);c,s=math.cos(a),math.sin(a)
    if axis=='x':return np.array([[1,0,0],[0,c,-s],[0,s,c]])
    if axis=='y':return np.array([[c,0,s],[0,1,0],[-s,0,c]])
    return np.array([[c,-s,0],[s,c,0],[0,0,1]])
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/assets/adventurerspack';ART=ROOT/'art/concept-armor'

def render(name,yaw=-22,pose=None,include=None,width=650,height=900,scale=18,transparent=False):
    geo=json.loads((ASSETS/f'geo/{name}.geo.json').read_text())['minecraft:geometry'][0]
    texname={'cursium_mage':'cursium_mage_armor','cursium_mage_elytra':'cursium_mage_armor_elytra'}.get(name,name)
    tex=np.array(Image.open(ASSETS/f'textures/models/armor/{texname}.png').convert('RGBA'))
    glow=np.array(Image.open(ASSETS/f'textures/models/armor/{texname}_glowmask.png').convert('RGBA'))
    bones={b['name']:b for b in geo['bones']};camera=rotation('x',7)@rotation('y',yaw)
    def transform(points,bone):
        b=bones[bone];angles=(pose or {}).get(bone,b.get('rotation',[0,0,0]))
        origin=np.array(b['pivot'])
        r=rotation('z',angles[2])@rotation('y',angles[1])@rotation('x',angles[0])
        points=(points-origin)@r.T+origin
        return transform(points,b['parent']) if 'parent' in b else points
    def relevant(b):
        if include is None:return True
        return b in include or ('parent' in bones[b] and relevant(bones[b]['parent']))
    pix=np.full((height,width,3),(20,23,30),dtype=np.uint8);depth=np.full((height,width),np.inf)
    for bone in geo['bones']:
        if not relevant(bone['name']):continue
        for cube in bone.get('cubes',[]):
            x,y,z=cube['origin'];X,Y,Z=np.array(cube['origin'])+cube['size']
            faces={'north':[(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],'south':[(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],
                   'east':[(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],'west':[(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],
                   'up':[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],'down':[(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)]}
            for face,vertices in faces.items():
                points=np.array(vertices,float)
                if bone['name'].startswith('ap_taper_'):
                    bottom,top=map(int,bone['name'].split('_')[2:4])
                    fraction=(points[:,1]-y)/(Y-y)
                    taper=(bottom+(top-bottom)*fraction)/100
                    points[:,0]=(X+x)/2+(points[:,0]-(X+x)/2)*taper
                    points[:,2]=(Z+z)/2+(points[:,2]-(Z+z)/2)*taper
                if 'rotation' in cube:
                    a=cube['rotation'];origin=np.array(cube['pivot'])
                    r=rotation('z',a[2])@rotation('y',a[1])@rotation('x',a[0]);points=(points-origin)@r.T+origin
                points=(transform(points,bone['name'])-[0,20,0])@camera.T
                normal=np.cross(points[1]-points[0],points[2]-points[0]);normal/=np.linalg.norm(normal)
                if normal[2]<=1e-8:continue
                screen=np.column_stack((width/2+points[:,0]*scale,height/2-points[:,1]*scale))
                low=np.maximum(np.floor(screen.min(axis=0)).astype(int),[0,0]);high=np.minimum(np.ceil(screen.max(axis=0)).astype(int),[width,height])
                if np.any(high<=low):continue
                uv=cube['uv'][face];u,v=uv['uv'];w,h=uv['uv_size']
                source=np.array([[u,v],[u+w,v],[u+w,v+h],[u,v+h]])
                yy,xx=np.mgrid[low[1]:high[1],low[0]:high[0]]
                grid=np.stack((xx+.5,yy+.5,np.ones_like(xx)),axis=-1)
                for tri in [[0,1,2],[0,2,3]]:
                    matrix=np.column_stack((screen[tri],np.ones(3)))
                    if abs(np.linalg.det(matrix))<1e-8:continue
                    weights=grid@np.linalg.inv(matrix)
                    coords=weights@source[tri]
                    zz=weights@points[tri,2]
                    reg=depth[low[1]:high[1],low[0]:high[0]]
                    mask=(weights.min(axis=2)>=-1e-6)&(zz<reg)
                    tx=np.clip(coords[:,:,0].astype(int),int(u),int(u+w-1));ty=np.clip(coords[:,:,1].astype(int),int(v),int(v+h-1))
                    pixels=tex[ty,tx];mask&=pixels[:,:,3]>0;reg[mask]=zz[mask]
                    light=.54+.46*abs(normal@np.array([.3,.4,.866]))
                    colors=pixels[:,:,:3]*light;emissive=glow[ty,tx].astype(float);colors=np.maximum(colors,emissive[:,:,:3]*emissive[:,:,3:]/255)
                    pix[low[1]:high[1],low[0]:high[0]][mask]=np.clip(colors,0,255).astype('uint8')[mask]
    if transparent:return Image.fromarray(np.dstack((pix,np.isfinite(depth).astype('uint8')*255)))
    return Image.fromarray(pix)

def icons():
    for prefix,model,winged,head,legs in [('ignis','ignis_armor','ignis_armor_winged','helmet','leggings'),('cursium_mage','cursium_mage','cursium_mage_elytra','circlet','skirt')]:
        slots=[(head,model,['armorHead']),('chestplate',model,['armorBody','armorLeftArm','armorRightArm']),
               ('chestplate_elytra' if prefix=='ignis' else 'elytra',winged,['armorBody','armorLeftArm','armorRightArm']),
               (legs,model,['armorLeggingTorsoLayer','armorLeftLeg','armorRightLeg']),('boots',model,['armorLeftBoot','armorRightBoot'])]
        for suffix,geo,bones in slots:
            img=render(geo,yaw=158 if geo==winged else -22,include=bones,transparent=True,scale=14)
            img=img.crop(img.getbbox());img.thumbnail((60,60),Image.Resampling.LANCZOS)
            icon=Image.new('RGBA',(64,64));icon.paste(img,((64-img.width)//2,(64-img.height)//2))
            path=ASSETS/f'textures/item/{prefix}_{suffix}.png'
            backup=ART/'before'/('icon_'+path.name)
            if path.exists() and not backup.exists():backup.write_bytes(path.read_bytes())
            icon.save(path)

if __name__=='__main__':
    icons()
    panels=[('CRIMSON','ignis_armor',-22),('END GAME','cursium_mage',-22),('CRIMSON - WINGED REAR','ignis_armor_winged',158),('END GAME - WINGED REAR','cursium_mage_elytra',158)]
    sheet=Image.new('RGB',(1300,1920),(20,23,30));d=ImageDraw.Draw(sheet)
    font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',28)
    for i,(label,name,angle) in enumerate(panels):
        image=render(name,angle,scale=14 if 'winged' in name or 'elytra' in name else 18);image.save(ART/f'{name}-preview.png')
        x=i%2*650;y=i//2*950;sheet.paste(image,(x,y+50));d.text((x+30,y+12),label,font=font,fill=(234,90,111))
    d.text((30,1885),'Exported model preview - Minecraft lighting and movement still need verification',font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',19),fill=(170,181,194))
    sheet.save(ART/'armor-model-preview.png')
