package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.NudgeListener;

/**
 * Created by Terje on 2016-07-12.
 */
public class NudgeView extends View {
    private static final String TAG = "NudgeView";


    private static  int MARGIN_RIGHT = 0;
    private static  int MARGIN_BOTTOM = 0 ;
    private static int centralButtonR;
    private NudgeListener listener;
    private int w=0;
    private int h=0;
    private float diameter =115;
    private Paint p1,p2,p3,p1neg,p2neg,p3neg;
    private RectF rectF;
    private final static float Sweep=90;
    private final static int[] angles = {45,135,225,315};
    private Path upArrow,rightA,downA,leftA;
    private boolean u,d,l,r,c;
    //Tells if timer is running for pressed button.
    private boolean downCount = false;
    private Context ctx;

    public NudgeView(Context context) {
        super(context);
        init(context);
    }

    public NudgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NudgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.ctx = context;
        p1 = new Paint();
        p1.setColor(context.getColor(R.color.primary_dark));
        p1.setStyle(Paint.Style.FILL);
        p1neg = new Paint();
        p1neg.setColor(Color.WHITE);
        p1neg.setStyle(Paint.Style.FILL);
        p2 = new Paint();
        p2.setColor(Color.WHITE);
        p2.setStyle(Paint.Style.STROKE);
        p2.setStrokeWidth(3);
        p3 = new Paint();
        p3.setColor(Color.WHITE);
        p3.setStyle(Paint.Style.FILL);
        p3neg = new Paint();
        p3neg.setColor(Color.BLACK);
        p3neg.setStyle(Paint.Style.FILL);

        DisplayMetrics metrics = new DisplayMetrics();
        float density = context.getResources().getDisplayMetrics().density;
        float sw = context.getResources().getDisplayMetrics().widthPixels;
        //diameter = (int)(sw*.15f);
        MARGIN_RIGHT = (int)dpMeasure(MARGIN_RIGHT);
        MARGIN_BOTTOM=(int)dpMeasure(MARGIN_BOTTOM);

        diameter = dpMeasure(diameter);

    Log.d(TAG," NEW DIAMETER: "+diameter);
        rectF= new RectF();

        Matrix m = new Matrix();
        //An arrow.
        upArrow =new Path();
        upArrow.moveTo(0, -diameter/9);
        upArrow.lineTo(-diameter/14f, diameter/6f);
        upArrow.lineTo(0, diameter/9);
        upArrow.lineTo(diameter/14f, diameter/6f);
        upArrow.close();

        m.setRotate(90);
        rightA =new Path();
        rightA.moveTo(0, -diameter/9);
        rightA.lineTo(-diameter/14f, diameter/6f);
        rightA.lineTo(0, diameter/9);
        rightA.lineTo(diameter/14f, diameter/6f);
        rightA.close();
        rightA.transform(m);

        m.setRotate(180);
        downA =new Path();
        downA.moveTo(0, -diameter/9);
        downA.lineTo(-diameter/14f, diameter/6f);
        downA.lineTo(0, diameter/9);
        downA.lineTo(diameter/14f, diameter/6f);
        downA.close();
        downA.transform(m);

        m.setRotate(270);
        leftA =new Path();
        leftA.moveTo(0, -diameter/9);
        leftA.lineTo(-diameter/14f, diameter/6f);
        leftA.lineTo(0, diameter/9);
        leftA.lineTo(diameter/14f, diameter/6f);
        leftA.close();
        leftA.transform(m);

        downCount=false;

        centralButtonR = (int)diameter/8;


        this.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        r=false;l=false;d=false;u=false;c=false;
                        if (rectF.contains(event.getX(),event.getY())) {

                            //distance from center
                            float y = Math.abs(event.getY()-(h - diameter /2));
                            float x = Math.abs(event.getX()-(w - diameter /2));
                            Log.d(TAG,"x: "+x+" y: "+y+" w: "+w+" h:"+h+" evX: "+event.getX()+" evy: "+event.getY()+ "evM?"+(event.getY()>(h- diameter)));
                            if(y < centralButtonR && x < centralButtonR) {
                                Log.d(TAG,"bulls eye!");
                                c=true;
                                //stop here.
                                return true;
                            }
                            if (event.getX()>(w- diameter /2)) {
                                if (y<=x) {
  //                                  Log.d(TAG, "Fan HÖ vettu");
                                    r = true;
                                }
                            } else if (event.getX()>(w- diameter)) {
                                if (y<=x) {
 //                                   Log.d(TAG, "Fan VÄ vettu");
                                    l = true;
                                }
                            }
                            if (event.getY() > (h - diameter /2)) {
                                if (x<=y) {
 //                                   Log.d(TAG, "Fan NER vettu");
                                    d = true;
                                }
                            } else if (event.getY() > (h - diameter)) {
                                if (x<=y) {
                                    u = true;
 //                                   Log.d(TAG, "Fan UPP vettu");
                                }
                            }
                            //Start timer for automatic nudging.
                            if (l||r||d||u ) {
                                downCount=true;
                                final Handler handler = new Handler();
                                final Runnable runnable = new Runnable(){
                                    public void run() {
                                       if (downCount) {
                                           listener.onNudge(isNudge(),2);
                                           handler.postDelayed(this,100);
                                       }
                                    }
                                };

                                handler.postDelayed(runnable, 1000);
                            }

                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        downCount=false;
                        if (!c)
                            listener.onNudge(isNudge(),1);
                        else
                            listener.centerOnNudgeDot();
                        r=false;l=false;d=false;u=false;c=false;
                        break;
                    default:
                        break;
                }
                invalidate();
                return true;
            }

            private NudgeListener.Direction isNudge() {
                NudgeListener.Direction direction = NudgeListener.Direction.NONE;
                if (r)
                    direction = NudgeListener.Direction.RIGHT;
                if (l)
                    direction = NudgeListener.Direction.LEFT;
                if (u)
                    direction = NudgeListener.Direction.UP;
                if (d)
                    direction = NudgeListener.Direction.DOWN;
                return direction;
            }
        });

    }


    private float dpMeasure(float pixSize) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixSize, ctx.getResources().getDisplayMetrics());
    }


    public void setListener(NudgeListener l) {
        listener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredWidth = (int)(diameter+MARGIN_RIGHT);
        int desiredHeight = (int)(diameter+MARGIN_BOTTOM);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }


        setMeasuredDimension(width, height);


    }

    private boolean calculated=false;

    @Override
    protected void onSizeChanged(int ww, int hh, int oldw, int oldh) {
        Log.d(TAG,"on size changed...new: "+ww+","+hh+", old: "+oldw+","+oldh);
        this.w = ww-MARGIN_RIGHT;
        this.h = hh-MARGIN_BOTTOM;
        float edge_Margin = diameter / 7;

        if (!calculated) {
            rectF.set(w - diameter, h - diameter, w, h);
            upArrow.offset(w - (diameter / 2), (h - diameter) + edge_Margin);
            rightA.offset(w - edge_Margin, h - (diameter) / 2);
            leftA.offset(w - diameter + edge_Margin, h - (diameter) / 2);
            downA.offset(w - diameter / 2, h - edge_Margin);
        }
        calculated = true;

        super.onSizeChanged(ww, hh, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int i=0;

        for (int startA:angles) {
            canvas.drawArc(rectF, startA, Sweep, true, (i==3&&r||i==0&&d||i==1&&l||i==2&&u?p1neg:p1));
            canvas.drawArc(rectF, startA, Sweep, true, p2);

            i++;
            //[x0 y0 x1 y1 x2 y2 ...]
            // canvas.drawLines(points[i++],p2);
        }

        canvas.drawPath(upArrow,u?p3neg:p3);

        canvas.drawPath(rightA,r?p3neg:p3);

        canvas.drawPath(leftA,l?p3neg:p3);

        canvas.drawPath(downA,d?p3neg:p3);

        canvas.drawCircle(rectF.left+diameter/2,rectF.top+diameter/2,centralButtonR,c?p3neg:p3);
        // canvas.drawCircle(w-diameter*density,h-diameter*density,diameter*density,p1);
        // canvas.drawCircle(w-diameter*density,h-diameter*density,diameter*density,p2);
    }


}
