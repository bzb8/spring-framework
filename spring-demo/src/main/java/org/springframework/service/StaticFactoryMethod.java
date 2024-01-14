package org.springframework.service;

public class StaticFactoryMethod {
	public static OldDog createOldDog(String name) {
		return new OldDog(name);
	}
	public static OldDog createOldDog(Long id, String name) {
		return new OldDog(id, name);
	}
}
