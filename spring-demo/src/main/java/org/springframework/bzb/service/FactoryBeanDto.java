package org.springframework.bzb.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class FactoryBeanDto {
	private String name;

	public FactoryBeanDto() {
	}

	public FactoryBeanDto(String name) {
		this.name = name;
	}
}
