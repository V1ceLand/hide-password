package com.kozyr.hidepassword;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Чистая логика маскировки пароля, без зависимостей от Minecraft.
 * Паролем считается всё после команды AuthMe-подобного плагина:
 * /login, /l, /register, /reg, /changepassword и т.п.
 */
public final class PasswordMask {
	public static final char MASK_CHAR = '*';

	private static final Pattern COMMAND_PATTERN =
			Pattern.compile("^\\s*/([A-Za-z]+)(\\s.*)?$", Pattern.DOTALL);

	/** Команды, после которых идёт пароль (AuthMe и аналоги). */
	private static final Set<String> PASSWORD_COMMANDS = Set.of(
			"login", "l", "log",
			"register", "reg",
			"changepassword", "changepass", "changepw", "cp",
			"unregister", "unreg",
			"auth", "email");

	private PasswordMask() {
	}

	/**
	 * Индекс первого символа пароля в строке или -1, если пароля нет.
	 * Возвращает -1 также для голой команды без аргументов (маскировать нечего).
	 */
	public static int passwordStart(String text) {
		if (text == null || text.isEmpty()) {
			return -1;
		}
		Matcher m = COMMAND_PATTERN.matcher(text);
		if (!m.matches()) {
			return -1;
		}
		if (!PASSWORD_COMMANDS.contains(m.group(1).toLowerCase(Locale.ROOT))) {
			return -1;
		}
		if (m.group(2) == null) {
			return -1;
		}
		int i = text.indexOf(m.group(1)) + m.group(1).length();
		while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
			i++;
		}
		return i < text.length() ? i : -1;
	}

	/** true, если строка — команда ввода пароля (с аргументами или без). */
	public static boolean isPasswordCommand(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		Matcher m = COMMAND_PATTERN.matcher(text);
		return m.matches() && PASSWORD_COMMANDS.contains(m.group(1).toLowerCase(Locale.ROOT));
	}

	/** Вся строка с замаскированным паролем (длина сохраняется 1:1). */
	public static String maskString(String text) {
		return maskFrom(text, passwordStart(text));
	}

	/** Маскирует символы начиная с индекса start (длина сохраняется 1:1). */
	public static String maskFrom(String text, int start) {
		if (text == null || start < 0 || start >= text.length()) {
			return text;
		}
		StringBuilder sb = new StringBuilder(text);
		for (int i = start; i < sb.length(); i++) {
			sb.setCharAt(i, MASK_CHAR);
		}
		return sb.toString();
	}

	/** true, если подпись поля похожа на «пароль» (AuthMe-диалоги присылают label «Пароль»). */
	public static boolean isPasswordLabelText(String labelText) {
		if (labelText == null || labelText.isEmpty()) {
			return false;
		}
		String lower = labelText.toLowerCase(Locale.ROOT);
		if (lower.contains("пароль")
				|| lower.contains("password")
				|| lower.contains("passwd")
				|| lower.contains("passwort")
				|| lower.contains("contraseña")
				|| lower.contains("senha")
				|| lower.contains("hasło")) {
			return true;
		}
		for (String token : lower.split("[^\\p{L}0-9]+")) {
			if (token.equals("pass")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Индекс первого маскируемого символа с учётом подписи поля:
	 * пароль после команды либо всё значение, если подпись — «пароль».
	 */
	public static int maskStart(String value, String labelText) {
		int start = passwordStart(value);
		if (start >= 0) {
			return start;
		}
		if (value != null && !value.isEmpty() && isPasswordLabelText(labelText)) {
			return 0;
		}
		return -1;
	}

	/**
	 * Маскирует срез строки для отрисовки.
	 * EditBox отдаёт форматтеру только видимый кусок + абсолютный offset,
	 * поэтому маска вычисляется относительно полной строки.
	 *
	 * @param slice  видимый кусок строки
	 * @param offset индекс первого символа {@code slice} в полной строке
	 * @param start  результат {@link #maskStart(String, String)}, -1 = не маскировать
	 */
	public static String maskSlice(String slice, int offset, int start) {
		if (start < 0) {
			return slice;
		}
		StringBuilder sb = new StringBuilder(slice);
		for (int i = 0; i < sb.length(); i++) {
			if (offset + i >= start) {
				sb.setCharAt(i, MASK_CHAR);
			}
		}
		return sb.toString();
	}
}
