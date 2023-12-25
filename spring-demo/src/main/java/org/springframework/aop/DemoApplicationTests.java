package org.springframework.aop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class DemoApplicationTests {
	@Autowired
	Person person;

	@Autowired
	ProxyClass proxyClass2;

	/**
	 * 两者互为代理
 	 */
    public void contextLoads() {
        ProxyClass proxyClass = (ProxyClass) person;
        proxyClass.print();

        Person person2 = (Person) proxyClass2;
        person2.out();
    }

}

