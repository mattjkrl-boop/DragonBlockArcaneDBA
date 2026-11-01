package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.item.geo.DbaWeaponTickets;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * GeoItemRenderer for DBA weapons. Two jobs the stock renderer cannot do:
 *
 * 1. Per-holder animation state. For each render it resolves the entity actually holding
 *    the stack (RenderData.itemOwner) and stamps DbaWeaponTickets with that holder's own
 *    swinging and using state. The animation predicate reads only those tickets, so a
 *    remote player's weapon swings and charges when that player does, on every screen.
 *    Renders with no living holder (GUI, ground, item frame) get neutral tickets.
 *
 * 2. Per-context presentation. GeckoLib ignores vanilla display transforms, so first
 *    person and third person corrections live here, keyed on ITEM_RENDER_PERSPECTIVE.
 *    First person renders slightly smaller and lower so a long blade stays in frame
 *    while swinging or charging (the visibility rule); third person renders slightly
 *    larger because the whole weapon is on show. Values are first-pass tuning in one
 *    place, to be refined per weapon from in-game screenshots.
 */
public class DbaWeaponRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public DbaWeaponRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void fireCompileRenderStateEvent(T animatable, RenderData renderData, GeoRenderState renderState, float partialTick) {
        super.fireCompileRenderStateEvent(animatable, renderData, renderState, partialTick);

        boolean swinging = false;
        float useRatio = -1f;
        ItemStack stack = renderData.itemStack();
        ItemOwner owner = renderData.itemOwner();
        LivingEntity holder = owner != null ? owner.asLivingEntity() : null;
        if (holder != null && stack != null) {
            boolean inAHand = holder.getMainHandItem().is(stack.getItem()) || holder.getOffhandItem().is(stack.getItem());
            swinging = com.dragonblockarcanedba.util.DbaCompat.isSwinging(holder) && inAHand;
            if (holder.isUsingItem() && holder.getUseItem().is(stack.getItem())) {
                int max = stack.getUseDuration(holder);
                if (max > 0) {
                    useRatio = Math.max(0f, Math.min(1f, 1f - holder.getUseItemRemainingTicks() / (float) max));
                }
            }
        }
        renderState.addGeckolibData(DbaWeaponTickets.HOLDER_SWINGING, swinging);
        renderState.addGeckolibData(DbaWeaponTickets.USE_RATIO, useRatio);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> info) {
        super.adjustRenderPose(info);
        ItemDisplayContext context = info.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            info.poseStack().translate(0f, -0.06f, 0f);
        }
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<GeoRenderState> info, float width, float height) {
        ItemDisplayContext context = info.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        float scale = switch (context) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> 0.9f;
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> 1.1f;
            default -> 1.0f;
        };
        super.scaleModelForRender(info, width * scale, height * scale);
    }
}
