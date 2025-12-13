package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class RepairKitItem extends Item {

    public RepairKitItem(Properties props) {
        super(props.stacksTo(1));
    }

    // ---------------- DATA ACCESS ----------------

    public static RepairKitData getData(ItemStack kit) {
        return kit.getOrDefault(
                ModComponents.REPAIR_KIT_DATA.get(),
                RepairKitData.EMPTY
        );
    }

    public static void setData(ItemStack kit, RepairKitData data) {
        kit.set(ModComponents.REPAIR_KIT_DATA.get(), data);
    }

    // ---------------- MATERIAL ----------------

    public static String getMaterial(ItemStack kit) {
        return getData(kit).material();
    }

    public static void setMaterial(ItemStack kit, String material) {
        RepairKitData d = getData(kit);
        setData(kit, new RepairKitData(
                material,
                d.storedValue(),
                d.tool(),
                d.ingot()
        ));
    }

    // ---------------- VALUE ----------------

    public static int getStoredValue(ItemStack kit) {
        return getData(kit).storedValue();
    }

    public static void setStoredValue(ItemStack kit, int value) {
        RepairKitData d = getData(kit);
        setData(kit, new RepairKitData(
                d.material(),
                value,
                d.tool(),
                d.ingot()
        ));
    }

    // ---------------- TOOL ----------------

    public static ItemStack getTool(ItemStack kit) {
        return getData(kit).tool().orElse(ItemStack.EMPTY).copy();
    }

    public static void setTool(ItemStack kit, ItemStack stack) {
        RepairKitData d = getData(kit);
        setData(kit, new RepairKitData(
                d.material(),
                d.storedValue(),
                stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy()),
                d.ingot()
        ));
    }

    // ---------------- INGOT ----------------

    public static ItemStack getIngot(ItemStack kit) {
        return getData(kit).ingot().orElse(ItemStack.EMPTY).copy();
    }
    public static void setIngot(ItemStack kit, ItemStack stack) {
        RepairKitData d = getData(kit);
        setData(kit, new RepairKitData(
                d.material(),
                d.storedValue(),
                d.tool(),
                stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy())
        ));
    }

    // ---------------- MENU ----------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) ->
                            new com.arsenalcrafting.mod.client.menu.RepairKitMenu(id, inv, stack),
                    Component.translatable("item.arsenalcrafting.repair_kit")
            ));
        }
        return InteractionResultHolder.success(stack);
    }
}
