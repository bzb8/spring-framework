package org.springframework.bzb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bzb.transactionbzb.User;
import org.springframework.bzb.transactionbzb.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/test")
@RestController
public class HelloController {

	@Autowired
	private UserService bzbUserService;

	@GetMapping("/hello")
	public String hello() {
		return "hello";
	}

	@GetMapping("/user")
	public String getUser() {
		return bzbUserService.getUserById(1).toString();
	}

}