package com.kozyr.hidepassword.client;

import com.kozyr.hidepassword.PasswordMaskState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Квадратная кнопка-«глазок» без подписи: показывает или скрывает пароль.
 */
public final class EyeButton extends Button {
	public static final int SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int GAP = 4;
	private static final Identifier ICON_SHOW = icon("eye");
	private static final Identifier ICON_HIDE = icon("eye_slash");

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

	public static EyeButton nextTo(EditBox field) {
		return new EyeButton(button -> button.setPosition(
				field.getRight() + GAP,
				field.getY() + (field.getHeight() - SIZE) / 2));
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(HidePasswordClient.MOD_ID, "icon/" + name);
	}

	private static Component currentMessage() {
		return Component.translatable(PasswordMaskState.isShown() ? "hidepassword.eye.hide" : "hidepassword.eye.show");
	}

	private static void toggle(Button button) {
		PasswordMaskState.toggle();
		button.setMessage(currentMessage());
	}

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.placement.accept(this);
		this.renderDefaultSprite(graphics);
		graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				PasswordMaskState.isShown() ? ICON_HIDE : ICON_SHOW,
				this.getX() + (this.getWidth() - ICON_SIZE) / 2,
				this.getY() + (this.getHeight() - ICON_SIZE) / 2,
				ICON_SIZE,
				ICON_SIZE,
				ARGB.white(this.alpha));
	}
}
