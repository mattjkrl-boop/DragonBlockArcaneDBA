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
        int startX = parent.getX();
        int startY = parent.getY();
        
        Identifier raceId = accessor.dba$getRaceId();
        List<Technique> tree = TechniqueRegistry.getTechniquesForRace(raceId);

        // Equip Slots in top-right corner
        int slotsY = startY + 10;
        int slotsStartX = startX + parent.getBgWidth() - (3 * (slotWidth + 10));
        
        // --- RENDER CLIPPED TREE VIEW ---
        int clipStartY = startY + 55;
        int clipEndY = startY + 140;
        context.enableScissor(startX, clipStartY, startX + parent.getBgWidth(), clipEndY);
        
        float treeOriginX = startX + 15 + (float)scrollX;
        float treeOriginY = startY + 65 + (float)scrollY;
        
        for (int i = 0; i < tree.size(); i++) {
            Technique tech = tree.get(i);
            int nx = i * 80;
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
        context.text(client.font, Component.literal("Technique Tree (Drag/Scroll)"), startX + 15, startY + 10, 0xFFFFAA00);

        // Selected Technique Details Panel (Bottom Left)
        int panelX = startX + 10;
        int panelY = startY + 145;
        if (selectedTech != null) {
            context.text(client.font, Component.literal("Selected: " + selectedTech.name()), panelX, panelY, 0xFFFFFFFF);
            context.text(client.font, Component.literal("Req Lvl: " + selectedTech.unlockLevel()), panelX, panelY + 12, 0xFFFFAA00);
            
            boolean unlocked = accessor.dba$hasTechnique(selectedTech.id());
            if (!unlocked) {
                // Draw Unlock Button
                int btnX = panelX + 100;
                int btnY = panelY;
                int btnW = 80;
                int btnH = 20;
                boolean hoverBtn = (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hoverBtn ? 0xFF55FF88 : 0xFF22AA55);
                context.centeredText(client.font, Component.literal("Unlock (" + selectedTech.apCost() + " AP)"), btnX + btnW / 2, btnY + 6, 0xFFFFFFFF);
            } else {
                context.text(client.font, Component.literal("Unlocked"), panelX + 100, panelY, 0xFF55FF88);
            }
            
            String desc = selectedTech.description();
            String[] lines = desc.split("\\. ");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] + (i < lines.length - 1 ? "." : "");
                context.text(client.font, Component.literal(line), panelX, panelY + 30 + (i * 12), 0xFFAAAAAA);
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
            int slotColor = hoverSlot ? 0xAA55FF88 : 0xAA333333;
            
            context.fill(sx, sy, sx + slotWidth, sy + slotHeight, slotColor);
            context.fill(sx, sy, sx + slotWidth, sy + 2, 0xFF55FF88); // top
            context.fill(sx, sy + slotHeight - 2, sx + slotWidth, sy + slotHeight, 0xFF55FF88); // bottom
            context.fill(sx, sy, sx + 2, sy + slotHeight, 0xFF55FF88); // left
            context.fill(sx + slotWidth - 2, sy, sx + slotWidth, sy + slotHeight, 0xFF55FF88); // right
            
            // Draw Key name in slot
            context.centeredText(client.font, Component.literal(keys[i]), sx + slotWidth / 2, sy - 10, 0xFFFFAA00);
            
            if (equippedId != null && !equippedId.isEmpty()) {
                Technique tech = TechniqueRegistry.getTechnique(Identifier.tryParse(equippedId));
                if (tech != null) {
                    // Shorten name if too long
                    String dName = tech.name();
                    if (client.font.width(dName) > slotWidth) {
                        dName = dName.substring(0, 4) + "..";
                    }
                    context.centeredText(client.font, Component.literal(dName), sx + slotWidth / 2, sy + 16, 0xFFFFFFFF);
                }
            } else {
                context.centeredText(client.font, Component.literal("Empty"), sx + slotWidth / 2, sy + 16, 0xFF888888);
            }
        }
    }

    private void drawNodeScaled(GuiGraphicsExtractor context, Minecraft client, int x, int y, String name, boolean unlocked, boolean selected, float currentZoom) {
        int w = (int)(nodeWidth * currentZoom);
        int h = (int)(nodeHeight * currentZoom);
        int bgColor = unlocked ? 0xEE1E2024 : 0xAA111111;
        int borderColor = selected ? 0xFFFFFFFF : (unlocked ? 0xFFFF7700 : 0xFF555555);
        if (selected) bgColor = 0xAA555555;
        
        context.fill(x, y, x + w, y + h, bgColor);
        
        context.fill(x, y, x + w, y + 2, borderColor); 
        context.fill(x, y + h - 2, x + w, y + h, borderColor);
        context.fill(x, y, x + 2, y + h, borderColor);
        context.fill(x + w - 2, y, x + w, y + h, borderColor); 

        if (currentZoom >= 0.5f) {
            String displayName = name;
            if (client.font.width(displayName) > w - 4) {
                displayName = displayName.substring(0, Math.min(displayName.length(), 7)) + "..";
            }
            context.centeredText(client.font, Component.literal(displayName), x + w / 2, y + (h/2) - 4, unlocked ? 0xFFFFFFFF : 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        
        double mouseX = event.x();
        double mouseY = event.y();

        int startX = parent.getX();
        int startY = parent.getY();

        // 1. Check Equip Slots (top layer)
        int slotsY = startY + 10;
        int slotsStartX = startX + parent.getBgWidth() - (3 * (slotWidth + 10));
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
            int btnX = panelX + 100;
            int btnY = panelY;
            int btnW = 80;
            int btnH = 20;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                ClientPlayNetworking.send(new C2SUnlockTechniquePayload(selectedTech.id()));
                return true;
            }
        }

        // 3. Check Tree Nodes (scaled and panned)
        Identifier raceId = accessor.dba$getRaceId();
        List<Technique> tree = TechniqueRegistry.getTechniquesForRace(raceId);
        
        float treeOriginX = startX + 15 + (float)scrollX;
        float treeOriginY = startY + 65 + (float)scrollY;

        for (int i = 0; i < tree.size(); i++) {
            int nx = i * 80;
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
