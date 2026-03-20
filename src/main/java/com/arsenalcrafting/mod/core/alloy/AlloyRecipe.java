package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.registry.ModRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AlloyRecipe implements Recipe<AlloyRecipeInput> {
    private final NonNullList<Ingredient> ingredients;

    // ── FLUID OUTPUT (new) ────────────────────────────────────────────────────
    private final String resultMaterial; // AlloyMaterial name e.g. "Bronze"
    private final int resultAmountMb;    // Output fluid in millibuckets

    // ── ITEM OUTPUT (old — kept but unused, comment back in if reverting) ─────
    // private final ItemStack result;

    private final int lavaCost;
    private final int craftTime;

    public AlloyRecipe(List<Ingredient> ingredients,
                       String resultMaterial, int resultAmountMb,
                       int lavaCost, int craftTime) {
        this.ingredients = NonNullList.create();
        this.ingredients.addAll(ingredients);
        this.resultMaterial = resultMaterial;
        this.resultAmountMb = resultAmountMb;
        this.lavaCost = lavaCost;
        this.craftTime = craftTime;
    }

    public int getLavaCost()        { return lavaCost; }
    public int getCraftTime()       { return craftTime; }
    public String getResultMaterial() { return resultMaterial; }
    public int getResultAmountMb()  { return resultAmountMb; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(AlloyRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        List<ItemStack> inputItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) inputItems.add(stack);
        }

        if (inputItems.size() != ingredients.size()) return false;

        List<Ingredient> remaining = new ArrayList<>(ingredients);
        for (ItemStack inputItem : inputItems) {
            boolean matched = false;
            for (int i = 0; i < remaining.size(); i++) {
                if (remaining.get(i).test(inputItem)) {
                    remaining.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return remaining.isEmpty();
    }

    // ── assemble / getResultItem kept for recipe system compatibility ─────────
    // Returns EMPTY since output is now fluid — override if item output needed
    @Override
    public ItemStack assemble(AlloyRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.ALLOY_SMELTING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AlloyRecipeType.ALLOY_SMELTING.get();
    }

    // ── SERIALIZER ────────────────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<AlloyRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.listOf().fieldOf("ingredients")
                                .forGetter(r -> r.ingredients.stream().toList()),
                        Codec.STRING.fieldOf("resultMaterial")
                                .forGetter(r -> r.resultMaterial),
                        Codec.INT.optionalFieldOf("resultAmountMb", 1000)
                                .forGetter(r -> r.resultAmountMb),
                        Codec.INT.optionalFieldOf("lavaCost", 1000)
                                .forGetter(r -> r.lavaCost),
                        Codec.INT.optionalFieldOf("craftTime", 200)
                                .forGetter(r -> r.craftTime)
                ).apply(instance, AlloyRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> {
                            buf.writeVarInt(recipe.ingredients.size());
                            for (Ingredient ingredient : recipe.ingredients) {
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                            }
                            buf.writeUtf(recipe.resultMaterial);
                            buf.writeVarInt(recipe.resultAmountMb);
                            buf.writeVarInt(recipe.lavaCost);
                            buf.writeVarInt(recipe.craftTime);
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            List<Ingredient> ingredients = new ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                            }
                            String resultMaterial = buf.readUtf();
                            int resultAmountMb    = buf.readVarInt();
                            int lavaCost          = buf.readVarInt();
                            int craftTime         = buf.readVarInt();
                            return new AlloyRecipe(ingredients, resultMaterial, resultAmountMb,
                                    lavaCost, craftTime);
                        }
                );

        @Override
        public MapCodec<AlloyRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}