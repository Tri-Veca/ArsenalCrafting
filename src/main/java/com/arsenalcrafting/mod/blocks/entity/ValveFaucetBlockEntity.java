package com.arsenalcrafting.mod.blocks.entity;


import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.CastingDatabase;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

public class ValveFaucetBlockEntity extends BlockEntity {

    // MB to transfer per tick — gradual like Tinkers
    public static final int MB_PER_TICK = 50;

    public enum PourState { OFF, POURING }

    private PourState pourState = PourState.OFF;

    // Fluid currently being poured — used for rendering the stream
    private String pouringMaterialName = null;
    private int remainingToPour = 0; // mb still to transfer to casting station

    public ValveFaucetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VALVE_FAUCET.get(), pos, state);
    }

    // ── ACCESSORS ─────────────────────────────────────────────────────────────

    public PourState getPourState()        { return pourState; }
    public boolean isPouring()             { return pourState == PourState.POURING; }
    public String getPouringMaterialName() { return pouringMaterialName; }
    public int getRemainingToPour()        { return remainingToPour; }

    // ── ACTIVATE ──────────────────────────────────────────────────────────────

    /**
     * Called on right click. Starts pouring if conditions are met.
     * Returns a message to show the player, or null on success.
     */
    public String activate(Level level, BlockPos pos) {
        if (isPouring()) {
            // Toggle off
            reset();
            return "Faucet closed";
        }

        // Find adjacent smelter
        AlloySmelterBlockEntity smelter = findAdjacentSmelter(level, pos);
        if (smelter == null) return "No Alloy Smelter found adjacent to faucet";

        // Find casting station below
        BlockPos belowPos = pos.below();
        if (!(level.getBlockEntity(belowPos) instanceof CastingStationBlockEntity station)) {
            return "No Casting Station found below faucet";
        }

        if (!station.canReceiveFluid()) {
            if (station.isReady())    return "Casting station has an item ready — collect it first";
            if (station.isSetting())  return "Already setting...";
            if (station.getMode() == CastingStationBlockEntity.CastMode.NONE)
                return "Place a mold or empty bucket in the casting station first";
            return "Casting station already has fluid";
        }

        // Determine fluid to pour
        FluidStack toPour = resolveCastTarget(smelter);
        if (toPour == null || toPour.isEmpty()) {
            if (smelter.castTargetFluid != null && !smelter.castTargetFluid.isEmpty()) {
                return "Cast target fluid (" + smelter.castTargetFluid + ") not found in tank";
            }
            return "No fluid in the Alloy Smelter output tank";
        }

        AlloyMaterial mat = AlloyMaterial.fromFluid(toPour.getFluid());
        if (mat == null) return "Unknown fluid type";

        // Check enough fluid for the mold
        String moldId = station.getMode() == CastingStationBlockEntity.CastMode.MOLD
                ? station.getMoldComponentId() : "ingot";
        int needed = CastingDatabase.getFluidCost(moldId != null ? moldId : "ingot");

        if (toPour.getAmount() < needed) {
            return "Not enough fluid — need " + needed + "mb, have " + toPour.getAmount() + "mb";
        }

        // Start pouring
        this.pourState          = PourState.POURING;
        this.pouringMaterialName = mat.getName();
        this.remainingToPour    = needed;
        setChanged();
        syncToClient(level);
        return null; // null = success
    }

    // ── TICK ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos,
                            BlockState state, ValveFaucetBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isPouring()) return;

        AlloySmelterBlockEntity smelter = be.findAdjacentSmelter(level, pos);
        if (smelter == null) { be.reset(); return; }

        BlockPos belowPos = pos.below();
        if (!(level.getBlockEntity(belowPos) instanceof CastingStationBlockEntity station)) {
            be.reset(); return;
        }

        // Drain MB_PER_TICK from smelter this tick
        int toDrain = Math.min(MB_PER_TICK, be.remainingToPour);
        AlloyMaterial mat = AlloyMaterial.fromName(be.pouringMaterialName);
        if (mat == null) { be.reset(); return; }

        var fluidSource = com.arsenalcrafting.mod.registry.ModFluids.getSource(mat);
        if (fluidSource == null) { be.reset(); return; }

        // Check smelter still has enough
        boolean hasFluid = smelter.getAllOutputFluids().stream()
                .anyMatch(f -> AlloyMaterial.fromFluid(f.getFluid()) == mat
                        && f.getAmount() >= toDrain);

        if (!hasFluid) { be.reset(); return; }

        smelter.drainFromOutputTank(fluidSource, toDrain);
        be.remainingToPour -= toDrain;

        be.syncToClient(level);

        // Done — notify casting station
        if (be.remainingToPour <= 0) {
            int needed = CastingDatabase.getFluidCost(
                    station.getMode() == CastingStationBlockEntity.CastMode.MOLD
                            && station.getMoldComponentId() != null
                            ? station.getMoldComponentId() : "ingot");
            station.receiveFluid(be.pouringMaterialName, needed);
            be.reset();
        }

        be.setChanged();
    }

    private void reset() {
        pourState         = PourState.OFF;
        pouringMaterialName = null;
        remainingToPour   = 0;
        setChanged();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private FluidStack resolveCastTarget(AlloySmelterBlockEntity smelter) {
        String castTarget = smelter.castTargetFluid;
        if (castTarget != null && !castTarget.isEmpty()) {
            for (FluidStack fluid : smelter.getAllOutputFluids()) {
                AlloyMaterial mat = AlloyMaterial.fromFluid(fluid.getFluid());
                if (mat != null && mat.getName().equals(castTarget)) return fluid;
            }
            return null;
        }
        // No cast target — use first available
        var fluids = smelter.getAllOutputFluids();
        return fluids.isEmpty() ? null : fluids.get(0);
    }

    public AlloySmelterBlockEntity findAdjacentSmelter(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : new net.minecraft.core.Direction[]{
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.EAST,  net.minecraft.core.Direction.WEST,
                net.minecraft.core.Direction.UP}) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof AlloySmelterBlockEntity s) {
                return s;
            }
        }
        return null;
    }

    // ── RENDER BOUNDING BOX ───────────────────────────────────────────────────
    // Extend downward so the stream renders even when looking from below



    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("PourState", pourState.name());
        if (pouringMaterialName != null) tag.putString("PouringMaterial", pouringMaterialName);
        tag.putInt("RemainingToPour", remainingToPour);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try { pourState = PourState.valueOf(tag.getString("PourState")); }
        catch (Exception e) { pourState = PourState.OFF; }
        pouringMaterialName = tag.contains("PouringMaterial") ? tag.getString("PouringMaterial") : null;
        remainingToPour     = tag.getInt("RemainingToPour");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("PourState", pourState.name());
        if (pouringMaterialName != null) tag.putString("PouringMaterial", pouringMaterialName);
        tag.putInt("RemainingToPour", remainingToPour);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient(Level level) {
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}