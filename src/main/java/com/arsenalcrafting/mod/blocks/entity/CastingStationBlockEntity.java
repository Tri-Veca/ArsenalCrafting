package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.CastingDatabase;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CastingStationBlockEntity extends BlockEntity {

    // Mode — either mold casting or bucket casting, not both
    public enum CastMode { NONE, MOLD, BUCKET }

    private CastMode mode = CastMode.NONE;

    // Mold state (used when mode == MOLD)
    private String moldComponentId = null;
    private boolean isBronzeMold   = false;

    // Fluid state
    private String fluidMaterialName = null;
    private int    fluidAmount       = 0;      // mb poured in
    private int    fluidNeeded       = 0;      // mb needed to start setting

    // Setting progress
    private int     settingProgress = 0;
    private int     settingTime     = 0;
    private boolean isSetting       = false;
    private boolean isReady         = false;

    // Add this method to CastingStationBlockEntity.java
    public boolean hasFluid() {
        return fluidAmount > 0;
    }

    public CastingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASTING_STATION.get(), pos, state);
    }

    // ── MODE & SLOT STATE ─────────────────────────────────────────────────────

    public CastMode getMode()          { return mode; }
    public boolean hasMold()           { return mode == CastMode.MOLD && moldComponentId != null; }
    public boolean hasBucket()         { return mode == CastMode.BUCKET; }
    public boolean isSetting()         { return isSetting; }
    public boolean isReady()           { return isReady; }
    public String getMoldComponentId() { return moldComponentId; }
    public boolean isBronzeMold()      { return isBronzeMold; }
    public String getFluidMaterialName(){ return fluidMaterialName; }
    public int getFluidAmount()        { return fluidAmount; }
    public int getFluidNeeded()        { return fluidNeeded; }
    public int getSettingProgress()    { return settingProgress; }
    public int getSettingTime()        { return settingTime; }

    public void setMold(String componentId, boolean bronze) {
        if (mode != CastMode.NONE) return; // already occupied
        this.mode          = CastMode.MOLD;
        this.moldComponentId = componentId;
        this.isBronzeMold    = bronze;
        this.fluidNeeded     = CastingDatabase.getFluidCost(componentId);
        setChanged(); syncToClient();
    }

    public void clearMold() {
        this.mode            = CastMode.NONE;
        this.moldComponentId = null;
        this.isBronzeMold    = false;
        this.fluidNeeded     = 0;
        setChanged(); syncToClient();
    }

    public void setBucket() {
        if (mode != CastMode.NONE) return;
        this.mode        = CastMode.BUCKET;
        this.fluidNeeded = 1000; // always 1 ingot worth for bucket
        setChanged(); syncToClient();
    }

    public void clearBucket() {
        this.mode        = CastMode.NONE;
        this.fluidNeeded = 0;
        setChanged(); syncToClient();
    }

    // ── FLUID POUR ────────────────────────────────────────────────────────────

    public boolean canReceiveFluid() {
        if (isReady) return false;           // Can't receive if ready to collect
        if (isSetting) return false;         // Can't receive while setting
        if (mode == CastMode.NONE) return false;  // No mold or bucket
        if (hasFluid()) return false;        // Already has fluid
        return true;
    }

    public void receiveFluid(String materialName, int amount) {
        this.fluidMaterialName = materialName;
        this.fluidAmount       = amount;

        AlloyMaterial mat = AlloyMaterial.fromName(materialName);
        if (mat != null) {
            String moldId = mode == CastMode.MOLD ? moldComponentId : "ingot";
            this.settingTime     = CastingDatabase.getSettingTicks(mat, moldId);
            this.settingProgress = 0;
            this.isSetting       = true;
        }
        setChanged(); syncToClient();
    }




    // ── TICK ─────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos,
                            BlockState state, CastingStationBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isSetting) return;

        be.settingProgress++;
        if (be.settingProgress >= be.settingTime) {
            be.isSetting       = false;
            be.isReady         = true;
            be.settingProgress = be.settingTime;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        be.setChanged();
    }



    // ── COLLECT ───────────────────────────────────────────────────────────────

    /**
     * Called on player right click when isReady.
     * Returns the output item. Caller is responsible for giving it to player
     * and returning the bucket/mold correctly.
     */
    public ItemStack collectOutput() {
        if (!isReady || fluidMaterialName == null) return ItemStack.EMPTY;

        AlloyMaterial mat = AlloyMaterial.fromName(fluidMaterialName);
        if (mat == null) return ItemStack.EMPTY;

        ItemStack result = ItemStack.EMPTY;

        if (mode == CastMode.BUCKET) {
            // Return alloy bucket
            var bucketHolder = ModItems.ALLOY_BUCKETS.get(mat);
            if (bucketHolder != null) result = new ItemStack(bucketHolder.get());
        } else if (mode == CastMode.MOLD) {
            if (moldComponentId != null) {
                if (moldComponentId.equals("ingot")) {
                    // Ingot mold → return alloy ingot
                    var ingotHolder = ModItems.ALLOY_ITEMS.get(mat);
                    if (ingotHolder != null) result = new ItemStack(ingotHolder.get());
                } else {
                    // Tool component mold
                    ToolComponentType type = ToolComponentType.fromId(moldComponentId);
                    if (type != null) {
                        var componentMap = ModItems.TOOL_COMPONENTS.get(mat);
                        if (componentMap != null) {
                            var holder = componentMap.get(type);
                            if (holder != null) result = new ItemStack(holder.get());
                        }
                    }
                }
            }
        }

        // Reset fluid state — mold stays, mode stays
        fluidMaterialName = null;
        fluidAmount       = 0;
        isReady           = false;
        settingProgress   = 0;
        settingTime       = 0;

        setChanged(); syncToClient();
        return result;
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Mode", mode.name());
        if (moldComponentId != null) tag.putString("MoldComponent", moldComponentId);
        tag.putBoolean("IsBronzeMold", isBronzeMold);
        if (fluidMaterialName != null) tag.putString("FluidMaterial", fluidMaterialName);
        tag.putInt("FluidAmount", fluidAmount);
        tag.putInt("FluidNeeded", fluidNeeded);
        tag.putInt("SettingProgress", settingProgress);
        tag.putInt("SettingTime", settingTime);
        tag.putBoolean("IsSetting", isSetting);
        tag.putBoolean("IsReady", isReady);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try { mode = CastMode.valueOf(tag.getString("Mode")); }
        catch (Exception e) { mode = CastMode.NONE; }
        moldComponentId  = tag.contains("MoldComponent") ? tag.getString("MoldComponent") : null;
        isBronzeMold     = tag.getBoolean("IsBronzeMold");
        fluidMaterialName= tag.contains("FluidMaterial") ? tag.getString("FluidMaterial") : null;
        fluidAmount      = tag.getInt("FluidAmount");
        fluidNeeded      = tag.getInt("FluidNeeded");
        settingProgress  = tag.getInt("SettingProgress");
        settingTime      = tag.getInt("SettingTime");
        isSetting        = tag.getBoolean("IsSetting");
        isReady          = tag.getBoolean("IsReady");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("Mode", mode.name());
        if (moldComponentId != null) tag.putString("MoldComponent", moldComponentId);
        tag.putBoolean("IsBronzeMold", isBronzeMold);
        if (fluidMaterialName != null) tag.putString("FluidMaterial", fluidMaterialName);
        tag.putInt("FluidAmount", fluidAmount);
        tag.putInt("FluidNeeded", fluidNeeded);
        tag.putInt("SettingProgress", settingProgress);
        tag.putInt("SettingTime", settingTime);
        tag.putBoolean("IsSetting", isSetting);
        tag.putBoolean("IsReady", isReady);
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

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}