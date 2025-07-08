package com.teraim.fieldapp.dynamic.templates;

import java.util.List;

public interface OnFilterSelectedListener {
    void onFiltersApplied(List<String> newTopFilters, List<String> newAvailableFilters);
}

