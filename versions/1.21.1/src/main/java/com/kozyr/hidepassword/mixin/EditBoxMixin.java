package com.kozyr.hidepassword.mixin;

import com.kozyr.hidepassword.PasswordMask;
import com.kozyr.hidepassword.PasswordMaskState;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/**
 * Маскирует пароль звёздочками в любом поле ввода: команды AuthMe в чате
 * ({@code /login ...}) и парольные поля серверных диалогов.
 * Реальное значение не меняется: звёздочки только на экране и в озвучке.
 * <p>
 * В этих версиях у EditBox один форматтер, и чат ставит свой (подсветка команд)
 * через {@code setFormatter}. Поэтому маска оборачивает любой форматтер, который
 * ставится в поле, а не добавляется рядом с ним.
 */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {
	@Unique
	private String hidepassword$label = "";

	@Inject(
			method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V",
			at = @At("TAIL"))
	private void hidepassword$init(
			Font font, int x, int y, int width, int height, EditBox oldBox, Component narration, CallbackInfo ci) {
		this.hidepassword$label = narration == null ? "" : narration.getString();
		// Стандартный форматтер пропускаем через setFormatter, чтобы его обернула маска.
		((EditBox) (Object) this).setFormatter((text, offset) -> FormattedCharSequence.forward(text, Style.EMPTY));
	}

	@ModifyVariable(method = "setFormatter", at = @At("HEAD"), argsOnly = true)
	private BiFunction<String, Integer, FormattedCharSequence> hidepassword$wrapFormatter(
			BiFunction<String, Integer, FormattedCharSequence> original) {
		EditBox self = (EditBox) (Object) this;
		// EditBox отдаёт форматтеру только видимый кусок строки и его смещение в полной строке.
		return (slice, offset) -> {
			if (!PasswordMaskState.isShown()) {
				int start = PasswordMask.maskStart(self.getValue(), this.hidepassword$label);
				if (start >= 0) {
					return FormattedCharSequence.forward(PasswordMask.maskSlice(slice, offset, start), Style.EMPTY);
				}
			}
			return original.apply(slice, offset);
		};
	}

	/** Рассказчик не должен зачитывать пароль вслух. */
	@Inject(method = "createNarrationMessage", at = @At("HEAD"), cancellable = true)
	private void hidepassword$maskNarration(CallbackInfoReturnable<MutableComponent> cir) {
		if (PasswordMaskState.isShown()) {
			return;
		}
		EditBox self = (EditBox) (Object) this;
		String value = self.getValue();
		int start = PasswordMask.maskStart(value, this.hidepassword$label);
		if (start >= 0) {
			cir.setReturnValue(Component.translatable("gui.narrate.editBox", self.getMessage(), PasswordMask.maskFrom(value, start)));
		}
	}
}
