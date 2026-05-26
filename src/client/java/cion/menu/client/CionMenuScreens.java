package cion.menu.client;

import cion.menu.CionMenu;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CionMenuScreens {
	private static final Component TITLE = Component.literal("Cion Menu");
	private static final Component TEST_MESSAGE = Component.literal("Cion Menu GUI test screen");
	private static final WidgetSprites MENU_BUTTON_SPRITES = new WidgetSprites(CionMenu.id("button/menu"));
	private static final int BUTTON_SIZE = 20;
	private static final int ICON_SIZE = 16;

	private CionMenuScreens() {}

	public static void init() {
		ScreenEvents.AFTER_INIT.register(CionMenuScreens::afterInit);
	}

	private static void afterInit(Minecraft client, Screen screen, int width, int height) {
		if (screen instanceof TitleScreen) {
			addMenuButton(client, screen, width / 2 + 84, height / 4 + 132);
		}

		if (screen instanceof PauseScreen) {
			addMenuButton(client, screen, width / 2 + 42, height / 4 + 96);
		}
	}

	private static void addMenuButton(Minecraft client, Screen screen, int x, int y) {
		Optional<AbstractWidget> rowEnd = findLastSmallButton(Screens.getWidgets(screen));
		if (rowEnd.isPresent()) {
			AbstractWidget lastButton = rowEnd.get();
			x = lastButton.getRight() + 4;
			y = lastButton.getY();
		}

		AbstractWidget button = SpriteIconButton.builder(TITLE, pressed -> openMenu(client, screen), true)
				.size(BUTTON_SIZE, BUTTON_SIZE)
				.sprite(MENU_BUTTON_SPRITES, ICON_SIZE, ICON_SIZE)
				.withTootip()
				.build();
		button.setPosition(x, y);
		Screens.getWidgets(screen).add(button);
	}

	private static Optional<AbstractWidget> findLastSmallButton(List<AbstractWidget> widgets) {
		return widgets.stream()
				.filter(widget -> widget.visible && widget.getWidth() == BUTTON_SIZE && widget.getHeight() == BUTTON_SIZE)
				.max(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getRight));
	}

	private static void openMenu(Minecraft client, Screen parent) {
		client.setScreenAndShow(new AlertScreen(
				() -> client.setScreenAndShow(parent),
				TITLE,
				TEST_MESSAGE,
				Component.literal("Done"),
				true
		));
	}
}
