package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Forms are a list on the left and one detail card on the right.
 * Transform and Revert still send the same actions for the selected form.
 */
public class FormsTab implements MenuTab {
    private static final int ROW_H = 22;

    private DbaMenuScreen parent;
    private int scroll;
    private int selected;
    private boolean chosen;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.scroll = 0;
        this.selected = 0;
        this.chosen = false;
    }

    private List<Form> forms(PlayerStatsAccessor accessor) {
        Identifier race = accessor.dba$getRaceId();
        if (race == null) {
            return List.of();
        }
        List<Form> list = new ArrayList<>();
        for (Form form : DbaRegistries.getForms().values()) {
            if (form.getCompatibleRaces().contains(race)) {
                list.add(form);
            }
        }
        return list;
    }

    private boolean unlocked(PlayerStatsAccessor accessor, Form form) {
        Form.UnlockRequirements req = form.getUnlockRequirements();
        if (accessor.dba$getLevel() < req.minLevel()) {
            return false;
        }
        if (req.minStats() != null) {
            if (accessor.dba$getStrength() < req.minStats().strength()) return false;
            if (accessor.dba$getDexterity() < req.minStats().agility()) return false;
            if (accessor.dba$getDefense() < req.minStats().defense()) return false;
            if (accessor.dba$getWillpower() < req.minStats().kiControl()) return false;
            if (accessor.dba$getSpirit() < req.minStats().kiCapacity()) return false;
        }
        return true;
    }

    private int listW(int w) {
        return Math.min(220, Math.max(120, w * 42 / 100));
    }

    private void clamp(int count, int viewH) {
        int content = count * (ROW_H + DbaUi.GAP);
        int max = Math.max(0, content - viewH);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
    }

    private void ensureSelected(List<Form> forms, Identifier active) {
        if (forms.isEmpty()) {
            selected = 0;
            return;
        }
        if (!chosen) {
            selected = 0;
            if (active != null) {
                for (int i = 0; i < forms.size(); i++) {
                    if (active.equals(forms.get(i).getId())) {
                        selected = i;
                        break;
                    }
                }
            }
            chosen = true;
        }
        if (selected < 0 || selected >= forms.size()) {
            selected = 0;
        }
    }

    private static String formName(Form form) {
        String name = form.getId().getPath().replace('_', ' ');
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor) || parent == null) {
            return;
        }
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        List<Form> forms = forms(accessor);
        if (forms.isEmpty()) {
            String message = accessor.dba$getRaceId() == null
                    ? "Choose a race before forms are available."
                    : "This race has no forms.";
            DbaUi.empty(context, client.font, x, y, w, h, message);
            return;
        }
        Identifier active = accessor.dba$getActiveFormId();
        ensureSelected(forms, active);
        int lw = listW(w);
        clamp(forms.size(), h);
        context.enableScissor(x, y, x + lw, y + h);
        for (int i = 0; i < forms.size(); i++) {
            int rowY = y + i * (ROW_H + DbaUi.GAP) - scroll;
            if (rowY + ROW_H < y || rowY > y + h) {
                continue;
            }
            Form form = forms.get(i);
            boolean on = i == selected;
            boolean hover = DbaUi.hit(mouseX, mouseY, x, rowY, lw, ROW_H);
            DbaUi.row(context, x, rowY, lw, ROW_H, hover, on);
            int color = unlocked(accessor, form) ? (on ? DbaUi.TEXT : DbaUi.MUTED) : DbaUi.MUTED;
            context.text(client.font, Component.literal(DbaUi.clip(client.font, formName(form), lw - 12)), x + 6, rowY + 7, color);
        }
        context.disableScissor();

        Form form = forms.get(selected);
        int dx = x + lw + 8;
        int dw = w - lw - 8;
        boolean open = unlocked(accessor, form);
        boolean transformed = active != null && active.equals(form.getId());
        context.text(client.font, Component.literal(DbaUi.clip(client.font, formName(form), dw)), dx, y + 4, DbaUi.TEXT);
        String detail;
        if (!open) {
            detail = "Requires level " + form.getUnlockRequirements().minLevel();
        } else {
            double mastery = accessor.dba$getFormMastery(form.getId());
            double drain = form.getBaseKiDrain() * (1.0 - (mastery / 100.0 * form.getMaxMasteryReduction()));
            detail = String.format(Locale.US, "Mastery %.0f%%", mastery);
            context.text(client.font, Component.literal(String.format(Locale.US, "Ki %.1f/s", drain)), dx, y + 28, DbaUi.MUTED);
        }
        context.text(client.font, Component.literal(DbaUi.clip(client.font, detail, dw)), dx, y + 16, DbaUi.MUTED);

        int btnW = Math.min(140, dw);
        int btnY = y + h - DbaUi.BUTTON_H;
        if (open) {
            boolean hover = DbaUi.hit(mouseX, mouseY, dx, btnY, btnW, DbaUi.BUTTON_H);
            DbaUi.button(context, client.font, dx, btnY, btnW, DbaUi.BUTTON_H,
                    transformed ? "Revert" : "Transform", hover, transformed ? DbaUi.Kind.DANGER : DbaUi.Kind.PRIMARY);
        } else {
            context.text(client.font, Component.literal("Locked"), dx, btnY + 4, DbaUi.MUTED);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        if (event.button() != 0 || parent == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (!(client.player instanceof PlayerStatsAccessor accessor)) {
            return false;
        }
        List<Form> forms = forms(accessor);
        if (forms.isEmpty()) {
            return false;
        }
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        if (!DbaUi.hit(event.x(), event.y(), x, y, w, h)) {
            return false;
        }
        Identifier active = accessor.dba$getActiveFormId();
        ensureSelected(forms, active);
        int lw = listW(w);
        clamp(forms.size(), h);
        if (event.x() < x + lw) {
            for (int i = 0; i < forms.size(); i++) {
                int rowY = y + i * (ROW_H + DbaUi.GAP) - scroll;
                if (rowY + ROW_H < y || rowY > y + h) {
                    continue;
                }
                if (DbaUi.hit(event.x(), event.y(), x, rowY, lw, ROW_H)) {
                    DbaUi.playClick();
                    selected = i;
                    return true;
                }
            }
            return true;
        }
        Form form = forms.get(selected);
        if (!unlocked(accessor, form)) {
            return true;
        }
        int dx = x + lw + 8;
        int dw = w - lw - 8;
        int btnW = Math.min(140, dw);
        int btnY = y + h - DbaUi.BUTTON_H;
        if (!DbaUi.hit(event.x(), event.y(), dx, btnY, btnW, DbaUi.BUTTON_H)) {
            return true;
        }
        DbaUi.playClick();
        boolean on = active != null && active.equals(form.getId());
        CompoundTag nbt = new CompoundTag();
        if (on) {
            nbt.putString("action", "untransform");
        } else {
            nbt.putString("action", "transform");
            nbt.putString("form", form.getId().toString());
        }
        ClientPlayNetworking.send(new ActionPayload(nbt));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (parent == null) {
            return false;
        }
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        if (!DbaUi.hit(mouseX, mouseY, x, y, listW(w), h)) {
            return false;
        }
        scroll -= (int) Math.signum(verticalAmount) * (ROW_H + DbaUi.GAP);
        return true;
    }
}
