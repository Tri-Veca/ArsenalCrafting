package com.arsenalcrafting.mod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0"); // Version

        // Register ToggleAugmentPacket
        registrar.playToServer(
                ToggleAugmentPacket.TYPE,
                ToggleAugmentPacket.STREAM_CODEC,
                ToggleAugmentPacket::handle
        );

        registrar.playToClient(
                SyncRepairKitPreviewPacket.TYPE,
                SyncRepairKitPreviewPacket.STREAM_CODEC,
                SyncRepairKitPreviewPacket::handle
        );

        // Power bank face mode
        registrar.playToServer(
                SetFaceModePacket.TYPE,
                SetFaceModePacket.STREAM_CODEC,
                SetFaceModePacket::handle
        );

        // Register Fluid Sync Packet
        registrar.playToClient(
                SyncAlloySmelterFluidPacket.TYPE,
                SyncAlloySmelterFluidPacket.STREAM_CODEC,
                SyncAlloySmelterFluidPacket::handle
        );

        registrar.playToClient(
                SyncAlloySmelterCapacityPacket.TYPE,
                SyncAlloySmelterCapacityPacket.STREAM_CODEC,
                SyncAlloySmelterCapacityPacket::handle
        );

        registrar.playToServer(
                ToggleMixingPacket.TYPE,
                ToggleMixingPacket.STREAM_CODEC,
                ToggleMixingPacket::handle
        );

        registrar.playToServer(
                ForceMixPacket.TYPE,
                ForceMixPacket.STREAM_CODEC,
                ForceMixPacket::handle
        );

        registrar.playToServer(
                ToggleMeltingPacket.TYPE,
                ToggleMeltingPacket.STREAM_CODEC,
                ToggleMeltingPacket::handle
        );

        registrar.playToServer(
                ForceRecipePacket.TYPE,
                ForceRecipePacket.STREAM_CODEC,
                ForceRecipePacket::handle
        );

        registrar.playToServer(
                DumpFluidPacket.TYPE,
                DumpFluidPacket.STREAM_CODEC,
                DumpFluidPacket::handle
        );

        registrar.playToServer(
                SetCastTargetPacket.TYPE,
                SetCastTargetPacket.STREAM_CODEC,
                SetCastTargetPacket::handle
        );

        registrar.playToServer(
                ExtractMoldPacket.TYPE,
                ExtractMoldPacket.STREAM_CODEC,
                ExtractMoldPacket::handle
        );


        registrar.playToServer(
               SelectPackagePacket.TYPE,
                SelectPackagePacket.STREAM_CODEC,
                SelectPackagePacket::handle
        );

        registrar.playToServer(
                SyncScrolledRowPacket.TYPE,
                SyncScrolledRowPacket.STREAM_CODEC,
                SyncScrolledRowPacket::handle
        );
        registrar.playToClient(
                SyncOutputTankCapacityPacket.TYPE,
                SyncOutputTankCapacityPacket.STREAM_CODEC,
                SyncOutputTankCapacityPacket::handle
        );


    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        player.connection.send(packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    public static void sendToAllTracking(CustomPacketPayload packet, Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    serverLevel.getChunkAt(pos).getPos(),
                    packet
            );
        }
    }

    // You can register more packets here as needed
}