package com.dragonblockarcanedba.client.compat;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.render.DynamicSkinManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Client-side Better Player Model (BPM) compatibility.
 * Hooks into SpecialPlayerRenderEvent using reflection to inject real-time dynamically
 * recolored Yardrat textures and requests model synchronization without hard compile-time dependencies.
 */
public final class BpmCompatClient {

    private BpmCompatClient() {}

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("better_player_model")) {
            return;
        }
        try {
            initEvents();
        } catch (Throwable ignored) {}
    }

    private static void initEvents() {
        try {
            Class<?> eventClass = Class.forName("com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent");
            Class<?> handlerInterface = Class.forName("com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent$RenderHandler");
            Field eventField = eventClass.getField("EVENT");
            Object eventInstance = eventField.get(null);

            Class<?> eventResultClass = Class.forName("dev.ysm.architectury.event.EventResult");
            Method passMethod = eventResultClass.getMethod("pass");
            Object passResult = passMethod.invoke(null);

            Object handlerProxy = Proxy.newProxyInstance(
                handlerInterface.getClassLoader(),
                new Class<?>[]{handlerInterface},
                (proxy, method, args) -> {
                    if (args != null && args.length > 0) {
                        Object renderEvent = args[0];
                        try {
                            Method getPlayerMethod = eventClass.getMethod("getPlayer");
                            Player player = (Player) getPlayerMethod.invoke(renderEvent);
                            if (player == null) {
                                player = Minecraft.getInstance().player;
                            }
                            if (player instanceof PlayerStatsAccessor accessor) {
                                int skin = parseHexColor(accessor.dba$getSkinColor(), 0xFFE0BD);
                                int hair = parseHexColor(accessor.dba$getHairColor(), 0xFFF08C);
                                Identifier dynamicTex = DynamicSkinManager.getOrGenerateSkin(skin, hair);
                                Method setTextureLocationMethod = eventClass.getMethod("setTextureLocation", Identifier.class);
                                setTextureLocationMethod.invoke(renderEvent, dynamicTex);
                            }
                        } catch (Throwable ignored) {}
                    }
                    return passResult;
                }
            );

            Method registerMethod = eventInstance.getClass().getMethod("register", Object.class);
            registerMethod.invoke(eventInstance, handlerProxy);
        } catch (Throwable ignored) {}
    }

    public static void enforceModel(String requestedId) {
        if (!FabricLoader.getInstance().isModLoaded("better_player_model")) {
            return;
        }
        try {
            String target = requestedId;
            try {
                Class<?> clientModelManagerClass = Class.forName("com.elfmcys.yesstevemodel.client.ClientModelManager");
                Method getModelDefinitions = clientModelManagerClass.getMethod("getModelDefinitions");
                java.util.Map<?, ?> modelDefs = (java.util.Map<?, ?>) getModelDefinitions.invoke(null);
                if (modelDefs != null) {
                    if (modelDefs.containsKey(requestedId)) {
                        target = requestedId;
                    } else if (modelDefs.containsKey("custom:" + requestedId)) {
                        target = "custom:" + requestedId;
                    } else if (requestedId.startsWith("custom:") && modelDefs.containsKey(requestedId.substring(7))) {
                        target = requestedId.substring(7);
                    } else {
                        for (Object key : modelDefs.keySet()) {
                            String k = String.valueOf(key);
                            if (k.equalsIgnoreCase(requestedId) || k.endsWith(":" + requestedId)) {
                                target = k;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            Class<?> packetClass = Class.forName("com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket");
            Object packet = packetClass.getConstructor(String.class, String.class).newInstance(target, "default");
            Class<?> networkHandlerClass = Class.forName("com.elfmcys.yesstevemodel.network.NetworkHandler");
            Method sendMethod = networkHandlerClass.getMethod("sendToServer", Object.class);
            sendMethod.invoke(null, packet);
        } catch (Throwable ignored) {}
    }

    public static void enforceYardratModel() {
        enforceModel("universal_humanoid");
    }

    private static int parseHexColor(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return defaultColor;
        }
    }
}
