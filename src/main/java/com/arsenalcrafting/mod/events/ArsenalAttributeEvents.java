package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.items.tool.ArsenalKatanaItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.GAME)
public class ArsenalAttributeEvents {

    private static final ResourceLocation KATANA_REACH_ID =
            ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "katana_reach");

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArsenalKatanaItem)) return;

        var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceLocation.withDefaultNamespace("player.entity_interaction_range"));

        attribute.ifPresent(attr -> event.addModifier(
                attr,
                new AttributeModifier(
                        KATANA_REACH_ID,
                        1.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND // This is where the slot filtering happens
        ));
    }
}