package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;

import com.arsenalcrafting.mod.core.UnifiedProcessor;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.SharpnessAugmentItem;
import com.arsenalcrafting.mod.items.tool.powers.CoreHandlers;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.PerfectStrike;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA;


/**
 * Arsenal Dagger — fast, cheap, crits hard.
 *
 * Identity vs Broadsword baseline:
 *   Damage:    -1.5 flat (lowest of all weapons)
 *   Durability: -25% (cheap material, quick to break)
 *   Crit:      +8% flat additive on top of blade material crit chance
 *   Unique:    Rush — consecutive hits buff movement speed + attack speed
 *
 * Parts: DAGGER_BLADE (head), DAGGER_HILT (rod), CORE (core)
 */
public class ArsenalDaggerItem extends SwordItem implements ArsenalTool {

    // ── CRIT IDENTITY ────────────────────────────────────────────────────────
    private static final float CRIT_CHANCE_MULT  = 1.0f;
    private static final float CRIT_DAMAGE_MULT  = 1.15f;
    // Flat additive bonus on top of material crit chance — dagger's signature
    public  static final float CRIT_CHANCE_BONUS = 0.08f;

    // ── DAGGER IDENTITY ───────────────────────────────────────────────────────
    private static final float  DAMAGE_PENALTY   = 1.5f;
    private static final float  DURABILITY_MULT  = 0.75f;

    // ── RUSH ──────────────────────────────────────────────────────────────
    // Each consecutive hit within RUSH_WINDOW_TICKS adds one stack (max MAX_STACKS)
    // Stacks decay if player doesn't land a hit within the window
    public  static final int    MAX_STACKS            = 5;
    private static final int    RUSH_WINDOW_TICKS = 60; // 3 seconds
    private static final float  MOVE_SPEED_PER_STACK  = 0.03f; // +3% per stack
    private static final float  ATTACK_SPEED_PER_STACK = 0.15f; // +0.15 per stack
    // ResourceLocations for the attribute modifiers so we can remove/re-add cleanly
    private static final net.minecraft.resources.ResourceLocation RUSH_MOVE_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "dagger_rush_move");
    private static final net.minecraft.resources.ResourceLocation RUSH_ATTACK_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("arsenalcrafting", "dagger_rush_attack");

    private final ArsenalToolType toolType = ArsenalToolType.DAGGER;
    private final AlloyMaterial baseMaterial;

    public ArsenalDaggerItem(AlloyMaterial material, Properties properties) {
        super(new DummyWeaponTier(material), properties.component(
                net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY.withTooltip(false)
        ));
        this.baseMaterial = material;
    }

    // ── ARSENAL TOOL IDENTITY ─────────────────────────────────────────────────

    @Override public ArsenalToolType getToolType() { return toolType; }

    @Override
    public AlloyMaterial getBaseMaterial() { return baseMaterial; }

    @Override
    public ToolComponentType getHeadComponentType() { return ToolComponentType.DAGGER_BLADE; }

    @Override
    public ToolComponentType getRodComponentType() { return ToolComponentType.DAGGER_HILT; }

    // ── DURABILITY ────────────────────────────────────────────────────────────

    @Override
    public int getMaxDamage(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial gripMat = AlloyMaterial.fromName(data.rodMaterial());
            if (gripMat != null) {
                float dur = (float) gripMat.getDurability();
                AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null) dur = def.handler().modifyMaxDurability(stack, dur);
                }
                return (int) (dur * DURABILITY_MULT);
            }
        }
        return (int) (baseMaterial.getDurability() * DURABILITY_MULT);
    }

    // ── DAMAGE ATTRIBUTES ─────────────────────────────────────────────────────

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return getToolAttributes(stack);
    }

    @Override
    public float getAssembledDamage(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) return bladeMat.getWeaponDamage() - DAMAGE_PENALTY;
        }
        return baseMaterial.getWeaponDamage() - DAMAGE_PENALTY;
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

    // ── CRIT — flat bonus additive on top of material ─────────────────────────

    @Override public float getCritChanceMultiplier() { return CRIT_CHANCE_MULT; }
    @Override public float getCritDamageMultiplier()  { return CRIT_DAMAGE_MULT; }

    /**
     * Overrides rollCrit to pass the flat +8% bonus into CritProcessor.
     * This stacks additively on top of whatever the blade material provides.
     */
    @Override
    public float rollCrit(ItemStack weapon, LivingEntity target, float baseDamage) {
        com.arsenalcrafting.mod.core.CritProcessor.CritResult result =
                com.arsenalcrafting.mod.core.CritProcessor.processCrit(
                        weapon,
                        baseDamage,
                        getCritChanceMultiplier(),
                        getCritDamageMultiplier(),
                        CRIT_CHANCE_BONUS // flat additive bonus — always applied regardless of material
                );
        if (result.isCrit()) {
            com.arsenalcrafting.mod.core.CritProcessor.applyArmorDurabilityDamage(target, result.armorDurabilityDamage());
            com.arsenalcrafting.mod.events.ArsenalCombatEvents.spawnRedCritParticles(target);
        }
        return result.finalDamage();
    }

    // ── COMBAT ────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (BrokenToolHandler.isBroken(stack)) return false;
        float baseDamage = getAssembledDamage(stack);

        // Step 1: Cores + augments on-hit
        UnifiedProcessor.processEntityHit(stack, attacker, target, baseDamage);

        // Step 2: Roll crit (includes +8% flat bonus)
        float finalDamage = rollCrit(stack, target, baseDamage);

        // Step 3: Apply damage
        net.minecraft.world.damagesource.DamageSource source = attacker instanceof Player p
                ? target.level().damageSources().playerAttack(p)
                : target.level().damageSources().mobAttack(attacker);
        target.hurt(source, finalDamage);

        // Step 4: Rush — add a stack on every hit
        if (attacker instanceof Player player) {
            applyRushStack(stack, player);
        }

        // Step 5: Hit summary
        float sharpnessBonus = 0f;
        float extraStrike = 0f;

        if (AugmentManager.isInstalled("sharpness", stack)) {
            sharpnessBonus = SharpnessAugmentItem.getBonusDamage(
                    AugmentManager.getTier("sharpness", stack)
            );
        }
        if (CoreManager.exists("perfect_strike", stack)) {
            extraStrike = PerfectStrike.getExtraStrikeDamage();
        }

        float totalDamage = finalDamage + sharpnessBonus + extraStrike;

        int stacks = getRushStacks(stack);
        boolean isCrit = finalDamage > baseDamage;

        System.out.println("[ArsenalDagger] ──── HIT SUMMARY ────"
                + " | base=" + String.format("%.2f", baseDamage)
                + (isCrit
                ? " | ★CRIT bonus=" + String.format("%.2f", finalDamage - baseDamage)
                : " | no crit")
                + (sharpnessBonus > 0
                ? " | sharpness=" + String.format("%.2f", sharpnessBonus)
                : "")
                + (extraStrike > 0
                ? " | extraStrike=" + String.format("%.2f", extraStrike)
                : " | no extra strike")
                + " | TOTAL=" + String.format("%.2f", totalDamage)
                + " | rush=" + stacks + "/" + MAX_STACKS
                + " | blade=" + getAssembledBladeName(stack));

        // Step 6: Durability
        EquipmentSlot slot = attacker.getMainHandItem() == stack
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, attacker, slot);

        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity killed && killed.isDeadOrDying()) {
            UnifiedProcessor.processEntityKill(stack, player, killed);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    // ── RUSH SYSTEM ───────────────────────────────────────────────────────

    /**
     * NBT keys stored on the stack (not the player) so rush is weapon-specific.
     * rush_stacks: int 0-5
     * rush_timer:  long — game tick when last hit landed
     */
    private static final String NBT_STACKS = "arsenal_rush_stacks";
    private static final String NBT_TIMER  = "arsenal_rush_timer";

    private int getRushStacks(ItemStack stack) {
        var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (tag == null) return 0;
        var nbt = tag.copyTag();
        return nbt.contains(NBT_STACKS) ? nbt.getInt(NBT_STACKS) : 0;
    }

    private void setRushStacks(ItemStack stack, int stacks, long gameTick) {
        var existing = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        var nbt = existing.copyTag();
        nbt.putInt(NBT_STACKS, Math.min(stacks, MAX_STACKS));
        nbt.putLong(NBT_TIMER, gameTick);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(nbt));
    }

    private void applyRushStack(ItemStack stack, Player player) {
        long now = player.level().getGameTime();
        var tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        var nbt = tag.copyTag();

        long lastHit = nbt.contains(NBT_TIMER) ? nbt.getLong(NBT_TIMER) : 0L;
        int current  = nbt.contains(NBT_STACKS) ? nbt.getInt(NBT_STACKS) : 0;

        // Reset if outside window
        if (now - lastHit > RUSH_WINDOW_TICKS) current = 0;

        int newStacks = Math.min(current + 1, MAX_STACKS);
        setRushStacks(stack, newStacks, now);
        updateRushAttributes(player, newStacks);
        System.out.println("[DaggerRush] stacks=" + newStacks + "/" + MAX_STACKS);
    }

    private void updateRushAttributes(Player player, int stacks) {
        // Movement speed
        var moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(RUSH_MOVE_ID);
            if (stacks > 0) {
                moveAttr.addTransientModifier(new AttributeModifier(
                        RUSH_MOVE_ID,
                        stacks * MOVE_SPEED_PER_STACK,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        }
        // Attack speed
        var attackAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackAttr != null) {
            attackAttr.removeModifier(RUSH_ATTACK_ID);
            if (stacks > 0) {
                attackAttr.addTransientModifier(new AttributeModifier(
                        RUSH_ATTACK_ID,
                        stacks * ATTACK_SPEED_PER_STACK,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }
    }

    // ── TICK — decay rush if window expires ────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;
        UnifiedProcessor.processToolTick(stack, player);

        if (!selected) {
            // Not holding — clear all stacks
            if (getRushStacks(stack) > 0) {
                setRushStacks(stack, 0, 0L);
                updateRushAttributes(player, 0);
            }
            return;
        }

        // Check window expiry
        var tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        var nbt = tag.copyTag();
        long lastHit = nbt.contains(NBT_TIMER) ? nbt.getLong(NBT_TIMER) : 0L;
        long now = level.getGameTime();

        if (now - lastHit > RUSH_WINDOW_TICKS && getRushStacks(stack) > 0) {
            setRushStacks(stack, 0, 0L);
            updateRushAttributes(player, 0);
            System.out.println("[DaggerRush] decayed — no hit within window");
        }
    }

    // ── DURABILITY SAVE ───────────────────────────────────────────────────────

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount,
                                                   @Nullable T entity,
                                                   Consumer<Item> onBroken) {
        if (BrokenToolHandler.interceptDamage(stack, amount, entity)) return 0;
        if (entity != null && UnifiedProcessor.shouldSaveDurability(stack, entity.getRandom())) return 0;
        return super.damageItem(stack, amount, entity, onBroken);
    }

    // ── GUI ───────────────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> arsenalResult = onToolUse(level, player, hand);
        if (arsenalResult.getResult().consumesAction()) return arsenalResult;
        return super.use(level, player, hand);
    }

    // ── TOOLTIP ───────────────────────────────────────────────────────────────

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        appendArsenalTooltip(stack, context, tooltip, flag);

        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial bladeMat = AlloyMaterial.fromName(data.headMaterial());
            if (bladeMat != null) {
                // Crit stats
                float baseCritChance = com.arsenalcrafting.mod.core.CritProcessor
                        .calcCritChance(bladeMat, CRIT_CHANCE_MULT, CRIT_CHANCE_BONUS) * 100f;
                tooltip.add(Component.literal("  Crit Chance: " + String.format("%.1f", baseCritChance)
                                + "% (+" + (int)(CRIT_CHANCE_BONUS * 100) + "% dagger bonus)")
                        .withStyle(ChatFormatting.RED));

                float critDmg = bladeMat.getCritDamage() * CRIT_DAMAGE_MULT;
                tooltip.add(Component.literal("  Crit Multiplier: " + String.format("%.2f", critDmg) + "x")
                        .withStyle(ChatFormatting.RED));

                // Rush info
                int stacks = getRushStacks(stack);
                tooltip.add(Component.literal("  ⚡ Rush: " + stacks + "/" + MAX_STACKS + " stacks")
                        .withStyle(stacks == MAX_STACKS ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
                if (Screen.hasShiftDown()) {
                    tooltip.add(Component.literal("    Each consecutive hit within 3s adds a stack")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    tooltip.add(Component.literal("    +" + (int)(MOVE_SPEED_PER_STACK * 100)
                                    + "% move speed & +" + ATTACK_SPEED_PER_STACK
                                    + " attack speed per stack")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    tooltip.add(Component.literal("    Max " + MAX_STACKS + " stacks: +"
                                    + (int)(MAX_STACKS * MOVE_SPEED_PER_STACK * 100) + "% speed, +"
                                    + String.format("%.1f", MAX_STACKS * ATTACK_SPEED_PER_STACK) + " atk spd")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private String getAssembledBladeName(ItemStack stack) {
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) return data.headMaterial();
        return "unassembled";
    }

    // ── DUMMY TIER ────────────────────────────────────────────────────────────
    // Stats are driven by ToolAssemblyData at runtime — this just satisfies SwordItem constructor.

    private static class DummyWeaponTier implements Tier {
        private final AlloyMaterial material;

        public DummyWeaponTier(AlloyMaterial material) {
            this.material = material;
        }

        @Override public int getUses() { return material.getDurability(); }
        @Override public float getSpeed() { return material.getEfficiency(); }
        @Override public float getAttackDamageBonus() { return 0f; } // driven by getAttributeModifiers()
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return AlloyMaterial.getToolBreakTag(material); }
        @Override public int getEnchantmentValue() { return 0; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }
}