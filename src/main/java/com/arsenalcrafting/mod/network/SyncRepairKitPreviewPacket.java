package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRepairKitPreviewPacket(int previewValue, String material) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "sync_repair_kit_preview");

    public static final Type<SyncRepairKitPreviewPacket> TYPE = new Type<>(ID);

    // StreamCodec for serialization/deserialization
    public static final StreamCodec<FriendlyByteBuf, SyncRepairKitPreviewPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, // previewValue
                    SyncRepairKitPreviewPacket::previewValue,
                    ByteBufCodecs.STRING_UTF8, // material
                    SyncRepairKitPreviewPacket::material,
                    SyncRepairKitPreviewPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Handler method (called on client side)
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof RepairKitMenu menu) {
                menu.setClientPreview(previewValue, material);
            }
        });
    }
}