package com.waldron.adventurerspack.client;

import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.cache.object.GeoQuad;
import mod.azure.azurelib.cache.object.GeoVertex;
import mod.azure.azurelib.loading.object.GeometryTree;
import mod.azure.azurelib.loading.object.BoneStructure;
import mod.azure.azurelib.loading.json.raw.ModelProperties;
import mod.azure.azurelib.loading.json.raw.Cube;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.factory.AzBakedModelFactory;
import mod.azure.azurelib.model.factory.impl.AzBuiltinBakedModelFactory;
import org.joml.Vector3f;

/** Tapers authored armor pieces once during resource baking, without adding faces. */
public final class ArmorShapeFactory extends AzBakedModelFactory {
    private final AzBakedModelFactory delegate = new AzBuiltinBakedModelFactory();

    @Override
    public AzBakedModel constructGeoModel(GeometryTree tree) {
        AzBakedModel model = delegate.constructGeoModel(tree);
        for (AzBone bone : model.getBonesByName().values()) {
            if (!bone.getName().startsWith("ap_taper_")) continue;
            String[] parts = bone.getName().split("_");
            float bottom = Integer.parseInt(parts[2]) / 100F;
            float top = Integer.parseInt(parts[3]) / 100F;
            bone.getCubes().replaceAll(cube -> taper(cube, bottom, top));
        }
        return model;
    }

    private static GeoCube taper(GeoCube cube, float bottom, float top) {
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            for (GeoVertex vertex : quad.vertices()) {
                min.min(vertex.position());
                max.max(vertex.position());
            }
        }
        if (max.y - min.y < 1E-6F) return cube;
        float cx = (min.x + max.x) / 2F, cz = (min.z + max.z) / 2F;
        GeoQuad[] quads = new GeoQuad[cube.quads().length];
        for (int i = 0; i < quads.length; i++) {
            GeoQuad quad = cube.quads()[i];
            if (quad == null) continue;
            GeoVertex[] vertices = new GeoVertex[quad.vertices().length];
            for (int j = 0; j < vertices.length; j++) {
                GeoVertex vertex = quad.vertices()[j];
                Vector3f p = new Vector3f(vertex.position());
                float t = (p.y - min.y) / (max.y - min.y);
                float scale = bottom + (top - bottom) * t;
                p.x = cx + (p.x - cx) * scale;
                p.z = cz + (p.z - cz) * scale;
                vertices[j] = new GeoVertex(p, vertex.texU(), vertex.texV());
            }
            Vector3f normal = new Vector3f(vertices[1].position()).sub(vertices[0].position())
                    .cross(new Vector3f(vertices[2].position()).sub(vertices[0].position())).normalize();
            if (normal.dot(quad.normal()) < 0) normal.negate();
            quads[i] = new GeoQuad(vertices, normal, quad.direction());
        }
        return new GeoCube(quads, cube.pivot(), cube.rotation(), cube.size(), cube.inflate(),
                cube.mirror(), cube.transform(), cube.normalFlips());
    }

    @Override
    public AzBone constructBone(BoneStructure bone, ModelProperties properties, AzBone parent) {
        return delegate.constructBone(bone, properties, parent);
    }

    @Override
    public GeoCube constructCube(Cube cube, ModelProperties properties, AzBone bone) {
        return delegate.constructCube(cube, properties, bone);
    }
}
