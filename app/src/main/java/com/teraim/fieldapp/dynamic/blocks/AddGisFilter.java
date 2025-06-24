package com.teraim.fieldapp.dynamic.blocks;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Tools;

import java.util.List;

public class AddGisFilter extends Block implements GisFilter {

	/**
	 * 
	 */

    private final String nName;
    private final String label;
    private final String targetObjectType;
    private final String targetLayer;
    private final String color;
	private float radius;
	private Style fillType;
	private PolyType polyType;
	private boolean hasWidget=true;
    private boolean isActive=true;
	private transient WF_Gis_Map myGis;
	private final List<EvalExpr> expressionE;


	public AddGisFilter(String id, String nName, String label, String targetObjectType,String targetLayer,
			String expression, String imgSource, 
			String radius, String color, String polyType, String fillType,
			boolean hasWidget, LoggerI o) {
		super();
        this.nName = nName;
		this.label = label;
		this.targetObjectType = targetObjectType;
		this.targetLayer = targetLayer;
		this.expressionE = Expressor.preCompileExpression(expression);
        this.color = color;
		this.fillType=Paint.Style.FILL;
		if  (fillType !=null) {
			if (fillType.equalsIgnoreCase("STROKE"))
				this.fillType = Paint.Style.STROKE;
			else if (fillType.equalsIgnoreCase("FILL_AND_STROKE"))
				this.fillType = Paint.Style.FILL_AND_STROKE;
		}
		this.polyType=PolyType.circle;
		this.radius=10;
		if (polyType!=null) {
			try {
			this.polyType=PolyType.valueOf(polyType);
			} catch (IllegalArgumentException e) {
				if (polyType.toUpperCase().equals("SQUARE")||polyType.toUpperCase().equals("RECT")||polyType.toUpperCase().equals("RECTANGLE"))
					this.polyType=PolyType.rect;
				else if (polyType.toUpperCase().equals("TRIANGLE"))
					this.polyType=PolyType.triangle;
				else {
				o.addRow("");
				o.addRedText("Unknown polytype: ["+polyType+"]. Will default to circle");
				}
			}
		}

		if (Tools.isNumeric(radius)) {
			this.radius=Float.parseFloat(radius);

		}
		this.hasWidget = hasWidget;



	}

	//Collect the gisobjects affected by the filter. Apply the filtering.
	public void create(WF_Context myContext) {

			
		myGis = myContext.getCurrentGis();
		if (myGis!=null) {

			final GisLayer gisLayer = myGis.getLayerFromLabel(targetLayer);
			if (gisLayer!=null) {
				if (hasWidget) {
					Log.d("vortex","Filter "+nName+" has a widget");
					//LinearLayout layersL = (LinearLayout)myGis.getWidget().findViewById(R.id.FiltersL);
					LayoutInflater li = LayoutInflater.from(myContext.getContext());
					View filtersRow = li.inflate(R.layout.filters_row, null);
					TextView filterNameT = filtersRow.findViewById(R.id.filterName);
					CheckBox lShow = filtersRow.findViewById(R.id.cbShow);
					filterNameT.setText(this.getLabel());
					lShow.setChecked(isActive);
					lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
							setActive(isChecked);
							myGis.getGis().invalidate();
						}


					});

					gisLayer.addObjectFilter(targetObjectType, this);
					//layersL.addView(filtersRow);
					//layersL.setVisibility(View.VISIBLE);
				}
			} else {
				o.addRow("");
				o.addRedText("Cannot add GisFilter in Block "+blockId+". Cannot find the Layer. Make sure this block comes AFTER the AddGisLayer Block");
			}
		}
	}

	private void setActive(boolean isChecked) {
		this.isActive=isChecked;
		
	}

	@Override
	public EvalExpr getExpression() {
		if (expressionE!=null && expressionE.size()==1)
			return expressionE.get(0);
		return null;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Bitmap getBitmap() {
		return null;
	}

	@Override
	public float getRadius() {
		return radius;
	}

	@Override
	public String getColor() {
		return color;
	}

	@Override
	public Style getStyle() {		
		return fillType;
	}

	
	@Override
	public boolean isActive() {
		return isActive;
	}


	@Override
	public PolyType getShape() {
		return polyType;
	}





}
