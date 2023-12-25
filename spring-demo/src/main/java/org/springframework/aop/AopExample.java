package org.springframework.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.springframework.stereotype.Component;


@Component
@Aspect
public class AopExample {

    @DeclareParents(value = "org.springframework.aop.Person", defaultImpl = ProxyClassImpl.class)
    ProxyClass proxy;


}

