package com.dragonblockarcanedba.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * Cross-version compatibility shims for the 26.2-and-up range.
 *
 * The mod compiles once against the 26.2 floor. A few vanilla members were renamed,
 * removed, or changed shape in 26.3 (see audits/MC-263-COMPAT-AUDIT.md). Where no
 * single source form compiles against both versions, these helpers resolve the
 * right member reflectively at runtime. Every helper tries the 26.2 form first and
 * falls back to the 26.3 form, so behavior on 26.2 is exactly the old direct call.
 */
public final class DbaCompat {
    private DbaCompat() {}

    // ---- Velocity sync marking: Entity.hurtMarked field (26.2) -> Entity.markHurt() (26.3) ----

    private static Field hurtMarkedField;
    private static Method markHurtMethod;
    private static boolean hurtResolved;

    private static void resolveHurt() {
        if (hurtResolved) return;
        hurtResolved = true;
        try {
            hurtMarkedField = Entity.class.getField("hurtMarked");
            return;
        } catch (NoSuchFieldException ignored) {
        }
        try {
            markHurtMethod = Entity.class.getDeclaredMethod("markHurt");
            markHurtMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
    }

    /** Flags the entity so the server syncs its velocity to clients this tick. */
    public static void markHurt(Entity entity) {
        if (entity == null) return;
        resolveHurt();
        try {
            if (hurtMarkedField != null) {
                hurtMarkedField.setBoolean(entity, true);
            } else if (markHurtMethod != null) {
                markHurtMethod.invoke(entity);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    // ---- Invulnerability timer: public field (26.2) -> get/setInvulnerableTime (26.3) ----

    private static Field invulnerableTimeField;
    private static Method invulnerableTimeGetter;
    private static Method invulnerableTimeSetter;
    private static boolean invulnerableResolved;

    private static void resolveInvulnerable() {
        if (invulnerableResolved) return;
        invulnerableResolved = true;
        try {
            invulnerableTimeField = Entity.class.getField("invulnerableTime");
            return;
        } catch (NoSuchFieldException ignored) {
        }
        try {
            invulnerableTimeGetter = Entity.class.getMethod("getInvulnerableTime");
            invulnerableTimeSetter = Entity.class.getMethod("setInvulnerableTime", int.class);
        } catch (NoSuchMethodException ignored) {
        }
    }

    public static int getInvulnerableTime(Entity entity) {
        resolveInvulnerable();
        try {
            if (invulnerableTimeField != null) return invulnerableTimeField.getInt(entity);
            if (invulnerableTimeGetter != null) return (int) invulnerableTimeGetter.invoke(entity);
        } catch (ReflectiveOperationException ignored) {
        }
        return 0;
    }

    public static void setInvulnerableTime(Entity entity, int ticks) {
        resolveInvulnerable();
        try {
            if (invulnerableTimeField != null) {
                invulnerableTimeField.setInt(entity, ticks);
            } else if (invulnerableTimeSetter != null) {
                invulnerableTimeSetter.invoke(entity, ticks);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    // ---- Arm swing: swing(Hand) (26.2) -> swing(Hand, SwingAnimation, boolean) (26.3) ----

    private static Method swingMethod;
    private static Object swingAnimationDefault;
    private static boolean swingResolved;

    private static void resolveSwing() {
        if (swingResolved) return;
        swingResolved = true;
        try {
            swingMethod = LivingEntity.class.getMethod("swing", InteractionHand.class);
            return;
        } catch (NoSuchMethodException ignored) {
        }
        try {
            Class<?> swingAnimation = Class.forName("net.minecraft.world.item.component.SwingAnimation");
            swingAnimationDefault = swingAnimation.getField("DEFAULT").get(null);
            swingMethod = LivingEntity.class.getMethod("swing", InteractionHand.class, swingAnimation, boolean.class);
        } catch (ReflectiveOperationException | ClassNotFoundException ignored) {
        }
    }

    /** Plays the arm swing exactly like the old one-argument LivingEntity.swing(hand). */
    public static void swing(LivingEntity entity, InteractionHand hand) {
        if (entity == null) return;
        resolveSwing();
        if (swingMethod == null) return;
        try {
            if (swingAnimationDefault == null) {
                swingMethod.invoke(entity, hand);
            } else {
                swingMethod.invoke(entity, hand, swingAnimationDefault, true);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    // ---- Swing progress: getAttackAnim(float) (26.2) -> getSwingAnimation(float) (26.3) ----

    private static Method attackAnimMethod;
    private static boolean attackAnimResolved;

    public static float attackAnim(LivingEntity entity, float partialTick) {
        if (!attackAnimResolved) {
            attackAnimResolved = true;
            try {
                attackAnimMethod = entity.getClass().getMethod("getAttackAnim", float.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    attackAnimMethod = entity.getClass().getMethod("getSwingAnimation", float.class);
                } catch (NoSuchMethodException ignoredAgain) {
                }
            }
        }
        if (attackAnimMethod == null) return 0f;
        try {
            return (float) attackAnimMethod.invoke(entity, partialTick);
        } catch (ReflectiveOperationException ignored) {
            return 0f;
        }
    }

    // ---- Render-state swing progress: attackTime field (26.2) -> swingAnimation field (26.3) ----

    private static Field swingProgressField;
    private static boolean swingProgressResolved;

    public static float swingProgress(Object renderState) {
        if (renderState == null) return 0f;
        if (!swingProgressResolved) {
            swingProgressResolved = true;
            Class<?> type = renderState.getClass();
            try {
                swingProgressField = type.getField("attackTime");
            } catch (NoSuchFieldException ignored) {
                try {
                    swingProgressField = type.getField("swingAnimation");
                } catch (NoSuchFieldException ignoredAgain) {
                }
            }
        }
        if (swingProgressField == null) return 0f;
        try {
            return swingProgressField.getFloat(renderState);
        } catch (ReflectiveOperationException ignored) {
            return 0f;
        }
    }

    // ---- Swing state: LivingEntity.swinging field (26.2) -> isSwinging() (26.3) ----

    private static Field swingingField;
    private static Method isSwingingMethod;
    private static boolean swingingResolved;

    public static boolean isSwinging(LivingEntity entity) {
        if (entity == null) return false;
        if (!swingingResolved) {
            swingingResolved = true;
            try {
                swingingField = LivingEntity.class.getField("swinging");
            } catch (NoSuchFieldException ignored) {
                try {
                    isSwingingMethod = LivingEntity.class.getMethod("isSwinging");
                } catch (NoSuchMethodException ignoredAgain) {
                }
            }
        }
        try {
            if (swingingField != null) return swingingField.getBoolean(entity);
            if (isSwingingMethod != null) return (boolean) isSwingingMethod.invoke(entity);
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    // ---- Block codec: BlockBehaviour.simpleCodec (26.2) -> removed in 26.3 ----

    /**
     * Builds a block MapCodec. On 26.2 this delegates to vanilla simpleCodec, identical
     * to the old direct call. On 26.3 vanilla no longer uses block codecs at all, so a
     * unit codec that decodes to a fresh instance is sufficient.
     */
    public static <B extends Block> MapCodec<B> blockCodec(Function<BlockBehaviour.Properties, B> factory) {
        try {
            Method simpleCodec = BlockBehaviour.class.getMethod("simpleCodec", Function.class);
            @SuppressWarnings("unchecked")
            MapCodec<B> codec = (MapCodec<B>) simpleCodec.invoke(null, factory);
            return codec;
        } catch (ReflectiveOperationException ignored) {
            return MapCodec.unit(() -> factory.apply(BlockBehaviour.Properties.of()));
        }
    }

    // ---- Properties.isViewBlocking: predicate type changed in 26.3 ----

    /**
     * Forces the isViewBlocking property to always-false, as the old lambda did.
     * The predicate interface differs between versions (StatePredicate in 26.2,
     * StateArgumentPredicate in 26.3), so a proxy of whichever type the runtime
     * Properties class declares is installed into the field.
     */
    public static BlockBehaviour.Properties notViewBlocking(BlockBehaviour.Properties properties) {
        try {
            Field field = BlockBehaviour.Properties.class.getDeclaredField("isViewBlocking");
            field.setAccessible(true);
            Class<?> predicateType = field.getType();
            InvocationHandler alwaysFalse = (proxy, method, args) -> {
                if (method.getName().equals("test")) return false;
                if (method.getName().equals("toString")) return "DbaCompat.notViewBlocking";
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return null;
            };
            Object predicate = Proxy.newProxyInstance(
                DbaCompat.class.getClassLoader(), new Class<?>[]{predicateType}, alwaysFalse);
            field.set(properties, predicate);
        } catch (ReflectiveOperationException ignored) {
        }
        return properties;
    }
}
