package org.springframework.entity;

/**
 * @author Pymjl
 * @version 1.0
 * @date 2022/6/11 17:01
 **/
public class User {
    private String username;
    private String password;
    private String gender;

	public User() {
	}

	public User(String username, String password, String gender) {
		this.username = username;
		this.password = password;
		this.gender = gender;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
}
