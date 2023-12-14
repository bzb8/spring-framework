package org.springframework.service;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Conditional(MyConditional.class)
@Service
public class ConditionalService {

	public void test() {
		System.out.println("I am ConditionalService test()");
	}
}
