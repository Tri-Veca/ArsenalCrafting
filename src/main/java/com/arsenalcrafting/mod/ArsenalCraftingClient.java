package com.arsenalcrafting.mod;

import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.client.render.LavaTankRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ArsenalCrafting.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ArsenalCraftingClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            // Make the glass frame actually transparent
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.LAVA_TANK.get(),
                    RenderType.translucent()
            );

            // Register renderer
            BlockEntityRenderers.register(
                    ModBlockEntities.LAVA_TANK.get(),
                    LavaTankRenderer::new
            );
        });
    }
}
