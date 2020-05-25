package com.teraim.fieldapp.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teraim.fieldapp.R;

public class BackgroundFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_background,
                container, false);
	}

}


