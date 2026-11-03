package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.ki.KiTechnique;
import com.dragonblockarcanedba.network.C2SEquipTechniquePayload;
import com.dragonblockarcanedba.network.C2SUnlockTechniquePayload;
import com.dragonblockarcanedba.network.C2SUpgradeTechniquePayload;
import com.dragonblockarcanedba.registry.Technique;
import com.dragonblockarcanedba.registry.TechniqueRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Live skill list from the technique registry, with the equip slots above it
 * and the learn, raise, and assign controls on the selected skill.
 */
public class TechniquesTab implements MenuTab {
    private static final int ROW_H = 22;
    private static final int SLOT_H = 32;

    private DbaMenuScreen parent;
    private int scroll;
    private int detailScroll;
    private String selectedId;
    private boolean inspector;
    private boolean dragging;
    private double lastY;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.dragging = false;
    }

    private List<Technique> techniques(PlayerStatsAccessor accessor) {
        Identifier race = accessor.dba$getRaceId();
        if (race != null) {
            List<Technique> list = TechniqueRegistry.getTechniquesForRace(race);
            if (!list.isEmpty()) {
                return list;
            }
        }
        return TechniqueRegistry.getTechniquesForRace(Identifier.tryParse("dragonblockarcanedba:human"));
    }

    private int groupColor(String group) {
        if (group == null) {
            return DbaUi.GOLD;
        }
        return switch (group.toLowerCase(Locale.ROOT)) {
            case "arcane" -> 0xFF7EB6FF;
            case "celestial" -> DbaUi.GOLD;
            case "astral" -> 0xFFC59BE8;
            case "abyssal" -> DbaUi.DANGER;
            default -> DbaUi.OK;
        };
    }

    private Technique find(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String namespaced = id.contains(":") ? id : "dragonblockarcanedba:" + id;
        return TechniqueRegistry.getTechnique(Identifier.tryParse(namespaced));
    }

    private String slotLabel(PlayerStatsAccessor accessor, int slot) {
        String equipped = accessor.dba$getEquippedTechnique(slot);
        if (equipped != null && !equipped.isEmpty()) {
            Technique tech = find(equipped);
            if (tech != null) {
                return tech.name();
            }
        }
        KiTechnique ki = accessor.dba$getKiTechniqueSlot(slot);
        if (ki != null && !ki.isEmpty) {
            return ki.displayName();
        }
        return "Empty";
    }

    private boolean skillInSlot(PlayerStatsAccessor accessor, int slot) {
        String equipped = accessor.dba$getEquippedTechnique(slot);
        if (equipped == null || equipped.isEmpty()) {
            return false;
        }
        return find(equipped) != null;
    }

    private int listW(int w) {
        if (!inspector) {
            return w;
        }
        return Math.max(140, w - inspectorW(w) - 6);
    }

    private int inspectorW(int w) {
        return Math.min(220, Math.max(160, w * 40 / 100));
    }

    private int listTop(int y) { return y + SLOT_H + 6; }

    private void clampList(int count, int viewH) {
        int content = count * (ROW_H + 2);
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
        Font font = client.font;
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        List<Technique> techs = techniques(accessor);

        int slotW = Math.max(48, (w - 8) / 3);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * (slotW + 4);
            boolean hover = DbaUi.hit(mouseX, mouseY, sx, y, slotW, SLOT_H);
            DbaUi.row(context, sx, y, slotW, SLOT_H, hover, false);
            context.text(font, Component.literal(DbaUi.clip(font, DbaUi.slotKey(i), slotW - 12)), sx + 6, y + 4, DbaUi.GOLD);
            context.text(font, Component.literal(DbaUi.clip(font, slotLabel(accessor, i), slotW - 12)), sx + 6, y + 16, DbaUi.TEXT);
        }

        int top = listTop(y);
        int listH = y + h - top;
        if (techs.isEmpty()) {
            DbaUi.empty(context, font, x, top, w, listH, "No skills for this race.");
            return;
        }
        int lw = listW(w);
        clampList(techs.size(), listH - 20);
        context.enableScissor(x, top, x + lw, y + h - 18);
        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            int rowY = top + i * (ROW_H + 2) - scroll;
            if (rowY + ROW_H < top || rowY > y + h - 18) {
                continue;
            }
            boolean on = inspector && tech.id().equals(selectedId);
            boolean hover = DbaUi.hit(mouseX, mouseY, x, rowY, lw, ROW_H);
            DbaUi.row(context, x, rowY, lw, ROW_H, hover, on);
            context.fill(x, rowY, x + 3, rowY + ROW_H, groupColor(tech.group()));
            boolean unlocked = accessor.dba$hasTechnique(tech.id()) || accessor.dba$getTechniqueLevel(tech.id()) > 0;
            context.text(font, Component.literal(DbaUi.clip(font, tech.name(), lw - 70)), x + 8, rowY + 7, unlocked ? DbaUi.TEXT : DbaUi.MUTED);
            String mark = unlocked ? "Lv " + accessor.dba$getTechniqueLevel(tech.id()) : "New";
            context.text(font, Component.literal(mark), x + lw - 8 - font.width(mark), rowY + 7, DbaUi.MUTED);
        }
        context.disableScissor();

        boolean fitHover = DbaUi.hit(mouseX, mouseY, x, y + h - 16, 52, 16);
        DbaUi.button(context, font, x, y + h - 16, 52, 16, "Fit", fitHover, DbaUi.Kind.SECONDARY);

        if (inspector && selectedId != null) {
            Technique selected = find(selectedId);
            if (selected != null) {
                int dw = inspectorW(w);
                drawInspector(context, font, mouseX, mouseY, x + w - dw, top, dw, listH, selected, accessor);
            }
        }
    }

    private void drawInspector(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY,
                               int dx, int dy, int dw, int dh, Technique tech, PlayerStatsAccessor accessor) {
        DbaUi.panel(context, dx, dy, dw, dh);
        context.fill(dx, dy, dx + 3, dy + dh, groupColor(tech.group()));
        boolean backHover = DbaUi.hit(mouseX, mouseY, dx + dw - 52, dy + 4, 46, 16);
        DbaUi.button(context, font, dx + dw - 52, dy + 4, 46, 16, "Back", backHover, DbaUi.Kind.SECONDARY);

        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        boolean prereq = prereqsMet(accessor, tech);
        boolean levelMet = accessor.dba$getLevel() >= tech.unlockLevel();
        int next = Math.min(10, Math.max(1, level + 1));
        int cost = PlayerStats.getTechniqueUpgradeCost(tech.id(), unlocked ? next : 1);
        boolean afford = accessor.dba$getStatPoints() >= cost;

        String status;
        int statusColor;
        if (!unlocked) {
            status = prereq ? (levelMet ? "Ready to learn" : "Requires level " + tech.unlockLevel()) : "Missing a required skill";
            statusColor = prereq && levelMet ? DbaUi.OK : DbaUi.DANGER;
        } else {
            status = "Level " + level + " / 10";
            statusColor = DbaUi.GOLD;
        }

        boolean live = false;
        boolean unlock = false;
        String action;
        if (!unlocked && prereq && levelMet && afford) {
            live = true;
            unlock = true;
            action = cost > 0 ? "Learn  ·  " + cost + " AP" : "Learn";
        } else if (unlocked && level < 10 && afford) {
            live = true;
            action = cost > 0 ? "Raise  ·  " + cost + " AP" : "Raise";
        } else if (!unlocked && !prereq) {
            action = missingName(tech);
        } else if (!unlocked) {
            action = levelMet ? "Need " + cost + " AP" : "Need level " + tech.unlockLevel();
        } else if (level >= 10) {
            action = "Mastered";
        } else {
            action = "Need " + cost + " AP";
        }

        int footer = DbaUi.BUTTON_H + 8 + (unlocked ? 12 + 3 * 18 : 0);
        int descTop = dy + 36;
        int descH = Math.max(12, dh - 36 - footer);
        List<String> lines = DbaUi.wrap(font, tech.description() == null ? "" : tech.description(), dw - 16);
        int maxScroll = Math.max(0, lines.size() * 10 - descH);
        if (detailScroll < 0) detailScroll = 0;
        if (detailScroll > maxScroll) detailScroll = maxScroll;

        context.text(font, Component.literal(DbaUi.clip(font, tech.name(), dw - 64)), dx + 8, dy + 6, DbaUi.TEXT);
        context.text(font, Component.literal(DbaUi.clip(font, status, dw - 16)), dx + 8, dy + 20, statusColor);
        context.enableScissor(dx, descTop, dx + dw, descTop + descH);
        for (int i = 0; i < lines.size(); i++) {
            context.text(font, Component.literal(lines.get(i)), dx + 8, descTop + i * 10 - detailScroll, DbaUi.MUTED);
        }
        context.disableScissor();

        int actionY = descTop + descH + 4;
        int actionW = dw - 16;
        if (live) {
            boolean hover = DbaUi.hit(mouseX, mouseY, dx + 8, actionY, actionW, DbaUi.BUTTON_H);
            DbaUi.button(context, font, dx + 8, actionY, actionW, DbaUi.BUTTON_H, action, hover, DbaUi.Kind.PRIMARY);
        } else {
            context.text(font, Component.literal(DbaUi.clip(font, action, actionW)), dx + 8, actionY + 4, DbaUi.MUTED);
        }
        if (unlocked) {
            int assignY = actionY + DbaUi.BUTTON_H + 6;
            context.text(font, Component.literal("Assign"), dx + 8, assignY, DbaUi.MUTED);
            for (int s = 0; s < 3; s++) {
                int ey = assignY + 12 + s * 18;
                boolean here = tech.id().equals(accessor.dba$getEquippedTechnique(s));
                boolean hover = DbaUi.hit(mouseX, mouseY, dx + 8, ey, actionW, 16);
                String label = "Slot " + (s + 1) + "  " + (here ? "Remove" : "Equip");
                DbaUi.button(context, font, dx + 8, ey, actionW, 16, label, hover, here ? DbaUi.Kind.DANGER : DbaUi.Kind.SECONDARY);
            }
        }
    }

    private boolean prereqsMet(PlayerStatsAccessor accessor, Technique tech) {
        if (!tech.hasPrerequisites()) {
            return true;
        }
        for (String id : tech.prerequisiteTechniqueIds()) {
            if (!accessor.dba$hasTechnique(id)) {
                return false;
            }
        }
        return true;
    }

    private String missingName(Technique tech) {
        if (!tech.hasPrerequisites()) {
            return "Locked";
        }
        for (String id : tech.prerequisiteTechniqueIds()) {
            Technique need = find(id);
            if (need != null) {
                return "Requires " + need.name();
            }
        }
        return "Locked";
    }

    private boolean liveAction(PlayerStatsAccessor accessor, Technique tech) {
        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        boolean prereq = prereqsMet(accessor, tech);
        boolean levelMet = accessor.dba$getLevel() >= tech.unlockLevel();
        int next = Math.min(10, Math.max(1, level + 1));
        int cost = PlayerStats.getTechniqueUpgradeCost(tech.id(), unlocked ? next : 1);
        boolean afford = accessor.dba$getStatPoints() >= cost;
        return (!unlocked && prereq && levelMet && afford) || (unlocked && level < 10 && afford);
    }

    private boolean unlockAction(PlayerStatsAccessor accessor, Technique tech) {
        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        return !unlocked && liveAction(accessor, tech);
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
        double mx = event.x();
        double my = event.y();
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        if (!DbaUi.hit(mx, my, x, y, w, h)) {
            return false;
        }
        List<Technique> techs = techniques(accessor);
        if (!techs.isEmpty() && DbaUi.hit(mx, my, x, y + h - 16, 52, 16)) {
            DbaUi.playClick();
            scroll = 0;
            return true;
        }

        int slotW = Math.max(48, (w - 8) / 3);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * (slotW + 4);
            if (!DbaUi.hit(mx, my, sx, y, slotW, SLOT_H)) {
                continue;
            }
            DbaUi.playClick();
            if (skillInSlot(accessor, i)) {
                selectedId = accessor.dba$getEquippedTechnique(i);
                inspector = true;
                detailScroll = 0;
            } else {
                parent.openCraft(i);
            }
            return true;
        }

        int top = listTop(y);
        int lw = listW(w);
        if (inspector && selectedId != null && mx >= x + w - inspectorW(w)) {
            return clickInspector(mx, my, x, y, w, h, accessor);
        }
        if (techs.isEmpty() || my < top) {
            return false;
        }
        clampList(techs.size(), y + h - top - 20);
        for (int i = 0; i < techs.size(); i++) {
            int rowY = top + i * (ROW_H + 2) - scroll;
            if (rowY + ROW_H < top || rowY > y + h - 18) {
                continue;
            }
            if (DbaUi.hit(mx, my, x, rowY, lw, ROW_H)) {
                DbaUi.playClick();
                selectedId = techs.get(i).id();
                inspector = true;
                detailScroll = 0;
                return true;
            }
        }
        if (mx < x + lw && my >= top && my < y + h - 18) {
            dragging = true;
            lastY = my;
            return true;
        }
        return false;
    }

    private boolean clickInspector(double mx, double my, int x, int y, int w, int h, PlayerStatsAccessor accessor) {
        Technique tech = find(selectedId);
        if (tech == null) {
            inspector = false;
            return true;
        }
        int dw = inspectorW(w);
        int dx = x + w - dw;
        int dy = listTop(y);
        int dh = y + h - dy;
        if (DbaUi.hit(mx, my, dx + dw - 52, dy + 4, 46, 16)) {
            DbaUi.playClick();
            inspector = false;
            return true;
        }
        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        int footer = DbaUi.BUTTON_H + 8 + (unlocked ? 12 + 3 * 18 : 0);
        int descTop = dy + 36;
        int descH = Math.max(12, dh - 36 - footer);
        int actionY = descTop + descH + 4;
        int actionW = dw - 16;
        if (liveAction(accessor, tech) && DbaUi.hit(mx, my, dx + 8, actionY, actionW, DbaUi.BUTTON_H)) {
            DbaUi.playClick();
            if (unlockAction(accessor, tech)) {
                ClientPlayNetworking.send(new C2SUnlockTechniquePayload(tech.id()));
            } else {
                ClientPlayNetworking.send(new C2SUpgradeTechniquePayload(tech.id()));
            }
            return true;
        }
        if (unlocked) {
            int assignY = actionY + DbaUi.BUTTON_H + 6;
            for (int s = 0; s < 3; s++) {
                int ey = assignY + 12 + s * 18;
                if (DbaUi.hit(mx, my, dx + 8, ey, actionW, 16)) {
                    DbaUi.playClick();
                    boolean here = tech.id().equals(accessor.dba$getEquippedTechnique(s));
                    ClientPlayNetworking.send(new C2SEquipTechniquePayload(s, here ? "" : tech.id()));
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging) {
            return false;
        }
        scroll -= (int) Math.round(event.y() - lastY);
        lastY = event.y();
        return true;
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
        int x = parent.getContentX();
        int y = parent.getContentY();
        int w = parent.getContentWidth();
        int h = parent.getContentHeight();
        if (!DbaUi.hit(mouseX, mouseY, x, y, w, h)) {
            return false;
        }
        if (inspector && mouseX >= x + w - inspectorW(w)) {
            detailScroll -= (int) Math.signum(verticalAmount) * 16;
            return true;
        }
        scroll -= (int) Math.signum(verticalAmount) * (ROW_H + 2);
        return true;
    }
}
