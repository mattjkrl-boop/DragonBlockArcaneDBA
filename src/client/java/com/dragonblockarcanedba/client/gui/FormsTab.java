package com.dragonblockarcanedba.client.gui;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.ActionPayload;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.stream.Collectors;

public class FormsTab implements MenuTab {
    private DbaMenuScreen parent;

    @Override
    public void init(DbaMenuScreen screen) {
        this.parent = screen;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        Identifier activeForm = accessor.dba$getActiveFormId();

        int startX = parent.getX();
        int startY = parent.getY();

        // Get compatible forms
        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());

        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int lineY = startY + 35 + i * 36;
            
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
            
            if (activeForm != null && activeForm.equals(form.getId())) {
                // Revert button
                parent.addTabWidget(Button.builder(Component.literal("Revert"), btn -> {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putString("action", "untransform");
                    ClientPlayNetworking.send(new ActionPayload(nbt));
                }).bounds(startX + 260, lineY - 2, 60, 20).build());
            } else if (activeForm == null) {
                // Transform button
                Button btn = Button.builder(Component.literal("Transform"), b -> {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putString("action", "transform");
                    nbt.putString("form", form.getId().toString());
                    ClientPlayNetworking.send(new ActionPayload(nbt));
                }).bounds(startX + 260, lineY - 2, 60, 20).build();
                btn.active = unlocked;
                parent.addTabWidget(btn);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) client.player;
        int startX = parent.getX();
        int startY = parent.getY();

        context.text(client.font, Component.literal("Transformations"), startX + 15, startY + 15, 0xFFFFAA00);

        List<Form> compatible = DbaRegistries.getForms().values().stream()
            .filter(f -> f.getCompatibleRaces().contains(accessor.dba$getRaceId()))
            .collect(Collectors.toList());

        if (compatible.isEmpty()) {
            context.text(client.font, Component.literal("No transformations compatible"), startX + 15, startY + 35, 0xFF888888);
            context.text(client.font, Component.literal("with your race."), startX + 15, startY + 47, 0xFF888888);
            return;
        }

        for (int i = 0; i < compatible.size(); i++) {
            Form form = compatible.get(i);
            int lineY = startY + 35 + i * 36;
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
            
            // Format form name nicely
            String formName = form.getId().getPath().replace("_", " ").toUpperCase();
            int titleColor = unlocked ? 0xFFFFFFFF : 0xFF888888;
            context.text(client.font, Component.literal(formName), startX + 15, lineY, titleColor);

            if (!unlocked) {
                String reqStr = "Req: Lvl " + form.getUnlockRequirements().minLevel() + " + Stats";
                context.text(client.font, Component.literal(reqStr), startX + 150, lineY, 0xFFFF5555);
            }

            // Mastery calculation
            String masteryText = String.format("Mastery: %.1f%%", mastery);
            context.text(client.font, Component.literal(masteryText), startX + 15, lineY + 10, 0xFFFFAA33);

            // Active drain scaling formula: Actual Ki Drain = Base Ki Drain * (1 - (Current Mastery / 100 * Max Mastery Reduction))
            double actualDrain = form.getBaseKiDrain() * (1.0 - (mastery / 100.0 * form.getMaxMasteryReduction()));
            String drainText = String.format("Drain: %.2f Ki/s", actualDrain);
            context.text(client.font, Component.literal(drainText), startX + 15, lineY + 20, 0xFF55FFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isRepeat) {
        return false;
    }
}
