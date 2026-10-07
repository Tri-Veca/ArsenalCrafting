package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client sync after a repair: authoritative storedValue + material.
 * Lets the client bar match the server. Doesn't include the ingot count,
 * so the ingot slot can look stale until the menu is reopened.
 */
public record SyncRepairKitPreviewPacket(int storedValue, String material) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "sync_repair_kit_preview");

    public static final Type<SyncRepairKitPreviewPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncRepairKitPreviewPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SyncRepairKitPreviewPacket::storedValue,
                    ByteBufCodecs.STRING_UTF8,
                    SyncRepairKitPreviewPacket::material,
                    SyncRepairKitPreviewPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Apply on the client's open RepairKitMenu (main thread). */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof RepairKitMenu menu) {
                menu.setClientPreview(storedValue, material);
            }
        });
    }
}
