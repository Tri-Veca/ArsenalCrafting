package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.items.augments.EfficiencyAugmentItem;
import com.arsenalcrafting.mod.items.tool.powers.BurnProtection;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import javax.annotation.Nullable;
import java.util.List;

import static com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA;

/**
 * Core interface for all Arsenal tools.
 * Provides augment slot management, material info, and GUI access.
 */
public interface ArsenalTool extends IItemExtension {

    ArsenalToolType getToolType();
    AlloyMaterial getBaseMaterial();

    /**
     * Returns the specific component type required for the head slot of this tool.
     */
    default ToolComponentType getHeadType() {
        // Default to PICKAXE_HEAD so existing tools don't break immediately
        return ToolComponentType.PICKAXE_HEAD;
    }

    default ToolComponentType getRodType() {
        // Default to PICKAXE_HEAD so existing tools don't break immediately
        return ToolComponentType.TOOL_ROD;
    }

    // ==================== AUGMENT SLOTS ====================

    default ToolAugmentSlots getAugmentSlots(ItemStack stack) {
        ToolAugmentSlots slots = stack.get(ModComponents.TOOL_AUGMENT_SLOTS.get());
        if (slots == null) {
            slots = ToolAugmentSlots.migrateFromOld(this, stack);
            if (slots == null) slots = new ToolAugmentSlots();
            stack.set(ModComponents.TOOL_AUGMENT_SLOTS.get(), slots);
        }
        return slots;
    }

    default void setAugmentSlots(ItemStack stack, ToolAugmentSlots slots) {
        if (slots == null) slots = new ToolAugmentSlots();
        stack.set(ModComponents.TOOL_AUGMENT_SLOTS.get(), slots);
    }

    default void setAugmentInSlot(ItemStack stack, int slot, @Nullable String augmentId) {
        ToolAugmentSlots current = getAugmentSlots(stack);
        ToolAugmentSlots updated = current.withSlot(slot, augmentId == null ? "" : augmentId);
        setAugmentSlots(stack, updated);
    }

    default void clearAugmentSlot(ItemStack stack, int slot) {
        ToolAugmentSlots current = getAugmentSlots(stack);
        setAugmentSlots(stack, current.clearSlot(slot));
    }

    default boolean isAugmentInSlot(ItemStack stack, String augmentId, int slot) {
        if (augmentId == null) return false;
        String slotAugment = getAugmentSlots(stack).getAugmentInSlot(slot);
        return augmentId.equals(slotAugment);
    }

    default int getUsedAugmentSlots(ItemStack stack) {
        return getAugmentSlots(stack).getUsedSlotCount();
    }

    default int getTotalAugmentSlots() {
        return 4;
    }

    default int getFirstFreeSlot(ItemStack stack) {
        ToolAugmentSlots slots = getAugmentSlots(stack);
        for (int i = 0; i < 4; i++) {
            if (slots.getAugmentInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // ==================== AUGMENT STATE ====================

    default boolean isAugmentEnabled(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        return AugmentManager.isInstalled(augmentId, stack);
    }

    default void setAugmentEnabled(ItemStack stack, String augmentId, boolean enabled) {
        if (augmentId == null || augmentId.isEmpty()) return;
        if (enabled) {
            ToolAugmentSlots slots = getAugmentSlots(stack);
            int slot = slots.findSlotForAugment(augmentId);
            if (slot >= 0) {
                AugmentManager.install(augmentId, stack, AugmentManager.getTier(augmentId, stack));
            }
        } else {
            AugmentManager.uninstall(augmentId, stack);
        }
    }

    default void toggleAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return;
        boolean currentlyEnabled = isAugmentEnabled(stack, augmentId);
        setAugmentEnabled(stack, augmentId, !currentlyEnabled);
    }

    // ==================== VALIDATION / INSTALLATION ====================

    default boolean isValidAugmentForTool(String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        var handler = AugmentManager.getHandler(augmentId);
        if (handler != null) {
            return handler.supportsToolType(getToolType());
        }
        return false;
    }

    default boolean canInstallAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        if (!isValidAugmentForTool(augmentId)) return false;
        if (getUsedAugmentSlots(stack) >= getTotalAugmentSlots()) return false;
        return !getAugmentSlots(stack).hasAugment(augmentId);
    }

    default boolean installAugment(ItemStack stack, String augmentId, int tier) {
        if (!canInstallAugment(stack, augmentId)) return false;
        int freeSlot = getFirstFreeSlot(stack);
        if (freeSlot >= 0) {
            setAugmentInSlot(stack, freeSlot, augmentId);
            AugmentManager.install(augmentId, stack, tier);
            return true;
        }
        return false;
    }

    default boolean uninstallAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        ToolAugmentSlots slots = getAugmentSlots(stack);
        int slot = slots.findSlotForAugment(augmentId);
        if (slot >= 0) {
            clearAugmentSlot(stack, slot);
            AugmentManager.uninstall(augmentId, stack);
            return true;
        }
        return false;
    }

    // ==================== GUI ====================

    default InteractionResultHolder<ItemStack> onToolUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Broken tools can't be used
        if (com.arsenalcrafting.mod.items.tool.BrokenToolHandler.isBroken(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!level.isClientSide) {
                openToolGui(player, stack);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    default void openToolGui(Player player, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new com.arsenalcrafting.mod.client.menu.ToolMenu(id, inventory, stack),
                Component.translatable("gui.arsenalcrafting.tool")
        ));
    }

    public static void tickHeldToolAugments(Player player, ItemStack stack) {
        // Just delegate to AugmentProcessor which has proper caching and error handling
        AugmentProcessor.processToolTick(stack, player);
    }

    // ==================== MATERIAL INFO ====================

    /**
     * Primary material used for repair cost.
     * Head material takes priority, fallback to base material.
     */
    default AlloyMaterial getRepairMaterial(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial head = AlloyMaterial.fromName(data.headMaterial());
            if (head != null) return head;
        }
        return getBaseMaterial();
    }

    default String getMaterialName(ItemStack stack) {
        AlloyMaterial mat = getRepairMaterial(stack);
        return mat != null ? mat.getName() : "";
    }
    /**
     * Repair efficiency - 1 value = 15% of durability
     */
    /**
     * Repair efficiency — average of head and rod material modifiers.
     * Lower value = cheaper to repair (modifier acts as a cost multiplier).
     * Core material has no effect on repair cost.
     *
     * Example: Steel rod (1.2) + Gold head (0.5) → (1.2 + 0.5) / 2 = 0.85
     */
    default float getRepairEfficiency(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());

        if (data != null && !data.isEmpty()) {
            AlloyMaterial headMat = AlloyMaterial.fromName(data.headMaterial());
            AlloyMaterial rodMat  = AlloyMaterial.fromName(data.rodMaterial());

            if (headMat != null && rodMat != null) {
                return (headMat.getModifier() + rodMat.getModifier()) / 2.0f;
            }
            if (headMat != null) return headMat.getModifier();
            if (rodMat  != null) return rodMat.getModifier();
        }

        // Fallback for unassembled tools
        AlloyMaterial base = getRepairMaterial(stack);
        return base != null ? base.getModifier() : 1.0f;
    }

    // ==================== TOOLTIP ====================

    @OnlyIn(Dist.CLIENT)
    default void appendArsenalTooltip(ItemStack stack, Item.TooltipContext context,
                                      List<Component> tooltip, TooltipFlag flag) {

        // ── BROKEN STATE ──────────────────────────────────────────────────────
        if (com.arsenalcrafting.mod.items.tool.BrokenToolHandler.isBroken(stack)) {
            tooltip.add(Component.literal("⚠ BROKEN! Repair to use.")
                    .withStyle(net.minecraft.ChatFormatting.RED)
                    .withStyle(net.minecraft.ChatFormatting.BOLD));
        }

        ToolAugmentSlots slots = getAugmentSlots(stack);
        int usedSlots = slots.getUsedSlotCount();

        if (usedSlots > 0) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("tooltip.arsenalcrafting.installed_augments")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));

            for (int i = 0; i < 4; i++) {
                String augmentId = slots.getAugmentInSlot(i);
                if (augmentId == null || augmentId.isEmpty()) continue;

                boolean enabled = isAugmentEnabled(stack, augmentId);
                net.minecraft.ChatFormatting color = enabled
                        ? net.minecraft.ChatFormatting.GREEN
                        : net.minecraft.ChatFormatting.RED;

                // Get translation key
                String translationKey;
                var def = AugmentManager.get(augmentId);
                if (def != null && def.hasTiers()) {
                    int tier = AugmentManager.getTier(augmentId, stack);
                    if (tier <= 0) tier = 1;
                    translationKey = "augment.arsenalcrafting." + def.id() + "_augment.tier" + tier;
                } else {
                    translationKey = AugmentManager.getTranslationKey(augmentId);
                }

                Component augmentName = Component.translatable(translationKey).withStyle(color);

                if (enabled) {
                    augmentName = augmentName.copy()
                            .append(Component.literal(" ✓").withStyle(net.minecraft.ChatFormatting.GREEN));
                } else {
                    augmentName = augmentName.copy()
                            .append(Component.literal(" ✗").withStyle(net.minecraft.ChatFormatting.RED));
                }

                tooltip.add(Component.literal("  Slot " + (i + 1) + ": ")
                        .withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(augmentName));
            }
        }


        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("item.arsenalcrafting.augment_slots",
                        usedSlots + "/" + getTotalAugmentSlots())
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));

        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (assemblyData != null && !assemblyData.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts").withStyle(ChatFormatting.DARK_AQUA));

            tooltip.add(Component.translatable("item.arsenalcrafting.head_material", assemblyData.headMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.rod_material", assemblyData.rodMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.core_material", assemblyData.coreMaterial()).withStyle(ChatFormatting.WHITE));

            AlloyMaterial headMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            AlloyMaterial rodMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());

            if (headMat != null && rodMat != null && coreMat != null) {
                tooltip.add(Component.literal(""));

                boolean isWeapon = getToolType().isWeapon();

                // --- DURABILITY (shared) ---
                // Use stack.getMaxDamage() so weapon-specific multipliers (katana -15%,
                // dagger -25%) and core modifyMaxDurability are all already applied.
                int finalDurability = stack.getMaxDamage();
                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat, getToolType());

                tooltip.add(Component.translatable("item.arsenalcrafting.repairability", headMat.getName())
                        .withStyle(headMat.getColor()));
                tooltip.add(Component.translatable("item.arsenalcrafting.durability")
                        .append(": " + finalDurability)
                        .withStyle(rodMat.getColor()));

                if (isWeapon) {
                    // --- WEAPON STATS ---
                    float finalDamage = getAssembledDamage(stack);
                    float finalAttackSpeed = getAssembledAttackSpeed(stack);
                    if (coreDef != null) {
                        finalDamage = coreDef.handler().modifyAttackDamage(stack, finalDamage);
                        finalAttackSpeed = coreDef.handler().modifyAttackSpeed(stack, finalAttackSpeed);
                    }
                    if (isAugmentEnabled(stack, "sharpness")) {
                        int sharpTier = AugmentManager.getTier("sharpness", stack);
                        finalDamage += com.arsenalcrafting.mod.items.augments.SharpnessAugmentItem.getBonusDamage(sharpTier);
                    }
                    tooltip.add(Component.literal("  Damage: " + String.format("%.1f", finalDamage))
                            .withStyle(headMat.getColor()));
                    tooltip.add(Component.literal("  Attack Speed: " + String.format("%.2f", finalAttackSpeed))
                            .withStyle(ChatFormatting.YELLOW));
                    if (coreDef != null) {
                        tooltip.add(Component.literal("• ")
                                .append(Component.translatable("core.arsenalcrafting." + coreDef.id())
                                        .withStyle(coreMat.getColor())));
                        if (Screen.hasShiftDown()) {
                            tooltip.add(Component.literal(coreDef.descriptionFor(true))
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        }

                    }
                } else {
                    // --- TOOL STATS ---
                    float finalEfficiency = headMat.getEfficiency();
                    if (coreDef != null) {
                        finalEfficiency = coreDef.handler().modifyMiningSpeed(stack, finalEfficiency);
                    }
                    if (isAugmentEnabled(stack, "efficiency")) {
                        finalEfficiency += EfficiencyAugmentItem.getSpeedBonus(stack, finalEfficiency);
                    }
                    tooltip.add(Component.translatable("item.arsenalcrafting.efficiency")
                            .append(": " + String.format("%.1f", finalEfficiency))
                            .withStyle(headMat.getColor()));
                    if (coreDef != null) {
                        tooltip.add(Component.literal("• ")
                                .append(Component.translatable("core.arsenalcrafting." + coreDef.id())
                                        .withStyle(coreMat.getColor())));
                        if (Screen.hasShiftDown()) {
                            tooltip.add(Component.literal(coreDef.descriptionFor(false))
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }
                }
            }
        }

    }



    // Add this method to your ArsenalTool interface
    default ItemAttributeModifiers getToolAttributes(ItemStack stack) {
        float damage = getAssembledDamage(stack);
        float attackSpeed = getAssembledAttackSpeed(stack);

        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                damage,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID,
                                attackSpeed - 4.0,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build()
                .withTooltip(false);
    }

    // ==================== WEAPON ATTRIBUTES ====================
    // Default implementations return 0 — weapons override these with blade material values.
    // Tools (Pickaxe, Shovel, Axe) never call getToolAttributes() so these are never used.

    ToolComponentType getHeadComponentType();

    ToolComponentType getRodComponentType();

    ItemAttributeModifiers getAttributeModifiers(ItemStack stack);

    default float getAssembledDamage(ItemStack stack) { return 0f; }
    default float getAssembledAttackSpeed(ItemStack stack) { return 4.0f; }

    // ==================== BURN PROTECTION ====================

    @Override
    default boolean canBeHurtBy(ItemStack stack, net.minecraft.world.damagesource.DamageSource source) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return !BurnProtection.hasBurnProtection(stack);
        }
        return IItemExtension.super.canBeHurtBy(stack, source);
    }



    // ==================== DURABILITY / BROKEN STATE ====================

    /**
     * Central durability intercept — inherited by ALL Arsenal tools and weapons.
     * Prevents item destruction at 0 durability by marking broken instead.
     *
     * Each tool still has its own damageItem for Sturdy core checks etc,
     * but they should call super (this default) or replicate this logic.
     *
     * Since this is an interface default, tools that DON'T override damageItem
     * get this behavior for free.
     */
    @Override
    default <T extends net.minecraft.world.entity.LivingEntity> int damageItem(
            ItemStack stack, int amount, @Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {

        // Already broken — swallow all damage
        if (BrokenToolHandler.isBroken(stack)) return 0;

        // Would hit max — mark broken instead of destroying
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
                BrokenToolHandler.checkAndBreak(stack, player);
                return 0;
            }
        }

        return amount; // let vanilla apply the damage normally
    }

    // ==================== CRIT SYSTEM ====================

    /**
     * Weapon class crit chance multiplier.
     * Override in each weapon class to define its identity.
     *
     * Broadsword: 1.0x  Katana: 1.1x  Dagger: 1.5x  Chakram: 1.2x
     */
    default float getCritChanceMultiplier() { return 1.0f; }

    /**
     * Weapon class crit damage multiplier.
     * Override in each weapon class to define its identity.
     *
     * Broadsword: 1.0x  Katana: 1.15x  Dagger: 0.9x  Chakram: 1.05x
     */
    default float getCritDamageMultiplier() { return 1.0f; }

    /**
     * Main crit entry point. Call this from hurtEnemy() AFTER UnifiedProcessor.processEntityHit().
     *
     * Returns the final damage value — same as baseDamage if no crit landed.
     * Also handles armor durability damage automatically if a crit lands.
     *
     * Usage:
     *   UnifiedProcessor.processEntityHit(stack, attacker, target, baseDamage);
     *   float finalDamage = rollCrit(stack, target, baseDamage);
     *
     * @param weapon     The weapon ItemStack
     * @param target     The entity being hit
     * @param baseDamage Damage value before crit
     * @return           Final damage (modified if crit, unchanged if not)
     */
    default float rollCrit(ItemStack weapon, net.minecraft.world.entity.LivingEntity target, float baseDamage) {
        // Broken weapons don't crit
        if (com.arsenalcrafting.mod.items.tool.BrokenToolHandler.isBroken(weapon)) {
            return 0f; // broken = no damage
        }

        com.arsenalcrafting.mod.core.CritProcessor.CritResult result =
                com.arsenalcrafting.mod.core.CritProcessor.processCrit(
                        weapon,
                        baseDamage,
                        getCritChanceMultiplier(),
                        getCritDamageMultiplier(),
                        0f  // flatBonuses — wire in Tin core bonus and helmet skill here when implemented
                );

        if (result.isCrit()) {
            com.arsenalcrafting.mod.core.CritProcessor.applyArmorDurabilityDamage(target, result.armorDurabilityDamage());
            com.arsenalcrafting.mod.events.ArsenalCombatEvents.spawnRedCritParticles(target);
        }

        return result.finalDamage();
    }



}