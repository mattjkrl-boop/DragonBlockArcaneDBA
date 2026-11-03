package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.ki.KiTechnique;
import com.dragonblockarcanedba.ki.KiTechniqueType;
import com.dragonblockarcanedba.network.C2SKiTechniqueSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Ki craft. Slots and the type list stay put. Color, charge, size, and the
 * ki slider scroll beside them, and Save still writes the slot.
 */
public class KiCustomizerTab implements MenuTab {
    private static final int[] COLORS = {
            0xFF2F6BFF, 0xFFD64545, 0xFF3CB371, 0xFFE2B657, 0xFF9B59D0, 0xFFF4F0E6
    };
    private static final String[] COLOR_NAMES = {"Azure", "Crimson", "Emerald", "Gold", "Violet", "White"};
    private static final double SIZE_MIN = 0.5;

    private final KiTechniqueType[] types = KiTechniqueType.values();

    private DbaMenuScreen parent;
    private int targetSlot;
    private int typeIdx;
    private int percentUsed = 50;
    private int colorIdx;
    private boolean barrage;
    private int chargeTicks;
    private double size = KiTechnique.DEFAULT_SIZE;
    private int scroll;
    private Drag dragging = Drag.NONE;
    private boolean loaded;

    private enum Drag { NONE, KI, CHARGE, SIZE }

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.dragging = Drag.NONE;
        if (!loaded) {
            loadSlot(0);
            loaded = true;
        }
    }

    public void focusSlot(int slot) {
        loadSlot(slot);
        loaded = true;
    }

    private void loadSlot(int slot) {
        this.targetSlot = Math.max(0, Math.min(2, slot));
        this.scroll = 0;
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor)) {
            return;
        }
        KiTechnique tech = accessor.dba$getKiTechniqueSlot(targetSlot);
        if (tech == null || tech.isEmpty) {
            typeIdx = 0;
            percentUsed = 50;
            colorIdx = 0;
            barrage = false;
            chargeTicks = KiTechnique.DEFAULT_CHARGE_TICKS;
            size = KiTechnique.DEFAULT_SIZE;
            return;
        }
        typeIdx = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == tech.type) {
                typeIdx = i;
                break;
            }
        }
        colorIdx = 0;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == tech.color) {
                colorIdx = i;
                break;
            }
        }
        percentUsed = tech.usedPercent <= 0 ? 50 : tech.usedPercent;
        barrage = tech.isBarrage && types[typeIdx] == KiTechniqueType.BLAST;
        chargeTicks = tech.getChargeTicks();
        size = tech.getSize();
    }

    private int saveCost() {
        KiTechniqueType type = types[typeIdx];
        int percent = type == KiTechniqueType.EXPLOSION ? 100 : percentUsed;
        return PlayerStats.getKiAttackSaveCost(type, percent, barrage && type == KiTechniqueType.BLAST);
    }

    private String typeId() {
        return types[typeIdx] == KiTechniqueType.BARRIER ? "barrier" : types[typeIdx].name();
    }

    private Layout layout() {
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        Layout layout = new Layout();
        layout.x = x;
        layout.y = y;
        layout.w = w;
        layout.h = h;
        int slotW = Math.max(36, (w - 8) / 3);
        layout.slotW = slotW;
        layout.slotY = y;
        for (int i = 0; i < 3; i++) {
            layout.slotX[i] = x + i * (slotW + 4);
        }
        layout.bodyY = y + 28;
        layout.bodyH = h - 28;
        layout.leftW = Math.min(168, Math.max(112, w * 38 / 100));
        layout.rightX = x + layout.leftW + 8;
        layout.rightW = w - layout.leftW - 8;
        layout.saveH = DbaUi.BUTTON_H;
        layout.saveY = layout.bodyY + layout.bodyH - layout.saveH;
        int count = types.length;
        int gaps = 2 * Math.max(0, count - 1);
        layout.typeH = Math.min(20, Math.max(14, (layout.bodyH - gaps) / Math.max(1, count)));
        layout.colorY = layout.bodyY;
        layout.swatch = Math.max(10, Math.min(18, (layout.rightW - (COLORS.length - 1) * 4) / Math.max(1, COLORS.length)));
        layout.viewY = layout.colorY + 14 + layout.swatch + 6;
        layout.viewH = Math.max(20, layout.saveY - 6 - layout.viewY);

        layout.showMode = types[typeIdx] == KiTechniqueType.BLAST;
        layout.showKi = types[typeIdx] != KiTechniqueType.EXPLOSION;
        int natural = (layout.showMode ? 34 : 0) + (layout.showKi ? 32 : 16) + 64;
        int maxScroll = Math.max(0, natural - layout.viewH);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;

        int cursor = layout.viewY - scroll;
        if (layout.showMode) {
            layout.modeY = cursor + 12;
            layout.modeW = Math.max(40, (layout.rightW - 4) / 2);
            cursor = layout.modeY + 22;
        }
        if (layout.showKi) {
            layout.kiY = cursor;
            cursor += 32;
        } else {
            layout.noteY = cursor;
            cursor += 16;
        }
        layout.chargeY = cursor;
        cursor += 32;
        layout.sizeY = cursor;
        layout.contentBottom = cursor + 32;
        return layout;
    }

    private boolean inView(Layout layout, double my) {
        return my >= layout.viewY && my < layout.viewY + layout.viewH;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        Layout layout = layout();
        boolean canSave = true;
        int cost = saveCost();
        if (client.player instanceof PlayerStatsAccessor accessor) {
            canSave = accessor.dba$getStatPoints() >= cost;
        }

        for (int i = 0; i < 3; i++) {
            boolean on = i == targetSlot;
            boolean hover = DbaUi.hit(mouseX, mouseY, layout.slotX[i], layout.slotY, layout.slotW, 20);
            String label = (i + 1) + "  " + DbaUi.slotKey(i);
            DbaUi.button(context, font, layout.slotX[i], layout.slotY, layout.slotW, 20, label, hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        for (int i = 0; i < types.length; i++) {
            int ty = layout.bodyY + i * (layout.typeH + 2);
            boolean on = i == typeIdx;
            boolean hover = DbaUi.hit(mouseX, mouseY, layout.x, ty, layout.leftW, layout.typeH);
            DbaUi.button(context, font, layout.x, ty, layout.leftW, layout.typeH, types[i].displayName(), hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        context.text(font, Component.literal("Color  " + COLOR_NAMES[colorIdx]), layout.rightX, layout.colorY, DbaUi.MUTED);
        int swatchY = layout.colorY + 12;
        for (int i = 0; i < COLORS.length; i++) {
            int cx = layout.rightX + i * (layout.swatch + 4);
            context.fill(cx, swatchY, cx + layout.swatch, swatchY + layout.swatch, COLORS[i]);
            if (i == colorIdx) {
                context.fill(cx - 1, swatchY - 1, cx + layout.swatch + 1, swatchY, DbaUi.TEXT);
                context.fill(cx - 1, swatchY + layout.swatch, cx + layout.swatch + 1, swatchY + layout.swatch + 1, DbaUi.TEXT);
                context.fill(cx - 1, swatchY, cx, swatchY + layout.swatch, DbaUi.TEXT);
                context.fill(cx + layout.swatch, swatchY, cx + layout.swatch + 1, swatchY + layout.swatch, DbaUi.TEXT);
            }
        }

        context.enableScissor(layout.rightX, layout.viewY, layout.rightX + layout.rightW, layout.viewY + layout.viewH);
        if (layout.showMode) {
            context.text(font, Component.literal("Discharge"), layout.rightX, layout.modeY - 12, DbaUi.MUTED);
            DbaUi.button(context, font, layout.rightX, layout.modeY, layout.modeW, 18, "Single", !barrage,
                    !barrage ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
            DbaUi.button(context, font, layout.rightX + layout.modeW + 4, layout.modeY, layout.modeW, 18, "Barrage", barrage,
                    barrage ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }
        if (layout.showKi) {
            DbaUi.slider(context, font, layout.rightX, layout.kiY, layout.rightW, "Ki spent", percentUsed + "%", percentUsed / 100.0f);
        } else {
            context.text(font, Component.literal("Uses all of your Ki"), layout.rightX, layout.noteY, DbaUi.DANGER);
        }
        float chargePct = chargeTicks / (float) KiTechnique.MAX_CHARGE_TICKS;
        DbaUi.slider(context, font, layout.rightX, layout.chargeY, layout.rightW, "Charge", chargeTicks + "t", chargePct);
        float sizePct = (float) ((size - SIZE_MIN) / (KiTechnique.MAX_SIZE - SIZE_MIN));
        DbaUi.slider(context, font, layout.rightX, layout.sizeY, layout.rightW, "Size", String.format(java.util.Locale.US, "%.1f", size), sizePct);
        context.disableScissor();

        String formName = types[typeIdx].displayName();
        if (types[typeIdx] == KiTechniqueType.BLAST && barrage) {
            formName = "Ki Barrage";
        }
        String saveLabel = canSave
                ? "Save " + formName + "  ·  " + cost + " AP"
                : "Need " + cost + " AP";
        if (canSave) {
            boolean hover = DbaUi.hit(mouseX, mouseY, layout.rightX, layout.saveY, layout.rightW, layout.saveH);
            DbaUi.button(context, font, layout.rightX, layout.saveY, layout.rightW, layout.saveH, saveLabel, hover, DbaUi.Kind.PRIMARY);
        } else {
            DbaUi.button(context, font, layout.rightX, layout.saveY, layout.rightW, layout.saveH, saveLabel, false, DbaUi.Kind.QUIET);
        }
    }

    private void applyKi(double mouseX, Layout layout) {
        float frac = (float) (mouseX - layout.rightX) / (float) layout.rightW;
        percentUsed = Math.max(5, Math.min(100, Math.round(frac * 100.0f)));
    }

    private void applyCharge(double mouseX, Layout layout) {
        float frac = (float) (mouseX - layout.rightX) / (float) layout.rightW;
        chargeTicks = Math.max(0, Math.min(KiTechnique.MAX_CHARGE_TICKS, Math.round(frac * KiTechnique.MAX_CHARGE_TICKS)));
    }

    private void applySize(double mouseX, Layout layout) {
        float frac = Math.max(0.0f, Math.min(1.0f, (float) (mouseX - layout.rightX) / (float) layout.rightW));
        double next = SIZE_MIN + (KiTechnique.MAX_SIZE - SIZE_MIN) * frac;
        size = Math.round(next * 10.0) / 10.0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0 || parent == null) {
            return false;
        }
        Layout layout = layout();
        double mx = event.x();
        double my = event.y();
        Minecraft client = Minecraft.getInstance();
        int cost = saveCost();
        boolean canSave = !(client.player instanceof PlayerStatsAccessor accessor) || accessor.dba$getStatPoints() >= cost;
        if (canSave && DbaUi.hit(mx, my, layout.rightX, layout.saveY, layout.rightW, layout.saveH)) {
            DbaUi.playClick();
            int percent = types[typeIdx] == KiTechniqueType.EXPLOSION ? 100 : percentUsed;
            ClientPlayNetworking.send(new C2SKiTechniqueSavePayload(
                    targetSlot,
                    typeId(),
                    percent,
                    COLORS[colorIdx],
                    barrage && types[typeIdx] == KiTechniqueType.BLAST,
                    chargeTicks,
                    size
            ));
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (DbaUi.hit(mx, my, layout.slotX[i], layout.slotY, layout.slotW, 20)) {
                DbaUi.playClick();
                loadSlot(i);
                return true;
            }
        }
        for (int i = 0; i < types.length; i++) {
            int ty = layout.bodyY + i * (layout.typeH + 2);
            if (DbaUi.hit(mx, my, layout.x, ty, layout.leftW, layout.typeH)) {
                DbaUi.playClick();
                typeIdx = i;
                if (types[typeIdx] != KiTechniqueType.BLAST) {
                    barrage = false;
                }
                return true;
            }
        }
        int swatchY = layout.colorY + 12;
        for (int i = 0; i < COLORS.length; i++) {
            int cx = layout.rightX + i * (layout.swatch + 4);
            if (DbaUi.hit(mx, my, cx, swatchY, layout.swatch, layout.swatch)) {
                DbaUi.playClick();
                colorIdx = i;
                return true;
            }
        }
        if (!inView(layout, my)) {
            return false;
        }
        if (layout.showMode) {
            if (DbaUi.hit(mx, my, layout.rightX, layout.modeY, layout.modeW, 18)) {
                DbaUi.playClick();
                barrage = false;
                return true;
            }
            if (DbaUi.hit(mx, my, layout.rightX + layout.modeW + 4, layout.modeY, layout.modeW, 18)) {
                DbaUi.playClick();
                barrage = true;
                return true;
            }
        }
        if (layout.showKi && DbaUi.sliderHit(layout.rightX, layout.kiY, layout.rightW, mx, my)) {
            dragging = Drag.KI;
            applyKi(mx, layout);
            return true;
        }
        if (DbaUi.sliderHit(layout.rightX, layout.chargeY, layout.rightW, mx, my)) {
            dragging = Drag.CHARGE;
            applyCharge(mx, layout);
            return true;
        }
        if (DbaUi.sliderHit(layout.rightX, layout.sizeY, layout.rightW, mx, my)) {
            dragging = Drag.SIZE;
            applySize(mx, layout);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == Drag.NONE || parent == null) {
            return false;
        }
        Layout layout = layout();
        if (dragging == Drag.KI && layout.showKi) {
            applyKi(event.x(), layout);
            return true;
        }
        if (dragging == Drag.CHARGE) {
            applyCharge(event.x(), layout);
            return true;
        }
        if (dragging == Drag.SIZE) {
            applySize(event.x(), layout);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = Drag.NONE;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (parent == null) {
            return false;
        }
        Layout layout = layout();
        if (!DbaUi.hit(mouseX, mouseY, layout.rightX, layout.viewY, layout.rightW, layout.viewH)) {
            return false;
        }
        scroll -= (int) Math.signum(verticalAmount) * 16;
        return true;
    }

    private static final class Layout {
        int x, y, w, h;
        int slotY, slotW;
        int[] slotX = new int[3];
        int bodyY, bodyH, leftW;
        int rightX, rightW;
        int saveY, saveH;
        int typeH;
        int colorY, swatch;
        int viewY, viewH;
        boolean showMode;
        int modeY, modeW;
        boolean showKi;
        int kiY, noteY, chargeY, sizeY;
        int contentBottom;
    }
}
