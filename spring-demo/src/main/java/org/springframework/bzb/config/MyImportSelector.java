package org.springframework.bzb.config;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		System.out.println("this is MyImportSelector");
		return new String[]{"org.springframework.entity.Person"};
    }
}