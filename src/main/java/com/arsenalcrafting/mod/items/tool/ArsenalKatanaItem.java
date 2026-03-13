package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.UnifiedProcessor;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA;

/**
 * Arsenal Katana — modular weapon class.
 *
 * Assembly: blade (headMaterial) + hilt (rodMaterial) + core (coreMaterial)
 *   - Blade  → base damage + crit stats
 *   - Hilt   → durability
 *   - Core   → special passive ability
 *
 * Crit identity: 1.1x chance multiplier, 1.15x damage multiplier
 * Future: swap SwordItem for a base weapon class when arc/multihit is implemented.
 *
 * Data gen naming convention:
 *   katana_blade_item_base.png  — blade part shown in components creative tab
 *   katana_blade_base.png       — blade layer rendered on the assembled katana
 *   katana_hilt_item_base.png   — hilt part shown in components creative tab
 *   katana_hilt_base.png        — hilt layer rendered on the assembled katana
 */
public class ArsenalKatanaItem extends SwordItem implements ArsenalTool {

    // ── CRIT IDENTITY ────────────────────────────────────────────────────────
    private static final float CRIT_CHANCE_MULT  = 0.9f;
    private static final float CRIT_DAMAGE_MULT  = 1.1f;

    // ── KATANA IDENTITY ───────────────────────────────────────────────────────
    // Slightly weaker per-hit than broadsword — compensated by arc + reach
    private static final float DAMAGE_PENALTY    = 0.5f;
    private static final float DURABILITY_MULT   = 0.85f;
    // Always hits at least MIN_ARC_EXTRAS+1 targets total (material arc can increase this)
    private static final int   MIN_ARC_EXTRAS    = 2;
    private static final float ARC_DAMAGE_FRACTION = 0.75f;
    private static final double ARC_RADIUS       = 3.0;
    // Public so ArsenalAttributeEvents can reference it
    public  static final double REACH_BONUS      = 1.0;

    private final ArsenalToolType toolType = ArsenalToolType.KATANA;
    private final AlloyMaterial baseMaterial;

    public ArsenalKatanaItem(AlloyMaterial material, Properties properties) {
        super(new DummyWeaponTier(material), properties.component(
                net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY.withTooltip(false)
        ));
        this.baseMaterial = material;
    }

    // ── ARSENAL TOOL IDENTITY ─────────────────────────────────────────────────

    @Override
    public ArsenalToolType getToolType() {
        return toolType;
    }

    @Override
    public AlloyMaterial getBaseMaterial() {
        return baseMaterial;
    }

    @Override
    public ToolComponentType getHeadType() {
        return ToolComponentType.KATANA_BLADE; // blade = head slot in ToolAssemblyData
    }

    @Override
    public ToolComponentType getRodType() {
        return ToolComponentType.KATANA_HILT;  // hilt = rod slot in ToolAssemblyData
    }

    @Override
    public ToolComponentType getHeadComponentType() {
        return null;
    }

    @Override
    public ToolComponentType getRodComponentType() {
        return null;
    }

    // ── CRIT MULTIPLIERS ─────────────────────────────────────────────────────

    @Override
    public float getCritChanceMultiplier() {
        return CRIT_CHANCE_MULT;
    }

    @Override
    public float getCritDamageMultiplier() {
        return CRIT_DAMAGE_MULT;
    }

    // ── ENCHANTING ────────────────────────────────────────────────────────────

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    // ── DUMMY TIER ───────────────────────────────────────────────────────────
    // Same pattern as tool classes. Stats are driven by ToolAssemblyData at runtime,
    // not by the tier. This just satisfies the SwordItem constructor.

    private static class DummyWeaponTier implements Tier {
        private final AlloyMaterial material;

        public DummyWeaponTier(AlloyMaterial material) {
            this.material = material;
        }

        @Override
        public int getUses() {
            return material.getDurability();
        }

        @Override
        public float getSpeed() {
            return material.getEfficiency();
        }

        @Override
        public float getAttackDamageBonus() {
            // Always 0 — damage is driven entirely by getAttributeModifiers()
            // which reads live from ToolAssemblyData. Tier value is ignored.
            return 0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return AlloyMaterial.getToolBreakTag(material);
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    }

    // ── DURABILITY ────────────────────────────────────────────────────────────
    // Hilt (rodMaterial) drives durability, same as tool rod.

    @Override
    public int getMaxDamage(ItemStack stack) {
        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (assemblyData != null && !assemblyData.isEmpty()) {
            // rodMaterial = hilt for weapons
            AlloyMaterial hiltMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            if (hiltMat != null) {
                float durability = (float) hiltMat.getDurability();
                AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null) {
                        durability = def.handler().modifyMaxDurability(stack, durability);
                    }
                }
                return (int) (durability * DURABILITY_MULT);
            }
        }
        return (int) (baseMaterial.getDurability() * DURABILITY_MULT);
    }

    // ── DAMAGE ATTRIBUTES ─────────────────────────────────────────────────────

    /**
     * Delegates to ArsenalTool.getToolAttributes() which reads live from ToolAssemblyData.
     * This replaces SwordItem's static tier-based attribute with dynamic blade material damage.
     */
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return getToolAttributes(stack);
    }



    // ── COMBAT ────────────────────────────────────────────────────────────────

    /**
     * Main combat hook. Called when this weapon hurts an entity.
     *
     * Flow:
     *   1. UnifiedProcessor.processEntityHit() — cores + augments
     *   2. rollCrit() — crit roll using blade material + Katana multipliers
     *   3. Apply crit bonus damage on top of vanilla's base hit
     *   4. Durability
     *
     * Note: vanilla already applied base damage via attributes before hurtEnemy fires.
     * We apply only the CRIT BONUS as additional damage here.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        float baseDamage = getAssembledDamage(stack);

        // Step 1: Cores + augments on primary target
        UnifiedProcessor.processEntityHit(stack, attacker, target, baseDamage);

        // Step 2: Roll crit
        float finalDamage = rollCrit(stack, target, baseDamage);

        // Step 3: Apply damage to primary target
        net.minecraft.world.damagesource.DamageSource source = attacker instanceof Player p
                ? target.level().damageSources().playerAttack(p)
                : target.level().damageSources().mobAttack(attacker);
        target.hurt(source, finalDamage);

        // Step 4: Arc multi-hit
        int arcExtras = getArcCount(stack);
        int arcHits = 0;
        float arcDmg = 0f;
        if (arcExtras > 0) {
            arcDmg = finalDamage * ARC_DAMAGE_FRACTION;
            java.util.List<LivingEntity> nearby = getNearbyArcTargets(target, attacker, arcExtras);
            arcHits = nearby.size();
            for (LivingEntity arcTarget : nearby) {
                UnifiedProcessor.processEntityHit(stack, attacker, arcTarget, baseDamage);
                arcTarget.hurt(source, arcDmg);
            }
        }

        // Step 5: Hit summary debug
        float sharpnessBonus = 0f;
        if (com.arsenalcrafting.mod.core.augments.AugmentManager.isInstalled("sharpness", stack)) {
            int sharpTier = com.arsenalcrafting.mod.core.augments.AugmentManager.getTier("sharpness", stack);
            sharpnessBonus = com.arsenalcrafting.mod.items.augments.SharpnessAugmentItem.getBonusDamage(sharpTier);
        }
        float totalDamage = finalDamage + sharpnessBonus + (arcHits * arcDmg);
        boolean isCrit = finalDamage > baseDamage;
        System.out.println("[ArsenalKatana] ──── HIT SUMMARY ────"
                + " | base=" + String.format("%.2f", baseDamage)
                + (isCrit ? " | ★CRIT bonus=" + String.format("%.2f", finalDamage - baseDamage) : " | no crit")
                + (sharpnessBonus > 0 ? " | sharpness=" + String.format("%.2f", sharpnessBonus) : "")
                + (arcHits > 0 ? " | arc=" + arcHits + "x" + String.format("%.2f", arcDmg) : "")
                + " | TOTAL=" + String.format("%.2f", totalDamage)
                + " | blade=" + getAssembledBladeName(stack));

        // Step 6: Durability
        EquipmentSlot slot = attacker.getMainHandItem() == stack
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, attacker, slot);

        return true;
    }

    /** Returns number of ADDITIONAL arc targets beyond primary. Minimum MIN_ARC_EXTRAS. */
    private int getArcCount(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) {
                int fromMaterial = Math.max(0, (int) bladeMat.getArc() - 1);
                return Math.max(fromMaterial, MIN_ARC_EXTRAS);
            }
        }
        return MIN_ARC_EXTRAS;
    }

    /** Finds nearby living entities for arc hits, excluding primary target and attacker. */
    private java.util.List<LivingEntity> getNearbyArcTargets(LivingEntity primary,
                                                             LivingEntity attacker, int max) {
        net.minecraft.world.phys.AABB box = primary.getBoundingBox().inflate(ARC_RADIUS);
        return primary.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e != primary && e != attacker && e.isAlive()
        ).stream().limit(max).toList();
    }

    /**
     * Called when this weapon kills an entity.
     * Routes to UnifiedProcessor for core + augment kill events (Gambler, etc.)
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity killed && killed.isDeadOrDying()) {
            UnifiedProcessor.processEntityKill(stack, player, killed);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    // ── DURABILITY SAVE ────────────────────────────────────────────────────────

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount,
                                                   @Nullable T entity,
                                                   Consumer<Item> onBroken) {
        if (entity != null) {
            if (UnifiedProcessor.shouldSaveDurability(stack, entity.getRandom())) {
                return 0;
            }
        }
        return super.damageItem(stack, amount, entity, onBroken);
    }

    // ── GUI ───────────────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> arsenalResult = onToolUse(level, player, hand);
        if (arsenalResult.getResult().consumesAction()) {
            return arsenalResult;
        }
        return super.use(level, player, hand);
    }

    // ── TICK ──────────────────────────────────────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player)) return;
        if (selected) {
            UnifiedProcessor.processToolTick(stack, player);
        }
    }

    // ── TEST MODE ─────────────────────────────────────────────────────────────

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (TestModeConfig.isTestModeEnabled()) {
            System.out.println("[TestMode] Installing all katana augments on new katana");
            for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
                var handler = AugmentManager.getHandler(def.id());
                if (handler != null && handler.supportsToolType(ArsenalToolType.KATANA)) {
                    if (TestModeConfig.isAugmentEnabled(def.id())) {
                        int freeSlot = getFirstFreeSlot(stack);
                        if (freeSlot >= 0) {
                            setAugmentInSlot(stack, freeSlot, def.id());
                            AugmentManager.install(def.id(), stack, def.hasTiers() ? def.maxTier() : 1);
                            System.out.println("[TestMode] Installed: " + def.name() + " in slot " + freeSlot);
                        }
                    }
                }
            }
        }
        return stack;
    }

    // ── TOOLTIP ───────────────────────────────────────────────────────────────

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        appendArsenalTooltip(stack, context, tooltip, flag);

        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (assemblyData != null && !assemblyData.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            if (bladeMat != null) {
                float critChance = com.arsenalcrafting.mod.core.CritProcessor.calcCritChance(
                        bladeMat, CRIT_CHANCE_MULT, 0f) * 100f;
                tooltip.add(Component.literal("  Crit Chance: " + String.format("%.1f", critChance) + "%")
                        .withStyle(ChatFormatting.RED));

                float critDamage = bladeMat.getCritDamage() * CRIT_DAMAGE_MULT;
                tooltip.add(Component.literal("  Crit Multiplier: " + String.format("%.2f", critDamage) + "x")
                        .withStyle(ChatFormatting.RED));

                tooltip.add(Component.literal("  ⚔ Gain extra interaction range and allows multi-hit attacks")
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Gets damage from blade material (headMaterial).
     * Falls back to base material if not yet assembled.
     */
    @Override
    public float getAssembledDamage(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) return bladeMat.getWeaponDamage() - DAMAGE_PENALTY;
        }
        return baseMaterial.getWeaponDamage() - DAMAGE_PENALTY;
    }

    /**
     * Gets attack speed from blade material.
     * Falls back to base material if not yet assembled.
     */
    @Override
    public float getAssembledAttackSpeed(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) return bladeMat.getAttackSpeed();
        }
        return baseMaterial.getAttackSpeed();
    }

    /** Debug helper — returns blade material name or "unassembled". */
    private String getAssembledBladeName(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) return data.headMaterial();
        return "unassembled";
    }
}