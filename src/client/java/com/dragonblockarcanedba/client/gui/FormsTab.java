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
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.stream.Collectors;

public class FormsTab implements MenuTab {
    private DbaMenuScreen parent;
    private double scrollY = 0;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollY += verticalAmount * 20;
        if (this.scrollY > 0) this.scrollY = 0;
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();
        int height = parent.getBgHeight();

        context.text(client.font, Component.literal("Arcane Transformations"), startX + 15, startY + 15, 0xFF55FF88);

        // enable scissor for scrolling
        context.enableScissor(startX, startY + 30, startX + width, startY + height);

        Identifier activeForm = accessor.dba$getActiveFormId();
        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());
            
        if (compatible.isEmpty()) {
            context.text(client.font, Component.literal("No compatible transformations found."), startX + 15, startY + 45, 0xFF888888);
            context.disableScissor();
            return;
        }

        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int cardHeight = 46;
            int cardX = startX + 10;
            int cardY = startY + 35 + i * (cardHeight + 8) + (int)scrollY;
            int cardWidth = width - 20;
            
            // if card is out of bounds, skip rendering
            if (cardY > startY + height || cardY + cardHeight < startY + 30) continue;

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
            
            int bgColor = isActive ? 0x661E3320 : (unlocked ? 0x66050810 : 0x44111111);
            int accentColor = isActive ? 0xFF55FF88 : (unlocked ? 0xFFFFAA00 : 0xFF555555);
            
            context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, bgColor);
            context.fill(cardX, cardY, cardX + 3, cardY + cardHeight, accentColor);
            
            // Glowing border
            context.fill(cardX, cardY, cardX + cardWidth, cardY + 1, 0x33FFFFFF);
            context.fill(cardX, cardY + cardHeight - 1, cardX + cardWidth, cardY + cardHeight, 0x33FFFFFF);
            context.fill(cardX + cardWidth - 1, cardY, cardX + cardWidth, cardY + cardHeight, 0x33FFFFFF);

            String formName = form.getId().getPath().replace("_", " ").toUpperCase();
            int titleColor = isActive ? 0xFF55FF88 : (unlocked ? 0xFFFFFFFF : 0xFF888888);
            context.text(client.font, Component.literal(formName), cardX + 15, cardY + 8, titleColor);

            if (!unlocked) {
                String reqStr = "Req: Lvl " + req.minLevel() + " + Stats";
                context.text(client.font, Component.literal(reqStr), cardX + 15, cardY + 24, 0xFFFF5555);
            } else {
                String masteryText = String.format("Mastery: %.1f%%", mastery);
                context.text(client.font, Component.literal(masteryText), cardX + 15, cardY + 24, 0xFFFFAA33);

                double actualDrain = form.getBaseKiDrain() * (1.0 - (mastery / 100.0 * form.getMaxMasteryReduction()));
                String drainText = String.format("Drain: %.1f/s", actualDrain);
                context.text(client.font, Component.literal(drainText), cardX + 105, cardY + 24, 0xFF55FFFF);
            }
            
            // Button Drawing
            int btnW = 75;
            int btnH = 20;
            int btnX = cardX + cardWidth - btnW - 10;
            int btnY = cardY + (cardHeight - btnH) / 2;
            boolean hoverBtn = (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
            
            if (isActive) {
                // Draw Revert Button
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hoverBtn ? 0xAAFF5555 : 0x55551111);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFFFF5555);
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFFFF5555);
                context.centeredText(client.font, Component.literal("REVERT"), btnX + btnW/2, btnY + 6, hoverBtn ? 0xFFFFFFFF : 0xFFFF5555);
            } else if (unlocked) {
                // Draw Transform Button
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, hoverBtn ? 0xAA55FF88 : 0x55113322);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFF55FF88);
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFF55FF88);
                context.centeredText(client.font, Component.literal("TRANSFORM"), btnX + btnW/2, btnY + 6, hoverBtn ? 0xFFFFFFFF : 0xFF55FF88);
            } else {
                // Locked Button
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x44111111);
                context.fill(btnX, btnY, btnX + btnW, btnY + 1, 0x44FFFFFF);
                context.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0x44FFFFFF);
                context.centeredText(client.font, Component.literal("LOCKED"), btnX + btnW/2, btnY + 6, 0xFF555555);
            }
        }
        
        context.disableScissor();
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getContentX();
        int startY = parent.getY();
        int width = parent.getContentWidth();
        int height = parent.getBgHeight();
        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseY < startY + 30 || mouseY > startY + height) return false;

        Identifier activeForm = accessor.dba$getActiveFormId();
        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());
            
        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int cardHeight = 46;
            int cardX = startX + 10;
            int cardY = startY + 35 + i * (cardHeight + 8) + (int)scrollY;
            int cardWidth = width - 20;
            
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

            int btnW = 75;
            int btnH = 20;
            int btnX = cardX + cardWidth - btnW - 10;
            int btnY = cardY + (cardHeight - btnH) / 2;
            
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
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
