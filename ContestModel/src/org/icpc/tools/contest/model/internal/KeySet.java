package org.icpc.tools.contest.model.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple key set to be used with the SimpleMap class.
 */
public class KeySet implements Set<String> {
	private String[] keys;
	private int size;

	protected KeySet(String[] keys, int size) {
		this.keys = keys;
		this.size = size;
	}

	@Override
	public int size() {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean isEmpty() {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean contains(Object o) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			int ind = 0;

			@Override
			public boolean hasNext() {
				return ind < size;
			}

			@Override
			public String next() {
				return keys[ind++];
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
	public boolean add(String e) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean remove(Object o) {
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

	@Override
	public void clear() {
		throw new IllegalArgumentException("Not supported");
	}
}