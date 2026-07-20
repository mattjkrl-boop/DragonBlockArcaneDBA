package com.dragonblockarcanedba.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class KiCustomizerTab implements MenuTab {
    private DbaMenuScreen parent;
    
    private final String[] types = {"Projectile", "Beam"};
    private final String[] sizes = {"Small", "Medium", "Large", "Massive"};
    private final String[] chargeTimes = {"Instant", "Short", "Medium", "Long"};
    private final String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple"};

    private int typeIdx = 0;
    private int sizeIdx = 1;
    private int chargeIdx = 1;
    private int colorIdx = 1;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        
        int startX = parent.getX() + 15;
        int startY = parent.getY() + 35;
        int btnWidth = 100;
        int btnHeight = 20;
        int spacing = 24;

        // Type Button
        parent.addTabWidget(Button.builder(
            Component.literal("Type: " + types[typeIdx]),
            btn -> {
                typeIdx = (typeIdx + 1) % types.length;
                parent.init();
            }
        ).bounds(startX, startY, btnWidth, btnHeight).build());

        // Size Button
        parent.addTabWidget(Button.builder(
            Component.literal("Size: " + sizes[sizeIdx]),
            btn -> {
                sizeIdx = (sizeIdx + 1) % sizes.length;
                parent.init();
            }
        ).bounds(startX, startY + spacing, btnWidth, btnHeight).build());

        // Charge Time Button
        parent.addTabWidget(Button.builder(
            Component.literal("Charge: " + chargeTimes[chargeIdx]),
            btn -> {
                chargeIdx = (chargeIdx + 1) % chargeTimes.length;
                parent.init();
            }
        ).bounds(startX, startY + spacing * 2, btnWidth, btnHeight).build());

        // Color Button
        parent.addTabWidget(Button.builder(
            Component.literal("Color: " + colors[colorIdx]),
            btn -> {
                colorIdx = (colorIdx + 1) % colors.length;
                parent.init();
            }
        ).bounds(startX, startY + spacing * 3, btnWidth, btnHeight).build());
        
        // Craft Button
        parent.addTabWidget(Button.builder(
            Component.literal("Craft Technique"),
            btn -> {
                // Future integration point for technique crafting payload
                System.out.println("Crafted: " + types[typeIdx] + " " + sizes[sizeIdx] + " " + chargeTimes[chargeIdx] + " " + colors[colorIdx]);
            }
        ).bounds(startX, startY + spacing * 4 + 10, 150, 20).build());
    }

    private double calculateKiCost() {
        double base = 10.0;
        double typeMult = typeIdx == 1 ? 2.5 : 1.0; // Beam vs Projectile
        double[] sizeMults = {1.0, 1.5, 2.5, 4.0};
        double[] chargeMults = {1.5, 1.0, 0.75, 0.5}; // Longer charge = cheaper
        
        return base * typeMult * sizeMults[sizeIdx] * chargeMults[chargeIdx];
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        int startX = parent.getX();
        int startY = parent.getY();

        context.text(client.font, Component.literal("Ki Technique Customizer"), startX + 15, startY + 15, 0xFF55FF88);

        // Render preview area on the right
        int previewX = startX + 160;
        int previewY = startY + 35;
        
        context.text(client.font, Component.literal("Technique Summary:"), previewX, previewY, 0xFFFFFFFF);
        context.text(client.font, Component.literal("- " + sizes[sizeIdx] + " " + colors[colorIdx] + " " + types[typeIdx]), previewX, previewY + 15, 0xFFAAAAAA);
        context.text(client.font, Component.literal("- Charge: " + chargeTimes[chargeIdx]), previewX, previewY + 25, 0xFFAAAAAA);
        
        double cost = calculateKiCost();
        context.text(client.font, Component.literal(String.format("Calculated Ki Cost: %.1f", cost)), previewX, previewY + 45, 0xFF55FFFF);
        
        // Decorative border for preview area
        context.fill(previewX - 5, previewY - 5, previewX + 160, previewY + 65, 0x44111111);
        context.fill(previewX - 5, previewY - 5, previewX - 4, previewY + 65, 0xFF55FF88);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
