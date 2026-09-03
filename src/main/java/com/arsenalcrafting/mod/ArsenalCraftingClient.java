package com.arsenalcrafting.mod;

import com.arsenalcrafting.mod.client.particle.RedCritParticle;
import com.arsenalcrafting.mod.client.render.*;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModParticles;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static com.arsenalcrafting.mod.items.tool.ArsenalTool.tickHeldToolAugments;

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

            BlockEntityRenderers.register(
                    ModBlockEntities.TOOLKIT.get(),
                    ToolKitRenderer::new
            );
        });

        BlockEntityRenderers.register(
                ModBlockEntities.CASTING_STATION.get(),
                CastingStationRenderer::new
        );
        BlockEntityRenderers.register(
                ModBlockEntities.FOUNDRY_BASIN.get(),
                FoundryBasinRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlockEntities.VALVE_FAUCET.get(),
                ValveFaucetRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlockEntities.ALLOY_SMELTER.get(),
                AlloySmelterRenderer::new
        );

        for (AlloyMaterial material : AlloyMaterial.values()) {
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.ALLOY_FLUID_BLOCKS.get(material).get(),
                    RenderType.translucent()
            );
        }

    }
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RED_CRIT.get(), RedCritParticle.Provider::new);
    }


    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, tickEvent -> {
            Player player = tickEvent.getEntity();

            if (!player.level().isClientSide()) return;
            //tick method for updating tick on ArsenalPickaxeItem for Light Augment
            tickHeldToolAugments(player, player.getMainHandItem());
            tickHeldToolAugments(player, player.getOffhandItem());
        });
    }
}

