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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {
	@Unique
	private String hidepassword$label = "";

	@Inject(
			method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V",
			at = @At("TAIL"))
	private void hidepassword$addMaskFormatter(
			Font font, int x, int y, int width, int height, EditBox oldBox, Component narration, CallbackInfo ci) {
		this.hidepassword$label = narration == null ? "" : narration.getString();
		EditBox self = (EditBox) (Object) this;
		self.addFormatter((slice, offset) -> {
			if (PasswordMaskState.isShown()) {
				return null;
			}
			int start = PasswordMask.maskStart(self.getValue(), this.hidepassword$label);
			if (start < 0) {
				return null;
			}
			return FormattedCharSequence.forward(PasswordMask.maskSlice(slice, offset, start), Style.EMPTY);
		});
	}

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
