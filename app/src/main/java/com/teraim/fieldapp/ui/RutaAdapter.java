package com.teraim.fieldapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.RutListEntry;

import java.util.List;

class RutaAdapter extends ArrayAdapter<RutListEntry> {

	
	public RutaAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	public RutaAdapter(Context context, int resource, List<RutListEntry> rutor) {
		super(context, resource, rutor);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;

		if (v == null) {
			LayoutInflater vi;
			vi = LayoutInflater.from(getContext());
			v = vi.inflate(R.layout.ruta_list_row, null);

		}
		RutListEntry rl = getItem(position);
		final String pi = Integer.toString(rl.id);
		TextView geo = v.findViewById(R.id.geo);
		TextView header = v.findViewById(R.id.header);
		header.setText(pi);
		
		geo.setText(rl.currentDistance);
		
		return v;

	}
	
	
}