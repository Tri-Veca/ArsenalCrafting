package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ForceRecipePacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ForceRecipePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "force_recipe"));

    public static final StreamCodec<FriendlyByteBuf, ForceRecipePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ForceRecipePacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), ForceRecipePacket::materialName,
                    ForceRecipePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ForceRecipePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be)) return;

            AlloyMaterial targetMat = AlloyMaterial.fromName(data.materialName);
            if (targetMat == null) return;

            // Find first slot containing an item that matches this material
            for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
                ItemStack slotItem = be.getItemHandler().getStackInSlot(slotIndex);
                if (slotItem.isEmpty()) continue;

                AlloyMaterial slotMat = AlloyMaterial.fromItem(slotItem.getItem());
                if (slotMat != targetMat) continue;

                // Look up recipe for this single item
                SimpleContainer singleInv = new SimpleContainer(9);
                singleInv.setItem(0, slotItem);

                var recipeOpt = context.player().level().getRecipeManager().getRecipeFor(
                        AlloyRecipeType.ALLOY_SMELTING.get(),
                        new AlloyRecipeInput(singleInv),
                        context.player().level());

                if (recipeOpt.isEmpty()) continue;

                var recipe = recipeOpt.get().value();

                // Respect capacity
                int resultMb = recipe.getResultAmountMb();
                if (be.OUTPUT_TANK.getTotalAmount() + resultMb > be.OUTPUT_TANK.getCapacity()) return;

                // Check lava
                if (be.getTotalLavaInTanks() < recipe.getLavaCost()) return;
                if (!be.consumeLavaFromTanks(recipe.getLavaCost())) return;

                // Consume item and produce fluid
                be.getItemHandler().extractItem(slotIndex, 1, false);
                var sourceFluid = ModFluids.getSource(targetMat);
                if (sourceFluid != null) {
                    be.OUTPUT_TANK.fill(
                            new FluidStack(sourceFluid, resultMb),
                            IFluidHandler.FluidAction.EXECUTE);
                }

                be.setChanged();
                return; // only process one slot per force
            }
        });
    }
}