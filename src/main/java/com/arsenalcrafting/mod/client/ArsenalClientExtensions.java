package com.arsenalcrafting.mod.client;

import com.arsenalcrafting.mod.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ArsenalClientExtensions {

    public static boolean isClientBlocking = false;

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {

            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return null;
            }

            @Override
            public boolean applyForgeHandTransform(
                    PoseStack poseStack,
                    LocalPlayer player,
                    HumanoidArm arm,
                    ItemStack itemInHand,
                    float partialTick,
                    float equipProcess,
                    float swingProcess) {

                if (!isClientBlocking) return false;
                if (arm != player.getMainArm()) return false;

                int dir = arm == HumanoidArm.RIGHT ? 1 : -1;

                poseStack.translate(dir * 0.65F, -0.63F, -1.0F);
                poseStack.translate(0.0F, equipProcess * -0.6F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 35F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10F));

                float swing = (float) Math.sin(Math.sqrt(swingProcess) * Math.PI);
                poseStack.mulPose(Axis.XP.rotationDegrees(swing * -20F));

                // Move down more to hide hilt, nudge right slightly
                poseStack.translate(dir * -0.42F, 0.00F, 0.3F);
                poseStack.translate(dir * 0.3F, 0.0F, 0.0F);

                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 35F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-70F));
                poseStack.mulPose(Axis.YP.rotationDegrees(dir * 10F));

                return true;
            }

        }, ModItems.ARSENAL_BROADSWORD.get());
    }
}