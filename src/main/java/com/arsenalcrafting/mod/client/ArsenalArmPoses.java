package com.arsenalcrafting.mod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

@OnlyIn(Dist.CLIENT)
public class ArsenalArmPoses {

    public static final EnumProxy<HumanoidModel.ArmPose> BROADSWORD_BLOCK_POSE =
            new EnumProxy<>(
                    HumanoidModel.ArmPose.class,
                    false,
                    (IArmPoseTransformer) ArsenalArmPoses::applyBroadswordBlock
            );

    private static void applyBroadswordBlock(
            HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {

        System.out.println("[ArsenalArmPoses] applyBroadswordBlock called for arm=" + arm);

        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = model.rightArm.xRot * 0.5F - 0.9424779F;
            model.rightArm.yRot = -30.0F * ((float) Math.PI / 180F);
        } else {
            model.leftArm.xRot = model.leftArm.xRot * 0.5F - 0.9424779F;
            model.leftArm.yRot = 30.0F * ((float) Math.PI / 180F);
        }
    }
}