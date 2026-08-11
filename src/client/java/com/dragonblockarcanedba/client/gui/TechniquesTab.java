package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import com.dragonblockarcanedba.network.C2SEquipTechniquePayload;
import com.dragonblockarcanedba.network.C2SUnlockTechniquePayload;
import com.dragonblockarcanedba.registry.Technique;
import com.dragonblockarcanedba.registry.TechniqueRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;

public class TechniquesTab implements MenuTab {
    private DbaMenuScreen parent;
    private final int nodeWidth = 60;
    private final int nodeHeight = 35;
    
    private final int slotWidth = 40;
    private final int slotHeight = 40;

    private Technique selectedTech = null;

    // Pan and Zoom states
    private double scrollX = 0;
    private double scrollY = 0;
    private float zoom = 1.0f;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        this.scrollX += dragX;
        this.scrollY += dragY;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.zoom += (float) verticalAmount * 0.1f;
        if (this.zoom < 0.2f) this.zoom = 0.2f;
        if (this.zoom > 3.0f) this.zoom = 3.0f;
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();
        
        Identifier raceId = accessor.dba$getRaceId();
        List<Technique> tree = TechniqueRegistry.getTechniquesForRace(raceId);

        // Equip Slots in top-right corner
        int slotsY = startY + 10;
        int slotsStartX = startX + width - (3 * (slotWidth + 10)) - 10;
        
        // --- RENDER CLIPPED TREE VIEW ---
        int clipStartY = startY + 55;
        int clipEndY = startY + 140;
        
        // Tree Background Panel (Arcane gradient / deep dark)
        context.fill(startX + 10, clipStartY, startX + width - 10, clipEndY, 0x55050810);
        // Subtle grid lines
        for(int i = startX + 10; i < startX + width - 10; i += 20) {
            context.fill(i, clipStartY, i + 1, clipEndY, 0x11FFFFFF);
        }
        for(int j = clipStartY; j < clipEndY; j += 20) {
            context.fill(startX + 10, j, startX + width - 10, j + 1, 0x11FFFFFF);
        }

        context.fill(startX + 10, clipStartY, startX + width - 10, clipStartY + 1, 0x4455FF88); // Top glow border
        context.fill(startX + 10, clipEndY - 1, startX + width - 10, clipEndY, 0x4455FF88); // Bottom glow border
        
        context.enableScissor(startX + 10, clipStartY, startX + width - 10, clipEndY);
        
        float treeOriginX = startX + 25 + (float)scrollX;
        float treeOriginY = startY + 70 + (float)scrollY;
        
        // Draw Connecting Lines FIRST so they are under nodes
        for (int i = 0; i < tree.size() - 1; i++) {
            int nx1 = i * 90;
            int ny1 = 0;
            int nx2 = (i + 1) * 90;
            int ny2 = 0;
            
            int rX1 = (int) (treeOriginX + (nx1 * zoom)) + (int)(nodeWidth * zoom / 2);
            int rY1 = (int) (treeOriginY + (ny1 * zoom)) + (int)(nodeHeight * zoom / 2);
            int rX2 = (int) (treeOriginX + (nx2 * zoom)) + (int)(nodeWidth * zoom / 2);
            int rY2 = (int) (treeOriginY + (ny2 * zoom)) + (int)(nodeHeight * zoom / 2);
            
            boolean nextUnlocked = accessor.dba$hasTechnique(tree.get(i+1).id());
            int lineCol = nextUnlocked ? 0xAA55FF88 : 0x44FFFFFF;
            
            if (rY1 == rY2) {
                context.fill(rX1, rY1 - 1, rX2, rY1 + 1, lineCol);
            }
        }

        for (int i = 0; i < tree.size(); i++) {
            Technique tech = tree.get(i);
            int nx = i * 90;
            int ny = 0;
            
            int renderX = (int) (treeOriginX + (nx * zoom));
            int renderY = (int) (treeOriginY + (ny * zoom));
            
            boolean unlocked = accessor.dba$hasTechnique(tech.id());
            boolean selected = (selectedTech == tech);
            drawNodeScaled(context, client, renderX, renderY, tech.name(), unlocked, selected, zoom);
        }
        
        context.disableScissor();
        // --------------------------------

        // Overlays outside clipping region
        context.text(client.font, Component.literal("Arcane Skill Tree"), startX + 15, startY + 15, 0xFF55FF88);
        context.text(client.font, Component.literal("[Drag to Pan • Scroll to Zoom]"), startX + 15, startY + 30, 0xFF666666);

        // Selected Technique Details Panel (Bottom Left)
        int panelX = startX + 10;
        int panelY = startY + 145;
        if (selectedTech != null) {
            // Draw Glass Panel Background
            context.fill(panelX, panelY, startX + width - 10, panelY + 95, 0x88050810);
            context.fill(panelX, panelY, panelX + 3, panelY + 95, 0xFF55FF88); // Neon Left accent
            context.fill(panelX, panelY, startX + width - 10, panelY + 1, 0x44FFFFFF); // Top subtle border
            
            // Header
            context.text(client.font, Component.literal(selectedTech.name()), panelX + 15, panelY + 10, 0xFFFFFFFF);
            context.text(client.font, Component.literal("Req Lvl: " + selectedTech.unlockLevel()), panelX + 15, panelY + 25, 0xFFFFAA00);
            
            boolean unlocked = accessor.dba$hasTechnique(selectedTech.id());
            if (!unlocked) {
                // Draw Sci-Fi Unlock Button
                int btnX = startX + width - 110;
                int btnY = panelY + 10;
                int btnW = 95;
                int btnH = 22;
                boolean hoverBtn = (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hoverBtn ? 0xAA55FF88 : 0x55113322);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFF55FF88); // top border
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFF55FF88); // bot border
                context.centeredText(client.font, Component.literal("UNLOCK (" + selectedTech.apCost() + " AP)"), btnX + btnW / 2, btnY + 7, hoverBtn ? 0xFFFFFFFF : 0xFF55FF88);
            } else {
                context.text(client.font, Component.literal("UNLOCKED"), startX + width - 70, panelY + 15, 0xFF55FF88);
            }
            
            // Description
            String desc = selectedTech.description();
            String[] lines = desc.split("\\. ");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] + (i < lines.length - 1 ? "." : "");
                context.text(client.font, Component.literal(line), panelX + 15, panelY + 45 + (i * 12), 0xFFAAAAAA);
            }
        }

        // Equip Slots (F7, F8, F9 dynamically named)
        String[] keys = {
            DragonBlockArcaneDBAClient.techSlot1Key.getTranslatedKeyMessage().getString(),
            DragonBlockArcaneDBAClient.techSlot2Key.getTranslatedKeyMessage().getString(),
            DragonBlockArcaneDBAClient.techSlot3Key.getTranslatedKeyMessage().getString()
        };
        
        for (int i = 0; i < 3; i++) {
            int sx = slotsStartX + (i * (slotWidth + 10));
            int sy = slotsY;
            String equippedId = accessor.dba$getEquippedTechnique(i);
            
            boolean hoverSlot = (mouseX >= sx && mouseX <= sx + slotWidth && mouseY >= sy && mouseY <= sy + slotHeight);
            int slotBgColor = hoverSlot ? 0x4455FF88 : 0x66000000;
            
            // Advanced Sci-Fi borders
            context.fill(sx, sy, sx + slotWidth, sy + slotHeight, slotBgColor);
            int brColor = hoverSlot ? 0xFFFFFFFF : 0xFF55FF88;
            
            // Top Left bracket
            context.fill(sx, sy, sx + 6, sy + 2, brColor);
            context.fill(sx, sy, sx + 2, sy + 6, brColor);
            // Top Right bracket
            context.fill(sx + slotWidth - 6, sy, sx + slotWidth, sy + 2, brColor);
            context.fill(sx + slotWidth - 2, sy, sx + slotWidth, sy + 6, brColor);
            // Bottom Left bracket
            context.fill(sx, sy + slotHeight - 2, sx + 6, sy + slotHeight, brColor);
            context.fill(sx, sy + slotHeight - 6, sx + 2, sy + slotHeight, brColor);
            // Bottom Right bracket
            context.fill(sx + slotWidth - 6, sy + slotHeight - 2, sx + slotWidth, sy + slotHeight, brColor);
            context.fill(sx + slotWidth - 2, sy + slotHeight - 6, sx + slotWidth, sy + slotHeight, brColor);
            
            // Draw Key name in slot
            context.centeredText(client.font, Component.literal(keys[i].toUpperCase()), sx + slotWidth / 2, sy - 10, 0xFFFFAA00);
            
            if (equippedId != null && !equippedId.isEmpty()) {
                Technique tech = TechniqueRegistry.getTechnique(Identifier.tryParse(equippedId));
                if (tech != null) {
                    String dName = tech.name();
                    if (client.font.width(dName) > slotWidth) dName = dName.substring(0, 4) + "..";
                    context.centeredText(client.font, Component.literal(dName), sx + slotWidth / 2, sy + 16, 0xFFFFFFFF);
                }
            } else {
                context.centeredText(client.font, Component.literal("EMPTY"), sx + slotWidth / 2, sy + 16, 0xFF555555);
            }
        }
    }

    private void drawNodeScaled(GuiGraphicsExtractor context, Minecraft client, int x, int y, String name, boolean unlocked, boolean selected, float currentZoom) {
        int w = (int)(nodeWidth * currentZoom);
        int h = (int)(nodeHeight * currentZoom);
        
        int bgColor = unlocked ? 0x99101520 : 0xAA050505;
        if (selected) bgColor = 0xAA223322;
        
        int borderColor = selected ? 0xFFFFAA00 : (unlocked ? 0xFF55FF88 : 0xFF333333);
        
        // Node background
        context.fill(x, y, x + w, y + h, bgColor);
        
        // Glowing borders
        context.fill(x, y, x + w, y + 2, borderColor); 
        context.fill(x, y + h - 2, x + w, y + h, borderColor);
        context.fill(x, y, x + 2, y + h, borderColor);
        context.fill(x + w - 2, y, x + w, y + h, borderColor); 
        
        // Inner tech details (only if zoomed in enough)
        if (currentZoom >= 0.6f) {
            String displayName = name;
            if (client.font.width(displayName) > w - 4) {
                displayName = displayName.substring(0, Math.min(displayName.length(), 6)) + "..";
            }
            context.centeredText(client.font, Component.literal(displayName), x + w / 2, y + (h/2) - 4, unlocked ? 0xFFFFFFFF : 0xFF666666);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        
        double mouseX = event.x();
        double mouseY = event.y();

        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();

        // 1. Check Equip Slots (top layer)
        int slotsY = startY + 10;
        int slotsStartX = startX + width - (3 * (slotWidth + 10)) - 10;
        for (int i = 0; i < 3; i++) {
            int sx = slotsStartX + (i * (slotWidth + 10));
            int sy = slotsY;
            if (mouseX >= sx && mouseX <= sx + slotWidth && mouseY >= sy && mouseY <= sy + slotHeight) {
                if (selectedTech != null && accessor.dba$hasTechnique(selectedTech.id())) {
                    ClientPlayNetworking.send(new C2SEquipTechniquePayload(i, selectedTech.id()));
                } else if (selectedTech == null) {
                    ClientPlayNetworking.send(new C2SEquipTechniquePayload(i, ""));
                }
                return true;
            }
        }

        // 2. Check Unlock Button
        int panelX = startX + 10;
        int panelY = startY + 145;
        if (selectedTech != null && !accessor.dba$hasTechnique(selectedTech.id())) {
            int btnX = startX + width - 110;
            int btnY = panelY + 10;
            int btnW = 95;
            int btnH = 22;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                ClientPlayNetworking.send(new C2SUnlockTechniquePayload(selectedTech.id()));
                return true;
            }
        }

        // 3. Check Tree Nodes (scaled and panned)
        Identifier raceId = accessor.dba$getRaceId();
        List<Technique> tree = TechniqueRegistry.getTechniquesForRace(raceId);
        
        float treeOriginX = startX + 25 + (float)scrollX;
        float treeOriginY = startY + 70 + (float)scrollY;

        for (int i = 0; i < tree.size(); i++) {
            int nx = i * 90;
            int ny = 0;
            
            double screenNX = treeOriginX + (nx * zoom);
            double screenNY = treeOriginY + (ny * zoom);
            double screenNW = nodeWidth * zoom;
            double screenNH = nodeHeight * zoom;
            
            if (mouseX >= screenNX && mouseX <= screenNX + screenNW && mouseY >= screenNY && mouseY <= screenNY + screenNH) {
                selectedTech = tree.get(i);
                return true;
            }
        }

        return false;
    }
}
