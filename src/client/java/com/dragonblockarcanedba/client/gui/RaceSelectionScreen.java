package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class RaceSelectionScreen extends Screen {
    private enum State {
        RACE_SELECT,
        CUSTOMIZATION
    }

    private State currentState = State.RACE_SELECT;

    private String selectedRace = "";
    private String customizationTab = "Skin";

    private int skinR = 255;
    private int skinG = 204;
    private int skinB = 153;

    private int hairR = 15;
    private int hairG = 15;
    private int hairB = 15;

    private RgbSliderWidget skinRedSlider;
    private RgbSliderWidget skinGreenSlider;
    private RgbSliderWidget skinBlueSlider;

    private RgbSliderWidget hairRedSlider;
    private RgbSliderWidget hairGreenSlider;
    private RgbSliderWidget hairBlueSlider;

    private float spinAngle;
    private float previewRotation = 35.0F;
    private int scrollOffset;

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

    private List<Race> getSelectableRaces() {
        List<Race> races = new ArrayList<>(
                DbaRegistries.getRaces().values()
        );

        races.removeIf(race -> {
            String path = race.getId().getPath();
            return "neo_tuffle".equals(path) || "android".equals(path);
        });

        return races;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        this.skinRedSlider = null;
        this.skinGreenSlider = null;
        this.skinBlueSlider = null;

        this.hairRedSlider = null;
        this.hairGreenSlider = null;
        this.hairBlueSlider = null;

        int leftColWidth = Math.max(100, this.width / 3);
        int midColWidth = Math.min(120, this.width / 4);
        int midStartX = leftColWidth;
        int rightStartX = leftColWidth + midColWidth;
        int rightColWidth = Math.max(140, this.width - rightStartX);

        int btnWidth = Math.min(100, midColWidth - 10);
        int btnHeight = 20;

        if (selectedRace == null || selectedRace.isEmpty()) {
            List<Race> selectable = getSelectableRaces();
            if (!selectable.isEmpty()) {
                selectedRace = selectable.get(0).getId().toString();
                String path = selectable.get(0).getId().getPath().toLowerCase();
                com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(path);
            }
        }
        applyLiveRecolor();

        if (currentState == State.RACE_SELECT) {
            initRaceSelection(
                    midStartX,
                    midColWidth,
                    rightStartX,
                    rightColWidth,
                    btnWidth,
                    btnHeight
            );
        } else {
            initCustomization(
                    midStartX,
                    midColWidth,
                    rightStartX,
                    rightColWidth,
                    btnWidth,
                    btnHeight
            );
        }
    }

    private void initRaceSelection(
            int midStartX,
            int midColWidth,
            int rightStartX,
            int rightColWidth,
            int btnWidth,
            int btnHeight
    ) {
        List<Race> races = getSelectableRaces();
        int totalRaces = races.size();

        int availableH = this.height - 40;
        int dynamicBtnHeight = Math.max(12, Math.min(15, (availableH - (totalRaces - 1) * 2) / totalRaces));
        int spacingY = dynamicBtnHeight + 2;
        int startY = 24 + (availableH - (totalRaces * spacingY)) / 2;

        int buttonX = midStartX + (midColWidth - btnWidth) / 2;

        for (int i = 0; i < totalRaces; i++) {
            Race race = races.get(i);
            boolean selected = race.getId().toString().equals(selectedRace);
            Component label = Component.literal(
                    selected ? "> " + race.getDisplayName() + " <" : race.getDisplayName()
            );

            addRenderableWidget(
                    Button.builder(
                            label,
                            button -> {
                                selectedRace = race.getId().toString();
                                String path = race.getId().getPath().toLowerCase();
                                if (path.contains("yardrat")) {
                                    skinR = 250; skinG = 180; skinB = 175;
                                    hairR = 255; hairG = 240; hairB = 140;
                                } else if (path.contains("namek")) {
                                    skinR = 90; skinG = 210; skinB = 110;
                                    hairR = 230; hairG = 140; hairB = 160;
                                } else if (path.contains("majin")) {
                                    skinR = 255; skinG = 145; skinB = 195;
                                    hairR = 255; hairG = 145; hairB = 195;
                                } else if (path.contains("arcosian")) {
                                    skinR = 240; skinG = 240; skinB = 252;
                                    hairR = 130; hairG = 40; hairB = 190;
                                } else if (path.contains("bio_android")) {
                                    skinR = 60; skinG = 180; skinB = 85;
                                    hairR = 240; hairG = 190; hairB = 30;
                                } else {
                                    skinR = 255; skinG = 204; skinB = 153;
                                    hairR = 25; hairG = 25; hairB = 25;
                                }
                                applyLiveRecolor();
                                com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(path);
                                init();
                            }
                    ).bounds(
                            buttonX,
                            startY + i * spacingY,
                            btnWidth,
                            dynamicBtnHeight
                    ).build()
            );
        }

        if (!selectedRace.isEmpty()) {
            int confirmBtnW = Math.min(100, rightColWidth - 20);
            addRenderableWidget(
                    Button.builder(
                            Component.literal("CUSTOMIZE"),
                            button -> {
                                currentState = State.CUSTOMIZATION;
                                customizationTab = "Skin";
                                init();
                            }
                    ).bounds(
                            rightStartX + rightColWidth - confirmBtnW - 10,
                            this.height - 28,
                            confirmBtnW,
                            btnHeight
                    ).build()
            );
        }
    }

    private void initCustomization(
            int midStartX,
            int midColWidth,
            int rightStartX,
            int rightColWidth,
            int btnWidth,
            int btnHeight
    ) {
        int tabWidth = Math.min(90, midColWidth - 10);
        int tabX = midStartX + (midColWidth - tabWidth) / 2;

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "Skin".equals(customizationTab)
                                        ? "[ SKIN ]"
                                        : "Skin"
                        ),
                        button -> {
                            customizationTab = "Skin";
                            init();
                        }
                ).bounds(
                        tabX,
                        this.height / 2 - 28,
                        tabWidth,
                        btnHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal(
                                "Hair".equals(customizationTab)
                                        ? "[ HAIR ]"
                                        : "Hair"
                        ),
                        button -> {
                            customizationTab = "Hair";
                            init();
                        }
                ).bounds(
                        tabX,
                        this.height / 2,
                        tabWidth,
                        btnHeight
                ).build()
        );

        int panelX = rightStartX + 5;
        int panelW = rightColWidth - 10;

        int sliderX = panelX + 8;
        int sliderW = panelW - 16;

        int sliderY = 40;

        if ("Skin".equals(customizationTab)) {
            skinRedSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY,
                    sliderW,
                    18,
                    "Red",
                    skinR,
                    value -> { skinR = value; applyLiveRecolor(); }
            );

            skinGreenSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY + 22,
                    sliderW,
                    18,
                    "Green",
                    skinG,
                    value -> { skinG = value; applyLiveRecolor(); }
            );

            skinBlueSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY + 44,
                    sliderW,
                    18,
                    "Blue",
                    skinB,
                    value -> { skinB = value; applyLiveRecolor(); }
            );

            addRenderableWidget(skinRedSlider);
            addRenderableWidget(skinGreenSlider);
            addRenderableWidget(skinBlueSlider);

            addSkinPresetButtons(
                    sliderX,
                    sliderY + 70,
                    sliderW
            );
        } else {
            hairRedSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY,
                    sliderW,
                    18,
                    "Red",
                    hairR,
                    value -> { hairR = value; applyLiveRecolor(); }
            );

            hairGreenSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY + 22,
                    sliderW,
                    18,
                    "Green",
                    hairG,
                    value -> { hairG = value; applyLiveRecolor(); }
            );

            hairBlueSlider = new RgbSliderWidget(
                    sliderX,
                    sliderY + 44,
                    sliderW,
                    18,
                    "Blue",
                    hairB,
                    value -> { hairB = value; applyLiveRecolor(); }
            );

            addRenderableWidget(hairRedSlider);
            addRenderableWidget(hairGreenSlider);
            addRenderableWidget(hairBlueSlider);

            addHairPresetButtons(
                    sliderX,
                    sliderY + 70,
                    sliderW
            );
        }

        int bottomY = this.height - 24;
        int actionBtnW = Math.min(75, (panelW - 16) / 2);

        addRenderableWidget(
                Button.builder(
                        Component.literal("BACK"),
                        button -> {
                            currentState = State.RACE_SELECT;
                            init();
                        }
                ).bounds(
                        panelX + 4,
                        bottomY,
                        actionBtnW,
                        18
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("CONFIRM"),
                        button -> confirmCharacter()
                ).bounds(
                        panelX + panelW - actionBtnW - 4,
                        bottomY,
                        actionBtnW,
                        18
                ).build()
        );
    }

    private void addSkinPresetButtons(
            int x,
            int y,
            int width
    ) {
        PresetColor[] presets = {
                new PresetColor("Light", 255, 204, 153),
                new PresetColor("Tan", 210, 180, 140),
                new PresetColor("Dark", 141, 85, 36),
                new PresetColor("Namek", 85, 255, 120),
                new PresetColor("Majin", 255, 140, 200),
                new PresetColor("Arcosian", 230, 230, 250)
        };

        addPresetGrid(
                presets,
                x,
                y,
                width,
                (preset) -> {
                    skinR = preset.r;
                    skinG = preset.g;
                    skinB = preset.b;

                    if (skinRedSlider != null) skinRedSlider.setIntValue(skinR);
                    if (skinGreenSlider != null) skinGreenSlider.setIntValue(skinG);
                    if (skinBlueSlider != null) skinBlueSlider.setIntValue(skinB);
                    applyLiveRecolor();
                }
        );
    }

    private void addHairPresetButtons(
            int x,
            int y,
            int width
    ) {
        PresetColor[] presets = {
                new PresetColor("Black", 15, 15, 15),
                new PresetColor("Gold", 255, 215, 0),
                new PresetColor("Brown", 80, 45, 20),
                new PresetColor("Red", 200, 30, 30),
                new PresetColor("Blue", 30, 180, 255),
                new PresetColor("White", 240, 240, 240)
        };

        addPresetGrid(
                presets,
                x,
                y,
                width,
                (preset) -> {
                    hairR = preset.r;
                    hairG = preset.g;
                    hairB = preset.b;

                    if (hairRedSlider != null) hairRedSlider.setIntValue(hairR);
                    if (hairGreenSlider != null) hairGreenSlider.setIntValue(hairG);
                    if (hairBlueSlider != null) hairBlueSlider.setIntValue(hairB);
                    applyLiveRecolor();
                }
        );
    }

    private void applyLiveRecolor() {
        int s = ((skinR & 0xFF) << 16) | ((skinG & 0xFF) << 8) | (skinB & 0xFF);
        int h = ((hairR & 0xFF) << 16) | ((hairG & 0xFF) << 8) | (hairB & 0xFF);
        
        Identifier rId = Identifier.tryParse(selectedRace);
        String racePath = (rId != null) ? rId.getPath() : "universal_humanoid";
        com.dragonblockarcanedba.client.render.DynamicSkinManager.getOrGenerateSkin(racePath, s, h);
        
        if (Minecraft.getInstance().player instanceof PlayerStatsAccessor acc) {
            acc.dba$setSkinColor(rgbToHex(skinR, skinG, skinB));
            acc.dba$setHairColor(rgbToHex(hairR, hairG, hairB));
        }
    }

    private void addPresetGrid(
            PresetColor[] presets,
            int x,
            int y,
            int width,
            java.util.function.Consumer<PresetColor> consumer
    ) {
        int columns = 3;
        int gap = 3;
        int buttonWidth = (width - gap * (columns - 1)) / columns;
        int buttonHeight = 16;

        for (int i = 0; i < presets.length; i++) {
            PresetColor preset = presets[i];
            int row = i / columns;
            int column = i % columns;
            int bx = x + column * (buttonWidth + gap);
            int by = y + row * (buttonHeight + gap);

            addRenderableWidget(
                    Button.builder(
                            Component.literal(preset.name),
                            button -> consumer.accept(preset)
                    ).bounds(
                            bx,
                            by,
                            buttonWidth,
                            buttonHeight
                    ).build()
            );
        }
    }

    private void confirmCharacter() {
        if (selectedRace.isEmpty()) {
            return;
        }

        String skinHex = rgbToHex(skinR, skinG, skinB);
        String hairHex = rgbToHex(hairR, hairG, hairB);

        CompoundTag data = new CompoundTag();
        data.putString("action", "select_race");
        data.putString("race", selectedRace);
        data.putString("skin_color", skinHex);
        data.putString("hair_color", hairHex);

        ClientPlayNetworking.send(new ActionPayload(data));
        applyLiveRecolor();
        Identifier rId = Identifier.tryParse(selectedRace);
        String racePath = (rId != null) ? rId.getPath() : "universal_humanoid";
        com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(racePath);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor) {
            accessor.dba$setHasSelectedRace(true);
            accessor.dba$setTailSevered(false);
        }
        this.onClose();
    }

    private static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (currentState != State.RACE_SELECT) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        List<Race> races = getSelectableRaces();
        int maxVisible = Math.max(1, (this.height - 80) / 22);
        int maxScroll = Math.max(0, races.size() - maxVisible);

        if (verticalAmount > 0) {
            scrollOffset--;
        } else if (verticalAmount < 0) {
            scrollOffset++;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        init();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        spinAngle += 2.0F;
        if (spinAngle >= 360.0F) {
            spinAngle -= 360.0F;
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        context.fill(0, 0, this.width, this.height, 0xF0101216);

        int leftColWidth = Math.max(100, this.width / 3);
        int midColWidth = Math.min(120, this.width / 4);
        int rightStartX = leftColWidth + midColWidth;
        int rightColWidth = Math.max(140, this.width - rightStartX);

        int borderColor = 0xFF00FFCC;

        context.fill(0, 0, this.width, 20, 0xFF0A0C0E);
        context.fill(0, 20, this.width, 22, borderColor);

        String title = currentState == State.RACE_SELECT ? "RACE SELECTION" : "CHARACTER CUSTOMIZATION";
        context.centeredText(this.font, Component.literal(title), this.width / 2, 6, 0xFFFFFFFF);

        int panelMargin = 5;
        int panelX = rightStartX + panelMargin;
        int panelY = 26;
        int panelW = rightColWidth - panelMargin * 2;
        int panelH = this.height - 58;

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0181B22);
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, borderColor);
        context.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, borderColor);
        context.fill(panelX, panelY, panelX + 2, panelY + panelH, borderColor);
        context.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, borderColor);

        if (currentState == State.CUSTOMIZATION) {
            context.centeredText(this.font, Component.literal(customizationTab.toUpperCase() + " TINT"), panelX + panelW / 2, panelY + 4, 0xFF00FFCC);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);

        int previewX = leftColWidth / 2;
        int previewY = this.height / 2;

        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            float savedYRot = localPlayer.getYRot();
            float savedYBodyRot = localPlayer.yBodyRot;
            float savedYHeadRot = localPlayer.yHeadRot;
            float savedXRot = localPlayer.getXRot();

            if (localPlayer instanceof PlayerStatsAccessor acc) {
                if (selectedRace != null && !selectedRace.isEmpty()) {
                    acc.dba$setRaceId(Identifier.parse(selectedRace));
                }
                String skinHex = rgbToHex(skinR, skinG, skinB);
                String hairHex = rgbToHex(hairR, hairG, hairB);
                acc.dba$setSkinColor(skinHex);
                acc.dba$setHairColor(hairHex);
                acc.dba$setTailSevered(false);
            }

            int x0 = previewX - 45;
            int y0 = previewY - 70;
            int x1 = previewX + 45;
            int y1 = previewY + 75;
            int scale = Math.min(45, this.height / 4);

            float rad = (float) Math.toRadians(previewRotation);
            float simMouseX = previewX - (float) Math.sin(rad) * 150.0F;
            float simMouseY = previewY;
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                context, x0, y0, x1, y1, scale, 0.0625F, simMouseX, simMouseY, localPlayer
            );
            context.centeredText(this.font, Component.literal("§7[ Drag to Rotate 360° ]"), previewX, y1 + 4, 0xFFA0B4C8);

            localPlayer.setYRot(savedYRot);
            localPlayer.yBodyRot = savedYBodyRot;
            localPlayer.yHeadRot = savedYHeadRot;
            localPlayer.setXRot(savedXRot);
        }
    }

    @Override
    public boolean mouseDragged(
            net.minecraft.client.input.MouseButtonEvent event,
            double dragX,
            double dragY
    ) {
        int leftColWidth = Math.max(100, this.width / 3);
        if (event.x() < leftColWidth + 30) {
            previewRotation += (float) dragX * 1.8F;
            if (previewRotation > 360.0F) previewRotation -= 360.0F;
            if (previewRotation < 0.0F) previewRotation += 360.0F;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private record PresetColor(String name, int r, int g, int b) {}

    public static class RgbSliderWidget extends AbstractSliderButton {
        private final String prefix;
        private final IntConsumer onChanged;

        public RgbSliderWidget(
                int x,
                int y,
                int width,
                int height,
                String prefix,
                int initialVal,
                IntConsumer onChanged
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.literal(prefix + ": " + initialVal),
                    initialVal / 255.0
            );
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
