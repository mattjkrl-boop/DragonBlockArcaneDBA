package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.network.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class RaceSelectionScreen extends Screen {
    private enum State {
        RACE_SELECT,
        CUSTOMIZATION
    }

    private State currentState = State.RACE_SELECT;
    private String selectedRace = "";
    private String customizationTab = "Skin";
    
    private net.minecraft.client.gui.components.EditBox skinColorInput;
    private net.minecraft.client.gui.components.EditBox hairColorInput;
    
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

            // Right Column: Content based on active tab
            int inputX = rightStartX + 20;
            int inputY = this.height / 2;

            if (skinColorInput == null) {
                skinColorInput = new net.minecraft.client.gui.components.EditBox(this.font, inputX, inputY, 100, 20, Component.literal("Skin Color"));
                skinColorInput.setMaxLength(7);
                skinColorInput.setValue("#FFCC99");
            }
            if (hairColorInput == null) {
                hairColorInput = new net.minecraft.client.gui.components.EditBox(this.font, inputX, inputY, 100, 20, Component.literal("Hair Color"));
                hairColorInput.setMaxLength(7);
                hairColorInput.setValue("#000000");
            }

            if (customizationTab.equals("Skin")) {
                addRenderableWidget(skinColorInput);
            } else if (customizationTab.equals("Hair")) {
                addRenderableWidget(hairColorInput);
            }

            // Right Column: Back and Confirm
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
                    CompoundTag data = new CompoundTag();
                    data.putString("action", "select_race");
                    data.putString("race", selectedRace);
                    data.putString("skin_color", skinColorInput.getValue());
                    data.putString("hair_color", hairColorInput.getValue());
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
        context.fill(0, 0, this.width, this.height, 0xEE1E2024);

        int leftColWidth = this.width * 2 / 5;
        int midColWidth = this.width * 1 / 5;
        int rightColWidth = this.width * 2 / 5;
        int midStartX = leftColWidth;
        int rightStartX = leftColWidth + midColWidth;

        int borderColor = 0xFF55FF88;

        // Draw top header bar
        context.fill(0, 0, this.width, 20, 0xFF111111);
        context.fill(0, 20, this.width, 22, borderColor);
        
        String headerTitle = currentState == State.RACE_SELECT ? "FULL-SCREEN RACE SELECTION" : "FULL-SCREEN CHARACTER CUSTOMIZATION";
        context.centeredText(this.font, Component.literal(headerTitle), this.width / 2, 6, 0xFFFFFFFF);

        // Left Column: Preview Placeholder
        int previewX = leftColWidth / 2;
        int previewY = this.height / 2;
        context.centeredText(this.font, Component.literal("3D Player Preview"), previewX, previewY - 40, 0xFFFFFFFF);
        context.centeredText(this.font, Component.literal("(Textures coming soon)"), previewX, previewY - 25, 0xFFAAAAAA);
        context.fill(previewX - 20, previewY, previewX + 20, previewY + 80, borderColor);
        
        // Draw decorative circle base for preview
        context.fill(previewX - 40, previewY + 80, previewX + 40, previewY + 82, borderColor);

        // Right Column: Details Panel Background
        int panelMargin = 10;
        int panelX = rightStartX + panelMargin;
        int panelY = 40;
        int panelW = rightColWidth - panelMargin * 2;
        int panelH = this.height - 90; // leave room for buttons at bottom
        
        // Panel Background and border
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE2A2D34);
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
                context.text(this.font, Component.literal(friendlyName + " Race Details"), panelX + 10, panelY + 10, 0xFFFFFFFF, false);
                
                context.text(this.font, Component.literal("Race Details:"), panelX + 10, panelY + 30, 0xFFAAAAAA, false);
                context.text(this.font, Component.literal("- Strength: Base 0"), panelX + 15, panelY + 45, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("- Dexterity: Base 0"), panelX + 15, panelY + 55, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("- Defense: Base 0"), panelX + 15, panelY + 65, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("- Willpower: Base 0"), panelX + 15, panelY + 75, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("- Spirit: Base 0"), panelX + 15, panelY + 85, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("- Vitality: Base 0"), panelX + 15, panelY + 95, 0xFFFFFFFF, false);

                context.text(this.font, Component.literal("Unique Traits:"), panelX + 10, panelY + 115, 0xFFAAAAAA, false);
                context.text(this.font, Component.literal("Specific abilities will"), panelX + 15, panelY + 130, 0xFFFFFFFF, false);
                context.text(this.font, Component.literal("be added here."), panelX + 15, panelY + 140, 0xFFFFFFFF, false);
            } else {
                context.centeredText(this.font, Component.literal("Select a race to view details"), panelX + panelW / 2, panelY + panelH / 2, 0xFFAAAAAA);
            }
        } else if (currentState == State.CUSTOMIZATION) {
            context.text(this.font, Component.literal("Character Customization"), panelX + 10, panelY + 10, 0xFFFFFFFF, false);
            
            if (customizationTab.equals("Skin")) {
                context.text(this.font, Component.literal("SKIN TINCTURE (Hex Color)"), panelX + 10, panelY + 30, 0xFFAAAAAA, false);
                context.text(this.font, Component.literal("Enter a valid hex code (e.g. #FFCC99)"), panelX + 10, panelY + 45, 0xFFFFFFFF, false);
            } else if (customizationTab.equals("Hair")) {
                context.text(this.font, Component.literal("FOLLICLE STRUCTURE (Hair Color)"), panelX + 10, panelY + 30, 0xFFAAAAAA, false);
                context.text(this.font, Component.literal("Enter a valid hex code (e.g. #000000)"), panelX + 10, panelY + 45, 0xFFFFFFFF, false);
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private record RaceOption(String name, String id) {}
}
