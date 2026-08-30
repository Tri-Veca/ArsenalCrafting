package com.arsenalcrafting.mod;
import com.arsenalcrafting.mod.commands.ArsenalCommands;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.events.ArsenalAttributeEvents;
import com.arsenalcrafting.mod.events.ArsenalPlayerEvents;
import com.arsenalcrafting.mod.generator.AlloyRecipeDefinitions;
import com.arsenalcrafting.mod.items.gadgets.ArsenalDrillItem;
import com.arsenalcrafting.mod.items.gadgets.ArsenalSawItem;
import com.arsenalcrafting.mod.power.PowerNetworkEvents;
import com.arsenalcrafting.mod.registry.*;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(ArsenalCrafting.MOD_ID)
public class ArsenalCrafting {
    public static final String MOD_ID = "arsenalcrafting";
    public ArsenalCrafting(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ArsenalCraftingClient::onClientSetup);

        // Register all deferred registries
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModComponents.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        AlloyRecipeType.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        NeoForge.EVENT_BUS.register(ArsenalAttributeEvents.class);
        NeoForge.EVENT_BUS.register(ArsenalPlayerEvents.class);
        NeoForge.EVENT_BUS.register(PowerNetworkEvents.class);
        NeoForge.EVENT_BUS.register(ArsenalCommands.class);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(this::registerCapabilities);



        TestModeConfig.setTestMode(false);
        System.out.println("[ArsenalCrafting] Constructor completed - Test Mode should be OFF");




        // IMPORTANT: The client setup (ModScreens) is handled automatically
        // by the @EventBusSubscriber annotation on the ModScreens class.
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            // When world loads, tell the Light handler to clean up
            if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
                if (!level.isClientSide()) {
                    com.arsenalcrafting.mod.core.augments.AugmentHandlers.Light.cleanupOnWorldLoad(level);
                }
            }
        });
    }



    public static void onClientSetup(final FMLClientSetupEvent event) {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            net.minecraft.world.level.material.FlowingFluid source = ModFluids.getSource(material);
            net.minecraft.world.level.material.FlowingFluid flowing = ModFluids.getFlowing(material);
            if (source != null) ItemBlockRenderTypes.setRenderLayer(source, RenderType.translucent());
            if (flowing != null) ItemBlockRenderTypes.setRenderLayer(flowing, RenderType.translucent());
        }

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AlloyRecipeDefinitions.init();
            System.out.println("[ArsenalCrafting] AlloyRecipeDefinitions initialized");
        });
    }
        // Here you would register networking packets, capabilities, etc.
        // This is always safe to run on both client and server.


    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        ModBlockEntities.registerCapabilities(event);

        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> new IEnergyStorage() {
                    @Override
                    public int receiveEnergy(int maxReceive, boolean simulate) {
                        int current = ArsenalSawItem.getEnergy(stack);
                        int accepted = Math.min(maxReceive,
                                Math.min(ArsenalSawItem.TRANSFER_RATE,
                                        ArsenalSawItem.MAX_ENERGY - current));
                        if (!simulate && accepted > 0)
                            ArsenalSawItem.setEnergy(stack, current + accepted);
                        return accepted;
                    }
                    @Override
                    public int extractEnergy(int maxExtract, boolean simulate) {
                        int current = ArsenalSawItem.getEnergy(stack);
                        int extracted = Math.min(current, Math.min(ArsenalSawItem.TRANSFER_RATE, maxExtract));
                        if (!simulate && extracted > 0)
                            ArsenalSawItem.setEnergy(stack, current - extracted);
                        return extracted;
                    }
                    @Override public int getEnergyStored() { return ArsenalSawItem.getEnergy(stack); }
                    @Override public int getMaxEnergyStored() { return ArsenalSawItem.MAX_ENERGY; }
                    @Override public boolean canExtract() { return true; }
                    @Override public boolean canReceive() { return true; }
                },
                ModItems.SAW.get()
        );

        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, ctx) -> new IEnergyStorage() {
                    @Override
                    public int receiveEnergy(int maxReceive, boolean simulate) {
                        int current = ArsenalDrillItem.getEnergy(stack);
                        int accepted = Math.min(maxReceive,
                                Math.min(ArsenalDrillItem.TRANSFER_RATE,
                                        ArsenalDrillItem.MAX_ENERGY - current));
                        if (!simulate && accepted > 0)
                            ArsenalDrillItem.setEnergy(stack, current + accepted);
                        return accepted;
                    }
                    @Override
                    public int extractEnergy(int maxExtract, boolean simulate) {
                        int current = ArsenalDrillItem.getEnergy(stack);
                        int extracted = Math.min(current, Math.min(ArsenalDrillItem.TRANSFER_RATE, maxExtract));
                        if (!simulate && extracted > 0)
                            ArsenalDrillItem.setEnergy(stack, current - extracted);
                        return extracted;
                    }
                    @Override public int getEnergyStored() { return ArsenalDrillItem.getEnergy(stack); }
                    @Override public int getMaxEnergyStored() { return ArsenalDrillItem.MAX_ENERGY; }
                    @Override public boolean canExtract() { return true; }
                    @Override public boolean canReceive() { return true; }
                },
                ModItems.DRILL.get()
        );
    }


}
