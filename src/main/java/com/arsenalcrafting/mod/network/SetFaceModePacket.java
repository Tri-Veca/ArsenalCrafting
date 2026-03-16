package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client → server when a player clicks a face button on the power bank GUI.
 * Server applies the face mode cycle and saves the BE.
 */
public record SetFaceModePacket(BlockPos pos, Direction direction) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetFaceModePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "set_face_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetFaceModePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeBlockPos(pkt.pos());
                        buf.writeEnum(pkt.direction());
                    },
                    buf -> new SetFaceModePacket(buf.readBlockPos(), buf.readEnum(Direction.class))
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── SERVER HANDLER ────────────────────────────────────────────────────────

    public static void handle(SetFaceModePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;
            ServerLevel level = serverPlayer.serverLevel();

            if (!level.isLoaded(packet.pos())) return;

            var be = level.getBlockEntity(packet.pos());
            if (!(be instanceof PowerBankBlockEntity bank)) return;

            // Security: player must have the menu open for this block
            if (!bank.getBlockPos().closerToCenterThan(serverPlayer.position(), 8)) return;

            bank.cycleFaceMode(packet.direction());
            bank.setChanged();
        });
    }
}