package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncAlloySmelterFluidPacket(BlockPos pos, FluidStack fluidStack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAlloySmelterFluidPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "sync_alloy_smelter_fluid"));

    // Use RegistryFriendlyByteBuf instead of FriendlyByteBuf
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAlloySmelterFluidPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncAlloySmelterFluidPacket::pos,
                    FluidStack.OPTIONAL_STREAM_CODEC, SyncAlloySmelterFluidPacket::fluidStack,
                    SyncAlloySmelterFluidPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncAlloySmelterFluidPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be) {
                be.DISPLAY_TANK.setFluid(data.fluidStack);
            }
        });
    }
}