package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.client.gui.AlloySmelterScreen;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public class AlloySmelterGuiHandler implements IGuiContainerHandler<AlloySmelterScreen> {
    @Override
    public List<Rect2i> getGuiExtraAreas(AlloySmelterScreen screen) {
        return screen.getGuiExtraAreas();
    }
}