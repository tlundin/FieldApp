package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.List;

public interface Filter {
	void filter(List<? extends Listable> list);
	boolean isRemovedByFilter(Listable l);
}
