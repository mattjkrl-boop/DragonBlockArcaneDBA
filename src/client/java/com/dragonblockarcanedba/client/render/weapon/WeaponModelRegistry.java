package com.dragonblockarcanedba.client.render.weapon;

import com.dragonblockarcanedba.client.render.weapon.model.BladePart;
import com.dragonblockarcanedba.client.render.weapon.model.BoxPart;
import com.dragonblockarcanedba.client.render.weapon.model.ConePart;
import com.dragonblockarcanedba.client.render.weapon.model.CylinderPart;
import com.dragonblockarcanedba.client.render.weapon.model.ModelPart;
import com.dragonblockarcanedba.client.render.weapon.model.RingPart;

import java.util.HashMap;
import java.util.Map;

public class WeaponModelRegistry {
    private static final Map<String, ModelPart> WEAPON_MODELS = new HashMap<>();

    static {
        registerDimensionalSword();
        registerPowerPole();
        registerDevilTrident();
        registerSickleOfSorrow();
        registerSpiritSword();
        registerBanshoFan();
        registerHollowsEdge();
        registerWhisStaff();
        registerZSword();
        registerCursedBlade();
    }

    public static ModelPart getModel(String name) {
        return WEAPON_MODELS.get(name);
    }

    private static void registerDimensionalSword() {
        ModelPart sword = new ModelPart();
        
        ModelPart handle = new CylinderPart(0.8f, 6.4f, 8).setColor(0xFF220044);
        handle.setPos(0, -3.2f, 0);

        ModelPart guard = new BoxPart(3.2f, 0.4f, 1.2f).setColor(0xFF000000);
        guard.setPos(0, 3.2f, 0);

        ModelPart blade = new BladePart(1.6f, 0.4f, 19.2f, 0.2f, 0.05f).setColor(0xFFAA00FF);
        blade.setPos(0, 3.6f, 0);

        ModelPart pommel = new ConePart(1.2f, 2.0f, 8).setColor(0xFF000000);
        pommel.setPos(0, -3.2f, 0);
        pommel.setRot((float)Math.PI, 0, 0); // Point down

        sword.addChild(handle);
        sword.addChild(guard);
        sword.addChild(blade);
        sword.addChild(pommel);

        WEAPON_MODELS.put("dimensional_sword", sword);
    }

    private static void registerPowerPole() {
        ModelPart poleRoot = new ModelPart();
        
        // Highly rounded cylinder for power pole
        ModelPart pole = new CylinderPart(0.8f, 28.8f, 12).setColor(0xFFDD0000);
        pole.setPos(0, -14.4f, 0);

        poleRoot.addChild(pole);
        WEAPON_MODELS.put("power_pole", poleRoot);
    }

    private static void registerDevilTrident() {
        ModelPart trident = new ModelPart();

        ModelPart shaft = new CylinderPart(0.8f, 24.0f, 8).setColor(0xFF333333);
        shaft.setPos(0, -12.0f, 0);

        // Center prong
        ModelPart centerProng = new ConePart(0.6f, 6.0f, 4).setColor(0xFFFF0000);
        centerProng.setPos(0, 12.0f, 0);

        // Left prong
        ModelPart leftProng = new ConePart(0.5f, 4.0f, 4).setColor(0xFFFF0000);
        leftProng.setPos(-2.4f, 12.0f, 0);
        leftProng.setRot(0, 0, 0.2f); // Angled out slightly

        // Right prong
        ModelPart rightProng = new ConePart(0.5f, 4.0f, 4).setColor(0xFFFF0000);
        rightProng.setPos(2.4f, 12.0f, 0);
        rightProng.setRot(0, 0, -0.2f); // Angled out slightly

        trident.addChild(shaft);
        trident.addChild(centerProng);
        trident.addChild(leftProng);
        trident.addChild(rightProng);

        WEAPON_MODELS.put("devil_trident", trident);
    }

    private static void registerSickleOfSorrow() {
        ModelPart sickle = new ModelPart();

        ModelPart pole = new CylinderPart(0.8f, 19.2f, 8).setColor(0xFF550055);
        pole.setPos(0, -9.6f, 0);

        ModelPart blade = new BladePart(1.2f, 0.4f, 12.0f, 0.1f, 0.05f).setColor(0xFFFF0055);
        blade.setPos(0, 9.6f, 0);
        blade.setRot(0, 0, (float)Math.PI / 2.5f); // Curved to the side like a scythe

        ModelPart ring = new RingPart(1.0f, 1.6f, 0.4f, 12).setColor(0xFFFF0055);
        ring.setPos(0, 9.6f, 0);
        ring.setRot((float)Math.PI / 2f, 0, 0);

        sickle.addChild(pole);
        sickle.addChild(blade);
        sickle.addChild(ring);

        WEAPON_MODELS.put("sickle_of_sorrow", sickle);
    }

    private static void registerSpiritSword() {
        ModelPart spiritSword = new ModelPart();

        ModelPart blade = new BladePart(1.2f, 1.2f, 22.4f, 0.0f, 0.0f).setColor(0xAA00FFFF);
        blade.setPos(0, -11.2f, 0); // Center around hand

        ModelPart energyRing = new RingPart(2.0f, 2.4f, 0.2f, 16).setColor(0xFFFFFFFF);
        energyRing.setPos(0, 0, 0); // Floating ring

        spiritSword.addChild(blade);
        spiritSword.addChild(energyRing);

        WEAPON_MODELS.put("spirit_sword", spiritSword);
    }

    private static void registerBanshoFan() {
        ModelPart fan = new ModelPart();

        ModelPart handle = new CylinderPart(0.6f, 4.0f, 8).setColor(0xFF442200);
        handle.setPos(0, -4.0f, 0);

        ModelPart leftLeaf = new BladePart(4.0f, 0.2f, 8.0f, 6.0f, 0.1f).setColor(0xFF228822);
        leftLeaf.setPos(0, 0, 0);
        leftLeaf.setRot(0, 0, 0.4f);

        ModelPart rightLeaf = new BladePart(4.0f, 0.2f, 8.0f, 6.0f, 0.1f).setColor(0xFF228822);
        rightLeaf.setPos(0, 0, 0);
        rightLeaf.setRot(0, 0, -0.4f);

        ModelPart centerLeaf = new BladePart(4.0f, 0.2f, 10.0f, 4.0f, 0.1f).setColor(0xFF116611);
        centerLeaf.setPos(0, 0, 0);

        fan.addChild(handle);
        fan.addChild(leftLeaf);
        fan.addChild(rightLeaf);
        fan.addChild(centerLeaf);

        WEAPON_MODELS.put("bansho_fan", fan);
    }

    private static void registerHollowsEdge() {
        ModelPart hollow = new ModelPart();

        ModelPart handle = new CylinderPart(0.6f, 4.0f, 8).setColor(0xFF222222);
        handle.setPos(0, -4.0f, 0);

        ModelPart darkBlade = new BladePart(2.4f, 0.4f, 16.0f, 0.8f, 0.1f).setColor(0xFF111111);
        darkBlade.setPos(0, 0, 0);

        ModelPart darkRing = new RingPart(1.6f, 3.2f, 0.2f, 12).setColor(0xFF000000);
        darkRing.setPos(0, 2.0f, 0);

        hollow.addChild(handle);
        hollow.addChild(darkBlade);
        hollow.addChild(darkRing);

        WEAPON_MODELS.put("hollows_edge", hollow);
    }

    private static void registerWhisStaff() {
        ModelPart staffRoot = new ModelPart();

        ModelPart staff = new CylinderPart(0.6f, 25.6f, 10).setColor(0xFF0055FF);
        staff.setPos(0, -12.8f, 0);

        ModelPart topOrb = new BoxPart(1.6f, 1.6f, 1.6f).setColor(0xFF000000);
        topOrb.setPos(0, 14.4f, 0);

        ModelPart floatingRing = new RingPart(2.4f, 3.2f, 0.4f, 16).setColor(0xFF00FFFF);
        floatingRing.setPos(0, 14.4f, 0);
        floatingRing.setRot((float)Math.PI / 4, 0, 0); // Angled halo

        staffRoot.addChild(staff);
        staffRoot.addChild(topOrb);
        staffRoot.addChild(floatingRing);

        WEAPON_MODELS.put("whis_staff", staffRoot);
    }

    private static void registerZSword() {
        ModelPart sword = new ModelPart();

        ModelPart handle = new CylinderPart(0.9f, 7.0f, 8).setColor(0xFF8B6508);
        handle.setPos(0, -3.5f, 0);

        ModelPart guard = new BoxPart(5.0f, 0.8f, 1.6f).setColor(0xFFFFD700);
        guard.setPos(0, 3.5f, 0);

        ModelPart blade = new BladePart(2.8f, 0.6f, 24.0f, 0.4f, 0.1f).setColor(0xFFE8E8E8);
        blade.setPos(0, 4.0f, 0);

        ModelPart divineFuller = new BoxPart(0.8f, 20.0f, 0.7f).setColor(0xFFFFD700);
        divineFuller.setPos(0, 14.0f, 0);

        ModelPart pommel = new ConePart(1.4f, 2.2f, 8).setColor(0xFFFFD700);
        pommel.setPos(0, -3.5f, 0);
        pommel.setRot((float) Math.PI, 0, 0);

        sword.addChild(handle);
        sword.addChild(guard);
        sword.addChild(blade);
        sword.addChild(divineFuller);
        sword.addChild(pommel);

        WEAPON_MODELS.put("z_sword", sword);
    }

    private static void registerCursedBlade() {
        ModelPart katana = new ModelPart();

        ModelPart handle = new CylinderPart(0.7f, 6.0f, 8).setColor(0xFF1A1A1A);
        handle.setPos(0, -3.0f, 0);

        ModelPart guard = new BoxPart(2.6f, 0.4f, 2.6f).setColor(0xFF2E0854);
        guard.setPos(0, 3.0f, 0);

        ModelPart blade = new BladePart(1.6f, 0.3f, 22.0f, 0.2f, 0.05f).setColor(0xFF110011);
        blade.setPos(0, 3.4f, 0);

        ModelPart bloodEdge = new BoxPart(0.2f, 21.0f, 0.4f).setColor(0xFF8B0000);
        bloodEdge.setPos(0.7f, 13.9f, 0);

        ModelPart cursedRing = new RingPart(1.0f, 1.8f, 0.2f, 10).setColor(0xFF800080);
        cursedRing.setPos(0, 3.2f, 0);
        cursedRing.setRot((float) Math.PI / 4f, 0, 0);

        katana.addChild(handle);
        katana.addChild(guard);
        katana.addChild(blade);
        katana.addChild(bloodEdge);
        katana.addChild(cursedRing);

        WEAPON_MODELS.put("cursed_blade", katana);
    }
}
