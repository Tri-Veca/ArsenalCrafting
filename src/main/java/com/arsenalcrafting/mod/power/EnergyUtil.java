package com.arsenalcrafting.mod.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Optional;

public class EnergyUtil {

    public static Optional<IEnergyStorage> getEnergy(Level level, BlockPos pos, Direction side) {

        IEnergyStorage storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                pos,
                level.getBlockState(pos),
                level.getBlockEntity(pos),
                side
        );

        return Optional.ofNullable(storage);
    }
}