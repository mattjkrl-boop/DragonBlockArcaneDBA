package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import com.dragonblockarcanedba.registry.Race;
import java.util.List;

public class RaceSelectionScreen extends Screen {
    private enum State {
        RACE_SELECT,
        CUSTOMIZATION
    }

    private State currentState = State.RACE_SELECT;
    private String selectedRace = "";
    private String customizationTab = "Skin";
    
    private int skinR = 255, skinG = 204, skinB = 153;
    private int hairR = 15, hairG = 15, hairB = 15;
    
    private RgbSliderWidget skinRedSlider, skinGreenSlider, skinBlueSlider;
    private RgbSliderWidget hairRedSlider, hairGreenSlider, hairBlueSlider;
    
    private float spinAngle = 0f;

    private static final List<RaceOption> RACES = List.of(
        new RaceOption("Saiyan", "dragonblockarcanedba:saiyan"),
        new RaceOption("Human", "dragonblockarcanedba:human"),
        new RaceOption("Namekian", "dragonblockarcanedba:namekian"),
        new RaceOption("Arcosian", "dragonblockarcanedba:arcosian"),
        new RaceOption("Half-Saiyan", "dragonblockarcanedba:half_saiyan"),
        new RaceOption("Yardrat", "dragonblockarcanedba:yardrat"),
        new RaceOption("Majin", "dragonblockarcanedba:majin"),
        new RaceOption("Bio-Android", "dragonblockarcanedba:bio_android"),
        new RaceOption("Tuffle", "dragonblockarcanedba:tuffle")
    );

    public RaceSelectionScreen() {
        super(Component.literal("Character Creation"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        
        int leftColWidth = this.width * 2 / 5;
        int midColWidth = this.width * 1 / 5;
        int rightColWidth = this.width * 2 / 5;
        int midStartX = leftColWidth;
        int rightStartX = leftColWidth + midColWidth;

        int btnWidth = 80;
        int btnHeight = 20;
        int spacingY = 22;

        if (currentState == State.RACE_SELECT) {
            // Middle Column: Race Buttons
            int startY = (this.height - (RACES.size() * spacingY)) / 2;
            if (startY < 10) startY = 10; // safety

            for (int i = 0; i < RACES.size(); i++) {
                RaceOption race = RACES.get(i);
                int x = midStartX + (midColWidth - btnWidth) / 2;
                int y = startY + i * spacingY;

                addRenderableWidget(Button.builder(
                    Component.literal(race.name),
                    btn -> {
                        selectedRace = race.id;
                        init(); // Refresh to show confirm button if needed
                    }
                ).bounds(x, y, btnWidth, btnHeight).build());
            }

            // Right Column: Confirm Button
            if (!selectedRace.isEmpty()) {
                addRenderableWidget(Button.builder(
                    Component.literal("CONFIRM"),
                    btn -> {
                        currentState = State.CUSTOMIZATION;
                        init();
                    }
                ).bounds(rightStartX + rightColWidth - btnWidth - 20, this.height - 40, btnWidth, 20).build());
            }

        } else if (currentState == State.CUSTOMIZATION) {
            // Middle Column: Customization Tabs
            String[] tabs = {"Skin", "Hair"};
            int startY = (this.height - (tabs.length * spacingY)) / 2;
            for (int i = 0; i < tabs.length; i++) {
                String tab = tabs[i];
                int x = midStartX + (midColWidth - btnWidth) / 2;
                int y = startY + i * spacingY;

                addRenderableWidget(Button.builder(
                    Component.literal(tab),
                    btn -> {
                        customizationTab = tab;
                        init();
                    }
                ).bounds(x, y, btnWidth, btnHeight).build());
            }

            // Right Column: Interactive Color Sliders & Preset Swatches
            int sliderX = rightStartX + 20;
            int sliderW = rightColWidth - 40;
            int panelY = 40;
            int startSliderY = panelY + 45; // Start below header

            if ("Skin".equals(customizationTab)) {
                // Red, Green, Blue sliders for Skin
                skinRedSlider = new RgbSliderWidget(sliderX, startSliderY, sliderW, 18, "Red", skinR, val -> skinR = val);
                skinGreenSlider = new RgbSliderWidget(sliderX, startSliderY + 22, sliderW, 18, "Green", skinG, val -> skinG = val);
                skinBlueSlider = new RgbSliderWidget(sliderX, startSliderY + 44, sliderW, 18, "Blue", skinB, val -> skinB = val);

                addRenderableWidget(skinRedSlider);
                addRenderableWidget(skinGreenSlider);
                addRenderableWidget(skinBlueSlider);

                // Quick Preset Swatches for Skin (2 rows of 3 buttons for optimal readability)
                PresetColor[] skinPresets = {
                    new PresetColor("Light", 255, 204, 153),
                    new PresetColor("Tan", 210, 180, 140),
                    new PresetColor("Dark", 141, 85, 36),
                    new PresetColor("Namek", 85, 255, 120),
                    new PresetColor("Majin", 255, 140, 200),
                    new PresetColor("Arcosian", 230, 230, 250)
                };

                int swatchStartY = startSliderY + 86; // Positioned below "Preset Palette:" label
                int cols = 3;
                int swatchW = (sliderW - (cols - 1) * 4) / cols;
                int swatchH = 18;

                for (int i = 0; i < skinPresets.length; i++) {
                    PresetColor p = skinPresets[i];
                    int row = i / cols;
                    int col = i % cols;
                    int bx = sliderX + col * (swatchW + 4);
                    int by = swatchStartY + row * (swatchH + 4);

                    addRenderableWidget(Button.builder(
                        Component.literal(p.name),
                        btn -> {
                            skinR = p.r; skinG = p.g; skinB = p.b;
                            skinRedSlider.setIntValue(p.r);
                            skinGreenSlider.setIntValue(p.g);
                            skinBlueSlider.setIntValue(p.b);
                        }
                    ).bounds(bx, by, swatchW, swatchH).build());
                }

            } else if ("Hair".equals(customizationTab)) {
                // Red, Green, Blue sliders for Hair
                hairRedSlider = new RgbSliderWidget(sliderX, startSliderY, sliderW, 18, "Red", hairR, val -> hairR = val);
                hairGreenSlider = new RgbSliderWidget(sliderX, startSliderY + 22, sliderW, 18, "Green", hairG, val -> hairG = val);
                hairBlueSlider = new RgbSliderWidget(sliderX, startSliderY + 44, sliderW, 18, "Blue", hairB, val -> hairB = val);

                addRenderableWidget(hairRedSlider);
                addRenderableWidget(hairGreenSlider);
                addRenderableWidget(hairBlueSlider);

                // Quick Preset Swatches for Hair (2 rows of 3 buttons)
                PresetColor[] hairPresets = {
                    new PresetColor("Black", 15, 15, 15),
                    new PresetColor("Gold", 255, 215, 0),
                    new PresetColor("Brown", 80, 45, 20),
                    new PresetColor("Red", 200, 30, 30),
                    new PresetColor("Blue", 30, 180, 255),
                    new PresetColor("White", 240, 240, 240)
                };

                int swatchStartY = startSliderY + 86;
                int cols = 3;
                int swatchW = (sliderW - (cols - 1) * 4) / cols;
                int swatchH = 18;

                for (int i = 0; i < hairPresets.length; i++) {
                    PresetColor p = hairPresets[i];
                    int row = i / cols;
                    int col = i % cols;
                    int bx = sliderX + col * (swatchW + 4);
                    int by = swatchStartY + row * (swatchH + 4);

                    addRenderableWidget(Button.builder(
                        Component.literal(p.name),
                        btn -> {
                            hairR = p.r; hairG = p.g; hairB = p.b;
                            hairRedSlider.setIntValue(p.r);
                            hairGreenSlider.setIntValue(p.g);
                            hairBlueSlider.setIntValue(p.b);
                        }
                    ).bounds(bx, by, swatchW, swatchH).build());
                }
            }

            // Right Column: Back and Confirm Buttons
            addRenderableWidget(Button.builder(
                Component.literal("BACK"),
                btn -> {
                    currentState = State.RACE_SELECT;
                    init();
                }
            ).bounds(rightStartX + rightColWidth - btnWidth * 2 - 30, this.height - 40, btnWidth, 20).build());

            addRenderableWidget(Button.builder(
                Component.literal("CONFIRM"),
                btn -> {
                    String skinHex = String.format("#%02X%02X%02X", skinR, skinG, skinB);
                    String hairHex = String.format("#%02X%02X%02X", hairR, hairG, hairB);

                    CompoundTag data = new CompoundTag();
                    data.putString("action", "select_race");
                    data.putString("race", selectedRace);
                    data.putString("skin_color", skinHex);
                    data.putString("hair_color", hairHex);
                    ClientPlayNetworking.send(new ActionPayload(data));
                    this.onClose();
                }
            ).bounds(rightStartX + rightColWidth - btnWidth - 20, this.height - 40, btnWidth, 20).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        spinAngle += 2.0f;
        if (spinAngle >= 360f) {
            spinAngle -= 360f;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw background before widgets
        context.fill(0, 0, this.width, this.height, 0xF0101216);

        int leftColWidth = this.width * 2 / 5;
        int midColWidth = this.width * 1 / 5;
        int rightColWidth = this.width * 2 / 5;
        int midStartX = leftColWidth;
        int rightStartX = leftColWidth + midColWidth;

        int borderColor = 0xFF00FFCC;

        // Draw top header bar
        context.fill(0, 0, this.width, 20, 0xFF0A0C0E);
        context.fill(0, 20, this.width, 22, borderColor);
        
        String headerTitle = currentState == State.RACE_SELECT ? "RACE SELECTION" : "CHARACTER CUSTOMIZATION";
        context.centeredText(this.font, Component.literal(headerTitle), this.width / 2, 6, 0xFFFFFFFF);

        // Left Column: 3D Player Model Preview Platform
        int previewX = leftColWidth / 2;
        int previewY = this.height / 2;
        context.centeredText(this.font, Component.literal("3D CHARACTER PREVIEW"), previewX, previewY - 85, 0xFF00FFCC);

        // Circular glow platform under player
        context.fill(previewX - 45, previewY + 68, previewX + 45, previewY + 70, 0xFF00FFCC);
        context.fill(previewX - 35, previewY + 70, previewX + 35, previewY + 72, 0xAA00FFCC);

        var localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer != null) {
            float savedYRot = localPlayer.getYRot();
            float savedYBodyRot = localPlayer.yBodyRot;
            float savedYHeadRot = localPlayer.yHeadRot;
            float savedXRot = localPlayer.getXRot();

            // Set spinning rotation
            localPlayer.setYRot(spinAngle);
            localPlayer.yBodyRot = spinAngle;
            localPlayer.yHeadRot = spinAngle;
            localPlayer.setXRot(0f);

            int scale = 40;
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                context,
                previewX - 40, previewY - 60,
                previewX + 40, previewY + 70,
                scale,
                0f,
                (float)mouseX, (float)mouseY,
                localPlayer
            );

            // Restore player angles
            localPlayer.setYRot(savedYRot);
            localPlayer.yBodyRot = savedYBodyRot;
            localPlayer.yHeadRot = savedYHeadRot;
            localPlayer.setXRot(savedXRot);
        }

        // Right Column: Details Panel Background
        int panelMargin = 10;
        int panelX = rightStartX + panelMargin;
        int panelY = 40;
        int panelW = rightColWidth - panelMargin * 2;
        int panelH = this.height - 90; // leave room for buttons at bottom
        
        // Panel Background and border
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0181B22);
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, borderColor);
        context.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, borderColor);
        context.fill(panelX, panelY, panelX + 2, panelY + panelH, borderColor);
        context.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, borderColor);

        if (currentState == State.RACE_SELECT) {
            if (!selectedRace.isEmpty()) {
                String friendlyName = "";
                for (RaceOption r : RACES) {
                    if (r.id.equals(selectedRace)) friendlyName = r.name;
                }
                context.text(this.font, Component.literal(friendlyName + " Race Details"), panelX + 12, panelY + 12, 0xFF00FFCC, false);
                
                Race raceObj = com.dragonblockarcanedba.registry.DbaRegistries.getRace(net.minecraft.resources.Identifier.parse(selectedRace));
                if (raceObj != null) {
                    var bs = raceObj.getBaseStats();
                    var sm = raceObj.getStatMultipliers();
                    context.text(this.font, Component.literal("• Strength: Base " + bs.strength() + " (+" + sm.strength() + "%)"), panelX + 15, panelY + 38, 0xFFFFFFFF, false);
                    context.text(this.font, Component.literal("• Defense: Base " + bs.defense() + " (+" + sm.defense() + "%)"), panelX + 15, panelY + 50, 0xFFFFFFFF, false);
                    context.text(this.font, Component.literal("• Ki Capacity: Base " + bs.kiCapacity() + " (+" + sm.kiCapacity() + "%)"), panelX + 15, panelY + 62, 0xFFFFFFFF, false);
                    context.text(this.font, Component.literal("• Ki Control: Base " + bs.kiControl() + " (+" + sm.kiControl() + "%)"), panelX + 15, panelY + 74, 0xFFFFFFFF, false);
                    context.text(this.font, Component.literal("• Agility: Base " + bs.agility() + " (+" + sm.agility() + "%)"), panelX + 15, panelY + 86, 0xFFFFFFFF, false);
                }
                context.text(this.font, Component.literal("Unique Traits:"), panelX + 12, panelY + 110, 0xFF00FFCC, false);
                context.text(this.font, Component.literal("Race-specific forms & power scaling"), panelX + 15, panelY + 124, 0xFFAAAAAA, false);
                context.text(this.font, Component.literal("unlocked through combat mastery."), panelX + 15, panelY + 136, 0xFFAAAAAA, false);
            } else {
                context.centeredText(this.font, Component.literal("Select a race to view details"), panelX + panelW / 2, panelY + panelH / 2, 0xFFAAAAAA);
            }
        } else if (currentState == State.CUSTOMIZATION) {
            int startSliderY = panelY + 45;
            if ("Skin".equals(customizationTab)) {
                context.text(this.font, Component.literal("SKIN TINCTURE PICKER"), panelX + 12, panelY + 10, 0xFF00FFCC, false);
                String skinHex = String.format("#%02X%02X%02X", skinR, skinG, skinB);
                context.text(this.font, Component.literal("Active Hex: " + skinHex), panelX + 12, panelY + 24, 0xFFAAAAAA, false);

                // Draw Color Preview Swatch Box on right side of header
                int argb = 0xFF000000 | (skinR << 16) | (skinG << 8) | skinB;
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 10, panelY + 40, argb);
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 10, panelY + 9, borderColor);
                context.fill(panelX + panelW - 44, panelY + 39, panelX + panelW - 10, panelY + 40, borderColor);
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 43, panelY + 40, borderColor);
                context.fill(panelX + panelW - 11, panelY + 8, panelX + panelW - 10, panelY + 40, borderColor);

                context.text(this.font, Component.literal("Preset Palette:"), panelX + 12, startSliderY + 70, 0xFF00FFCC, false);
            } else if ("Hair".equals(customizationTab)) {
                context.text(this.font, Component.literal("FOLLICLE COLOR PICKER"), panelX + 12, panelY + 10, 0xFF00FFCC, false);
                String hairHex = String.format("#%02X%02X%02X", hairR, hairG, hairB);
                context.text(this.font, Component.literal("Active Hex: " + hairHex), panelX + 12, panelY + 24, 0xFFAAAAAA, false);

                // Draw Color Preview Swatch Box on right side of header
                int argb = 0xFF000000 | (hairR << 16) | (hairG << 8) | hairB;
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 10, panelY + 40, argb);
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 10, panelY + 9, borderColor);
                context.fill(panelX + panelW - 44, panelY + 39, panelX + panelW - 10, panelY + 40, borderColor);
                context.fill(panelX + panelW - 44, panelY + 8, panelX + panelW - 43, panelY + 40, borderColor);
                context.fill(panelX + panelW - 11, panelY + 8, panelX + panelW - 10, panelY + 40, borderColor);

                context.text(this.font, Component.literal("Preset Palette:"), panelX + 12, startSliderY + 70, 0xFF00FFCC, false);
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private record RaceOption(String name, String id) {}
    private record PresetColor(String name, int r, int g, int b) {}

    private class RgbSliderWidget extends net.minecraft.client.gui.components.AbstractSliderButton {
        private final String prefix;
        private final java.util.function.IntConsumer onChanged;

        public RgbSliderWidget(int x, int y, int width, int height, String prefix, int initialVal, java.util.function.IntConsumer onChanged) {
            super(x, y, width, height, Component.literal(prefix + ": " + initialVal), initialVal / 255.0);
            this.prefix = prefix;
            this.onChanged = onChanged;
            updateMessage();
        }

        public int getIntValue() {
            return (int) Math.round(this.value * 255.0);
        }

        public void setIntValue(int val) {
            this.value = Math.max(0, Math.min(255, val)) / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(prefix + ": " + getIntValue()));
        }

        @Override
        protected void applyValue() {
            updateMessage();
            if (onChanged != null) {
                onChanged.accept(getIntValue());
            }
        }
    }
}
