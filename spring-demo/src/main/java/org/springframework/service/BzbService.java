package org.springframework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.entity.Person;

public class BzbService {

	@Autowired
	private Person person;

	public void test() {
		System.out.println("hello test");
	}
}
