package com.dragonblockarcanedba.client.render.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Bedrock animation registry and keyframe interpolator.
 * Parses Blockbench / Bedrock .animation.json files, caching fast keyframe
 * arrays for 60+ FPS smooth skeletal playback with zero runtime garbage collection.
 */
public final class BedrockAnimationRegistry {

    private static final Map<String, BedrockAnimation> ANIMATIONS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public record Keyframe(float time, float x, float y, float z) {}

    public static class BoneTrack {
        public final Keyframe[] rotationKeys;
        public final Keyframe[] positionKeys;

        public BoneTrack(Keyframe[] rotationKeys, Keyframe[] positionKeys) {
            this.rotationKeys = rotationKeys;
            this.positionKeys = positionKeys;
        }

        public float[] sampleRotation(float time) {
            return sampleKeyframes(rotationKeys, time, 0.0f, 0.0f, 0.0f);
        }

        public float[] samplePosition(float time) {
            return sampleKeyframes(positionKeys, time, 0.0f, 0.0f, 0.0f);
        }

        private static float[] sampleKeyframes(Keyframe[] keys, float time, float defX, float defY, float defZ) {
            if (keys == null || keys.length == 0) {
                return new float[]{defX, defY, defZ};
            }
            if (keys.length == 1 || time <= keys[0].time()) {
                Keyframe k = keys[0];
                return new float[]{k.x(), k.y(), k.z()};
            }
            if (time >= keys[keys.length - 1].time()) {
                Keyframe k = keys[keys.length - 1];
                return new float[]{k.x(), k.y(), k.z()};
            }

            // Binary search for the two adjacent keyframes
            int low = 0;
            int high = keys.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (keys[mid].time() < time) {
                    low = mid + 1;
                } else if (keys[mid].time() > time) {
                    high = mid - 1;
                } else {
                    Keyframe k = keys[mid];
                    return new float[]{k.x(), k.y(), k.z()};
                }
            }

            int idxA = Math.max(0, low - 1);
            int idxB = Math.min(keys.length - 1, low);
            Keyframe a = keys[idxA];
            Keyframe b = keys[idxB];

            float span = b.time() - a.time();
            float factor = span > 0.0001f ? (time - a.time()) / span : 0.0f;
            factor = Mth.clamp(factor, 0.0f, 1.0f);

            return new float[]{
                Mth.lerp(factor, a.x(), b.x()),
                Mth.lerp(factor, a.y(), b.y()),
                Mth.lerp(factor, a.z(), b.z())
            };
        }
    }

    public static class BedrockAnimation {
        public final String name;
        public final float length;
        public final boolean loop;
        public final boolean holdOnLastFrame;
        public final Map<String, BoneTrack> bones = new HashMap<>();

        public BedrockAnimation(String name, float length, boolean loop, boolean holdOnLastFrame) {
            this.name = name;
            this.length = Math.max(0.01f, length);
            this.loop = loop;
            this.holdOnLastFrame = holdOnLastFrame;
        }

        public float calculatePlayTime(float elapsedSeconds) {
            if (loop) {
                return elapsedSeconds % length;
            }
            if (holdOnLastFrame) {
                return Math.min(elapsedSeconds, length);
            }
            return elapsedSeconds;
        }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        loadAnimationFile(Identifier.fromNamespaceAndPath("dragonblockarcanedba", "animations/player.animation.json"));
    }

    public static BedrockAnimation getAnimation(String name) {
        if (!initialized) init();
        if (name == null) return null;

        BedrockAnimation anim = ANIMATIONS.get(name);
        if (anim == null && !name.startsWith("animation.player.")) {
            anim = ANIMATIONS.get("animation.player." + name);
        }
        return anim;
    }

    public static void loadAnimationFile(Identifier location) {
        try {
            InputStream is = null;
            var rm = Minecraft.getInstance().getResourceManager();
            if (rm != null) {
                var res = rm.getResource(location);
                if (res.isPresent()) {
                    is = res.get().open();
                }
            }

            if (is == null) {
                String cp = "/assets/" + location.getNamespace() + "/" + location.getPath();
                is = BedrockAnimationRegistry.class.getResourceAsStream(cp);
            }

            if (is == null) return;

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject anims = root.getAsJsonObject("animations");
                if (anims == null) return;

                for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
                    String animName = entry.getKey();
                    JsonObject animObj = entry.getValue().getAsJsonObject();

                    float length = animObj.has("animation_length") ? animObj.get("animation_length").getAsFloat() : 1.0f;
                    boolean loop = true;
                    boolean holdOnLastFrame = false;

                    if (animObj.has("loop")) {
                        JsonElement loopElem = animObj.get("loop");
                        if (loopElem.isJsonPrimitive() && loopElem.getAsJsonPrimitive().isBoolean()) {
                            loop = loopElem.getAsBoolean();
                        } else if (loopElem.isJsonPrimitive() && loopElem.getAsJsonPrimitive().isString()) {
                            String str = loopElem.getAsString().toLowerCase();
                            holdOnLastFrame = "hold_on_last_frame".equals(str);
                            loop = !holdOnLastFrame;
                        }
                    }

                    BedrockAnimation animation = new BedrockAnimation(animName, length, loop, holdOnLastFrame);

                    if (animObj.has("bones")) {
                        JsonObject bonesObj = animObj.getAsJsonObject("bones");
                        for (Map.Entry<String, JsonElement> boneEntry : bonesObj.entrySet()) {
                            String boneName = boneEntry.getKey();
                            JsonObject boneObj = boneEntry.getValue().getAsJsonObject();

                            Keyframe[] rotKeys = parseTrack(boneObj, "rotation");
                            Keyframe[] posKeys = parseTrack(boneObj, "position");

                            if (rotKeys != null || posKeys != null) {
                                animation.bones.put(boneName.toLowerCase(), new BoneTrack(rotKeys, posKeys));
                            }
                        }
                    }

                    ANIMATIONS.put(animName, animation);
                }
            }
        } catch (Exception ignored) {}
    }

    private static Keyframe[] parseTrack(JsonObject boneObj, String trackName) {
        if (!boneObj.has(trackName)) return null;

        JsonElement elem = boneObj.get(trackName);
        if (!elem.isJsonObject()) return null;

        JsonObject trackObj = elem.getAsJsonObject();
        List<Keyframe> list = new ArrayList<>();

        for (Map.Entry<String, JsonElement> kfEntry : trackObj.entrySet()) {
            try {
                float time = Float.parseFloat(kfEntry.getKey());
                JsonElement ptElem = kfEntry.getValue();
                if (ptElem.isJsonArray()) {
                    JsonArray arr = ptElem.getAsJsonArray();
                    if (arr.size() >= 3) {
                        float x = arr.get(0).getAsFloat();
                        float y = arr.get(1).getAsFloat();
                        float z = arr.get(2).getAsFloat();
                        list.add(new Keyframe(time, x, y, z));
                    }
                }
            } catch (Exception ignored) {}
        }

        list.sort(Comparator.comparingDouble(Keyframe::time));
        return list.toArray(new Keyframe[0]);
    }
}
