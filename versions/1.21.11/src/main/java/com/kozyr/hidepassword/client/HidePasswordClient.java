package com.kozyr.hidepassword.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HidePasswordClient implements ClientModInitializer {
	public static final String MOD_ID = "hidepassword";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("[Hide Password] loaded: passwords are hidden by default");
		if (Boolean.getBoolean("hidepassword.selftest")) {
			selfTest();
		}
	}

	private static void selfTest() {
		for (Class<?> target : new Class<?>[] {EditBox.class, ChatScreen.class, DialogScreen.class}) {
			LOGGER.info("[Hide Password] selftest: {} loaded", target.getName());
		}
		LOGGER.info("[Hide Password] SELFTEST OK");
		System.exit(0);
	}
}
