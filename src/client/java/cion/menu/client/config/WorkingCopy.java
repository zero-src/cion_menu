package cion.menu.client.config;

import cion.core.config.ConfigManager;

/**
 * Editable snapshot of one mod's config. Holds a deep clone the user mutates
 * via the UI, plus the originating {@link ConfigManager} so we can compare to
 * the live instance for dirty-tracking and commit back on save.
 */
public final class WorkingCopy {
	private final ConfigManager<Object> manager;
	private final Object draft;
	private String liveSnapshot;

	@SuppressWarnings("unchecked")
	public WorkingCopy(ConfigManager<?> manager) {
		this.manager = (ConfigManager<Object>) manager;
		this.draft = this.manager.clone(this.manager.get());
		this.liveSnapshot = this.manager.toJson(this.manager.get());
	}

	public String modId() {
		return manager.modId();
	}

	public Object draft() {
		return draft;
	}

	public boolean dirty() {
		return !manager.toJson(draft).equals(liveSnapshot);
	}

	public void commit() {
		manager.set(draft);
		manager.save();
		liveSnapshot = manager.toJson(draft);
	}
}
