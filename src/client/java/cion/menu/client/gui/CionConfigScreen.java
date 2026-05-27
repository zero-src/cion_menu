package cion.menu.client.gui;

import cion.core.config.ConfigManager;
import cion.core.config.ConfigRegistry;
import cion.menu.CionMenu;
import cion.menu.client.config.ConfigFields;
import cion.menu.client.config.WorkingCopy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CionConfigScreen extends Screen {
	private static final Component TITLE = Component.translatable("cion_menu.config.title");
	private static final Component SAVE = Component.translatable("cion_menu.config.save");
	private static final Component ON = Component.translatable("cion_menu.config.bool.on");
	private static final Component OFF = Component.translatable("cion_menu.config.bool.off");

	private static final int TAB_Y = 24;
	private static final int TAB_W = 110;
	private static final int TAB_H = 20;
	private static final int TAB_GAP = 4;
	private static final int FORM_TOP = 54;
	private static final int ROW_H = 22;
	private static final int LABEL_X = 20;
	private static final int LABEL_W = 180;
	private static final int LABEL_H = 12;
	private static final int LABEL_DY = 6;
	private static final int WIDGET_W = 220;
	private static final int WIDGET_H = 20;
	private static final int FOOTER_GAP = 30;
	private static final int FOOTER_BTN_W = 100;
	private static final int FOOTER_BTN_H = 20;
	private static final int FOOTER_BTN_GAP = 8;
	private static final int VIEWPORT_PAD = 6;

	private final Screen parent;
	private final List<WorkingCopy> sections = new ArrayList<>();
	private final List<List<ConfigFields.Entry>> sectionEntries = new ArrayList<>();
	private final List<FormRow> formRows = new ArrayList<>();

	private int activeIndex;
	private int scrollRows;
	private int viewportRows;
	private Button saveButton;

	public CionConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
		for (ConfigManager<?> mgr : ConfigRegistry.all()) {
			WorkingCopy wc = new WorkingCopy(mgr);
			sections.add(wc);
			sectionEntries.add(ConfigFields.walk(wc.modId(), wc.draft()));
		}
	}

	@Override
	protected void init() {
		formRows.clear();
		addTitleLabel();
		buildTabs();
		buildFooter();
		buildActiveSection();
		viewportRows = Math.max(1, (viewportBottom() - FORM_TOP) / ROW_H);
		clampScroll();
		layoutForm();
		updateSaveActive();
	}

	private int viewportBottom() {
		return this.height - FOOTER_GAP - VIEWPORT_PAD;
	}

	private void addTitleLabel() {
		int titleW = this.font.width(TITLE);
		StringWidget title = new StringWidget((this.width - titleW) / 2, 8, titleW, LABEL_H, TITLE, this.font);
		addRenderableWidget(title);
	}

	private void buildTabs() {
		if (sections.isEmpty()) return;
		int x = LABEL_X;
		for (int i = 0; i < sections.size(); i++) {
			final int idx = i;
			Component label = Component.translatableWithFallback(
					sections.get(i).modId() + ".config.title",
					sections.get(i).modId());
			Button tab = Button.builder(label, b -> switchTab(idx))
					.bounds(x, TAB_Y, TAB_W, TAB_H)
					.build();
			tab.active = (i != activeIndex);
			addRenderableWidget(tab);
			x += TAB_W + TAB_GAP;
		}
	}

	private void buildFooter() {
		int y = this.height - FOOTER_GAP;
		int totalW = FOOTER_BTN_W * 2 + FOOTER_BTN_GAP;
		int cancelX = (this.width - totalW) / 2;
		int saveX = cancelX + FOOTER_BTN_W + FOOTER_BTN_GAP;
		Button cancel = Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
				.bounds(cancelX, y, FOOTER_BTN_W, FOOTER_BTN_H)
				.build();
		saveButton = Button.builder(SAVE, b -> commit())
				.bounds(saveX, y, FOOTER_BTN_W, FOOTER_BTN_H)
				.build();
		saveButton.active = false;
		addRenderableWidget(cancel);
		addRenderableWidget(saveButton);
	}

	private void buildActiveSection() {
		if (sections.isEmpty()) {
			Component msg = Component.translatable("cion_menu.config.empty");
			int w = this.font.width(msg);
			StringWidget empty = new StringWidget((this.width - w) / 2, FORM_TOP + 10, w, LABEL_H, msg, this.font);
			addRenderableWidget(empty);
			return;
		}
		int formW = formWidth();
		int widgetX = formWidgetX(formW);
		int widgetW = Math.min(WIDGET_W, formW - (widgetX - LABEL_X) - 4);
		int rowIdx = 0;
		for (ConfigFields.Entry entry : sectionEntries.get(activeIndex)) {
			Component label = Component.translatableWithFallback(entry.labelKey, entry.labelFallback);
			if (entry.kind == ConfigFields.Kind.HEADER) {
				Component styled = label.copy().withStyle(ChatFormatting.GOLD);
				StringWidget heading = new StringWidget(LABEL_X, 0,
						this.font.width(styled), LABEL_H, styled, this.font);
				addRenderableWidget(heading);
				formRows.add(new FormRow(rowIdx, heading, null, LABEL_DY, 0));
				rowIdx++;
				continue;
			}
			int indent = indentFor(entry.path);
			StringWidget row = new StringWidget(LABEL_X + indent, 0,
					Math.max(20, widgetX - (LABEL_X + indent) - 4), LABEL_H, label, this.font);
			addRenderableWidget(row);
			AbstractWidget widget = buildWidget(entry, widgetX, widgetW);
			if (widget != null) addRenderableWidget(widget);
			formRows.add(new FormRow(rowIdx, row, widget, LABEL_DY, 0));
			rowIdx++;
		}
	}

	private int formWidth() {
		return Math.max(280, this.width - LABEL_X * 2);
	}

	private int formWidgetX(int formW) {
		int labelArea = Math.max(LABEL_W, formW / 2);
		return LABEL_X + labelArea;
	}

	private int indentFor(String path) {
		int depth = 0;
		for (int i = 0; i < path.length(); i++) if (path.charAt(i) == '.') depth++;
		return depth * 10;
	}

	private AbstractWidget buildWidget(ConfigFields.Entry entry, int x, int w) {
		switch (entry.kind) {
			case BOOLEAN: return booleanButton(entry, x, w);
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING: return editBox(entry, x, w);
			default: return null;
		}
	}

	private Button booleanButton(ConfigFields.Entry entry, int x, int w) {
		Button[] holder = new Button[1];
		Button btn = Button.builder(boolValue(entry) ? ON : OFF, b -> {
			boolean next = !boolValue(entry);
			entry.set(next);
			holder[0].setMessage(next ? ON : OFF);
			updateSaveActive();
		}).bounds(x, 0, w, WIDGET_H).build();
		holder[0] = btn;
		return btn;
	}

	private static boolean boolValue(ConfigFields.Entry entry) {
		Object v = entry.get();
		return v != null && (Boolean) v;
	}

	private EditBox editBox(ConfigFields.Entry entry, int x, int w) {
		EditBox box = new EditBox(this.font, x, 0, w, WIDGET_H, Component.empty());
		box.setMaxLength(128);
		Object current = entry.get();
		box.setValue(current == null ? "" : String.valueOf(current));
		box.setResponder(text -> {
			Object parsed = parse(entry.kind, text);
			if (parsed == null && !text.isEmpty()) return;
			entry.set(parsed != null ? parsed : defaultFor(entry.kind));
			updateSaveActive();
		});
		return box;
	}

	private Object parse(ConfigFields.Kind kind, String text) {
		try {
			switch (kind) {
				case INTEGER: return text.isEmpty() ? null : Integer.parseInt(text);
				case LONG: return text.isEmpty() ? null : Long.parseLong(text);
				case FLOAT: return text.isEmpty() ? null : Float.parseFloat(text);
				case DOUBLE: return text.isEmpty() ? null : Double.parseDouble(text);
				case STRING: return text;
				default: return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Object defaultFor(ConfigFields.Kind kind) {
		switch (kind) {
			case INTEGER: return 0;
			case LONG: return 0L;
			case FLOAT: return 0f;
			case DOUBLE: return 0d;
			case STRING: return "";
			default: return null;
		}
	}

	private void layoutForm() {
		int top = FORM_TOP;
		int bottom = viewportBottom();
		for (FormRow row : formRows) {
			int rowY = top + (row.index - scrollRows) * ROW_H;
			boolean visible = rowY >= top && rowY + ROW_H <= bottom;
			row.label.setY(rowY + row.labelDy);
			row.label.visible = visible;
			if (row.widget != null) {
				row.widget.setY(rowY + row.widgetDy);
				row.widget.visible = visible;
				row.widget.active = visible;
			}
		}
	}

	private void clampScroll() {
		int max = Math.max(0, formRows.size() - viewportRows);
		if (scrollRows > max) scrollRows = max;
		if (scrollRows < 0) scrollRows = 0;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
		if (formRows.isEmpty()) return false;
		int before = scrollRows;
		scrollRows -= (int) Math.signum(scrollY);
		clampScroll();
		if (scrollRows != before) layoutForm();
		return true;
	}

	private void switchTab(int idx) {
		if (idx == activeIndex) return;
		activeIndex = idx;
		scrollRows = 0;
		this.clearWidgets();
		this.init();
	}

	private void updateSaveActive() {
		if (saveButton != null) saveButton.active = anyDirty();
	}

	private boolean anyDirty() {
		for (WorkingCopy wc : sections) if (wc.dirty()) return true;
		return false;
	}

	private void commit() {
		for (WorkingCopy wc : sections) {
			if (wc.dirty()) wc.commit();
		}
		CionMenu.LOGGER.info("cion_menu: saved configs ({})", sections.size());
		Minecraft.getInstance().setScreenAndShow(parent);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(parent);
	}

	private static final class FormRow {
		final int index;
		final AbstractWidget label;
		final AbstractWidget widget;
		final int labelDy;
		final int widgetDy;

		FormRow(int index, AbstractWidget label, AbstractWidget widget, int labelDy, int widgetDy) {
			this.index = index;
			this.label = label;
			this.widget = widget;
			this.labelDy = labelDy;
			this.widgetDy = widgetDy;
		}
	}
}
