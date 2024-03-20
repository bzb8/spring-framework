package org.springframework.bzb.controller;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.bzb.service.CService;
import org.springframework.stereotype.Controller;
import org.springframework.bzb.transactionbzb.User;
import org.springframework.bzb.transactionbzb.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/test")
@Controller
public class HelloController implements ApplicationContextAware {

	ApplicationContext applicationContext;

	private CService cService;

	@Autowired
	public HelloController(CService cService) {
		this.cService = cService;
	}

	@GetMapping("/hello")
	public String hello() {
		return "hello";
	}

	@GetMapping("/user")
	public User getUser() {
		return applicationContext.getBean("bzbUserService", UserService.class).getUserById(1);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}