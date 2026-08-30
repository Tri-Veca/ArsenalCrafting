package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.core.augments.ToolEquipTracker;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.GAME)
public class ArsenalPlayerEvents {

    private static final Map<UUID, Integer> lastSelectedSlot = new HashMap<>();
    private static final Map<UUID, ItemStack> lastHeldTool = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return; // server only

        UUID uuid = player.getUUID();
        int currentSlot = player.getInventory().selected;
        ItemStack current = player.getInventory().getItem(currentSlot);
        ItemStack previous = lastHeldTool.get(uuid);

        boolean changed = previous == null || previous.getItem() != current.getItem();

        if (changed) {
            // Unequip old tool
            if (previous != null && !previous.isEmpty()) {
                AugmentProcessor.processToolUnequip(previous, player);
            }

            // Equip new tool
            if (current != null && !current.isEmpty()) {
                AugmentProcessor.processToolEquip(current, player);
            }

            // Update last-held snapshot
            lastHeldTool.put(uuid, current.copy());
            lastSelectedSlot.put(uuid, currentSlot);
        }
    }
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        Player player = event.getPlayer();
        Level level = player.level();

        if (level.isClientSide()) return;

        ItemStack stack = itemEntity.getItem();

        // Only gravel
        if (!stack.is(Items.GRAVEL)) return;

        // Make THIS dropped gravel fire/lava resistant.
        stack.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);

        // Check every tick until the gravel hits lava.
        checkGravel(itemEntity);
    }

    private static void checkGravel(ItemEntity itemEntity) {
        if (!itemEntity.isAlive()) return;

        Level level = itemEntity.level();

        BlockPos itemPos = itemEntity.blockPosition();

        // Check the block the item is in and blocks immediately around it.
        for (BlockPos pos : BlockPos.betweenClosed(
                itemPos.offset(-1, -1, -1),
                itemPos.offset(1, 1, 1))) {

            BlockState state = level.getBlockState(pos);

            if (!state.is(Blocks.LAVA)) continue;

            int count = itemEntity.getItem().getCount();

            // Remove the gravel
            itemEntity.discard();

            // Turn lava into air
            level.setBlock(
                    pos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
            );

            // Give the material
            ItemStack reward = new ItemStack(
                    ModItems.BASIC_MATERIAL.get(),
                    count
            );

            Block.popResource(level, pos, reward);

            return;
        }

        // Check again next tick.
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(
                    serverLevel.getServer().getTickCount() + 1,
                    () -> checkGravel(itemEntity)
            ));
        }
    }
}