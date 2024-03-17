package org.springframework.bzb.service;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@Component
public class MyFactoryBean implements FactoryBean<FactoryBeanDto> {
	@Override
	public FactoryBeanDto getObject() throws Exception {
		return new FactoryBeanDto("bzb");
	}

	@Override
	public Class<?> getObjectType() {
		return FactoryBeanDto.class;
	}
}
