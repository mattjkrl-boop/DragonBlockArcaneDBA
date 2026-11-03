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
 * Skill constellations. The slot strip, the nodes, and the inspector share one set of rectangles.
 */
public class TechniquesTab implements MenuTab {
    private static final int STRIP = 44;

    private DbaMenuScreen parent;
    private static double panX;
    private static double panY;
    private static double zoom = 1.0;
    private static boolean fitted;

    private boolean dragging;
    private double lastX;
    private double lastY;
    private String selectedId;
    private boolean inspector;
    private int inspectorScroll;

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

    private void fit(int listW, int listH, List<Technique> techs) {
        if (techs == null || techs.isEmpty()) {
            panX = 0;
            panY = 0;
            zoom = 1.0;
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Technique tech : techs) {
            minX = Math.min(minX, tech.x());
            maxX = Math.max(maxX, tech.x());
            minY = Math.min(minY, tech.y());
            maxY = Math.max(maxY, tech.y());
        }
        int spanX = Math.max(80, (maxX - minX) + 40);
        int spanY = Math.max(80, (maxY - minY) + 40);
        int availW = Math.max(80, listW - 20);
        int availH = Math.max(80, listH - STRIP - 28);
        zoom = Math.max(0.35, Math.min(1.4, Math.min((double) availW / spanX, (double) availH / spanY)));
        panX = -((minX + maxX) / 2.0) * zoom;
        panY = -((minY + maxY) / 2.0) * zoom + 8;
    }

    private int nodeX(Technique tech, int index, int total) {
        if (tech.x() != 0 || tech.y() != 0) {
            return tech.x();
        }
        double angle = (2.0 * Math.PI * index) / Math.max(1, total) - Math.PI / 2.0;
        return (int) (Math.cos(angle) * 110.0);
    }

    private int nodeY(Technique tech, int index, int total) {
        if (tech.x() != 0 || tech.y() != 0) {
            return tech.y();
        }
        double angle = (2.0 * Math.PI * index) / Math.max(1, total) - Math.PI / 2.0;
        return (int) (Math.sin(angle) * 110.0);
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

    private int inspectorW(int listW) {
        return Math.min(196, Math.max(148, listW * 38 / 100));
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
        if (!fitted) {
            fit(w, h, techs);
            fitted = true;
        }
        DbaUi.panel(context, x, y, w, h);

        int slotW = Math.max(48, (w - 16) / 3);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * (slotW + 4);
            boolean hover = DbaUi.hit(mouseX, mouseY, sx, y, slotW, 36);
            DbaUi.row(context, sx, y, slotW, 36, hover, false);
            String key = DbaUi.clip(font, DbaUi.slotKey(i), slotW - 12);
            context.text(font, Component.literal(key), sx + 8, y + 4, DbaUi.GOLD);
            context.text(font, Component.literal(DbaUi.clip(font, slotLabel(accessor, i), slotW - 12)), sx + 8, y + 16, DbaUi.TEXT);
        }

        if (techs.isEmpty()) {
            DbaUi.empty(context, font, x, y + STRIP, w, h - STRIP, "No skills for this race.");
            return;
        }

        int canvasY = y + STRIP;
        int canvasH = h - STRIP;
        context.enableScissor(x, canvasY, x + w, y + h);
        int originX = x + w / 2 + (int) Math.round(panX);
        int originY = canvasY + canvasH / 2 + (int) Math.round(panY);
        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            if (!tech.hasPrerequisites()) {
                continue;
            }
            int ax = originX + (int) Math.round(nodeX(tech, i, techs.size()) * zoom);
            int ay = originY + (int) Math.round(nodeY(tech, i, techs.size()) * zoom);
            for (String need : tech.prerequisiteTechniqueIds()) {
                for (int p = 0; p < techs.size(); p++) {
                    if (!techs.get(p).id().equals(need)) {
                        continue;
                    }
                    int bx = originX + (int) Math.round(nodeX(techs.get(p), p, techs.size()) * zoom);
                    int by = originY + (int) Math.round(nodeY(techs.get(p), p, techs.size()) * zoom);
                    boolean linked = accessor.dba$hasTechnique(need);
                    drawLine(context, bx, by, ax, ay, linked ? groupColor(tech.group()) : DbaUi.LINE);
                    break;
                }
            }
        }

        int half = Math.max(6, Math.min(11, (int) Math.round(8 * Math.sqrt(zoom))));
        Technique hovered = null;
        for (int i = 0; i < techs.size(); i++) {
            Technique tech = techs.get(i);
            int nx = originX + (int) Math.round(nodeX(tech, i, techs.size()) * zoom);
            int ny = originY + (int) Math.round(nodeY(tech, i, techs.size()) * zoom);
            boolean unlocked = accessor.dba$hasTechnique(tech.id()) || accessor.dba$getTechniqueLevel(tech.id()) > 0;
            int color = groupColor(tech.group());
            context.fill(nx - half, ny - half, nx + half, ny + half, unlocked ? DbaUi.HEADER_BG : DbaUi.INK_DEEP);
            int edge = unlocked ? color : DbaUi.LINE;
            context.fill(nx - half, ny - half, nx + half, ny - half + 1, edge);
            context.fill(nx - half, ny + half - 1, nx + half, ny + half, edge);
            context.fill(nx - half, ny, nx - half + 1, ny + half, edge);
            context.fill(nx + half - 1, ny, nx + half, ny + half, edge);
            if (tech.id().equals(selectedId) && inspector) {
                context.fill(nx - half - 2, ny - half - 2, nx + half + 2, ny - half - 1, DbaUi.TEXT);
            }
            if (zoom >= 0.55) {
                String mark = tech.name().isEmpty() ? "?" : tech.name().substring(0, 1).toUpperCase(Locale.ROOT);
                context.centeredText(font, Component.literal(mark), nx, ny - 4, unlocked ? DbaUi.TEXT : DbaUi.MUTED);
            }
            if (DbaUi.hit(mouseX, mouseY, nx - half - 2, ny - half - 2, half * 2 + 4, half * 2 + 4)
                    && mouseY >= canvasY && mouseY < y + h) {
                hovered = tech;
            }
        }
        context.disableScissor();

        int fitW = 52;
        int fitX = x + 4;
        int fitY = y + h - 20;
        boolean fitHover = DbaUi.hit(mouseX, mouseY, fitX, fitY, fitW, 16);
        DbaUi.button(context, font, fitX, fitY, fitW, 16, "Fit", fitHover, DbaUi.Kind.SECONDARY);

        if (inspector && selectedId != null) {
            Technique selected = find(selectedId);
            if (selected != null) {
                int dw = inspectorW(w);
                drawInspector(context, font, mouseX, mouseY, x + w - dw, y, dw, h, selected, accessor);
            }
        } else if (hovered != null && !dragging) {
            drawTip(context, font, mouseX, mouseY, x, y, w, h, hovered, accessor);
        }
    }

    private void drawInspector(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY,
                               int dx, int dy, int dw, int dh, Technique tech, PlayerStatsAccessor accessor) {
        DbaUi.panel(context, dx, dy, dw, dh);
        context.fill(dx, dy, dx + 2, dy + dh, groupColor(tech.group()));
        Sheet sheet = measure(font, dx, dy, dw, dh, tech, accessor);
        boolean backHover = DbaUi.hit(mouseX, mouseY, sheet.backX, sheet.backY, 44, 16);
        DbaUi.button(context, font, sheet.backX, sheet.backY, 44, 16, "Back", backHover, DbaUi.Kind.SECONDARY);

        context.enableScissor(dx, sheet.viewTop, dx + dw, sheet.viewTop + sheet.viewH);
        int textW = dw - 16;
        context.text(font, Component.literal(DbaUi.clip(font, tech.name(), textW)), dx + 8, sheet.nameY, DbaUi.TEXT);
        context.text(font, Component.literal(DbaUi.clip(font, sheet.status, textW)), dx + 8, sheet.statusY, sheet.statusColor);
        for (int i = 0; i < sheet.lines.size(); i++) {
            context.text(font, Component.literal(sheet.lines.get(i)), dx + 8, sheet.descY + i * 10, DbaUi.MUTED);
        }
        if (sheet.liveAction) {
            boolean hover = DbaUi.hit(mouseX, mouseY, sheet.actionX, sheet.actionY, sheet.actionW, DbaUi.BUTTON_H)
                    && sheet.actionY >= sheet.viewTop && sheet.actionY + DbaUi.BUTTON_H <= sheet.viewTop + sheet.viewH;
            DbaUi.button(context, font, sheet.actionX, sheet.actionY, sheet.actionW, DbaUi.BUTTON_H, sheet.actionLabel, hover, DbaUi.Kind.PRIMARY);
        } else {
            context.text(font, Component.literal(DbaUi.clip(font, sheet.actionLabel, textW)), dx + 8, sheet.actionY + 4, DbaUi.MUTED);
        }
        if (sheet.equips) {
            context.text(font, Component.literal("Assign"), dx + 8, sheet.assignY, DbaUi.MUTED);
            for (int s = 0; s < 3; s++) {
                boolean here = tech.id().equals(accessor.dba$getEquippedTechnique(s));
                boolean hover = DbaUi.hit(mouseX, mouseY, sheet.actionX, sheet.equipY[s], sheet.actionW, 16)
                        && sheet.equipY[s] >= sheet.viewTop && sheet.equipY[s] + 16 <= sheet.viewTop + sheet.viewH;
                String label = "Slot " + (s + 1) + "  " + (here ? "Remove" : "Equip");
                DbaUi.button(context, font, sheet.actionX, sheet.equipY[s], sheet.actionW, 16, label, hover,
                        here ? DbaUi.Kind.DANGER : DbaUi.Kind.SECONDARY);
            }
        }
        context.disableScissor();
    }

    private Sheet measure(Font font, int dx, int dy, int dw, int dh, Technique tech, PlayerStatsAccessor accessor) {
        Sheet sheet = new Sheet();
        sheet.backX = dx + dw - 52;
        sheet.backY = dy + 6;
        sheet.viewTop = dy + 26;
        sheet.viewH = dh - 30;
        int y = sheet.viewTop - inspectorScroll;
        sheet.nameY = y;
        y += 14;
        int level = accessor.dba$getTechniqueLevel(tech.id());
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || level > 0;
        boolean prereq = prereqsMet(accessor, tech);
        boolean levelMet = accessor.dba$getLevel() >= tech.unlockLevel();
        int next = Math.min(10, Math.max(1, level + 1));
        int cost = PlayerStats.getTechniqueUpgradeCost(tech.id(), unlocked ? next : 1);
        boolean afford = accessor.dba$getStatPoints() >= cost;
        if (!unlocked) {
            sheet.status = prereq ? (levelMet ? "Ready to learn" : "Requires level " + tech.unlockLevel()) : "Missing a required skill";
            sheet.statusColor = prereq && levelMet ? DbaUi.OK : DbaUi.DANGER;
        } else {
            sheet.status = "Level " + level + " / 10";
            sheet.statusColor = DbaUi.GOLD;
        }
        sheet.statusY = y;
        y += 14;
        sheet.lines = DbaUi.wrap(font, tech.description() == null ? "" : tech.description(), dw - 16);
        sheet.descY = y;
        y += sheet.lines.size() * 10 + 6;
        sheet.actionX = dx + 8;
        sheet.actionY = y;
        sheet.actionW = dw - 16;
        if (!unlocked && prereq && levelMet && afford) {
            sheet.liveAction = true;
            sheet.unlock = true;
            sheet.actionLabel = cost > 0 ? "Learn  ·  " + cost + " AP" : "Learn";
        } else if (unlocked && level < 10 && afford) {
            sheet.liveAction = true;
            sheet.actionLabel = cost > 0 ? "Raise  ·  " + cost + " AP" : "Raise";
        } else if (!unlocked && !prereq) {
            sheet.actionLabel = missingName(tech);
        } else if (!unlocked) {
            sheet.actionLabel = levelMet ? "Need " + cost + " AP" : "Need level " + tech.unlockLevel();
        } else if (level >= 10) {
            sheet.actionLabel = "Mastered";
        } else {
            sheet.actionLabel = "Need " + cost + " AP";
        }
        y += DbaUi.BUTTON_H + 8;
        sheet.equips = unlocked;
        if (unlocked) {
            sheet.assignY = y;
            y += 12;
            sheet.equipY = new int[3];
            for (int s = 0; s < 3; s++) {
                sheet.equipY[s] = y;
                y += 20;
            }
        }
        int content = y - (sheet.viewTop - inspectorScroll);
        int max = Math.max(0, content - sheet.viewH);
        if (inspectorScroll < 0) inspectorScroll = 0;
        if (inspectorScroll > max) inspectorScroll = max;
        return sheet;
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

    private void drawTip(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY,
                         int x, int y, int w, int h, Technique tech, PlayerStatsAccessor accessor) {
        int tipW = Math.min(180, w - 16);
        int tipH = 52;
        int tipX = Math.min(mouseX + 10, x + w - tipW - 4);
        int tipY = Math.min(Math.max(y + 4, mouseY - 8), y + h - tipH - 4);
        if (tipX < x + 4) tipX = x + 4;
        DbaUi.panel(context, tipX, tipY, tipW, tipH);
        context.fill(tipX, tipY, tipX + tipW, tipY + 1, groupColor(tech.group()));
        boolean unlocked = accessor.dba$hasTechnique(tech.id()) || accessor.dba$getTechniqueLevel(tech.id()) > 0;
        context.text(font, Component.literal(DbaUi.clip(font, tech.name(), tipW - 12)), tipX + 6, tipY + 6, DbaUi.TEXT);
        String state = unlocked ? "Level " + accessor.dba$getTechniqueLevel(tech.id()) + " / 10" : "Not learned";
        context.text(font, Component.literal(state), tipX + 6, tipY + 18, DbaUi.MUTED);
        String desc = tech.description() == null ? "" : tech.description();
        context.text(font, Component.literal(DbaUi.clip(font, desc, tipW - 12)), tipX + 6, tipY + 30, DbaUi.MUTED);
        context.text(font, Component.literal("Open"), tipX + 6, tipY + 40, DbaUi.GOLD);
    }

    private void drawLine(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            return;
        }
        int steps = Math.max(1, (int) (dist / 3));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) (x1 + t * dx);
            int py = (int) (y1 + t * dy);
            context.fill(px, py, px + 2, py + 2, color);
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
        int fitX = x + 4;
        int fitY = y + h - 20;
        if (!techs.isEmpty() && DbaUi.hit(mx, my, fitX, fitY, 52, 16)) {
            DbaUi.playClick();
            fit(w, h, techs);
            fitted = true;
            return true;
        }

        int slotW = Math.max(48, (w - 16) / 3);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * (slotW + 4);
            if (!DbaUi.hit(mx, my, sx, y, slotW, 36)) {
                continue;
            }
            DbaUi.playClick();
            if (skillInSlot(accessor, i)) {
                selectedId = accessor.dba$getEquippedTechnique(i);
                inspector = true;
                inspectorScroll = 0;
            } else {
                parent.openCraft(i);
            }
            return true;
        }

        if (inspector && selectedId != null) {
            Technique selected = find(selectedId);
            int dw = inspectorW(w);
            int dx = x + w - dw;
            if (selected != null && DbaUi.hit(mx, my, dx, y, dw, h)) {
                Font font = client.font;
                Sheet sheet = measure(font, dx, y, dw, h, selected, accessor);
                if (DbaUi.hit(mx, my, sheet.backX, sheet.backY, 44, 16)) {
                    DbaUi.playClick();
                    inspector = false;
                    return true;
                }
                if (sheet.liveAction && inView(sheet, sheet.actionY, DbaUi.BUTTON_H)
                        && DbaUi.hit(mx, my, sheet.actionX, sheet.actionY, sheet.actionW, DbaUi.BUTTON_H)) {
                    DbaUi.playClick();
                    if (sheet.unlock) {
                        ClientPlayNetworking.send(new C2SUnlockTechniquePayload(selected.id()));
                    } else {
                        ClientPlayNetworking.send(new C2SUpgradeTechniquePayload(selected.id()));
                    }
                    return true;
                }
                if (sheet.equips) {
                    for (int s = 0; s < 3; s++) {
                        if (inView(sheet, sheet.equipY[s], 16)
                                && DbaUi.hit(mx, my, sheet.actionX, sheet.equipY[s], sheet.actionW, 16)) {
                            DbaUi.playClick();
                            boolean here = selected.id().equals(accessor.dba$getEquippedTechnique(s));
                            ClientPlayNetworking.send(new C2SEquipTechniquePayload(s, here ? "" : selected.id()));
                            return true;
                        }
                    }
                }
                return true;
            }
        }

        if (techs.isEmpty()) {
            return false;
        }
        int canvasY = y + STRIP;
        int canvasH = h - STRIP;
        int originX = x + w / 2 + (int) Math.round(panX);
        int originY = canvasY + canvasH / 2 + (int) Math.round(panY);
        int half = Math.max(6, Math.min(11, (int) Math.round(8 * Math.sqrt(zoom))));
        if (my >= canvasY) {
            for (int i = 0; i < techs.size(); i++) {
                Technique tech = techs.get(i);
                int nx = originX + (int) Math.round(nodeX(tech, i, techs.size()) * zoom);
                int ny = originY + (int) Math.round(nodeY(tech, i, techs.size()) * zoom);
                if (DbaUi.hit(mx, my, nx - half - 2, ny - half - 2, half * 2 + 4, half * 2 + 4)) {
                    DbaUi.playClick();
                    selectedId = tech.id();
                    inspector = true;
                    inspectorScroll = 0;
                    return true;
                }
            }
        }
        dragging = true;
        lastX = mx;
        lastY = my;
        return true;
    }

    private boolean inView(Sheet sheet, int controlY, int controlH) {
        return controlY >= sheet.viewTop && controlY + controlH <= sheet.viewTop + sheet.viewH;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!dragging) {
            return false;
        }
        panX += event.x() - lastX;
        panY += event.y() - lastY;
        lastX = event.x();
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
        int dw = inspectorW(w);
        int dx = x + w - dw;
        if (inspector && mouseX >= dx) {
            inspectorScroll -= (int) Math.signum(verticalAmount) * 16;
            return true;
        }
        double old = zoom;
        double next = Math.max(0.3, Math.min(2.2, old * (verticalAmount > 0 ? 1.12 : 0.89)));
        if (Math.abs(next - old) < 0.0001) {
            return true;
        }
        int cx = x + w / 2;
        int cy = y + STRIP + (h - STRIP) / 2;
        panX = panX + (mouseX - cx - panX) * (1.0 - next / old);
        panY = panY + (mouseY - cy - panY) * (1.0 - next / old);
        zoom = next;
        return true;
    }

    private static final class Sheet {
        int backX, backY;
        int viewTop, viewH;
        int nameY, statusY, descY;
        int statusColor;
        String status = "";
        String actionLabel = "";
        boolean liveAction;
        boolean unlock;
        int actionX, actionY, actionW;
        boolean equips;
        int assignY;
        int[] equipY = new int[3];
        List<String> lines = List.of();
    }
}
