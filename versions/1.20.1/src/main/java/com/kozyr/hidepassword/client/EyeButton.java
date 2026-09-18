package com.kozyr.hidepassword.client;

import com.kozyr.hidepassword.PasswordMaskState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Квадратная кнопка-«глазок» без подписи: показывает или скрывает пароль.
 */
public final class EyeButton extends Button {
	public static final int SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final ResourceLocation ICON_SHOW = icon("eye");
	private static final ResourceLocation ICON_HIDE = icon("eye_slash");

	private final Consumer<EyeButton> placement;

	private EyeButton(Consumer<EyeButton> placement) {
		super(0, 0, SIZE, SIZE, currentMessage(), EyeButton::toggle, DEFAULT_NARRATION);
		this.placement = placement;
		this.setTooltip(Tooltip.create(Component.translatable("hidepassword.eye.tooltip")));
		this.placement.accept(this);
	}

	public static EyeButton placed(Consumer<EyeButton> placement) {
		return new EyeButton(placement);
	}

	private static ResourceLocation icon(String name) {
		return new ResourceLocation(HidePasswordClient.MOD_ID, "textures/gui/sprites/icon/" + name + ".png");
	}

	private static Component currentMessage() {
		return Component.translatable(PasswordMaskState.isShown() ? "hidepassword.eye.hide" : "hidepassword.eye.show");
	}

	private static void toggle(Button button) {
		PasswordMaskState.toggle();
		button.setMessage(currentMessage());
	}

	private int getTextureY() {
		int i = 1;
		if (!this.active) {
			i = 0;
		} else if (this.isHoveredOrFocused()) {
			i = 2;
		}
		return 46 + i * 20;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.placement.accept(this);
		graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		graphics.blitNineSliced(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		ResourceLocation icon = PasswordMaskState.isShown() ? ICON_HIDE : ICON_SHOW;
		graphics.blit(icon,
				this.getX() + (this.getWidth() - ICON_SIZE) / 2,
				this.getY() + (this.getHeight() - ICON_SIZE) / 2,
				0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}
}
