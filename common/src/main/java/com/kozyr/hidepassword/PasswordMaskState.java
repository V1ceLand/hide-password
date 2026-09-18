package com.kozyr.hidepassword;

/**
 * Состояние «глазка»: показывать пароль или скрывать.
 * <p>
 * Намеренно не сохраняется между запусками: мод всегда стартует со скрытием,
 * иначе однажды включённый показ светил бы пароль и в следующей сессии.
 */
public final class PasswordMaskState {
	private static volatile boolean shown;

	private PasswordMaskState() {
	}

	public static boolean isShown() {
		return shown;
	}

	public static void toggle() {
		shown = !shown;
	}
}
