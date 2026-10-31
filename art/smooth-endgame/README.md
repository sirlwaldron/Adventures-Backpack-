# Smooth Endgame

Reference: user-reference.png, supplied by the user. This is a newly authored polygon interpretation of that single concept image, not a recovered original 3D model.

Run `python tools/build_smooth_endgame.py` after legacy arsenal generators. It writes only Endgame resources and previews. Existing display transforms and item behavior are preserved. Forge 1.20.1 supplies the OBJ loader; no new runtime dependency is required.

The previous Endgame JSON is saved as endgame-before.json. The old optimized texture remains available. Restore that JSON to revert.

Validation covers finite geometry, OBJ indices, material references, unchanged display transforms and packaged asset equality. Previews are generated from the same triangles exported to OBJ; they are not Minecraft screenshots. In-game model loading, lighting, performance and hand placement still need a visual check.
