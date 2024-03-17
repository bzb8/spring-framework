package org.springframework.bzb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bzb.entity.Person;

public class BzbService {

	@Autowired(required = false)
	private Person person;

	public void test() {
		System.out.println("hello test");
	}
}
