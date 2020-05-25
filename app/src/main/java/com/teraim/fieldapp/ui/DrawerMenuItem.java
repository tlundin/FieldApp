package com.teraim.fieldapp.ui;

import android.view.LayoutInflater;
import android.view.View;

interface DrawerMenuItem {
    int getViewType();
    View getView(LayoutInflater inflater, View convertView);
}
