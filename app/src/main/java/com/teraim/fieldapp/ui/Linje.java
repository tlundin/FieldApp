package com.teraim.fieldapp.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.utils.Tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Linje extends View {
	private static final String TAG = "Linje";


	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint pTag = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint iTag = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private final Paint tagText = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint bm_paint = new Paint(Paint.FILTER_BITMAP_FLAG);

	private static float h=700;
    private static float w;
    private final LineMarkerFactory lmFactory;
	private final MovingMarker user = new MovingMarker(w,h);
	private static final float TAG_W = 20;
    private static final float TAG_H = 10;
    private static final float Max_Dev_X = 50;
	private static final float LineLengthInMeters=200;
    private static final float areaWidthInMeters=2*Max_Dev_X;
    private final String mPole;
	private final Bitmap bm;

	private final Map <String,Map<String,LineMarker>> markers = new HashMap<>();


	public Linje(Context context, String pole) {
		super(context);
		mPole = pole;
		lmFactory = new LineMarkerFactory();
		//setImageResource(R.drawable.linje_bg);
		p.setColor(Color.WHITE);
		p.setStyle(Style.STROKE);
		p.setStrokeWidth(4);

		p1.setColor(Color.WHITE);
		p1.setStyle(Style.STROKE);
		p1.setStrokeWidth(3);

		p2.setColor(Color.WHITE);
		p2.setStyle(Style.STROKE);
		p2.setTextSize(15);
		p2.setTextAlign(Align.CENTER);
		
		pp.setColor(Color.BLUE);
		pp.setStyle(Style.STROKE);
		pp.setTextSize(15);
		pp.setStrokeWidth(1);
		pp.setTextAlign(Align.CENTER);

		
		pTag.setStyle(Style.FILL);
		pTag.setStrokeWidth(1);
		
		iTag.setStrokeWidth(1);
		iTag.setStyle(Style.STROKE);
//	    iTag.setPathEffect(new DashPathEffect(new float[]{5, 10, 15, 20}, 0));
		iTag.setColor(Color.WHITE);
		
		tagText.setColor(Color.WHITE);
		tagText.setStyle(Style.STROKE);
		tagText.setTextSkewX(-0.25f);
		tagText.setTextSize(12);
		tagText.setTextAlign(Align.LEFT);
		

		//bm_paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		bm = BitmapFactory.decodeResource(getResources(), R.drawable.linje_bg);
	}

	
	@Override
	protected void onDraw(Canvas canvas) {

		super.onDraw(canvas);

		//canvas.drawBitmap(bm, 0, 0, bm_paint);
		h = this.getHeight();
		w = this.getWidth();
        float r = w / 9;
		canvas.drawBitmap(bm, null, new RectF(0, 0, w, h), null);
		float lineLength = (h-2*(r +w/8));
        float lineEnd = r + w / 8;
        float lineStart = h - lineEnd;
		float pixelsPerMeter_Y = lineLength/LineLengthInMeters;
		float pixelsPerMeter_X = w/areaWidthInMeters;
        float lineX = w / 2;
		canvas.drawText(mPole, lineX, w/8- r -10, pp);
		canvas.drawCircle(lineX, w/8, r, p);
		canvas.drawText("SLUT", lineX, w/8, p2);
		canvas.drawCircle(lineX, h-w/8, r, p);
		canvas.drawText("START", lineX, h-w/8, p2);
		canvas.drawLine(lineX, lineEnd, lineX, lineStart, p1);
		
		if (!markers.isEmpty()) {
			Set<Entry<String, Map<String, LineMarker>>> markerE = markers.entrySet();
			for (Entry<String, Map<String, LineMarker>>e:markerE) {
				Map<String, LineMarker> f = e.getValue();
				Set<Entry<String, LineMarker>> g = f.entrySet();
				int shiftX = 0;
				for (Entry<String, LineMarker>h:g) {
					String tag = h.getKey();
					LineMarker lm = h.getValue();
					if (lm.isInterval()) {

						float startY = lineStart -lm.getStart()*pixelsPerMeter_Y;
						float stopY = lineStart -lm.getEnd()*pixelsPerMeter_Y;
						canvas.drawLine(lineX, startY, lineX +TAG_W, startY, iTag);
						canvas.drawLine(lineX +TAG_W/2, startY, lineX +TAG_W/2, stopY, iTag);
						canvas.drawLine(lineX, stopY, lineX +TAG_W, stopY, iTag);
						canvas.drawText(lm.tag, lineX +TAG_W+2, startY-(startY-stopY)/2, tagText);
					} else {
						pTag.setColor(lm.myColor);			
						float left = shiftX+ lineX -TAG_W/2;
						float right = shiftX+ lineX +TAG_W/2;
						float bottom = lineStart - lm.getStart()*pixelsPerMeter_Y+TAG_H/2;
						float top = bottom-TAG_H;
						canvas.drawRect(left, top,right,bottom,pTag);
						canvas.drawText(tag, right, bottom, tagText);
					}
				}
				
			}
		}
		
		if (user.isInsideScreen()) {
			float x = user.getPosX();
			float y = user.getPosY();
			pTag.setColor(Color.WHITE);
			
			if ( x<Max_Dev_X && 
				 x>-Max_Dev_X &&
				 y>0 && 
				 y<=LineLengthInMeters )
				
				pTag.setColor(Color.GREEN);
			else
				pTag.setColor(Color.RED);
			canvas.drawCircle(lineX +x*pixelsPerMeter_X, lineStart -y*pixelsPerMeter_Y, 10, pTag);
		}
	}
	
	
	
	
	public void addMarker(String start,String tag) {
		addMarker(start,null,tag);
	}
	
	public void removeMarker(String tag, String meters) {
		Log.e("vortex","Removing marker "+tag+" at "+meters+" meters");
		Map<String,LineMarker> lm = markers.get(meters);
		LineMarker a;
		if (lm!=null && !lm.isEmpty())
			if ((a=lm.get(tag))!=null)
				lm.remove(a);
	}
	
	public void addMarker(String start, String end, String tag) {
		Log.d(TAG,"Adding marker");
		if (!Tools.isNumeric(start)||(end!=null && !Tools.isNumeric(end)))
			return;
		Map<String,LineMarker> lm = markers.get(start);
		if (lm==null) {
			lm = new HashMap<>();
			markers.put(start, lm);
		}
		lm.put(tag, lmFactory.create(start,end,tag));	
		this.invalidate();
	}
	
	
	private class LineMarker {
		private final String start;
        private final String end;
		final String tag;
		 final int myColor;
		
		 LineMarker(String start,String end,String tag,int color) {
			this.start=start;
			this.end=end;
			this.tag=tag;
			this.myColor=color;
		}
		 boolean isInterval() {
			return end!=null;
		}
		
		float getStart() {
			return Float.parseFloat(start);
		}
		float getEnd() {
			return Float.parseFloat(end);
		}
	}
	private class LineMarkerFactory {
		
		
		
		final Integer[] colors = {Color.parseColor("#CC3232"),Color.BLUE,Color.GREEN,Color.RED,Color.YELLOW,Color.DKGRAY,Color.WHITE};
		int currentColor = 0;
		final Map <String, Integer> assigned = new HashMap<>();
		
		LineMarker create(String start, String end, String tag) {
			Integer c = assigned.get(tag);
			if (c==null) {
				c = colors[currentColor];
				assigned.put(tag, c);
				currentColor = (currentColor + 1)%colors.length;			
			} 
			
			return new LineMarker(start,end,tag,c);
		}
	}
	
	
	
	class MovingMarker {
		
		float x=-1,y=-1;
		final float w;
        final float h;
		
		 MovingMarker (float w, float h) {
			this.w=w;
			this.h=h;
		}
		
		//x and y are relative to the Line that is in the center of the view. 
		 void setPos(float x,float y) {
			this.x=x;
			this.y=y;
		}
		
		private boolean isInsideScreen() {
			return x>=-(w/2)&&x<=(w/2)&&y>=0&&y<=h;
			
		}
		
		private float getPosX() {
			return x;
		}
		
		private float getPosY() {
			return y;
		}
		
	}

	public void setUserPos(float distX, float distY) {
		user.setPos(distX, distY);
	}


	public void removeAllMarkers() {
		markers.clear();
	}


	public void init(double eastStart, double northStart, boolean isHorizontal, boolean isUpOrLeft) {
		
	}
	
}
