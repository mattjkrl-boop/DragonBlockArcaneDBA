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
 * Fully dynamic: auto-derives pivots, scale, and head metrics from any model geometry.
 * Just drop in an OBJ with named groups (head, body, rightarm, leftarm, rightleg, leftleg)
 * and everything auto-adapts — eye positions, hair attachment, tail attachment, animations.
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

    public float modelScale = 0.2229f;

    // Head metrics for eye/hair/feature placement (auto-derived from geometry)
    public float headMinX, headMaxX;
    public float headMinY, headMaxY;
    public float headMinZ, headMaxZ;
    public float topOfHeadY = -0.5f;
    public float headHeight = 0.5f;
    public float headWidth = 0.5f;
    public float faceFrontZ = -0.255f;

    private final Map<String, LimbGroup> limbs = new HashMap<>();

    public LimbGroup getLimb(String name) {
        return limbs.get(name.toLowerCase(Locale.ROOT));
    }

    public Set<String> getLimbNames() {
        return limbs.keySet();
    }

    /**
     * Parses an OBJ input stream and bakes it into pre-aligned limb groups.
     * Fully dynamic — auto-derives pivots, scale, and head metrics from geometry.
     */
    public static ObjMesh parse(InputStream in) {
        ObjMesh mesh = new ObjMesh();

        List<float[]> vertices = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        // Collect raw face indices per group for two-pass processing
        Map<String, List<int[][]>> groupFaces = new LinkedHashMap<>();
        // Track which vertex indices belong to each group (for bounding box calculation)
        Map<String, List<Integer>> groupVertexIndices = new LinkedHashMap<>();
        String currentGroup = "body";
        groupFaces.put(currentGroup, new ArrayList<>());
        groupVertexIndices.put(currentGroup, new ArrayList<>());

        // --- Pass 1: Read all geometry data ---
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
                        currentGroup = parts[1].toLowerCase(Locale.ROOT);
                        groupFaces.computeIfAbsent(currentGroup, k -> new ArrayList<>());
                        groupVertexIndices.computeIfAbsent(currentGroup, k -> new ArrayList<>());
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
                        groupFaces.get(currentGroup).add(new int[][]{vIdx, vtIdx, vnIdx});

                        // Track vertex indices for this group
                        List<Integer> gvIdx = groupVertexIndices.get(currentGroup);
                        for (int vi : vIdx) {
                            gvIdx.add(vi);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DBA] Error reading OBJ stream: " + e.getMessage());
            e.printStackTrace();
        }

        // --- Pass 2: Compute bounding boxes per group ---
        Map<String, float[]> groupBounds = new HashMap<>(); // [minX, maxX, minY, maxY, minZ, maxZ]
        for (Map.Entry<String, List<Integer>> entry : groupVertexIndices.entrySet()) {
            String gname = entry.getKey();
            List<Integer> vIndices = entry.getValue();
            if (vIndices.isEmpty()) continue;

            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            for (int vi : vIndices) {
                if (vi >= 0 && vi < vertices.size()) {
                    float[] v = vertices.get(vi);
                    minX = Math.min(minX, v[0]);
                    maxX = Math.max(maxX, v[0]);
                    minY = Math.min(minY, v[1]);
                    maxY = Math.max(maxY, v[1]);
                    minZ = Math.min(minZ, v[2]);
                    maxZ = Math.max(maxZ, v[2]);
                }
            }

            if (minX <= maxX) {
                groupBounds.put(gname, new float[]{minX, maxX, minY, maxY, minZ, maxZ});
            }
        }

        // --- Pass 3: Auto-derive skeletal pivots and scale ---

        // Use sensible defaults if a group is missing
        float[] bodyB = groupBounds.getOrDefault("body", new float[]{-0.6f, 0.6f, 3.5f, 6.8f, -0.4f, 0.4f});
        float[] headB = groupBounds.getOrDefault("head", new float[]{-0.5f, 0.5f, 6.8f, 8.8f, -0.5f, 0.5f});
        float[] rArmB = groupBounds.getOrDefault("rightarm", new float[]{-1.8f, -1.0f, 4.0f, 6.8f, -0.3f, 0.3f});
        float[] lArmB = groupBounds.getOrDefault("leftarm", new float[]{1.0f, 1.8f, 4.0f, 6.8f, -0.3f, 0.3f});
        float[] rLegB = groupBounds.getOrDefault("rightleg", new float[]{-0.8f, -0.1f, 0.8f, 3.5f, -0.3f, 0.3f});
        float[] lLegB = groupBounds.getOrDefault("leftleg", new float[]{0.1f, 0.8f, 0.8f, 3.5f, -0.3f, 0.3f});

        float bodyMidX = (bodyB[0] + bodyB[1]) * 0.5f;
        float bodyMidZ = (bodyB[4] + bodyB[5]) * 0.5f;
        float bodyTopY = bodyB[3]; // neck line

        // Compute pivots at skeletal joints:
        // - Body: center of body, at the neck/top (Minecraft body pivot is at top)
        // - Head: body center X/Z, bottom of head Y (neck joint)
        // - Arms: inner shoulder edge, top of arm Y, arm center Z
        // - Legs: leg center X, top of leg Y (hip joint), leg center Z
        Map<String, float[]> pivots = new HashMap<>();
        pivots.put("body", new float[]{bodyMidX, bodyTopY, bodyMidZ});
        pivots.put("head", new float[]{bodyMidX, headB[2], bodyMidZ}); // headB[2] = head bottom Y = neck
        pivots.put("rightarm", new float[]{rArmB[1], rArmB[3], (rArmB[4] + rArmB[5]) * 0.5f}); // inner shoulder
        pivots.put("leftarm", new float[]{lArmB[0], lArmB[3], (lArmB[4] + lArmB[5]) * 0.5f}); // inner shoulder
        pivots.put("rightleg", new float[]{(rLegB[0] + rLegB[1]) * 0.5f, rLegB[3], (rLegB[4] + rLegB[5]) * 0.5f}); // hip center
        pivots.put("leftleg", new float[]{(lLegB[0] + lLegB[1]) * 0.5f, lLegB[3], (lLegB[4] + lLegB[5]) * 0.5f}); // hip center

        // Scale: use the BODY TORSO height mapped to Minecraft's 12-pixel body.
        // This avoids head decorations (antennae, horns) or feet decorations inflating scale.
        // The body group spans from waist/hips to neck. Minecraft body = 12 pixels = 0.75 blocks.
        float bodyHeight = bodyTopY - bodyB[2]; // from bottom of body to top (neck)
        if (bodyHeight <= 0.001f) bodyHeight = 3.364f; // fallback

        // Scale: 12 pixels (body height in MC) = bodyHeight OBJ units
        // 1 pixel = 1/16 block, so 12 pixels = 12/16 = 0.75 blocks
        final float scale = (12.0f / bodyHeight) * 0.0625f;

        mesh.modelScale = scale;

        // --- Compute head metrics for eye/hair placement ---
        // Use head vertices that are NEAR the body center (within 1.5x body width)
        // to avoid antennae/horns/decorations inflating the skull metrics
        float bodyWidth = bodyB[1] - bodyB[0];
        float skullMarginX = bodyWidth * 0.9f; // how far from body center a vertex can be to count as "skull"

        float skullMinX = Float.MAX_VALUE, skullMaxX = -Float.MAX_VALUE;
        float skullMinY = Float.MAX_VALUE, skullMaxY = -Float.MAX_VALUE;
        float skullMinZ = Float.MAX_VALUE, skullMaxZ = -Float.MAX_VALUE;
        boolean hasSkullVerts = false;

        List<Integer> headVertIndices = groupVertexIndices.getOrDefault("head", Collections.emptyList());
        for (int vi : headVertIndices) {
            if (vi >= 0 && vi < vertices.size()) {
                float[] v = vertices.get(vi);
                // A vertex is "core skull" if its X position is within the body width from body center
                float distFromCenter = Math.abs(v[0] - bodyMidX);
                if (distFromCenter <= skullMarginX) {
                    skullMinX = Math.min(skullMinX, v[0]);
                    skullMaxX = Math.max(skullMaxX, v[0]);
                    skullMinY = Math.min(skullMinY, v[1]);
                    skullMaxY = Math.max(skullMaxY, v[1]);
                    skullMinZ = Math.min(skullMinZ, v[2]);
                    skullMaxZ = Math.max(skullMaxZ, v[2]);
                    hasSkullVerts = true;
                }
            }
        }

        // Fallback: use full head bounds if no core skull verts found
        if (!hasSkullVerts) {
            skullMinX = headB[0]; skullMaxX = headB[1];
            skullMinY = headB[2]; skullMaxY = headB[3];
            skullMinZ = headB[4]; skullMaxZ = headB[5];
        }

        mesh.headMinX = skullMinX;
        mesh.headMaxX = skullMaxX;
        mesh.headMinY = skullMinY;
        mesh.headMaxY = skullMaxY;
        mesh.headMinZ = skullMinZ;
        mesh.headMaxZ = skullMaxZ;
        mesh.topOfHeadY = -(skullMaxY - skullMinY) * scale;
        mesh.headHeight = Math.max(0.2f, (skullMaxY - skullMinY) * scale);
        mesh.headWidth = Math.max(0.2f, (skullMaxX - skullMinX) * scale);
        mesh.faceFrontZ = -(skullMaxZ - bodyMidZ) * scale;

        // --- Pass 4: Bake faces with correct winding and pivot-relative positioning ---
        for (Map.Entry<String, List<int[][]>> entry : groupFaces.entrySet()) {
            String gname = entry.getKey();
            LimbGroup group = mesh.limbs.computeIfAbsent(gname, LimbGroup::new);
            float[] pivot = pivots.getOrDefault(gname, pivots.get("body"));
            float px = pivot[0];
            float py = pivot[1];
            float pz = pivot[2];

            for (int[][] face : entry.getValue()) {
                int[] vIdx = face[0];
                int[] vtIdx = face[1];
                int[] vnIdx = face[2];
                int count = vIdx.length;

                if (count == 4) {
                    QuadFace q = new QuadFace();
                    // Reversed winding order for correct face orientation in Minecraft's renderer
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
                    group.faces.add(q);
                } else if (count == 3) {
                    QuadFace q = new QuadFace();
                    // Reversed winding for tris (degenerate quad: last vertex doubled)
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
                    group.faces.add(q);
                }
            }
        }

        System.out.println("[DBA] Parsed OBJ: " + mesh.limbs.size() + " groups, scale=" + 
                String.format("%.4f", scale) + ", bodyH=" + String.format("%.2f", bodyHeight) +
                ", skullY=[" + String.format("%.2f", skullMinY) + "," + String.format("%.2f", skullMaxY) + "]");

        return mesh;
    }
}
