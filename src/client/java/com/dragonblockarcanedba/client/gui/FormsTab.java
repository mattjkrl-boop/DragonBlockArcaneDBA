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

public class FormsTab implements MenuTab {
    private static final int ROW_H = 46;

    private DbaMenuScreen parent;
    private int scroll;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.scroll = 0;
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

    private void clamp(int count, int viewH) {
        int content = count * (ROW_H + DbaUi.GAP);
        int max = Math.max(0, content - viewH);
        if (scroll < 0) scroll = 0;
        if (scroll > max) scroll = max;
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
        clamp(forms.size(), h);
        context.enableScissor(x, y, x + w, y + h);
        Identifier active = accessor.dba$getActiveFormId();
        int btnW = Math.min(88, Math.max(64, w / 5));
        for (int i = 0; i < forms.size(); i++) {
            Form form = forms.get(i);
            int rowY = y + i * (ROW_H + DbaUi.GAP) - scroll;
            if (rowY + ROW_H < y || rowY > y + h) {
                continue;
            }
            boolean open = unlocked(accessor, form);
            boolean on = active != null && active.equals(form.getId());
            boolean rowHover = DbaUi.hit(mouseX, mouseY, x, rowY, w, ROW_H);
            DbaUi.row(context, x, rowY, w, ROW_H, rowHover, on);

            String name = form.getId().getPath().replace('_', ' ');
            name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
            int textW = w - btnW - 28;
            context.text(client.font, Component.literal(DbaUi.clip(client.font, name, textW)), x + 10, rowY + 6, on ? DbaUi.GOLD : DbaUi.TEXT);

            String detail;
            if (!open) {
                Form.UnlockRequirements req = form.getUnlockRequirements();
                detail = "Requires level " + req.minLevel();
            } else {
                double mastery = accessor.dba$getFormMastery(form.getId());
                double drain = form.getBaseKiDrain() * (1.0 - (mastery / 100.0 * form.getMaxMasteryReduction()));
                detail = String.format(Locale.US, "Mastery %.0f%%    Ki %.1f/s", mastery, drain);
            }
            context.text(client.font, Component.literal(DbaUi.clip(client.font, detail, textW)), x + 10, rowY + 20, DbaUi.MUTED);

            if (open) {
                int bx = x + w - btnW - 8;
                int by = rowY + (ROW_H - DbaUi.BUTTON_H) / 2;
                boolean hover = DbaUi.hit(mouseX, mouseY, bx, by, btnW, DbaUi.BUTTON_H);
                DbaUi.button(context, client.font, bx, by, btnW, DbaUi.BUTTON_H,
                        on ? "Revert" : "Transform", hover, on ? DbaUi.Kind.DANGER : DbaUi.Kind.PRIMARY);
            } else {
                context.text(client.font, Component.literal("Locked"), x + w - 8 - client.font.width("Locked"), rowY + 18, DbaUi.MUTED);
            }
        }
        context.disableScissor();
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
        clamp(forms.size(), h);
        Identifier active = accessor.dba$getActiveFormId();
        int btnW = Math.min(88, Math.max(64, w / 5));
        for (int i = 0; i < forms.size(); i++) {
            Form form = forms.get(i);
            if (!unlocked(accessor, form)) {
                continue;
            }
            int rowY = y + i * (ROW_H + DbaUi.GAP) - scroll;
            int bx = x + w - btnW - 8;
            int by = rowY + (ROW_H - DbaUi.BUTTON_H) / 2;
            if (!DbaUi.hit(event.x(), event.y(), bx, by, btnW, DbaUi.BUTTON_H)) {
                continue;
            }
            if (by + DbaUi.BUTTON_H < y || by > y + h) {
                continue;
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
        return false;
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
        if (!DbaUi.hit(mouseX, mouseY, x, y, w, h)) {
            return false;
        }
        scroll -= (int) Math.signum(verticalAmount) * (ROW_H + DbaUi.GAP);
        return true;
    }
}
