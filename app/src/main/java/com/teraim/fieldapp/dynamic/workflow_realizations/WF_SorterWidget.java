package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.ToggleButton;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter.FilterType;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Filter;
import com.teraim.fieldapp.log.LogRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class WF_SorterWidget extends WF_Widget {


    private transient WF_Filter existing=null;
	private transient WF_List targetList;
	private transient ToggleButton previousButton=null;

	public WF_SorterWidget(String name,WF_Context ctx, final String type, final WF_List targetList,final ViewGroup container,final String selectionField, final String displayField,String selectionPattern,boolean isVisible) {
		super(name,new LinearLayout(ctx.getContext()),isVisible,ctx);
		LinearLayout buttonPanel;

		o = LogRepository.getInstance();
		LayoutParams lp;
		int orientation =  ((LinearLayout)container).getOrientation();
		if (orientation==LinearLayout.HORIZONTAL)
			lp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		else 
			lp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

		buttonPanel = (LinearLayout) getWidget();

		//buttonPanel.setBackgroundColor(Color.parseColor("red"));
		buttonPanel.setOrientation(LinearLayout.VERTICAL);
		Log.d("vortex","orientation: "+((orientation==LinearLayout.HORIZONTAL)?"Horizontal":"Vertical"));
		buttonPanel.setLayoutParams(lp);

        LayoutInflater inflater = LayoutInflater.from(ctx.getContext());
		//Button WildCardButton = new Button(ctx.getContext());
		//WildCardButton.setText("*");
		//WildCardButton.setOnClickListener(new OnClickListener() {
		//	@Override
		//	public void onClick(View view) {
		//		if (existing !=null) {
		//		    Log.d("vortex","onclick wildcard");
		//			targetList.removeFilter(existing);
		//			existing = null;
        //            targetList.draw();
		//		}
		//	}
		//});

		this.targetList=targetList;

		if (type.equals("alphanumeric")) {
			final CompoundButton.OnCheckedChangeListener cl = new CompoundButton.OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
					String ch = compoundButton.getText().toString();
					Log.d("Strand","User pressed "+ch+" ischecked is "+isChecked);
					//This shall apply a new Alpha filter on target.
					//First, remove any existing alpha filter.
					targetList.removeFilter(existing);
					//untoggle the previous button.
					if (previousButton !=null) {
						//if previous was this button and not clicked, skip
						if (previousButton.equals(compoundButton) && isChecked) {
							//skip
						} else {
							previousButton.setOnCheckedChangeListener(null);
							previousButton.setChecked(false);
							previousButton.setOnCheckedChangeListener(this);
						}
					}
					if (isChecked) {
						existing = new WF_Column_Name_Filter(ch, ch, displayField, FilterType.prefix);
						targetList.addFilter(existing);
						previousButton = (ToggleButton)compoundButton;
					} else {
						existing = null;
						previousButton = null;
					}
					//running the filters will trigger redraw.
					targetList.draw();
				}
			};
			ToggleButton b;

            final String[] alfabet = {
                    "ABCD", "EFGH", "IJKL", "MNOP", "QRST", "UVXY", "ZÅÄÖ"};
			//Add wildcard button
            //buttonPanel.addView(WildCardButton);
            for (String c: alfabet) {
				b = new ToggleButton(ctx.getContext());
				b.setTextOn(c);
				b.setTextOff(c);
				b.setText(c);
				b.setOnCheckedChangeListener(cl);
				buttonPanel.addView(b);
				//Log.d("nils","Added button "+c);
			}
		} else if (type.equals("column") ) {
            final CompoundButton.OnCheckedChangeListener cl = new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    String ch = compoundButton.getText().toString();
                    Log.d("Strand","User pressed "+ch+" ischecked is "+isChecked);
                    //This shall apply a new Alpha filter on target.
                    //First, remove any existing alpha filter.
                    targetList.removeFilter(existing);
                    //untoggle the previous button.
                    if (previousButton !=null) {
                        //if previous was this button and not clicked, skip
                        if (previousButton.equals(compoundButton) && isChecked) {
                            //skip
                        } else {
                            previousButton.setOnCheckedChangeListener(null);
                            previousButton.setChecked(false);
                            previousButton.setOnCheckedChangeListener(this);
                        }
                    }
                    if (isChecked) {
                        existing = new WF_Column_Name_Filter(ch, ch, displayField, FilterType.sets);
                        targetList.addFilter(existing);
                        previousButton = (ToggleButton)compoundButton;
                    } else {
                        existing = null;
                        previousButton = null;
                    }
                    //running the filters will trigger redraw.
                    targetList.draw();
                }
            };

			//Generate buttons from artlista.
			//Pick fields that are of type Familj
			VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			Table t = al.getTable();
			List<List<String>> rows = t.getRowsContaining(selectionField,selectionPattern);
			if (rows!=null) {
				Log.d("nils","SORTERWIDGET: GETROWS RETURNED "+rows.size()+" FOR SELFIELD "+selectionField+" AND SELP: "+selectionPattern);

				int cIndex = t.getColumnIndex(displayField);
				if (cIndex != -1) {
					Set<String> txts = new TreeSet<String>();
					ToggleButton b;
					for(List<String>row:rows) {
						if (row.size()>cIndex) {
							String sortFacets = row.get(cIndex);
							if (sortFacets!=null) {
								String facets[] = sortFacets.split("\\|");
								if (facets.length>0) {
                                    Collections.addAll(txts, facets);
								}
							}
						}
						else {
							o.addText("");
							o.addCriticalText("SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the current row only contains "+row.size()+" elements");
							Log.e("vortex","SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the row is shorter:"+row.size());
							Log.e("vortex","Current row: "+row.toString() );
							o.addText("");
							o.addText("Current Columns:"+t.getColumnHeaders().toString());
							o.addText("Current row: "+row.toString() );
						}
					}
					//Add a wildcard button.
					//buttonPanel.addView(WildCardButton);
					for (String txt:txts)				
						if (txt !=null && txt.trim().length()>0) {
							b = new ToggleButton(ctx.getContext());
							b.setText(txt);
							b.setTextOff(txt);
							b.setTextOn(txt);
							b.setOnCheckedChangeListener(cl);
							buttonPanel.addView(b);				
							//Log.d("nils","Added button "+txt+" length "+txt.length());
						}


				} else{
					o.addText("");
					o.addCriticalText("Could not find column <display_field>: "+displayField+" in WF_SorterWidget. Check your xml for block_create_sort_widget");

				}
			} else {
				o.addText("");
				o.addCriticalText("Found no rows for selection: ["+selectionField+"] and pattern ["+selectionPattern+"] in WF_SorterWidget. Check your xml for block_create_sort_widget");
			}
		} else if (type.equals("column_toggle") ) {
			final CompoundButton.OnCheckedChangeListener dl = (button, isChecked) -> {
                String ch = button.getText().toString();
                Log.d("Strand","User pressed "+ch);
                //This shall apply a new Alpha filter on target.
                //First, remove any existing alpha filter.
                targetList.removeFilter(existing);

                if (isChecked) {
                    existing = new WF_Column_Name_Filter(ch, ch, displayField, FilterType.sets);
                    //existing = new WF_Column_Name_Filter(ch,ch,Col_Art)
                    targetList.addFilter(existing);
                    //running the filters will trigger redraw.
                }
                targetList.draw();
            };
			//Generate buttons from artlista.
			//Pick fields that are of type Familj
			VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			Table t = al.getTable();
			List<List<String>> rows = t.getRowsContaining(selectionField,selectionPattern);
			if (rows!=null) {
				Log.d("nils","SORTERWIDGET: GETROWS RETURNED "+rows.size()+" FOR SELFIELD "+selectionField+" AND SELP: "+selectionPattern);

				int cIndex = t.getColumnIndex(displayField);
				if (cIndex != -1) {
					Set<String> txts = new TreeSet<String>();


					for(List<String>row:rows) {
						if (row.size()>cIndex) {
							String sortFacets = row.get(cIndex);
							if (sortFacets!=null) {
								String facets[] = sortFacets.split("\\|");
								if (facets.length>0) {
									Collections.addAll(txts, facets);
								}
							}

						}
						else {
							o.addText("");
							o.addCriticalText("SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the current row only contains "+row.size()+" elements");
							Log.e("vortex","SorterWidget: column to sort on ["+displayField+"] was found in column# "+(cIndex+1)+" but the row is shorter:"+row.size());
							Log.e("vortex","Current row: "+row.toString() );
							o.addText("");
							o.addText("Current Columns:"+t.getColumnHeaders().toString());
							o.addText("Current row: "+row.toString() );
						}
					}
					//Add a wildcard button.
					ToggleButton toggleB;
					//ToggleButton toggleB = new ToggleButton(ctx);


					for (String txt:txts)
						if (txt !=null && txt.trim().length()>0) {
							toggleB = new ToggleButton(ctx.getContext());
							toggleB.setTextOn(txt);
							toggleB.setTextOff(txt);
							toggleB.setChecked(false);
							toggleB.setOnCheckedChangeListener(dl);
							buttonPanel.addView(toggleB);
							Log.d("cair","Added toggle button "+txt+" length "+txt.length());
						}


				} else{
					o.addText("");
					o.addCriticalText("Could not find column <display_field>: "+displayField+" in WF_SorterWidget. Check your xml for block_create_sort_widget");

				}
			} else {
				o.addText("");
				o.addCriticalText("Found no rows for selection: ["+selectionField+"] and pattern ["+selectionPattern+"] in WF_SorterWidget. Check your xml for block_create_sort_widget");
			}
		}
		else 
			Log.e("parser","Sorry, unknown filtering type");


	}



	private void removeExistingFilter() {
		if (existing!=null) {
			targetList.removeFilter(existing);
			targetList.draw();
			existing = null;
		}

	}

	/* (non-Javadoc)
	 * @see com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget#hide()
	 */
	@Override
	public void hide() {
		super.hide();
		removeExistingFilter();
	}



}
