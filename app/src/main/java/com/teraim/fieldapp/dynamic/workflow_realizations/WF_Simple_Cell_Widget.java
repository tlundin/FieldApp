package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.non_generics.Constants;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_Simple_Cell_Widget extends WF_Widget implements WF_Cell, EventListener {


	private final Map<String, String> myHash;
	private final CheckBox myCheckBox;
	private Variable myVariable = null;
	private Drawable originalBackground;
	private final Context ctx;
	private ActionMode mActionMode;
	private static final int backgroundColor=Color.TRANSPARENT;

	private void setBackgroundColor(int color) {
		if (originalBackground==null)
			originalBackground = getWidget().getBackground();
		getWidget().setBackgroundColor(color);
	}
	@SuppressLint("NewApi")
	private void revertBackgroundColor() {
		if (originalBackground!=null) {
			getWidget().setBackground(originalBackground);
			originalBackground=null;
		} else
			getWidget().setBackgroundColor(backgroundColor);

	}

	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.tagpopmenu, menu);
			setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			MenuItem x = menu.getItem(0);
			MenuItem y = menu.getItem(1);
			MenuItem z = menu.getItem(2);
			z.setVisible(false);

			if (myVariable!=null) {

				List<String> row = myVariable
						.getBackingDataSet();
				String url = al.getUrl(row);

				if (url == null || url.length() == 0)
					x.setVisible(false);
				else
					x.setVisible(true);
				if (row != null && al.getVariableDescription(row) != null
						&& al.getVariableDescription(row).length() > 0)
					y.setVisible(true);
				else
					y.setVisible(false);

			} else {
				x.setVisible(false);
				y.setVisible(false);
				z.setVisible(false);
			}
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			List<String> row = null;
			String msg = "";
			row = myVariable.getBackingDataSet();

			switch (item.getItemId()) {
				case R.id.menu_goto:
					if (row != null) {
						String url = al.getUrl(row);
						if (url != null) {
							Intent browse = new Intent(Intent.ACTION_VIEW,
									Uri.parse(url));
							browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							GlobalState.getInstance().getContext().startActivity(browse);
						}
					}
					return true;

				case R.id.menu_info:
					if (row != null && ctx != null) {
						msg =
								"Var_Label: " + al.getVarLabel(row) + "\n" +
										"Var_Desc : " + al.getVariableDescription(row) + "\n";
						msg +=
								" Group Label : " + al.getGroupLabel(row) + "\n" +
										"Group_Description:  " + al.getGroupDescription(row) + "\n";


						new AlertDialog.Builder(ctx)
								.setTitle(GlobalState.getInstance().getString(R.string.description))
								.setMessage(msg)
								.setPositiveButton(android.R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												mode.finish();
											}
										})
								.setIcon(android.R.drawable.ic_dialog_info).show();

					}
					return true;
				default:
					return false;
			}
		}

			// Called when the user exits the action mode
			@Override
			public void onDestroyActionMode(ActionMode mode) {

				mActionMode = null;
				revertBackgroundColor();
			}
		};








		public WF_Simple_Cell_Widget(Map<String, String> columnKeyHash, String headerT, String descriptionT,
									 final WF_Context context, String id,boolean isVisible) {
			super(id,new CheckBox(context.getContext()),isVisible,context);
			myCheckBox = (CheckBox)this.getWidget();
			myCheckBox.setGravity(Gravity.CENTER_VERTICAL);
			myHash = columnKeyHash;
			ctx = context.getContext();

			getWidget().setOnLongClickListener(new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {

					if (mActionMode != null) {
						return false;
					}

					// Start the CAB using the ActionMode.Callback defined above
					mActionMode = ((Activity) ctx)
							.startActionMode(mActionModeCallback);
					myCheckBox.setSelected(true);
					return true;

				}
			});


			myCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (myVariable!=null) {
						if (isChecked)
							myVariable.setValue("true");
						else
							myVariable.deleteValue();

						context
								.registerEvent(new WF_Event_OnSave("table_cell", null,null));
					}
				}
			});
			context.registerEventListener(this, Event.EventType.onSave);
		}

		@Override
		public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue) {
			myVariable = GlobalState.getInstance().getVariableCache().getCheckedVariable(myHash, varId, prefetchValue, prefetchValue!=null);
			//Log.d("bozo","prefetchvalue for "+varId+"is "+prefetchValue);
			if (myVariable!=null) {
				String val = myVariable.getValue();
				myCheckBox.setChecked(val!=null && val.equals("true"));
			}

		}


		@Override
		public boolean hasValue() {
			return myVariable!=null && myVariable.getValue()!=null;
		}


		@Override
		public void refresh() {
			//this.getWidget().refreshDrawableState();
			//this.getWidget().requestLayout();
		}

		@Override
		public Map<String, String> getKeyHash() {
			return null;
		}

		@Override
		public Set<Variable> getAssociatedVariables() {
			if (myVariable!=null) {
				Set<Variable> ret = new HashSet<Variable>();
				ret.add(myVariable);
				return ret;
			}
			return null;

		}


		@Override
		public void onEvent(Event e) {
			if (e.getProvider().equals(Constants.SYNC_ID)) {
				String val = myVariable.getValue();
				myCheckBox.setChecked(val!=null && val.equals("true"));
			}
		}

		@Override
		public String getName() {
			return "cell "+this.getId();
		}
	}
