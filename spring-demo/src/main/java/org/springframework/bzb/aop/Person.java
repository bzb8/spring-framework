package org.springframework.bzb.aop;

import org.springframework.stereotype.Service;

@Service
public class Person {

    public void out(){
        System.out.println("hello");
    }
}
