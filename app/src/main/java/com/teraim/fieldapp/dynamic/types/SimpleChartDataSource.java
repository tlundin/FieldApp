package com.teraim.fieldapp.dynamic.types;

import org.achartengine.model.CategorySeries;

/**
 * Created by Terje on 2016-07-11.
 */
public interface SimpleChartDataSource extends DataSource {

    CategorySeries getSeries();
    int[] getCurrentValues();
    int getSize();
    int[] getColors();
    String[] getCategories();
}
