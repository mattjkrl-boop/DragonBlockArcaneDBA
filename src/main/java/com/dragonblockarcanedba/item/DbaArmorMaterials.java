package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public class DbaArmorMaterials {

    public static final ResourceKey<EquipmentAsset> BATTLE_ARMOR_ASSET_KEY = 
        ResourceKey.create(EquipmentAssets.ROOT_ID, DragonBlockArcaneDBA.id("battle_armor"));

    public static final ResourceKey<EquipmentAsset> PICCOLO_OUTFIT_ASSET_KEY = 
        ResourceKey.create(EquipmentAssets.ROOT_ID, DragonBlockArcaneDBA.id("piccolo_outfit"));

    public static final TagKey<Item> REPAIRS_BATTLE_ARMOR = 
        TagKey.create(net.minecraft.core.registries.Registries.ITEM, DragonBlockArcaneDBA.id("repairs_battle_armor"));
    
    public static final TagKey<Item> REPAIRS_PICCOLO_OUTFIT = 
        TagKey.create(net.minecraft.core.registries.Registries.ITEM, DragonBlockArcaneDBA.id("repairs_piccolo_outfit"));

    public static final ArmorMaterial BATTLE_ARMOR_MATERIAL = new ArmorMaterial(
            15, // durability multiplier
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 6
            ),
            9, // enchantability
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0F, // toughness
            0.0F, // knockback resistance
            REPAIRS_BATTLE_ARMOR,
            BATTLE_ARMOR_ASSET_KEY
    );

    public static final ArmorMaterial PICCOLO_OUTFIT_MATERIAL = new ArmorMaterial(
            12, // durability multiplier
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 4,
                    ArmorType.CHESTPLATE, 5,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 5
            ),
            12, // enchantability
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0F, // toughness
            0.0F, // knockback resistance
            REPAIRS_PICCOLO_OUTFIT,
            PICCOLO_OUTFIT_ASSET_KEY
    );

    public static void load() {}
}
