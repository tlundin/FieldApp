//package com.teraim.fieldapp.log;
//
//import android.content.Context;
//import android.content.Intent;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import android.text.Spannable;
//import android.text.SpannableString;
//import android.text.SpannableStringBuilder;
//import android.text.style.TextAppearanceSpan;
//import android.util.Log;
//import android.widget.TextView;
//
//import com.teraim.fieldapp.R;
//import com.teraim.fieldapp.ui.MenuActivity;
//
//public class CriticalOnlyLogger implements LoggerI {
//	private boolean hasRed=false;
//	private final SpannableStringBuilder myTxt = new SpannableStringBuilder();
//	private SpannableString s;
//	private TextView log = null;
//	private final Context myContext;
//
//	public CriticalOnlyLogger(Context myContext) {
//		this.myContext = myContext;
//	}
//
//	@Override
//	public void setOutputView(TextView txt) {
//		log = txt;
//	}
//
//	@Override
//	public void addRow(String text) {
//
//	}
//
//	@Override
//	public void addRedText(String text) {
//		Log.d("CRIT", text);
//		if (!hasRed) {
//			hasRed = true;
//			// ... (your other logic)
//			LocalBroadcastManager.getInstance(myContext).sendBroadcast(new Intent(MenuActivity.REDRAW));
//		}
//
//		if (text != null && text.length() > 0) {
//			// Assume myTxt is a SpannableStringBuilder, which is necessary for modification.
//			// If it's not, you should initialize it as:
//			// SpannableStringBuilder myTxt = new SpannableStringBuilder();
//
//			// 1. Get the starting position, which is the current end of the text.
//			int start = myTxt.length();
//
//			// 2. Append the new text (as a plain string).
//			String stringToAppend = "\n" + text;
//			myTxt.append(stringToAppend);
//
//			// 3. Get the new end position.
//			int end = myTxt.length();
//
//			// 4. Apply the span to the newly appended text section within myTxt.
//			myTxt.setSpan(new TextAppearanceSpan(myContext, R.style.RedStyle), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//			// 5. Update your TextView.
//			if (log != null) {
//				log.setText(myTxt);
//			}
//		}
//	}
//
//	@Override
//	public void addGreenText(String text) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void addYellowText(String text) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void addText(String text) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void addCriticalText(String text) {
//		s = new SpannableString("\n"+text);
//		myTxt.append(s);
//	}
//
//	@Override
//	public CharSequence getLogText() {
//		return myTxt;
//	}
//
//	@Override
//	public void draw() {
//		if (log!=null) {
//			log.setText(myTxt);
//		}
//		else
//			Log.e("nils","LOG WAS NULL IN DRAW!!");
//	}
//
//	@Override
//	public void clear() {
//		myTxt.clear();
//		if (log!=null) log.setText(myTxt);
//	}
//
//	@Override
//	public void addPurpleText(String string) {
//		// TODO Auto-generated method stub
//
//	}
//
//
//	private void removeTicky() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void removeLine() {
//		// TODO Auto-generated method stub
//
//	}
//
//
//	@Override
//	public boolean hasRed() {
//		//Log.d("vortex","calling hasred on "+this.toString());
//		if (hasRed) {
//			Log.d("vortex","hasred!");
//			hasRed=false;
//			return true;
//		}
//		return false;
//	}
//
//}
