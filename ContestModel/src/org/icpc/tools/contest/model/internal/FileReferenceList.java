package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class FileReferenceList implements Iterable<FileReference> {
	private List<FileReference> refs = new ArrayList<>(4);

	public FileReferenceList() {
		// ignore
	}

	public FileReferenceList(FileReference ref) {
		refs.add(ref);
	}

	public FileReferenceList(Object value) {
		Object[] objs = JSONParser.getOrReadArray(value);
		for (Object obj : objs) {
			refs.add(new FileReference((JsonObject) obj));
		}
	}

	@Override
	public int hashCode() {
		return refs.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileReferenceList))
			return false;

		FileReferenceList refList = (FileReferenceList) o;
		if (refList.refs.size() != refs.size())
			return false;
		for (int i = 0; i < refs.size(); i++) {
			if (!refs.get(i).equals(refList.refs.get(i)))
				return false;
		}
		return true;
	}

	public void add(FileReference ref) {
		refs.add(ref);
	}

	public boolean isEmpty() {
		return refs.isEmpty();
	}

	public int size() {
		return refs.size();
	}

	public FileReference first() {
		return refs.get(0);
	}

	@Override
	public Iterator<FileReference> iterator() {
		return refs.iterator();
	}

	public Object resolve(String url) {
		for (FileReference ref : refs) {
			if (ref.getURL().endsWith(url)) {
				if (ref.file != null && ref.file.exists())
					return ref.file;

				return ref.data;
			}
		}
		return null;
	}

	public String getJSON() {
		StringBuilder sb = new StringBuilder();
		for (FileReference ref : refs) {
			if (ref != null) {
				if (sb.length() != 0)
					sb.append(",");
				sb.append(ref.getJSON());
			}
		}
		return "[" + sb.toString() + "]";
	}

	public static Object resolve(String url, FileReferenceList... lists) {
		for (FileReferenceList list : lists) {
			if (list != null) {
				Object obj = list.resolve(url);
				if (obj != null)
					return obj;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getJSON();
	}
}