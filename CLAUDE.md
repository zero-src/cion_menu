# cion_menu project notes

Fabric client mod for MC 26.2-pre-2, Fabric Loader 0.19.2, Fabric API 0.150.1+26.2, Java 25, Loom 1.16.

## Project conventions

- Mod id: `cion_menu`.
- Main entrypoint: `cion.menu.CionMenu`.
- Client entrypoint: `cion.menu.client.CionMenuClient`.
- Use `CionMenu.id(String)` for mod identifiers. It wraps `Identifier.fromNamespaceAndPath("cion_menu", path)`.
- The mod currently has no access widener and no custom mixin config.
- The menu entry point is a small icon button injected into `TitleScreen` and `PauseScreen` through Fabric Screen API.
- Button sprite path: `assets/cion_menu/textures/gui/sprites/button/menu.png`.

## Minecraft 26.2 notes

- `Identifier.of(String, String)` does not exist in this environment. Use `Identifier.fromNamespaceAndPath(String namespace, String path)`.
- `ScreenEvents.AFTER_INIT` callback signature is `(Minecraft client, Screen screen, int width, int height)`.
- Use `SpriteIconButton` for icon buttons that should look like vanilla buttons. `ImageButton` draws only the sprite and has no button background.
- `WidgetSprites(CionMenu.id("button/menu"))` resolves to `assets/cion_menu/textures/gui/sprites/button/menu.png`.
- Use `Minecraft#setScreenAndShow(Screen)` to open screens. `setScreen(Screen)` is not present in these mappings.

## Local tooling

- Java 25 SDK path used by IntelliJ: `E:\__include\.jdks\temurin-25.0.3`.
