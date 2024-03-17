package org.springframework.bzb.config;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyDefereredImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		System.out.println("this is MyDefereredImportSelector");
        return new String[]{"org.springframework.entity.Person"};
    }
}