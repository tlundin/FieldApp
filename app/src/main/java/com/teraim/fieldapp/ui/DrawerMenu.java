package com.teraim.fieldapp.ui;

import android.app.Activity;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.android.material.navigation.NavigationView;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.non_generics.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DrawerMenu implements NavigationView.OnNavigationItemSelectedListener {

	private final Activity frameActivity;
	private SubMenu currentSubMenu = null;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle toggle;
	private int nextItemId = 1000;
	private SparseIntArray index;
	private Map<Integer, Workflow> workflowMap = new HashMap<>();
	private boolean open = false;
	private NavigationView navigationView;


	public DrawerMenu(Activity a, Toolbar toolbar) {
		frameActivity=a;
		this.createMenu(toolbar);
	}




	void createMenu(Toolbar toolbar) {

		mDrawerLayout = frameActivity.findViewById(R.id.drawer_layout);
		navigationView  = frameActivity.findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		toggle = new ActionBarDrawerToggle(
				frameActivity,                  /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				toolbar,  /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				Log.d("axion","drawer closed");
				open = false;
			}



			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				//createDrawerMenu(wfs);
				//mAdapter.notifyDataSetChanged();				
				super.onDrawerOpened(drawerView);
				Log.d("axion","drawer opened");
				open=true;
			}

		};
		mDrawerLayout.addDrawerListener(toggle);
		toggle.syncState();

	}

	public void addHeader(String label, int bgColor,int textColor) {
		Menu menu = navigationView.getMenu();
		currentSubMenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, label);
	}

	public void addItem(String label, Workflow wf,int bgColor,int textColor) {
		Log.d("axion","adding item "+label+" with color "+bgColor+" and text color "+textColor);
		if (currentSubMenu == null) {
			// Handle cases where addItem is called before addHeader, maybe add to a default section or throw an error
			// For this example, let's add a default header if none exists
			addHeader("Vecka "+Constants.getWeekNumber(), android.R.color.transparent, android.R.color.black);
		}

		int itemId = nextItemId++;
		MenuItem menuItem = currentSubMenu.add(Menu.NONE, itemId, Menu.NONE, label);
		menuItem.setCheckable(true); // Make items selectable

		// Store the workflow object
		workflowMap.put(itemId, wf);
	}

	private MenuItem findFirstSelectableItem(Menu menu) {
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (item.hasSubMenu()) {
				SubMenu subMenu = item.getSubMenu();
				for (int j = 0; j < subMenu.size(); j++) {
					MenuItem subMenuItem = subMenu.getItem(j);
					if (subMenuItem.isCheckable()) {
						return subMenuItem;
					}
				}
			}
		}
		return null;
	}
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks.
		// You can retrieve the associated Workflow object using workflowMap
		Workflow workflow = workflowMap.get(item.getItemId());
		if (workflow != null) {
			// Use the workflow object to determine which fragment to load
			Start.singleton.changePage(workflow,null);
			// Highlight the selected item
			navigationView.setCheckedItem(item.getItemId());

		} else
			Log.e("vortex","ups!!! Got null when looking for workflow ");

		mDrawerLayout.closeDrawer(GravityCompat.START);
		return true;
	}

	public boolean isDrawerOpen() {
		return open;
	}
	public void closeDrawer() {
		Log.d("axion","closing drawer");
		mDrawerLayout.closeDrawers();
	}

	public void openDrawer() {	
		toggle.syncState();
		mDrawerLayout.openDrawer(GravityCompat.START);
	}


	public ActionBarDrawerToggle getDrawerToggle() {
		// TODO Auto-generated method stub
		return toggle;
	}

	public void clear() {
		navigationView.getMenu().clear(); // Clear existing menu items
		currentSubMenu = null; // Reset current submenu
		workflowMap.clear(); // Clear the workflow map
		nextItemId = 1000; // Reset item ID counter
	}



}