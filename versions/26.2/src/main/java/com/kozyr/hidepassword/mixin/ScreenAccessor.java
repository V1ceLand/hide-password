package com.kozyr.hidepassword.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface ScreenAccessor {
	/** Список обработчиков ввода экрана: клик достаётся первому элементу под курсором. */
	@Accessor("children")
	List<GuiEventListener> hidepassword$getChildren();
}
