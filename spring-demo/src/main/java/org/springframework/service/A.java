package org.springframework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: bzb
 * @date: 2023-12-15 14:57
 **/
@Service
public class A {

	@Autowired
	@Qualifier("b")
	private B b;

//	public A(B b) {
//		this.b = b;
//	}
}
