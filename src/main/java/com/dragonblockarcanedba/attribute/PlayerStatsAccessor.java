package com.dragonblockarcanedba.attribute;

import com.dragonblockarcanedba.ki.KiTechnique;
import net.minecraft.resources.Identifier;

public interface PlayerStatsAccessor {
    Identifier dba$getRaceId();
    void dba$setRaceId(Identifier id);

    boolean dba$hasSelectedRace();
    void dba$setHasSelectedRace(boolean selected);

    String dba$getSkinColor();
    void dba$setSkinColor(String color);

    String dba$getHairColor();
    void dba$setHairColor(String color);

    double dba$getCurrentKi();
    void dba$setCurrentKi(double ki);
    void dba$addKi(double amount);

    int dba$getLevel();
    void dba$setLevel(int level);
    void dba$addLevel(int levels);

    int dba$getXp();
    void dba$setXp(int xp);
    void dba$addXp(int amount);

    int dba$getStatPoints();
    void dba$setStatPoints(int points);

    int dba$getStrength();
    void dba$setStrength(int value);

    int dba$getDexterity();
    void dba$setDexterity(int value);

    int dba$getDefense();
    void dba$setDefense(int value);

    int dba$getWillpower();
    void dba$setWillpower(int value);
    
    int dba$getSpirit();
    void dba$setSpirit(int value);
    
    int dba$getVitality();
    void dba$setVitality(int value);
    
    int dba$getStatUpgradeCount(String stat);
    void dba$setStatUpgradeCount(String stat, int count);

    double dba$getCurrentStamina();
    void dba$setCurrentStamina(double stamina);
    void dba$addStamina(double amount);
    
    Identifier dba$getActiveFormId();
    void dba$setActiveFormId(Identifier formId);

    double dba$getFormMastery(Identifier formId);
    void dba$setFormMastery(Identifier formId, double mastery);
    void dba$addFormMastery(Identifier formId, double amount);

    boolean dba$hasTechnique(String technique);
    void dba$setTechniqueUnlocked(String technique, boolean unlocked);

    int dba$getTechniqueLevel(String technique);
    void dba$setTechniqueLevel(String technique, int level);

    boolean dba$isTechniqueActive(String technique);
    void dba$setTechniqueActive(String technique, boolean active);

    boolean dba$isSickleActive();

    String dba$getEquippedTechnique(int slot);
    void dba$setEquippedTechnique(int slot, String technique);
    
    void dba$syncStats();

    KiTechnique dba$getKiTechniqueSlot(int slot);
    void dba$setKiTechniqueSlot(int slot, KiTechnique tech);

    int dba$getSpeedPercent();
    void dba$setSpeedPercent(int percent);

    boolean dba$hasTail();
    boolean dba$isTailSevered();
    void dba$setTailSevered(boolean severed);

    void dba$pauseKiRecovery(int ticks);
    void dba$pauseStaminaRecovery(int ticks);

    String dba$getActiveEmote();
    void dba$setActiveEmote(String emote);

    void dba$copyFrom(PlayerStatsAccessor original);
}
