package org.springframework.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MyInvocationHandler implements InvocationHandler {
 
    private Object target;
 
    public MyInvocationHandler(Object target){
        this.target = target;
    }
 
    @Override
    public Object invoke(Object proxy, Method method, Object[] args ) throws Throwable {
        System.out.println("method :" + method.getName() + " is invoked!");
        return method.invoke( target,args );
    }

	public static void main( String[] args ) {
		Game game = (Game) Proxy.newProxyInstance(
				MyInvocationHandler.class.getClassLoader(),
				new Class<?>[]{Game.class},
				new MyInvocationHandler( new GameImpl() )
		);
		game.playGame();
	}

    
}