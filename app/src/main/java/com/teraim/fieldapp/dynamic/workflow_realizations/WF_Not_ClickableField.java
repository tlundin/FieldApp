package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.utils.CombinedRangeAndListFilter;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class WF_Not_ClickableField extends WF_ListEntry {

	private final int textColorC;
	final LinearLayout outputContainer;
	private final DisplayFieldBlock displayFieldFormat;

	final WF_Context myContext;
	String myDescription;
	private boolean showAuthor  = false;
	final Map<Variable,OutC> myOutputFields = new HashMap<Variable,OutC>();

	//Hack! Used to determine what is the master key for this type of element.
	//If DisplayOut & Virgin --> This is master key.
	boolean virgin=true;
	//Removed myVar 2.07.15
	//protected Variable myVar;
	protected abstract LinearLayout getFieldLayout();

    private String entryFieldAuthor = null;
	int backgroundColor=Color.TRANSPARENT;

	//	public abstract String getFormattedText(Variable varId, String value);


	@Override
	public Set<Variable> getAssociatedVariables() {
		Set<Variable> s = new HashSet<Variable>();
		s.add(myVar);
		return s;
	}

	class OutC {
		OutC(LinearLayout ll, String f) {
			view = ll;
			format = f;
		}
		OutC() {}

        LinearLayout view;
		String format;
	}

	class OutSpin extends OutC {
		final String[] opt;
        final String[] val;

		OutSpin(LinearLayout ll, String[] opt, String[] val) {
			this.view = ll;
			this.opt=opt;
			this.val=val;
		}

	}


	@SuppressWarnings("WrongConstant")
    WF_Not_ClickableField(String id, final String label, final String descriptionT, WF_Context myContext,
                          View view, boolean isVisible, DisplayFieldBlock format) {
		super(id,view,myContext,isVisible);


		this.myContext = myContext;
        TextView myHeader = getWidget().findViewById(R.id.editfieldtext);
		outputContainer = getWidget().findViewById(R.id.outputContainer);
		//outputContainer.setLayoutParams(params);
		//Log.d("taxx","variable label: "+label+" variable ID: "+id);
		textColorC = Tools.getColorResource(myContext.getContext(),format.getTextColor());
		//myheader can be null in case this is a Cell in a table.
		if (myHeader !=null) {
			myHeader.setTextColor(textColorC);
			myHeader.setText(label);
		}
		//change between horizontal and vertical


		this.label = label;
		myDescription = descriptionT;
		//Show owner.
		showAuthor = GlobalState.getInstance().getGlobalPreferences().getB(PersistenceHelper.SHOW_AUTHOR_KEY);
		if (format.getBackgroundColor()!=null) {
			backgroundColor = Tools.getColorResource(myContext.getContext(),format.getBackgroundColor());
			getWidget().setBackgroundColor(backgroundColor);
		}

		displayFieldFormat = format;

		//Log.d("vortex","setting background to "+this.backgroundColor);

		LinearLayout topElem = getWidget().findViewById(R.id.entryRoot);

		//ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams)bg.getLayoutParams());
		if (topElem!=null)
			topElem.setPadding(0,displayFieldFormat.getVerticalMargin(),0,0);


	}


	@Override
	public void postDraw() {
		View lastElem = getWidget().findViewById(R.id.lastElement);
		if (lastElem!=null) {
			lastElem.setPadding(0, 0, 0, displayFieldFormat.getVerticalMargin());
		}
	}


	public void addVariable(Variable var, boolean displayOut, String format, boolean isVisible) {

		if (displayOut && virgin) {
			virgin = false;
			super.setKey(var);
			myDescription=al.getDescription(var.getBackingDataSet());
		}

		if (displayOut) {
			LinearLayout ll = getFieldLayout();


			/*
			String value = Variable.getPrintedValue();
			if (!value.isEmpty()) {
				o.setText(varLabel+": "+value);	
				u.setText(" ("+Variable.getPrintedUnit()+")");
			}
			 */
			myOutputFields.put(var,new OutC(ll,format));
			outputContainer.addView(ll);

			//Log.d("franco","Added view "+var.getLabel()+" with width: "+ll.getWidth());
			myVar = var;
		}

	}


	void refreshOutputField(Variable variable, OutC outC) {


		LinearLayout ll = outC.view;
		TextView o = ll.findViewById(R.id.outputValueField);
		TextView u = ll.findViewById(R.id.outputUnitField);
		String value = variable.getValue();

		//Log.d("nils","In refreshoutputfield for variable "+variable.getId()+" with value "+variable.getValue());

		if (value!=null&&!value.isEmpty()) {
			ll.setVisibility(View.VISIBLE);
			CombinedRangeAndListFilter filter = variable.getLimitFilter();
			//if (filter!=null)
			//	filter.testRun();

			if (variable.hasBrokenRules()||variable.hasValueOutOfRange()) {
				Log.d("nils","VARID: "+variable.getId()+" hasBroken: "+variable.hasBrokenRules()+" hasoutofRange: "+variable.hasValueOutOfRange());
				o.setTextColor(Color.RED);
				u.setTextColor(Color.RED);
			} else {
				if (variable.isUsingDefault()) {
					Log.d("nils","Variable "+variable.getId()+" is purple");
					int purple = myContext.getContext().getResources().getColor(R.color.purple,myContext.getContext().getTheme());
					o.setTextColor(purple);
					u.setTextColor(purple);
					//if (myHeader!=null)
					//	myHeader.setTextColor(purple);
				} else {
					o.setTextColor(textColorC);
					u.setTextColor(textColorC);
				}
			}
			String outS="";

			if (variable.getType() != Variable.DataType.bool) {

				if (outC instanceof OutSpin) {
					Log.d("bort","gets here. "+ Arrays.toString(((OutSpin) outC).opt));
					outS = value;
					OutSpin os = ((OutSpin)outC);
					if (os.opt!=null && os.val!=null)						
						for (int i=0;i<os.val.length;i++)
							if (os.val[i].equals(value)) {
								outS = os.opt[i];
								break;
							}
				} else
					outS = getFormattedText(value,outC.format);
			} 
			//boolean..use yes or no.
			else {
				if (value.length()>0) {
					if(value.equals("false"))
						outS=myContext.getContext().getString(R.string.no);
					else if (value.equals("true"))
						outS=myContext.getContext().getString(R.string.yes);
//					Log.e("vortex","VARIABELVÄRDE: "+value);
				}
			}
			o.setText(outS);	
			u.setText(variable.getPrintedUnit());				
		}
		else {
			if (shouldHideOutputView())
				ll.setVisibility(View.GONE);
			o.setText("");
			u.setText("");
		}	

		if (showAuthor)
			setBackgroundColor(variable);
	}

	protected abstract boolean shouldHideOutputView();

	private enum Role {None,Mix,Master,Slave}


    private void setBackgroundColor(Variable var) {
		GlobalState gs = GlobalState.getInstance();


		String author = var.getWhoGaveThisValue();
		Role role = Role.None;			
		//Log.d("vortex","author var: "+author+" entryfield owner: "+entryFieldAuthor);

		if (author!=null) {
			boolean IdidIt = author.equals(gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY));
			if (IdidIt && gs.isMaster() || !IdidIt && gs.isSlave())
				role = Role.Master;
			else
				role = Role.Slave;


			if (entryFieldAuthor != null && !author.equals(entryFieldAuthor))
				role = Role.Mix;
			else
				entryFieldAuthor = author;



			int color=0;
			switch (role) {
			case Mix:
				color = R.color.mixed_owner_bg;
				break;				
			case Master:
				color = R.color.master_owner_bg;
				break;
			case Slave:
				color = R.color.client_owner_bg;
				break;
			case None:
				//Log.e("vortex","no color assigned");
				break;

			}
			//Log.e("vortex","Color of entryfield now "+role.name());
			if (color!=0) {
				Context ctx = GlobalState.getInstance().getContext();
				getWidget().setBackgroundColor(ctx.getResources().getColor(color, ctx.getTheme()));
			}
		}

	}

	@Override
	public void refresh() {
		//Log.d("nils","refreshoutput called on "+myHeader);
		Iterator<Map.Entry<Variable,OutC>> it = myOutputFields.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Variable,OutC> pairs = it.next();
			//Log.d("nils","Iterator has found "+pairs.getKey()+" "+pairs.getValue());
			refreshOutputField(pairs.getKey(),pairs.getValue());
		}	
	}



	public static String getFormattedText(String value, String format) {
		int lf=0,rf=0;
		boolean hasFormat = false, hasDot = false;
		if (value!=null&&value.length()>0) {
			if (format!=null) {
				if (format.contains(".")) {
					hasDot = true;
					String[] p = format.split("\\.");
					if (p!=null && p.length==2) {
						lf = p[0].length();
						rf = p[1].length();
					} 
				} else
					lf = format.length();
				//hasformat true if lf or rf is not 0 length.
				hasFormat = (lf!=0||rf!=0);
			}

			if (hasFormat) {

				if (hasDot) {
					if (!value.contains(".")) {
						value += ".0";
					}
					String[] p = value.split("\\.");
					if (p!=null && p.length==2) {
						String Rf = p[1];
						if (Rf.length()>rf) 
							Rf = p[1].substring(0, rf);					
						if (Rf.length()<rf)
							Rf = addZeros(Rf,rf-Rf.length());
						String Lf = p[0];
						if (Lf.length()>lf) 
							Lf = p[0].substring(0,lf);					
						if (Lf.length()<lf)
							Lf = addSpaces(Lf,lf-Lf.length());
						value = Lf+"."+Rf;
					}		
				} else {
					if(value.contains(".")) {
						String p[]  = value.split("\\.");
						value = p[0];
					}
					if (value.length()<lf) 
						value = addSpaces(value,lf-value.length());

				}

			}
		}
		return value;
	}

	private static String addZeros(String s,int i) {
		while (i-->0)
			s=s+"0";
		return s;
	}
	private static String addSpaces(String s,int i) {
		while (i-->0)
			s=" "+s;
		return s;
	}


}
