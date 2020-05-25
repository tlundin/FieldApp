package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.DataSource;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.utils.Tools;

import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;

/**
 * Created by Terje on 2016-07-19.
 */
public abstract class ChartBlock  extends Block implements EventListener {

    GraphicalView chart;
    Context ctx;
    WF_Context myContext;
    private int insertIndex = -1;
    final String label;
    private final String container;
    final String name;
    final String margins;
    boolean displayValues = true;
    private boolean isVisible = true;
    private int height;
    private int width;
    final float textSize;
    DataSource myDataSource;
    private WF_Widget myWidget;
    private WF_Container myContainer;
    int[] intMargins=null;

    ChartBlock(String blockId, String name, String label, String container,
               String textSize, String margins, int height, int width, boolean displayValues,
               boolean isVisible) {
        super();
        float textSizeF = 10;
        try {
            textSizeF = Float.parseFloat(textSize);
        } catch (NumberFormatException e) {
            Log.d("vortex", "error in format...default to 10");
        }
        this.blockId = blockId;
        this.name = name;
        this.label = label;
        this.container = container;
        this.textSize = textSizeF;
        this.margins = margins;
        this.displayValues = displayValues;
        this.isVisible = isVisible;
        this.height = height;
        this.width = width;
        Log.d("vortex", "height" + height);
        if (margins!=null) {
            String[] ms = margins.split(" ");
            if (ms.length > 0) {
                int i = 0;
                intMargins = new int[ms.length];
                for (String margin : ms) {
                    if (Tools.isNumeric(margin))
                        intMargins[i++] = Integer.parseInt(margin);
                }
            }
        } else
            intMargins = new int[] {5,5,5,5};

    }


    public void create(WF_Context myContext) {
        // Color of each Pie Chart Sections

        this.myContext = myContext;
        this.ctx = myContext.getContext();
        //delay creat until after other blocks executed, to make sure datasource has been added to context.
        //(In case this block is executed after the datasource block)
        myContext.registerEventListener(this, Event.EventType.onFlowExecuted);
        myContext.registerEventListener(this, Event.EventType.onSave);
        Log.d("blio","now listening to onsave");
        o = GlobalState.getInstance().getLogger();


        myContainer = (WF_Container) myContext.getContainer(container);

        if (myContainer != null) {
            insertIndex = myContainer.getWidgets().size();

        }

    }


    private float dpMeasure(float textSize) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize, ctx.getResources().getDisplayMetrics());
    }


    @Override
    public void onEvent(Event e) {

        Log.d("blio","got event "+e.getType());
        if (e.getType() == Event.EventType.onFlowExecuted) {
            WF_Container myContainer = (WF_Container) myContext.getContainer(container);
            //Create chart chart if not already done.
            if (chart == null) {
                Log.d("vortex", "got onAttach event in pieblock. h w " + height + "," + width);

                if (myContainer != null) {
                    //call abstract for specialization
                    initializeChart();
                    Log.d("vortex","After initChar for: "+getName());
                    myWidget = new WF_Widget(blockId, chart, isVisible, myContext);
                    myContainer.add(myWidget);
                    myContainer.getViewGroup().addView(myWidget.getWidget(), insertIndex);
                    myWidget.getWidget().post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("rasco","www");
                            WF_Container myContainer = (WF_Container) myContext.getContainer(container);
                            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) chart.getLayoutParams();
                            if (height < 0) {
                                height = myContainer.getViewGroup().getHeight();
                            } else {
                                Log.d("vortex", "Applying DIP measurements for height." + height );
                                height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, ctx.getResources().getDisplayMetrics());
                            }
                            if (width < 0) {
                                if (width == -1)
                                    width = myContainer.getViewGroup().getWidth();
                                else {
                                    Log.d("bretox","w set to fill!");
                                    int w = myContainer.getViewGroup().getWidth();
                                    width = w;
                                    height = w;
                                }
                            } else {
                                Log.d("vortex", "Applying DIP measurements for width." + height );
                                width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, ctx.getResources().getDisplayMetrics());
                            }

                            layoutParams.height = height;
                            layoutParams.width = width;

                            myWidget.getWidget().requestLayout();
                            Log.d("vortex","My name: "+getName());
                            Log.d("vortex","W: "+layoutParams.width+" H:"+layoutParams.height);
                            Log.d("vortex","chart is attached: "+myWidget.getWidget().isShown()+" w"+myWidget.getWidget().getWidth()+" h:"+myWidget.getWidget().getHeight());
                            Log.d("vortex","chart is attached: "+ chart.isShown()+" "+myWidget.isVisible());
                        }
                    });
                } else {
                    o.addRow("");
                    o.addRedText("Failed to add round chart block with id " + blockId + " - missing container " + container);
                    Log.e("vortex","missing container in chartblox...fail");
                    myContext.removeEventListener(this);
                    return;
                }


            } else {
                refreshChart();
            }

        } else if (e.getType() == Event.EventType.onSave) {
            Log.d("blio","onsave event occured myDatasource is "+myDataSource);
            if (myDataSource != null && myDataSource.hasChanged()) {
                Log.d("vortex","trying to repaing chart");
                generate();
                chart.repaint();
            }

        }

        if (chart !=null)
            chart.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }


    private void refreshChart() {
        generate();
        WF_Widget myWidget = new WF_Widget(blockId, chart, isVisible, myContext);
        myContainer.add(myWidget);
        ViewGroup parent = ((ViewGroup) chart.getParent());
        if (parent!=null)
            parent.removeView(chart);
        myContainer.getViewGroup().addView(myWidget.getWidget(), insertIndex);
    }


    protected abstract void initializeChart();

    protected abstract CategorySeries generate();

    public String getName() {
        return name;
    }
}