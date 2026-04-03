package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetCastTargetPacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetCastTargetPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "set_cast_target"));

    public static final StreamCodec<FriendlyByteBuf, SetCastTargetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetCastTargetPacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), SetCastTargetPacket::materialName,
                    SetCastTargetPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SetCastTargetPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be) {
                // Set the cast target (null or empty string means clear)
                if (data.materialName == null || data.materialName.isEmpty()) {
                    be.castTargetFluid = null;
                } else {
                    be.castTargetFluid = data.materialName;
                }
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        });
    }
}