package com.kozyr.hidepassword.client;

import com.kozyr.hidepassword.PasswordMaskState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Квадратная кнопка-«глазок» без подписи: показывает или скрывает пароль.
 */
public final class EyeButton extends Button {
	public static final int SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final WidgetSprites SPRITES = new WidgetSprites(
			ResourceLocation.withDefaultNamespace("widget/button"),
			ResourceLocation.withDefaultNamespace("widget/button_disabled"),
			ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
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
		return ResourceLocation.fromNamespaceAndPath(HidePasswordClient.MOD_ID, "icon/" + name);
	}

	private static Component currentMessage() {
		return Component.translatable(PasswordMaskState.isShown() ? "hidepassword.eye.hide" : "hidepassword.eye.show");
	}

	private static void toggle(Button button) {
		PasswordMaskState.toggle();
		button.setMessage(currentMessage());
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.placement.accept(this);
		graphics.blitSprite(
				RenderType::guiTextured,
				SPRITES.get(this.active, this.isHoveredOrFocused()),
				this.getX(),
				this.getY(),
				this.getWidth(),
				this.getHeight(),
				ARGB.white(this.alpha));

		ResourceLocation icon = PasswordMaskState.isShown() ? ICON_HIDE : ICON_SHOW;
		graphics.blitSprite(
				RenderType::guiTextured,
				icon,
				this.getX() + (this.getWidth() - ICON_SIZE) / 2,
				this.getY() + (this.getHeight() - ICON_SIZE) / 2,
				ICON_SIZE,
				ICON_SIZE,
				ARGB.white(this.alpha));
	}
}
