package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.CoupledVariableGroupBlock;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;

public class WF_ClickableField_Slider extends WF_ClickableField implements EventListener {

	private final SeekBar sb;
	private final LinearLayout ll;
	private final String groupName;
	private final DisplayFieldBlock format;
	private Variable var = null;
	private int min,max;
	private EditText etview;
	private TextView tv;
	//private boolean addVarCalled = false;


	@SuppressWarnings("WrongConstant")
	public WF_ClickableField_Slider(String headerT, String descriptionT,
									WF_Context context, String id, boolean isVisible, final String groupName, final int min, final int max, DisplayFieldBlock format) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(format.isHorisontal()?R.layout.selection_field_normal_horizontal:R.layout.selection_field_normal_vertical,null),
				isVisible,format);

		this.min = min;
		this.max = max;

		context.registerEventListener(this, Event.EventType.onSave);
		ll = (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_slider_element,null);
		sb = ll.findViewById(R.id.spinnerOut);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (tv!=null)
					tv.setText(WF_ClickableField_Slider.this.min + progress +"");
				//remove me from group..
				if (groupName!=null && fromUser) {
					//Log.d("zaxx","groupname: "+groupName);
					CoupledVariableGroupBlock myGroup = myContext.getSliderGroup(groupName);
					if (myGroup!=null)
						myGroup.removeSliderFromCalibration(WF_ClickableField_Slider.this);
					else
						Log.e("zaxx","slidergroup missing");

				}
				/*
				else {
					if (!addVarCalled) {
						if (o!=null) {
							o.addRow("");
							o.addRedText("Variable not added to entry field. Could you be missing 'block_add_variable_to_entry_field' on clickablefield " + WF_ClickableField_Slider.this.getId());
						}
						Log.e("vortex", "Addvar was never called on clickablefield " + WF_ClickableField_Slider.this.getId());
					}
					Log.e("bladda", "tv was null in onProgressChanged");
				}
				*/
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				CoupledVariableGroupBlock myGroup = myContext.getSliderGroup(groupName);
				if (myGroup!=null)
					myGroup.resetCounter();
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				setValueFromSlider();
				CoupledVariableGroupBlock myGroup = myContext.getSliderGroup(groupName);

			}
		});
		if (groupName!=null) {
			Log.d("Vortex","Adding seekbar to group "+groupName);
			myContext.addSliderToGroup(groupName,this);
		}
		this.groupName = groupName ;



		this.format = format;

		sb.setMax(max);


	}

	@Override
	public LinearLayout getFieldLayout() {
		Log.d("brexit","Getting field layout for slide!!");
		if (this.format.isHorisontal())
			ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));
		else
			ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
		return ll;
	}

	@Override
	protected boolean shouldHideOutputView() {
		return true;
	}

	@Override
	public void onEvent(Event e) {

		if (myContext.myEndIsNear()) {
			Log.d("vortex","myendisnear...discarding event in slider");
			return;
		}
		if (getId() == null || !getId().equals(e.getProvider())) {
			if (getId() == null) {
				Log.e("gulp","MY ID IS NULL THIS IS NOT GOOD");
			}
			Log.d("nils","In onEvent for WF_ClickableField_Slider_OnSave. Provider: "+e.getProvider());
			//Check that group is not active. If it is, this event should be dropped.
			if (getGroup()!=null) {
				CoupledVariableGroupBlock group = myContext.getSliderGroup(getGroup());
				if (group!=null && group.isActive()) {
					Log.d("vortex", "DISCARD ACTIVE GROUP");

					return;
				}

			}
			refresh();
			setSeekBarAccordingToVariableValue();
		} else
			Log.d("nils","Discarded...from me");
	}

	@Override
	public String getName() {
		return "CLICKABLE_SLIDER "+this.getId();
	}


	@Override
	public void addVariable(final Variable var, boolean displayOut,
							String format, boolean isVisible, boolean showHistorical) {
		Integer val=null;

		if (this.var!=null) {
			Log.e("vortex","only one variable allowed for slider entryfield");
			String varId = "<null_value_given>";
			if (var!=null)
				varId = var.getId();
				o.addRow("");
				o.addRedText("Attempt to add more than one variable to slider entryfield. Variable not added: " +varId);

			return;
		}

		if (var.getValue()!=null && !var.getValue().isEmpty())
			val = Integer.parseInt(var.getValue());

		String limitDesc = GlobalState.getInstance().getVariableConfiguration().getLimitDescription(var.getBackingDataSet());


		if (limitDesc!=null) {

			if (!limitDesc.contains(",")) {
				String[] pair = limitDesc.split("-");
				if (pair.length == 2) {
					try {

						min = Integer.parseInt(pair[0]);
						max = Integer.parseInt(pair[1]);
						Log.d("vortex", "managed to set min max to " + min + "," + max);
						sb.setMax(max);
					} catch (NumberFormatException e) {
					}
				}
			}
		}

		if (val!=null && (val>max||val<min)) {
			Log.e("vortex","variable out of boundaries");
			o.addRow("");
			o.addRedText("Variable "+var.getId()+" is out of boundaries for slider. Value: "+val+" Min Max: ["+min+","+max+"]");
			if (val > max)
				max = val;
			else
				min = val;
			o.addRow("");
			o.addRedText("Readjusted bonds to [" + min + "," + max + "]");

		}

		super.addVariable(var,displayOut,null,isVisible,showHistorical);
		super.createInputFields();
		if (!myVars.isEmpty()) {
			this.var=var;
			//Has to set value for inner view as well.
			etview = myVars.get(var).view.findViewById(R.id.edit);
			OutC outC = myOutputFields.get(var);
			LinearLayout ll = outC.view;
			tv = ll.findViewById(R.id.outputValueField);

		} else {
			Log.e("vortex", "cannot initialize seekbar! empty? " + myVars.isEmpty());
			o.addRow("");
			o.addRedText("Cannot initialize seekbar with variable "+var.getId()+".");
			return;
		}
		Log.d("vortex","Calling initialize seekbar for "+var.getId()+" with value "+var.getValue());
		setSeekBarAccordingToVariableValue();
	}

	private void setSeekBarAccordingToVariableValue() {

		if (var!=null && var.getValue()!=null) {
			try {
				int value = Integer.parseInt(var.getValue());
				Log.d("Exeter","setting progress to "+value+" for "+this.label);
				sb.post(new Runnable() {
					@Override
					public void run() {
						sb.setProgress(value);
					}
				});
				//sb.setProgress(value);
            } catch (NumberFormatException e) {
				o.addRow("");
				o.addRedText("The variable used for slider " + this.getId() + " is not containing a numeric value");
			}

		}
	}


	private String getGroup() {
		return groupName;
	}

	public int getPosition() {
		return min + sb.getProgress();
	}

	public void setPosition(int value) {
		sb.setProgress(value-min);
	}



	public Integer getSliderValue() {
		try {
			if (var!=null && var.getValue()!=null)
				return Integer.parseInt(var.getValue());
		} catch (NumberFormatException e) {
			o.addRow("");
			o.addRedText("The variable used for slider " + this.getId() + " is not containing a numeric value: "+var.getValue());
		}
		Log.d("vortex","var null or integer exep in getSliderValue for "+this.getName());
		return null;
	}

	public void setValueFromSlider() {
		if (var!=null) {

			etview.setText(getPosition()+"");
			save();
			refresh();

		}
	}

	public int getMin() {
		return min;
	}
	public int getMax() {
		return max;
	}

	private boolean sliderWasDecreased = false;
	private boolean sliderWasIncreased = false;

	public void wasDecreased() {
		sliderWasDecreased=true;
	}

	public void wasIncreased() {
		sliderWasIncreased=true;
	}

	public boolean wasDecreasedLastTime() {
		boolean ret = sliderWasDecreased;
		sliderWasDecreased=false;
		return ret;
	}
	public boolean wasIncreasedLastTime() {
		boolean ret = sliderWasIncreased;
		sliderWasIncreased=false;
		return ret;
	}
}
