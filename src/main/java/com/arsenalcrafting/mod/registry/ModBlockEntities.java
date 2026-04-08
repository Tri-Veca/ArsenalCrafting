package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.*;
import com.arsenalcrafting.mod.power.GeneratorType;
import com.arsenalcrafting.mod.power.PowerTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsenalCrafting.MOD_ID);

    // ── EXISTING ──────────────────────────────────────────────────────────────

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolkitBlockEntity>> TOOLKIT =
            BLOCK_ENTITIES.register("toolkit", () ->
                    BlockEntityType.Builder.of(ToolkitBlockEntity::new,
                            ModBlocks.TOOLKIT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LavaTankBlockEntity>> LAVA_TANK =
            BLOCK_ENTITIES.register("lava_tank", () ->
                    BlockEntityType.Builder.of(LavaTankBlockEntity::new,
                            ModBlocks.LAVA_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
            BLOCK_ENTITIES.register("alloy_smelter", () ->
                    BlockEntityType.Builder.of(AlloySmelterBlockEntity::new,
                            ModBlocks.ALLOY_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingStationBlockEntity>> CASTING_STATION =
            BLOCK_ENTITIES.register("casting_station", () ->
                    BlockEntityType.Builder.of(CastingStationBlockEntity::new,
                            ModBlocks.CASTING_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ValveFaucetBlockEntity>> VALVE_FAUCET =
            BLOCK_ENTITIES.register("valve_faucet", () ->
                    BlockEntityType.Builder.of(ValveFaucetBlockEntity::new,
                            ModBlocks.VALVE_FAUCET.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintCastingTableBlockEntity>> BLUEPRINT_CASTING_TABLE =
            BLOCK_ENTITIES.register("blueprint_casting_table", () ->
                    BlockEntityType.Builder.of(BlueprintCastingTableBlockEntity::new,
                            ModBlocks.BLUEPRINT_CASTING_TABLE.get()).build(null));
    // ── GENERATORS ────────────────────────────────────────────────────────────

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> FOSSIL_GENERATOR =
            BLOCK_ENTITIES.register("fossil_generator", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new GeneratorBlockEntity(
                                    GeneratorType.FOSSIL, PowerTier.COPPER, pos, state),
                            ModBlocks.COPPER_FOSSIL_GENERATOR.get(),
                            ModBlocks.IRON_FOSSIL_GENERATOR.get(),
                            ModBlocks.VOID_FOSSIL_GENERATOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> SOLAR_GENERATOR =
            BLOCK_ENTITIES.register("solar_generator", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new GeneratorBlockEntity(
                                    GeneratorType.SOLAR, PowerTier.COPPER, pos, state),
                            ModBlocks.COPPER_SOLAR_PANEL.get(),
                            ModBlocks.IRON_SOLAR_PANEL.get(),
                            ModBlocks.VOID_SOLAR_PANEL.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> BIO_GENERATOR =
            BLOCK_ENTITIES.register("bio_generator", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new GeneratorBlockEntity(
                                    GeneratorType.BIO, PowerTier.COPPER, pos, state),
                            ModBlocks.COPPER_BIO_GENERATOR.get(),
                            ModBlocks.IRON_BIO_GENERATOR.get(),
                            ModBlocks.VOID_BIO_GENERATOR.get()
                    ).build(null));

    // ── POWER BANKS ───────────────────────────────────────────────────────────

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBankBlockEntity>> COPPER_POWER_BANK =
            BLOCK_ENTITIES.register("copper_power_bank", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new PowerBankBlockEntity(PowerTier.COPPER, pos, state),
                            ModBlocks.COPPER_POWER_BANK.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBankBlockEntity>> IRON_POWER_BANK =
            BLOCK_ENTITIES.register("iron_power_bank", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new PowerBankBlockEntity(PowerTier.IRON, pos, state),
                            ModBlocks.IRON_POWER_BANK.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBankBlockEntity>> VOID_POWER_BANK =
            BLOCK_ENTITIES.register("void_power_bank", () ->
                    BlockEntityType.Builder.of(
                            (pos, state) -> new PowerBankBlockEntity(PowerTier.TITANIUM, pos, state),
                            ModBlocks.VOID_POWER_BANK.get()
                    ).build(null));

    /**
     * Resolves the correct BlockEntityType for a given power bank tier.
     */
    public static BlockEntityType<PowerBankBlockEntity> getPowerBankType(PowerTier tier) {
        return switch (tier) {
            case IRON -> IRON_POWER_BANK.get();
            case TITANIUM -> VOID_POWER_BANK.get();
            default -> COPPER_POWER_BANK.get();
        };
    }

    // ── CAPABILITY REGISTRATION ───────────────────────────────────────────────

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        System.out.println("[ModBlockEntities] Registering energy capabilities...");
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                FOSSIL_GENERATOR.get(), (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                SOLAR_GENERATOR.get(), (be, side) -> be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                BIO_GENERATOR.get(), (be, side) -> be.getEnergyStorage());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                COPPER_POWER_BANK.get(), (be, side) -> side != null ? be.getEnergyForSide(side) : be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                IRON_POWER_BANK.get(), (be, side) -> side != null ? be.getEnergyForSide(side) : be.getEnergyStorage());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                VOID_POWER_BANK.get(), (be, side) -> side != null ? be.getEnergyForSide(side) : be.getEnergyStorage());
        System.out.println("[ModBlockEntities] Capabilities registered.");
    }

        public static void register (IEventBus eventBus){
            BLOCK_ENTITIES.register(eventBus);
        }
    }
