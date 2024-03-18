package org.springframework.bzb.aop;

class User {
	public int age() {
		System.out.println("用户年龄...");
		return 0;
	}

	public int age(int i) {
		System.out.println("用户年龄.....(" + i + ")");
		return i;
	}

	public void money() {
		System.out.println("我没钱,好穷");
	}

	public void money(String name, int i) {
		System.out.println(name + " 有钱" + i);
	}

}