package com.arsenalcrafting.mod.client;

import com.arsenalcrafting.mod.items.tool.ArsenalBroadSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ArsenalClientInput {

    // How many ticks to keep blocking state alive after key release
    // keyUse.isDown() drops for 1-2 ticks while the game processes the click
    // so we hold the state for a few extra ticks to smooth it out
    private static final int BLOCKING_HOLD_TICKS = 5;
    private static int blockingCooldown = 0;

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;

        var player = mc.player;
        var mainhand = player.getMainHandItem();

        boolean holdingBroadsword = mainhand.getItem() instanceof ArsenalBroadSwordItem;
        boolean rightClickHeld = mc.options.keyUse.isDown();

        if (holdingBroadsword && rightClickHeld) {
            blockingCooldown = BLOCKING_HOLD_TICKS;
        } else if (blockingCooldown > 0) {
            blockingCooldown--;
        }

        boolean wasBlocking = ArsenalClientExtensions.isClientBlocking;
        ArsenalClientExtensions.isClientBlocking = holdingBroadsword && blockingCooldown > 0;

        if (ArsenalClientExtensions.isClientBlocking != wasBlocking) {
            System.out.println("[ArsenalClientInput] blocking state changed -> "
                    + ArsenalClientExtensions.isClientBlocking);
        }

        // Set arm pose directly on the model (bypasses isUsingItem gate)
        if (holdingBroadsword) {
            var entityRenderer = mc.getEntityRenderDispatcher().getRenderer(player);
            if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                var model = playerRenderer.getModel();
                if (ArsenalClientExtensions.isClientBlocking) {
                    model.rightArmPose = ArsenalArmPoses.BROADSWORD_BLOCK_POSE.getValue();
                } else {
                    model.rightArmPose = HumanoidModel.ArmPose.ITEM;
                }
            }
        }
    }
}