package com.arsenalcrafting.mod.generator;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = "arsenalcrafting")
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        System.out.println("[DataGenerators] Starting data generation...");

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client-side data (lang, models, textures)
        generator.addProvider(event.includeClient(), new LangGenerator(packOutput));

        // You can add more providers here:
        // generator.addProvider(event.includeServer(), new ArsenalRecipeProvider(packOutput, lookupProvider));
        // generator.addProvider(event.includeClient(), new ArsenalBlockStateProvider(packOutput, existingFileHelper));
        // generator.addProvider(event.includeClient(), new ArsenalItemModelProvider(packOutput, existingFileHelper));

        System.out.println("[DataGenerators] Data generation setup complete");
    }
}