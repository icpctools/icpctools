package org.icpc.tools.contest.model.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple String set backed by an array, which automatically ignores (doesn't add) null keys.
 */
public class SimpleSet implements Set<String> {
	private static final int INITIAL_SIZE = 8;
	private static final int GROW = 4;
	private String[] values = new String[INITIAL_SIZE];
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
	public boolean contains(Object o) {
		if (o == null)
			return false;

		for (String v : values)
			if (o.equals(v))
				return true;

		return false;
	}

	@Override
	public boolean add(String value) {
		if (value == null)
			return false;

		int arrSize = values.length;
		if (size == arrSize) {
			// time to grow
			String[] newValues = new String[arrSize + GROW];
			System.arraycopy(values, 0, newValues, 0, arrSize);
			values = newValues;
		}
		values[size] = value;
		size++;
		return true;
	}

	@Override
	public boolean remove(Object key) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public void clear() {
		values = new String[INITIAL_SIZE];
		size = 0;
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			int count = 0;

			@Override
			public boolean hasNext() {
				return count < size - 1;
			}

			@Override
			public String next() {
				return values[count++];
			}
		};
	}

	@Override
	public Object[] toArray() {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new IllegalArgumentException("Not supported");
	}
}