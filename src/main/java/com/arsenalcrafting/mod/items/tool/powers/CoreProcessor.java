package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CoreProcessor {

    private static final Map<String, CoreHandler> HANDLER_CACHE = new HashMap<>();

    @Nullable
    public static CoreHandler getActiveCore(ItemStack tool) {
        if (!(tool.getItem() instanceof ArsenalTool)) return null;

        ToolAssemblyData assemblyData = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get()); // FIXED: _DATA
        if (assemblyData == null) return null;

        String coreName = assemblyData.coreMaterial();
        if (coreName == null || coreName.isEmpty()) return null;

        AlloyMaterial material = AlloyMaterial.fromName(coreName);
        if (material == null) return null;

        CoreManager.CoreDefinition def = CoreManager.getForMaterial(material);
        if (def == null) return null;

        return HANDLER_CACHE.computeIfAbsent(def.id(), id -> def.handler());
    }

    public static void onBlockDestroyed(ItemStack tool, Level level,
                                        BlockState state, BlockPos pos,
                                        LivingEntity entity) {
        System.out.println("[CoreProcessor] Block destroyed: " + state.getBlock());

        CoreHandler core = getActiveCore(tool);
        if (core == null) {
            System.out.println("[CoreProcessor] No active core found");
            return;
        }

        System.out.println("[CoreProcessor] Found core: " + core.getClass().getSimpleName());
        core.onBlockDestroyed(tool, level, state, pos, entity);
    }


    public static float applyMiningSpeedModifiers(ItemStack tool, float baseSpeed, BlockState state) {
        CoreHandler core = getActiveCore(tool);
        if (core == null) return baseSpeed;

        // WorkHorse: ×30 for hard blocks ONLY
        if (core instanceof CoreHandlers.WorkHorseCore && WorkHorse.isHardBlock(state)) {
            float blockHardness = state.getDestroySpeed(null, null);
            return Math.max(blockHardness * 100.0f, 2000.0f); // YOUR EXACT LOGIC
        }

        // Other cores modify normally
        return core.modifyMiningSpeed(tool, baseSpeed);
    }

    public static boolean checkToolCorrectness(ItemStack tool, BlockState state) {
        CoreHandler core = getActiveCore(tool);
        return core instanceof CoreHandlers.WorkHorseCore && WorkHorse.isHardBlock(state);
    }

    public static boolean shouldSaveDurability(ItemStack tool, RandomSource random) {
        CoreHandler core = getActiveCore(tool);
        if (core == null) return false;
        return core instanceof CoreHandlers.SturdyCore &&
                Sturdy.trySaveDurability(random);
    }

    public static float applyDurabilityModifiers(ItemStack tool, float baseDurability) {
        CoreHandler core = getActiveCore(tool);
        return core != null ? core.modifyMaxDurability(tool, baseDurability) : baseDurability;
    }

    public static void processToolTick(ItemStack tool, Player player) {
        CoreHandler core = getActiveCore(tool);
        if (core != null) {
            core.onToolUpdate(tool, player);
        }
    }

    /**
     * Called from UnifiedProcessor.processEntityHit().
     * Runs core on-hit effects before crit is rolled.
     */
    public static void onEntityHit(ItemStack weapon, LivingEntity attacker,
                                   net.minecraft.world.entity.Entity target, float damage) {
        CoreHandler core = getActiveCore(weapon);
        if (core == null) return;
        core.onEntityHit(weapon, attacker, target, damage);
    }

    /**
     * Called from UnifiedProcessor.processEntityKill().
     * Runs core on-kill effects (Gambler drop table, etc.)
     */
    public static void onEntityKill(ItemStack weapon, LivingEntity attacker,
                                    LivingEntity killed) {
        CoreHandler core = getActiveCore(weapon);
        if (core == null) return;
        core.onEntityKill(weapon, attacker, killed);
    }
}
