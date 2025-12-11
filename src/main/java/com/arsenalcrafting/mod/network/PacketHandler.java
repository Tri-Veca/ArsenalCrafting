package com.arsenalcrafting.mod.network;

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

        // You can register more packets here as needed
    }
}