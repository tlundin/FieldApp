package com.teraim.fieldapp.dynamic.blocks;

import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

import com.teraim.fieldapp.dynamic.types.SimpleChartDataSource;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

public class RoundChartBlock extends ChartBlock  {


	private String startAngle;
	private transient SimpleChartDataSource myDataSource;


	public RoundChartBlock(String blockId, String name, String label, String container,
						   String textSize, String margins,String startAngle, int height,int width, boolean displayValues,
						   boolean isVisible) {
		super(blockId,name,label,container,textSize,margins,height,width,displayValues,isVisible);
		this.startAngle = startAngle;
		Log.d("vortex","height"+height);

	}






	private float dpMeasure(float textSize) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize, ctx.getResources().getDisplayMetrics());
	}





	protected void initializeChart() {
		if (startAngle == null || startAngle.isEmpty())
			startAngle = "0";

		myDataSource = (SimpleChartDataSource) myContext.getChartDataSource(name);
		super.myDataSource = myDataSource;
		if (myDataSource != null) {
			// Instantiating CategorySeries to plot Pie Chart
			DefaultRenderer defaultRenderer = new DefaultRenderer();
			for (int i = 0; i < myDataSource.getSize(); i++) {
				SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
				seriesRenderer.setColor(myDataSource.getColors()[i]);
				//seriesRenderer.setDisplayChartValues(true);
				// Adding a renderer for a slice
				defaultRenderer.addSeriesRenderer(seriesRenderer);
			}
			defaultRenderer.setChartTitle(label);
			defaultRenderer.setChartTitleTextSize(dpMeasure(textSize + 10));
			defaultRenderer.setLabelsTextSize(dpMeasure(textSize));
			//defaultRenderer.setZoomButtonsVisible(true);
			defaultRenderer.setDisplayValues(displayValues);
			defaultRenderer.setShowLegend(false);
			defaultRenderer.setShowLabels(true);
			defaultRenderer.setApplyBackgroundColor(true);
			defaultRenderer.setBackgroundColor(Color.TRANSPARENT);
			defaultRenderer.setLabelsColor(Color.parseColor("#696969"));
			defaultRenderer.setPanEnabled(false);
			if (intMargins!=null) {
				Log.d("patox","setting margins to "+margins);
				defaultRenderer.setMargins(intMargins);
			}

			defaultRenderer.setStartAngle(Float.parseFloat(startAngle));

			chart = ChartFactory.getPieChartView(ctx, generate(), defaultRenderer);


		} else {
			o.addText("");
			o.addCriticalText("Failed to add round chart block with id " + blockId + " - missing datasource!");
			Log.e("vortex","Failed to add round chart block with id " + blockId + " - missing datasource!");
			myContext.removeEventListener(this);
			return;
		}





}

	@Override
	protected CategorySeries generate() {
		CategorySeries distributionSeries = myDataSource.getSeries();
		distributionSeries.clear();
		boolean hasCategories = myDataSource.getCategories()!=null;
		int[] currentValues = myDataSource.getCurrentValues();
		//If all elements are 0, we want to make a default chart with a message.
		boolean sumzero = true;
		for(int i=0;i<myDataSource.getSize();i++) {
			Log.d("vortex","current value "+i+": "+currentValues[i]);
			if (hasCategories)
				distributionSeries.add(myDataSource.getCategories()[i],currentValues[i]);
			else
				distributionSeries.add(currentValues[i]);
			if (currentValues[i]>0)
				sumzero=false;
		}
		if (sumzero) {
			distributionSeries.set(myDataSource.getSize()-1,"EMPTY",1);
		}
		return distributionSeries;

	}












@Override
public String getName() {
		return name;
		}


}
