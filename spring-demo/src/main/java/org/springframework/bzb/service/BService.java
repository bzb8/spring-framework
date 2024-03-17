package org.springframework.bzb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BService {

	@Autowired
	private AService aService;

	public void testB() {
		System.out.println("I am BService#testB(), AService: " + aService);
	}
}
