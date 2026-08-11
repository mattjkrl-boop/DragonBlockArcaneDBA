package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.ki.KiTechnique;
import com.dragonblockarcanedba.ki.KiTechniqueType;
import com.dragonblockarcanedba.network.C2SKiTechniqueSavePayload;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    
    private int targetSlot = 0; // 0, 1, 2 for F7, F8, F9
    private int typeIdx = 0;
    private int percentUsed = 50;
    private int colorIdx = 0;
    private boolean isBarrage = false;

    private boolean loadedInitial = false;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        
        if (!loadedInitial) {
            loadSlotData(0);
            loadedInitial = true;
        }
    }

    private void loadSlotData(int slot) {
        this.targetSlot = slot;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
            KiTechnique tech = accessor.dba$getKiTechniqueSlot(slot);
            if (tech != null && !tech.isEmpty) {
                // Find type idx
                typeIdx = 0;
                for (int i = 0; i < types.length; i++) {
                    if (types[i] == tech.type) {
                        typeIdx = i;
                        break;
                    }
                }
                // Find color idx
                colorIdx = 0;
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i] == tech.color) {
                        colorIdx = i;
                        break;
                    }
                }
                percentUsed = tech.usedPercent;
                isBarrage = tech.isBarrage;
            } else {
                typeIdx = 0;
                percentUsed = 50;
                colorIdx = 0;
                isBarrage = false;
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();

        // Slot Selector (Top Row)
        String[] keys = {
            DragonBlockArcaneDBAClient.techSlot1Key.getTranslatedKeyMessage().getString(),
            DragonBlockArcaneDBAClient.techSlot2Key.getTranslatedKeyMessage().getString(),
            DragonBlockArcaneDBAClient.techSlot3Key.getTranslatedKeyMessage().getString()
        };
        
        for (int i = 0; i < 3; i++) {
            int slotX = startX + 15 + (i * 70);
            int slotY = startY + 15;
            int slotW = 65;
            int slotH = 20;
            boolean selected = (i == targetSlot);
            int bgColor = selected ? 0xFF22AA55 : 0x44222222;
            
            context.fill(slotX, slotY, slotX + slotW, slotY + slotH, bgColor);
            if (!selected) {
                context.fill(slotX, slotY, slotX + slotW, slotY + 1, 0x44FFFFFF);
                context.fill(slotX, slotY + slotH - 1, slotX + slotW, slotY + slotH, 0x44FFFFFF);
                context.fill(slotX, slotY, slotX + 1, slotY + slotH, 0x44FFFFFF);
                context.fill(slotX + slotW - 1, slotY, slotX + slotW, slotY + slotH, 0x44FFFFFF);
            }
            context.centeredText(client.font, Component.literal("Slot " + (i+1) + " [" + keys[i].toUpperCase() + "]"), slotX + slotW/2, slotY + 6, selected ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        // Type Selector
        context.text(client.font, Component.literal("Type:"), startX + 15, startY + 40, 0xFF55FF88);
        for (int i = 0; i < types.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int tx = startX + 15 + (col * 75);
            int ty = startY + 55 + (row * 20);
            int tw = 70;
            int th = 16;
            boolean selected = (i == typeIdx);
            
            // Sleek hover check
            boolean hoverT = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th;
            int bgColor = selected ? 0xFF55FF88 : (hoverT ? 0x66222222 : 0x44111111);
            int textColor = selected ? 0xFF111111 : 0xFFFFFFFF;
            
            context.fill(tx, ty, tx + tw, ty + th, bgColor);
            if (!selected) {
                context.fill(tx, ty, tx + 1, ty + th, 0x4455FF88); // Left accent
            }
            context.centeredText(client.font, Component.literal(types[i].displayName()), tx + tw/2, ty + 4, textColor);
        }
        
        // Color Selector
        context.text(client.font, Component.literal("Color:"), startX + 15, startY + 120, 0xFF55FF88);
        for (int i = 0; i < colors.length; i++) {
            int cx = startX + 15 + (i * 22);
            int cy = startY + 135;
            int cw = 16;
            int ch = 16;
            boolean selected = (i == colorIdx);
            
            context.fill(cx, cy, cx + cw, cy + ch, colors[i]);
            if (selected) {
                context.fill(cx - 2, cy - 2, cx + cw + 2, cy, 0xFFFFFFFF); // Top
                context.fill(cx - 2, cy + ch, cx + cw + 2, cy + ch + 2, 0xFFFFFFFF); // Bottom
                context.fill(cx - 2, cy, cx, cy + ch, 0xFFFFFFFF); // Left
                context.fill(cx + cw, cy, cx + cw + 2, cy + ch, 0xFFFFFFFF); // Right
            }
        }
        
        // Mode (if Blast)
        int nextY = 160;
        if (types[typeIdx] == KiTechniqueType.BLAST) {
            context.text(client.font, Component.literal("Mode:"), startX + 15, startY + 160, 0xFF55FF88);
            // Single
            int mx = startX + 15;
            int my = startY + 175;
            context.fill(mx, my, mx + 50, my + 16, !isBarrage ? 0xFF55FF88 : 0x44111111);
            if (isBarrage) context.fill(mx, my, mx + 1, my + 16, 0x4455FF88);
            context.centeredText(client.font, Component.literal("Single"), mx + 25, my + 4, !isBarrage ? 0xFF111111 : 0xFFFFFFFF);
            // Barrage
            mx += 55;
            context.fill(mx, my, mx + 50, my + 16, isBarrage ? 0xFF55FF88 : 0x44111111);
            if (!isBarrage) context.fill(mx, my, mx + 1, my + 16, 0x4455FF88);
            context.centeredText(client.font, Component.literal("Barrage"), mx + 25, my + 4, isBarrage ? 0xFF111111 : 0xFFFFFFFF);
            
            nextY = 200;
        }
        
        // Save Button
        int saveW = 105;
        int saveH = 18;
        int saveX = startX + 15;
        int saveY = startY + nextY;
        boolean hoverSave = mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + saveH;
        context.fill(saveX, saveY, saveX + saveW, saveY + saveH, hoverSave ? 0xFF55FF88 : 0xFF22AA55);
        context.centeredText(client.font, Component.literal("SAVE TECHNIQUE"), saveX + saveW/2, saveY + 5, hoverSave ? 0xFF111111 : 0xFFFFFFFF);

        // Preview Area (Right side)
        int previewX = startX + 175;
        int previewY = startY + 45;
        int previewW = width - 185;
        int previewH = 175;
        
        context.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0x44000000);
        int activeColor = colors[colorIdx];
        context.fill(previewX, previewY, previewX + previewW, previewY + 2, activeColor); // Top
        context.fill(previewX, previewY + previewH - 2, previewX + previewW, previewY + previewH, activeColor); // Bottom
        context.fill(previewX, previewY, previewX + 2, previewY + previewH, activeColor); // Left
        context.fill(previewX + previewW - 2, previewY, previewX + previewW, previewY + previewH, activeColor); // Right
        
        context.centeredText(client.font, Component.literal("Summary"), previewX + previewW / 2, previewY + 15, 0xFFFFFFFF);
        String displayName = types[typeIdx].displayName();
        if (types[typeIdx] == KiTechniqueType.BLAST && isBarrage) displayName = "Ki Barrage";
        context.centeredText(client.font, Component.literal(displayName), previewX + previewW / 2, previewY + 35, activeColor);
        
        if (types[typeIdx] == KiTechniqueType.EXPLOSION) {
            context.centeredText(client.font, Component.literal("Cost: 100%"), previewX + previewW / 2, previewY + 55, 0xFFFF5555);
            context.centeredText(client.font, Component.literal("Self Damage!"), previewX + previewW / 2, previewY + 70, 0xFFFF2222);
        } else {
            // Percent Used Slider
            context.centeredText(client.font, Component.literal("Ki Cost: " + percentUsed + "%"), previewX + previewW / 2, previewY + 55, 0xFF55FFFF);
            int px = previewX + 10;
            int py = previewY + 70;
            int pw = previewW - 20;
            int ph = 6;
            context.fill(px, py, px + pw, py + ph, 0x44000000);
            context.fill(px, py, px + (int)(pw * (percentUsed / 100.0f)), py + ph, 0xFF55FFFF);
            // Slider border
            context.fill(px - 1, py - 1, px + pw + 1, py, 0x55FFFFFF);
            context.fill(px - 1, py + ph, px + pw + 1, py + ph + 1, 0x55FFFFFF);
            context.fill(px - 1, py, px, py + ph, 0x55FFFFFF);
            context.fill(px + pw, py, px + pw + 1, py + ph, 0x55FFFFFF);
        }
        
        // Color swatch graphic inside preview
        context.fill(previewX + previewW / 2 - 12, previewY + 100, previewX + previewW / 2 + 12, previewY + 124, activeColor);
        context.fill(previewX + previewW / 2 - 10, previewY + 102, previewX + previewW / 2 + 10, previewY + 122, 0xFFFFFFFF);
    }
    
    private void updatePercentFromMouse(double mx, int px, int pw) {
        double ratio = (mx - px) / (double)pw;
        if (ratio < 0.01) ratio = 0.01;
        if (ratio > 1.0) ratio = 1.0;
        percentUsed = (int)(ratio * 100);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mx = event.x();
        double my = event.y();
        int startX = parent.getContentX();
        int startY = parent.getY();
        
        int previewX = startX + 175;
        int previewY = startY + 45;
        int previewW = parent.getContentWidth() - 185;
        
        int px = previewX + 10;
        int py = previewY + 70;
        int pw = previewW - 20;
        int ph = 6;
        
        if (mx >= px && mx <= px + pw && my >= py - 2 && my <= py + ph + 2) {
            updatePercentFromMouse(mx, px, pw);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mx = event.x();
        double my = event.y();
        int startX = parent.getContentX();
        int startY = parent.getY();
        
        // Slots
        for (int i = 0; i < 3; i++) {
            int slotX = startX + 15 + (i * 70);
            int slotY = startY + 15;
            int slotW = 65;
            int slotH = 20;
            if (mx >= slotX && mx <= slotX + slotW && my >= slotY && my <= slotY + slotH) {
                loadSlotData(i);
                return true;
            }
        }
        
        // Types
        for (int i = 0; i < types.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int tx = startX + 15 + (col * 75);
            int ty = startY + 55 + (row * 20);
            int tw = 70;
            int th = 16;
            if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + th) {
                typeIdx = i;
                if (types[typeIdx] != KiTechniqueType.BLAST) isBarrage = false;
                return true;
            }
        }
        
        // Colors
        for (int i = 0; i < colors.length; i++) {
            int cx = startX + 15 + (i * 22);
            int cy = startY + 135;
            int cw = 16;
            int ch = 16;
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch) {
                colorIdx = i;
                return true;
            }
        }
        
        // Mode
        int nextY = 160;
        if (types[typeIdx] == KiTechniqueType.BLAST) {
            int mx1 = startX + 15;
            int my1 = startY + 175;
            if (mx >= mx1 && mx <= mx1 + 50 && my >= my1 && my <= my1 + 16) {
                isBarrage = false;
                return true;
            }
            int mx2 = mx1 + 55;
            if (mx >= mx2 && mx <= mx2 + 50 && my >= my1 && my <= my1 + 16) {
                isBarrage = true;
                return true;
            }
            nextY = 200;
        }
        
        // Slider Click
        int previewX = startX + 175;
        int previewY = startY + 45;
        int previewW = parent.getContentWidth() - 185;
        
        int px = previewX + 10;
        int py = previewY + 70;
        int pw = previewW - 20;
        int ph = 6;
        
        if (mx >= px && mx <= px + pw && my >= py - 2 && my <= py + ph + 2) {
            updatePercentFromMouse(mx, px, pw);
            return true;
        }
        
        // Save
        int saveW = 105;
        int saveH = 18;
        int saveX = startX + 15;
        int saveY = startY + nextY;
        if (mx >= saveX && mx <= saveX + saveW && my >= saveY && my <= saveY + saveH) {
            ClientPlayNetworking.send(new C2SKiTechniqueSavePayload(
                targetSlot,
                types[typeIdx].name(),
                percentUsed,
                colors[colorIdx],
                isBarrage
            ));
            return true;
        }

        return false;
    }
}
