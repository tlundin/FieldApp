package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_Table_Row extends WF_Widget implements Listable,Comparable<Listable> {
	private static final String TAG = "WF_Table_Row";

	private transient WF_Table myWfTable;
	private List<String> myRow;
	private List<WF_Cell> myColumns;
	private final WF_Context myContext;
	private LinearLayout myBody;
	private final TextView headerT;
	private final String id;

	public WF_Table_Row(WF_Table myWfTable,String id,View v,WF_Context ctx,boolean isVisible) {
		super(id,v,isVisible,ctx);
		myColumns=null;
		myContext = ctx;
		headerT = v.findViewById(R.id.headerT);
		this.id=id;

		this.myWfTable = myWfTable;
		//Log.d(TAG,"Added header "+this.getLabel() );

	}
	//Add the Entry (row header)
	//Note that the row here could be any row belonging to the entry.
	public void addEntryField(List<String> row) {
		myRow=row;
		String label = getLabel();
		if (label==null) {
			Log.e("vortex","label null for row with id "+id);
			label="*null*";
		}
		//Headertext is not there for header row..

		if (headerT!=null)
			headerT.setText(label);
		else
			headerT.setVisibility(View.GONE);



	}


	@Override
	public String getSortableField(String columnId) {
		return al.getTable().getElement(columnId, myRow);
	}


	//Not supported.
	@Override
	public long getTimeStamp() {
		return 0;
	}



	@Override
	public boolean hasValue() {
		if (myColumns==null)
			return false;
		for (WF_Cell w:myColumns)
			if (w.hasValue())
				return true;

		return false;

	}



	@Override
	public String getLabel() {
		return al.getEntryLabel(myRow);
	}

	@Override
	public void refresh() {

		if (myColumns==null)
			return ;
		for (WF_Cell w:myColumns)
			w.refresh();

	}

	//Return any cells assoicated variables since each cell contain same.
	@Override
	public Set<Variable> getAssociatedVariables() {
		if (myColumns!=null && !myColumns.isEmpty()) {
			return myColumns.get(0).getAssociatedVariables();
		}
		return null;
	}


	@Override
	public int compareTo(Listable other) {
		return this.getLabel().compareTo(other.getLabel());
	}

	public View addNoClickHeaderCell(String label, String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header,null);
		TextView tv= emptyCell.findViewById(R.id.headerT);
		tv.setText(label);
		((TableRow)this.getWidget()).addView(emptyCell);
		if (backgroundColor!=null)
			emptyCell.setBackgroundColor(Tools.getColorResource(myContext.getContext(),backgroundColor));
		if (textColor!=null)
			tv.setTextColor(Tools.getColorResource(myContext.getContext(),textColor));
		return emptyCell;
	}

	private int headerIndex=1;

	//Add a cell of purely graphical nature.
	public void addHeaderCell(String label, String backgroundColor, String textColor) {
		View headerC = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header,null);
		TextView headerT = headerC.findViewById(R.id.headerT);
		headerT.setText(label);
		((TableRow)this.getWidget()).addView(headerC);
		if (backgroundColor!=null)
			headerC.setBackgroundColor(Tools.getColorResource(myContext.getContext(),backgroundColor));
		if (textColor!=null)
			headerT.setTextColor(Tools.getColorResource(myContext.getContext(),textColor));
		//If no column selected, selectedIndex is -1

		headerC.setOnClickListener(new OnClickListener() {
			//headerIndex is a counter that is starting at 1.
			final int myHeaderIndex=headerIndex;

			@Override
			public void onClick(View v) {
				Log.d(TAG,"column "+myHeaderIndex+" clicked!");
				myWfTable.setSelectedColumnIndex(myHeaderIndex);
				Log.d(TAG,"selectedcolumnindex is now "+myWfTable.getSelectedColumnIndex());

			}

		});


		headerIndex++;
	}


	//Add a Vortex Cell.
	public void addCell(String colHeader, String colKey, Map<String,String> columnKeyHash, String type, int width, WF_Cell.CellType cellType) {

		if (myColumns==null)
			myColumns = new ArrayList<WF_Cell>();
			WF_Cell widget;
			if ("simple".equals(type)) {
				View simpleCellL = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_simple, null);
				widget = new WF_Simple_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow),
						myContext, this.getId() + colKey, true, cellType);
			}
			else {
				widget = new WF_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow),
						myContext, this.getId() + colKey, true,cellType);

			if (width!=-1)
				widget.getWidget().setMinimumWidth(width);


			}


			//TODO:ADD FORMAT! NULL BELOW
			//SHOW HISTORICAL IS TRUE!
			//Variable v= al.getVariableUsingKey(columnKeyHash, this.getKey());
			//if (v!=null)
			//	widget.addVariable(v, true,null,true,true);
			myColumns.add(widget);
			((TableRow)getWidget()).addView(widget.getWidget());
			//Log.d(TAG,"Row now has "+((TableRow) getWidget()).getChildCount()+" children");
	}

	public TextView addAggregateTextCell(String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_text_aggregate,null);
		View bg = emptyCell.findViewById(R.id.outputContainer);
		TextView tv= emptyCell.findViewById(R.id.contentT);

		((TableRow)this.getWidget()).addView(emptyCell);
		bg.setBackgroundColor(Tools.getColorResource(myContext.getContext(),backgroundColor));
		tv.setTextColor(Tools.getColorResource(myContext.getContext(),textColor));
//		Log.d(TAG,"var for row "+this.getLabel());
//		Log.d(TAG,"v: "+al.getVarName(myRow)+" key: "+al.getKeyChain(myRow));
		return tv;
	}

	public CheckBox addAggregateLogicalCell(String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_logical_aggregate,null);

		//View bg = (View)emptyCell.findViewById(R.id.outputContainer);
		CheckBox cb= emptyCell.findViewById(R.id.contentT);

		((TableRow)this.getWidget()).addView(emptyCell);
		emptyCell.setBackgroundColor(Tools.getColorResource(myContext.getContext(),backgroundColor));
		cb.setTextColor(Tools.getColorResource(myContext.getContext(),textColor));
		return cb;
	}


	public List<WF_Cell> getCells() {
		return myColumns;
	}

	@Override
	public String getKey() {
		return getLabel();
	}
	@Override
	public Map<String, String> getKeyChain() {
		// TODO Auto-generated method stub
		return null;
	}


}
