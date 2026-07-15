package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.Technique;
import com.dragonblockarcanedba.registry.TechniqueRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;

public class TechniquesTab implements MenuTab {
    private DbaMenuScreen parent;
    private final int nodeWidth = 50;
    private final int nodeHeight = 35;
    private final int spacingX = 10;
    private final int spacingY = 20;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getX();
        int startY = parent.getY();

        Identifier raceId = accessor.dba$getRaceId();
        List<Technique> tree = TechniqueRegistry.getTechniquesForRace(raceId);
        String raceName = raceId.getPath().substring(0, 1).toUpperCase() + raceId.getPath().substring(1).replace("_", "-");
        
        context.text(client.font, Component.literal(raceName + " Techniques Tree"), startX + 15, startY + 15, 0xFFFFAA00);

        if (tree.isEmpty()) {
            context.text(client.font, Component.literal("No techniques available for this race."), startX + 15, startY + 40, 0xFF888888);
            return;
        }

        int level = accessor.dba$getLevel();
        int gridStartX = startX + 15;
        int gridStartY = startY + 40;

        // Draw connecting lines first so they are behind nodes
        for (int i = 0; i < tree.size() - 1; i++) {
            int col1 = i % 4;
            int row1 = i / 4;
            int col2 = (i + 1) % 4;
            int row2 = (i + 1) / 4;

            int n1X = gridStartX + col1 * (nodeWidth + spacingX);
            int n1Y = gridStartY + row1 * (nodeHeight + spacingY);
            int n2X = gridStartX + col2 * (nodeWidth + spacingX);
            int n2Y = gridStartY + row2 * (nodeHeight + spacingY);

            int lineColor = 0xFF555555;
            int activeLineColor = 0xFFFF7700;
            boolean active = level >= tree.get(i + 1).unlockLevel();
            int color = active ? activeLineColor : lineColor;

            if (row1 == row2) {
                // Same row, horizontal line
                context.fill(n1X + nodeWidth, n1Y + nodeHeight / 2 - 1, n2X, n1Y + nodeHeight / 2 + 1, color);
            }
        }

        // Draw Nodes
        Technique hoveredTech = null;
        for (int i = 0; i < tree.size(); i++) {
            Technique tech = tree.get(i);
            int col = i % 4;
            int row = i / 4;
            int nx = gridStartX + col * (nodeWidth + spacingX);
            int ny = gridStartY + row * (nodeHeight + spacingY);
            boolean unlocked = level >= tech.unlockLevel();

            drawNode(context, client, nx, ny, tech.name(), "Lvl " + tech.unlockLevel(), unlocked);
            
            // Check hover
            if (mouseX >= nx && mouseX <= nx + nodeWidth && mouseY >= ny && mouseY <= ny + nodeHeight) {
                hoveredTech = tech;
            }
        }

        // Hover tooltip
        if (hoveredTech != null) {
            String desc = hoveredTech.description();
            int strWidth = client.font.width(desc);
            context.fill(mouseX + 5, mouseY - 15, mouseX + 9 + strWidth, mouseY - 3, 0xEE000000);
            context.fill(mouseX + 5, mouseY - 15, mouseX + 9 + strWidth, mouseY - 14, 0xFF55FF88);
            context.text(client.font, Component.literal(desc), mouseX + 7, mouseY - 12, 0xFFFFFFFF);
        } else {
            context.text(client.font, Component.literal("Gain levels to unlock advanced techniques!"), startX + 15, startY + 160, 0xFF888888);
        }
    }

    private void drawNode(GuiGraphicsExtractor context, Minecraft client, int x, int y, String name, String req, boolean unlocked) {
        // Node frame
        int bgColor = unlocked ? 0xEE1E2024 : 0xAA111111;
        int borderColor = unlocked ? 0xFFFF7700 : 0xFF555555;
        
        context.fill(x, y, x + nodeWidth, y + nodeHeight, bgColor);
        
        // Borders
        context.fill(x, y, x + nodeWidth, y + 2, borderColor); // Top
        context.fill(x, y + nodeHeight - 2, x + nodeWidth, y + nodeHeight, borderColor); // Bottom
        context.fill(x, y, x + 2, y + nodeHeight, borderColor); // Left
        context.fill(x + nodeWidth - 2, y, x + nodeWidth, y + nodeHeight, borderColor); // Right

        // Title and requirements (Truncate name if too long)
        String displayName = name;
        if (client.font.width(displayName) > nodeWidth - 4) {
            // Very simple truncate
            int chars = Math.min(displayName.length(), 6);
            displayName = displayName.substring(0, chars) + "..";
        }
        
        context.centeredText(client.font, Component.literal(displayName), x + nodeWidth / 2, y + 8, unlocked ? 0xFFFFFFFF : 0xFF888888);
        context.centeredText(client.font, Component.literal(req), x + nodeWidth / 2, y + 20, unlocked ? 0xFFFFAA00 : 0xFF555555);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
