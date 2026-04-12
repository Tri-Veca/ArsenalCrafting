package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public enum ExtraMoldType {

    INGOT(
            () -> ModBlocks.INGOT_MOLD.get().asItem(),
            () -> ModBlocks.BRONZE_INGOT_MOLD.get().asItem(),
            "Ingot Mold"
    );
    // Future additions:
    // DIAMOND(() -> ModBlocks.DIAMOND_MOLD.get().asItem(), () -> ModBlocks.BRONZE_DIAMOND_MOLD.get().asItem(), "Diamond Mold"),

    private final Supplier<Item> standardMold;
    private final Supplier<Item> bronzeMold;
    private final String displayName;

    ExtraMoldType(Supplier<Item> standardMold, Supplier<Item> bronzeMold, String displayName) {
        this.standardMold = standardMold;
        this.bronzeMold = bronzeMold;
        this.displayName = displayName;
    }

    public Item getMold(boolean bronze) {
        return bronze ? bronzeMold.get() : standardMold.get();
    }

    public String getDisplayName() { return displayName; }
}