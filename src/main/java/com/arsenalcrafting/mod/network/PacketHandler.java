package com.arsenalcrafting.mod.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
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
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        player.connection.send(packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }

    // You can register more packets here as needed
}