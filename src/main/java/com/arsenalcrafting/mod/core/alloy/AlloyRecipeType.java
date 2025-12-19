package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.ArsenalCrafting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AlloyRecipeType {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ArsenalCrafting.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyRecipe>> ALLOY_SMELTING =
            RECIPE_TYPES.register("alloy_smelting", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "alloy_smelting")));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}