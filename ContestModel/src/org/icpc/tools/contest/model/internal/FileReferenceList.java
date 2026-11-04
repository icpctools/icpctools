package org.icpc.tools.contest.model.internal;

import java.io.File;
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
		if (value instanceof FileReferenceList) {
			FileReferenceList list = (FileReferenceList) value;
			for (FileReference ref : list.refs)
				refs.add(ref);

			return;
		}
		Object[] objs = JSONParser.getOrReadArray(value);
		if (objs != null) {
			for (Object obj : objs) {
				refs.add(new FileReference((JsonObject) obj));
			}
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
		if (refs.isEmpty())
			return null;
		return refs.get(0);
	}

	public FileReference get(int i) {
		return refs.get(i);
	}

	@Override
	public Iterator<FileReference> iterator() {
		return refs.iterator();
	}

	public File resolve(String url) {
		for (FileReference ref : refs) {
			if (ref.getURL().endsWith(url)) {
				if (ref.file != null && ref.file.exists())
					return ref.file;
			}
		}

		// support files that live in a folder of their own (e.g. HLS index and segments): if the ref
		// is <folder>/<file> on disk and in its href, allow access to other files in the same folder
		int ind = url.lastIndexOf('/');
		if (ind <= 0)
			return null;

		String folderUrl = url.substring(0, ind);
		String name = url.substring(ind + 1);
		if (name.isEmpty() || name.startsWith(".") || name.contains("\\"))
			return null;

		for (FileReference ref : refs) {
			if (ref.file == null || ref.href == null)
				continue;

			// only applies when the href ends with the actual folder and file name
			File folder = ref.file.getParentFile();
			if (folder == null || !folderUrl.equals(folder.getName())
					|| !ref.href.endsWith("/" + folder.getName() + "/" + ref.file.getName()))
				continue;

			File f = new File(folder, name);
			if (f.isFile() && folder.equals(f.getParentFile()))
				return f;
		}
		return null;
	}

	public String[] getHrefs() {
		String[] hrefs = new String[refs.size()];
		int i = 0;
		for (FileReference ref : refs)
			hrefs[i++] = ref.href;

		return hrefs;
	}

	public String[] getRefs() {
		int size = refs.size();
		String[] s = new String[size];
		for (int i = 0; i < size; i++) {
			s[i] = refs.get(i).getJSON();
		}
		return s;
	}

	public static File resolve(String url, FileReferenceList... lists) {
		for (FileReferenceList list : lists) {
			if (list != null) {
				File f = list.resolve(url);
				if (f != null)
					return f;
			}
		}
		return null;
	}

	/**
	 * Returns true if the source file matches a file in this list - i.e. has the same height,
	 * width, and mime type.
	 */
	public boolean containsFile(FileReference sourceFile) {
		for (FileReference ref : refs) {
			if (ref.height == sourceFile.height && ref.width == sourceFile.width
					&& (ref.mime == null || ref.mime.equals(sourceFile.mime))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "File Reference List [" + refs.size() + "]";
	}
}