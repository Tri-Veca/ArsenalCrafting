package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

/**
 * Immutable, null-safe container for 4 augment slots.
 * Empty slot is represented by the empty string ("") — never null.
 */
public final class ToolAugmentSlots {

    public static final Codec<ToolAugmentSlots> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.listOf().fieldOf("slots").forGetter(s -> Arrays.asList(s.slots))
                    ).apply(instance, list -> new ToolAugmentSlots(list.toArray(new String[4])))
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolAugmentSlots> STREAM_CODEC =
            StreamCodec.of(
                    (buf, slots) -> {
                        // Always write 4 UTF strings (empty string means empty slot)
                        for (int i = 0; i < 4; i++) {
                            String id = slots.slots[i];
                            buf.writeUtf(id == null ? "" : id);
                        }
                    },
                    buf -> {
                        String[] arr = new String[4];
                        for (int i = 0; i < 4; i++) {
                            String id = buf.readUtf();
                            arr[i] = id == null ? "" : id;
                        }
                        return new ToolAugmentSlots(arr);
                    }
            );

    private final String[] slots;

    public static final ToolAugmentSlots EMPTY = new ToolAugmentSlots();

    /** Default: four empty slots ("") */
    public ToolAugmentSlots() {
        this.slots = new String[] { "", "", "", "" };
    }

    private ToolAugmentSlots(String[] slots) {
        this.slots = new String[4];
        for (int i = 0; i < 4; i++) {
            this.slots[i] = (slots != null && i < slots.length && slots[i] != null) ? slots[i] : "";
        }
    }


    public String getAugmentInSlot(int slot) {
        if (slot < 0 || slot >= 4) return "";
        return slots[slot];
    }

    /**
     * Immutable setter — returns a new ToolAugmentSlots with the given slot set.
     * Passing null will clear the slot (becomes "").
     */
    public ToolAugmentSlots withSlot(int slot, String augmentId) {
        if (slot < 0 || slot >= 4) return this;
        String[] copy = Arrays.copyOf(this.slots, 4);
        copy[slot] = augmentId == null ? "" : augmentId;
        return new ToolAugmentSlots(copy);
    }

    public ToolAugmentSlots clearSlot(int slot) {
        return withSlot(slot, "");
    }

    public int getUsedSlotCount() {
        int c = 0;
        for (String id : slots) {
            if (id != null && !id.isEmpty()) c++;
        }
        return c;
    }

    public boolean hasAugment(String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        for (String id : slots) {
            if (augmentId.equals(id)) return true;
        }
        return false;
    }

    public int findSlotForAugment(String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return -1;
        for (int i = 0; i < 4; i++) {
            if (augmentId.equals(slots[i])) return i;
        }
        return -1;
    }

    /**
     * Migrate from old dynamic system into this safe container.
     */
    public static ToolAugmentSlots migrateFromOld(com.arsenalcrafting.mod.items.tool.ArsenalTool tool, net.minecraft.world.item.ItemStack stack) {
        ToolAugmentSlots newSlots = new ToolAugmentSlots();
        try {
            var allAugments = com.arsenalcrafting.mod.core.augments.AugmentManager.getAll();
            if (allAugments != null) {
                int nextSlot = 0;
                for (com.arsenalcrafting.mod.core.augments.AugmentManager.AugmentDefinition def : allAugments) {
                    if (def != null && com.arsenalcrafting.mod.core.augments.AugmentManager.isInstalled(def.id(), stack) && nextSlot < 4) {
                        newSlots = newSlots.withSlot(nextSlot++, def.id());
                        int tier = AugmentManager.getTier(def.id(), stack);
                        AugmentManager.install(def.id(), stack, tier);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ToolAugmentSlots] Failed to migrate old augments: " + e.getMessage());
        }
        return newSlots;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ToolAugmentSlots other = (ToolAugmentSlots) obj;
        return Arrays.equals(slots, other.slots);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(slots);
    }

    @Override
    public String toString() {
        return "ToolAugmentSlots{" + Arrays.toString(slots) + '}';
    }
}
