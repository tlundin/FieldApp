package com.teraim.fieldapp.dynamic.blocks;

import android.os.Handler;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Slider;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by Terje on 2016-07-08.
 */




public class CoupledVariableGroupBlock extends Block implements EventListener {
    private static final String TAG = "CoupledVariableGroupBlock";


    private transient WF_Context myC;
    private transient boolean active = false;
    private transient Set<Variable> myVariables;
    private transient int currentSum = 0;
    private transient Handler handler = null;
    private transient Set<WF_ClickableField_Slider> touchedSliders = null;
    private static final Random r = new Random();
    private final List<Expressor.EvalExpr> argumentE;
    private final String groupName;
    private String function;
    private Integer currentEvaluationOfArg = null;
    private long delay=25;

    public CoupledVariableGroupBlock(String id, String groupName, String function, String argument, String delay) {
        blockId=id;
        if (groupName==null||groupName.isEmpty())
            groupName="NoName";
        this.groupName=groupName;
        this.function=function;
        argumentE= Expressor.preCompileExpression(argument);
        if (delay!=null && !delay.isEmpty())
            this.delay=Long.parseLong(delay);
    }

    private static final String SUM = "SUM";
    private static final String MIN_SUM = "MINSUM";
    private static final String MAX_SUM = "MAXSUM";
    private static final String SUM_STICKY_LIMITS = "SUM_STICKY_LIMITS";
    private static final String SUM_STICKY_MIN = "SUM_STICKY_MIN";
    private static final String SUM_STICKY_MAX = "SUM_STICKY_MAX";
    private static final String SUM_STICKY = "SUM_STICKY";

    public void create(final WF_Context myContext) {
        myC = myContext;
        active=false;
        myVariables=new HashSet<Variable>();
        currentEvaluationOfArg = null;
        myContext.registerEventListener(this, Event.EventType.onSave);
        myContext.addSliderGroup(this);
        if (function==null || function.isEmpty()) {
            function = "SUM";
            Log.d(TAG,"?");
        }
        Log.d(TAG,"function is "+function);
        String eventID = blockId==null?"BLOCK_ID_WAS_NULL":blockId;
        blockId = eventID;
        onEvent(new WF_Event_OnSave(eventID));
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onEvent(Event e) {
        Log.d(TAG,"in onEvent");
        if (e.getType() == Event.EventType.onSave) {
            String argument = Expressor.analyze(argumentE);
            Log.d(TAG,"in onSave with "+ argument);
            if (!Tools.isNumeric(argument)){
                Log.d(TAG,"cannot calibrate...argument evaluates to non numeric: "+ argument);
                o.addText("");
                o.addCriticalText("Argument to SUM in SliderGroup "+getName()+" is not numeric: "+ argument +" Expr: "+argumentE);
                return;
            }
            final int sumToReach = Integer.parseInt(argument);
            Log.d(TAG,"sum to reach: "+sumToReach);
            boolean change = false;
            Log.d(TAG,"curreval: "+currentEvaluationOfArg);
            if (currentEvaluationOfArg == null || (sumToReach != currentEvaluationOfArg)) {
                currentEvaluationOfArg = sumToReach;
                change = true;
                Log.d(TAG,"arg change in group "+groupName);
            } //Check if a variable value has changed
            else {
                WF_Event_OnSave onS = (WF_Event_OnSave)e;
                if (onS.getListable() != null) {
                    Set<Variable> variables = onS.getListable().getAssociatedVariables();
                    for (Variable v : variables) {
                        if (isOneofMyVariables(v)) {
                            change = true;
                            Log.d(TAG,"var change in group "+groupName);
                            break;
                        }
                    }
                }
            }
            //If no calibration is active, and value of function has changed or one of the variables in slidergroup has changed.
            if (!active && change) {
                active=true;
                calibrateMe(myC.getSliderGroupMembers(groupName), currentEvaluationOfArg);
            } else {
                Log.d(TAG,"not required");
            }

        }
    }

    private boolean isOneofMyVariables(Variable v) {
        if (myVariables.isEmpty()) {
            List<WF_ClickableField_Slider> sliders = myC.getSliderGroupMembers(getName());
            if (sliders!=null) {
                for(WF_ClickableField entryField:sliders) {
                    Set<Variable> variables = entryField.getAssociatedVariables();
                    if (variables!=null) {
                        myVariables.addAll(variables);
                    }


                }
            }
        }
        if (myVariables!=null) {
            return myVariables.contains(v);
        }

        return false;
    }

    @Override
    public String getName() {
        return groupName;
    }

    public void resetCounter() {
        if (handler!=null) {
            handler.removeCallbacksAndMessages(null);
            active=false;
        }
    }


    private class Range {

        Range(int min, int max) {
            this.min=min;
            this.max=max;
        }
        final int min;
        final int max;

        boolean outsideRange(int sumToReach) {
            Log.d(TAG,"SumtoReach "+sumToReach+" min:"+min+" max: "+max);
            return sumToReach>max || sumToReach<min;
        }
    }

    //spawn a thread that calibrates group towards approved value.
    private void calibrateMe(final List<WF_ClickableField_Slider> sliders, final int sumToReach) {


        Range allSlidersTogether = null;

        if (sliders==null || sliders.isEmpty())
            return;
        if (functionIsStickyLimits() && sliders.size()<3) {
            Log.d(TAG,"sticky return");
            return;
        }
        if (functionIsSticky() && sliders.size()<2) {
            Log.d(TAG,"sticky min max return");
            return;
        }

        Set<WF_ClickableField_Slider> min=null,max=null;
        //try to leave touched sliders still if possible. if not, try a second time with all sliders.

        boolean done = false;
        min = new HashSet<>();
        max = new HashSet<>();

        //Try first with as many sliders fixed as possible.
        boolean touchedFixed=true, minFixed=functionIsStickyLimits() || functionIsStickyLimitsMin(),
                maxFixed = functionIsStickyLimits() || functionIsStickyLimitsMax();
        if (touchedSliders!=null)
            Log.d(TAG,"touchedsliders has  "+touchedSliders.size()+" elements");
        allSlidersTogether = calculateRange(sliders, touchedSliders, touchedFixed,minFixed,maxFixed);
        WF_ClickableField_Slider last = null;

        if (allSlidersTogether.outsideRange(sumToReach)) {
            Log.d(TAG, "cannot reach sum...try to give back all but last slider");
            if (touchedSliders!=null&&!touchedSliders.isEmpty()) {
                Iterator<WF_ClickableField_Slider> it = touchedSliders.iterator();

                while (it.hasNext())
                    last = it.next();
                Set<WF_ClickableField_Slider> onlyLast = new HashSet<>();
                onlyLast.add(last);
                allSlidersTogether = calculateRange(sliders, onlyLast, true, minFixed, maxFixed);
            }
            if (allSlidersTogether.outsideRange(sumToReach)) {
                last = null;
                Log.d(TAG, "cannot reach sum...try to give back all touched");
                touchedFixed=false;
                allSlidersTogether = calculateRange(sliders, null, false,minFixed,maxFixed);
                if (allSlidersTogether.outsideRange(sumToReach)) {
                    Log.d(TAG, "cannot reach sum...try to give back all min max");
                    //Still outside?, try without fixing minmax
                    minFixed=false;
                    maxFixed=false;
                    allSlidersTogether = calculateRange(sliders, null, false,false,false);
                }
            }
        }



        //if still outside we cannot reach and we have an error.
        boolean isOutside = allSlidersTogether.outsideRange(sumToReach);

        if (isOutside) {
            Log.d(TAG,"over Max or below min: "+sumToReach+", "+allSlidersTogether.min
                    +" , "+allSlidersTogether.max);
            o = LogRepository.getInstance();
            o.addText("");
            o.addCriticalText("Argument to SUM in SliderGroup "+getName()+" is not possible to reach: "+sumToReach+". Outside the range: "+allSlidersTogether.min+" - "+allSlidersTogether.max);
            return;
        }
        final List<WF_ClickableField_Slider> slidersToCalibrate= new LinkedList<>(sliders);
        //Remove sliders that are static.
        if (last!=null) {
            slidersToCalibrate.remove(last);
        } else if (touchedFixed &&touchedSliders!=null && !touchedSliders.isEmpty()) {
            slidersToCalibrate.removeAll(touchedSliders);

        }

        if (minFixed || maxFixed) {
            for(WF_ClickableField_Slider slider:sliders) {
                if ((minFixed && slider.getPosition() == slider.getMin()) ||
                        (maxFixed && slider.getPosition() == slider.getMax()))
                    slidersToCalibrate.remove(slider);
            }
        }

        Log.d(TAG,"SUM TO REACH: "+sumToReach);
        Log.d(TAG,"sl "+sliders.size()+" touch "+(touchedSliders==null?"null":touchedSliders.size())+" min "+(min==null?"null":min.size())+" max "+(max==null?"null":max.size()));
        currentSum = 0;

        for (WF_ClickableField_Slider slider:sliders) {
            Integer value = slider.getSliderValue();
            if (value!=null) {
                currentSum += value;
                Log.d(TAG,"slider "+slider.getId()+" value: "+value);
            }

        }
        Log.d(TAG,"Currentsum initial is: "+currentSum);
        Log.d(TAG,"number of sliders:" +slidersToCalibrate.size());

        if (slidersToCalibrate.size()>0) {


            handler = new Handler();
            Runnable runnable = new Runnable() {
                public void run() {
                    if (currentSum != sumToReach) {
                        //Check if any change.
                        int anyChange = 0;
                        int oldSum = currentSum;
                        Log.d(TAG, "Currentsum is: " + currentSum + " Min: " + functionIsMinSum() + " Max: " + functionIsMaxSum() + " SL: " + functionIsStickyLimits() + " SUM: " + functionIsSum() + " STICKY: " + functionIsSticky() + " sum to reach: " + sumToReach);
                        Log.d(TAG, "sliderstocalibrate has "+slidersToCalibrate.size()+" members");
                        int remainingDifference = 0;
                        for (WF_ClickableField_Slider slider : slidersToCalibrate) {
                            remainingDifference = sumToReach-currentSum;

                            if (remainingDifference > 0 ) {
                                increaseSlider(slider,remainingDifference);

                            } else if (remainingDifference < 0 ) {
                                decreaseSlider(slider,currentSum-sumToReach);
                            }

                        }

                        handler.postDelayed(this, 25);
                        Log.d(TAG, "Current sum is: " + currentSum);
                    } else {
                        Log.d(TAG, "Sliders are calibrated.update variables.");
                        updateSliderVariables(slidersToCalibrate);
                        if (touchedSliders!=null)
                            touchedSliders.clear();
                        active = false;
                    }
                }


            };

            handler.postDelayed(runnable, delay);
        }
        else {
            active = false;
            Log.d(TAG, "No sliders to calibrate");
        }


    }

    //calculate the possible min to max range with current settings
    private Range calculateRange(List<WF_ClickableField_Slider> sliders, Set<WF_ClickableField_Slider> touchedSliders, boolean touchedFixed,boolean minFixed, boolean maxFixed) {
        int totalMin=0,totalMax=0;
        for (WF_ClickableField_Slider slider:sliders) {
            //check if slider is locked or variable. If variable, use the min max values. if static, use current.
            if (
                    (touchedSliders!=null && touchedSliders.contains(slider) && touchedFixed) ||
                            (minFixed && slider.getPosition() == slider.getMin()) ||
                            (maxFixed && slider.getPosition() == slider.getMax()))

            {
                totalMin += slider.getSliderValue();
                totalMax += slider.getSliderValue();
            } else {
                totalMin += slider.getMin();
                totalMax += slider.getMax();
            }


        }

        return new Range(totalMin,totalMax);
    }

    private boolean functionIsSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM);
    }

    private boolean functionIsMaxSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.MAX_SUM);
    }


    private boolean functionIsMinSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.MIN_SUM);
    }

    private boolean functionIsSticky() {
        return function.toUpperCase().startsWith(CoupledVariableGroupBlock.SUM_STICKY);
    }
    private boolean functionIsStickyLimits() {
        return function.toUpperCase().startsWith(CoupledVariableGroupBlock.SUM_STICKY_LIMITS);
    }
    private boolean functionIsStickyLimitsMin() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM_STICKY_MIN);
    }
    private boolean functionIsStickyLimitsMax() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM_STICKY_MAX);
    }
    private void updateSliderVariables(List<WF_ClickableField_Slider> sliders) {
        for (WF_ClickableField_Slider slider:sliders) {
            slider.setValueFromSlider();
        }
    }

    private void increaseSlider(WF_ClickableField_Slider slider, int remainingDifference) {
        int curr = slider.getPosition();
        int increase = calc(curr, slider.getMin(),slider.getMax());

        if (increase==0 && r.nextBoolean()) {
            Log.e("vortex","fecckoo");
            increase = 1;
        }

        if (increase>remainingDifference)
            increase=remainingDifference;
        // Log.d(TAG,"INC: "+increase);

        if ((curr+increase)<=slider.getMax()) {
            Log.d(TAG,"INCREASE SLIDER: "+slider.getName()+" OLD: "+curr+" NEW: "+(curr+increase));
            currentSum+=increase;
            slider.setPosition(curr+increase);
            slider.wasIncreased();
            //   Log.d(TAG,"currsum: "+currentSum);
        } else
            Log.d(TAG,slider.getId()+" is over max in increase: "+slider.getPosition());
    }

    private void decreaseSlider(WF_ClickableField_Slider slider, int remainingDifference) {
        int curr = slider.getPosition();
        int decrease = calc(curr, slider.getMin(),slider.getMax());
        if (decrease==0 && r.nextBoolean()) {
            decrease=1;
        }
        if (decrease>remainingDifference)
            decrease=remainingDifference;


        if ((curr-decrease)>=slider.getMin()) {
            currentSum-=decrease;
            slider.setPosition(curr-decrease);
            slider.wasDecreased();
            Log.d(TAG,"DECREASE SLIDER: "+slider.getName()+" OLD: "+curr+" NEW: "+(curr-decrease));
        } else
            Log.d(TAG,slider.getName()+" is below min in increase: "+slider.getPosition());



    }


    private int calc(float x, int min, int max) {
       return 2;
    }




    //removes a slider and subtracts its current value from the target to reach.
    //This is done when the user has touched a slider. The touched slider should no longer be adjusted.

    public void removeSliderFromCalibration(WF_ClickableField_Slider slider) {
        if (slider==null)
            return;
        if (touchedSliders==null)
            touchedSliders=new LinkedHashSet<>();
        //keep order...last touched should be last element.
        touchedSliders.remove(slider);
        touchedSliders.add(slider);
    }
}
