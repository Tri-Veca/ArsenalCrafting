package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleMixingPacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleMixingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "toggle_mixing"));

    public static final StreamCodec<FriendlyByteBuf, ToggleMixingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ToggleMixingPacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), ToggleMixingPacket::materialName,
                    ToggleMixingPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ToggleMixingPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be) {
                if (be.mixingBlocked.contains(data.materialName)) {
                    be.mixingBlocked.remove(data.materialName);
                } else {
                    be.mixingBlocked.add(data.materialName);
                }
                be.setChanged();

                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
        });
    }
}