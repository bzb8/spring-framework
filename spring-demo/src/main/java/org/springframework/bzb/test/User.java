package org.springframework.bzb.test;

/**
 * <p>用户表</p>
 *
 * @author hanchao 2018/2/14 22:30
 */
public class User{
    public String username = "张三";
    private int password = 123456;

    /**
     * <p>测试：java反射-参数Parameter</p>
     *
     * @author hanchao 2018/3/4 14:24
     **/
    public void initUser(@MyAnnotationA @MyAnnotationB String username, @MyAnnotationB String password) {
    }
}