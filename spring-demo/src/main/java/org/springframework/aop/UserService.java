package org.springframework.aop;

import org.springframework.stereotype.Component;

@Component
public class UserService {
	public void test(){
		System.out.println("advice test!!!");
	}
 
}