/*
 * Copyright (c) 2012 Jason Polites
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teraim.fieldapp.gis;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GestureImageView extends androidx.appcompat.widget.AppCompatImageView  {
	private static final String TAG = "GestureImageView";




	private static final String GLOBAL_NS = "http://schemas.android.com/apk/res/android";
	private static final String LOCAL_NS = "http://schemas.polites.com/android";

	private final Semaphore drawLock = new Semaphore(0);
	private Animator animator;

	private Drawable drawable;

	float x = 0;
	float y = 0;

	private boolean layout = false;

	float scaleAdjust = 1.0f;
	private float startingScale = -1.0f;

	final float scale = 1.0f;
	private float maxScale = 5.0f;
	private float minScale = 0.75f;
	private float fitScaleHorizontal = 1.0f;
	private float fitScaleVertical = 1.0f;
	private float rotation = 0.0f;

	private float centerX;
	private float centerY;
	
	private Float startX, startY;
	float polyVertexX;
    float polyVertexY;

    private Integer resId = null;
	private boolean recycle = false;
	private boolean strict = false;

	int displayHeight;
	int displayWidth;

	private int alpha = 255;
	private ColorFilter colorFilter;

	private int deviceOrientation = -1;
	private int imageOrientation;

	private GestureImageViewListener gestureImageViewListener;
	private GestureImageViewTouchListener gestureImageViewTouchListener;
	
	private OnTouchListener customOnTouchListener;
	private OnClickListener onClickListener;
	private OnLongClickListener onLongClickListener;
	public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs);
	}

	public GestureImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		String scaleType = attrs.getAttributeValue(GLOBAL_NS, "scaleType");
		
		if(scaleType == null || scaleType.trim().length() == 0) {
			setScaleType(ScaleType.CENTER_INSIDE);
		}
		
		String strStartX = attrs.getAttributeValue(LOCAL_NS, "start-x");
		String strStartY = attrs.getAttributeValue(LOCAL_NS, "start-y");
		
		if(strStartX != null && strStartX.trim().length() > 0) {
			startX = Float.parseFloat(strStartX);
		}
		
		if(strStartY != null && strStartY.trim().length() > 0) {
			startY = Float.parseFloat(strStartY);
		}
		
		setStartingScale(attrs.getAttributeFloatValue(LOCAL_NS, "start-scale", startingScale));
		setMinScale(attrs.getAttributeFloatValue(LOCAL_NS, "min-scale", minScale));
		setMaxScale(attrs.getAttributeFloatValue(LOCAL_NS, "max-scale", maxScale));
		setStrict(attrs.getAttributeBooleanValue(LOCAL_NS, "strict", strict));
		setRecycle(attrs.getAttributeBooleanValue(LOCAL_NS, "recycle", recycle));
		
		initImage();
	}

	public void setPolyVertex(float polyX,float polyY) {
		polyVertexX=polyX;
		polyVertexY=polyY;
	}
	public GestureImageView(Context context) {
		super(context);
		setScaleType(ScaleType.CENTER_INSIDE);
		initImage();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		if(drawable != null) {
			int orientation = getResources().getConfiguration().orientation;
			if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
				displayHeight = MeasureSpec.getSize(heightMeasureSpec);

				if(getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
					float ratio = (float) getImageWidth() / (float) getImageHeight();
					displayWidth = Math.round( (float) displayHeight * ratio) ;
				}
				else {
					displayWidth = MeasureSpec.getSize(widthMeasureSpec);
				}
			}
			else {
				displayWidth = MeasureSpec.getSize(widthMeasureSpec);
				if(getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
					float ratio = (float) getImageHeight() / (float) getImageWidth();
					displayHeight = Math.round( (float) displayWidth * ratio) ;
				}
				else {
					displayHeight = MeasureSpec.getSize(heightMeasureSpec);
				}				
			}
		}
		else {
			displayHeight = MeasureSpec.getSize(heightMeasureSpec);
			displayWidth = MeasureSpec.getSize(widthMeasureSpec);
		}

		setMeasuredDimension(displayWidth, displayHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if(changed || !layout) {
			setupCanvas(displayWidth, displayHeight, getResources().getConfiguration().orientation);
		}
	}

	private void setupCanvas(int measuredWidth, int measuredHeight, int orientation) {
		Log.d(TAG,"layout is called now");
		if(deviceOrientation != orientation) {
			layout = false;
			deviceOrientation = orientation;
		}

		if(drawable != null && !layout) {
			int imageWidth = getImageWidth();
			int imageHeight = getImageHeight();

            int hWidth = Math.round(((float) imageWidth / 2.0f));
            int hHeight = Math.round(((float) imageHeight / 2.0f));
			
			measuredWidth -= (getPaddingLeft() + getPaddingRight());
			measuredHeight -= (getPaddingTop() + getPaddingBottom());
			
			computeCropScale(imageWidth, imageHeight, measuredWidth, measuredHeight);
			
			if(startingScale <= 0.0f) {
				computeStartingScale(imageWidth, imageHeight, measuredWidth, measuredHeight);
			}

			scaleAdjust = startingScale;

			this.centerX = (float) measuredWidth / 2.0f;
			this.centerY = (float) measuredHeight / 2.0f;
			
			if(startX == null) {
				x = centerX;
			}
			else {
				x = startX;
			}

			if(startY == null) {
				y = centerY;
			}
			else {
				y = startY;
			}	

			gestureImageViewTouchListener = new GestureImageViewTouchListener(this, measuredWidth, measuredHeight);
			
			if(isLandscape()) {
				gestureImageViewTouchListener.setMinScale(minScale * fitScaleHorizontal);
			}
			else {
				gestureImageViewTouchListener.setMinScale(minScale * fitScaleVertical);
			}
			
			
			gestureImageViewTouchListener.setMaxScale(maxScale * startingScale);
			
			gestureImageViewTouchListener.setFitScaleHorizontal(fitScaleHorizontal);
			gestureImageViewTouchListener.setFitScaleVertical(fitScaleVertical);
			gestureImageViewTouchListener.setCanvasWidth(measuredWidth);
			gestureImageViewTouchListener.setCanvasHeight(measuredHeight);
			gestureImageViewTouchListener.setOnClickListener(onClickListener);
			gestureImageViewTouchListener.setOnLongClickListener(onLongClickListener);
			

			drawable.setBounds(-hWidth,-hHeight, hWidth, hHeight);

			super.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(customOnTouchListener != null) {
						customOnTouchListener.onTouch(v, event);
					}
					return gestureImageViewTouchListener.onTouch(v, event);
				}
			});	

			layout = true;
		}
	}
	
	private void computeCropScale(int imageWidth, int imageHeight, int measuredWidth, int measuredHeight) {
		fitScaleHorizontal = (float) measuredWidth / (float) imageWidth;
		fitScaleVertical = (float) measuredHeight / (float) imageHeight;
	}
	
	private void computeStartingScale(int imageWidth, int imageHeight, int measuredWidth, int measuredHeight) {
		switch(getScaleType()) {
			case CENTER:
				// Center the image in the view, but perform no scaling.
				startingScale = 1.0f;
				break;
				
			case CENTER_CROP:
				// Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions
				// (width and height) of the image will be equal to or larger than the corresponding dimension of the view (minus padding).
				startingScale = Math.max((float) measuredHeight / (float) imageHeight, (float) measuredWidth/ (float) imageWidth);
				break;
				
			case CENTER_INSIDE:

				// Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions
				// (width and height) of the image will be equal to or less than the corresponding dimension of the view (minus padding).
				float wRatio = (float) imageWidth / (float) measuredWidth;
				float hRatio = (float) imageHeight / (float) measuredHeight;

				if(wRatio > hRatio) {
					startingScale = fitScaleHorizontal;
				}
				else {
					startingScale = fitScaleVertical;
				}

				break;
		}
	}

	private boolean isRecycled() {
		if(drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
			if(bitmap != null) {
				return !bitmap.isRecycled();
			}
		}
		return true;
	}

	private void recycle() {
		if(recycle && drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
			if(bitmap != null) {
				Log.d(TAG,"recycling!");
				bitmap.recycle();
			}
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(layout) {
			if(drawable != null && isRecycled()) {
				canvas.save();
				
				float adjustedScale = scale * scaleAdjust;

				canvas.translate(x, y);

				if(rotation != 0.0f) {
					canvas.rotate(rotation);
				}

				if(adjustedScale != 1.0f) {
					canvas.scale(adjustedScale, adjustedScale);
				}

				drawable.draw(canvas);
				canvas.restore();
			} //else
			//	Log.d(TAG, "nodraw since drawable is null");

			if(drawLock.availablePermits() <= 0) {
				drawLock.release();
			}
		} else
			Log.e("vortex","no layout no draw!");
	}

	/**
	 * Waits for a draw
	 * @param timeout time to wait for draw (ms)
	 * @throws InterruptedException
	 */
	public boolean waitForDraw(long timeout) throws InterruptedException {
		return drawLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void onAttachedToWindow() {
		animator = new Animator(this, "GestureImageViewAnimator");
		animator.start();

		if(resId !=null && drawable == null) {
			setImageResource(resId);
		}

		super.onAttachedToWindow();
	}

	public void animationStart(Animation animation) {
		if(animator != null) {
			animator.play(animation);
		}
	}

	public void animationStop() {
		if(animator != null) {
			animator.cancel();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if(animator != null) {
			animator.finish();
		}
		if(recycle && drawable != null && isRecycled()) {
			recycle();
			drawable = null;
		}
		super.onDetachedFromWindow();
	}

	private void initImage() {
		if(this.drawable != null) {
			this.drawable.setAlpha(alpha);
			this.drawable.setFilterBitmap(true);
			if(colorFilter != null) {
				this.drawable.setColorFilter(colorFilter);
			}
		} else
			Log.e("vortex","drawable was null in initImage!!");
		
		if(!layout) {
			Log.e("vortex","layout!!!");
			requestLayout();
			redraw();
		}
	}


	public void setImageBitmap(Bitmap image) {
		if (image==null)
			Log.d(TAG,"Image null!");
		if(this.drawable != null) {
			this.recycle();
			Log.d(TAG,"REcycle!");
		}
		this.drawable = new BitmapDrawable(getResources(), image);
		if (drawable!=null) {
			Log.d(TAG, "Drawable not null!");
			layout=false;
		}
		else
			Log.d(TAG,"Drawable null!");
		initImage();
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		this.drawable = drawable;
		initImage();
	}

	public void setImageResource(int id) {
		if(this.drawable != null) {
			this.recycle();
		}
		this.resId = id;
		setImageDrawable(getContext().getResources().getDrawable(id,getContext().getTheme()));

	}

	int getScaledWidth() {
		return Math.round(getImageWidth() * getScale());
	}
	
	int getScaledHeight() {
		return Math.round(getImageHeight() * getScale());
	}

	public int getImageWidth() {
		if(drawable != null) {
			return drawable.getIntrinsicWidth();
		}
		return bWidth;
	}

	public int getImageHeight() {
		if(drawable != null) {
			return drawable.getIntrinsicHeight();
		}
		return bHeight;
	}

	private int bWidth=0;
    private int bHeight=0;
	void setStaticMeasure(int w, int h) {
		bWidth=w;
		bHeight=h;
	}

	public void moveBy(float x, float y) {
		this.x += x;
		this.y += y;
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void redraw() {
		postInvalidate();
	}

	private void setMinScale(float min) {
		this.minScale = min;
		if(gestureImageViewTouchListener != null) {
			gestureImageViewTouchListener.setMinScale(min * fitScaleHorizontal);
		}
	}

	private void setMaxScale(float max) {
		this.maxScale = max;
		if(gestureImageViewTouchListener != null) {
			gestureImageViewTouchListener.setMaxScale(max * startingScale);
		}
	}

	public void setScale(float scale) {
		
			scaleAdjust = scale;
		
	}

	public void handleScale(float scaleDiff) {
		
		//float newScale = scaleAdjust+scaleDiff;
		//Log.d(TAG,"new scale: "+newScale);
		
		gestureImageViewTouchListener.startZoom(centerX, centerY, scaleDiff);
		//oldScale = scaleAdjust; 
		//float newX = (ratio * xDiff) + startX;
		//float newY = (ratio * yDiff) + startY;
		//gestureImageViewTouchListener.handleScale(newScale,newX, newY);
	}
	
	/**
	 * 
	 * @param scaleDiff
	 * @return true if max scaleout reached.
	 */

	public boolean handleScaleOut(float scaleDiff) {
		Log.d(TAG,"ScaleAdjust: "+scaleAdjust+" fitScaleHori: "+fitScaleHorizontal+" minSc: "+gestureImageViewTouchListener.getMinScale());
		gestureImageViewTouchListener.startZoom(centerX, centerY, scaleDiff);
		boolean maxReached = (Math.abs(scaleAdjust - fitScaleHorizontal) < 0.1);
		Log.d(TAG,"ScaleAdjust: "+scaleAdjust+" fitScaleHori: "+fitScaleHorizontal+" minSc: "+gestureImageViewTouchListener.getMinScale());
		Log.d(TAG,"maxReached: "+maxReached);
		return (maxReached) ;		
	}
	
	public float getScale() {
		Log.d(TAG,"getScale returns "+scaleAdjust);
		return scaleAdjust;
	}

	public float getImageX() {
		return x;
	}

	public float getImageY() {
		return y;
	}

	public boolean isStrict() {
		return strict;
	}

	private void setStrict(boolean strict) {
		this.strict = strict;
	}

	public boolean isRecycle() {
		return recycle;
	}

	private void setRecycle(boolean recycle) {
		this.recycle = recycle;
	}

	public void reset() {
		x = centerX;
		y = centerY;
		scaleAdjust = startingScale;
		if (gestureImageViewTouchListener != null) {
		    gestureImageViewTouchListener.reset();
		}
		redraw();
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public void setGestureImageViewListener(GestureImageViewListener pinchImageViewListener) {
		this.gestureImageViewListener = pinchImageViewListener;
	}

	public GestureImageViewListener getGestureImageViewListener() {
		return gestureImageViewListener;
	}

	@Override
	public Drawable getDrawable() {
		return drawable;
	}

	@Override
	public void setImageAlpha(int alpha) {
		this.alpha = alpha;
		if(drawable != null) {
			drawable.setAlpha(alpha);
		}
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		this.colorFilter = cf;
		if(drawable != null) {
			drawable.setColorFilter(cf);
		}
	}

	@Override
	public void setImageURI(Uri mUri) {
		if ("content".equals(mUri.getScheme())) {
			try {
				String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
				
				Cursor cur = getContext().getContentResolver().query(mUri, orientationColumn, null, null, null);
				
				if (cur != null && cur.moveToFirst()) {
					imageOrientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
				}  
				
				InputStream in = null;
				
				try {
					in = getContext().getContentResolver().openInputStream(mUri);
					Bitmap bmp = BitmapFactory.decodeStream(in);
					
					if(imageOrientation != 0) {
						Matrix m = new Matrix();
						m.postRotate(imageOrientation);
						Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
						bmp.recycle();
						setImageDrawable(new BitmapDrawable(getResources(), rotated));
					}
					else {
						setImageDrawable(new BitmapDrawable(getResources(), bmp));
					}
				}
				finally {
					if(in != null) {
						in.close();
					}
					
					if(cur != null) {
						cur.close();
					}
				}
			}
			catch (Exception e) {
				Log.w("GestureImageView", "Unable to open content: " + mUri, e);
			}
		}
		else {
			setImageDrawable(Drawable.createFromPath(mUri.toString()));
		}

		if (drawable == null) {
			Log.e("GestureImageView", "resolveUri failed on bad bitmap uri: " + mUri);
			// Don't try again.
			mUri = null;
		}
	}

	@Override
	public Matrix getImageMatrix() {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}		
		return super.getImageMatrix();
	}

	@Override
	public void setScaleType(ScaleType scaleType) {
		if(scaleType == ScaleType.CENTER ||
			scaleType == ScaleType.CENTER_CROP ||
			scaleType == ScaleType.CENTER_INSIDE) {
			
			super.setScaleType(scaleType);
		}
		else if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	@Override
	public void invalidateDrawable(Drawable dr) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
		super.invalidateDrawable(dr);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
		return super.onCreateDrawableState(extraSpace);
	}

	@Override
	public void setAdjustViewBounds(boolean adjustViewBounds) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
		super.setAdjustViewBounds(adjustViewBounds);
	}

	@Override
	public void setImageLevel(int level) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
		super.setImageLevel(level);
	}

	@Override
	public void setImageMatrix(Matrix matrix) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	@Override
	public void setImageState(int[] state, boolean merge) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	@Override
	public void setSelected(boolean selected) {
		if(strict) {
			throw new UnsupportedOperationException("Not supported");
		}
		super.setSelected(selected);
	}

	@Override
	public void setOnTouchListener(OnTouchListener l) {
		this.customOnTouchListener = l;
	}
	
	public float getCenterX() {
		return centerX;
	}
	
	public float getCenterY() {
		return centerY;
	}
	
	public boolean isLandscape() {
		return getImageWidth() >= getImageHeight();
	}
	
	private boolean isPortrait() {
		return getImageWidth() <= getImageHeight();
	}
	
	private void setStartingScale(float startingScale) {
		Log.d(TAG,"setting startingscale to "+startingScale);
		this.startingScale = startingScale;
	}
	
	public void setStartingPosition(float x, float y) {
		this.startX = x;
		this.startY = y;
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		this.onClickListener = l;
		Log.d(TAG,"setting click listener?");
		
		if(gestureImageViewTouchListener != null) {
			gestureImageViewTouchListener.setOnClickListener(l);
		} else
			Log.d(TAG,"no listener?");
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		this.onLongClickListener = l;
		if(gestureImageViewTouchListener != null) {
			gestureImageViewTouchListener.setOnLongClickListener(l);
		} 
	}

	/**
	 * Returns true if the image dimensions are aligned with the orientation of the device.
	 * @return
	 */
	public boolean isOrientationAligned() {
		if(deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			return isLandscape();
		}
		else if(deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
			return isPortrait();
		}
		return true;
	}
	
	public int getDeviceOrientation() {
		return deviceOrientation;
	}
	
	//Change zoom.
	public void startZoom(float x, float y, float zoomFactor) {
		gestureImageViewTouchListener.startZoom(x, y, zoomFactor);
	}
}
