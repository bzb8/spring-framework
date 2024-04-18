package org.springframework.bzb.config;

import org.springframework.bzb.entity.Bzb;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MyDeferredImportSelector implements DeferredImportSelector {
	@Override
	public Class<? extends Group> getImportGroup() {
		return Group.class;
	}

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[] {Bzb.class.getName()};
	}

	@Override
	public Predicate<String> getExclusionFilter() {
		return DeferredImportSelector.super.getExclusionFilter();
	}

	private static class Group implements DeferredImportSelector.Group {
		private final List<Entry> imports = new ArrayList<>();
		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			for (String selectImport : selector.selectImports(metadata)) {
				imports.add(new Entry(metadata, selectImport));
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			return imports;
		}
	}
}
