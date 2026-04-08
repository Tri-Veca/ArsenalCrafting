package com.arsenalcrafting.mod.network;

import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.client.menu.BlueprintCastingTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectPackagePacket(String packageId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectPackagePacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "select_package"));

    public static final StreamCodec<FriendlyByteBuf, SelectPackagePacket> STREAM_CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeUtf(packet.packageId()),
                    buf -> new SelectPackagePacket(buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SelectPackagePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (player.containerMenu instanceof BlueprintCastingTableMenu menu) {
                PackageType packageType = PackageType.fromId(data.packageId());
                if (packageType != null) {
                    menu.setSelectedPackage(packageType);
                }
            }
        });
    }
}