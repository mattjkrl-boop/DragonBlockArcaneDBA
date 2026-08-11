package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.ki.KiTechniqueType;
import com.dragonblockarcanedba.network.C2SKiTechniqueSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class KiCustomizerTab implements MenuTab {
    private DbaMenuScreen parent;

    private final KiTechniqueType[] types = KiTechniqueType.values();
    private final int[] colors = {
        0xFF00AAFF, // Blue
        0xFFFF2222, // Red
        0xFF22FF22, // Green
        0xFFEEEE22, // Yellow
        0xFFAA22FF, // Purple
        0xFFFFFFFF  // White
    };
    private final String[] colorNames = {"Blue", "Red", "Green", "Yellow", "Purple", "White"};

    private int typeIdx = 0;
    private int percentUsed = 50;
    private int colorIdx = 0;
    private boolean isBarrage = false;
    private int targetSlot = 0; // 0, 1, 2 for F7, F8, F9

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;

        int startX = parent.getX() + 15;
        int startY = parent.getY() + 35;
        int btnWidth = 140;
        int btnHeight = 20;
        int spacing = 24;

        // Type Button
        parent.addTabWidget(Button.builder(
            Component.literal("Type: " + types[typeIdx].displayName()),
            btn -> {
                typeIdx = (typeIdx + 1) % types.length;
                if (types[typeIdx] != KiTechniqueType.BLAST) {
                    isBarrage = false;
                }
                parent.init();
            }
        ).bounds(startX, startY, btnWidth, btnHeight).build());

        // Ki Percent +/- Buttons
        int pY = startY + spacing;
        parent.addTabWidget(Button.builder(
            Component.literal("-"),
            btn -> {
                percentUsed = Math.max(1, percentUsed - 5);
                parent.init();
            }
        ).bounds(startX, pY, 20, btnHeight).build());
        
        parent.addTabWidget(Button.builder(
            Component.literal("+"),
            btn -> {
                percentUsed = Math.min(100, percentUsed + 5);
                parent.init();
            }
        ).bounds(startX + 120, pY, 20, btnHeight).build());

        // Color Button
        parent.addTabWidget(Button.builder(
            Component.literal("Color: " + colorNames[colorIdx]),
            btn -> {
                colorIdx = (colorIdx + 1) % colors.length;
                parent.init();
            }
        ).bounds(startX, startY + spacing * 2, btnWidth, btnHeight).build());

        // Barrage Toggle (only for Blast)
        if (types[typeIdx] == KiTechniqueType.BLAST) {
            parent.addTabWidget(Button.builder(
                Component.literal("Mode: " + (isBarrage ? "Barrage" : "Single")),
                btn -> {
                    isBarrage = !isBarrage;
                    parent.init();
                }
            ).bounds(startX, startY + spacing * 3, btnWidth, btnHeight).build());
        }

        // Slot Selector
        int sY = startY + spacing * 4;
        parent.addTabWidget(Button.builder(
            Component.literal("Equip Slot: " + (targetSlot + 1) + " (F" + (targetSlot + 7) + ")"),
            btn -> {
                targetSlot = (targetSlot + 1) % 3;
                parent.init();
            }
        ).bounds(startX, sY, btnWidth, btnHeight).build());

        // Save Button
        parent.addTabWidget(Button.builder(
            Component.literal("Save Technique"),
            btn -> {
                ClientPlayNetworking.send(new C2SKiTechniqueSavePayload(
                    targetSlot,
                    types[typeIdx].name(),
                    percentUsed,
                    colors[colorIdx],
                    isBarrage
                ));
            }
        ).bounds(startX, sY + spacing + 10, btnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        int startX = parent.getX();
        int startY = parent.getY();

        context.text(client.font, Component.literal("Ki Customizer"), startX + 15, startY + 15, 0xFF55FF88);

        // Render percent value between - and +
        context.centeredText(client.font, Component.literal(percentUsed + "% Ki"), startX + 85, startY + 35 + 24 + 6, 0xFFFFFF);

        // Preview Area
        int previewX = startX + 170;
        int previewY = startY + 35;

        context.text(client.font, Component.literal("Technique Summary"), previewX, previewY, 0xFFFFFFFF);
        
        String displayName = types[typeIdx].displayName();
        if (types[typeIdx] == KiTechniqueType.BLAST && isBarrage) {
            displayName = "Ki Barrage";
        }
        
        context.text(client.font, Component.literal(displayName), previewX, previewY + 20, colors[colorIdx]);
        
        if (types[typeIdx] == KiTechniqueType.EXPLOSION) {
            context.text(client.font, Component.literal("Cost: 100% of current Ki"), previewX, previewY + 35, 0xFFFF5555);
            context.text(client.font, Component.literal("Warning: Damages self!"), previewX, previewY + 50, 0xFFFF2222);
        } else {
            context.text(client.font, Component.literal("Cost: " + percentUsed + "% of current Ki"), previewX, previewY + 35, 0xFF55FFFF);
        }

        // Preview box border
        context.fill(previewX - 5, previewY - 5, previewX + 160, previewY + 80, 0x44111111);
        context.fill(previewX - 5, previewY - 5, previewX - 4, previewY + 80, colors[colorIdx]);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
