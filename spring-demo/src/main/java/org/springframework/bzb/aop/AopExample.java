package org.springframework.bzb.aop;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Component
@Aspect
public class AopExample {

    //@DeclareParents(value = "org.springfra mework.aop.Person", defaultImpl = ProxyClassImpl.class)
    ProxyClass proxy;


}

