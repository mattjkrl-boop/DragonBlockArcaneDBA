package com.dragonblockarcanedba.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
    @SuppressWarnings("rawtypes")
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory) java.lang.reflect.Proxy.newProxyInstance(
                ConfigScreenFactory.class.getClassLoader(),
                new Class[]{ConfigScreenFactory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("create")) {
                        try {
                            return ConfigScreen.class.getDeclaredConstructor(Screen.class).newInstance(args[0]);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }
        );
    }

    private static class ConfigScreen extends Screen {
        private final Screen parent;

        protected ConfigScreen(Screen parent) {
            super(Component.literal("Dragon Block Arcane DBA Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = this.width / 2;
            int y = this.height / 2;

            // Toggle Visuals Button
            addRenderableWidget(Button.builder(
                Component.literal("Aura Visuals: " + (DbaConfig.chargeVisualsEnabled ? "ON" : "OFF")),
                btn -> {
                    DbaConfig.chargeVisualsEnabled = !DbaConfig.chargeVisualsEnabled;
                    btn.setMessage(Component.literal("Aura Visuals: " + (DbaConfig.chargeVisualsEnabled ? "ON" : "OFF")));
                    DbaConfig.save();
                }
            ).bounds(x - 100, y - 40, 200, 20).build());

            // Cycle Ki Recovery Multiplier
            addRenderableWidget(Button.builder(
                Component.literal("Ki Recovery: " + DbaConfig.baseKiRecoveryMultiplier + "x"),
                btn -> {
                    if (DbaConfig.baseKiRecoveryMultiplier == 1.0) {
                        DbaConfig.baseKiRecoveryMultiplier = 1.5;
                    } else if (DbaConfig.baseKiRecoveryMultiplier == 1.5) {
                        DbaConfig.baseKiRecoveryMultiplier = 2.0;
                    } else if (DbaConfig.baseKiRecoveryMultiplier == 2.0) {
                        DbaConfig.baseKiRecoveryMultiplier = 0.5;
                    } else {
                        DbaConfig.baseKiRecoveryMultiplier = 1.0;
                    }
                    btn.setMessage(Component.literal("Ki Recovery: " + DbaConfig.baseKiRecoveryMultiplier + "x"));
                    DbaConfig.save();
                }
            ).bounds(x - 100, y - 10, 200, 20).build());

            // Cycle Stat Gain Multiplier
            addRenderableWidget(Button.builder(
                Component.literal("Stat Gain: " + DbaConfig.statGainMultiplier + "x"),
                btn -> {
                    if (DbaConfig.statGainMultiplier == 1.0) {
                        DbaConfig.statGainMultiplier = 1.5;
                    } else if (DbaConfig.statGainMultiplier == 1.5) {
                        DbaConfig.statGainMultiplier = 2.0;
                    } else if (DbaConfig.statGainMultiplier == 2.0) {
                        DbaConfig.statGainMultiplier = 0.5;
                    } else {
                        DbaConfig.statGainMultiplier = 1.0;
                    }
                    btn.setMessage(Component.literal("Stat Gain: " + DbaConfig.statGainMultiplier + "x"));
                    DbaConfig.save();
                }
            ).bounds(x - 100, y + 20, 200, 20).build());

            // Save & Close Button
            addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(parent);
                    }
                }
            ).bounds(x - 100, y + 60, 200, 20).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
            super.extractRenderState(extractor, mouseX, mouseY, delta);
            extractor.centeredText(this.font, this.title.getString(), this.width / 2, 20, 0xFFFFAA00);
        }
    }
}
