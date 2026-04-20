package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FoundryBasinBlockEntity extends BlockEntity {

    private static final int FLUID_PER_BLOCK = 9000; // 9 ingots = 1 block
    private static final int BASE_SETTING_TICKS = 100;

    private String fluidMaterialName = null;
    private int fluidAmount = 0;
    private int settingProgress = 0;
    private int settingTime = 0;
    private boolean isSetting = false;
    private boolean isReady = false;

    public FoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN.get(), pos, state);
    }

    public boolean canReceiveFluid() {
        return !isSetting && !isReady && fluidAmount == 0;
    }

    public boolean isSetting()          { return isSetting; }
    public boolean isReady()            { return isReady; }
    public String getFluidMaterialName(){ return fluidMaterialName; }
    public int getFluidAmount()         { return fluidAmount; }
    public int getFluidNeeded()         { return FLUID_PER_BLOCK; }
    public int getSettingProgress()     { return settingProgress; }
    public int getSettingTime()         { return settingTime; }

    public void receiveFluid(String materialName, int amount, float speedMultiplier) {
        this.fluidMaterialName = materialName;
        this.fluidAmount = amount;
        this.settingTime = Math.max(20, Math.round(BASE_SETTING_TICKS / speedMultiplier));
        this.settingProgress = 0;
        this.isSetting = true;
        this.isReady = false;
        setChanged();
        syncToClient();
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            FoundryBasinBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isSetting) return;

        be.settingProgress++;
        if (be.settingProgress >= be.settingTime) {
            be.isSetting = false;
            be.isReady = true;
            be.settingProgress = be.settingTime;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        be.setChanged();
    }

    public ItemStack collectOutput() {
        if (!isReady || fluidMaterialName == null) return ItemStack.EMPTY;

        AlloyMaterial mat = AlloyMaterial.fromName(fluidMaterialName);
        if (mat == null) return ItemStack.EMPTY;

        ItemStack result = switch (mat) {
            case COPPER    -> new ItemStack(Items.COPPER_BLOCK);
            case IRON      -> new ItemStack(Items.IRON_BLOCK);
            case GOLD      -> new ItemStack(Items.GOLD_BLOCK);
            case DIAMOND   -> new ItemStack(Items.DIAMOND_BLOCK);
            case NETHERITE -> new ItemStack(Items.NETHERITE_BLOCK);
            case COAL      -> new ItemStack(Items.COAL_BLOCK);
            default        -> new ItemStack(Items.IRON_BLOCK);//ItemStackHelper.getAlloyBlock(mat); // modded alloys
        };

        fluidMaterialName = null;
        fluidAmount = 0;
        isReady = false;
        settingProgress = 0;
        settingTime = 0;
        isSetting = false;

        setChanged();
        syncToClient();
        return result;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (fluidMaterialName != null) tag.putString("FluidMaterial", fluidMaterialName);
        tag.putInt("FluidAmount", fluidAmount);
        tag.putInt("SettingProgress", settingProgress);
        tag.putInt("SettingTime", settingTime);
        tag.putBoolean("IsSetting", isSetting);
        tag.putBoolean("IsReady", isReady);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidMaterialName = tag.contains("FluidMaterial") ? tag.getString("FluidMaterial") : null;
        fluidAmount       = tag.getInt("FluidAmount");
        settingProgress   = tag.getInt("SettingProgress");
        settingTime       = tag.getInt("SettingTime");
        isSetting         = tag.getBoolean("IsSetting");
        isReady           = tag.getBoolean("IsReady");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (fluidMaterialName != null) tag.putString("FluidMaterial", fluidMaterialName);
        tag.putInt("FluidAmount", fluidAmount);
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
}