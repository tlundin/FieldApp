package com.teraim.fieldapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.dynamic.types.Marker;
import com.teraim.fieldapp.dynamic.types.MovingMarker;
import com.teraim.fieldapp.non_generics.DelyteManager.Coord;

import java.util.List;

/**
 * @author Terje
 * 
 * This class is used to draw a Provyta with all its parts (delytor)
 */
public class FixytaView extends View {




	private final Paint px = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint p20 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    private final MovingMarker user = null;
	private List<Marker> fixpunkter;
	private String msg = "";

	//private Path tag = new Path();

	public void setFixedMarkers (List<Marker> fixpunkter) {
		this.fixpunkter=fixpunkter;
	}

	public FixytaView(Context context, AttributeSet attrs) {
		super(context,attrs);


		px.setColor(Color.DKGRAY);
		px.setTypeface(Typeface.SANS_SERIF);


		pl.setColor(Color.BLACK);
		pl.setStyle(Style.STROKE);
		pl.setTypeface(Typeface.DEFAULT_BOLD); 
		pl.setTextAlign(Align.CENTER);
		pl.setTextSize(25);


		p.setColor(Color.BLACK);
		p.setStyle(Style.STROKE);
		p.setTextSize(20);

		p20.setColor(Color.BLUE);
		p20.setStrokeWidth(2);
		p20.setStyle(Style.STROKE);		
		p20.setTypeface(Typeface.SANS_SERIF);


        Paint p50 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p50.setColor(Color.RED);
		p50.setStrokeWidth(3);
		p50.setStyle(Style.STROKE);
		p50.setTypeface(Typeface.SANS_SERIF); 



	}


    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);			
		int w = getWidth();
		int h = getHeight();
		double r;
		int cy;
		int cx;
		r=(w>=h)?((h/2)-h*.1):((w/2)-w*.1);
		cx = w/2;
		cy = h/2;
        double realRadiusinDeciMeter = 200;
        double oScaleF = r / realRadiusinDeciMeter;
		//A dot in the middle!
		canvas.drawPoint(cx, cy, p);

//		canvas.drawCircle(cx, cy,(int)r, p50);
//		canvas.drawCircle(cx, cy,(float)(200f*oScaleF), p20);
		canvas.drawCircle(cx, cy,(float)(int)r, p20);
		canvas.drawCircle(cx, cy,(float)(100f* oScaleF), p);
        canvas.drawText("200",(int)(cx+r)-20f, cy, p);
//		canvas.drawText("200",(int)(cx+(200f*oScaleF))-20f, cy, p);
		canvas.drawText("100",(int)(cx+(100f* oScaleF))-20f, cy, p);
		canvas.drawText("N",cx,(float)(h*.1), pl);

		for(Marker focusMarker:fixpunkter) {
			if (focusMarker.hasPosition()) {								
				if(focusMarker.getDistance()<= realRadiusinDeciMeter) {
					float ux = (float) (cx+focusMarker.x * oScaleF);
					float uy = (float) (cy+focusMarker.y * oScaleF);
					ux = ux - Marker.Pic_H/2;
					uy = uy - Marker.Pic_H/2; 
					Log.d("nils","gets here... "+focusMarker.dist+" iscenter: "+focusMarker.isCentre());
					
					//if (focusMarker.isCentre()) 
					canvas.drawText("A: "+focusMarker.getDistance()+" R: "+focusMarker.riktning, ux, uy, p);
	
					canvas.drawBitmap(focusMarker.bmp, ux, uy, null);
					//canvas.drawText("A: "+focusMarker.getDistance()+" R: "+focusMarker.riktning, ux, uy, p);
//					canvas.restore();
					
				} else {
					//Log.d("NILS","Blue is outside radius");
					//Given that blue is outside current Max Radius, draw an arrow to indicate where..					
					Coord t = new Coord((int) realRadiusinDeciMeter,focusMarker.riktning);
					float x = t.x;
					float y =  t.y;
					float zx = (float) (cx+x * oScaleF);
					float zy = (float) (cy+y * oScaleF);
                    float uy = zy - Marker.Pic_H/2;
					//canvas.save();
					//canvas.rotate((float)(180+(180*t.rikt*0.0174532925/Math.PI)), x, y);
					canvas.drawBitmap(focusMarker.bmp, zx, uy, null);
					canvas.drawText("A: "+focusMarker.getDistance()+" R: "+focusMarker.riktning, zx, zy, p);
					//canvas.restore();
					//TODO:
					//If last value closer than this value, draw arrow pointing towards middle
				} 
			}
		}

		//Msg in top.
		if (msg!=null && msg.length()>0)
			canvas.drawText(msg, cx-msg.length()*3, (float)(h*.1+30), px);

		//update other fixpoints.


	}


	public void showDistance(int dist) {
		msg = "Avst: "+String.valueOf(dist)+"m";
	}

	




	public void showUser(String deviceColor, int wx, int wy,int dist) {

		user.set(wx,wy,dist);

	}




	public void showWaiting() {
		msg = "Väntar på GPS";
	}



	



}


