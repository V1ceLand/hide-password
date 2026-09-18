package com.kozyr.passwordmask.client;

import com.kozyr.passwordmask.PasswordMask;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Поиск парольных полей на экране, включая вложенные в контейнеры и скроллы. */
public final class PasswordFields {
	private PasswordFields() {
	}

	/** Первое парольное поле среди виджетов (обход в глубину) или null. */
	public static @Nullable EditBox findFirst(List<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			if (widget instanceof EditBox box && isPasswordField(box)) {
				return box;
			}
			if (widget instanceof ContainerEventHandler container) {
				EditBox nested = findFirst(container.children());
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	private static boolean isPasswordField(EditBox box) {
		Component message = box.getMessage();
		String label = message == null ? "" : message.getString();
		return PasswordMask.isPasswordLabelText(label) || PasswordMask.isPasswordCommand(box.getValue());
	}
}
