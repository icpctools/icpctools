package org.icpc.tools.cds.presentations;

public class PresentationInfo {
	private String id;
	private String name;
	private String category;
	private String className;
	private String image;
	private String description;
	private String[] properties;
	private String[] data;
	private boolean isTransition;

	public PresentationInfo(String id, String name, String category, String className, String description,
			String[] properties, String image, boolean isTransition) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.className = className;
		this.description = description;
		this.properties = properties;
		this.image = image;
		this.isTransition = isTransition;
	}

	@Override
	public PresentationInfo clone() {
		return new PresentationInfo(id, name, category, className, description, properties, image, isTransition);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getClassName() {
		return className;
	}

	public String getDescription() {
		return description;
	}

	public String[] getProperties() {
		return properties;
	}

	public String getImage() {
		return image;
	}

	public boolean isTransition() {
		return isTransition;
	}

	public void setData(String s) {
		data = new String[] { s };
	}

	public void setData(String[] s) {
		data = s;
	}

	public String[] getData() {
		return data;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PresentationInfo))
			return false;

		PresentationInfo pw = (PresentationInfo) o;
		return id.equals(pw.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}