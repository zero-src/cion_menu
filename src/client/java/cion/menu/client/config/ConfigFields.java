package cion.menu.client.config;

import cion.menu.CionMenu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection-based walker that turns a config POJO into a flat list of
 * {@link ConfigFields.Entry} rows for the UI. Nested objects become header
 * rows followed by their leaf fields with dotted paths.
 */
public final class ConfigFields {
	private ConfigFields() {}

	public enum Kind { HEADER, BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE, STRING }

	public static final class Entry {
		public final Kind kind;
		public final String path;        // dotted path: hearts.enabled
		public final String labelKey;    // translation key: <modId>.config.<path>
		public final String labelFallback;
		public final Object holder;      // owning object (null for HEADER)
		public final Field field;        // null for HEADER

		private Entry(Kind kind, String path, String labelKey, String labelFallback, Object holder, Field field) {
			this.kind = kind;
			this.path = path;
			this.labelKey = labelKey;
			this.labelFallback = labelFallback;
			this.holder = holder;
			this.field = field;
		}

		public Object get() {
			if (field == null) throw new IllegalStateException("get() on HEADER entry");
			try {
				return field.get(holder);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(Object value) {
			if (field == null) throw new IllegalStateException("set() on HEADER entry");
			try {
				field.set(holder, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static List<Entry> walk(String modId, Object root) {
		List<Entry> out = new ArrayList<>();
		walk(modId, root, "", out);
		return out;
	}

	private static void walk(String modId, Object obj, String prefix, List<Entry> out) {
		if (obj == null) return;
		for (Field f : obj.getClass().getDeclaredFields()) {
			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) continue;
			f.setAccessible(true);
			String path = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
			Kind kind = kindOf(f.getType());
			String labelKey = modId + ".config." + path;
			String fallback = prettify(f.getName());
			if (kind != null) {
				out.add(new Entry(kind, path, labelKey, fallback, obj, f));
				continue;
			}
			// nested object — add a header then recurse
			Class<?> t = f.getType();
			if (t.isPrimitive() || t.isArray() || t.isEnum() || t == Object.class
					|| java.util.Collection.class.isAssignableFrom(t)
					|| java.util.Map.class.isAssignableFrom(t)) {
				CionMenu.LOGGER.warn("ConfigFields: skipping unsupported field {}.{} of type {}",
						modId, path, t.getSimpleName());
				continue;
			}
			Object child;
			try {
				child = f.get(obj);
			} catch (IllegalAccessException e) {
				CionMenu.LOGGER.warn("ConfigFields: cannot access nested field {}.{}", modId, path, e);
				continue;
			}
			if (child == null) continue;
			out.add(new Entry(Kind.HEADER, path, labelKey, fallback, null, null));
			walk(modId, child, path, out);
		}
	}

	private static Kind kindOf(Class<?> t) {
		if (t == boolean.class || t == Boolean.class) return Kind.BOOLEAN;
		if (t == int.class || t == Integer.class) return Kind.INTEGER;
		if (t == long.class || t == Long.class) return Kind.LONG;
		if (t == float.class || t == Float.class) return Kind.FLOAT;
		if (t == double.class || t == Double.class) return Kind.DOUBLE;
		if (t == String.class) return Kind.STRING;
		return null;
	}

	private static String prettify(String camel) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < camel.length(); i++) {
			char c = camel.charAt(i);
			if (i == 0) {
				sb.append(Character.toUpperCase(c));
			} else if (Character.isUpperCase(c)) {
				sb.append(' ').append(c);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
