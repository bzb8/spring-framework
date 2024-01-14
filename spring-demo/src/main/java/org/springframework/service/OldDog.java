package org.springframework.service;

public class OldDog {
	private String name;
	private Long id;
	public OldDog() {}
	public OldDog(String name) {
		this.name = name;
	}
	public OldDog(Long id, String name) {
		this(name);
		this.id = id;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "OldDog{" +
				"name='" + name + '\'' +
				", id=" + id +
				'}';
	}
}