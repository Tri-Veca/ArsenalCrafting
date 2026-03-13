package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.power.FuelRegistry;
import com.arsenalcrafting.mod.power.GeneratorType;
import com.arsenalcrafting.mod.power.PowerTier;
import com.arsenalcrafting.mod.power.PowerStorage;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Server-only ticking block entity for all three generator types.
 *
 * Fossil: burns fuel items from a single inventory slot → FE/t while burning
 * Solar:  generates FE/t during daylight when sky is visible
 * Bio:    burns organic items from a single inventory slot → FE/t while burning
 *
 * Energy is pushed outward to adjacent blocks each tick.
 * Does NOT tick on client — all state is server-authoritative.
 */
public class GeneratorBlockEntity extends BlockEntity {

    // ── IDENTITY ──────────────────────────────────────────────────────────────

    private final GeneratorType type;
    private final PowerTier tier;

    // ── ENERGY ────────────────────────────────────────────────────────────────

    private final PowerStorage energy;

    // ── FUEL (Fossil + Bio only) ──────────────────────────────────────────────

    /** Single fuel slot inventory (slot 0). Ignored by Solar. */
    private final SimpleContainer fuelInventory = new SimpleContainer(1);

    /** Ticks remaining on current fuel item burn. 0 = not burning. */
    private int burnTimeRemaining = 0;

    /** Total burn ticks of the currently-burning item (for progress bar). */
    private int burnTimeTotalForCurrentFuel = 0;

    // ── CONSTRUCTORS ──────────────────────────────────────────────────────────

    /**
     * Matches lambda: (pos, state) -> new GeneratorBlockEntity(type, tier, pos, state)
     * Used in ModBlockEntities registration. Type resolved via resolveType() helper
     * to avoid self-referential holder access inside the registration lambda.
     */
    public GeneratorBlockEntity(GeneratorType generatorType, PowerTier tier,
                                BlockPos pos, BlockState state) {
        super(resolveType(generatorType), pos, state);
        this.type = generatorType;
        this.tier = tier;
        // Generator internal buffer = 10x output so it doesn't stall on transfer
        this.energy = new PowerStorage(tier.generatorOutput() * 10, tier.transferRate());
    }

    /** Resolves the correct BlockEntityType for a given GeneratorType at runtime. */
    private static BlockEntityType<GeneratorBlockEntity> resolveType(GeneratorType generatorType) {
        return switch (generatorType) {
            case FOSSIL -> ModBlockEntities.FOSSIL_GENERATOR.get();
            case SOLAR  -> ModBlockEntities.SOLAR_GENERATOR.get();
            case BIO    -> ModBlockEntities.BIO_GENERATOR.get();
        };
    }

    // ── TICK ──────────────────────────────────────────────────────────────────

    public void tickServer() {
        Level level = getLevel();
        if (level == null) return;

        boolean changed = false;

        switch (type) {
            case FOSSIL -> changed = tickFossil();
            case BIO    -> changed = tickBio();
            case SOLAR  -> changed = tickSolar(level);
        }

        // Push energy to all adjacent blocks every tick
        pushEnergyToNeighbors(level);

        if (changed) setChanged();
    }

    // ── FOSSIL ────────────────────────────────────────────────────────────────

    private boolean tickFossil() {
        boolean changed = false;

        // Consume fuel to refill burn timer
        if (burnTimeRemaining <= 0) {
            ItemStack fuel = fuelInventory.getItem(0);
            int burnTime = FuelRegistry.getFossilBurnTime(fuel);
            if (burnTime > 0 && !fuel.isEmpty()) {
                burnTimeRemaining = burnTime;
                burnTimeTotalForCurrentFuel = burnTime;
                fuel.shrink(1);
                fuelInventory.setItem(0, fuel);
                changed = true;
            }
        }

        // Generate while burning and buffer has room
        if (burnTimeRemaining > 0) {
            int generated = energy.receiveEnergy(tier.generatorOutput(), false);
            if (generated > 0) {
                burnTimeRemaining--;
                changed = true;
            }
        }

        return changed;
    }

    // ── BIO ───────────────────────────────────────────────────────────────────

    private boolean tickBio() {
        boolean changed = false;

        if (burnTimeRemaining <= 0) {
            ItemStack fuel = fuelInventory.getItem(0);
            int burnTime = FuelRegistry.getBioBurnTime(fuel);
            if (burnTime > 0 && !fuel.isEmpty()) {
                burnTimeRemaining = burnTime;
                burnTimeTotalForCurrentFuel = burnTime;
                fuel.shrink(1);
                fuelInventory.setItem(0, fuel);
                changed = true;
            }
        }

        if (burnTimeRemaining > 0) {
            int generated = energy.receiveEnergy(tier.generatorOutput(), false);
            if (generated > 0) {
                burnTimeRemaining--;
                changed = true;
            }
        }

        return changed;
    }

    // ── SOLAR ─────────────────────────────────────────────────────────────────

    private boolean tickSolar(Level level) {
        if (!level.isDay()) return false;
        if (!level.canSeeSky(worldPosition)) return false;
        if (level.isRaining() || level.isThundering()) return false;

        int generated = energy.receiveEnergy(tier.generatorOutput(), false);
        return generated > 0;
    }

    // ── ENERGY PUSH ───────────────────────────────────────────────────────────

    /**
     * Push energy outward to all 6 adjacent blocks.
     * Only pushes if the neighbor can receive — no energy is lost.
     */
    private void pushEnergyToNeighbors(Level level) {
        if (energy.getEnergyStored() <= 0) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);

            // Skip unloaded chunks
            if (!level.isLoaded(neighborPos)) continue;

            IEnergyStorage neighbor = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    level.getBlockState(neighborPos),
                    level.getBlockEntity(neighborPos),
                    dir.getOpposite()
            );

            if (neighbor == null || !neighbor.canReceive()) continue;

            int toSend = Math.min(energy.getEnergyStored(), tier.transferRate());
            int accepted = neighbor.receiveEnergy(toSend, false);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
                setChanged();
            }
        }
    }

    // ── CAPABILITY EXPOSURE ───────────────────────────────────────────────────

    /** Expose our energy storage as a capability. Called from ModCapabilities. */
    public PowerStorage getEnergyStorage() {
        return energy;
    }

    /** Expose our fuel inventory. Called from GUI menu provider. */
    public SimpleContainer getFuelInventory() {
        return fuelInventory;
    }

    // ── ACCESSORS (for GUI / HUD) ─────────────────────────────────────────────

    public GeneratorType getGeneratorType() { return type; }
    public PowerTier getTier()              { return tier; }
    public int getEnergyStored()            { return energy.getEnergyStored(); }
    public int getMaxEnergy()               { return energy.getMaxEnergyStored(); }
    public int getBurnTimeRemaining()       { return burnTimeRemaining; }
    public int getBurnTimeTotalForCurrentFuel() { return burnTimeTotalForCurrentFuel; }
    public boolean isBurning()              { return burnTimeRemaining > 0; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("BurnTime", burnTimeRemaining);
        tag.putInt("BurnTimeTotal", burnTimeTotalForCurrentFuel);

        // Save fuel slot
        if (!fuelInventory.getItem(0).isEmpty()) {
            tag.put("FuelSlot", fuelInventory.getItem(0).save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.setEnergy(tag.getInt("Energy"));
        burnTimeRemaining = tag.getInt("BurnTime");
        burnTimeTotalForCurrentFuel = tag.getInt("BurnTimeTotal");

        if (tag.contains("FuelSlot")) {
            ItemStack saved = ItemStack.parseOptional(registries, tag.getCompound("FuelSlot"));
            fuelInventory.setItem(0, saved);
        }
    }
}