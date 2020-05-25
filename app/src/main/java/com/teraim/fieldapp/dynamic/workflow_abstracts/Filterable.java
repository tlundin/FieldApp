package com.teraim.fieldapp.dynamic.workflow_abstracts;


public interface Filterable {
	String getId();
	void removeFilter(Filter f);
	void addFilter(Filter f);
}
