package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class RaceSelectionScreen extends Screen {
    private enum Page { RACE, COLORS }

    private static final String[] CHANNELS = {"Skin", "Hair", "Eyes"};
    private static final Preset[] SKIN = {
            new Preset("Light", 255, 204, 153),
            new Preset("Tan", 210, 180, 140),
            new Preset("Dark", 141, 85, 36),
            new Preset("Namek", 85, 255, 120),
            new Preset("Majin", 255, 140, 200),
            new Preset("Arcosian", 230, 230, 250)
    };
    private static final Preset[] HAIR = {
            new Preset("Black", 15, 15, 15),
            new Preset("Gold", 255, 215, 0),
            new Preset("Brown", 80, 45, 20),
            new Preset("Red", 200, 30, 30),
            new Preset("Blue", 30, 180, 255),
            new Preset("White", 240, 240, 240)
    };

    private Page page = Page.RACE;
    private String selectedRace = "";
    private int channel;
    private int skinR = 255, skinG = 204, skinB = 153;
    private int hairR = 15, hairG = 15, hairB = 15;
    private int eyeR = 255, eyeG = 255, eyeB = 255;
    private int scroll;
    private int draggingChannel = -1;
    private float previewRotation = 35.0F;

    private int frameX, frameY, frameW, frameH;
    private int previewW, midW;

    public RaceSelectionScreen() {
        super(Component.literal("Race"));
    }

    private List<Race> races() {
        List<Race> list = new ArrayList<>(DbaRegistries.getRaces().values());
        list.removeIf(race -> {
            String path = race.getId().getPath();
            return "neo_tuffle".equals(path) || "android".equals(path);
        });
        return list;
    }

    @Override
    protected void init() {
        place();
        if (selectedRace.isEmpty()) {
            List<Race> list = races();
            if (!list.isEmpty()) {
                Race initial = list.get(0);
                boolean keepColors = false;
                if (Minecraft.getInstance().player instanceof PlayerStatsAccessor accessor
                        && accessor.dba$getRaceId() != null) {
                    String saved = accessor.dba$getRaceId().toString();
                    for (Race race : list) {
                        if (race.getId().toString().equals(saved)) {
                            initial = race;
                            int[] skin = parseRgb(accessor.dba$getSkinColor(), skinR, skinG, skinB);
                            skinR = skin[0];
                            skinG = skin[1];
                            skinB = skin[2];
                            int[] hair = parseRgb(accessor.dba$getHairColor(), hairR, hairG, hairB);
                            hairR = hair[0];
                            hairG = hair[1];
                            hairB = hair[2];
                            int[] eye = parseRgb(accessor.dba$getEyeColor(), eyeR, eyeG, eyeB);
                            eyeR = eye[0];
                            eyeG = eye[1];
                            eyeB = eye[2];
                            keepColors = true;
                            break;
                        }
                    }
                }
                applyRace(initial, !keepColors);
            }
        }
        this.clearWidgets();
    }

    private void place() {
        this.frameW = Math.min(760, Math.max(480, this.width - 16));
        this.frameH = Math.min(440, Math.max(260, this.height - 16));
        if (this.frameW > this.width - 8) this.frameW = Math.max(320, this.width - 8);
        if (this.frameH > this.height - 8) this.frameH = Math.max(220, this.height - 8);
        this.frameX = (this.width - frameW) / 2;
        this.frameY = (this.height - frameH) / 2;
        this.previewW = Math.max(120, Math.min(220, frameW / 3));
        this.midW = Math.max(120, Math.min(190, (frameW - previewW) / 3));
    }

    private int detailX() { return frameX + previewW + midW; }
    private int detailW() { return frameW - previewW - midW; }

    private void applyRace(Race race, boolean recolor) {
        selectedRace = race.getId().toString();
        if (recolor) {
            String path = race.getId().getPath().toLowerCase();
            if (path.contains("yardrat")) {
                skinR = 250; skinG = 180; skinB = 175;
                hairR = 255; hairG = 240; hairB = 140;
            } else if (path.contains("namek")) {
                skinR = 90; skinG = 210; skinB = 110;
                hairR = 230; hairG = 140; hairB = 160;
            } else if (path.contains("majin")) {
                skinR = 255; skinG = 145; skinB = 195;
                hairR = 255; hairG = 145; hairB = 195;
            } else if (path.contains("arcosian")) {
                skinR = 240; skinG = 240; skinB = 252;
                hairR = 130; hairG = 40; hairB = 190;
            } else if (path.contains("bio_android")) {
                skinR = 60; skinG = 180; skinB = 85;
                hairR = 240; hairG = 190; hairB = 30;
            } else {
                skinR = 255; skinG = 204; skinB = 153;
                hairR = 25; hairG = 25; hairB = 25;
            }
            eyeR = 255; eyeG = 255; eyeB = 255;
        }
        applyLiveRecolor();
        com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(race.getId().getPath().toLowerCase());
    }

    private void applyLiveRecolor() {
        int skin = ((skinR & 0xFF) << 16) | ((skinG & 0xFF) << 8) | (skinB & 0xFF);
        int hair = ((hairR & 0xFF) << 16) | ((hairG & 0xFF) << 8) | (hairB & 0xFF);
        int eye = ((eyeR & 0xFF) << 16) | ((eyeG & 0xFF) << 8) | (eyeB & 0xFF);
        Identifier id = Identifier.tryParse(selectedRace);
        String path = id != null ? id.getPath() : "universal_humanoid";
        com.dragonblockarcanedba.client.render.DynamicSkinManager.getOrGenerateSkin(path, skin, hair, eye);
        if (Minecraft.getInstance().player instanceof PlayerStatsAccessor accessor) {
            accessor.dba$setSkinColor(hex(skinR, skinG, skinB));
            accessor.dba$setHairColor(hex(hairR, hairG, hairB));
            accessor.dba$setEyeColor(hex(eyeR, eyeG, eyeB));
        }
    }

    private static String hex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /** Race-select hex, with or without a leading #. Falls back when the save has no color yet. */
    private static int[] parseRgb(String hex, int r, int g, int b) {
        if (hex == null || hex.isEmpty()) {
            return new int[] {r, g, b};
        }
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int rgb = Integer.parseInt(value, 16);
            return new int[] {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        } catch (RuntimeException ignored) {
            return new int[] {r, g, b};
        }
    }

    private int[] channelColor() {
        return switch (channel) {
            case 1 -> new int[] {hairR, hairG, hairB};
            case 2 -> new int[] {eyeR, eyeG, eyeB};
            default -> new int[] {skinR, skinG, skinB};
        };
    }

    private void setChannel(int index, int value) {
        int clamped = Math.max(0, Math.min(255, value));
        switch (channel) {
            case 1 -> {
                if (index == 0) hairR = clamped;
                else if (index == 1) hairG = clamped;
                else hairB = clamped;
            }
            case 2 -> {
                if (index == 0) eyeR = clamped;
                else if (index == 1) eyeG = clamped;
                else eyeB = clamped;
            }
            default -> {
                if (index == 0) skinR = clamped;
                else if (index == 1) skinG = clamped;
                else skinB = clamped;
            }
        }
        applyLiveRecolor();
    }

    private int listTop() { return frameY + DbaUi.HEADER + 8; }
    private int listBottom() { return frameY + frameH - 8; }
    private int rowH() { return 22; }

    private void confirm() {
        if (selectedRace.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putString("action", "select_race");
        data.putString("race", selectedRace);
        data.putString("skin_color", hex(skinR, skinG, skinB));
        data.putString("hair_color", hex(hairR, hairG, hairB));
        data.putString("eye_color", hex(eyeR, eyeG, eyeB));
        ClientPlayNetworking.send(new ActionPayload(data));
        applyLiveRecolor();
        Identifier id = Identifier.tryParse(selectedRace);
        String path = id != null ? id.getPath() : "universal_humanoid";
        com.dragonblockarcanedba.client.compat.BpmCompatClient.enforceModel(path);
        if (Minecraft.getInstance().player instanceof PlayerStatsAccessor accessor) {
            accessor.dba$setHasSelectedRace(true);
            accessor.dba$setTailSevered(false);
        }
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        place();
        DbaUi.dim(context, this.width, this.height);
        DbaUi.frame(context, frameX, frameY, frameW, frameH);
        String heading = page == Page.RACE ? "Race" : "Colors";
        DbaUi.title(context, this.font, frameX, frameY, DbaUi.closeX(frameX, frameW) - frameX - 8, heading);
        DbaUi.closeControl(context, this.font, frameX, frameY, frameW, mouseX, mouseY);

        int previewX = frameX + previewW / 2;
        int previewTop = frameY + DbaUi.HEADER + 8;
        int previewBottom = frameY + frameH - 28;
        DbaUi.panel(context, frameX + 8, previewTop, previewW - 16, previewBottom - previewTop);
        drawPreview(context, previewX, (previewTop + previewBottom) / 2, previewTop, previewBottom);
        context.centeredText(this.font, Component.literal("Drag to turn"), previewX, previewBottom + 6, DbaUi.MUTED);

        if (page == Page.RACE) {
            drawRaceList(context, mouseX, mouseY);
        } else {
            drawChannels(context, mouseX, mouseY);
            drawColors(context, mouseX, mouseY);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawPreview(GuiGraphicsExtractor context, int previewX, int previewY, int top, int bottom) {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }
        float savedYRot = localPlayer.getYRot();
        float savedYBodyRot = localPlayer.yBodyRot;
        float savedYHeadRot = localPlayer.yHeadRot;
        float savedXRot = localPlayer.getXRot();
        if (localPlayer instanceof PlayerStatsAccessor accessor && !selectedRace.isEmpty()) {
            Identifier id = Identifier.tryParse(selectedRace);
            if (id != null) {
                accessor.dba$setRaceId(id);
            }
            accessor.dba$setSkinColor(hex(skinR, skinG, skinB));
            accessor.dba$setHairColor(hex(hairR, hairG, hairB));
            accessor.dba$setEyeColor(hex(eyeR, eyeG, eyeB));
            accessor.dba$setTailSevered(false);
        }
        int scale = Math.min(42, Math.max(24, (bottom - top) / 5));
        float rad = (float) Math.toRadians(previewRotation);
        float simMouseX = previewX - (float) Math.sin(rad) * 150.0F;
        net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                context, previewX - 40, top + 8, previewX + 40, bottom - 8, scale, 0.0625F, simMouseX, previewY, localPlayer);
        localPlayer.setYRot(savedYRot);
        localPlayer.yBodyRot = savedYBodyRot;
        localPlayer.yHeadRot = savedYHeadRot;
        localPlayer.setXRot(savedXRot);
    }

    private void drawRaceList(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<Race> list = races();
        int lx = frameX + previewW + 6;
        int lw = midW - 12;
        if (list.isEmpty()) {
            DbaUi.empty(context, this.font, lx, listTop(), lw + detailW(), frameH - DbaUi.HEADER - 16, "No races are available.");
            return;
        }
        int view = listBottom() - listTop();
        int stride = rowH() + 3;
        int max = Math.max(0, list.size() * stride - view);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
        context.enableScissor(lx, listTop(), lx + lw, listBottom());
        for (int i = 0; i < list.size(); i++) {
            Race race = list.get(i);
            int ry = listTop() + i * stride - scroll;
            if (ry + rowH() < listTop() || ry > listBottom()) {
                continue;
            }
            boolean on = race.getId().toString().equals(selectedRace);
            boolean hover = DbaUi.hit(mouseX, mouseY, lx, ry, lw, rowH());
            DbaUi.row(context, lx, ry, lw, rowH(), hover, on);
            context.text(this.font, Component.literal(DbaUi.clip(this.font, race.getDisplayName(), lw - 12)), lx + 8, ry + 7, on ? DbaUi.GOLD : DbaUi.TEXT);
        }
        context.disableScissor();

        int dx = detailX() + 8;
        int dw = detailW() - 16;
        context.text(this.font, Component.literal("Selected"), dx, frameY + DbaUi.HEADER + 10, DbaUi.MUTED);
        String name = "None";
        for (Race race : list) {
            if (race.getId().toString().equals(selectedRace)) {
                name = race.getDisplayName();
                break;
            }
        }
        context.text(this.font, Component.literal(DbaUi.clip(this.font, name, dw)), dx, frameY + DbaUi.HEADER + 24, DbaUi.TEXT);
        context.text(this.font, Component.literal(DbaUi.clip(this.font, "Next you set skin, hair, and eyes.", dw)),
                dx, frameY + DbaUi.HEADER + 40, DbaUi.MUTED);
        int by = frameY + frameH - DbaUi.BUTTON_H - 12;
        boolean hover = DbaUi.hit(mouseX, mouseY, dx, by, dw, DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, dx, by, dw, DbaUi.BUTTON_H, "Continue", hover, DbaUi.Kind.PRIMARY);
    }

    private void drawChannels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int lx = frameX + previewW + 6;
        int lw = midW - 12;
        for (int i = 0; i < CHANNELS.length; i++) {
            int ry = listTop() + i * 28;
            boolean on = i == channel;
            boolean hover = DbaUi.hit(mouseX, mouseY, lx, ry, lw, 22);
            DbaUi.button(context, this.font, lx, ry, lw, 22, CHANNELS[i], hover || on, on ? DbaUi.Kind.PRIMARY : DbaUi.Kind.SECONDARY);
        }
    }

    private void drawColors(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int dx = detailX() + 8;
        int dw = detailW() - 16;
        int[] rgb = channelColor();
        String[] names = {"Red", "Green", "Blue"};
        for (int i = 0; i < 3; i++) {
            int sy = frameY + DbaUi.HEADER + 12 + i * 32;
            DbaUi.slider(context, this.font, dx, sy, dw, names[i], Integer.toString(rgb[i]), rgb[i] / 255.0f);
        }
        Preset[] presets = channel == 1 ? HAIR : (channel == 0 ? SKIN : null);
        int py = frameY + DbaUi.HEADER + 112;
        if (presets == null) {
            context.text(this.font, Component.literal("Eyes use the sliders."), dx, py, DbaUi.MUTED);
        } else {
            int cols = 3;
            int gap = 4;
            int bw = Math.max(28, (dw - gap * (cols - 1)) / cols);
            for (int i = 0; i < presets.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int bx = dx + col * (bw + gap);
                int by = py + row * 22;
                if (by + 18 > frameY + frameH - 36) {
                    break;
                }
                boolean hover = DbaUi.hit(mouseX, mouseY, bx, by, bw, 18);
                DbaUi.button(context, this.font, bx, by, bw, 18, presets[i].name, hover, DbaUi.Kind.SECONDARY);
            }
        }
        int by = frameY + frameH - DbaUi.BUTTON_H - 12;
        int half = (dw - DbaUi.GAP) / 2;
        boolean back = DbaUi.hit(mouseX, mouseY, dx, by, half, DbaUi.BUTTON_H);
        boolean ok = DbaUi.hit(mouseX, mouseY, dx + half + DbaUi.GAP, by, half, DbaUi.BUTTON_H);
        DbaUi.button(context, this.font, dx, by, half, DbaUi.BUTTON_H, "Back", back, DbaUi.Kind.SECONDARY);
        DbaUi.button(context, this.font, dx + half + DbaUi.GAP, by, half, DbaUi.BUTTON_H, "Confirm", ok, DbaUi.Kind.PRIMARY);
    }

    private int sliderRowY(int index) {
        return frameY + DbaUi.HEADER + 12 + index * 32;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0) {
            return super.mouseClicked(event, isRepeat);
        }
        place();
        double mx = event.x();
        double my = event.y();
        if (DbaUi.closeHit(frameX, frameY, frameW, mx, my)) {
            DbaUi.playClick();
            this.onClose();
            return true;
        }
        if (page == Page.RACE) {
            return clickRace(mx, my);
        }
        int lx = frameX + previewW + 6;
        int lw = midW - 12;
        for (int i = 0; i < CHANNELS.length; i++) {
            int ry = listTop() + i * 28;
            if (DbaUi.hit(mx, my, lx, ry, lw, 22)) {
                DbaUi.playClick();
                channel = i;
                return true;
            }
        }
        int dx = detailX() + 8;
        int dw = detailW() - 16;
        for (int i = 0; i < 3; i++) {
            if (DbaUi.sliderHit(dx, sliderRowY(i), dw, mx, my)) {
                draggingChannel = i;
                setChannel(i, sliderValue(mx, dx, dw));
                return true;
            }
        }
        Preset[] presets = channel == 1 ? HAIR : (channel == 0 ? SKIN : null);
        if (presets != null) {
            int py = frameY + DbaUi.HEADER + 112;
            int cols = 3;
            int gap = 4;
            int bw = Math.max(28, (dw - gap * (cols - 1)) / cols);
            for (int i = 0; i < presets.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int bx = dx + col * (bw + gap);
                int by = py + row * 22;
                if (by + 18 > frameY + frameH - 36) {
                    break;
                }
                if (DbaUi.hit(mx, my, bx, by, bw, 18)) {
                    DbaUi.playClick();
                    setChannel(0, presets[i].r);
                    setChannel(1, presets[i].g);
                    setChannel(2, presets[i].b);
                    return true;
                }
            }
        }
        int by = frameY + frameH - DbaUi.BUTTON_H - 12;
        int half = (dw - DbaUi.GAP) / 2;
        if (DbaUi.hit(mx, my, dx, by, half, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            page = Page.RACE;
            return true;
        }
        if (DbaUi.hit(mx, my, dx + half + DbaUi.GAP, by, half, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            confirm();
            return true;
        }
        return super.mouseClicked(event, isRepeat);
    }

    private boolean clickRace(double mx, double my) {
        List<Race> list = races();
        if (list.isEmpty()) {
            return false;
        }
        int lx = frameX + previewW + 6;
        int lw = midW - 12;
        int stride = rowH() + 3;
        int view = listBottom() - listTop();
        int max = Math.max(0, list.size() * stride - view);
        if (scroll > max) scroll = max;
        if (my >= listTop() && my < listBottom()) {
            for (int i = 0; i < list.size(); i++) {
                int ry = listTop() + i * stride - scroll;
                if (ry + rowH() < listTop() || ry > listBottom()) {
                    continue;
                }
                if (DbaUi.hit(mx, my, lx, ry, lw, rowH())) {
                    DbaUi.playClick();
                    applyRace(list.get(i), true);
                    return true;
                }
            }
        }
        int dx = detailX() + 8;
        int dw = detailW() - 16;
        int by = frameY + frameH - DbaUi.BUTTON_H - 12;
        if (DbaUi.hit(mx, my, dx, by, dw, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            page = Page.COLORS;
            channel = 0;
            return true;
        }
        return false;
    }

    private int sliderValue(double mouseX, int x, int width) {
        float frac = (float) (mouseX - x) / (float) Math.max(1, width);
        return Math.max(0, Math.min(255, Math.round(frac * 255.0f)));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        place();
        if (draggingChannel >= 0 && page == Page.COLORS) {
            int dx = detailX() + 8;
            int dw = detailW() - 16;
            setChannel(draggingChannel, sliderValue(event.x(), dx, dw));
            return true;
        }
        if (event.x() >= frameX && event.x() < frameX + previewW) {
            previewRotation += (float) dragX * 1.8F;
            if (previewRotation > 360.0F) previewRotation -= 360.0F;
            if (previewRotation < 0.0F) previewRotation += 360.0F;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingChannel = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page != Page.RACE) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int lx = frameX + previewW;
        if (mouseX >= lx && mouseX < lx + midW && mouseY >= listTop() && mouseY < listBottom()) {
            scroll -= (int) Math.signum(verticalAmount) * (rowH() + 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DbaUi.escape(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private record Preset(String name, int r, int g, int b) {}
}
