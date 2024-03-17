package org.springframework.bzb.test;

// 模拟资金操作
public class FundsService {
    // 账户余额
    private double balance = 1000;

    // 模拟充值
	public double recharge(String userName, double price) {
        System.out.printf("%s充值%s%n", userName, price);
        balance += price;
        return balance;
    }

    // 模拟提现
    double cashOut(String userName, double price) {
        if (balance < price) {
            throw new RuntimeException("余额不足!");
        }
        System.out.printf("%s提现%s%n", userName, price);
        balance -= price;
        return balance;
    }

    // 获取余额
    double getBalance(String userName) {
        return balance;
    }
}