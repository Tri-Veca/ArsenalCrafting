package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.generator.AlloyRecipeDefinitions;
import com.arsenalcrafting.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ForceMixPacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ForceMixPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "force_mix"));

    public static final StreamCodec<FriendlyByteBuf, ForceMixPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ForceMixPacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), ForceMixPacket::materialName,
                    ForceMixPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ForceMixPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be)) {
                System.out.println("[ForceMix] No block entity at " + data.pos);
                return;
            }

            AlloyMaterial targetMat = AlloyMaterial.fromName(data.materialName);
            System.out.println("[ForceMix] Target: " + data.materialName + " resolved to: " + targetMat);
            if (targetMat == null) return;

            System.out.println("[ForceMix] Available fluids: ");
            for (FluidStack f : be.getAllOutputFluids()) {
                System.out.println("  - " + f.getAmount() + "mb " + f.getFluid().getFluidType().getDescription().getString());
            }

            System.out.println("[ForceMix] Checking " + AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.size() + " recipes");
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition matchingRecipe = null;
            for (AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe : AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.values()) {
                AlloyMaterial outputMat = AlloyMaterial.fromFluid(recipe.output().getFluid());
                System.out.println("[ForceMix] Recipe: " + recipe.name() + " outputMat: " + outputMat + " canCraft: " + recipe.canCraft(be.getAllOutputFluids()));
                if (outputMat == targetMat && recipe.canCraft(be.getAllOutputFluids())) {
                    matchingRecipe = recipe;
                    break;
                }
            }

            System.out.println("[ForceMix] Matching recipe: " + matchingRecipe);
            if (matchingRecipe == null) return;

            int resultMb   = matchingRecipe.output().getAmount();
            int consumedMb = matchingRecipe.getConsumedFluids().stream().mapToInt(FluidStack::getAmount).sum();
            System.out.println("[ForceMix] Tank: " + be.OUTPUT_TANK.getTotalAmount() + "/" + be.OUTPUT_TANK.getCapacity() + " resultMb: " + resultMb + " consumedMb: " + consumedMb);
            if (be.OUTPUT_TANK.getTotalAmount() - consumedMb + resultMb > be.OUTPUT_TANK.getCapacity()) {
                System.out.println("[ForceMix] FAILED capacity check");
                return;
            }

            for (FluidStack input : matchingRecipe.getConsumedFluids()) {
                be.drainFromOutputTank(input.getFluid(), input.getAmount());
            }
            be.addToOutputTank(matchingRecipe.output().copy());
            be.setChanged();
            System.out.println("[ForceMix] SUCCESS");
        });
    }
}