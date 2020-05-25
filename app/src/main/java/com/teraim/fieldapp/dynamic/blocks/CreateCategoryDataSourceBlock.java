package com.teraim.fieldapp.dynamic.blocks;

import android.graphics.Color;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.SimpleChartDataSource;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Expressor;

import org.achartengine.model.CategorySeries;

import java.util.Arrays;
import java.util.List;

public class CreateCategoryDataSourceBlock extends Block {

	private final List<Expressor.EvalExpr> argumentE;
	String id=null;
    private String myChart=null;

	private final CategorySeries series;
	private String[] myCategories =null;


	private final int[] colors = { Color.RED, Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN,
			Color.YELLOW,Color.BLACK,Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
			Color.YELLOW };
	
	public CreateCategoryDataSourceBlock(String id,String title,String chart, String[] categories, String expressions, String[] colorNames) {
		super();
		this.blockId = id;
		series = new CategorySeries(title);
		argumentE= Expressor.preCompileExpression(expressions);

		myChart=chart;
		Log.d("battox",argumentE.toString());
		if (categories != null && argumentE!=null && (categories.length == argumentE.size())) {

			myCategories = categories;
			Log.d("vortex","categories ok");
		}
		int j=0;
		if (colorNames!=null) {
			for (String colorName:colorNames) {
				try {
					int c = Color.parseColor(colorName.trim());

					colors[j++] = c;
				} catch (IllegalArgumentException e) {
					WorkFlowBundleConfiguration.debugConsole.addCriticalText("Non existing color: "+colorName);
				}
			}
		}
	}

	public void create(WF_Context myContext) {
		Variable v;
		VariableCache cache = GlobalState.getInstance().getVariableCache();
		LoggerI o = GlobalState.getInstance().getLogger();


		myContext.addChartDataSource(myChart, new SimpleChartDataSource() {
			@Override
			public boolean hasChanged() {
				return true;
			}

			@Override
			public CategorySeries getSeries() {
				return series;
			}

			@Override
			public int[] getCurrentValues() {

				int j = 0;

				int[] ret = Expressor.intAnalyzeList(argumentE);
				//Log.d("botox","values: "+Expressor.analyze(argumentE));

				Log.d("vortex","CurrValues: "+ Arrays.toString(ret));
				return ret;
			}

			@Override
			public int getSize() {
				return argumentE.size();
			}

			@Override
			public int[] getColors() {
				return colors;
			}

			@Override
			public String[] getCategories() {
				return  myCategories;
			}
		});
	}

	
}
