package org.springframework.bzb.aop;

// 1、定义基础接口
public interface HttpApi {
	String get(String url);
}

// 2、网络请求的真正实现
class RealModule implements HttpApi {
	@Override
	public String get(String url) {
		return "result";
	}
}

