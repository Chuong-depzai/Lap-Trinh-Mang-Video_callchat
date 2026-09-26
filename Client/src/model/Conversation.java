package model;

public class Conversation {
	public enum Type {
		USER, GROUP
	}

	private int id;
	private String name;
	private Type type;	

	public Conversation(int id, String name, Type type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		// Để hiển thị đẹp trên danh sách
		return (type == Type.GROUP ? "* " : " ") + name;
	}
}
