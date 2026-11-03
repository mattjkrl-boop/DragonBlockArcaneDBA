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
 * Ki craft. One layout object feeds both the drawing and the clicks.
 */
public class KiCustomizerTab implements MenuTab {
    private static final int[] COLORS = {
            0xFF2F6BFF, 0xFFD64545, 0xFF3CB371, 0xFFE2B657, 0xFF9B59D0, 0xFFF4F0E6
    };
    private static final String[] COLOR_NAMES = {"Azure", "Crimson", "Emerald", "Gold", "Violet", "White"};

    private final KiTechniqueType[] types = KiTechniqueType.values();

    private DbaMenuScreen parent;
    private int targetSlot;
    private int typeIdx;
    private int percentUsed = 50;
    private int colorIdx;
    private boolean barrage;
    private int scroll;
    private boolean dragging;
    private boolean loaded;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.dragging = false;
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
    }

    private int saveCost() {
        KiTechniqueType type = types[typeIdx];
        int percent = type == KiTechniqueType.EXPLOSION ? 100 : percentUsed;
        return PlayerStats.getKiAttackSaveCost(type, percent, barrage && type == KiTechniqueType.BLAST);
    }

    private Layout layout() {
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        int rightW = Math.min(170, Math.max(108, w / 3));
        int leftW = w - rightW - 8;
        int saveH = DbaUi.BUTTON_H;
        int saveY = y + h - saveH;
        Layout layout = new Layout();
        layout.leftX = x;
        layout.leftY = y;
        layout.leftW = leftW;
        layout.leftH = h;
        layout.rightX = x + leftW + 8;
        layout.rightY = y;
        layout.rightW = rightW;
        layout.rightH = h;
        layout.saveX = x;
        layout.saveY = saveY;
        layout.saveW = leftW;
        layout.saveH = saveH;
        layout.viewTop = y;
        layout.viewBottom = saveY - 6;

        int cy = y - scroll;
        cy += 2;
        int slotW = Math.max(36, (leftW - 8) / 3);
        layout.slotsY = cy + 12;
        for (int i = 0; i < 3; i++) {
            layout.slotX[i] = x + i * (slotW + 4);
            layout.slotW = slotW;
        }
        cy = layout.slotsY + 22 + 8;

        cy += 12;
        int cols = leftW < 220 ? 2 : 3;
        int typeW = Math.max(40, (leftW - (cols - 1) * 4) / cols);
        int rows = (types.length + cols - 1) / cols;
        layout.typeCols = cols;
        layout.typeW = typeW;
        layout.typeY = cy;
        cy += rows * 22;

        cy += 8;
        cy += 12;
        int swatch = Math.max(8, Math.min(22, (leftW - (COLORS.length - 1) * 4) / Math.max(1, COLORS.length)));
        layout.colorY = cy;
        layout.colorW = swatch;
        cy += swatch + 8;

        layout.showMode = types[typeIdx] == KiTechniqueType.BLAST;
        if (layout.showMode) {
            cy += 12;
            int modeW = Math.max(40, (leftW - 4) / 2);
            layout.modeY = cy;
            layout.modeW = modeW;
            cy += 22;
        }

        layout.showSlider = types[typeIdx] != KiTechniqueType.EXPLOSION;
        if (layout.showSlider) {
            layout.sliderX = x;
            layout.sliderY = cy;
            layout.sliderW = leftW;
            cy += 32;
        } else {
            layout.noteY = cy;
            cy += 16;
        }
        layout.contentBottom = cy;
        int viewH = Math.max(20, layout.viewBottom - layout.viewTop);
        int overflow = Math.max(0, (layout.contentBottom + scroll) - layout.viewBottom);
        // contentBottom is in scrolled coordinates (includes -scroll). Natural height:
        int natural = layout.contentBottom + scroll - y;
        int maxScroll = Math.max(0, natural - viewH);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
        return layout;
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

        context.enableScissor(layout.leftX, layout.viewTop, layout.leftX + layout.leftW, layout.viewBottom);
        int labelY = layout.slotsY - 12;
        context.text(font, Component.literal("Slot"), layout.leftX, labelY, DbaUi.MUTED);
        for (int i = 0; i < 3; i++) {
            boolean on = i == targetSlot;
            boolean hover = inView(layout, mouseY) && DbaUi.hit(mouseX, mouseY, layout.slotX[i], layout.slotsY, layout.slotW, 20);
            String label = (i + 1) + "  " + DbaUi.slotKey(i);
            DbaUi.button(context, font, layout.slotX[i], layout.slotsY, layout.slotW, 20, label, hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        context.text(font, Component.literal("Form"), layout.leftX, layout.typeY - 12, DbaUi.MUTED);
        for (int i = 0; i < types.length; i++) {
            int col = i % layout.typeCols;
            int row = i / layout.typeCols;
            int tx = layout.leftX + col * (layout.typeW + 4);
            int ty = layout.typeY + row * 22;
            boolean on = i == typeIdx;
            boolean hover = DbaUi.hit(mouseX, mouseY, tx, ty, layout.typeW, 18);
            DbaUi.button(context, font, tx, ty, layout.typeW, 18, types[i].displayName(), hover || on,
                    on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        context.text(font, Component.literal("Color  " + COLOR_NAMES[colorIdx]), layout.leftX, layout.colorY - 12, DbaUi.MUTED);
        for (int i = 0; i < COLORS.length; i++) {
            int cx = layout.leftX + i * (layout.colorW + 4);
            context.fill(cx, layout.colorY, cx + layout.colorW, layout.colorY + layout.colorW, COLORS[i]);
            if (i == colorIdx) {
                context.fill(cx - 1, layout.colorY - 1, cx + layout.colorW + 1, layout.colorY, DbaUi.TEXT);
                context.fill(cx - 1, layout.colorY + layout.colorW, cx + layout.colorW + 1, layout.colorY + layout.colorW + 1, DbaUi.TEXT);
                context.fill(cx - 1, layout.colorY, cx, layout.colorY + layout.colorW, DbaUi.TEXT);
                context.fill(cx + layout.colorW, layout.colorY, cx + layout.colorW + 1, layout.colorY + layout.colorW, DbaUi.TEXT);
            }
        }

        if (layout.showMode) {
            context.text(font, Component.literal("Discharge"), layout.leftX, layout.modeY - 12, DbaUi.MUTED);
            DbaUi.button(context, font, layout.leftX, layout.modeY, layout.modeW, 18, "Single", !barrage, !barrage ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
            DbaUi.button(context, font, layout.leftX + layout.modeW + 4, layout.modeY, layout.modeW, 18, "Barrage", barrage, barrage ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }

        if (layout.showSlider) {
            DbaUi.slider(context, font, layout.sliderX, layout.sliderY, layout.sliderW, "Ki spent", percentUsed + "%", percentUsed / 100.0f);
        } else {
            context.text(font, Component.literal("Uses all of your Ki"), layout.leftX, layout.noteY, DbaUi.DANGER);
        }
        context.disableScissor();

        String saveLabel = canSave ? "Save to slot " + (targetSlot + 1) + "  ·  " + cost + " AP" : "Need " + cost + " AP";
        if (canSave) {
            boolean hover = DbaUi.hit(mouseX, mouseY, layout.saveX, layout.saveY, layout.saveW, layout.saveH);
            DbaUi.button(context, font, layout.saveX, layout.saveY, layout.saveW, layout.saveH, saveLabel, hover, DbaUi.Kind.PRIMARY);
        } else {
            DbaUi.button(context, font, layout.saveX, layout.saveY, layout.saveW, layout.saveH, saveLabel, false, DbaUi.Kind.QUIET);
        }

        DbaUi.panel(context, layout.rightX, layout.rightY, layout.rightW, layout.rightH);
        context.text(font, Component.literal("Preview"), layout.rightX + 8, layout.rightY + 8, DbaUi.MUTED);
        int mark = Math.min(36, layout.rightW - 24);
        int mx = layout.rightX + (layout.rightW - mark) / 2;
        int my = layout.rightY + 28;
        context.fill(mx, my, mx + mark, my + mark, COLORS[colorIdx]);
        context.fill(mx + mark / 4, my + mark / 4, mx + mark * 3 / 4, my + mark * 3 / 4, DbaUi.TEXT);

        String formName = types[typeIdx].displayName();
        if (types[typeIdx] == KiTechniqueType.BLAST && barrage) {
            formName = "Ki Barrage";
        }
        int textY = my + mark + 10;
        int textW = layout.rightW - 16;
        String[] lines = {
                formName,
                COLOR_NAMES[colorIdx],
                (types[typeIdx] == KiTechniqueType.EXPLOSION ? 100 : percentUsed) + "% Ki",
                "Slot " + (targetSlot + 1) + "  " + DbaUi.slotKey(targetSlot),
                "Saving replaces the skill in this slot."
        };
        for (String line : lines) {
            if (textY > layout.rightY + layout.rightH - 12) {
                break;
            }
            context.text(font, Component.literal(DbaUi.clip(font, line, textW)), layout.rightX + 8, textY, DbaUi.TEXT);
            textY += 12;
        }
    }

    private boolean inView(Layout layout, double my) {
        return my >= layout.viewTop && my < layout.viewBottom;
    }

    private void applySlider(double mouseX, Layout layout) {
        float frac = (float) (mouseX - layout.sliderX) / (float) layout.sliderW;
        percentUsed = Math.max(5, Math.min(100, Math.round(frac * 100.0f)));
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
        if (canSave && DbaUi.hit(mx, my, layout.saveX, layout.saveY, layout.saveW, layout.saveH)) {
            DbaUi.playClick();
            int percent = types[typeIdx] == KiTechniqueType.EXPLOSION ? 100 : percentUsed;
            ClientPlayNetworking.send(new C2SKiTechniqueSavePayload(
                    targetSlot,
                    types[typeIdx].name(),
                    percent,
                    COLORS[colorIdx],
                    barrage && types[typeIdx] == KiTechniqueType.BLAST
            ));
            return true;
        }
        if (!inView(layout, my)) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (DbaUi.hit(mx, my, layout.slotX[i], layout.slotsY, layout.slotW, 20)) {
                DbaUi.playClick();
                loadSlot(i);
                return true;
            }
        }
        for (int i = 0; i < types.length; i++) {
            int col = i % layout.typeCols;
            int row = i / layout.typeCols;
            int tx = layout.leftX + col * (layout.typeW + 4);
            int ty = layout.typeY + row * 22;
            if (DbaUi.hit(mx, my, tx, ty, layout.typeW, 18)) {
                DbaUi.playClick();
                typeIdx = i;
                if (types[typeIdx] != KiTechniqueType.BLAST) {
                    barrage = false;
                }
                return true;
            }
        }
        for (int i = 0; i < COLORS.length; i++) {
            int cx = layout.leftX + i * (layout.colorW + 4);
            if (DbaUi.hit(mx, my, cx, layout.colorY, layout.colorW, layout.colorW)) {
                DbaUi.playClick();
                colorIdx = i;
                return true;
            }
        }
        if (layout.showMode) {
            if (DbaUi.hit(mx, my, layout.leftX, layout.modeY, layout.modeW, 18)) {
                DbaUi.playClick();
                barrage = false;
                return true;
            }
            if (DbaUi.hit(mx, my, layout.leftX + layout.modeW + 4, layout.modeY, layout.modeW, 18)) {
                DbaUi.playClick();
                barrage = true;
                return true;
            }
        }
        if (layout.showSlider && DbaUi.sliderHit(layout.sliderX, layout.sliderY, layout.sliderW, mx, my)) {
            dragging = true;
            applySlider(mx, layout);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging || parent == null) {
            return false;
        }
        Layout layout = layout();
        if (layout.showSlider) {
            applySlider(event.x(), layout);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (parent == null) {
            return false;
        }
        Layout layout = layout();
        if (!DbaUi.hit(mouseX, mouseY, layout.leftX, layout.viewTop, layout.leftW, layout.viewBottom - layout.viewTop)) {
            return false;
        }
        scroll -= (int) Math.signum(verticalAmount) * 16;
        return true;
    }

    private static final class Layout {
        int leftX, leftY, leftW, leftH;
        int rightX, rightY, rightW, rightH;
        int saveX, saveY, saveW, saveH;
        int viewTop, viewBottom;
        int slotsY;
        int[] slotX = new int[3];
        int slotW;
        int typeCols, typeW, typeY;
        int colorY, colorW;
        boolean showMode;
        int modeY, modeW;
        boolean showSlider;
        int sliderX, sliderY, sliderW;
        int noteY;
        int contentBottom;
    }
}
