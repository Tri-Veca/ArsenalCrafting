package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncScrolledRowPacket(BlockPos pos, int scrolledRow) implements CustomPacketPayload {
    public static final Type<SyncScrolledRowPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "sync_scrolled_row"));

    public static final StreamCodec<FriendlyByteBuf, SyncScrolledRowPacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    (p, b) -> {
                        b.writeBlockPos(p.pos);
                        b.writeInt(p.scrolledRow);
                    },
                    b -> new SyncScrolledRowPacket(b.readBlockPos(), b.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(SyncScrolledRowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(packet.pos);
            if (be instanceof AlloySmelterBlockEntity smelter) {
                smelter.setScrolledRow(packet.scrolledRow);
            }
        });
    }
}