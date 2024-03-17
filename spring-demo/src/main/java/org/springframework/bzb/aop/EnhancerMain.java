package org.springframework.bzb.aop;

import org.springframework.cglib.proxy.Enhancer;

// 此处需要说明：Enhancer实际属于CGLIB包的，也就是`net.sf.cglib.proxy.Enhancer`
// 但是Spring把这些类都拷贝到自己这来了，因此我用的Spring的Enhancer，包名为;`org.springframework.cglib.proxy.Enhancer`

public class EnhancerMain {
	public static void main(String[] args) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(GameImpl.class);
		enhancer.setCallback(new MyMethodInterceptor());

		Game game = (Game) enhancer.create();
		game.playGame();
	}

}

