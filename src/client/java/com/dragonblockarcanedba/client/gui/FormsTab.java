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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Transformation Sanctuary:
 * - Themed transformation cards with aura borders (gold, crimson, silver, purple)
 * - Mastery progress gauges (0-100%)
 * - Ki drain metrics and stat multiplier chips
 * - Smooth scrolling and tactile transform/revert buttons
 */
public class FormsTab implements MenuTab {
    private DbaMenuScreen parent;
    private double scrollY = 0;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        this.scrollY = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollY += verticalAmount * 24;
        if (this.scrollY > 0) this.scrollY = 0;
        return true;
    }

    private int getFormAuraColor(String formId) {
        if (formId.contains("kaioken")) return 0xFFFF2222;
        if (formId.contains("super_saiyan") || formId.contains("ssj")) return 0xFFFFDD44;
        if (formId.contains("god")) return 0xFFFF3366;
        if (formId.contains("blue") || formId.contains("ssb")) return 0xFF00E5FF;
        if (formId.contains("instinct")) return 0xFFEEEEFF;
        if (formId.contains("ego")) return 0xFFDD22DD;
        if (formId.contains("beast")) return 0xFFCC3333;
        if (formId.contains("golden")) return 0xFFFFCC00;
        return 0xFF55FF88;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        int startX = parent.getContentX();
        int startY = parent.getContentY();
        int width = parent.getContentWidth();
        int height = parent.getContentHeight();

        // Header
        context.text(client.font, Component.literal("ARCANE TRANSFORMATIONS"), startX + 10, startY + 6, 0xFF55FF88);

        Identifier activeForm = accessor.dba$getActiveFormId();
        if (activeForm != null) {
            String activeStr = "● ACTIVE: " + activeForm.getPath().replace("_", " ").toUpperCase();
            context.text(client.font, Component.literal(activeStr), startX + width - client.font.width(activeStr) - 10, startY + 6, 0xFF55FF88);
        }

        int listY = startY + 20;
        int listH = height - 24;

        context.enableScissor(startX, listY, startX + width, listY + listH);

        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());

        if (compatible.isEmpty()) {
            context.centeredText(client.font, Component.literal("No compatible transformations found for your race."), startX + width / 2, listY + 40, 0xFF888888);
            context.disableScissor();
            return;
        }

        int cardH = 50;
        int spacing = 6;
        int totalHeight = compatible.size() * (cardH + spacing);
        int maxScroll = Math.max(0, totalHeight - listH);
        scrollY = Math.max(-maxScroll, Math.min(0, scrollY));

        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int cardY = listY + i * (cardH + spacing) + (int) scrollY;
            int cardX = startX + 6;
            int cardW = width - 12;

            if (cardY + cardH < listY || cardY > listY + listH) continue; // Culling

            double mastery = accessor.dba$getFormMastery(form.getId());
            com.dragonblockarcanedba.registry.Form.UnlockRequirements req = form.getUnlockRequirements();
            boolean levelMet = accessor.dba$getLevel() >= req.minLevel();
            boolean statsMet = true;
            if (req.minStats() != null) {
                if (accessor.dba$getStrength() < req.minStats().strength()) statsMet = false;
                if (accessor.dba$getDexterity() < req.minStats().agility()) statsMet = false;
                if (accessor.dba$getDefense() < req.minStats().defense()) statsMet = false;
                if (accessor.dba$getWillpower() < req.minStats().kiControl()) statsMet = false;
                if (accessor.dba$getSpirit() < req.minStats().kiCapacity()) statsMet = false;
            }
            boolean unlocked = levelMet && statsMet;
            boolean isActive = (activeForm != null && activeForm.equals(form.getId()));

            int auraCol = getFormAuraColor(form.getId().getPath());
            int bg = isActive ? 0xDD122A1E : (unlocked ? 0xAA0D131F : 0x66080C14);
            int border = isActive ? 0xFF55FF88 : (unlocked ? 0x4400E5FF : 0x22334455);

            // Card Background & Borders
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);
            context.fill(cardX, cardY, cardX + cardW, cardY + 1, border);
            context.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, border);
            context.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, border);
            context.fill(cardX, cardY, cardX + 3, cardY + cardH, auraCol); // Themed left aura line

            // Form Title
            String formName = form.getId().getPath().replace("_", " ").toUpperCase();
            int titleCol = isActive ? 0xFF55FF88 : (unlocked ? 0xFFFFFFFF : 0xFF778899);
            context.text(client.font, Component.literal(formName + (isActive ? " §a●" : "")), cardX + 12, cardY + 7, titleCol);

            // Row 2: Mastery Bar & Ki Drain
            if (unlocked) {
                // Mastery Progress Bar
                int mBarX = cardX + 12;
                int mBarY = cardY + 22;
                int mBarW = 110;
                int mBarH = 6;
                context.fill(mBarX, mBarY, mBarX + mBarW, mBarY + mBarH, 0x88000000);
                int mFill = (int) (mBarW * Math.min(1.0, mastery / 100.0));
                context.fill(mBarX, mBarY, mBarX + mFill, mBarY + mBarH, auraCol);

                String mTxt = String.format(Locale.US, "Mastery: %.1f%%", mastery);
                context.text(client.font, Component.literal(mTxt), cardX + 130, cardY + 21, 0xFFFFAA00);

                double actualDrain = form.getBaseKiDrain() * (1.0 - (mastery / 100.0 * form.getMaxMasteryReduction()));
                String drainTxt = String.format(Locale.US, "Ki Drain: %.1f/s", actualDrain);
                context.text(client.font, Component.literal(drainTxt), cardX + 12, cardY + 34, 0xFF00E5FF);
            } else {
                String reqStr = "Requirements: Level " + req.minLevel() + " + Attributes";
                context.text(client.font, Component.literal(reqStr), cardX + 12, cardY + 24, 0xFFFF5555);
            }

            // Right Action Button
            int btnW = 90;
            int btnH = 22;
            int btnX = cardX + cardW - btnW - 10;
            int btnY = cardY + (cardH - btnH) / 2;
            boolean hoverBtn = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            String btnTxt;
            int btnBg;
            int btnBorder;
            int btnTxtCol;

            if (isActive) {
                btnTxt = "REVERT";
                btnBg = hoverBtn ? 0xDDFF4444 : 0x88CC2222;
                btnBorder = 0xFFFF4444;
                btnTxtCol = 0xFFFFFFFF;
            } else if (unlocked) {
                btnTxt = "TRANSFORM";
                btnBg = hoverBtn ? 0xDD00C853 : 0x88009624;
                btnBorder = 0xFF55FF88;
                btnTxtCol = 0xFFFFFFFF;
            } else {
                btnTxt = "LOCKED";
                btnBg = 0x33222222;
                btnBorder = 0x55555555;
                btnTxtCol = 0xFF777777;
            }

            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
            context.fill(btnX, btnY, btnX + btnW, btnY + 1, btnBorder);
            context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, btnBorder);
            context.fill(btnX, btnY, btnX + 1, btnY + btnH, btnBorder);
            context.fill(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, btnBorder);
            context.centeredText(client.font, Component.literal(btnTxt), btnX + btnW / 2, btnY + 7, btnTxtCol);
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;

        int startX = parent.getContentX();
        int startY = parent.getContentY();
        int width = parent.getContentWidth();
        int height = parent.getContentHeight();

        double mouseX = event.x();
        double mouseY = event.y();

        int listY = startY + 20;
        int listH = height - 24;

        if (mouseY < listY || mouseY > listY + listH) return false;

        Identifier activeForm = accessor.dba$getActiveFormId();
        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());

        int cardH = 50;
        int spacing = 6;

        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int cardY = listY + i * (cardH + spacing) + (int) scrollY;
            int cardX = startX + 6;
            int cardW = width - 12;

            if (cardY + cardH < listY || cardY > listY + listH) continue;

            com.dragonblockarcanedba.registry.Form.UnlockRequirements req = form.getUnlockRequirements();
            boolean levelMet = accessor.dba$getLevel() >= req.minLevel();
            boolean statsMet = true;
            if (req.minStats() != null) {
                if (accessor.dba$getStrength() < req.minStats().strength()) statsMet = false;
                if (accessor.dba$getDexterity() < req.minStats().agility()) statsMet = false;
                if (accessor.dba$getDefense() < req.minStats().defense()) statsMet = false;
                if (accessor.dba$getWillpower() < req.minStats().kiControl()) statsMet = false;
                if (accessor.dba$getSpirit() < req.minStats().kiCapacity()) statsMet = false;
            }
            boolean unlocked = levelMet && statsMet;
            boolean isActive = (activeForm != null && activeForm.equals(form.getId()));

            int btnW = 90;
            int btnH = 22;
            int btnX = cardX + cardW - btnW - 10;
            int btnY = cardY + (cardH - btnH) / 2;

            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                DbaMenuScreen.playClickSound();
                if (isActive) {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putString("action", "untransform");
                    ClientPlayNetworking.send(new ActionPayload(nbt));
                    return true;
                } else if (unlocked) {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putString("action", "transform");
                    nbt.putString("form", form.getId().toString());
                    ClientPlayNetworking.send(new ActionPayload(nbt));
                    return true;
                }
            }
        }

        return false;
    }
}
