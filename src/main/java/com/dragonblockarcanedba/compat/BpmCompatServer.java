package com.dragonblockarcanedba.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Server-side Better Player Model (BPM) compatibility.
 * Uses reflection to ensure that players are assigned the Yardrat / race model rig
 * without compile-time hard dependencies or IDE classpath errors.
 */
public final class BpmCompatServer {

    private BpmCompatServer() {}

    public static void onPlayerJoin(ServerPlayer player) {
        if (!FabricLoader.getInstance().isModLoaded("better_player_model")) {
            return;
        }
        try {
            ensureYardratModel(player);
        } catch (Throwable ignored) {}
    }

    private static String resolveModelId(String requested) {
        try {
            Class<?> serverModelManagerClass = Class.forName("com.elfmcys.yesstevemodel.model.ServerModelManager");
            Method getInfoMethod = serverModelManagerClass.getMethod("getServerModelInfo");
            java.util.Map<?, ?> infoMap = (java.util.Map<?, ?>) getInfoMethod.invoke(null);
            if (infoMap != null) {
                if (infoMap.containsKey(requested)) return requested;
                if (infoMap.containsKey("custom:" + requested)) return "custom:" + requested;
                if (requested.startsWith("custom:") && infoMap.containsKey(requested.substring(7))) return requested.substring(7);
                for (Object key : infoMap.keySet()) {
                    String k = String.valueOf(key);
                    if (k.equalsIgnoreCase(requested) || k.endsWith(":" + requested)) {
                        return k;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return requested;
    }

    private static void ensureYardratModel(ServerPlayer player) {
        try {
            Class<?> capClass = Class.forName("com.elfmcys.yesstevemodel.capability.ModelInfoCapability");
            Method getMethod = capClass.getMethod("get", net.minecraft.world.entity.player.Player.class);
            Optional<?> opt = (Optional<?>) getMethod.invoke(null, player);
            if (opt.isPresent()) {
                Object cap = opt.get();
                Method getModelId = capClass.getMethod("getModelId");
                String currentModel = (String) getModelId.invoke(cap);
                if (currentModel == null || currentModel.isEmpty() || "default".equals(currentModel)) {
                    String target = resolveModelId("universal_humanoid");
                    Method setModel = capClass.getMethod("setModelAndTexture", String.class, String.class);
                    setModel.invoke(cap, target, "default");
                    Method setDisabled = capClass.getMethod("setDisabled", boolean.class);
                    setDisabled.invoke(cap, false);
                    Method markDirty = capClass.getMethod("markDirty");
                    markDirty.invoke(cap);
                    Class<?> packetClass = Class.forName("com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket");
                    Method sendMethod = packetClass.getMethod("sendImmediateModelState", ServerPlayer.class, capClass);
                    sendMethod.invoke(null, player, cap);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void switchPlayerModel(ServerPlayer player, String modelId) {
        if (!FabricLoader.getInstance().isModLoaded("better_player_model")) {
            return;
        }
        try {
            String resolved = resolveModelId(modelId);
            Class<?> capClass = Class.forName("com.elfmcys.yesstevemodel.capability.ModelInfoCapability");
            Method getMethod = capClass.getMethod("get", net.minecraft.world.entity.player.Player.class);
            Optional<?> opt = (Optional<?>) getMethod.invoke(null, player);
            if (opt.isPresent()) {
                Object cap = opt.get();
                Method setModel = capClass.getMethod("setModelAndTexture", String.class, String.class);
                setModel.invoke(cap, resolved, "default");
                Method setDisabled = capClass.getMethod("setDisabled", boolean.class);
                setDisabled.invoke(cap, false);
                Method markDirty = capClass.getMethod("markDirty");
                markDirty.invoke(cap);
                Class<?> packetClass = Class.forName("com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket");
                Method sendMethod = packetClass.getMethod("sendImmediateModelState", ServerPlayer.class, capClass);
                sendMethod.invoke(null, player, cap);
            }
        } catch (Throwable ignored) {}
    }
}
