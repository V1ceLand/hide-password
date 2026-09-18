package com.kozyr.passwordmask.mixin;

import com.kozyr.passwordmask.client.EyeButton;
import com.kozyr.passwordmask.client.PasswordFields;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Серверные диалоги (например, экран «Вход» с полем «Пароль» от AuthMe):
 * ставит кнопку-«глазок» вплотную справа от парольного поля.
 * Сами звёздочки рисует {@link EditBoxMixin}.
 */
@Mixin(DialogScreen.class)
public abstract class DialogScreenMixin extends Screen {
	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void passwordmask$addEyeButton(CallbackInfo ci) {
		// Поля диалога лежат внутри скролл-контейнера, поэтому ищем рекурсивно.
		EditBox field = PasswordFields.findFirst(this.children());
		if (field != null) {
			this.addRenderableWidget(EyeButton.nextTo(field));
		}
	}
}
