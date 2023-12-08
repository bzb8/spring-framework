package org.springframework.service.impl;

import org.springframework.service.AopService;
import org.springframework.stereotype.Service;

/**
 * @author: bzb
 * @date: 2023-12-08 09:26
 **/
@Service
public class AopServiceImpl implements AopService {

	@Override
	public void testAop() {
		System.out.println("I am AopService#testAop()方法");
	}
}
