package com.teraim.fieldapp.ui;


import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.DrawerMenuAdapter.RowType;

public class DrawerMenuSelectable implements DrawerMenuItem {

	    private final String  str1;
	    private final int bgColor;
    private int textColor;

	    public DrawerMenuSelectable(String text1,int bgColor,int textColor) {
	        this.str1 = text1;
	        this.bgColor = bgColor;
	        this.textColor= textColor;
	    }

	    @Override
	    public int getViewType() {
	        return RowType.LIST_ITEM.ordinal();
	    }

	    @Override
	    public View getView(LayoutInflater inflater, View convertView) {
	        View view;
			TextView tv;
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.drawer_menu_selectable, null);
				tv = view.findViewById(R.id.list_content1);
				//Log.d("vortex","Menuheader bg text colors: "+bgColor+" "+textColor);
				tv.setBackgroundColor(bgColor);
				tv.setTextColor(textColor);

	            // Do some initialization
	        } else {
	            view = convertView;
				tv = view.findViewById(R.id.list_content1);
	        }


	        tv.setText(str1);

	        return view;
	    }

	}
