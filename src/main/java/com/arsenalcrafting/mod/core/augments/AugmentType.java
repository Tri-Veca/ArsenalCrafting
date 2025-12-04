package com.arsenalcrafting.mod.core.augments;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

public enum AugmentType {

    // ========= EXISTING AUGMENTS =========

    AUTO_SMELT(
            "auto_smelt",
            ModComponents.AUTO_SMELT_ENABLED.get(),
            "augment.arsenalcrafting.auto_smelt",
            TestModeConfig::isAutoSmeltEnabled,
            new AugmentHandlers.AutoSmelt()
    ),

    ORE_MINER(
            "ore_miner",
            ModComponents.ORE_MINER_ENABLED.get(),
            "augment.arsenalcrafting.ore_miner",
            TestModeConfig::isOreMinerEnabled,
            new AugmentHandlers.OreMiner()
    ),

    FORTUNE(
            "fortune",
            ModComponents.FORTUNE_ENABLED.get(),
            "augment.arsenalcrafting.fortune",
            TestModeConfig::isFortuneEnabled,
            new AugmentHandlers.Fortune()
    );

    // ========= FUTURE AUGMENTS =========

//    MAGNETISM(
//            "magnetism",
//            ModComponents.MAGNETISM_ENABLED.get(),          // TODO: add this in ModComponents
//            "augment.arsenalcrafting.magnetism",            // TODO: add language key
//            TestModeConfig::isMagnetismEnabled,             // TODO: add to TestModeConfig
//            new AugmentHandlers.Magnet()                    // TODO: implement handler
//    ),

//    FORTUNE(
//            "fortune",
//            ModComponents.FORTUNE_ENABLED.get(),            // TODO
//            "augment.arsenalcrafting.fortune",
//            TestModeConfig::isFortuneEnabled,               // TODO
//            new AugmentHandlers.Fortune()                   // TODO
//    ),


    // ========= FIELDS =========

    private final String id;
    private final DataComponentType<Boolean> flag;
    private final String translationKey;
    private final BooleanSupplier testModeFlag;
    private final AugmentHandler handler;

    AugmentType(String id,
                DataComponentType<Boolean> flag,
                String translationKey,
                BooleanSupplier testModeFlag,
                AugmentHandler handler) {
        this.id = id;
        this.flag = flag;
        this.translationKey = translationKey;
        this.testModeFlag = testModeFlag;
        this.handler = handler;
    }

    public String id() {
        return id;
    }

    public DataComponentType<Boolean> flag() {
        return flag;
    }

    public String translationKey() {
        return translationKey;
    }

    public AugmentHandler handler() {
        return handler;
    }

    public boolean isOn(ItemStack stack) {
        return stack.getOrDefault(flag, false);
    }

    public boolean isEnabledInTestMode() {
        return testModeFlag != null && testModeFlag.getAsBoolean();
    }
}
