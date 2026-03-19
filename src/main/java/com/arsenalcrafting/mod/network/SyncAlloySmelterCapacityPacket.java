package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncAlloySmelterCapacityPacket(BlockPos pos, int capacity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAlloySmelterCapacityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "sync_alloy_smelter_capacity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAlloySmelterCapacityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncAlloySmelterCapacityPacket::pos,
                    ByteBufCodecs.INT, SyncAlloySmelterCapacityPacket::capacity,
                    SyncAlloySmelterCapacityPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncAlloySmelterCapacityPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be) {
                be.DISPLAY_TANK.updateCapacity(data.capacity);
            }
        });
    }
}