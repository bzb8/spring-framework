package org.springframework.aop;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
 
import java.lang.reflect.Method;
 
// 注意：这个是org.springframework.cglib.proxy.MethodInterceptor
// 而不是org.aopalliance.intercept包下的
public class MyMethodInterceptor implements MethodInterceptor {
 
    @Override
    public Object intercept( Object o, Method method, Object[] objects, MethodProxy methodProxy ) throws Throwable {
        // 此处千万不能调用method得invoke方法，否则会死循环的 只能使用methodProxy.invokeSuper 进行调用
        Object intercept = methodProxy.invokeSuper( o,objects );
        System.out.println("拦截器生效..");
        return intercept;
    }
}