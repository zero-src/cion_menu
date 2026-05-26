package cion.menu.client;

import cion.menu.CionMenu;
import net.fabricmc.api.ClientModInitializer;

public class CionMenuClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CionMenuScreens.init();
		CionMenu.LOGGER.info("cion_menu client initialized");
	}
}
