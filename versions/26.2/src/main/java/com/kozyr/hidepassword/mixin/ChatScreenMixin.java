package com.kozyr.hidepassword.mixin;

import com.kozyr.hidepassword.PasswordMask;
import com.kozyr.hidepassword.PasswordMaskState;
import com.kozyr.hidepassword.client.EyeButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Чат: кнопка-«глазок» в правом нижнем углу над строкой ввода (видна, пока вводится
 * команда с паролем); пароль не попадает в историю, черновик и озвучку.
 * Сами звёздочки рисует {@link EditBoxMixin}.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
	/** Строка ввода чата занимает нижние 14 px экрана. */
	@Unique
	private static final int INPUT_AREA_HEIGHT = 14;
	@Unique
	private static final int MARGIN = 2;

	@Shadow
	protected EditBox input;

	@Shadow
	@Final
	private static Component USAGE_TEXT;

	@Shadow
	@Final
	private static Component RESTRICTED_NARRATION_TEXT;

	@Shadow
	private ChatComponent.DisplayMode displayMode;

	@Unique
	private EyeButton hidepassword$eye;

	protected ChatScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	public abstract String normalizeChatMessage(String message);

	@Inject(method = "init", at = @At("TAIL"))
	private void hidepassword$addEyeButton(CallbackInfo ci) {
		this.hidepassword$eye = this.addRenderableWidget(EyeButton.placed(this::hidepassword$placeEye));
		this.hidepassword$updateEyeVisibility();
	}

	/**
	 * Правый нижний угол над строкой ввода. Если угол уже занят кнопками других модов
	 * (например, No Chat Reports), сдвигаемся левее них.
	 */
	@Unique
	private void hidepassword$placeEye(EyeButton eye) {
		int x = this.width - EyeButton.SIZE - MARGIN;
		int y = this.height - INPUT_AREA_HEIGHT - EyeButton.SIZE - MARGIN;
		boolean moved = true;
		while (moved && x > 0) {
			moved = false;
			for (GuiEventListener child : this.children()) {
				if (child != eye && child != this.input && child instanceof AbstractWidget other && other.visible
						&& other.getX() < x + EyeButton.SIZE && other.getRight() > x
						&& other.getY() < y + EyeButton.SIZE && other.getBottom() > y) {
					x = other.getX() - EyeButton.SIZE - MARGIN;
					moved = true;
				}
			}
		}
		eye.setPosition(x, y);
	}

	@Inject(method = "onEdited", at = @At("TAIL"))
	private void hidepassword$onEdited(String value, CallbackInfo ci) {
		this.hidepassword$updateEyeVisibility();
	}

	@Unique
	private void hidepassword$updateEyeVisibility() {
		if (this.hidepassword$eye != null) {
			this.hidepassword$eye.visible = PasswordMask.isPasswordCommand(this.input.getValue());
		}
	}

	/**
	 * Не сохраняем пароль в историю команд (стрелки вверх/вниз и command_history.txt),
	 * но отправляем его на сервер как обычно.
	 */
	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void hidepassword$skipHistory(String message, boolean addToRecent, CallbackInfo ci) {
		if (!addToRecent || !PasswordMask.isPasswordCommand(message)) {
			return;
		}
		String normalized = this.normalizeChatMessage(message);
		if (!normalized.isEmpty()) {
			if (normalized.startsWith("/")) {
				this.minecraft.player.connection.sendCommand(normalized.substring(1));
			} else {
				this.minecraft.player.connection.sendChat(normalized);
			}
		}
		ci.cancel();
	}

	/** Не даём паролю сохраниться как черновик чата. */
	@Inject(method = "removed", at = @At("HEAD"))
	private void hidepassword$clearDraft(CallbackInfo ci) {
		if (PasswordMask.isPasswordCommand(this.input.getValue())) {
			this.input.setValue("");
		}
	}

	/** Рассказчик (Narrator) не должен зачитывать пароль вслух. */
	@Inject(method = "updateNarrationState", at = @At("HEAD"), cancellable = true)
	private void hidepassword$maskNarration(NarrationElementOutput output, CallbackInfo ci) {
		output.add(NarratedElementType.TITLE, this.getTitle());
		if (this.displayMode.showRestrictedPrompt) {
			output.add(NarratedElementType.USAGE, CommonComponents.joinForNarration(USAGE_TEXT, RESTRICTED_NARRATION_TEXT));
		} else {
			output.add(NarratedElementType.USAGE, USAGE_TEXT);
		}

		String value = PasswordMaskState.isShown()
				? this.input.getValue()
				: PasswordMask.maskString(this.input.getValue());
		if (!value.isEmpty()) {
			output.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", value));
		}
		ci.cancel();
	}
}
