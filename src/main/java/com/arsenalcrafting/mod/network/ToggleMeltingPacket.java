package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleMeltingPacket(BlockPos pos, String materialName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleMeltingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "toggle_melting"));

    public static final StreamCodec<FriendlyByteBuf, ToggleMeltingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ToggleMeltingPacket::pos,
                    StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf), ToggleMeltingPacket::materialName,
                    ToggleMeltingPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ToggleMeltingPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be)) return;
            if (be.meltingBlocked.contains(data.materialName)) {
                be.meltingBlocked.remove(data.materialName);
            } else {
                be.meltingBlocked.add(data.materialName);
            }
            be.setChanged();

            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        });
    }
}