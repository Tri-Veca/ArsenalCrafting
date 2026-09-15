package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class CoreManager {

    private static final Map<AlloyMaterial, CoreDefinition> DEFINITIONS = new EnumMap<>(AlloyMaterial.class);

    /**
     * Core definition with separate tool/weapon descriptions.
     *
     * - toolDescription:   shown on pickaxe, shovel, axe
     * - weaponDescription: shown on katana, broadsword, dagger, chakram
     *
     * For cores where the effect is the same on both (e.g. Burn Protection, Sturdy),
     * pass the same string to both fields.
     *
     * Steel is registered multiple times via registerSteelVariant() —
     * one entry per ArsenalToolType — each with its own id and descriptions.
     */
    public record CoreDefinition(
            String id,
            String name,
            String toolDescription,
            String weaponDescription,
            CoreHandler handler
    ) {
        /** Convenience — returns the correct description for this tool's type. */
        public String descriptionFor(boolean isWeapon) {
            return isWeapon ? weaponDescription : toolDescription;
        }
    }

    // Steel has one definition per tool type
    private static final Map<ArsenalToolType, CoreDefinition> STEEL_VARIANTS = new EnumMap<>(ArsenalToolType.class);


    static {
        // ── STANDARD CORES ────────────────────────────────────────────────────
        // Cores where tool vs weapon just changes the description text.

        register(AlloyMaterial.COPPER, new CoreDefinition(
                "speed_boost", "Durability Speed",
                "Higher mining speed when durability is low",
                "Higher attack speed when durability is low",
                CoreHandlers.SPEED_BOOST
        ));

        register(AlloyMaterial.IRON, new CoreDefinition(
                "repair_reduction", "Repair Reduction",
                "Decreases the repair cost slightly",
                "Decreases the repair cost slightly",
                CoreHandlers.REPAIR_REDUCTION
        ));

        register(AlloyMaterial.GOLD, new CoreDefinition(
                "greedy", "Greedy",
                "+2 Efficiency, -15% Durability",
                "+15% Attack Speed, -15% Durability",
                CoreHandlers.GREEDY
        ));

        register(AlloyMaterial.TIN, new CoreDefinition(
                "momentum", "Momentum",
                "Consecutive mining grants increasing Haste levels",
                "Consecutive hits grant increasing attack speed",
                CoreHandlers.MOMENTUM
        ));

        register(AlloyMaterial.NETHERITE, new CoreDefinition(
                "burn_protection", "Burn Protection",
                "Grants item burn protection",
                "Grants item burn protection",
                CoreHandlers.BURN_PROTECTION
        ));

        register(AlloyMaterial.BRONZE, new CoreDefinition(
                "sturdy", "Sturdy",
                "40% chance to not use durability",
                "40% chance to not use durability",
                CoreHandlers.STURDY
        ));

        register(AlloyMaterial.DIAMOND, new CoreDefinition(
                "gambler", "Gambler",
                "7% chance for bonus loot when mining",
                "7% chance for bonus loot when killing",
                CoreHandlers.GAMBLER
        ));

        register(AlloyMaterial.TITANIUM, new CoreDefinition(
                "perfect_strike", "Perfect Strike",
                "8% chance to Instantly breaks blocks, +1 ore drop",
                "8% chance to deal an extra hit ",
                CoreHandlers.PERFECT_STRIKE
        ));

        // ── STEEL — RULE BREAKER ──────────────────────────────────────────────
        // Steel breaks the rules. Each tool type gets a completely unique power.
        // The base DEFINITIONS entry is a fallback — real lookup uses STEEL_VARIANTS.

        register(AlloyMaterial.STEEL, new CoreDefinition(
                "workhorse", "Workhorse",
                "Instantly breaks hard blocks regardless of tool tier",
                "7% chance to apply slowness",
                CoreHandlers.WORKHORSE
        ));

        // Per-tool Steel variants — used when tool type is known
        registerSteelVariant(ArsenalToolType.PICKAXE, new CoreDefinition(
                "steel_pickaxe", "Workhorse",
                "Instantly breaks hard blocks regardless of tool tier",
                "Instantly breaks hard blocks regardless of tool tier",
                CoreHandlers.WORKHORSE
        ));
        registerSteelVariant(ArsenalToolType.SHOVEL, new CoreDefinition(
                "steel_shovel", "WorkHorse",
                "Replaces path making with hoe ability",
                "Replaces path making with hoe ability",
                CoreHandlers.WORKHORSE // placeholder until implemented
        ));
        registerSteelVariant(ArsenalToolType.AXE, new CoreDefinition(
                "steel_axe", "WorkHorse",
                "Allows you to sheer",
                "Allows you to sheer",
                CoreHandlers.WORKHORSE // placeholder until implemented
        ));
        registerSteelVariant(ArsenalToolType.KATANA, new CoreDefinition(
                "steel_katana", "WorkHorse",
                "7% chance to apply slowness",
                "7% chance to apply slowness",
                CoreHandlers.WORKHORSE // placeholder until implemented
        ));
        registerSteelVariant(ArsenalToolType.BROADSWORD, new CoreDefinition(
                "steel_broadsword", "WorkHorse",
                "7% chance to apply slowness",
                "7% chance to apply slowness",
                CoreHandlers.WORKHORSE // placeholder until implemented
        ));
        registerSteelVariant(ArsenalToolType.DAGGER, new CoreDefinition(
                "steel_dagger", "WorkHorse",
                "7% chance to apply slowness",
                "7% chance to apply slowness",
                CoreHandlers.WORKHORSE // placeholder until implemented
        ));
    }

    // ── REGISTRATION ──────────────────────────────────────────────────────────

    private static void register(AlloyMaterial material, CoreDefinition def) {
        DEFINITIONS.put(material, def);
    }

    private static void registerSteelVariant(ArsenalToolType toolType, CoreDefinition def) {
        STEEL_VARIANTS.put(toolType, def);
    }

    // ── LOOKUP ────────────────────────────────────────────────────────────────

    @Nullable
    public static CoreDefinition getForMaterial(AlloyMaterial material) {
        return DEFINITIONS.get(material);
    }

    /**
     * Primary lookup for tooltip and effect resolution.
     * Pass toolType so Steel returns the correct per-tool variant.
     * For non-Steel materials toolType is ignored.
     */
    @Nullable
    public static CoreDefinition getForMaterial(AlloyMaterial material, @Nullable ArsenalToolType toolType) {
        if (material == AlloyMaterial.STEEL && toolType != null) {
            CoreDefinition variant = STEEL_VARIANTS.get(toolType);
            if (variant != null) return variant;
        }
        return DEFINITIONS.get(material);
    }

    @Nullable
    public static CoreDefinition getForMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return null;
        AlloyMaterial material = AlloyMaterial.fromName(materialName);
        return material != null ? DEFINITIONS.get(material) : null;
    }

    @Nullable
    public static CoreDefinition getFromTool(ItemStack tool) {
        ToolAssemblyData assemblyData = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (assemblyData == null) return null;
        return getForMaterialName(assemblyData.coreMaterial());
    }

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    public static Collection<CoreDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());

    }
    public static Collection<CoreDefinition> getAllSteelVariants() {
        return Collections.unmodifiableCollection(STEEL_VARIANTS.values());
    }

    @Nullable
    public static CoreDefinition get(String id) {
        for (CoreDefinition def : DEFINITIONS.values()) {
            if (def.id().equals(id)) return def;
        }
        // Also search steel variants
        for (CoreDefinition def : STEEL_VARIANTS.values()) {
            if (def.id().equals(id)) return def;
        }
        return null;
    }

    public static boolean exists(String id, ItemStack stack) {
        return get(id) != null;
    }
}