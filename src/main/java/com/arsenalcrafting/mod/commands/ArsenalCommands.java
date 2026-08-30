package com.arsenalcrafting.mod.commands;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.BrokenToolHandler;
import com.arsenalcrafting.mod.registry.ModItems;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * /arsenal give <item> <player>
 *   Gives the target player a randomly assembled Arsenal tool or weapon.
 *
 * /arsenal random
 *   Randomly assembles whatever Arsenal tool you are holding.
 *
 * /arsenal clear
 *   Clears assembly data from held tool.
 *
 * /arsenal info
 *   Prints current assembly of held tool.
 */
@EventBusSubscriber(modid = ArsenalCrafting.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ArsenalCommands {

    private static final Random RANDOM = new Random();
    private static final List<AlloyMaterial> ALL_MATERIALS = Arrays.stream(AlloyMaterial.values())
            .filter(AlloyMaterial::shouldGenerateParts)
            .toList();

    /** Registry of giveable Arsenal items — name → item supplier */
    private static final Map<String, java.util.function.Supplier<Item>> GIVEABLE = new LinkedHashMap<>();

    static {
        GIVEABLE.put("katana",      () -> ModItems.ARSENAL_KATANA.get());
        GIVEABLE.put("broadsword",  () -> ModItems.ARSENAL_BROADSWORD.get());
        GIVEABLE.put("dagger",      () -> ModItems.ARSENAL_DAGGER.get());
        GIVEABLE.put("pickaxe",     () -> ModItems.ARSENAL_PICKAXE.get());
        GIVEABLE.put("shovel",      () -> ModItems.ARSENAL_SHOVEL.get());
        GIVEABLE.put("axe",         () -> ModItems.ARSENAL_AXE.get());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // /swap <part> <material> — top level shortcut for swapping tool parts
        event.getDispatcher().register(
                Commands.literal("swap")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("part", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("head");
                                    builder.suggest("rod");
                                    builder.suggest("core");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("material", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (AlloyMaterial mat : ALL_MATERIALS) {
                                                builder.suggest(mat.getName().toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> swapPart(ctx,
                                                StringArgumentType.getString(ctx, "part"),
                                                StringArgumentType.getString(ctx, "material")))
                                )
                        )
        );

        event.getDispatcher().register(
                Commands.literal("arsenal")
                        .requires(src -> src.hasPermission(2))

                        // /arsenal give <item> <player>
                        .then(Commands.literal("give")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            GIVEABLE.keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("player", EntityArgument.players())
                                                .executes(ctx -> giveItem(ctx,
                                                        StringArgumentType.getString(ctx, "item"),
                                                        EntityArgument.getPlayers(ctx, "player")))
                                        )
                                        // /arsenal give <item> — gives to self
                                        .executes(ctx -> {
                                            ServerPlayer self = ctx.getSource().getPlayerOrException();
                                            return giveItem(ctx,
                                                    StringArgumentType.getString(ctx, "item"),
                                                    List.of(self));
                                        })
                                )
                        )

                        // /arsenal random — assembles held item randomly
                        .then(Commands.literal("random")
                                .executes(ArsenalCommands::randomAssembly)
                        )

                        // /arsenal clear
                        .then(Commands.literal("clear")
                                .executes(ArsenalCommands::clearAssembly)
                        )

                        // /arsenal swap <part> <material> — swaps a specific part on held tool
                        .then(Commands.literal("swap")
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("head");
                                            builder.suggest("rod");
                                            builder.suggest("core");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("material", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (AlloyMaterial mat : ALL_MATERIALS) {
                                                        builder.suggest(mat.getName().toLowerCase());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> swapPart(ctx,
                                                        StringArgumentType.getString(ctx, "part"),
                                                        StringArgumentType.getString(ctx, "material")))
                                        )
                                )
                        )

                        // /arsenal break — marks held tool as broken for testing
                        .then(Commands.literal("break")
                                .executes(ArsenalCommands::breakTool)
                        )

                        // /arsenal repair — repairs held tool to full and clears broken state
                        .then(Commands.literal("repair")
                                .executes(ArsenalCommands::repairTool)
                        )

                        // /arsenal info
                        .then(Commands.literal("info")
                                .executes(ArsenalCommands::printInfo)
                        )
        );
    }

    // ── GIVE ──────────────────────────────────────────────────────────────────

    private static int giveItem(CommandContext<CommandSourceStack> ctx,
                                String itemName,
                                Collection<ServerPlayer> targets) {
        var supplier = GIVEABLE.get(itemName.toLowerCase());
        if (supplier == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown item: " + itemName + ". Valid: " + String.join(", ", GIVEABLE.keySet())));
            return 0;
        }

        for (ServerPlayer target : targets) {
            ItemStack stack = new ItemStack(supplier.get());
            ToolAssemblyData data = new ToolAssemblyData(
                    randomMaterial().getName(),
                    randomMaterial().getName(),
                    randomMaterial().getName()
            );
            stack.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), data);
            target.getInventory().add(stack);

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aGave §f" + stack.getHoverName().getString()
                            + " §7to §f" + target.getName().getString()
                            + " §7[Head: §e" + data.headMaterial()
                            + "§7 | Rod: §e" + data.rodMaterial()
                            + "§7 | Core: §e" + data.coreMaterial() + "§7]"), true);
        }

        return Command.SINGLE_SUCCESS;
    }

    // ── RANDOM (held item) ────────────────────────────────────────────────────

    private static int randomAssembly(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool)) {
            ctx.getSource().sendFailure(Component.literal("Hold an Arsenal tool or weapon first."));
            return 0;
        }

        ToolAssemblyData data = new ToolAssemblyData(
                randomMaterial().getName(),
                randomMaterial().getName(),
                randomMaterial().getName()
        );
        held.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), data);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aAssembled: §f" + held.getHoverName().getString()
                        + " §7[Head: §e" + data.headMaterial()
                        + "§7 | Rod: §e" + data.rodMaterial()
                        + "§7 | Core: §e" + data.coreMaterial() + "§7]"), false);

        return Command.SINGLE_SUCCESS;
    }

    // ── SWAP PART ─────────────────────────────────────────────────────────────

    private static int swapPart(CommandContext<CommandSourceStack> ctx,
                                String part, String materialName) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool)) {
            ctx.getSource().sendFailure(Component.literal("Hold an Arsenal tool or weapon first."));
            return 0;
        }

        // Validate part
        String partLower = part.toLowerCase();
        if (!partLower.equals("head") && !partLower.equals("rod") && !partLower.equals("core")) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown part: " + part + ". Use: head, rod, core"));
            return 0;
        }

        // Validate material
        AlloyMaterial mat = null;
        for (AlloyMaterial m : ALL_MATERIALS) {
            if (m.getName().equalsIgnoreCase(materialName)) {
                mat = m;
                break;
            }
        }
        if (mat == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown material: " + materialName + ". Valid: " + getMaterialList()));
            return 0;
        }

        // Get or create assembly data
        ToolAssemblyData existing = held.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        String head = existing != null ? existing.headMaterial() : "";
        String rod  = existing != null ? existing.rodMaterial()  : "";
        String core = existing != null ? existing.coreMaterial() : "";

        // Swap the requested part
        final AlloyMaterial finalMat = mat;
        switch (partLower) {
            case "head" -> head = finalMat.getName();
            case "rod"  -> rod  = finalMat.getName();
            case "core" -> core = finalMat.getName();
        }

        held.set(ModComponents.TOOL_ASSEMBLY_DATA.get(),
                new ToolAssemblyData(head, rod, core));

        final String fHead = head, fRod = rod, fCore = core;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aSwapped §e" + partLower + " §ato §e" + finalMat.getName()
                        + " §7| Head: §e" + fHead
                        + " §7| Rod: §e" + fRod
                        + " §7| Core: §e" + fCore), false);

        return Command.SINGLE_SUCCESS;
    }

    // ── BREAK (debug) ─────────────────────────────────────────────────────────

    private static int breakTool(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool)) {
            ctx.getSource().sendFailure(Component.literal("Hold an Arsenal tool or weapon first."));
            return 0;
        }

        // Set durability to max-1 and mark broken
        held.setDamageValue(held.getMaxDamage() - 1);
        BrokenToolHandler.markBroken(held, player);

        ctx.getSource().sendSuccess(() ->
                        Component.literal("§c⚠ " + held.getHoverName().getString() + " is now BROKEN."),
                false);
        return Command.SINGLE_SUCCESS;
    }

    // ── REPAIR (debug) ────────────────────────────────────────────────────────

    private static int repairTool(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool)) {
            ctx.getSource().sendFailure(Component.literal("Hold an Arsenal tool or weapon first."));
            return 0;
        }

        held.setDamageValue(0);
        BrokenToolHandler.markRepaired(held);

        ctx.getSource().sendSuccess(() ->
                        Component.literal("§a✓ " + held.getHoverName().getString() + " fully repaired."),
                false);
        return Command.SINGLE_SUCCESS;
    }

    // ── CLEAR ─────────────────────────────────────────────────────────────────

    private static int clearAssembly(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool)) {
            ctx.getSource().sendFailure(Component.literal("Hold an Arsenal tool or weapon first."));
            return 0;
        }

        held.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), ToolAssemblyData.EMPTY);
        ctx.getSource().sendSuccess(() ->
                        Component.literal("§7Cleared assembly from " + held.getHoverName().getString()),
                false);
        return Command.SINGLE_SUCCESS;
    }

    // ── INFO ──────────────────────────────────────────────────────────────────

    private static int printInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }

        ItemStack held = player.getMainHandItem();
        ToolAssemblyData data = held.get(ModComponents.TOOL_ASSEMBLY_DATA.get());

        if (data == null || data.isEmpty()){
            ctx.getSource().sendSuccess(() ->
                    Component.literal("§7No assembly data on held item."), false);
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§6" + held.getHoverName().getString() + "\n"
                        + "§7Head: §e"  + data.headMaterial() + "\n"
                        + "§7Rod:  §e"  + data.rodMaterial()  + "\n"
                        + "§7Core: §e"  + data.coreMaterial()), false);

        return Command.SINGLE_SUCCESS;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private static AlloyMaterial randomMaterial() {
        return ALL_MATERIALS.get(RANDOM.nextInt(ALL_MATERIALS.size()));
    }

    private static String getMaterialList() {
        StringBuilder sb = new StringBuilder();
        for (AlloyMaterial mat : ALL_MATERIALS) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(mat.getName().toLowerCase());
        }
        return sb.toString();
    }
}