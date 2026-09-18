package com.kozyr.passwordmask.mixin;

import com.kozyr.passwordmask.PasswordMask;
import com.kozyr.passwordmask.PasswordMaskState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Маскирует пароль звёздочками в любом поле ввода: команды AuthMe в чате
 * ({@code /login ...}) и парольные поля серверных диалогов.
 * Реальное значение не меняется: звёздочки только на экране и в озвучке.
 */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {
	@Unique
	private String passwordmask$label = "";

	@Inject(
			method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V",
			at = @At("TAIL"))
	private void passwordmask$addMaskFormatter(
			Font font, int x, int y, int width, int height, EditBox oldBox, Component narration, CallbackInfo ci) {
		this.passwordmask$label = narration == null ? "" : narration.getString();
		EditBox self = (EditBox) (Object) this;
		// EditBox отдаёт форматтеру только видимый кусок строки и его смещение в полной строке.
		self.addFormatter((slice, offset) -> {
			if (PasswordMaskState.isShown()) {
				return null;
			}
			int start = PasswordMask.maskStart(self.getValue(), this.passwordmask$label);
			if (start < 0) {
				return null;
			}
			return FormattedCharSequence.forward(PasswordMask.maskSlice(slice, offset, start), Style.EMPTY);
		});
	}

	/** Рассказчик не должен зачитывать пароль вслух. */
	@Inject(method = "createNarrationMessage", at = @At("HEAD"), cancellable = true)
	private void passwordmask$maskNarration(CallbackInfoReturnable<MutableComponent> cir) {
		if (PasswordMaskState.isShown()) {
			return;
		}
		EditBox self = (EditBox) (Object) this;
		String value = self.getValue();
		int start = PasswordMask.maskStart(value, this.passwordmask$label);
		if (start >= 0) {
			cir.setReturnValue(Component.translatable("gui.narrate.editBox", self.getMessage(), PasswordMask.maskFrom(value, start)));
		}
	}
}
