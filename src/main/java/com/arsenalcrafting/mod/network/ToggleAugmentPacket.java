package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.ToolMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleAugmentPacket(String augmentId) implements CustomPacketPayload {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "toggle_augment");

    public static final Type<ToggleAugmentPacket> TYPE = new Type<>(ID);

    // StreamCodec for serialization/deserialization
    public static final StreamCodec<FriendlyByteBuf, ToggleAugmentPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, // First field: augmentId
            ToggleAugmentPacket::augmentId,
            ToggleAugmentPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Handler method (called on server side)
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu instanceof ToolMenu toolMenu) {
                    toolMenu.toggleAugment(augmentId);
                }
            }
        });
    }
}