package org.springframework.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LouzaiAspect {
    
    @Pointcut("execution(* org.springframework.aop.Louzai.everyDay())")
	public void myPointCut() {
    }

    // 前置通知
    @Before("myPointCut()")
    public void myBefore() {
        System.out.println("吃饭");
    }

    // 后置通知
    @AfterReturning(value = "myPointCut()")
    public void myAfterReturning() {
        System.out.println("打豆豆。。。");
    }
}