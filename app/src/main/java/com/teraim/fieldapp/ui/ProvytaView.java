package com.teraim.fieldapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.dynamic.types.Delyta;
import com.teraim.fieldapp.dynamic.types.Marker;
import com.teraim.fieldapp.dynamic.types.Point;
import com.teraim.fieldapp.dynamic.types.Segment;
import com.teraim.fieldapp.non_generics.DelyteManager.Coord;

import java.util.List;

/**
 * @author Terje
 * 
 * This class is used to draw a Provyta with all its parts (delytor)
 */
public class ProvytaView extends View {



	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private final Paint px = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    private final Paint pSma = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pSmaSelected = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Marker focusMarker;
	private String msg = "";
	private List<Delyta> delytor;

	private boolean abo=false;


	public ProvytaView(Context context, AttributeSet attrs, boolean isNineHoler) {
		super(context,attrs);

		abo = isNineHoler;
		pf.setColor(Color.BLACK);
		pf.setStyle(Style.STROKE);
		pf.setStrokeWidth(1);


        Paint pySelected = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        pySelected.setColor(Color.BLACK);
		pySelected.setStyle(Style.STROKE);
		pySelected.setStrokeWidth(3);


		p.setColor(Color.BLACK);
		p.setStyle(Style.STROKE);
		p.setStrokeWidth(2);

		px.setColor(Color.DKGRAY);
		px.setTypeface(Typeface.SANS_SERIF);


		pl.setColor(Color.BLACK);
		pl.setStyle(Style.STROKE);
		pl.setTypeface(Typeface.DEFAULT_BOLD); 
		pl.setTextSize(15);

        Paint p50 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p50.setColor(Color.BLUE);
		p50.setStrokeWidth(2);
		p50.setStyle(Style.STROKE);

		p50.setTypeface(Typeface.SANS_SERIF);


        Paint p100 = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p100.setColor(Color.BLACK);
		p100.setStrokeWidth(3);
		p100.setStyle(Style.STROKE);
		p100.setTypeface(Typeface.SANS_SERIF); 

		pSma.setColor(Color.GREEN);
		pSma.setStyle(Style.FILL);

		//user = new MovingMarker(BitmapFactory.decodeResource(context.getResources(),
		//		R.drawable.gps_pil));
		//this.focusMarker=focusMarker;

	}




	final float innerRealRadiusInMeter = 10;
	final float midRealRadiusInMeter = 50;
    float rScaleF=0;

    public void setIndicatorLocation(int deg) {
        Log.d("nils","got "+deg);
	}


	private float r;

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);			
		float w = getWidth();
		float h = getHeight();

		boolean largeSize = (h>500);
		float margY = largeSize?20:0;
		float cy;
		float cx;
		if (largeSize) {
			r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
			px.setTextSize(25);		
		}
		else {
			r=h/2-15;
			px.setTextSize(13);
		}
		cx = w/2;
		cy = (h/2+margY);//h/2;
		//tag.lineTo(w-50,0);
		//Log.d("NILS","w h r"+w+" "+h+" "+r);
        float realRadiusinMeter = 100;
        float oScaleF = r / realRadiusinMeter;
		//A dot in the middle!
		canvas.drawPoint(cx, cy, p);

		//rita småprovytorna.
		//28 cm i diameter

		float sr = 2.8f* oScaleF;
		Coord c=null;
		//6 meter ut på 0,120,240
		int sDists[] = new int[]{30,50,70};
		for (int i = 0; i<(abo?3:1);i++) {
			int sDist = sDists[i];
			c = calc_xy((int)(sDist* oScaleF),0);
			canvas.drawCircle(cx,cy+c.y,sr,pSma);
			canvas.drawCircle(cx,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist* oScaleF),120);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist* oScaleF),240);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
		}
/*
		if (focusMarker.getDistance()>midRealRadiusInMeter) {
			canvas.drawCircle(cx, cy,(int)r, p100);
			canvas.drawCircle(cx, cy,(float)(50.0*oScaleF), p50);
			canvas.drawCircle(cx, cy,(float)(10.0*oScaleF), px);
			rScaleF = oScaleF;
			canvas.drawText("100",(int)(cx+r)-25, cy, p100);
			canvas.drawText("50",(int)(cx+(50.0*oScaleF))-20, cy, p50);
			canvas.drawText("10",(int)(cx+(10.0*oScaleF))-15, cy, pf);
		} else 
			if (focusMarker.getDistance()>innerRealRadiusInMeter) {
				rScaleF = r/midRealRadiusInMeter;
				canvas.drawCircle(cx, cy,(int)r, p50);
				canvas.drawCircle(cx, cy,(float)(10.0*rScaleF), p);		
				canvas.drawText("50",(int)(cx+r)-25, cy, p50);
				canvas.drawText("10",(int)(cx+(10.0*rScaleF))-15, cy, pf);

			} else {
				//canvas.drawCircle(cx, cy,(int)r, p);
				rScaleF = r/innerRealRadiusInMeter;
				canvas.drawText("10",(int)(cx+r)-15, cy, pf);
				if (delytor != null && delytor.size()>0)
					this.drawDelytor(canvas,cx,cy,(float)oScaleF);
			}		
*/
		
		drawDelytor(canvas,cx,cy, oScaleF);
		canvas.drawText("10",(int)(cx+r)-15, cy, pl);
		if (largeSize)
			canvas.drawText("N",cx,(cy-r)-25, pl);


		//canvas.drawLine(0, 0, w, h, p);
		// canvas.drawPath(tag,p);
		//mDrawable.draw(canvas);
		//canvas.drawPath(tag, p);
		/*
		if (focusMarker.hasPosition()) {
			double alfa;
			//Log.d("NILS","Blue has position");
			if(focusMarker.getDistance()<realRadiusinMeter) {
				alfa = 0;//focusMarker.getMovementDirection();
				float degAlfa = (float)(180*alfa/Math.PI);
				int ux = (int) (cx-focusMarker.x*rScaleF);
				//icon is a rotating arrow. Get it to point exavtly on x,y.
				int iconx = (int)(Marker.Pic_H/2+Marker.Pic_H *  Math.sin(alfa));
				ux = ux - iconx;
				//inverted north/south
				int uy = (int) (cy+focusMarker.y*rScaleF);
				int icony = (int)(MovingMarker.Pic_H/2+MovingMarker.Pic_H *  Math.cos(alfa));
				uy = uy + icony;
				//Log.d("NILS","iconx icony "+iconx+" "+icony);
				canvas.save();
				canvas.rotate(180+degAlfa, ux, uy);
				canvas.drawBitmap(focusMarker.bmp, ux, uy, null);
				canvas.restore();
				msg = "X: "+ux+" Y: "+uy+" icX: "+(-iconx)+" icY: "+icony;
			} else {
				//Log.d("NILS","Blue is outside radius");
				//Given that blue is outside current Max Radius, draw an arrow to indicate where..
				alfa = Geomatte.getRikt2(focusMarker.y, focusMarker.x,0,0);
				float x = (float)(cx + r * Math.sin(alfa));
				float y =  (float)(cy - r * Math.cos(alfa));
				canvas.save();
				canvas.rotate((float)(180+(180*alfa/Math.PI)), x, y);
				canvas.drawBitmap(focusMarker.bmp, x, y, null);
				canvas.restore();
				//TODO:
				//If last value closer than this value, draw arrow pointing towards middle
			} 
		}

		//Msg in top.
		if (msg!=null && msg.length()>0)
			canvas.drawText(msg, cx-msg.length()*3, (float)(h*.1+30), px);
		 
		//update other fixpoints.
		if (scords!=null) {
			for (int i=0;i<scords.size()-1;i++) {
				canvas.drawLine(cx+scords.get(i).x, cy+scords.get(i).y, cx+scords.get(i+1).x, 
						cy+scords.get(i+1).y, p100);
			}
		}
		 */
	}


    private boolean isSelected=false;


	public void setSpecial(List<Coord> cords) {
    }

	public void showDistance(int dist) {
		msg = "Avst: "+String.valueOf(dist)+"m";
	}





	private void drawDelytor(Canvas c, float cx, float cy, float oScaleF) {

		for (Delyta d:delytor) {
			if (!d.isSelected())
				drawDelyta(d,c,cx,cy,oScaleF);
		}
		for (Delyta d:delytor) {
			if (d.isSelected())
				drawDelyta(d,c,cx,cy,oScaleF);
		}
	}

	private void drawDelyta(Delyta d,Canvas c, float cx, float cy, float oScaleF) {
		float startX,startY,endX,endY;
		for (Segment s:d.getSegments()) {	
			if (s.isArc) {
//				if (!d.isSelected())
//					continue;	
//				else {
					RectF oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);	
					Log.d("nils","start - end"+s.start.rikt+"-"+s.end.rikt);
					float start = s.start.rikt;
					float end = Delyta.rDist((int)start,s.end.rikt);
					Log.d("nils","Drawing arc from "+start+" with sweep "+end);
					start = start-90;
					if (start<0)
						start +=360;
					
					p.setColor(d.isSelected()?Color.BLACK:isSelected?Color.LTGRAY:d.getColor());
					c.drawArc(oval, start, end, false, p);
//					c.drawArc(oval, start, end, false, pySelected);

//				}
			} else {
				//float mirror = margY+r*2;							
				startX = cx+(s.start.x*oScaleF);
				startY = cy+(s.start.y*oScaleF);
				endX = cx+(s.end.x*oScaleF);
				endY = cy+(s.end.y*oScaleF);				
				Log.d("nils","Drawing Start: "+startX+","+startY+" End: "+endX+","+endY);
				p.setColor(d.isSelected()?Color.BLACK:isSelected?Color.LTGRAY:d.getColor());
//				c.drawLine(startX,startY,endX,endY, d.isSelected()?pySelected:p);	
				c.drawLine(startX,startY,endX,endY, p);
			}

		}
		Point numPos = d.getNumberPos();
		if (numPos!=null) {
			float nx = cx+(numPos.x*oScaleF);
			float ny = cy+(numPos.y*oScaleF);
			Log.d("nils","Drawing number at: "+nx+","+ny);
			px.setColor(d.isSelected()?Color.BLACK:isSelected?Color.LTGRAY:d.getColor());
			c.drawText(d.getId()+"["+Math.round(d.getArea()/100)+"]", nx, ny, pl);
		}	}

	public void showDelytor(List<Delyta> delytor, boolean isSelected) {
		
		if (delytor==null) {
			removeDelytor();
			return;
		}
		this.delytor = delytor;
		this.isSelected = isSelected;
		/*
		for (Delyta d:delytor) 
			if (d.getId()==2)
				setSpecial(d.getSpecial());	
		 */	
		this.invalidate();
	}

	private void removeDelytor() {
		showDelytor(null,false);
	}


	public void showUser(String deviceColor, Location arg0, double alfa,
			double dist) {
		// TODO Auto-generated method stub

	}




	public void showUser(String deviceColor, int wx, int wy,int dist) {

		focusMarker.set(wx,wy,dist);

	}




	public void showWaiting() {
		msg = "Väntar på GPS";
	}



	private Coord calc_xy(int avst, int rikt) {
		return new Coord(avst,rikt);
	}





}


