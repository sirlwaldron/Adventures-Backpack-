"""Validate armor slot ownership, animation targets, UVs, and release assets."""
from pathlib import Path
import json, math, sys, zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/adventurerspack'
NAMES=['ignis_armor','ignis_armor_winged','cursium_mage','cursium_mage_elytra']
SLOTS={'armorHead','armorBody','armorRightArm','armorLeftArm','armorRightLeg','armorLeftLeg','armorRightBoot','armorLeftBoot','armorLeggingTorsoLayer'}
for name in NAMES:
    geo=json.loads((ASSETS/f'geo/{name}.geo.json').read_text())['minecraft:geometry'][0]
    bones={b['name']:b for b in geo['bones']}
    assert len(bones)==len(geo['bones']) and SLOTS<=bones.keys()
    cubes=0
    for bone in bones.values():
        ancestors=[];current=bone
        while current:
            assert current['name'] not in ancestors, 'Cyclic rig'
            ancestors.append(current['name'])
            current=bones[current['parent']] if 'parent' in current else None
        if bone.get('cubes'):assert len(SLOTS.intersection(ancestors))==1, (name,bone['name'],'slot ownership')
        if bone['name'].startswith('ap_taper_'):
            factors=list(map(int,bone['name'].split('_')[2:4]))
            assert all(0<f<=100 for f in factors)
        for cube in bone.get('cubes',[]):
            cubes+=1
            assert all(math.isfinite(n) and n>0 for n in cube['size'])
            assert set(cube['uv'])=={'north','south','east','west','up','down'}
            for face in cube['uv'].values():
                assert all(u>=0 and 0<s and u+s<=512 for u,s in zip(face['uv'],face['uv_size']))
    assert cubes<150
    texname={'cursium_mage':'cursium_mage_armor','cursium_mage_elytra':'cursium_mage_armor_elytra'}.get(name,name)
    for suffix in ['', '_glowmask']:
        image=Image.open(ASSETS/f'textures/models/armor/{texname}{suffix}.png')
        assert image.size==(512,512) and image.mode=='RGBA'
    if name.endswith(('winged','elytra')):
        animation='ignis_armor_winged' if name.startswith('ignis') else 'cursium_mage'
        clips=json.loads((ASSETS/f'animations/item/{animation}.animation.json').read_text())['animations']
        assert {'idle','flying'}<=clips.keys()
        for clip in clips.values():
            for bone in clip['bones']:
                assert bone in bones
                current=bones[bone]
                while current['name']!='armorBody':current=bones[current['parent']]
    print(f'{name}: {cubes} cubes; slots, UVs and animations valid')

if len(sys.argv)>1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        assert 'com/waldron/adventurerspack/client/ArmorShapeFactory.class' in jar.namelist()
        for path in ASSETS.rglob('*'):
            if path.is_file() and (path.name.startswith(('ignis_','cursium_mage')) or path.parent.name=='armor'):
                resource='assets/adventurerspack/'+path.relative_to(ASSETS).as_posix()
                assert jar.read(resource)==path.read_bytes(), resource
    print('Release contains the current armor models, textures, icons, animations and shape factory.')
