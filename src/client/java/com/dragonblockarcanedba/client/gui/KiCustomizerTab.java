package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import com.dragonblockarcanedba.ki.KiTechnique;
import com.dragonblockarcanedba.ki.KiTechniqueType;
import com.dragonblockarcanedba.network.C2SKiTechniqueSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Dedicated Ki Crafter Tab:
 * - Left: Scrollable controls with docked bottom Save button (zero overlaps!).
 * - Right: Animated glowing Ki Core Preview Chamber.
 */
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
    private final String[] colorNames = {"Azure", "Crimson", "Emerald", "Golden", "Violet", "Pure"};

    private int targetSlot = 0; // 0, 1, 2 for F7, F8, F9
    private int typeIdx = 0;
    private int percentUsed = 50;
    private int colorIdx = 0;
    private boolean isBarrage = false;

    private double scrollY = 0;
    private boolean isDraggingSlider = false;
    private boolean loadedInitial = false;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.isDraggingSlider = false;
        this.scrollY = 0;
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
                typeIdx = 0;
                for (int i = 0; i < types.length; i++) {
                    if (types[i] == tech.type) {
                        typeIdx = i;
                        break;
                    }
                }
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
        if (client.player == null) return;

        int startX = parent.getContentX();
        int startY = parent.getContentY();
        int width = parent.getContentWidth();
        int height = parent.getContentHeight();

        // =========================================================================
        // 1. LEFT COLUMN: CRAFTER CONTROLS (width ~ 280-320)
        // =========================================================================
        int leftW = Math.min(310, width - 150);
        int leftX = startX + 6;
        int leftY = startY + 6;
        int leftH = height - 12;

        context.fill(leftX, leftY, leftX + leftW, leftY + leftH, 0xAA0D131F);
        context.fill(leftX, leftY, leftX + leftW, leftY + 1, 0x4400E5FF);
        context.fill(leftX, leftY + leftH - 1, leftX + leftW, leftY + leftH, 0x4400E5FF);
        context.fill(leftX, leftY, leftX + 1, leftY + leftH, 0x4400E5FF);
        context.fill(leftX + leftW - 1, leftY, leftX + leftW, leftY + leftH, 0x4400E5FF);

        // Docked Bottom Save Button (24px high)
        int saveH = 22;
        int saveW = leftW - 16;
        int saveX = leftX + 8;
        int saveY = leftY + leftH - saveH - 6;

        boolean hoverSave = mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= saveY && mouseY <= saveY + saveH;
        context.fill(saveX, saveY, saveX + saveW, saveY + saveH, hoverSave ? 0xDD00C853 : 0xAA009624);
        context.fill(saveX, saveY, saveX + saveW, saveY + 1, 0xFF55FF88);
        context.fill(saveX, saveY + saveH - 1, saveX + saveW, saveY + saveH, 0xFF55FF88);
        context.fill(saveX, saveY, saveX + 1, saveY + saveH, 0xFF55FF88);
        context.fill(saveX + saveW - 1, saveY, saveX + saveW, saveY + saveH, 0xFF55FF88);

        String saveTxt = "SAVE TECHNIQUE TO SLOT " + (targetSlot + 1);
        context.centeredText(client.font, Component.literal(saveTxt), saveX + saveW / 2, saveY + 7, 0xFFFFFFFF);

        // Separator above save button
        context.fill(leftX + 4, saveY - 4, leftX + leftW - 4, saveY - 3, 0x3300E5FF);

        // Scrollable Controls Region (above save button)
        int scrollAreaY = leftY + 4;
        int scrollAreaH = saveY - scrollAreaY - 6;

        int totalContentH = 220; // Natural height of all controls
        int maxScroll = Math.max(0, totalContentH - scrollAreaH);
        scrollY = Math.max(-maxScroll, Math.min(0, scrollY));

        context.enableScissor(leftX, scrollAreaY, leftX + leftW, scrollAreaY + scrollAreaH);

        int cy = scrollAreaY + 4 + (int) scrollY;

        // 1. Slot Selector
        context.text(client.font, Component.literal("TARGET QUICK-SLOT:"), leftX + 10, cy, 0xFF55FF88);
        cy += 11;

        String[] keys = {
            DragonBlockArcaneDBAClient.techSlot1Key.getTranslatedKeyMessage().getString().toUpperCase(),
            DragonBlockArcaneDBAClient.techSlot2Key.getTranslatedKeyMessage().getString().toUpperCase(),
            DragonBlockArcaneDBAClient.techSlot3Key.getTranslatedKeyMessage().getString().toUpperCase()
        };

        int slotBtnW = (leftW - 28) / 3;
        for (int i = 0; i < 3; i++) {
            int sx = leftX + 10 + i * (slotBtnW + 4);
            int sy = cy;
            int sh = 16;
            boolean selected = (i == targetSlot);
            boolean hoverS = mouseX >= sx && mouseX <= sx + slotBtnW && mouseY >= sy && mouseY <= sy + sh && mouseY < saveY - 4;

            int bg = selected ? 0xDD00C853 : (hoverS ? 0x66223344 : 0x44111822);
            int border = selected ? 0xFF55FF88 : 0x44FFFFFF;

            context.fill(sx, sy, sx + slotBtnW, sy + sh, bg);
            context.fill(sx, sy, sx + slotBtnW, sy + 1, border);
            context.fill(sx, sy + sh - 1, sx + slotBtnW, sy + sh, border);
            context.fill(sx, sy, sx + 1, sy + sh, border);
            context.fill(sx + slotBtnW - 1, sy, sx + slotBtnW, sy + sh, border);

            String label = "SLOT " + (i + 1) + " [" + keys[i] + "]";
            context.centeredText(client.font, Component.literal(label), sx + slotBtnW / 2, sy + 4, selected ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        cy += 22;

        // 2. Attack Archetype Chips
        context.text(client.font, Component.literal("ATTACK ARCHETYPE:"), leftX + 10, cy, 0xFF55FF88);
        cy += 11;

        int typeCols = 3;
        int typeBtnW = (leftW - 28) / typeCols;
        for (int i = 0; i < types.length; i++) {
            int col = i % typeCols;
            int row = i / typeCols;
            int tx = leftX + 10 + col * (typeBtnW + 4);
            int ty = cy + row * 18;
            int th = 15;

            boolean selected = (i == typeIdx);
            boolean hoverT = mouseX >= tx && mouseX <= tx + typeBtnW && mouseY >= ty && mouseY <= ty + th && mouseY < saveY - 4;

            int bg = selected ? 0xDD0B3848 : (hoverT ? 0x66223344 : 0x44111822);
            int border = selected ? 0xFF00E5FF : 0x33FFFFFF;
            int textCol = selected ? 0xFFFFFFFF : (hoverT ? 0xFFCCDDEE : 0xFFAAAAAA);

            context.fill(tx, ty, tx + typeBtnW, ty + th, bg);
            context.fill(tx, ty, tx + typeBtnW, ty + 1, border);
            context.fill(tx, ty + th - 1, tx + typeBtnW, ty + th, border);
            context.fill(tx, ty, tx + 1, ty + th, border);
            context.fill(tx + typeBtnW - 1, ty, tx + typeBtnW, ty + th, border);

            context.centeredText(client.font, Component.literal(types[i].displayName()), tx + typeBtnW / 2, ty + 4, textCol);
        }
        cy += 40;

        // 3. Aura Color Chips
        context.text(client.font, Component.literal("AURA ENERGY COLOR:"), leftX + 10, cy, 0xFF55FF88);
        cy += 11;

        int colorBtnW = (leftW - 32) / colors.length;
        for (int i = 0; i < colors.length; i++) {
            int cx = leftX + 10 + i * (colorBtnW + 4);
            int cyBtn = cy;
            int ch = 14;
            boolean selected = (i == colorIdx);

            context.fill(cx, cyBtn, cx + colorBtnW, cyBtn + ch, colors[i]);
            if (selected) {
                context.fill(cx - 2, cyBtn - 2, cx + colorBtnW + 2, cyBtn, 0xFFFFFFFF);
                context.fill(cx - 2, cyBtn + ch, cx + colorBtnW + 2, cyBtn + ch + 2, 0xFFFFFFFF);
                context.fill(cx - 2, cyBtn, cx, cyBtn + ch, 0xFFFFFFFF);
                context.fill(cx + colorBtnW, cyBtn, cx + colorBtnW + 2, cyBtn + ch, 0xFFFFFFFF);
            }
        }
        cy += 20;

        // 4. Mode Switcher (if BLAST)
        if (types[typeIdx] == KiTechniqueType.BLAST) {
            context.text(client.font, Component.literal("DISCHARGE MODE:"), leftX + 10, cy, 0xFF55FF88);
            cy += 11;

            int modeW = (leftW - 24) / 2;
            int m1X = leftX + 10;
            int m2X = m1X + modeW + 4;
            int mh = 15;

            // Single
            context.fill(m1X, cy, m1X + modeW, cy + mh, !isBarrage ? 0xDD00C853 : 0x44111822);
            context.centeredText(client.font, Component.literal("Single Fire"), m1X + modeW / 2, cy + 4, !isBarrage ? 0xFFFFFFFF : 0xFFAAAAAA);

            // Barrage
            context.fill(m2X, cy, m2X + modeW, cy + mh, isBarrage ? 0xDD00C853 : 0x44111822);
            context.centeredText(client.font, Component.literal("Rapid Barrage"), m2X + modeW / 2, cy + 4, isBarrage ? 0xFFFFFFFF : 0xFFAAAAAA);

            cy += 20;
        }

        // 5. Ki Power / Cost Slider
        if (types[typeIdx] == KiTechniqueType.EXPLOSION) {
            context.text(client.font, Component.literal("POWER: SELF-DESTRUCT (100% KI)"), leftX + 10, cy, 0xFFFF5555);
            cy += 16;
        } else {
            String pStr = "KI POWER: " + percentUsed + "%";
            context.text(client.font, Component.literal(pStr), leftX + 10, cy, 0xFFFFAA00);
            cy += 10;

            int sldX = leftX + 10;
            int sldY = cy;
            int sldW = leftW - 20;
            int sldH = 6;

            context.fill(sldX, sldY, sldX + sldW, sldY + sldH, 0x88000000);
            int fill = (int) (sldW * (percentUsed / 100.0f));
            context.fill(sldX, sldY, sldX + fill, sldY + sldH, 0xFFFFAA00);
            // Knob
            int knobX = sldX + fill;
            context.fill(knobX - 2, sldY - 1, knobX + 2, sldY + sldH + 1, 0xFFFFFFFF);
            cy += 14;
        }

        context.disableScissor();

        // =========================================================================
        // 2. RIGHT COLUMN: ANIMATED KI CORE CHAMBER
        // =========================================================================
        int rightX = leftX + leftW + 8;
        int rightW = width - leftW - 14;
        int rightY = startY + 6;
        int rightH = height - 12;

        context.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0xAA0A0E17);
        context.fill(rightX, rightY, rightX + rightW, rightY + 1, 0x4400E5FF);
        context.fill(rightX, rightY + rightH - 1, rightX + rightW, rightY + rightH, 0x4400E5FF);
        context.fill(rightX, rightY, rightX + 1, rightY + rightH, 0x4400E5FF);
        context.fill(rightX + rightW - 1, rightY, rightX + rightW, rightY + rightH, 0x4400E5FF);

        context.text(client.font, Component.literal("ATTACK SIMULATION"), rightX + 10, rightY + 8, 0xFF00E5FF);

        // Animated Orb Core (Proportional to window height)
        int coreX = rightX + rightW / 2;
        int coreY = rightY + Math.min(65, rightH / 4);
        long time = System.currentTimeMillis();
        int pulse = (int) (Math.sin(time / 200.0) * 3);
        int orbRadius = 18 + pulse;
        int activeColor = colors[colorIdx];

        // Outer Aura
        for (int r = orbRadius + 6; r >= orbRadius; r -= 2) {
            int alpha = (int) (40 * ((double) (r - orbRadius) / 6.0));
            int auraCol = (alpha << 24) | (activeColor & 0x00FFFFFF);
            for (int y = -r; y <= r; y++) {
                int w = (int) Math.sqrt(r * r - y * y);
                context.fill(coreX - w, coreY + y, coreX + w + 1, coreY + y + 1, auraCol);
            }
        }
        // Solid Core
        for (int y = -orbRadius; y <= orbRadius; y++) {
            int w = (int) Math.sqrt(orbRadius * orbRadius - y * y);
            context.fill(coreX - w, coreY + y, coreX + w + 1, coreY + y + 1, activeColor);
        }
        // White Hot Center
        int inner = orbRadius / 2;
        for (int y = -inner; y <= inner; y++) {
            int w = (int) Math.sqrt(inner * inner - y * y);
            context.fill(coreX - w, coreY + y, coreX + w + 1, coreY + y + 1, 0xFFFFFFFF);
        }

        // Summary Details Card below Orb
        int cardY = coreY + orbRadius + 14;
        int cardH = rightY + rightH - cardY - 8;
        if (cardH > 40) {
            context.fill(rightX + 8, cardY, rightX + rightW - 8, rightY + rightH - 8, 0x66060910);

            String typeName = types[typeIdx].displayName();
            if (types[typeIdx] == KiTechniqueType.BLAST && isBarrage) typeName = "Ki Barrage";
            context.text(client.font, Component.literal("Form: " + typeName), rightX + 14, cardY + 6, 0xFFFFFFFF);
            context.text(client.font, Component.literal("Color: " + colorNames[colorIdx]), rightX + 14, cardY + 18, activeColor);

            int costPct = (types[typeIdx] == KiTechniqueType.EXPLOSION) ? 100 : percentUsed;
            context.text(client.font, Component.literal("Ki Cost: " + costPct + "%"), rightX + 14, cardY + 30, 0xFF00E5FF);

            String modeName = (types[typeIdx] == KiTechniqueType.BLAST) ? (isBarrage ? "Rapid Barrage" : "Single Shot") : "Standard Flow";
            context.text(client.font, Component.literal("Discharge: " + modeName), rightX + 14, cardY + 42, 0xFFFFAA00);

            String slotBound = "Bound: Slot " + (targetSlot + 1) + " [" + keys[targetSlot] + "]";
            context.text(client.font, Component.literal(slotBound), rightX + 14, cardY + 54, 0xFF55FF88);
        }
    }

    private void updatePercentFromMouse(double mx, int sldX, int sldW) {
        float frac = (float) (mx - sldX) / (float) sldW;
        percentUsed = Math.max(5, Math.min(100, Math.round(frac * 100.0f)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int leftW = Math.min(310, parent.getContentWidth() - 150);
        int leftX = parent.getContentX() + 6;
        if (mouseX >= leftX && mouseX <= leftX + leftW) {
            this.scrollY += verticalAmount * 18;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mx = event.x();
        double my = event.y();

        int startX = parent.getContentX();
        int startY = parent.getContentY();
        int width = parent.getContentWidth();
        int height = parent.getContentHeight();

        int leftW = Math.min(310, width - 150);
        int leftX = startX + 6;
        int leftY = startY + 6;
        int leftH = height - 12;

        // Save Button Click (Docked at bottom)
        int saveH = 22;
        int saveW = leftW - 16;
        int saveX = leftX + 8;
        int saveY = leftY + leftH - saveH - 6;
        if (mx >= saveX && mx <= saveX + saveW && my >= saveY && my <= saveY + saveH) {
            DbaMenuScreen.playClickSound();
            ClientPlayNetworking.send(new C2SKiTechniqueSavePayload(
                targetSlot,
                types[typeIdx].name(),
                percentUsed,
                colors[colorIdx],
                isBarrage
            ));
            return true;
        }

        // Scrollable region clicks
        int scrollAreaY = leftY + 4;
        int scrollAreaH = saveY - scrollAreaY - 6;
        if (my < scrollAreaY || my > scrollAreaY + scrollAreaH) return false;

        int cy = scrollAreaY + 4 + (int) scrollY;

        // 1. Slot Selector
        cy += 11;
        int slotBtnW = (leftW - 28) / 3;
        for (int i = 0; i < 3; i++) {
            int sx = leftX + 10 + i * (slotBtnW + 4);
            if (mx >= sx && mx <= sx + slotBtnW && my >= cy && my <= cy + 16) {
                DbaMenuScreen.playClickSound();
                loadSlotData(i);
                return true;
            }
        }
        cy += 22;

        // 2. Type Chips
        cy += 11;
        int typeCols = 3;
        int typeBtnW = (leftW - 28) / typeCols;
        for (int i = 0; i < types.length; i++) {
            int col = i % typeCols;
            int row = i / typeCols;
            int tx = leftX + 10 + col * (typeBtnW + 4);
            int ty = cy + row * 18;
            if (mx >= tx && mx <= tx + typeBtnW && my >= ty && my <= ty + 15) {
                DbaMenuScreen.playClickSound();
                typeIdx = i;
                if (types[typeIdx] != KiTechniqueType.BLAST) isBarrage = false;
                return true;
            }
        }
        cy += 40;

        // 3. Color Swatches
        cy += 11;
        int colorBtnW = (leftW - 32) / colors.length;
        for (int i = 0; i < colors.length; i++) {
            int cx = leftX + 10 + i * (colorBtnW + 4);
            if (mx >= cx && mx <= cx + colorBtnW && my >= cy && my <= cy + 14) {
                DbaMenuScreen.playClickSound();
                colorIdx = i;
                return true;
            }
        }
        cy += 20;

        // 4. Mode Buttons (if BLAST)
        if (types[typeIdx] == KiTechniqueType.BLAST) {
            cy += 11;
            int modeW = (leftW - 24) / 2;
            int m1X = leftX + 10;
            int m2X = m1X + modeW + 4;
            if (mx >= m1X && mx <= m1X + modeW && my >= cy && my <= cy + 15) {
                DbaMenuScreen.playClickSound();
                isBarrage = false;
                return true;
            }
            if (mx >= m2X && mx <= m2X + modeW && my >= cy && my <= cy + 15) {
                DbaMenuScreen.playClickSound();
                isBarrage = true;
                return true;
            }
            cy += 20;
        }

        // 5. Ki Power Slider
        if (types[typeIdx] != KiTechniqueType.EXPLOSION) {
            cy += 10;
            int sldX = leftX + 10;
            int sldW = leftW - 20;
            if (mx >= sldX - 4 && mx <= sldX + sldW + 4 && my >= cy - 4 && my <= cy + 10) {
                this.isDraggingSlider = true;
                updatePercentFromMouse(mx, sldX, sldW);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingSlider && types[typeIdx] != KiTechniqueType.EXPLOSION) {
            int leftW = Math.min(310, parent.getContentWidth() - 150);
            int leftX = parent.getContentX() + 6;
            int sldX = leftX + 10;
            int sldW = leftW - 20;
            updatePercentFromMouse(event.x(), sldX, sldW);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingSlider = false;
        return false;
    }
}
