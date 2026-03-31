package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DumpFluidPacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DumpFluidPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "dump_fluid"));

    public static final StreamCodec<FriendlyByteBuf, DumpFluidPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DumpFluidPacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), DumpFluidPacket::materialName,
                    DumpFluidPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final DumpFluidPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be)) return;

            AlloyMaterial targetMat = AlloyMaterial.fromName(data.materialName);
            if (targetMat == null) return;

            // Find and drain all of this fluid from the output tank
            for (FluidStack fluid : be.getAllOutputFluids()) {
                AlloyMaterial fluidMat = AlloyMaterial.fromFluid(fluid.getFluid());
                if (fluidMat == targetMat) {
                    be.drainFromOutputTank(fluid.getFluid(), fluid.getAmount());
                    break;
                }
            }

            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        });
    }
}