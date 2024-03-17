//package org.springframework.bzb.config;
//
//import org.springframework.bzb.AppConfig;
//import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
//import org.springframework.bzb.wevmvc.WebConfig;
//
//public class MyWebAppInitializer2 extends AbstractAnnotationConfigDispatcherServletInitializer {
//
//	//@Override
//	//public void onStartup(ServletContext servletContext) throws ServletException {
//	//    // 创建Spring应用上下文
//	//    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
//	//    context.register(WebConfig.class); // 注册你的Spring MVC配置类
//	//
//	//    // 配置并注册DispatcherServlet
//	//	ServletRegistration.Dynamic servlet = servletContext.addServlet("dispatcher", new DispatcherServlet(context));
//	//    servlet.setLoadOnStartup(1);
//	//    servlet.addMapping("/"); // 映射所有请求到DispatcherServlet
//	//
//	//    // 可以在此处添加Filter配置
//	//    // ...
//	//}
//
//	// 指定 Spring 应用上下文配置类（主要配置 web 组件的 Bean）
//	@Override
//	protected Class<?>[] getServletConfigClasses() {
//		return new Class[]{WebConfig.class};
//	}
//
//	// 相对应的另一个应用上下文配置类（应用中的其他 Bean）
//	@Override
//	protected Class<?>[] getRootConfigClasses() {
//		return new Class[]{AppConfig.class};
//	}
//
//	// 将 DispatcherServlet 映射到 "/"
//	@Override
//	protected String[] getServletMappings() {
//		return new String[]{"/"};
//	}
//}