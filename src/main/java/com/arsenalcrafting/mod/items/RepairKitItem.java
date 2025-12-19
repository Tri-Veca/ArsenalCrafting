package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
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
        setData(kit, d.withMaterial(material));
    }

    // ---------------- VALUE ----------------
    public static int getStoredValue(ItemStack kit) {
        return getData(kit).storedValue();
    }

    public static void setStoredValue(ItemStack kit, int value) {
        RepairKitData d = getData(kit);
        setData(kit, d.withStoredValue(value));
    }

    // ---------------- MATERIAL LOCK ----------------
    public static boolean isMaterialLocked(ItemStack kit) {
        return getData(kit).materialLocked();
    }

    public static void setMaterialLocked(ItemStack kit, boolean locked) {
        RepairKitData d = getData(kit);
        setData(kit, d.withMaterialLocked(locked));
    }

    // ---------------- RESET MATERIAL ----------------
    public static boolean resetMaterial(ItemStack kit) {
        RepairKitData d = getData(kit);
        if (d.storedValue() > 0) {
            // Can't reset if there's stored value
            return false;
        }
        setData(kit, d.withMaterial("").withMaterialLocked(false));
        return true;
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
                d.ingot(),
                d.materialLocked()
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
                stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy()),
                d.materialLocked()
        ));
    }

    // ---------------- MENU ----------------
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new com.arsenalcrafting.mod.client.menu.RepairKitMenu(id, inv, stack),
                    Component.translatable("item.arsenalcrafting.repair_kit")
            ));
        }
        return InteractionResultHolder.success(stack);
    }


    // ---------------- TOOLTIP ----------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        RepairKitData data = getData(stack);

        if (data.materialLocked()) {
            tooltip.add(Component.literal("Material Locked")
                    .withStyle(ChatFormatting.RED));
        }

        // Check for repair reduction in stored tool
        ItemStack tool = getTool(stack);
        if (!tool.isEmpty()) {
            ToolAssemblyData toolData = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
            if (toolData != null && !toolData.isEmpty()) {
                AlloyMaterial coreMat = AlloyMaterial.fromName(toolData.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null && def.id().equals("repair_reduction")) {
                        tooltip.add(Component.literal("Core: Repair Reduction (-20% cost)")
                                .withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }
    }
}