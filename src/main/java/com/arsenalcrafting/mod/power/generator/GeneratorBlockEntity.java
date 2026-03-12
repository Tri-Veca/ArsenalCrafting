package com.arsenalcrafting.mod.power.generator;

import com.arsenalcrafting.mod.power.PowerTier;
import com.arsenalcrafting.mod.power.storage.PowerStorage;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratorBlockEntity extends BlockEntity {

    private GeneratorType type;
    private PowerTier tier;
    private final PowerStorage energy;

    // REQUIRED for NeoForge registration
    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state);
        this.type = GeneratorType.SOLAR;  // default scaffolding
        this.tier = PowerTier.COPPER;     // default scaffolding
        this.energy = new PowerStorage(
                tier.bankCapacity() / 10,
                tier.transferRate()
        );
    }

    // Optional constructor for convenience
    public GeneratorBlockEntity(BlockPos pos, BlockState state, GeneratorType type, PowerTier tier) {
        super(ModBlockEntities.GENERATOR.get(), pos, state);
        this.type = type;
        this.tier = tier;
        this.energy = new PowerStorage(
                tier.bankCapacity() / 10,
                tier.transferRate()
        );
    }

    public void tickServer() {
        Level level = getLevel();
        if (level == null) return;

        switch (type) {
            case SOLAR -> generateSolar(level);
            case BIO -> generateBio();
            case FOSSIL -> generateFossil();
        }
    }

    private void generateSolar(Level level) {
        if (!level.isDay()) return;
        if (!level.canSeeSky(worldPosition.above())) return;
        energy.receiveEnergy(tier.generatorOutput(), false);
    }

    private void generateBio() {
        // scaffolding placeholder
    }

    private void generateFossil() {
        // scaffolding placeholder
    }
}