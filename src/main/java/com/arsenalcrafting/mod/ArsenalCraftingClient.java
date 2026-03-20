package com.arsenalcrafting.mod;

import com.arsenalcrafting.mod.client.render.ToolKitRenderer;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.client.render.LavaTankRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;
import java.util.stream.Collectors;

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

        for (AlloyMaterial material : AlloyMaterial.values()) {
            ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.ALLOY_FLUID_BLOCKS.get(material).get(),
                    RenderType.translucent()
            );
        }



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

