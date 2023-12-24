package org.springframework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AService {

	@Autowired
	private BService bService;

	public void testA() {
		System.out.println("i am AService#testA(), BService: " + bService);
	}
}
