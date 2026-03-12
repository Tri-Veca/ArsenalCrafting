package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.UnifiedProcessor;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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
 * Arsenal Broadsword — modular weapon class.
 *
 * Assembly: blade (headMaterial) + grip (rodMaterial) + core (coreMaterial)
 *   - Blade  → base damage + crit stats
 *   - Grip   → durability
 *   - Core   → special passive ability
 *
 * Crit identity: 1.0x chance multiplier, 1.0x damage multiplier (baseline weapon)
 * Unique ability: Right-click to block (like pre-1.9 swords) — reduces incoming
 *                 damage by 50% while held. Restores the classic feel.
 *
 * Data gen naming convention:
 *   broadsword_blade_item_base.png  — blade part shown in components creative tab
 *   broadsword_blade_base.png       — blade layer rendered on the assembled broadsword
 *   broadsword_hilt_item_base.png   — grip part shown in components creative tab
 *   broadsword_hilt_base.png        — grip layer rendered on the assembled broadsword
 */
public class ArsenalBroadSwordItem extends SwordItem implements ArsenalTool {

    // ── CRIT IDENTITY ────────────────────────────────────────────────────────
    // Baseline weapon — other weapons are balanced relative to this.
    private static final float CRIT_CHANCE_MULT = 1.0f;
    private static final float CRIT_DAMAGE_MULT = 1.0f;

    // ── BLOCK MECHANIC ────────────────────────────────────────────────────────
    // Damage reduction while actively blocking (right-click held)
    private static final float BLOCK_DAMAGE_REDUCTION = 0.50f;

    private final ArsenalToolType toolType = ArsenalToolType.BROADSWORD;
    private final AlloyMaterial baseMaterial;

    public ArsenalBroadSwordItem(AlloyMaterial material, Properties properties) {
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
        return ToolComponentType.BROADSWORD_BLADE;
    }

    @Override
    public ToolComponentType getRodType() {
        return ToolComponentType.BROADSWORD_HILT;
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
            // Always 0 — damage driven entirely by getAttributeModifiers()
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

    @Override
    public int getMaxDamage(ItemStack stack) {
        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (assemblyData != null && !assemblyData.isEmpty()) {
            AlloyMaterial gripMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            if (gripMat != null) {
                float durability = (float) gripMat.getDurability();
                AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null) {
                        durability = def.handler().modifyMaxDurability(stack, durability);
                    }
                }
                return (int) durability;
            }
        }
        return baseMaterial.getDurability();
    }

    // ── DAMAGE ATTRIBUTES ─────────────────────────────────────────────────────

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return getToolAttributes(stack);
    }

    // ── BLOCKING (Classic Sword Block) ────────────────────────────────────────
    // ArmPose.BLOCK is returned via ArsenalClientExtensions.onRegisterClientExtensions()
    // which registers IClientItemExtensions for this item on the MOD event bus.

    /**
     * Registering DEFAULT_SHIELD_ACTIONS is all vanilla needs to treat this
     * item as a shield — blocking arm pose, UseAnim.BLOCK, and 50% damage
     * reduction all flow from this automatically.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Arsenal GUI (shift+right-click) — server only
        if (!level.isClientSide) {
            InteractionResultHolder<ItemStack> arsenalResult = onToolUse(level, player, hand);
            if (arsenalResult.getResult().consumesAction()) {
                return arsenalResult;
            }
        }

        // Always start blocking — same as ShieldItem.use()
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // ── COMBAT ────────────────────────────────────────────────────────────────

    /**
     * Main combat hook.
     *
     * Flow:
     *   1. UnifiedProcessor.processEntityHit() — cores + augments
     *   2. rollCrit() — crit roll (baseline 1.0x/1.0x)
     *   3. Apply full final damage
     *   4. Durability
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        float baseDamage = getAssembledDamage(stack);

        // Step 1: Cores + augments on-hit
        UnifiedProcessor.processEntityHit(stack, attacker, target, baseDamage);

        // Step 2: Roll crit
        float finalDamage = rollCrit(stack, target, baseDamage);

        // Step 3: Apply full final damage
        net.minecraft.world.damagesource.DamageSource source = attacker instanceof Player p
                ? target.level().damageSources().playerAttack(p)
                : target.level().damageSources().mobAttack(attacker);

        target.hurt(source, finalDamage);

        // Step 4: Hit summary
        float sharpnessBonus = 0f;
        if (com.arsenalcrafting.mod.core.augments.AugmentManager.isInstalled("sharpness", stack)) {
            int sharpTier = com.arsenalcrafting.mod.core.augments.AugmentManager.getTier("sharpness", stack);
            sharpnessBonus = com.arsenalcrafting.mod.items.augments.SharpnessAugmentItem.getBonusDamage(sharpTier);
        }
        float totalDamage = finalDamage + sharpnessBonus;
        boolean isCrit = finalDamage > baseDamage;
        System.out.println("[ArsenalBroadsword] ──── HIT SUMMARY ────"
                + " | base=" + String.format("%.2f", baseDamage)
                + (isCrit ? " | ★CRIT bonus=" + String.format("%.2f", finalDamage - baseDamage) : " | no crit")
                + (sharpnessBonus > 0 ? " | sharpness=" + String.format("%.2f", sharpnessBonus) : "")
                + " | TOTAL=" + String.format("%.2f", totalDamage)
                + " | blade=" + getAssembledBladeName(stack));

        // Step 4: Durability
        EquipmentSlot slot = attacker.getMainHandItem() == stack
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, attacker, slot);

        return true;
    }

    /**
     * Called when this weapon kills an entity.
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
            System.out.println("[TestMode] Installing all broadsword augments on new broadsword");
            for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
                var handler = AugmentManager.getHandler(def.id());
                if (handler != null && handler.supportsToolType(ArsenalToolType.BROADSWORD)) {
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

                tooltip.add(Component.literal("  ⛨ Right-click to Block (50% damage reduction)")
                        .withStyle(ChatFormatting.BLUE));
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    @Override
    public float getAssembledDamage(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) return bladeMat.getWeaponDamage();
        }
        return baseMaterial.getWeaponDamage();
    }

    @Override
    public float getAssembledAttackSpeed(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) return bladeMat.getAttackSpeed();
        }
        return baseMaterial.getAttackSpeed();
    }

    private String getAssembledBladeName(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) return data.headMaterial();
        return "unassembled";
    }
}