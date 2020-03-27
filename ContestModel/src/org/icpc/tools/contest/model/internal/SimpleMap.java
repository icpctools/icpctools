package org.icpc.tools.contest.model.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A simple map backed by arrays, which automatically ignores (doesn't add) null keys.
 */
public class SimpleMap implements Map<String, Object> {
	private static final int INITIAL_SIZE = 7;
	private static final int GROW = 4;
	private String[] keys = new String[INITIAL_SIZE];
	private Object[] values = new Object[INITIAL_SIZE];
	private int size = 0;

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key == null)
			return false;

		for (String k : keys)
			if (key.equals(k))
				return true;

		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value == null)
			return false;

		for (Object v : values)
			if (value.equals(v))
				return true;

		return false;
	}

	@Override
	public Object get(Object key) {
		if (key == null)
			return false;

		for (int i = 0; i < size; i++)
			if (keys[i].equals(key))
				return values[i];

		return null;
	}

	@Override
	public Object put(String key, Object value) {
		if (key == null)
			return null;

		int arrSize = keys.length;
		if (size == arrSize) {
			// time to grow
			String[] newKeys = new String[arrSize + GROW];
			System.arraycopy(keys, 0, newKeys, 0, arrSize);
			keys = newKeys;
			Object[] newValues = new Object[arrSize + GROW];
			System.arraycopy(values, 0, newValues, 0, arrSize);
			values = newValues;
		}
		keys[size] = key;
		values[size] = value;
		size++;
		return null;
	}

	@Override
	public Object remove(Object key) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public void clear() {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public Set<String> keySet() {
		return new KeySet(keys, size);
	}

	@Override
	public Collection<Object> values() {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		throw new IllegalArgumentException("Not supported");
	}
}