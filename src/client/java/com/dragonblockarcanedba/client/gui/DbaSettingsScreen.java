package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.client.config.DbaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Dedicated In-Game Settings Screen for Dragon Block Arcane.
 * Matches the cosmic dark obsidian & glowing cyan/mint aesthetic.
 */
public class DbaSettingsScreen extends Screen {
    private final Screen parent;
    private int bgWidth = 420;
    private int bgHeight = 280;
    private int x;
    private int y;

    // Slider dragging
    private boolean isDraggingTransparency = false;

    // Stars
    private static final int STAR_COUNT = 40;
    private static final int[][] STARS = new int[STAR_COUNT][3];

    static {
        java.util.Random rnd = new java.util.Random(999);
        for (int i = 0; i < STAR_COUNT; i++) {
            STARS[i][0] = rnd.nextInt(500) - 250;
            STARS[i][1] = rnd.nextInt(350) - 175;
            STARS[i][2] = 140 + rnd.nextInt(115);
        }
    }

    public DbaSettingsScreen(Screen parent) {
        super(Component.literal("Dragon Block Arcane Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.bgWidth = Math.min(460, Math.max(360, this.width - 24));
        this.bgHeight = Math.min(300, Math.max(240, this.height - 24));
        this.x = (this.width - bgWidth) / 2;
        this.y = (this.height - bgHeight) / 2;
        this.isDraggingTransparency = false;
        this.clearWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        Minecraft client = Minecraft.getInstance();

        // 1. Dark Cosmic Backdrop
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(x, y, x + bgWidth, y + bgHeight, 0xF00A0E17);

        // Twinkling stars inside frame
        long time = System.currentTimeMillis();
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = x + bgWidth / 2 + STARS[i][0];
            int sy = y + bgHeight / 2 + STARS[i][1];
            if (sx >= x + 2 && sx <= x + bgWidth - 2 && sy >= y + 2 && sy <= y + bgHeight - 2) {
                int twinkle = (int) (Math.sin(time / 350.0 + i) * 30);
                int alpha = Math.max(40, Math.min(255, STARS[i][2] + twinkle));
                int starColor = (alpha << 24) | 0xCCDDEE;
                context.fill(sx, sy, sx + 1, sy + 1, starColor);
            }
        }

        // Glowing border
        int borderCol = 0xAA00E5FF;
        context.fill(x, y, x + bgWidth, y + 1, borderCol);
        context.fill(x, y + bgHeight - 1, x + bgWidth, y + bgHeight, borderCol);
        context.fill(x, y, x + 1, y + bgHeight, borderCol);
        context.fill(x + bgWidth - 1, y, x + bgWidth, y + bgHeight, borderCol);

        // 2. Header Bar
        int headerH = 30;
        context.fill(x + 1, y + 1, x + bgWidth - 1, y + headerH, 0xDD0D131F);
        context.fill(x + 1, y + headerH - 1, x + bgWidth - 1, y + headerH, 0x3300E5FF);

        context.text(client.font, Component.literal("✦ DRAGON BLOCK ARCANE — SETTINGS"), x + 12, y + 10, 0xFF55FF88);

        // Close [ ✕ ] Button
        int closeW = 16;
        int closeH = 16;
        int closeX = x + bgWidth - closeW - 8;
        int closeY = y + 7;
        boolean hoverClose = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH;
        context.fill(closeX, closeY, closeX + closeW, closeY + closeH, hoverClose ? 0xAAFF4444 : 0x33442222);
        context.centeredText(client.font, Component.literal("✕"), closeX + closeW / 2, closeY + 4, hoverClose ? 0xFFFFFFFF : 0xFFAAAAAA);

        // 3. Settings Cards Grid
        int cy = y + 40;
        int cardW = bgWidth - 24;
        int cardX = x + 12;

        // Row 1: Aura Visuals Toggle
        renderOptionCard(context, mouseX, mouseY, cardX, cy, cardW, 26,
            "Aura & Charge Visuals", "Toggles visible energy auras while powering up",
            DbaConfig.chargeVisualsEnabled ? "ENABLED" : "DISABLED",
            DbaConfig.chargeVisualsEnabled ? 0xFF55FF88 : 0xFFFF5555,
            DbaConfig.chargeVisualsEnabled ? 0xDD0E3320 : 0x44331111);
        cy += 32;

        // Row 2: 3D Weapon Models Toggle
        renderOptionCard(context, mouseX, mouseY, cardX, cy, cardW, 26,
            "3D Weapon Models", "Render weapons with custom 3D mesh geometry",
            DbaConfig.use3dWeapons ? "3D ON" : "FLAT 2D",
            DbaConfig.use3dWeapons ? 0xFF00E5FF : 0xFFAAAAAA,
            DbaConfig.use3dWeapons ? 0xDD0B2838 : 0x44111822);
        cy += 32;

        // Row 3: First Person Hand Transparency Slider
        int sliderH = 28;
        context.fill(cardX, cy, cardX + cardW, cy + sliderH, 0xAA0D131F);
        context.fill(cardX, cy, cardX + cardW, cy + 1, 0x33FFFFFF);
        context.fill(cardX, cy + sliderH - 1, cardX + cardW, cy + sliderH, 0x33FFFFFF);
        context.fill(cardX, cy, cardX + 1, cy + sliderH, 0x33FFFFFF);
        context.fill(cardX + cardW - 1, cy, cardX + cardW, cy + sliderH, 0x33FFFFFF);

        context.text(client.font, Component.literal("First-Person Transparency"), cardX + 10, cy + 5, 0xFFFFFFFF);
        String pctStr = DbaConfig.firstPersonTransparency + "%";
        context.text(client.font, Component.literal(pctStr), cardX + cardW - 10 - client.font.width(pctStr), cy + 5, 0xFF00E5FF);

        int sldX = cardX + 10;
        int sldY = cy + 16;
        int sldW = cardW - 20;
        int sldH = 6;
        context.fill(sldX, sldY, sldX + sldW, sldY + sldH, 0x88000000);
        int fill = (int) (sldW * (DbaConfig.firstPersonTransparency / 100.0f));
        context.fill(sldX, sldY, sldX + fill, sldY + sldH, 0xFF00E5FF);
        int knobX = sldX + fill;
        context.fill(knobX - 2, sldY - 1, knobX + 2, sldY + sldH + 1, 0xFFFFFFFF);
        cy += 34;

        // Row 4: Ki Recovery Multiplier (Cycle 0.5x, 1.0x, 1.5x, 2.0x)
        String kiRecStr = String.format(Locale.US, "%.1fx", DbaConfig.baseKiRecoveryMultiplier);
        renderOptionCard(context, mouseX, mouseY, cardX, cy, cardW, 26,
            "Base Ki Recovery Rate", "Passive Ki regeneration multiplier",
            kiRecStr, 0xFFFFAA00, 0x44221A05);
        cy += 32;

        // Row 5: Stat Gain Multiplier (Cycle 0.5x, 1.0x, 1.5x, 2.0x)
        String statGainStr = String.format(Locale.US, "%.1fx", DbaConfig.statGainMultiplier);
        renderOptionCard(context, mouseX, mouseY, cardX, cy, cardW, 26,
            "Stat Gain Multiplier", "Attribute scaling multiplier per training upgrade",
            statGainStr, 0xFFDD88FF, 0x44220E2E);
        cy += 36;

        // 4. Bottom Footer Buttons: [ RESET DEFAULTS ] and [ DONE ]
        int btnW = (cardW - 12) / 2;
        int btnH = 20;
        int b1X = cardX;
        int b2X = b1X + btnW + 12;
        int bY = y + bgHeight - btnH - 10;

        // Reset
        boolean hoverReset = mouseX >= b1X && mouseX <= b1X + btnW && mouseY >= bY && mouseY <= bY + btnH;
        context.fill(b1X, bY, b1X + btnW, bY + btnH, hoverReset ? 0xAA442222 : 0x66221111);
        context.fill(b1X, bY, b1X + btnW, bY + 1, 0x88FF4444);
        context.centeredText(client.font, Component.literal("RESET DEFAULTS"), b1X + btnW / 2, bY + 6, hoverReset ? 0xFFFFFFFF : 0xFFAAAAAA);

        // Done
        boolean hoverDone = mouseX >= b2X && mouseX <= b2X + btnW && mouseY >= bY && mouseY <= bY + btnH;
        context.fill(b2X, bY, b2X + btnW, bY + btnH, hoverDone ? 0xDD00C853 : 0xAA009624);
        context.fill(b2X, bY, b2X + btnW, bY + 1, 0xFF55FF88);
        context.centeredText(client.font, Component.literal("SAVE & CLOSE"), b2X + btnW / 2, bY + 6, 0xFFFFFFFF);
    }

    private void renderOptionCard(GuiGraphicsExtractor context, int mouseX, int mouseY,
                                  int cx, int cy, int cw, int ch,
                                  String title, String subtitle, String btnText, int textCol, int bgCol) {
        Minecraft client = Minecraft.getInstance();

        // Card frame
        context.fill(cx, cy, cx + cw, cy + ch, 0xAA0D131F);
        context.fill(cx, cy, cx + cw, cy + 1, 0x33FFFFFF);
        context.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0x33FFFFFF);
        context.fill(cx, cy, cx + 1, cy + ch, 0x33FFFFFF);
        context.fill(cx + cw - 1, cy, cx + cw, cy + ch, 0x33FFFFFF);

        // Title & Subtitle
        context.text(client.font, Component.literal(title), cx + 10, cy + 5, 0xFFFFFFFF);
        context.text(client.font, Component.literal(subtitle), cx + 10, cy + 15, 0xFF778899);

        // Right button pill
        int bW = 85;
        int bH = 18;
        int bx = cx + cw - bW - 8;
        int by = cy + (ch - bH) / 2;
        boolean hoverBtn = mouseX >= bx && mouseX <= bx + bW && mouseY >= by && mouseY <= by + bH;

        context.fill(bx, by, bx + bW, by + bH, hoverBtn ? 0xDD182838 : bgCol);
        context.fill(bx, by, bx + bW, by + 1, textCol);
        context.fill(bx, by + bH - 1, bx + bW, by + bH, textCol);
        context.fill(bx, by, bx + 1, by + bH, textCol);
        context.fill(bx + bW - 1, by, bx + bW, by + bH, textCol);

        context.centeredText(client.font, Component.literal(btnText), bx + bW / 2, by + 5, hoverBtn ? 0xFFFFFFFF : textCol);
    }

    private void updateTransparencyFromMouse(double mx, int sldX, int sldW) {
        float frac = (float) (mx - sldX) / (float) sldW;
        int pct = Math.max(0, Math.min(100, Math.round(frac * 100.0f)));
        DbaConfig.firstPersonTransparency = pct;
        DbaConfig.save();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        double mx = event.x();
        double my = event.y();

        // Close button
        int closeW = 16;
        int closeH = 16;
        int closeX = x + bgWidth - closeW - 8;
        int closeY = y + 7;
        if (mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH) {
            DbaMenuScreen.playClickSound();
            closeScreen();
            return true;
        }

        int cardW = bgWidth - 24;
        int cardX = x + 12;
        int cy = y + 40;

        // Row 1: Aura Visuals Toggle
        int bW = 85;
        int bH = 18;
        int b1x = cardX + cardW - bW - 8;
        int b1y = cy + (26 - bH) / 2;
        if (mx >= b1x && mx <= b1x + bW && my >= b1y && my <= b1y + bH) {
            DbaMenuScreen.playClickSound();
            DbaConfig.chargeVisualsEnabled = !DbaConfig.chargeVisualsEnabled;
            DbaConfig.save();
            return true;
        }
        cy += 32;

        // Row 2: 3D Weapons Toggle
        int b2x = cardX + cardW - bW - 8;
        int b2y = cy + (26 - bH) / 2;
        if (mx >= b2x && mx <= b2x + bW && my >= b2y && my <= b2y + bH) {
            DbaMenuScreen.playClickSound();
            DbaConfig.use3dWeapons = !DbaConfig.use3dWeapons;
            DbaConfig.save();
            return true;
        }
        cy += 32;

        // Row 3: Transparency Slider
        int sldX = cardX + 10;
        int sldW = cardW - 20;
        int sldY = cy + 16;
        if (mx >= sldX - 4 && mx <= sldX + sldW + 4 && my >= sldY - 4 && my <= sldY + 12) {
            this.isDraggingTransparency = true;
            updateTransparencyFromMouse(mx, sldX, sldW);
            return true;
        }
        cy += 34;

        // Row 4: Ki Recovery Multiplier Cycle
        int b4x = cardX + cardW - bW - 8;
        int b4y = cy + (26 - bH) / 2;
        if (mx >= b4x && mx <= b4x + bW && my >= b4y && my <= b4y + bH) {
            DbaMenuScreen.playClickSound();
            if (DbaConfig.baseKiRecoveryMultiplier == 1.0) DbaConfig.baseKiRecoveryMultiplier = 1.5;
            else if (DbaConfig.baseKiRecoveryMultiplier == 1.5) DbaConfig.baseKiRecoveryMultiplier = 2.0;
            else if (DbaConfig.baseKiRecoveryMultiplier == 2.0) DbaConfig.baseKiRecoveryMultiplier = 0.5;
            else DbaConfig.baseKiRecoveryMultiplier = 1.0;
            DbaConfig.save();
            return true;
        }
        cy += 32;

        // Row 5: Stat Gain Multiplier Cycle
        int b5x = cardX + cardW - bW - 8;
        int b5y = cy + (26 - bH) / 2;
        if (mx >= b5x && mx <= b5x + bW && my >= b5y && my <= b5y + bH) {
            DbaMenuScreen.playClickSound();
            if (DbaConfig.statGainMultiplier == 1.0) DbaConfig.statGainMultiplier = 1.5;
            else if (DbaConfig.statGainMultiplier == 1.5) DbaConfig.statGainMultiplier = 2.0;
            else if (DbaConfig.statGainMultiplier == 2.0) DbaConfig.statGainMultiplier = 0.5;
            else DbaConfig.statGainMultiplier = 1.0;
            DbaConfig.save();
            return true;
        }

        // Bottom buttons: Reset & Done
        int btnW = (cardW - 12) / 2;
        int btnH = 20;
        int rX = cardX;
        int dX = rX + btnW + 12;
        int bY = y + bgHeight - btnH - 10;

        if (mx >= rX && mx <= rX + btnW && my >= bY && my <= bY + btnH) {
            DbaMenuScreen.playClickSound();
            DbaConfig.chargeVisualsEnabled = true;
            DbaConfig.firstPersonTransparency = 0;
            DbaConfig.use3dWeapons = false;
            DbaConfig.baseKiRecoveryMultiplier = 1.0;
            DbaConfig.statGainMultiplier = 1.0;
            DbaConfig.save();
            return true;
        }

        if (mx >= dX && mx <= dX + btnW && my >= bY && my <= bY + btnH) {
            DbaMenuScreen.playClickSound();
            closeScreen();
            return true;
        }

        return super.mouseClicked(event, isRepeat);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingTransparency) {
            int cardW = bgWidth - 24;
            int cardX = x + 12;
            int sldX = cardX + 10;
            int sldW = cardW - 20;
            updateTransparencyFromMouse(event.x(), sldX, sldW);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingTransparency = false;
        return super.mouseReleased(event);
    }

    private void closeScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == 256) { // GLFW_KEY_ESCAPE
            closeScreen();
            return true;
        }
        return super.keyPressed(event);
    }
}
