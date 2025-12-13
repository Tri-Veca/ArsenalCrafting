package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolKitRenderer implements BlockEntityRenderer<ToolkitBlockEntity> {

    public ToolKitRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(
            ToolkitBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        poseStack.pushPose();

        // 1️⃣ Position on the side of the block
        poseStack.translate(0.9, 0.25, 0.5);

        // 2️⃣ Rotate so the pickaxe is vertical (handle down)
        poseStack.mulPose(Axis.XP.rotationDegrees(0));


        // 3️⃣ Move it UP so the head is near the top
        poseStack.translate(0.0, 0.6, 0.0);

        // 4️⃣ Scale (optional, keep 1 for now)
        poseStack.scale(1.1F, 1.1F, 1.1F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.IRON_AXE),
                ItemDisplayContext.FIXED,
                light,
                overlay,
                poseStack,
                buffer,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }

}


