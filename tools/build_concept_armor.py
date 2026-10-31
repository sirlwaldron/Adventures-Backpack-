"""Crimson mage and End Game battlemage assets for the existing Azure armor rig.

Painted bevels, rune panels and restrained glow avoid dense decorative meshes.
All armor slot names and wing controller names remain compatible with the mod.
"""
from pathlib import Path
import copy, json, math, shutil
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/adventurerspack'
ART=ROOT/'art/concept-armor';BACKUP=ART/'before';BACKUP.mkdir(parents=True,exist_ok=True)
PALETTE={'armor':(36,38,46),'steel':(130,138,151),'cloth':(105,13,32),'dark':(18,20,27),
         'red':(174,20,43),'ruby':(216,25,53),'leather':(42,29,36),'visor':(247,42,71)}

def folded_wing_rotation(side, segment):
    # Sweep the root back and down; tuck the outer sections toward the spine.
    return [0, -side*65, -side*25] if segment == 0 else [0, 0, -side*15]

class Armor:
    def __init__(self,name,heavy,winged):
        self.name=name;self.heavy=heavy;self.winged=winged;self.bones=[];self.byname={}
        self.texture=Image.new('RGBA',(512,512),(0,0,0,0));self.glow=self.texture.copy()
        self.cursor=[2,2,0];self.tiles={}
    def bone(self,name,parent=None,pivot=(0,0,0),rotation=None):
        b={'name':name,'pivot':list(pivot)}
        if parent:b['parent']=parent
        if rotation:b['rotation']=rotation
        self.bones.append(b);self.byname[name]=b
        return name
    def tile(self,w,h,material,runes):
        w=max(3,math.ceil(w*4));h=max(3,math.ceil(h*4));key=(w,h,material,runes)
        if key in self.tiles:return self.tiles[key]
        x,y,row=self.cursor
        if x+w+2>=512:x=2;y+=row+3;row=0
        assert y+h+2<512,(self.name,key,'atlas overflow')
        base=PALETTE[material];d=ImageDraw.Draw(self.texture);g=ImageDraw.Draw(self.glow)
        d.rectangle((x,y,x+w-1,y+h-1),fill=(*base,255))
        # Broad shaded borders read as forged bevels at Minecraft viewing size.
        shine=tuple(min(255,c+50) for c in base)+(255,);shadow=tuple(max(0,c-17) for c in base)+(255,)
        d.line([(x,y+h-1),(x,y),(x+w-1,y)],fill=shine,width=1)
        d.line([(x+w-1,y),(x+w-1,y+h-1),(x,y+h-1)],fill=shadow,width=1)
        if min(w,h)>=9 and material in ['armor','cloth']:
            d.rectangle((x+2,y+2,x+w-3,y+h-3),outline=(117,121,134,255),width=1)
            d.rectangle((x+3,y+3,x+w-4,y+h-4),outline=(27,20,30,255),width=1)
            for xx,yy in [(x+3,y+3),(x+w-4,y+3),(x+3,y+h-4),(x+w-4,y+h-4)]:d.point((xx,yy),fill=(211,214,220,255))
        if material=='cloth':
            for xx in range(x+5,x+w-4,5):d.line((xx,y+4,xx,y+h-5),fill=(87,10,28,255))
        if material=='leather':
            for yy in range(y+2,y+h,5):d.line((x,yy,x+w-1,yy+2),fill=(84,46,55,255))
        if runes and w>=9 and h>=12:
            cx=x+w//2
            for yy in range(y+7,y+h-5,10):
                glyph=[(cx,yy-3),(cx,yy+3),(cx-2,yy),(cx+2,yy-2)]
                d.line(glyph,fill=(230,40,65,255),width=1)
                g.line(glyph,fill=(188,17,40,190),width=1)
        if material in ['ruby','visor']:
            d.polygon([(x+1,y+h//2),(x+w//2,y+1),(x+w-2,y+h//2)],fill=(255,87,114,255))
            d.polygon([(x+1,y+h//2),(x+w//2,y+h-2),(x+w-2,y+h//2)],fill=(144,8,31,255))
            g.rectangle((x+1,y+1,x+w-2,y+h-2),fill=(221,27,55,235))
        self.cursor=[x+w+3,y,max(row,h)]
        self.tiles[key]={'uv':[x,y],'uv_size':[w,h]};return copy.deepcopy(self.tiles[key])
    def box(self,bone,center,size,material='armor',rotation=None,runes=False,taper=None):
        if taper:
            bone=self.bone(f'ap_taper_{taper[0]}_{taper[1]}_{len(self.bones)}',bone,center)
        cube={'origin':[round(c-s/2,4) for c,s in zip(center,size)],'size':list(size),'uv':{}}
        for face,w,h in [('north',size[0],size[1]),('south',size[0],size[1]),('east',size[2],size[1]),('west',size[2],size[1]),('up',size[0],size[2]),('down',size[0],size[2])]:
            cube['uv'][face]=self.tile(w,h,material,runes and face in ['north','south'])
        if rotation:cube.update(pivot=list(center),rotation=rotation)
        self.byname[bone].setdefault('cubes',[]).append(cube)
    def gem(self,bone,x,y,z,size):
        self.box(bone,(x,y,z),(size*1.35,size*1.35,.28),'steel',[0,0,45])
        self.box(bone,(x,y,z-.2),(size,size,.38),'ruby',[0,0,45])
    def rig(self):
        for label,pivot in [('Head',(0,24,0)),('Body',(0,24,0)),('RightArm',(-5,22,0)),('LeftArm',(5,22,0)),('RightLeg',(-2,12,0)),('LeftLeg',(2,12,0))]:
            self.bone('biped'+label,pivot=pivot);self.bone('armor'+label,'biped'+label,pivot)
        self.bone('armorLeggingTorsoLayer','bipedBody',(0,24,0))
        for side,x in [('Right',-2),('Left',2)]:self.bone('armor'+side+'Boot','biped'+side+'Leg',(x,12,0))
    def body(self):
        h=self.heavy
        self.box('armorBody',(0,18,0),(8.7,11.6,5.0),'dark')
        self.box('armorBody',(0,19.5,-2.65),(7.5,7.7,.8),'armor',runes=True)
        self.box('armorBody',(0,19.5,2.65),(7.5,8,.8),'armor',runes=True)
        for side in [-1,1]:
            self.box('armorBody',(side*2.0,20,-3.15),(1,7,.45),'steel',[0,0,side*15])
            self.box('armorBody',(side*2.7,18.8,-3.40),(.5,6,.45),'red',[0,0,side*13])
            # Raised coat lapels and collar keep the two sets' mage identity.
            self.box('armorBody',(side*3.5,23,0),(1.1,3,5.8),'cloth',[0,0,side*13],True)
            self.box('armorBody',(side*3.5,16.4,-3),(1.5,5,.55),'cloth',runes=True)
        self.gem('armorBody',0,21,-3.45,1.35 if h else 1.1)
        for y in [15.6,14.2]:self.box('armorBody',(0,y,-2.9),(7.8,.85,.7),'armor',runes=True)
        self.box('armorLeggingTorsoLayer',(0,12.3,0),(9.1,1.8,5.6),'leather')
        self.gem('armorLeggingTorsoLayer',0,12.3,-3.1,.85)
        # Each skirt half follows its own leg; no rigid floor-length chest piece.
        for side,x in [('Right',-2),('Left',2)]:
            sign=1 if x>0 else -1
            leg='armor'+side+'Leg';arm='armor'+side+'Arm';boot='armor'+side+'Boot'
            self.box(leg,(x,8.2,0),(3.95,7.8,4.25),'dark')
            self.box(leg,(x,8.4,-2.35),(3.7,5.2,.65),'armor',runes=True)
            for z in [-2.85,2.85]:
                self.box(leg,(x+sign*.5,8.6 if h else 7,z),(4.2,6 if h else 9.2,.65),'armor' if h else 'cloth',[0,0,sign*5],True)
                self.box(leg,(x+sign*2.3,8.6 if h else 7,z-sign*.05),(.45,6.2 if h else 9.4,.75),'steel',[0,0,sign*5])
            self.box(leg,(x+sign*2.3,8,.1),(.7,7.7,4.9),'armor',[0,0,sign*8],True)
            self.box(boot,(x,2.2,-.3),(3.95,4.0,5.1),'armor',runes=True)
            self.box(boot,(x,4.1,-2.7),(3.6,3.4,.9),'armor',[12,0,0],True)
            self.box(boot,(x,.8,-1.35),(3.98,1.6,6.2),'steel')
            self.box(boot,(x,1.5,-3.9),(3.9,1.2,.5),'armor')
            self.gem(boot,x,3.8,-3.25,.55)
            ax=sign*6
            self.box(arm,(ax,17.5,0),(4.35,10.7,4.5),'dark')
            self.box(arm,(ax,14.8,-2.5),(4.55,4.6,1),'armor',runes=True)
            self.box(arm,(ax,12.6,0),(4.9,1.15,4.95),'steel')
            self.box(arm,(ax,17.6,-2.6),(4.3,.65,.5),'red')
            for i in range(3):
                self.box(arm,(ax+sign*(.25+i*.28),23-i*1.35,0),(5.1+(h*.55)-i*.4,1.7,5.7-i*.3),'armor',[0,0,-sign*(10+i*6)],True)
            if h:
                self.box(arm,(ax+sign*2.4,24.1,.2),(1.15,3.3,1.8),'steel',[0,0,-sign*40])
            self.gem(arm,ax,22,-3.0,.6)
    def head(self):
        bone='armorHead'
        if self.heavy:
            self.box(bone,(0,28,0),(8.8,8.4,8.7),'armor')
            self.box(bone,(0,29,-4.5),(8.0,1.2,.5),'dark')
            for s in [-1,1]:
                self.box(bone,(s*2.1,29.1,-4.8),(3.2,.45,.32),'visor',[0,0,-s*8])
                self.box(bone,(s*3.3,26.6,-4.6),(1.25,4.3,.7),'steel')
                self.box(bone,(s*2.4,25.5,-4.65),(1.6,2.6,.6),'armor')
                # Three simple angled horn sections, rather than many tiny spikes.
                for x,y,w,hh,a in [(4.8,31.2,1.8,3.7,42),(6.0,33.7,1.5,3.5,-18),(5.6,36, .85,2.3,-28)]:
                    self.box(bone,(s*x,y,0),(w,hh,1.8 if w>1 else 1),'steel',[0,0,s*a],taper=(100,18 if w<1 else 65))
            self.box(bone,(0,27,-4.85),(.8,5.3,.65),'steel')
            self.box(bone,(0,32.1,0),(1,1,8.9),'red')
            self.gem(bone,0,31.2,-4.9,.7)
        else:
            self.box(bone,(0,27.5,4),(8.45,7.4,.8),'dark')
            for s in [-1,1]:self.box(bone,(s*4.1,27.5,.2),(.7,7.4,8),'dark')
            self.box(bone,(0,30.5,0),(15.8,.85,13),'armor',[0,0,-4])
            self.box(bone,(0,30.9,0),(13.7,.75,11.8),'cloth',[0,0,-4],True)
            # Consecutive tapered sections share endpoints along the bent crown.
            bx,by=0,31.1
            widths=[8.8,6.5,4.5,2.7,1.2,.12]
            for i,(hh,a) in enumerate([(3.8,0),(3.2,12),(2.7,28),(2.3,50),(1.8,80)]):
                dx=-math.sin(math.radians(a));dy=math.cos(math.radians(a));w=widths[i]
                self.box(bone,(bx+dx*hh/2,by+dy*hh/2,0),(w,hh+.25,w*.94),'dark',[0,0,a],taper=(100,round(widths[i+1]/w*100)))
                bx+=dx*hh;by+=dy*hh
            self.box(bone,(0,32.2,-4.35),(8.7,1.6,.45),'cloth',runes=True)
            self.gem(bone,0,32.6,-4.7,1.15)
            for s in [-1,1]:self.box(bone,(s*3.8,26.5,-3.7),(1,4,1.2),'armor',[0,0,s*7])
    def wings(self):
        if not self.winged:return
        heavy=self.heavy
        if heavy:self.bone('wings','armorBody',(0,21,3))
        for side in [-1,1]:
            names=(['wing','sub1','sub2'] if side<0 else ['wing2','sub3','sub4']) if heavy else (['RightWing','RightWing2','RightWing3'] if side<0 else ['LeftWing','LeftWing2','LeftWing3'])
            parent='wings' if heavy else 'armorBody'
            for i,name in enumerate(names):
                self.bone(name,parent,(side*(3+5*i),21-i*3,3.6),folded_wing_rotation(side,i));parent=name
                self.box(name,(side*(4.8+i*3),21.5-i*2,4+i*.3),(4.2,1.2,1),'armor',[0,0,-side*33])
                for j in range(3):
                    rx=side*(4+i*3+j*.9);ry=22-i*2-j*.7
                    length=(8.5+i*1.6-j*.8)*(1.12 if heavy else .9)
                    angle=side*(25+i*8+j*5)
                    x=rx+math.sin(math.radians(angle))*length/2;y=ry-math.cos(math.radians(angle))*length/2
                    self.box(name,(x,y,4+i*.3),(2.6-j*.25,length,.65),'armor',[0,0,angle],True,taper=(5,100))
                    self.box(name,(x,y+.15,4.39+i*.3),(1.1,length*.85,.18),'cloth',[0,0,angle],True,taper=(5,100))
            # Anchor jewel and forged shoulder joint on each side.
            self.box('armorBody',(side*3.1,21,3.7),(2.4,4.0,1.25),'steel',[0,0,-side*22])
        self.box('armorBody',(0,21.1,3.3),(2.5,3,.8),'ruby',[0,0,45])
    def save(self):
        self.rig();self.body();self.head();self.wings()
        cubes=sum(len(b.get('cubes',[])) for b in self.bones)
        assert cubes<200,(self.name,cubes)
        geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+self.name,'texture_width':512,'texture_height':512,'visible_bounds_width':6,'visible_bounds_height':6,'visible_bounds_offset':[0,1.3,0]},'bones':self.bones}]}
        path=ASSETS/f'geo/{self.name}.geo.json'
        if path.exists() and not (BACKUP/path.name).exists():shutil.copy2(path,BACKUP/path.name)
        path.write_text(json.dumps(geo,indent=2)+'\n')
        texname={'cursium_mage':'cursium_mage_armor','cursium_mage_elytra':'cursium_mage_armor_elytra'}.get(self.name,self.name)
        for suffix,img in [('',self.texture),('_glowmask',self.glow)]:
            target=ASSETS/f'textures/models/armor/{texname}{suffix}.png'
            if target.exists() and not (BACKUP/target.name).exists():shutil.copy2(target,BACKUP/target.name)
            img.save(target)
        return geo,cubes

def animations(heavy):
    path=ASSETS/('animations/item/cursium_mage.animation.json' if heavy else 'animations/item/ignis_armor_winged.animation.json')
    if not (BACKUP/path.name).exists():shutil.copy2(path,BACKUP/path.name)
    states={}
    for state in (['idle','flying','fall_fly'] if heavy else ['idle','flying']):
        bones={}
        if heavy:bones['wings']={'rotation':[0,0,0]}
        for side in [-1,1]:
            names=(['wing','sub1','sub2'] if side<0 else ['wing2','sub3','sub4']) if heavy else (['RightWing','RightWing2','RightWing3'] if side<0 else ['LeftWing','LeftWing2','LeftWing3'])
            for i,name in enumerate(names):
                # Animation offsets are relative to the folded model pose.
                if state=='idle':
                    bones[name]={'rotation':[0,0,0]}
                    continue
                elif state=='fall_fly':values=[(0,[-12,side*-12,side*-18])]
                else:values=[(0,[-8,side*-12,side*-18]),(.5,[8,side*14,side*4]),(1,[-8,side*-12,side*-18])]
                bones[name]={'rotation':{str(t):{'vector':[round(v/(i+1)-rest,3) for v,rest in zip(value,folded_wing_rotation(side,i))]} for t,value in values}}
        states[state]={'loop':True,'animation_length':2 if state=='idle' else 1,'bones':bones}
    path.write_text(json.dumps({'format_version':'1.8.0','animations':states},indent=2)+'\n')

if __name__=='__main__':
    records=[]
    for name,heavy,winged in [('ignis_armor',False,False),('ignis_armor_winged',False,True),('cursium_mage',True,False),('cursium_mage_elytra',True,True)]:
        armor=Armor(name,heavy,winged);geo,cubes=armor.save();records.append({'name':name,'cubes':cubes,'base_triangles':cubes*12})
    animations(False);animations(True)
    (ART/'build-report.json').write_text(json.dumps(records,indent=2)+'\n');print(json.dumps(records,indent=2))
