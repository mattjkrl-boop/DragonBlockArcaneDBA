package com.dragonblockarcanedba.client.dev;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Dev-only preview camera for model review screenshots.
 * /dbacam front|back|side|yaw <deg>|reset orbits the third-person camera around the local player
 * without touching the player's own rotation. Only registered in a development environment.
 */
public final class DevPreviewCamera {
    public static volatile float yawOffset = 0f;

    private DevPreviewCamera() {}

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
            ClientCommands.literal("dbacam")
                .then(ClientCommands.literal("front").executes(c -> set(c.getSource(), 180f)))
                .then(ClientCommands.literal("back").executes(c -> set(c.getSource(), 0f)))
                .then(ClientCommands.literal("side").executes(c -> set(c.getSource(), 90f)))
                .then(ClientCommands.literal("yaw")
                    .then(ClientCommands.argument("deg", FloatArgumentType.floatArg())
                        .executes(c -> set(c.getSource(), FloatArgumentType.getFloat(c, "deg")))))
                .then(ClientCommands.literal("reset").executes(c -> {
                    yawOffset = 0f;
                    Minecraft mc = Minecraft.getInstance();
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    return 1;
                }))));
    }

    private static int set(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, float deg) {
        yawOffset = deg;
        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        return 1;
    }
}
