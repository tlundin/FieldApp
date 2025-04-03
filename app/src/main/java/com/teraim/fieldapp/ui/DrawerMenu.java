package com.teraim.fieldapp.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.ui.DrawerMenuAdapter.RowType;

import java.util.ArrayList;
import java.util.List;

public class DrawerMenu {

	private final Activity frameActivity;
	private DrawerMenuAdapter mAdapter;
	private List<DrawerMenuItem> items;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private int currentIndex=0;
	private SparseArray<Integer> index;
	private List<Workflow> workflowsL;

	public DrawerMenu(Activity a) {
		frameActivity=a;
		createMenu();
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		//private boolean firstTimeClick=true;

		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			/*
			if (firstTimeClick) {
				// Create new fragment and transaction
				Fragment newFragment = new BackgroundFragment();
				FragmentTransaction transaction = frameActivity.getFragmentManager().beginTransaction();
				// Replace whatever is in the fragment_container view with this fragment,
				// and add the transaction to the back stack
				transaction.replace(R.id.content_frame, newFragment);			
				// Commit the transaction
				transaction.commit();
				firstTimeClick=false;

			}
			*/
			if (items.get(position).getViewType()!=RowType.HEADER_ITEM.ordinal()) {				
				selectItem(position);
			} else {
				Log.d("nils","header selected. no action");
			}
		}

		/** Swaps fragments in the main content view */
		private void selectItem(int position) {

			// Highlight the selected item, update the title, and close the drawer
			mDrawerList.setItemChecked(position, true);
			//String wfId = mapItemsToName.get(position);
			Integer pos = index.get(position);
			if (pos!=null) {
				Workflow wf = workflowsL.get(pos);
				//Workflow wf = gs.getWorkflowFromLabel(wfId);
				if (wf == null)
					Log.e("vortex","ups!!! Got null when looking for workflow ");
				else {
					// Create a new fragment and specify the  to show based on position
					DrawerMenu.this.closeDrawer();
					// Insert the fragment by replacing any existing fragment
					//TODO: This might not work...might need to know statusvariable.
					Start.singleton.changePage(wf,null);
				}
			} else
				Log.e("vortex","Could not find any entry for menu position "+position);
		}
	}
	private void createMenu() {
		//drawer items
		//different if already created?
		items = new ArrayList<DrawerMenuItem>(); 
		mAdapter = new DrawerMenuAdapter(frameActivity, items);

		mDrawerLayout = frameActivity.findViewById(R.id.drawer_layout);
		mDrawerList = frameActivity.findViewById(R.id.left_drawer);

		// Set the adapter for the list view
		mDrawerList.setAdapter(mAdapter);
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(
				frameActivity,                  /* host Activity */
				mDrawerLayout,         /* DrawerLayout object */
				  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);

			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				//createDrawerMenu(wfs);
				//mAdapter.notifyDataSetChanged();				
				super.onDrawerOpened(drawerView);

			}

		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.addDrawerListener(mDrawerToggle);

		mAdapter.notifyDataSetChanged();				

		workflowsL = new ArrayList<Workflow>();
		index = new SparseArray<>();
		currentIndex=0;

	}

	public void addHeader(String label, int bgColor,int textColor) {
		items.add(new DrawerMenuHeader(label,bgColor,textColor));
	}

	public void addItem(String label, Workflow wf,int bgColor,int textColor) {
		//add the workflow reference to a list
		workflowsL.add(wf);
		//keep track of the location.
		index.put(items.size(),currentIndex++);
		items.add(new DrawerMenuSelectable(label,bgColor,textColor));

	}

	public void closeDrawer() {
		mDrawerLayout.closeDrawers();
	}

	public void openDrawer() {	
		mAdapter.notifyDataSetChanged();				
		mDrawerLayout.openDrawer(Gravity.LEFT);	
	}


	public ActionBarDrawerToggle getDrawerToggle() {
		// TODO Auto-generated method stub
		return mDrawerToggle;
	}

	public void clear() {
		items.clear();
		mAdapter.notifyDataSetChanged();				
	}



}