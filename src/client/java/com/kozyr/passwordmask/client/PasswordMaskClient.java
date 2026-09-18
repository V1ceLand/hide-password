package com.kozyr.passwordmask.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PasswordMaskClient implements ClientModInitializer {
	public static final String MOD_ID = "passwordmask";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// Вся работа делается миксинами, здесь только отметка в логе.
		LOGGER.info("[Password Mask] loaded: passwords are hidden by default");
	}
}
