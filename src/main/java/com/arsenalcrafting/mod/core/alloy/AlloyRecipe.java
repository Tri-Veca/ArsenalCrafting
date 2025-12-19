package com.arsenalcrafting.mod.core.alloy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AlloyRecipe implements Recipe<AlloyRecipeInput> {
    private final List<Ingredient> ingredients;
    private final ItemStack result;

    public AlloyRecipe(List<Ingredient> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public boolean matches(AlloyRecipeInput input, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        // Create a list of items in the input grid (first 9 slots)
        List<ItemStack> inputItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                inputItems.add(stack);
            }
        }

        // Check if we have the right number of ingredients
        if (inputItems.size() != ingredients.size()) {
            return false;
        }

        // Create a copy of ingredients to check against
        List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);

        // Try to match each input item with an ingredient
        for (ItemStack inputItem : inputItems) {
            boolean matched = false;
            for (int i = 0; i < remainingIngredients.size(); i++) {
                if (remainingIngredients.get(i).test(inputItem)) {
                    remainingIngredients.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        return remainingIngredients.isEmpty();
    }

    @Override
    public ItemStack assemble(AlloyRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AlloyRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return AlloyRecipeType.ALLOY_SMELTING.get();
    }

    public static class AlloyRecipeSerializer implements RecipeSerializer<AlloyRecipe> {
        public static final AlloyRecipeSerializer INSTANCE = new AlloyRecipeSerializer();

        private static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
                ).apply(instance, AlloyRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public AlloyRecipe decode(RegistryFriendlyByteBuf buf) {
                        int size = buf.readVarInt();
                        List<Ingredient> ingredients = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                        }
                        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                        return new AlloyRecipe(ingredients, result);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, AlloyRecipe recipe) {
                        buf.writeVarInt(recipe.ingredients.size());
                        for (Ingredient ingredient : recipe.ingredients) {
                            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                        }
                        ItemStack.STREAM_CODEC.encode(buf, recipe.result);
                    }
                };

        @Override
        public MapCodec<AlloyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}