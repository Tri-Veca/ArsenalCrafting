package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.client.menu.CastingPackageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExtractMoldPacket(int slotIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ExtractMoldPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "extract_mold"));

    public static final StreamCodec<FriendlyByteBuf, ExtractMoldPacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeInt(packet.slotIndex()),
                    buf -> new ExtractMoldPacket(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ExtractMoldPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (player.containerMenu instanceof CastingPackageMenu menu) {
                ItemStack extracted = menu.extractMold(data.slotIndex());
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                }
            }
        });
    }
}