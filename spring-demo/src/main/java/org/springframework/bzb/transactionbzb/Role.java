package org.springframework.bzb.transactionbzb;

import java.util.Objects;

public class Role {
	private Integer id;
	private String role;

	public Role() {
	}

	public Role(Integer id, String role) {
		this.id = id;
		this.role = role;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Role role1 = (Role) o;
		return Objects.equals(id, role1.id) && Objects.equals(role, role1.role);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, role);
	}

	public Integer getId() {

		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "Role{" +
				"id=" + id +
				", role='" + role + '\'' +
				'}';
	}
}
