package com.kozyr.passwordmask.client;

import com.kozyr.passwordmask.PasswordMaskState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * Квадратная кнопка-«глазок» без подписи: показывает или скрывает пароль.
 * <p>
 * Значок отражает действие: открытый глаз — «показать», перечёркнутый — «скрыть».
 * Текст остаётся только в подсказке и для рассказчика.
 */
public final class EyeButton extends Button {
	public static final int SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int GAP = 4;
	private static final Identifier ICON_SHOW = icon("eye");
	private static final Identifier ICON_HIDE = icon("eye_slash");

	/** Поле, к которому кнопка привязана справа; null — фиксированная позиция. */
	private final @Nullable EditBox anchor;

	private EyeButton(int x, int y, @Nullable EditBox anchor) {
		super(x, y, SIZE, SIZE, currentMessage(), EyeButton::toggle, DEFAULT_NARRATION);
		this.anchor = anchor;
		this.setTooltip(Tooltip.create(Component.translatable("passwordmask.eye.tooltip")));
		this.followAnchor();
	}

	/** Кнопка в заданной точке экрана. */
	public static EyeButton at(int x, int y) {
		return new EyeButton(x, y, null);
	}

	/** Кнопка вплотную справа от поля; следует за ним, если поле сдвинется (прокрутка, ресайз). */
	public static EyeButton nextTo(EditBox field) {
		return new EyeButton(0, 0, field);
	}

	private static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(PasswordMaskClient.MOD_ID, "icon/" + name);
	}

	private static Component currentMessage() {
		return Component.translatable(PasswordMaskState.isShown() ? "passwordmask.eye.hide" : "passwordmask.eye.show");
	}

	private static void toggle(Button button) {
		PasswordMaskState.toggle();
		button.setMessage(currentMessage());
	}

	private void followAnchor() {
		if (this.anchor != null) {
			this.setPosition(this.anchor.getRight() + GAP, this.anchor.getY() + (this.anchor.getHeight() - SIZE) / 2);
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.followAnchor();
		this.extractDefaultSprite(graphics);
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
