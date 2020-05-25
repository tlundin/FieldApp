package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_Table extends WF_List  {

	//protected final List<Listable> tableRows = new  ArrayList<Listable>(); //Instantiated in constructor
	protected final List<Filter> myFilters=new ArrayList<Filter>();
	protected final List<Sorter> mySorters=new ArrayList<Sorter>();
	private final View headerCell;
	protected List<? extends Listable> filteredList;

	private final WF_Context myContext;
	private final GlobalState gs;
	private final VariableConfiguration al;
	private String myVariator;
	private LinearLayout headerV;
	private final LayoutInflater inflater ;
	private final TableLayout tableView;
	//The index of the currently selected column.
	private int selectedColumnIndex =-1;

	private int rowNumber=0, numberOfColumns =1;
    private final WF_Table_Row headerRow;
	private boolean tableTypeSimple;

    //How about using the Container's panel?? TODO
	public WF_Table(String id,String label,boolean isVisible,WF_Context ctx, View tableV) {
		super(id,isVisible,ctx,tableV);	
		tableView = tableV.findViewById(R.id.table);
        myContext = ctx;
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		al = gs.getVariableConfiguration();
		tableTypeSimple=false;
		//myTable = new GridView(ctx.getContext());
		
		//Create rows.
		inflater = (LayoutInflater)ctx.getContext().getSystemService
				(Context.LAYOUT_INFLATER_SERVICE); 
		
		//Add the header.
        String colHeadId = "TableHeader";
        headerRow = new WF_Table_Row(this, colHeadId,inflater.inflate(R.layout.header_table_row, null),myContext,true);
		//Add a first empty cell 
		headerCell = headerRow.addNoClickHeaderCell(label, null, null);
		tableView.addView(headerRow.getWidget());
		//
		//tableView.setStretchAllColumns (false);
		addSorter(new WF_Alphanumeric_Sorter());

	}


	private Map<String, Map<String, String>> allInstances;

	private final Map<String,Set<String>>varIdMap=new HashMap<String,Set<String>>();
	//Creates new rows and adds dataset to each.
	public void addRows(List<List<String>> rows,String variatorColumn, String selectionPattern) {

		this.myVariator=variatorColumn;
		allInstances = gs.getDb().preFetchValues(myContext.getKeyHash(), selectionPattern, myVariator);
		Log.d("nils","in update entry fields. AllInstances contain "+allInstances.size()+ ": "+allInstances.toString());

		//Rows are not containing unique entries. only need one of each.
		Map<String,List<String>>uRows = new HashMap<String,List<String>>();
		for (List<String> row:rows) {
			
			String key = al.getEntryLabel(row);
			if (uRows.get(key)==null)
				uRows.put(key,row);							//Smprov:bjork:selectedSpy1
															//smaprov:ek:selectedSpy1
															//smaprov:bjork:selectedSpy2
			//collect all variables existing under one label. GrName_VarGrId_VarId
			Set<String> s = varIdMap.get(key);
			if (s==null){
				s=new HashSet<String>();
				varIdMap.put(key, s);
			}
			s.add(al.getVarName(row));
		}	
		//Now add only the unique entrylabel ones
		for (String rowKey:uRows.keySet()) {
			addRow(uRows.get(rowKey));
		}
	}
	//Create a new row + dataset.
    private void addRow(List<String> row) {
		WF_Table_Row rowWidget = new WF_Table_Row(this,(rowNumber++)+"",inflater.inflate(R.layout.table_row, null),myContext,true);
		rowWidget.addEntryField(row);
		add(rowWidget);
	}

	//only selectedcolumn should be uncollapsed. All other should be closed.
	public void setSelectedColumnIndex(int index) {
		boolean showAll=true;
		if (index == selectedColumnIndex) {
			selectedColumnIndex = -1;
			Log.d("vortex","Deselected existing column.");

		} else {
			showAll = false;
			selectedColumnIndex = index;
		}


		for (int i = 1; i < getNumberofColumns(); i++) {
			if (showAll)
				tableView.setColumnCollapsed(i,false);
			else
				tableView.setColumnCollapsed(i, selectedColumnIndex!=i);
		}

	}

	public int getSelectedColumnIndex() {
		return selectedColumnIndex;
	}

	private int getNumberofColumns() {
		return numberOfColumns;
	}

	public void addColumns(List<String> labels,
			List<String> columnKeyL, String type, String widthS,String backgroundColor, String textColor) {

		boolean useColumKeyAsHeader = labels==null;
		
		//Add as many columns as there are keys. Check if labels are used or if the columnkey should be used.
		if (useColumKeyAsHeader&&labels.size()<columnKeyL.size()) {
			Log.e("vortex","There are too few labels in addColumns! Labels: "+labels.toString());
			o.addRow("");
			o.addRedText("There are too few labels in addColumns! Labels: "+labels.toString());
			return;
		}
		if (columnKeyL==null) {
			Log.e("vortex","columnkeys missing for addColumn!!");
			o.addRow("");
			o.addRow("columnkeys missing for addColumn!!");
			return;
		}
		String k,l;
		int width = -1;
		if (widthS!=null) try {
			Log.d("baloba","seting with "+width);
			width = Integer.parseInt(widthS);

		} catch (NumberFormatException e) {}

        for (int i=0;i<columnKeyL.size();i++) {
			
			k = columnKeyL.get(i);
			l = (useColumKeyAsHeader?null:labels.get(i));
			//Add the column
			addColumn(l,k,type,width,backgroundColor,textColor);
			numberOfColumns++;
			
		}
		if (headerCell!=null) {
			Log.d("baloba","Headercell!!!");
			headerCell.setMinimumWidth(width);
		}
		//for (String s:columnKeys)
		//	Log.d("vortex","my columns: "+s);
		if (type!=null && type.equals("simple"))
			tableTypeSimple=true;
		
	}
/*
	public void unCollapse() {
		if (this.getSelectedColumnIndex() == -1) {
			if (columnKeys != null) {
				int numColumns = columnKeys.size();
				Log.d("vortex", "uncollapsing all columns: " + numColumns);
				for (int i = 1; i < numColumns + 1; i++) {
					tableView.setColumnCollapsed(i, false);
				}
			}
		}
	}
*/
	
	//Keep column keys in memory.
	private final List<String> columnKeys = new ArrayList<String>();

	
	private void addColumn(String header, String colKey, String type, int width, String backgroundColor, String textColor) {
		//Copy the key and add the variator.
		Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
		colHash.put(myVariator, colKey);
		
		//Add header to the header row? Duh!!
		headerRow.addHeaderCell(header,backgroundColor,textColor);
		
		
		//Create all row entries.
		for (Listable l:get()) {
			WF_Table_Row wft = (WF_Table_Row)l;
			wft.addCell(header, colKey, colHash, type, width);
		}
		columnKeys.add(colKey);
	}



	//Keep track of Aggregate column cells.
	public enum AggregateFunction {
		AND, OR, COUNT, SUM, MIN, MAX, aggF, AVG
	}

    private class AggregateColumn implements EventListener {



		AggregateColumn(String label, Expressor.EvalExpr expressionE, String format, AggregateFunction aggregationFunction, boolean isLogical) {
			myCells=new ArrayList<View>();
			myRows = new ArrayList<WF_Table_Row>();
			this.expressionE=expressionE;
			this.format=format;
			myContext.registerEventListener(this , Event.EventType.onSave);
			aggF = aggregationFunction;
			this.label=label;
			this.isLogical=isLogical;
		}

		final boolean isLogical;
		final Expressor.EvalExpr expressionE;
		final List<View>myCells;
		final List<WF_Table_Row>myRows;
		final String format;
		final AggregateFunction aggF;
		final String label;

		public List<View> getMyCells() {
			return myCells;
		}

		void addRow(View textView, WF_Table_Row myRow) {
				myCells.add(textView);
				myRows.add(myRow);
		}

		@Override
		public void onEvent(Event e) {
			if (e.getType()==Event.EventType.onSave) {
				Log.d("vortex","caught onSave in aggregate_column!");
				if (myCells!=null) {
					//loop over mycells (or over rows...doesnt matter. Equal number)
					TextView tv=null; CheckBox cb = null;
					for(int i=0;i<myCells.size();i++) {
						if (!isLogical)
							tv = (TextView)myCells.get(i);
						else
							cb = (CheckBox)myCells.get(i);
						WF_Table_Row row = myRows.get(i);
						//if (aggregationFunction.equals(AgAND)
						Set<Variable> vars;

						boolean completeResB = true;
						Integer completeRes = 0;
						if (aggF==AggregateFunction.MIN || aggF==AggregateFunction.MAX)
							completeRes=null;

						boolean done=false;
						//Aggregate over all cells in a row.
						for (WF_Cell cell:row.getCells()) {
							vars=cell.getAssociatedVariables();
							//Evaluate expression with given variables as context.
							//Log.d("vortex","Cell has these variables: ");

							//for (Variable v:vars)
							//	Log.d("vortex",v.getId());
							if (isLogical) {
								Boolean result = Expressor.analyzeBooleanExpression(expressionE, vars);

								switch (aggF) {
									case AND:
										if (result ==null || !result) {
											completeResB=false;
											done=true;
										}
										break;
									case OR:
										if (result!=null && result) {
											completeResB=true;
											done= true;
										} else
											completeResB=false;
										break;
								}
							}
							else {
								String result = Expressor.analyze(expressionE, vars);
								if (!Tools.isNumeric(result)) {
									//Log.e("vortex", "couldnt use " + result + " for " + aggF + ". Not numeric");
									continue;
								}
								//Numeric result.
								int res = Integer.parseInt(result);
								Log.e("vortex", "got numeric "+res);
								switch (aggF) {

									case SUM:
									case AVG:
										completeRes+=res;
										break;
									case COUNT:
										completeRes++;
										break;
									case MIN:
										if (completeRes==null || completeRes>res)
											completeRes=res;
										break;
									case MAX:
										if (completeRes==null || completeRes<res)
											completeRes=res;
										break;
								}
							}
							if (done) {
								//Log.d("vortex","I am done..exiting");
								break;
							}
						}
						//Here we are done for row.
						if (isLogical) {
							cb.setChecked(completeResB);
						} else {
							if (completeRes==null) {
								Log.e("vortex","no result..completeRes is null");
								tv.setText("");
							} else {
								if (aggF==AggregateFunction.AVG) {
									int size = row.getCells().size();
									completeRes=completeRes/size;
								}
								tv.setText(completeRes.toString());
							}
						}
					}
				}
			}
		}

		@Override
		public String getName() {
			return "Aggregate_column";
		}


	}


	public void addAggregateColumn(String label, Expressor.EvalExpr expressionE, String aggregationFunction, String format, String width, boolean isDisplayed, String backgroundColor, String textColor) {
		AggregateFunction aggF;
		try {
			aggF = AggregateFunction.valueOf(aggregationFunction.toUpperCase());
		} catch (IllegalArgumentException e) {
			o.addRow("");
			o.addRedText("The aggregate function "+aggregationFunction+" is not supported. Supported are: "+
					Arrays.asList(AggregateFunction.values()));
			return;
		}
		boolean isLogical = (aggF==AggregateFunction.AND || aggF == AggregateFunction.OR);
		AggregateColumn aggregateCol = new AggregateColumn(label, expressionE, format,aggF,isLogical);

		if (label==null || label.isEmpty())
			label = aggregationFunction;
		//Add header
		headerRow.addNoClickHeaderCell(label,backgroundColor,textColor);
		int widthI = 50;
		if (width!=null) try {
			widthI = Integer.parseInt(width);
		} catch (NumberFormatException e) {}
        //Add elements
		View view;
		for (Listable l:get()) {
			WF_Table_Row wft = (WF_Table_Row)l;
			if (!isLogical) {
				TextView tv = wft.addAggregateTextCell(backgroundColor,textColor);
				view = tv;
				aggregateCol.addRow(tv, wft);
				tv.setMinWidth(widthI);
			} else {
				CheckBox cb = wft.addAggregateLogicalCell(backgroundColor,textColor);
				aggregateCol.addRow(cb, wft);
				view = cb;
			}
			if (view!=null) {
				if (!isDisplayed)
					view.setVisibility(View.INVISIBLE);
			}
		}

		//trigger refresh.

		aggregateCol.onEvent(new WF_Event_OnSave("initial"));

	}





	@Override
	protected void prepareDraw() {
		tableView.removeAllViews();
		tableView.addView(headerRow.getWidget());
		
	}

	public void addVariableToEveryCell(String variableSuffix,
			boolean displayOut, String format, boolean isVisible,
			boolean showHistorical, String initialValue) {
		//Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
		//get rows.
		for (Listable l:get()) {
			WF_Table_Row wft = (WF_Table_Row)l;
			Set<String> varIds = varIdMap.get(wft.getLabel());
			String varGrId=null;
			if (varIds==null) {
				Log.e("vortex","No variableIds found for "+wft.getLabel());
				return;
			} else {
				//Log.d("vortex","varIds contains "+varIds.size()+" variables");
				for (String varGr:varIds) {
					//Log.d("vortex","varGr: "+varGr);
					if (varGr.endsWith(variableSuffix)) {
						varGrId = varGr;
						break;
					}
				}
			}
			if (varGrId==null) {
				Log.e("vortex","found no variable with suffix: "+variableSuffix);
				o.addRow("");
				o.addRedText("Could not add variables with suffix: "+variableSuffix+". No instances found. Check spelling and case");
				return;
			}
			//Construct variablename. 
			//String varId = varNamePrefix+Constants.VariableSeparator+varGrId+Constants.VariableSeparator+variableSuffix;
			//Log.d("vortex","Adding variable "+varGrId);
			if (tableTypeSimple) {
				List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varGrId);
				if (row != null) {
					Variable.DataType type = gs.getVariableConfiguration().getnumType(row);
					if (type != Variable.DataType.bool) {
						Log.e("vortex", "use of non boolean type variable in simple column. Forbidden!");
						o.addRow("");
						o.addRedText("Variable with suffix [" + variableSuffix+ "] is not type Boolean. Only boolean variables are allowed for checkboxes. Please check your XML.");
						return;
					}
				}
			}
			//Get prefetchvalue per variator value. 
			Map<String, String> valueMap = allInstances.get(varGrId);
			
				
			//add to each cell
			
			int columnIndex=0;
			for (WF_Cell cell:wft.getCells()) {
				//Get columnKey
				String colKey = columnKeys.get(columnIndex);

				//Get value for this column
				String prefetchValue = null;
				if (valueMap!=null) {
					prefetchValue = valueMap.get(colKey);
				}
				if (prefetchValue!=null) {
					Log.d("vortex","valueMap: "+valueMap.toString()+" colKey: "+colKey);
					Log.d("vortex","found prefetch value "+prefetchValue);
				}
				cell.addVariable(varGrId, displayOut, format, isVisible, showHistorical,prefetchValue);
				columnIndex++;
			}
		
		}
	}



	@Override
	public void draw() {
		super.draw();
		Log.d("vortex", "This is after draw");
		Log.d("vortex", "selected column: " + selectedColumnIndex);
		if (selectedColumnIndex != -1 && !tableView.isColumnCollapsed(selectedColumnIndex))
			Log.d("vortex", "And this matches the collapsed in tableview");

		//Need to check that all cells are visible that should be.
		if (selectedColumnIndex != -1) {
			for (Listable l : get()) {
				WF_Table_Row wft = (WF_Table_Row) l;
				//Log.d("vortex", l.getLabel() + " Cells: " + wft.getCells().size());
				//for (int i =0;i<wft.getCells().size();i++) {
				WF_Cell cell = wft.getCells().get(selectedColumnIndex-1);
				if (!cell.getWidget().isShown())
					cell.getWidget().setVisibility(View.VISIBLE);
//				Log.d("Vortex", "cell " + selectedColumnIndex + " is shown? " + cell.getWidget().isShown());
			}
		}
	}
}
