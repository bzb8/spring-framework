package org.springframework.entity;

public class User {
	private String name;
	private String pwd;
	private String gender;

	public User() {
	}

	public User(String name, String pwd, String gender) {
		this.name = name;
		this.pwd = pwd;
		this.gender = gender;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
}
