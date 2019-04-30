package org.icpc.tools.cds.checklist;

public class Item {
	private static int ID = 0;
	public int id = ID++;
	public int indent;
	public String name;
	public String owner;
	public boolean isComplete;

	public Item(String name, String owner, int indent, boolean isComplete) {
		this.name = name;
		this.owner = owner;
		this.indent = indent;
		this.isComplete = isComplete;
	}

	public Item(String name, String owner, int indent) {
		this(name, owner, indent, false);
	}
}