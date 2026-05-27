package cion.menu.client;

import cion.menu.CionMenu;
import cion.menu.client.gui.CionConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class CionMenuScreens {
	private static final Component TITLE = Component.translatable("cion_menu.title");
	private static final WidgetSprites MENU_BUTTON_SPRITES = new WidgetSprites(CionMenu.id("button/menu"));
	private static final int BUTTON_SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int BUTTON_GAP = 4;
	private static final boolean ICON_ONLY = true;

	private CionMenuScreens() {}

	public static void init() {
		ScreenEvents.AFTER_INIT.register(CionMenuScreens::afterInit);
	}

	private static void afterInit(Minecraft client, Screen screen, int width, int height) {
		if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
			addMenuButton(screen);
		}
	}

	private static void addMenuButton(Screen screen) {
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		AbstractWidget anchor = findIconRowAnchor(widgets);
		if (anchor == null) {
			CionMenu.LOGGER.warn("No 20x20 icon-row anchor on {}; menu button skipped", screen.getClass().getSimpleName());
			return;
		}

		AbstractWidget button = SpriteIconButton.builder(TITLE, pressed -> openMenu(screen), ICON_ONLY)
				.size(BUTTON_SIZE, BUTTON_SIZE)
				.sprite(MENU_BUTTON_SPRITES, ICON_SIZE, ICON_SIZE)
				.withTootip()
				.build();
		button.setPosition(anchor.getRight() + BUTTON_GAP, anchor.getY());
		widgets.add(button);
	}

	private static AbstractWidget findIconRowAnchor(List<AbstractWidget> widgets) {
		AbstractWidget best = null;
		for (AbstractWidget widget : widgets) {
			if (!widget.visible) continue;
			if (widget.getWidth() != BUTTON_SIZE || widget.getHeight() != BUTTON_SIZE) continue;
			if (best == null
					|| widget.getY() > best.getY()
					|| (widget.getY() == best.getY() && widget.getRight() > best.getRight())) {
				best = widget;
			}
		}
		return best;
	}

	private static void openMenu(Screen parent) {
		Minecraft.getInstance().setScreenAndShow(new CionConfigScreen(parent));
	}
}
