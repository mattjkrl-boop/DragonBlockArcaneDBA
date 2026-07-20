package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.DbaNetwork;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements PlayerStatsAccessor {
    @Unique
    private Identifier dbaRaceId = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human");
    @Unique
    private String dbaSkinColor = "";
    @Unique
    private String dbaHairColor = "";
    @Unique
    private boolean dbaHasSelectedRace = false;
    @Unique
    private double dbaCurrentKi = 100.0;
    @Unique
    private double dbaCurrentStamina = 100.0;
    @Unique
    private int dbaLevel = 1;
    @Unique
    private int dbaXp = 0;
    @Unique
    private int dbaAp = 0;

    @Unique
    private final Map<String, Integer> dbaStatsMap = new HashMap<>();
    @Unique
    private Identifier dbaActiveFormId = null;
    
    @Unique
    private final Map<String, Double> dbaFormMasteryMap = new HashMap<>();
    @Unique
    private final Map<String, Integer> dbaFormMasteryXpMap = new HashMap<>();
    
    @Unique
    private final Map<String, Boolean> dbaUnlockedTechniques = new HashMap<>();
    @Unique
    private final Map<String, Boolean> dbaActiveTechniques = new HashMap<>();
    @Unique
    private final String[] dbaEquippedTechniques = new String[]{"", "", ""};

    // ==================== SAVE / LOAD (ValueOutput / ValueInput) ====================

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void dba$writeNbt(ValueOutput output, CallbackInfo ci) {
        ValueOutput dbaOut = output.child("dragonblockarcane");
        dbaOut.putString("raceId", dbaRaceId.toString());
        dbaOut.putString("skinColor", dbaSkinColor);
        dbaOut.putString("hairColor", dbaHairColor);
        dbaOut.putBoolean("hasSelectedRace", dbaHasSelectedRace);
        dbaOut.putDouble("currentKi", dbaCurrentKi);
        dbaOut.putDouble("currentStamina", dbaCurrentStamina);
        dbaOut.putInt("level", dbaLevel);
        dbaOut.putInt("xp", dbaXp);
        dbaOut.putInt("ap", dbaAp);

        ValueOutput statsOut = dbaOut.child("stats");
        dbaStatsMap.forEach(statsOut::putInt);

        if (dbaActiveFormId != null) {
            dbaOut.putString("activeFormId", dbaActiveFormId.toString());
        }

        ValueOutput masteryOut = dbaOut.child("mastery");
        dbaFormMasteryMap.forEach(masteryOut::putDouble);

        ValueOutput masteryXpOut = dbaOut.child("masteryXp");
        dbaFormMasteryXpMap.forEach(masteryXpOut::putInt);

        ValueOutput techUnlockedOut = dbaOut.child("unlockedTechniques");
        dbaUnlockedTechniques.forEach(techUnlockedOut::putBoolean);
        
        ValueOutput techActiveOut = dbaOut.child("activeTechniques");
        dbaActiveTechniques.forEach(techActiveOut::putBoolean);

        ValueOutput equipOut = dbaOut.child("equippedTechniques");
        for (int i = 0; i < 3; i++) {
            equipOut.putString("slot" + i, dbaEquippedTechniques[i]);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void dba$readNbt(ValueInput input, CallbackInfo ci) {
        ValueInput dbaIn = input.childOrEmpty("dragonblockarcane");

        dbaRaceId = Identifier.parse(dbaIn.getStringOr("raceId", "dragonblockarcanedba:human"));
        dbaSkinColor = dbaIn.getStringOr("skinColor", "");
        dbaHairColor = dbaIn.getStringOr("hairColor", "");
        dbaHasSelectedRace = dbaIn.getBooleanOr("hasSelectedRace", false);
        dbaCurrentKi = dbaIn.getDoubleOr("currentKi", 100.0);
        dbaCurrentStamina = dbaIn.getDoubleOr("currentStamina", 100.0);
        dbaLevel = dbaIn.getIntOr("level", 1);
        dbaXp = dbaIn.getIntOr("xp", 0);
        dbaAp = dbaIn.getIntOr("ap", 0);

        dbaStatsMap.clear();
        ValueInput statsIn = dbaIn.childOrEmpty("stats");
        for (String stat : new String[]{"strength", "dexterity", "defense", "willpower", "spirit", "vitality"}) {
            int val = statsIn.getIntOr(stat, 0);
            if (val != 0) {
                if (stat.equals("vitality")) {
                    dba$setVitality(val); // Trigger the setter to apply max health bonus
                } else {
                    dbaStatsMap.put(stat, val);
                }
            }
        }

        dbaIn.getString("activeFormId").ifPresentOrElse(
            s -> dbaActiveFormId = Identifier.parse(s),
            () -> dbaActiveFormId = null
        );

        dbaFormMasteryMap.clear();
        // Mastery data is read via childOrEmpty; individual keys must be checked
        ValueInput masteryIn = dbaIn.childOrEmpty("mastery");
        // We store mastery per-form, so iterate known forms
        for (Identifier formId : DbaRegistries.getAllFormIds()) {
            double val = masteryIn.getDoubleOr(formId.toString(), 0.0);
            if (val > 0.0) {
                dbaFormMasteryMap.put(formId.toString(), val);
            }
        }

        dbaFormMasteryXpMap.clear();
        ValueInput masteryXpIn = dbaIn.childOrEmpty("masteryXp");
        for (Identifier formId : DbaRegistries.getAllFormIds()) {
            int val = masteryXpIn.getIntOr(formId.toString(), 0);
            if (val > 0) {
                dbaFormMasteryXpMap.put(formId.toString(), val);
            }
        }
        
        dbaUnlockedTechniques.clear();
        ValueInput techUnlockedIn = dbaIn.childOrEmpty("unlockedTechniques");
        for (com.dragonblockarcanedba.registry.Technique tech : com.dragonblockarcanedba.registry.TechniqueRegistry.getAllTechniques()) {
            if (techUnlockedIn.getBooleanOr(tech.id(), false)) {
                dbaUnlockedTechniques.put(tech.id(), true);
            }
        }

        dbaActiveTechniques.clear();
        ValueInput techActiveIn = dbaIn.childOrEmpty("activeTechniques");
        for (com.dragonblockarcanedba.registry.Technique tech : com.dragonblockarcanedba.registry.TechniqueRegistry.getAllTechniques()) {
            if (techActiveIn.getBooleanOr(tech.id(), false)) {
                dbaActiveTechniques.put(tech.id(), true);
            }
        }

        ValueInput equipIn = dbaIn.childOrEmpty("equippedTechniques");
        for (int i = 0; i < 3; i++) {
            dbaEquippedTechniques[i] = equipIn.getStringOr("slot" + i, "");
        }
    }

    // ==================== TICK ====================

    @Inject(method = "tick", at = @At("HEAD"))
    private void dba$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }

        // Ki & Stamina recovery & drain mechanics
        double kiRecovery = PlayerStats.getKiRecovery(player);
        double kiChange = kiRecovery / 20.0;
        
        // Recover stamina if not sprinting (10 stamina/sec roughly), drain if sprinting (5 stamina/sec)
        if (player.isSprinting()) {
            dba$addStamina(-5.0 / 20.0);
            if (dbaCurrentStamina <= 0.0) {
                player.setSprinting(false);
            }
        } else {
            dba$addStamina(10.0 / 20.0);
        }

        Identifier formId = dba$getActiveFormId();
        if (formId != null) {
            Form form = DbaRegistries.getForm(formId);
            if (form != null) {
                double mastery = dba$getFormMastery(formId);
                double baseDrain = form.getBaseKiDrain();
                double maxReduction = form.getMaxMasteryReduction();
                double actualDrain = baseDrain * (1.0 - (mastery / 100.0 * maxReduction));

                // Mastery XP growth (1 XP per tick in form)
                dba$addFormMasteryXp(formId, 1);

                kiChange -= (actualDrain / 20.0);
            }
        }
        
        // Active Technique drains
        if (dba$isTechniqueActive("ki_sense")) {
            // Drain 1 Ki per second (0.05 per tick)
            kiChange -= (1.0 / 20.0);
        }

        if (kiChange != 0.0) {
            dba$addKi(kiChange);
        }

        // Revert transformation if Ki drops to 0
        if (dbaCurrentKi <= 0.0) {
            if (dbaActiveFormId != null) {
                dba$setActiveFormId(null);
                dba$syncStats();
            }
            if (dba$isTechniqueActive("ki_sense")) {
                dba$setTechniqueActive("ki_sense", false);
                dba$syncStats();
            }
        }

        // Periodic sync to client
        if (player.tickCount % 20 == 0) {
            dba$syncStats();
        }
    }

    // ==================== ACCESSOR IMPLEMENTATIONS ====================

    @Unique
    @Override
    public boolean dba$hasSelectedRace() {
        return dbaHasSelectedRace;
    }

    @Unique
    @Override
    public void dba$setHasSelectedRace(boolean selected) {
        this.dbaHasSelectedRace = selected;
    }

    @Unique
    @Override
    public String dba$getSkinColor() {
        return dbaSkinColor;
    }

    @Unique
    @Override
    public void dba$setSkinColor(String color) {
        this.dbaSkinColor = color;
    }

    @Unique
    @Override
    public String dba$getHairColor() {
        return dbaHairColor;
    }

    @Unique
    @Override
    public void dba$setHairColor(String color) {
        this.dbaHairColor = color;
    }

    @Unique
    @Override
    public Identifier dba$getRaceId() {
        return dbaRaceId;
    }

    @Unique
    @Override
    public void dba$setRaceId(Identifier id) {
        this.dbaRaceId = id;
    }

    @Unique
    @Override
    public double dba$getCurrentKi() {
        return dbaCurrentKi;
    }

    @Unique
    @Override
    public void dba$setCurrentKi(double ki) {
        double max = PlayerStats.getMaxKi((Player) (Object) this);
        this.dbaCurrentKi = Math.max(0.0, Math.min(max, ki));
    }

    @Unique
    @Override
    public void dba$addKi(double amount) {
        dba$setCurrentKi(dbaCurrentKi + amount);
    }

    @Unique
    @Override
    public int dba$getLevel() {
        return dbaLevel;
    }

    @Unique
    @Override
    public void dba$setLevel(int level) {
        this.dbaLevel = level;
    }

    @Unique
    @Override
    public int dba$getXp() {
        return dbaXp;
    }

    @Unique
    @Override
    public void dba$setXp(int xp) {
        this.dbaXp = xp;
    }

    @Unique
    @Override
    public void dba$addXp(int amount) {
        this.dbaXp += amount;
        int req = PlayerStats.getXpToNextLevel(dbaLevel);
        while (this.dbaXp >= req) {
            this.dbaXp -= req;
            dbaLevel++;
            dbaAp += 3; // Gain 3 attribute points on level up
            req = PlayerStats.getXpToNextLevel(dbaLevel);
        }
        dba$syncStats();
    }

    @Unique
    @Override
    public int dba$getStatPoints() {
        return dbaAp;
    }

    @Unique
    @Override
    public void dba$setStatPoints(int points) {
        this.dbaAp = points;
    }

    @Unique
    @Override
    public int dba$getStrength() {
        return dbaStatsMap.getOrDefault("strength", 0);
    }

    @Unique
    @Override
    public void dba$setStrength(int value) {
        dbaStatsMap.put("strength", Math.min(value, 5000));
    }

    @Unique
    @Override
    public int dba$getDexterity() {
        return dbaStatsMap.getOrDefault("dexterity", 0);
    }

    @Unique
    @Override
    public void dba$setDexterity(int value) {
        dbaStatsMap.put("dexterity", Math.min(value, 5000));
    }

    @Unique
    @Override
    public int dba$getDefense() {
        return dbaStatsMap.getOrDefault("defense", 0);
    }

    @Unique
    @Override
    public void dba$setDefense(int value) {
        dbaStatsMap.put("defense", Math.min(value, 5000));
    }

    @Unique
    @Override
    public int dba$getWillpower() {
        return dbaStatsMap.getOrDefault("willpower", 0);
    }

    @Unique
    @Override
    public void dba$setWillpower(int value) {
        dbaStatsMap.put("willpower", Math.min(value, 5000));
    }
    
    @Unique
    @Override
    public int dba$getSpirit() {
        return dbaStatsMap.getOrDefault("spirit", 0);
    }

    @Unique
    @Override
    public void dba$setSpirit(int value) {
        dbaStatsMap.put("spirit", Math.min(value, 5000));
    }

    @Unique
    @Override
    public int dba$getVitality() {
        return dbaStatsMap.getOrDefault("vitality", 0);
    }

    @Unique
    @Override
    public void dba$setVitality(int value) {
        value = Math.min(value, 5000);
        dbaStatsMap.put("vitality", value);
        // Sync Max Health when vitality changes
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance healthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                // Base health 20 + vitality scaling
                healthAttr.setBaseValue(20.0 + (value * 2.0)); 
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }
    
    @Unique
    @Override
    public double dba$getCurrentStamina() {
        return dbaCurrentStamina;
    }

    @Unique
    @Override
    public void dba$setCurrentStamina(double stamina) {
        double max = PlayerStats.getMaxStamina((Player) (Object) this);
        this.dbaCurrentStamina = Math.max(0.0, Math.min(max, stamina));
    }

    @Unique
    @Override
    public void dba$addStamina(double amount) {
        dba$setCurrentStamina(dbaCurrentStamina + amount);
    }

    @Unique
    @Override
    public Identifier dba$getActiveFormId() {
        return dbaActiveFormId;
    }

    @Unique
    @Override
    public void dba$setActiveFormId(Identifier formId) {
        this.dbaActiveFormId = formId;
    }

    @Unique
    @Override
    public double dba$getFormMastery(Identifier formId) {
        return dbaFormMasteryMap.getOrDefault(formId.toString(), 0.0);
    }

    @Unique
    @Override
    public void dba$setFormMastery(Identifier formId, double mastery) {
        dbaFormMasteryMap.put(formId.toString(), Math.max(0.0, Math.min(100.0, mastery)));
    }

    @Unique
    @Override
    public void dba$addFormMastery(Identifier formId, double amount) {
        dba$setFormMastery(formId, dba$getFormMastery(formId) + amount);
    }

    @Unique
    private void dba$addFormMasteryXp(Identifier formId, int amount) {
        String key = formId.toString();
        double currentMastery = dba$getFormMastery(formId);
        if (currentMastery >= 100.0) return;

        int xp = dbaFormMasteryXpMap.getOrDefault(key, 0) + amount;
        int req = PlayerStats.getFormMasteryXpToNextLevel((int) currentMastery);
        if (xp >= req) {
            xp -= req;
            dba$setFormMastery(formId, currentMastery + 1.0);
        }
        dbaFormMasteryXpMap.put(key, xp);
    }
    
    @Unique
    @Override
    public boolean dba$hasTechnique(String technique) {
        return dbaUnlockedTechniques.getOrDefault(technique, false);
    }

    @Unique
    @Override
    public void dba$setTechniqueUnlocked(String technique, boolean unlocked) {
        dbaUnlockedTechniques.put(technique, unlocked);
    }

    @Unique
    @Override
    public boolean dba$isTechniqueActive(String technique) {
        return dbaActiveTechniques.getOrDefault(technique, false);
    }

    @Unique
    @Override
    public void dba$setTechniqueActive(String technique, boolean active) {
        if (active && dbaCurrentKi <= 0) return; // Cannot activate if 0 Ki
        dbaActiveTechniques.put(technique, active);
    }

    @Unique
    @Override
    public String dba$getEquippedTechnique(int slot) {
        if (slot >= 0 && slot < 3) return dbaEquippedTechniques[slot];
        return "";
    }

    @Unique
    @Override
    public void dba$setEquippedTechnique(int slot, String technique) {
        if (slot >= 0 && slot < 3) {
            dbaEquippedTechniques[slot] = technique != null ? technique : "";
        }
    }

    // ==================== NETWORK SYNC ====================

    /**
     * Serializes DBA player data to a CompoundTag for network transmission.
     * This is independent of the save/load mixin methods which now use ValueOutput/ValueInput.
     */
    @Unique
    private CompoundTag dba$toSyncNbt() {
        CompoundTag dbaNbt = new CompoundTag();
        dbaNbt.putString("raceId", dbaRaceId.toString());
        dbaNbt.putString("skinColor", dbaSkinColor);
        dbaNbt.putString("hairColor", dbaHairColor);
        dbaNbt.putBoolean("hasSelectedRace", dbaHasSelectedRace);
        dbaNbt.putDouble("currentKi", dbaCurrentKi);
        dbaNbt.putDouble("currentStamina", dbaCurrentStamina);
        dbaNbt.putInt("level", dbaLevel);
        dbaNbt.putInt("xp", dbaXp);
        dbaNbt.putInt("ap", dbaAp);

        CompoundTag statsNbt = new CompoundTag();
        dbaStatsMap.forEach(statsNbt::putInt);
        dbaNbt.put("stats", statsNbt);

        if (dbaActiveFormId != null) {
            dbaNbt.putString("activeFormId", dbaActiveFormId.toString());
        }

        CompoundTag masteryNbt = new CompoundTag();
        dbaFormMasteryMap.forEach(masteryNbt::putDouble);
        dbaNbt.put("mastery", masteryNbt);

        CompoundTag masteryXpNbt = new CompoundTag();
        dbaFormMasteryXpMap.forEach(masteryXpNbt::putInt);
        dbaNbt.put("masteryXp", masteryXpNbt);
        
        CompoundTag techUnlockedNbt = new CompoundTag();
        dbaUnlockedTechniques.forEach(techUnlockedNbt::putBoolean);
        dbaNbt.put("unlockedTechniques", techUnlockedNbt);

        CompoundTag techActiveNbt = new CompoundTag();
        dbaActiveTechniques.forEach(techActiveNbt::putBoolean);
        dbaNbt.put("activeTechniques", techActiveNbt);

        CompoundTag equipNbt = new CompoundTag();
        for (int i = 0; i < 3; i++) {
            equipNbt.putString("slot" + i, dbaEquippedTechniques[i]);
        }
        dbaNbt.put("equippedTechniques", equipNbt);

        return dbaNbt;
    }

    @Unique
    @Override
    public void dba$syncStats() {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag syncData = dba$toSyncNbt();
            DbaNetwork.sendStatsSync(serverPlayer, syncData);
        }
    }
}
