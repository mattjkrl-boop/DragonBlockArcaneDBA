package com.dragonblockarcanedba.client.render.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * High-performance 3D Wavefront OBJ mesh representation and renderer for Minecraft 26.2.
 * Pre-transforms and caches polygon vertex buffers relative to Minecraft HumanoidModel pivots.
 */
public class ObjMesh {

    public static class QuadFace {
        public final float[] x = new float[4];
        public final float[] y = new float[4];
        public final float[] z = new float[4];
        public final float[] u = new float[4];
        public final float[] v = new float[4];
        public final float[] nx = new float[4];
        public final float[] ny = new float[4];
        public final float[] nz = new float[4];
    }

    public static class LimbGroup {
        public final String name;
        public final List<QuadFace> faces = new ArrayList<>();

        public LimbGroup(String name) {
            this.name = name;
        }

        public void render(Matrix4f matrix, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
            for (QuadFace q : faces) {
                consumer.addVertex(matrix, q.x[0], q.y[0], q.z[0]).setColor(r, g, b, a).setUv(q.u[0], q.v[0]).setOverlay(overlay).setLight(light).setNormal(q.nx[0], q.ny[0], q.nz[0]);
                consumer.addVertex(matrix, q.x[1], q.y[1], q.z[1]).setColor(r, g, b, a).setUv(q.u[1], q.v[1]).setOverlay(overlay).setLight(light).setNormal(q.nx[1], q.ny[1], q.nz[1]);
                consumer.addVertex(matrix, q.x[2], q.y[2], q.z[2]).setColor(r, g, b, a).setUv(q.u[2], q.v[2]).setOverlay(overlay).setLight(light).setNormal(q.nx[2], q.ny[2], q.nz[2]);
                consumer.addVertex(matrix, q.x[3], q.y[3], q.z[3]).setColor(r, g, b, a).setUv(q.u[3], q.v[3]).setOverlay(overlay).setLight(light).setNormal(q.nx[3], q.ny[3], q.nz[3]);
            }
        }
    }

    private final Map<String, LimbGroup> limbs = new HashMap<>();

    public LimbGroup getLimb(String name) {
        return limbs.get(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> getLimbNames() {
        return limbs.keySet();
    }

    /**
     * Parses an OBJ input stream and bakes it into pre-aligned limb groups.
     */
    public static ObjMesh parse(InputStream in) {
        ObjMesh mesh = new ObjMesh();

        // Bone pivots in OBJ coordinate space
        Map<String, float[]> pivots = new HashMap<>();
        pivots.put("head", new float[]{-0.208f, 6.794f, 0.104f});
        pivots.put("body", new float[]{-0.208f, 6.794f, 0.104f});
        pivots.put("rightarm", new float[]{-1.107f, 6.794f, 0.104f});
        pivots.put("leftarm", new float[]{0.691f, 6.794f, 0.104f});
        pivots.put("rightleg", new float[]{-0.561f, 3.469f, 0.104f});
        pivots.put("leftleg", new float[]{0.146f, 3.469f, 0.104f});

        // 12 pixels per 3.364 OBJ units * 1/16 meter per pixel = 0.22294887 meters per OBJ unit
        final float scale = (12.0f / 3.364f) * 0.0625f;

        List<float[]> vertices = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        LimbGroup currentGroup = new LimbGroup("body");
        mesh.limbs.put("body", currentGroup);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                String tag = parts[0];

                switch (tag) {
                    case "v" -> {
                        float vx = Float.parseFloat(parts[1]);
                        float vy = Float.parseFloat(parts[2]);
                        float vz = Float.parseFloat(parts[3]);
                        vertices.add(new float[]{vx, vy, vz});
                    }
                    case "vt" -> {
                        float u = Float.parseFloat(parts[1]);
                        float v = Float.parseFloat(parts[2]);
                        texCoords.add(new float[]{u, v});
                    }
                    case "vn" -> {
                        float nx = Float.parseFloat(parts[1]);
                        float ny = Float.parseFloat(parts[2]);
                        float nz = Float.parseFloat(parts[3]);
                        normals.add(new float[]{nx, ny, nz});
                    }
                    case "o", "g" -> {
                        String groupName = parts[1].toLowerCase(Locale.ROOT);
                        currentGroup = mesh.limbs.computeIfAbsent(groupName, LimbGroup::new);
                    }
                    case "f" -> {
                        if (parts.length < 4) continue;

                        int count = parts.length - 1;
                        int[] vIdx = new int[count];
                        int[] vtIdx = new int[count];
                        int[] vnIdx = new int[count];

                        for (int i = 0; i < count; i++) {
                            String[] token = parts[i + 1].split("/");
                            vIdx[i] = Integer.parseInt(token[0]) - 1;
                            vtIdx[i] = token.length > 1 && !token[1].isEmpty() ? Integer.parseInt(token[1]) - 1 : -1;
                            vnIdx[i] = token.length > 2 && !token[2].isEmpty() ? Integer.parseInt(token[2]) - 1 : -1;
                        }

                        float[] pivot = pivots.getOrDefault(currentGroup.name, pivots.get("body"));
                        float px = pivot[0];
                        float py = pivot[1];
                        float pz = pivot[2];

                        if (count == 4) {
                            QuadFace q = new QuadFace();
                            int[] winding = new int[]{0, 3, 2, 1};
                            for (int w = 0; w < 4; w++) {
                                int i = winding[w];
                                float[] v = vertices.get(vIdx[i]);
                                q.x[w] = (v[0] - px) * scale;
                                q.y[w] = -(v[1] - py) * scale;
                                q.z[w] = (v[2] - pz) * scale;

                                if (vtIdx[i] >= 0 && vtIdx[i] < texCoords.size()) {
                                    float[] vt = texCoords.get(vtIdx[i]);
                                    q.u[w] = vt[0];
                                    q.v[w] = 1.0f - vt[1];
                                }

                                if (vnIdx[i] >= 0 && vnIdx[i] < normals.size()) {
                                    float[] vn = normals.get(vnIdx[i]);
                                    q.nx[w] = vn[0];
                                    q.ny[w] = -vn[1];
                                    q.nz[w] = vn[2];
                                }
                            }
                            currentGroup.faces.add(q);
                        } else if (count == 3) {
                            QuadFace q = new QuadFace();
                            int[] winding = new int[]{0, 2, 1, 1};
                            for (int w = 0; w < 4; w++) {
                                int i = winding[w];
                                float[] v = vertices.get(vIdx[i]);
                                q.x[w] = (v[0] - px) * scale;
                                q.y[w] = -(v[1] - py) * scale;
                                q.z[w] = (v[2] - pz) * scale;

                                if (vtIdx[i] >= 0 && vtIdx[i] < texCoords.size()) {
                                    float[] vt = texCoords.get(vtIdx[i]);
                                    q.u[w] = vt[0];
                                    q.v[w] = 1.0f - vt[1];
                                }

                                if (vnIdx[i] >= 0 && vnIdx[i] < normals.size()) {
                                    float[] vn = normals.get(vnIdx[i]);
                                    q.nx[w] = vn[0];
                                    q.ny[w] = -vn[1];
                                    q.nz[w] = vn[2];
                                }
                            }
                            currentGroup.faces.add(q);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DBA] Error parsing OBJ mesh: " + e.getMessage());
            e.printStackTrace();
        }

        return mesh;
    }
}
