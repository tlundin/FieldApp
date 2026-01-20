package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition.SpinnerElement;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.CombinedRangeAndListFilter;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class WF_ClickableField extends WF_Not_ClickableField implements EventGenerator {
	private static final String TAG = "WF_ClickableField";



    private final LinearLayout innerInputContainer;
    private final ScrollView scrollableInputContainer;
    private final TextView headerInputCointainer;

    final Map<Variable, VariableView> myVars = new LinkedHashMap<>();

    private boolean autoOpenSpinner = true;
    private final GlobalState gs;
    private static String[] opt=null, val=null;
    private final VariableConfiguration al;
    private static final boolean HIDE = false;
    private static final boolean SHOW = true;
    private final Map<Variable, String[]> values = new HashMap<>();



    private final SpinnerDefinition sd;

    // Special behavior: If only a single boolean, don't open up the dialog.
    // Just set the value on click.
    private boolean singleBoolean = false;

    private Drawable originalBackground=null;

    boolean iAmOpen = false;
    private Spinner firstSpinner = null;
    private List<Rule> myRules;

    class VariableView {
        View view;
        boolean displayOut;
        String format;
        boolean isVisible;
        boolean showHistorical;
        String listTag;
        SpinnerAdapter adapter;
    }
    protected abstract LinearLayout getFieldLayout();

    @Override
    public Set<Variable> getAssociatedVariables() {
        return myVars.keySet();
    }

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.tagpopmenu, menu);
            setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem x = menu.getItem(0);
            MenuItem y = menu.getItem(1);
            MenuItem z = menu.getItem(2);
            Log.d(TAG, "myVars has " + myVars.size() + " elements. "
                    + myVars.toString());
            if (myVars.size() > 0) {
                z.setVisible(true);
                List<String> row = myVars.keySet().iterator().next()
                        .getBackingDataSet();
                String url = al.getUrl(row);

                if (url == null || url.length() == 0)
                    x.setVisible(false);
                else
                    x.setVisible(true);
                if (row != null && ((al.getVariableDescription(row) != null
                        && al.getVariableDescription(row).length() > 0 ) || (al.getGroupDescription(row)!=null && al.getGroupDescription(row).length()>0)))
                    y.setVisible(true);
                else {
                    y.setVisible(false);
                    Log.d(TAG,(row==null?"null":"vD: "+al.getVariableDescription(row)));
                }

            } else {
                x.setVisible(false);
                y.setVisible(false);
                z.setVisible(false);
            }
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            List<String> row = null;
            Iterator<Variable> it = myVars.keySet().iterator();
            if (it.hasNext())
                row = it.next().getBackingDataSet();

            switch (item.getItemId()) {
                case R.id.menu_goto:
                    if (row != null) {
                        String url = al.getUrl(row);
                        if (url!=null) {
                            Intent browse = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse(url));
                            browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            gs.getContext().startActivity(browse);
                        }
                    }
                    return true;
                case R.id.menu_delete:

                    if(innerInputContainer.getChildCount()==1) {
                        Log.d(TAG,"creating input fields!");
                        createInputFields();

                    }
                    for (Entry<Variable, VariableView> pairs : myVars.entrySet()) {
                        Variable variable = pairs.getKey();
                        Log.d(TAG, "deleting variable " + variable.getId()
                                + " with value " + variable.getValue());
                        DataType type = variable.getType();
                        View view = pairs.getValue().view;

                        if (type == DataType.numeric || type == DataType.decimal
                                || type == DataType.text) {
                            EditText etview = view
                                    .findViewById(R.id.edit);
                            etview.setText("");
                        } else if (type == DataType.list) {
                            LinearLayout sl = (LinearLayout) view;
                            Spinner sp = sl.findViewById(R.id.spinner);
                            if (sp.getTag(R.string.u1) != null) {
                                TextView descr = sl
                                        .findViewById(R.id.extendedDescr);
                                descr.setText("");
                            }
                            sp.setSelection(-1);

                        } else if (type == DataType.bool) {
                            RadioGroup rbg = view
                                    .findViewById(R.id.radioG);
                            rbg.check(-1);
                        }

                    }
                    save();
                    refresh();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_info:
                    if (row != null) {
                        StringBuilder msg =
                                new StringBuilder("Var_Label: " + al.getVarLabel(row) + "\n" +
                                        "Var_Desc : " + al.getVariableDescription(row) + "\n");
                        int i = 1;

                        while (row!=null)  {
                            msg.append(" Group_Lbl ").append(i).append(": ").append(al.getGroupLabel(row)).append("\n").append(" Group_Desc ").append(i).append(":  ").append(al.getGroupDescription(row)).append("\n");
                            i++;
                            row = (it.hasNext()?it.next().getBackingDataSet():null);
                        }


                        new AlertDialog.Builder(myContext.getContext())
                                .setTitle(gs.getString(R.string.description))
                                .setMessage(msg.toString())
                                .setPositiveButton(android.R.string.yes,
                                        (dialog, which) -> mode.finish())
                                .setIcon(android.R.drawable.ic_dialog_info).show();
                    }
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG,"ondestroy! for "+getLabel());
            mActionMode = null;
            revertBackgroundColor();
        }
    };




    private ActionMode mActionMode;

    WF_ClickableField(final String label, final String descriptionT,
                      WF_Context context, String id, View view, boolean isVisible, DisplayFieldBlock format) {
        super(id, label, descriptionT, context, view, isVisible, format);
        //Log.d(TAG,"Creating WF_ClickableField: label: "+label+" descr: "+descriptionT+ " id: "+id);
        //change between horizontal and vertical


        gs = GlobalState.getInstance();
        sd = gs.getSpinnerDefinitions();
        al = gs.getVariableConfiguration();
        o = gs.getLogger();



        // SpannableString content = new SpannableString(headerT);
        // content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        scrollableInputContainer = (ScrollView)LayoutInflater.from(myContext.getContext()).inflate(
                R.layout.input_container, null);
        innerInputContainer = (LinearLayout)scrollableInputContainer.findViewById(R.id.inner);
        headerInputCointainer = (TextView) innerInputContainer.findViewById(R.id.header);
       // inputContainer = new LinearLayout(context.getContext());
       // inputContainer.setOrientation(LinearLayout.VERTICAL);
       // inputContainer.setLayoutParams(new LinearLayout.LayoutParams(
        //        LinearLayout.LayoutParams.MATCH_PARENT,
        //        LinearLayout.LayoutParams.MATCH_PARENT, 1));

        // Empty all inputs and save.
        getWidget().setClickable(true);
        getWidget().setOnLongClickListener(v -> {

            if (mActionMode != null) {
                return false;
            }




            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = ((Activity) myContext.getContext())
                    .startActionMode(mActionModeCallback);
            WF_ClickableField.this.getWidget().setSelected(true);
            return true;

        });

        getWidget().setOnClickListener(v -> {
            if (WF_ClickableField.this instanceof WF_ClickableField_Slider) {
                Log.d(TAG,"click denied! I am slider ");
                return;
            }

            if(innerInputContainer.getChildCount()==1) {
                Log.d(TAG,"creating input fields!");
                createInputFields();

            } else {
                Log.d(TAG,"not! creating input fields!");
            }
            setBackgroundColor(Color.parseColor(Constants.Color_Pressed));

            // special case. No dialog.

            if (singleBoolean) {
                Log.d(TAG,"singleboolean true..setting radio");
                VariableView vv = myVars.values().iterator().next();
                Variable var = myVars.keySet().iterator().next();
                String value = var.getValue();
                RadioButton ja = vv.view.findViewById(R.id.ja);
                RadioButton nej = vv.view.findViewById(R.id.nej);
                if (value == null || var.getValue().equals("false"))
                    ja.setChecked(true);
                else
                    nej.setChecked(true);
                save();
                refresh();
                //v.setBackgroundDrawable(originalBackground);
                revertBackgroundColor();
            } else {
                // On click, create dialog
                AlertDialog.Builder alert =
                        new AlertDialog.Builder(v.getContext());
                alert.setTitle(label);
                //alert.setMessage(myDescription);
                headerInputCointainer.setText(myDescription);
                refreshInputFields();
                iAmOpen = true;

                alert.setPositiveButton(R.string.save,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                iAmOpen = false;
                                save();
                                refresh();
                                ViewGroup x = ((ViewGroup) scrollableInputContainer
                                        .getParent());
                                if (x != null)
                                    x.removeView(scrollableInputContainer);
                                revertBackgroundColor();
                                //v.setBackgroundDrawable(originalBackground);
                            }
                        });
                alert.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                iAmOpen = false;
                                ViewGroup x = ((ViewGroup) scrollableInputContainer
                                        .getParent());
                                if (x != null)
                                    x.removeView(scrollableInputContainer);
                                revertBackgroundColor();

                                //v.setBackgroundDrawable(originalBackground);
                            }
                        });
                if (scrollableInputContainer.getParent() != null)
                    ((ViewGroup) scrollableInputContainer.getParent())
                            .removeView(scrollableInputContainer);
                Dialog d = alert.setView(scrollableInputContainer).create();
                d.setCancelable(true);
                // WindowManager.LayoutParams lp = new
                // WindowManager.LayoutParams();
                // lp.copyFrom(d.getWindow().getAttributes());
                // lp.height = WindowManager.LayoutParams.FILL_PARENT;
                // lp.height = 600;

                d.show();

            }
            // d.getWindow().setAttributes(lp);

        });

    }

    private void setBackgroundColor(int color) {
        if (originalBackground==null)
            originalBackground = getWidget().getBackground();
        getWidget().setBackgroundColor(color);
    }


    @SuppressLint("NewApi")
    private void revertBackgroundColor() {
        if (originalBackground!=null) {
            getWidget().setBackgroundColor(backgroundColor);
            getWidget().setBackground(originalBackground);
            originalBackground=null;
        } else
            getWidget().setBackgroundColor(backgroundColor);

    }



    private boolean useStatic = false;

    public void addStaticVariable(final Variable var, boolean displayOut,
                            String format, boolean isVisible, boolean showHistorical) {
        this.useStatic=true;

        addVariable(var,displayOut,format,isVisible,showHistorical);

    }

    public static void clearStaticGlobals() {
        WF_ClickableField.opt=null;
        WF_ClickableField.val=null;
    }


    public void addVariable(final Variable var, boolean displayOut,
                            String format, boolean isVisible, boolean showHistorical) {


        String varId = var.getId();
        Log.d(TAG,"Adding "+var.getLabel());

        if (virgin) {
            virgin = false;
            if (var.getType() != null && var.getType().equals(DataType.bool))
                singleBoolean = true;
            myDescription = al.getDescription(var.getBackingDataSet());
        } else
            // cancel singleboolean if it was set.
            if (singleBoolean)
                singleBoolean = false;

        if (super.getKey() == null) {
            Log.d(TAG, "Setting key variable to " + varId);
            super.setKey(var);

        }

        if (var.getType() == null) {
            o.addText("");
            o.addCriticalText("VARIABLE " + var.getId()
                    + " HAS NO TYPE. TYPE ASSUMED TO BE NUMERIC");
            var.setType(DataType.numeric);
        }

        //Delay creation of ui elements. Store info.

        VariableView vv = new VariableView();

        vv.isVisible=isVisible;
        vv.displayOut=displayOut;
        vv.format=format;
        vv.showHistorical=showHistorical;
        vv.listTag = null;

        myVars.put(var,vv);


        //create list.

        if(var.getType()== DataType.list) {
            String[] opt=null;
            String[] val=null;

            //If adding variables in a list, they will all share same opt and val. Can reuse.
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    myContext.getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new ArrayList<String>());
            vv.adapter = adapter;

            if (useStatic && WF_ClickableField.opt!=null) {
                Log.d(TAG,"using static");
                useStatic=false;
                opt = Arrays.copyOfRange(WF_ClickableField.opt,0,WF_ClickableField.opt.length);
                if (WF_ClickableField.val!=null)
                    val = Arrays.copyOfRange(WF_ClickableField.val,0,WF_ClickableField.val.length);
                values.put(var, val==null?opt:val);
            } else {

                String listValues = al.getTable().getElement("List Values",
                        var.getBackingDataSet());

                // Parse
                if (listValues.startsWith("@file")) {
                    Log.d(TAG, "Found complex spinner");
                    if (sd == null) {
                        o.addText("");
                        o.addCriticalText("Spinner definition file has not loaded. Spinners cannot be created!");
                    } else {
                        List<SpinnerElement> elems = sd.get(var.getId().toLowerCase());
                        if (elems == null) {
                            Log.e("nils",
                                    "No spinner elements for variable "
                                            + var.getId());
                            Log.e("nils", "backing row: " + var.getBackingDataSet());
                            o.addText("");
                            o.addCriticalText("Complex Spinner variable "
                                    + var.getId()
                                    + " is not defining any elements in the configuration file (Spinners.csv). Correct file version?");

                        } else {
                            Log.d(TAG, "Spinner variable: " + var.getId());
                            int i = 0;
                            opt = new String[elems.size()];
                            val = new String[elems.size()];
                            for (SpinnerElement se : elems) {
                                Log.d(TAG, "Spinner element: " + se.opt
                                        + " Value: " + se.value);
                                opt[i] = se.opt;
                                val[i++] = se.value;
                            }
                            vv.listTag = var.getId();
                            values.put(var, val);

                        }
                    }
                } else {
                    if (listValues.startsWith("@col")) {
                        vv.listTag = "dynamic";
                        Log.d(TAG,"dynamic!!");
                    } else {
                        Log.d(TAG, "Found static list definition for"+var.getLabel()+"..parsing");
                        opt = listValues.split("\\|");
                        if (opt == null || opt.length < 2) {
                            o.addText("");
                            o.addCriticalText("Could not split List Values for variable "
                                    + var.getId() + ". Did you use '|' symbol??");
                        } else {

                            if (opt[0].contains("=")) {
                                Log.d(TAG, "found static list with value pairs");
                                // we have a value.
                                Log.d(TAG, "List found is " + listValues
                                        + "...opt has " + opt.length + " elements.");
                                val = new String[opt.length];
                                int c = 0;
                                String tmp[];
                                for (String s : opt) {
                                    s = s.replace("{", "");
                                    s = s.replace("}", "");
                                    tmp = s.split("=");
                                    if (tmp == null || tmp.length != 2) {
                                        Log.e("nils", "found corrupt element: " + s);
                                        o.addText("");
                                        o.addCriticalText("One of the elements in list "
                                                + var.getId()
                                                + "has a corrupt element. Comma missing?");
                                        val[c] = "****";
                                        opt[c] = "*saknar värde*";
                                    } else {
                                        val[c] = tmp[1];
                                        opt[c] = tmp[0];
                                    }
                                    c++;
                                }
                                values.put(var, val);
                            } else
                                values.put(var, opt);

                        }
                    }
                }
                WF_ClickableField.opt = opt;
                WF_ClickableField.val = val;

            }
            if (opt != null) {
                adapter.addAll(opt);
                Log.d(TAG, "Adapter has " + adapter.getCount() + " elements");
                //update static with this calc.

                adapter.notifyDataSetChanged();
            } else
                Log.e("nils",
                        "Couldnt add elements to spinner - opt was null in WF_ClickableField");


        } else {
            opt=null;
            val=null;
        }



        OutC w = null;
        Log.d(TAG,"in addvariable, displayout is "+displayOut+" for variable "+var.getId());
        if (displayOut) {
            LinearLayout ll = getFieldLayout();
            w = opt!=null ? new OutSpin(ll, opt, val) : new OutC(ll, format);
            myOutputFields.put(var, w);
            outputContainer.addView(ll,0);
            //Log.d(TAG,"Added viewz "+var.getLabel()+" with width: "+ll.getWidth());
            // refreshInputFields();
            refreshOutputField(var, w);

        }

    }

    private int findSpinnerIndexFromValue(String hist, String[] val) {
        int h = Integer.parseInt(hist);
        if (val == null)
            return h;
        int i = 0;
        for (String v : val) {
            if (Tools.isNumeric(v)) {
                if (hist.equals(v))
                    return i;
            }
            i++;
        }
        return h;
    }

    void save() {
        Log.d(TAG,"in save");
        boolean saveEvent = false;
        String newValue = null, existingValue = null;
        // for now only delytevariabler.
        Map<Variable, String> oldValue = new HashMap<Variable, String>();
        Iterator<Map.Entry<Variable, VariableView>> it = myVars.entrySet().iterator();
        // String invalidateKeys=null;
        Context ctx = myContext.getContext();

        while (it.hasNext()) {
            Map.Entry<Variable, VariableView> pairs = it
                    .next();
            Variable variable = pairs.getKey();
            existingValue = variable.getValue();
            oldValue.put(variable, existingValue);
            DataType type = variable.getType();
            View view = pairs.getValue().view;
            Log.d(TAG, "Variable: "+variable.getLabel()+" Existing value: " + existingValue);
            if (type == DataType.bool) {
                // Get the yes radiobutton.
                RadioGroup rbg = view.findViewById(R.id.radioG);
                // If checked set value to True.
                int id = rbg.getCheckedRadioButtonId();

                if (id == R.id.nej) {
                    newValue = "false";
                } else if (id == R.id.ja) {
                    newValue = "true";
                } else
                    newValue = null;
            } else if (type == DataType.numeric || type == DataType.text
                    || type == DataType.decimal) {
                EditText etview = view.findViewById(R.id.edit);
                String txt = etview.getText().toString();
                if (txt.trim().length() > 0)
                    newValue = txt;
                else
                    newValue = null;
            } else if (type == DataType.list) {
                LinearLayout sl = (LinearLayout) view;
                Spinner sp = sl.findViewById(R.id.spinner);
                int s = sp.getSelectedItemPosition();
                String v[] = values.get(variable);
                if (v != null) {
                    if (s >= 0 && s < v.length)
                        newValue = v[s];
                    else
                        newValue = null;
                    Log.d(TAG, "VALUE FOR SPINNER A " + newValue);
                } else {
                    newValue = (String) sp.getSelectedItem();
                    Log.d(TAG, "VALUE FOR SPINNER B " + newValue);
                }
            } else if (type == DataType.auto_increment) {
                EditText etview = view.findViewById(R.id.edit);
                String s = etview.getText().toString();
                if (s != null && s.length() > 0) {
                    int val = Integer.parseInt(etview.getText().toString());
                    val++;
                    newValue = val + "";
                } else {
                    Log.e("vortex", "value is null or len 0 in auto_increment");
                    newValue = existingValue;
                }
            }

            if (newValue == null || !newValue.equals(existingValue)
                    || variable.isUsingDefault()) {
                Log.d(TAG, "New value: " + newValue);
                saveEvent = true;

                if (newValue == null) {
                    Log.e("vortex", "Calling delete on " + variable.getId()
                            + "Obj:" + variable + " with keychain\n"
                            + variable.getKeyChain().toString());
                    variable.deleteValue();
                    Log.e("vortex",
                            "Getvalue now returns: " + variable.getValue());
                } else {
                    // Re-evaluate rules.
                    if (variable.hasValueOutOfRange()) {
                        saveEvent = false;
                        String earlierValue = variable.getValue();
                        if (earlierValue == null)
                            earlierValue = "";
                        Vibrator myVibrator = (Vibrator) ctx
                                .getSystemService(Context.VIBRATOR_SERVICE);
                        myVibrator.vibrate(250);
                        new AlertDialog.Builder(ctx)
                                .setTitle("Incorrect value")
                                .setMessage(
                                        "The value you entered is outside the allowed range. Earlier value will be used: ["
                                                + earlierValue + "]")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .setNeutralButton("Ok",
                                        new Dialog.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                // TODO Auto-generated method
                                                // stub

                                            }
                                        }).show();
                    } else {
                        // check rules if value is in range.
                        variable.setOnlyCached(newValue);

                    }

                }

            } else {
                Log.d(TAG, "New value was not set: " + newValue);
            }
        }

        Rule r = checkRules();
        if (r != null) {
            revertChanges();
            saveEvent = false;
            Vibrator myVibrator = (Vibrator) ctx
                    .getSystemService(Context.VIBRATOR_SERVICE);
            myVibrator.vibrate(250);
            new AlertDialog.Builder(ctx).setTitle(r.getRuleHeader())
                    .setMessage(r.getRuleText())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setNeutralButton("Ok", new Dialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub

                        }
                    }).show();
        }
        if (saveEvent) {

            boolean contextChanged = false;
            // Commit cached value into db.
            for (Variable v : myVars.keySet()) {

                String value = v.getValue();
                v.setOnlyCached(null);
                // If value of variable has changed and it is a context
                // variable, the whole workflow needs reload!
                if (v.setValue(value) && myContext.isContextVariable(v.getId())) {
                    Log.e("vortex",
                            "detected change of context variable in wf_clickfield: "
                                    + v.getLabel());
                    contextChanged = true;
                    // If context changed, rerun the workflow.

                }

            }
            if (contextChanged) {
                // No save event...reload the whole workflow instead.
                myContext.reload();
                return;
            }
            Log.d(TAG, "IN SAVE() SENDING EVENT");
            gs.sendEvent(MenuActivity.REDRAW);
            myContext
                    .registerEvent(new WF_Event_OnSave(this.getId(), oldValue,this));

            // myContext.registerEvent(new WF_Event_OnContextChange());
            // if (contextChanged)
            // myContext.reload();
        }

    }

    /**
     * If a rule is broken, revert value changes.
     */
    private void revertChanges() {
        for (Variable v : myVars.keySet()) {
            v.revert();
        }
    }

    private Rule checkRules() {

        if (myRules == null)
            return null;
        Log.d(TAG, "In checkRules. I have " + myRules.size() + " rules");
        for (Rule r : myRules) {
            Log.d(TAG, " Rule: " + r.getCondition());

                Boolean res = r.execute();
                if (res != null && !res)
                    return r;

        }
        return null;
    }



    //Check, and if required, create the inputfield elements.
    void createInputFields() {
        Log.d(TAG,"in createInputFields");

        int vc=0;
        for (Variable var : myVars.keySet()) {
            VariableView varV = myVars.get(var);
            String unit = var.getPrintedUnit();
            String varLabel = var.getLabel();
            String varId = var.getId();
            String hist=null;


            if (varV.showHistorical) {
                hist = var.getHistoricalValue();
                Log.d(TAG,"historical fetched");

            }


            switch (var.getType()) {

                case bool:
                    // o.addText("Adding boolean dy-variable with label "+label+", name "+varId+", type "+var.getType().name()+" and unit "+unit.name());
                    View view = LayoutInflater.from(myContext.getContext()).inflate(
                            R.layout.ja_nej_radiogroup, null);
                    TextView header = view.findViewById(R.id.header);

                    if (Tools.isNumeric(hist)) {
                        String histTxt = (hist.equals("true") ? gs.getContext().getString(
                                R.string.yes) : gs.getContext().getString(R.string.no));
                        SpannableString s = new SpannableString(varLabel + " ("
                                + histTxt + ")");
                        s.setSpan(new TextAppearanceSpan(gs.getContext(),
                                        R.style.PurpleStyle), varLabel.length() + 2,
                                s.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        header.setText(s);
                    } else
                        header.setText(varLabel);
                    innerInputContainer.addView(view);
                    varV.view=view;
                    break;
                case list:

                    LinearLayout sl = (LinearLayout) LayoutInflater.from(
                            myContext.getContext()).inflate(
                            R.layout.edit_field_spinner, null);
                    final TextView sHeader = sl.findViewById(R.id.header);
                    final TextView sDescr = sl
                            .findViewById(R.id.extendedDescr);
                    final Spinner spinner = sl.findViewById(R.id.spinner);

                    spinner.setAdapter(varV.adapter);
                    innerInputContainer.addView(sl);
                    Log.d(TAG, "Adding spinner for label " + label);

                    if (firstSpinner == null && vc==0 && autoOpenSpinner)
                        firstSpinner = spinner;
                    Log.d(TAG,"Setting tag to "+varV.listTag);
                    spinner.setTag(R.string.u1, varV.listTag);

                    varV.view=sl;

                    sHeader.setText(varLabel + (hist != null ? " (" + hist + ")" : ""));
                    //String listValues = al.getTable().getElement("List Values",
                    //        var.getBackingDataSet());


                    String[] opt =values.get(var);
                    if (opt != null && hist != null) {
                        try {
                            int histI = findSpinnerIndexFromValue(hist, opt);
                            if (histI < opt.length) {
                                String histT = opt[histI];

                                SpannableString s = new SpannableString(varLabel
                                        + " (" + histT + ")");
                                s.setSpan(new TextAppearanceSpan(gs.getContext(),
                                                R.style.PurpleStyle),
                                        varLabel.length() + 2, s.length() - 1,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                sHeader.setText(s);
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "Hist spinner value is not a number: "
                                    + hist);
                        }
                    }

                    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView,
                                                   View selectedItemView, int position, long id) {
                            // Check if this spinner has side effects.
                            if (sd != null) {
                                String emsS = (String) spinner
                                        .getTag(R.string.u1);
                                List<SpinnerElement> ems = null;
                                if (emsS != null)
                                    ems = sd.get(emsS);
                                @SuppressWarnings("unchecked")
                                List<String> curMapping = (List<String>) spinner
                                        .getTag(R.string.u2);
                                if (ems != null) {
                                    SpinnerElement e = ems.get(position);
                                    Log.d(TAG,
                                            "In onItemSelected. Spinner Element is "
                                                    + e.opt + " with variables "
                                                    + e.varMapping.toString());
                                    if (e.varMapping != null) {
                                        // hide the views for the last selected.
                                        hideOrShowViews(curMapping, HIDE);
                                        hideOrShowViews(e.varMapping, SHOW);
                                        spinner.setTag(R.string.u2, e.varMapping);
                                        sDescr.setText(e.descr);
                                        Log.d(TAG, "DESCR TEXT SET TO " + e.descr);
                                    }
                                }
                            }
                        }

                        private void hideOrShowViews(List<String> varIds, boolean mode) {
                            Log.d(TAG, "In hideOrShowViews...");
                            if (varIds == null || varIds.size() == 0)
                                return;

                            for (String varId : varIds) {
                                Log.d(TAG, "Trying to find " + varId);
                                if (varId != null) {
                                    for (Variable v : myVars.keySet()) {
                                        Log.d(TAG, "Comparing with " + v.getId());
                                        if (v.getId().equalsIgnoreCase(varId.trim())) {
                                            Log.d(TAG, "Match! " + v.getId());
                                            View gView = myVars.get(v).view;
                                            gView.setVisibility(mode ? View.VISIBLE
                                                    : View.GONE);
                                            if (gView instanceof LinearLayout) {
                                                EditText et = gView
                                                        .findViewById(R.id.edit);
                                                if (et != null && mode == HIDE) {
                                                    Log.e("nils",
                                                            "Setting view text to empty for "
                                                                    + v.getId());
                                                    et.setText("");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {

                        }

                    });

                    break;
                case text:
                    Log.d(TAG, "Adding text field for dy-variable with label "
                            + label + ", name " + varId + ", type "
                            + var.getType().name());
                    View l = LayoutInflater.from(myContext.getContext()).inflate(
                            R.layout.edit_field_text, null);
                    header = l.findViewById(R.id.header);

                    header.setText(varLabel + " " + unit
                            + (hist != null ? " (" + hist + ")" : ""));
                    innerInputContainer.addView(l);
                    varV.view=l;
                    break;
                case numeric:
                case decimal:

                    // o.addText("Adding edit field for dy-variable with label "+label+", name "+varId+", type "+numType.name()+" and unit "+unit.name());
                    if (var.getType() == DataType.numeric) {
                        if (varV.format != null && varV.format.equals("slider")) {
                            l = LayoutInflater.from(myContext.getContext()).inflate(
                                    R.layout.edit_field_slider, null);
                            SeekBar sb = l.findViewById(R.id.seekbar);
                            final EditText et = l.findViewById(R.id.edit);
                            et.setKeyListener(null);
                            String value = var.getValue();
                            //Initiate seekbar to variable value if any.
                            if (value != null)
                                sb.setProgress(Integer.parseInt(value));
                            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    et.setText(Integer.toString(progress));
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {

                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                    Log.d(TAG, "hepp!");
                                }
                            });
                        } else {
                            l = LayoutInflater.from(myContext.getContext()).inflate(
                                    R.layout.edit_field_numeric, null);
                        }
                    } else
                        l = LayoutInflater.from(myContext.getContext()).inflate(
                                R.layout.edit_field_float, null);
                    header = l.findViewById(R.id.header);

                    String headerTxt = varLabel
                            + ((unit != null && unit.length() > 0) ? " (" + unit + ")"
                            : "");
                    if (hist != null && varV.showHistorical) {
                        SpannableString s = new SpannableString(headerTxt + " (" + hist
                                + ")");
                        s.setSpan(new TextAppearanceSpan(gs.getContext(),
                                R.style.PurpleStyle), headerTxt.length() + 2, s
                                .length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        header.setText(s);
                    } else
                        header.setText(headerTxt);

			/*
			 * String limitDesc =
			 * al.getLimitDescription(var.getBackingDataSet()); if
			 * (limitDesc!=null&&limitDesc.length()>0) { EditText etNum =
			 * (EditText)l.findViewById(R.id.edit); CombinedRangeAndListFilter
			 * filter =
			 * FilterFactory.getInstance().createLimitFilter(var,limitDesc);
			 * etNum.setFilters(new InputFilter[] {filter}); }
			 */
                    // ruleExecutor.parseFormulas(al.getDynamicLimitExpression(var.getBackingDataSet()),var.getId());
                    innerInputContainer.addView(l);
                    varV.view=l;
                    break;
                case auto_increment:
                    Log.d(TAG, "Adding AUTO_INCREMENT variable " + varLabel);
                    l = LayoutInflater.from(myContext.getContext()).inflate(
                            R.layout.edit_field_numeric, null);
                    header = l.findViewById(R.id.header);
                    header.setText(varLabel);
                    @SuppressLint("CutPasteId") EditText etNum = l.findViewById(R.id.edit);
                    etNum.setFocusable(false);
                    innerInputContainer.addView(l);
                    varV.view=l;
                    break;
            }
            if (!varV.isVisible)
                myVars.get(var).view.setVisibility(View.GONE);
            //next variable.
            vc++;
        }
    }
    // @Override
    void refreshInputFields() {
        DataType numType;
        Log.d(TAG, "In refreshinputfields");

        Set<Entry<Variable, VariableView>> vars = myVars.entrySet();
        for (Entry<Variable, VariableView> entry : vars) {
            Variable variable = entry.getKey();
            String value = variable.getValue();
            Log.d(TAG, "Variable: " + variable.getLabel() + " value: "
                    + variable.getValue());
            numType = variable.getType();

            View v = entry.getValue().view;

            if (numType == DataType.bool) {
                RadioButton ja = v.findViewById(R.id.ja);
                RadioButton nej = v.findViewById(R.id.nej);
                if (value != null) {
                    if (value.equals("true"))
                        ja.setChecked(true);
                    else
                        nej.setChecked(true);
                }
            } else if (numType == DataType.numeric || numType == DataType.text) {

                // Log.d(TAG,"refreshing edittext with varid "+variable.getId());
                EditText et = v.findViewById(R.id.edit);
                CombinedRangeAndListFilter filter = variable.getLimitFilter();
                if (filter != null)
                    et.setFilters(new InputFilter[] { filter });

                TextView limit = v.findViewById(R.id.limit);
                CharSequence limiTxt = new SpannableString("");
                et.setTextColor(Color.BLACK);
                if (variable.isUsingDefault()) {
                    et.setTextColor(myContext.getContext().getResources()
                            .getColor(R.color.purple,myContext.getContext().getTheme()));
                } else
                    Log.d(TAG, "Variable " + variable.getId()
                            + " is NOT YELLOW");
                if (filter != null) {
                    if (variable.hasValueOutOfRange())
                        et.setTextColor(Color.RED);
                    limiTxt = TextUtils.concat(limiTxt, filter.prettyPrint());
                }
                et.setTextColor(Color.BLACK);
				/*
				 * CharSequence ruleExec =
				 * ruleExecutor.getRuleExecutionAsString(
				 * variable.getRuleState()); if (ruleExec!=null) { limiTxt =
				 * TextUtils.concat(limiTxt,ruleExec); if
				 * (variable.hasBrokenRules()) et.setTextColor(Color.RED); }
				 */
                limit.setText(limiTxt);
                et.setText(value == null ? "" : value);
                int position = et.getText().length();
                Selection.setSelection(et.getEditableText(), position);

            } else if (numType == DataType.list) {
                // this is the spinner.
                final Spinner sp = v.findViewById(R.id.spinner);

                final Handler h = new Handler();
                if (firstSpinner != null)
                    new Thread(new Runnable() {
                        public void run() {

                            h.postDelayed(new Runnable() {
                                public void run() {
                                    // Open the Spinner...
                                    if (firstSpinner.isShown())
                                        firstSpinner.performClick();
                                }
                            }, 500);
                        }
                    }).start();

                String[] opt = null;
                String tag = (String) sp.getTag(R.string.u1);
                Log.d(TAG,"TAG IS "+tag);
                String val[] = values.get(variable);
                if (val != null) {

                    for (int i = 0; i < val.length; i++) {
                        if (val[i].equals(variable.getValue()))
                            sp.setSelection(i);
                    }
                }

                else if (tag != null && tag.equals("dynamic")) {
                    // Get the list values
                    opt = Tools.generateList(variable);

                    // Add dropdown.
                    if (opt == null)
                        Log.e("nils", "OPT IS STILL NULL!!!");
                    else {

                        ((ArrayAdapter<String>) sp.getAdapter()).clear();
                        ((ArrayAdapter<String>) sp.getAdapter()).addAll(opt);
                        String item = null;
                        if (sp.getAdapter().getCount() > 0) {
                            if (value != null) {
                                for (int i = 0; i < sp.getAdapter().getCount(); i++) {
                                    item = (String) sp.getAdapter().getItem(i);
                                    if (item!=null && item.equals(value)) {
                                        sp.setSelection(i);
                                    }
                                }
                            }
                        } else {
                            o.addText("");
                            o.addCriticalText("Empty spinner for variable " + v
                                    + ". Check your variable configuration.");
                        }
                    }
                }

            } else if (numType == DataType.auto_increment) {
                EditText et = v.findViewById(R.id.edit);
                et.setText(value == null ? "0" : value);
            }

        }
    }



    /*
     * private CombinedRangeAndListFilter getFilter(EditText et) { InputFilter[]
     * tmp = et.getFilters(); return
     * tmp.length==0?null:(CombinedRangeAndListFilter)tmp[0]; }
     */
    void setAutoOpenSpinner(boolean open) {
        autoOpenSpinner = open;
    }


    public void attachRule(Rule r) {
        if (myRules == null)
            myRules = new ArrayList<>();
        myRules.add(r);
        Log.d(TAG,"Added rule "+r.getCondition());
    }

}
