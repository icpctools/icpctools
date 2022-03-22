package org.icpc.tools.contest.model.internal;

import java.io.File;

import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class FileReference {
	public String mime;
	public String href = null;
	public File file;
	public String filename;
	public long lastModified;
	public String etag;
	public int width = -1;
	public int height = -1;
	public Object data;

	public FileReference() {
		// do nothing
	}

	public FileReference(JsonObject obj) {
		href = obj.getString("href");
		mime = obj.getString("mime");
		height = obj.getInt("height");
		width = obj.getInt("width");
		filename = obj.getString("filename");
	}

	public String getURL() {
		return href;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected String getJSON() {
		// if (file == null)
		// return "{\"href\":\"" + href + "\"}";
		StringBuilder sb = new StringBuilder("{\"href\":\"" + href + "\"");
		if (filename != null)
			sb.append(",\"filename\":\"" + filename + "\"");
		if (mime != null)
			sb.append(",\"mime\":\"" + mime + "\"");
		if (width > 0)
			sb.append(",\"width\":" + width);
		if (height > 0)
			sb.append(",\"height\":" + height);
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		if (href != null)
			return href.hashCode();
		if (file != null)
			return file.hashCode();
		if (mime != null)
			return mime.hashCode();
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileReference))
			return false;

		FileReference ref = (FileReference) o;
		if ((file == null && ref.file != null) || (file != null && !file.equals(ref.file)))
			return false;
		if ((filename == null && ref.filename != null) || (filename != null && !filename.equals(ref.filename)))
			return false;
		if ((mime == null && ref.mime != null) || (mime != null && !mime.equals(ref.mime)))
			return false;
		if (lastModified != ref.lastModified)
			return false;
		if (width != ref.width)
			return false;
		if (height != ref.height)
			return false;

		return true;
	}

	@Override
	public String toString() {
		if (file != null)
			return file.getName() + " - " + getJSON();
		return getJSON();
	}
}
