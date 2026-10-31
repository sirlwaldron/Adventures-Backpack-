# Shape-preserving arsenal conversion

The original designs in before/ are authoritative. The redesigned draft was rejected and replaced.

All 17 variants use native Forge OBJ meshes. Sixteen preserve every original vertex, UV layout and texture. The pickaxe retains its original crystal/armor/pommel polygons from reference_pickaxe.py, replacing the narrow box-strip approximation with triangulated extruded outlines. Its other parts are unchanged.

Run tools/build_smooth_arsenal.py, then tools/validate_smooth_arsenal.py. Do not use the legacy rebuild_arsenal.py for these assets; it overwrites newer sword and lance work.

Maximum mesh: 1,508 triangles. Pickaxe: 548 triangles. Model transforms and all bow predicates are preserved. Previews use exported geometry and existing textures; actual Minecraft appearance still needs a visual check.
