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

public record SyncOutputTankCapacityPacket(BlockPos pos, int capacity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncOutputTankCapacityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "sync_output_tank_capacity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOutputTankCapacityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncOutputTankCapacityPacket::pos,
                    ByteBufCodecs.INT, SyncOutputTankCapacityPacket::capacity,
                    SyncOutputTankCapacityPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncOutputTankCapacityPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.getBlockEntity(data.pos) instanceof AlloySmelterBlockEntity be) {
                be.OUTPUT_TANK.setCapacity(data.capacity);
            }
        });
    }
}