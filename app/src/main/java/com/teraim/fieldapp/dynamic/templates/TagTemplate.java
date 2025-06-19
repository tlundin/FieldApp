package com.teraim.fieldapp.dynamic.templates;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Delyta;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.non_generics.DelyteManager.Coord;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.ui.ProvytaView;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;




public class TagTemplate extends Executor implements EventListener, OnGesturePerformedListener {


	private static final int MAX_TAG = 5,MAX_DELPUNKTER =6;

	private static final int COLS=MAX_DELPUNKTER;

	private GestureLibrary gestureLib;
	private LinearLayout gl;
	private ProvytaView pyv;
	private LayoutInflater inflater;
	private DelyteManager dym;
    private Button sparaB;
    private Button calculateB;
	private LinearLayout areaL;


	@Override
	protected List<WF_Container> getContainers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean execute(String function, String target) {

		return true;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		View v = inflater.inflate(R.layout.template_tag, container, false);	

		final FrameLayout py = v.findViewById(R.id.circle);
		gl = v.findViewById(R.id.tagLayout);

		areaL = v.findViewById(R.id.areaL);

		dym = DelyteManager.getInstance();

		pyv = new ProvytaView(this.getActivity(), null, Constants.isAbo(dym.getPyID()));		

		py.addView(pyv);

        VariableConfiguration al = gs.getVariableConfiguration();
		//Get all variables from group "delningstag".

		//Delyta equals variable name..eg. T�g1 = Delyta 1 asf.

		//tableElements[i][index]=ar+","+s;
		this.inflater = inflater;



		//drawEmptyTable();

		fillTable();

		pyv.showDelytor(dym.getDelytor(),false);



		//					final EditText avst = (EditText)l.findViewById(R.id.avst);
		//					final EditText rikt = (EditText)l.findViewById(R.id.rikt);	

		/*
		int[][] t = { 

				{100,233,0,360,64,322,100,47},
				{100,288,100,48,100,120,100,263},
				{100,48,100,288},
				{100,233,57,180,100,143},
				{100,29,75,336,100,320},
				{100,261,100,36,100,98,100,200},
				{100,219,100,116},
				{100,116,100,30},
				{100,30,100,270}	

		};
		 */
		/*	
		Button[] button = new Button[5];
		for (int i=0;i<5;i++) {
			button[i]=(Button)v.findViewById(buttonIds[i]);
			final int c = i;
			button[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pyv.showDelytor(dy.get(c));
				}
			});
		}

		 */

//		calculateB = (Button)v.findViewById(R.id.redraw);
//		calculateB.setEnabled(false);
        Button nyUtlaggB = v.findViewById(R.id.rensa);
		nyUtlaggB.setEnabled(gs.isMaster()||gs.isSolo());
		sparaB = v.findViewById(R.id.spara);
		sparaB.setEnabled(false);
		gl.setEnabled(false);
/*
		calculateB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				createDelytorFromTable();
				dym.analyze();
				TagTemplate.this.updateAreaField();
				pyv.showDelytor(dym.getDelytor(),false);
				sparaB.setEnabled(true);
			}


		});
*/
		nyUtlaggB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {

				final String rutaId = varCache.getVariableValue(null, "Current_Ruta");
				final String provytaId = varCache.getVariableValue(null, "Current_Provyta");
				new AlertDialog.Builder(TagTemplate.this.getActivity())
				.setTitle("Nyutlägg - Varning")
				.setMessage("Nytt utlägg av provyta görs om tillståndet på provytan har förändrats så att en ny provytedelning måste göras, eller om man inte hittar provytecentrum.\nAlla delytor under ruta/provyta ["+rutaId+":"+provytaId+"] kommer suddas. Är du säker?")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						drawEmptyTable();
						gs.getDb().eraseDelytor(rutaId,provytaId);	


						//Mark as nyutlägg.
						Variable nyUtlagg = varCache.getVariable(gs.getVariableConfiguration().createProvytaKeyMap(), NamedVariables.NYUTLAGG);
						nyUtlagg.setValue("1");
						//init.
						dym.clear();
						dym.init();
						//show default
						pyv.showDelytor(dym.getDelytor(),false);
						//nyUtlaggB.setEnabled(false);
						//calculateB.setEnabled(true);
						gl.setEnabled(true);
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// do nothing
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();

			}
		});
		sparaB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(TagTemplate.this.getActivity())
				.setTitle("Spara")
				.setMessage("Det här sparar alla förändringar permanent. Är du säker?")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						if(save()) {
							//Trigger bluetooth transfer.
							gs.sendEvent(MenuActivity.SYNC_REQUIRED);
							myContext.getFragmentActivity().getFragmentManager().popBackStackImmediate();
						}
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						// do nothing
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
			}
		});


		GestureOverlayView gestureOverlayView = v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  


		//Display area if any delytor.
		if (dym.getDelytor().size()>0) {
			updateAreaField();
		} else
			Log.d("nils","NO DELYTOR WHEN TIME TO DRAW AREA");


		return v;

	}



	/*
	private final OnTouchListener hav = new OnTouchListener() {
	    @Override
	    public boolean onTouch(View v, MotionEvent event) {
	        if(MotionEvent.ACTION_UP == event.getAction())
				if (!Tools.isNumeric(((EditText)v).getText().toString()))
					((EditText)v).setText("");
	        return false;
	    }
	};
	 */

	@SuppressLint("DefaultLocale")
	private void updateAreaField() {

		areaL.removeAllViews();
		Map<Integer,Float> aMap = new HashMap<Integer,Float>();
		float a;
		for (Delyta d:dym.getDelytor()) {			
			a=d.getArea();
			Log.d("nils","Area for dy "+d.getId()+" is "+a);
			if (a==0) 
				Log.e("nils","AREA WAS 0 in updateAreaField for DY "+d.getId());
			else {
				Float area = aMap.get(d.getId());
				if (area==null)
					area = a;
				else 
					area +=a;
				aMap.put(d.getId(), area);
			}
		}
		Set<Integer> keys = aMap.keySet();
		LinearLayout el;
		TextView h,tv;

		for (Integer id:keys) {				
			el = (LinearLayout)inflater.inflate(R.layout.area_elem, null);
			h = el.findViewById(R.id.header);
			tv= el.findViewById(R.id.value);
			h.setText("DY "+id+":");
			tv.setText((String.format("%.2f", (aMap.get(id)/100f)))+"\u33A1");
			Log.d("nils","AREA added for delyta "+id+": "+aMap.get(id));
			areaL.addView(el);
		}

		Log.d("nils","exiting area calc");

	}



	private final OnFocusChangeListener hav = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if(hasFocus)
				if (!Tools.isNumeric(((EditText)v).getText().toString()))
					((EditText)v).setText("");
		}
	};



	private void drawEmptyTable() {
		Log.d("nils","In drawEmptyTable");
		gl.removeAllViews();
		TextView header; LinearLayout l,tagRow;
		for (int j = 1; j<= MAX_TAG; j++) {
			tagRow = (LinearLayout)inflater.inflate(R.layout.tag_table_row, null);
			header = tagRow.findViewById(R.id.tagHeader);
			header.setText("Tåg "+j);
			gl.addView(tagRow);
			final TextView tagTextView = tagRow.findViewById(R.id.tagBody);
			tagTextView.setText("Lägg till tåg");
			final int row = j;
			tagRow.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {					
					openEditor(row,tagTextView);
				}


			});

			//Fill row.
			/*
			for (int i=0;i<COLS;i++)	{
				final int ii = i;final int jj = j;
				l = (LinearLayout)inflater.inflate(R.layout.edit_fields_tag, null);
				tagRow.addView(l);
				final EditText avst = (EditText)l.findViewById(R.id.avst);
				final EditText rikt = (EditText)l.findViewById(R.id.rikt);
				avst.setText("avs");
				rikt.setText("rik");

				avst.setOnFocusChangeListener(hav);
				rikt.setOnFocusChangeListener(hav);
				avst.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						avst.setText("avs");
						return true;
					}
				});
				rikt.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						rikt.setText("rik");
						return true;
					}
				});	
				/*
					rikt.setOnKeyListener(new OnKeyListener() {
						@Override
						public boolean onKey(View v, int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_ENTER) {
								Log.d("nils","GETS A TAB OR ENTER");
								LinearLayout l2 = (LinearLayout)(gl.getChildAt(ii+(jj%COLS+1)*COLS));
								EditText avst = ((EditText)(l2).findViewById(R.id.avst));
								if(!Tools.isNumeric(avst.getText().toString()))
									avst.setText("");
								return true;
							}
							return false;
						}
					});
			 */





		}


	}
	private EditText focusedView = null;

	private void openEditor(int row, final TextView tagTextView) {
		//check if there is data for this one.
		final String rawTag = tagTextView.getText().toString();
		//Open popup.
		View popUpView = inflater.inflate(R.layout.tag_edit_popup_with_rows, null);
		final PopupWindow mpopup = new PopupWindow(popUpView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true); //Creation of popup
		mpopup.setAnimationStyle(android.R.style.Animation_Dialog);   
		Button avbrytB = popUpView.findViewById(R.id.avbrytB);
		Button sparaLB = popUpView.findViewById(R.id.sparaB);
		Button hundraB = popUpView.findViewById(R.id.hundraB);
		Button rensaB = popUpView.findViewById(R.id.rensaB);
		final EditText[] ETA = new EditText[MAX_DELPUNKTER];
		final EditText[] ETR = new EditText[MAX_DELPUNKTER];
		ETA[0] = popUpView.findViewById(R.id.ETA1);
		ETA[1] = popUpView.findViewById(R.id.ETA2);
		ETA[2] = popUpView.findViewById(R.id.ETA3);
		ETA[3] = popUpView.findViewById(R.id.ETA4);
		ETA[4] = popUpView.findViewById(R.id.ETA5);
		ETA[5] = popUpView.findViewById(R.id.ETA6);
		ETR[0] = popUpView.findViewById(R.id.ETR1);
		ETR[1] = popUpView.findViewById(R.id.ETR2);
		ETR[2] = popUpView.findViewById(R.id.ETR3);
		ETR[3] = popUpView.findViewById(R.id.ETR4);
		ETR[4] = popUpView.findViewById(R.id.ETR5);
		ETR[5] = popUpView.findViewById(R.id.ETR6);

		

		
		final OnFocusChangeListener focusListener = new OnFocusChangeListener(){
			      public void onFocusChange(View v, boolean hasFocus){
			                if(hasFocus){
			                focusedView = (EditText)v;
			              } else {
			                  focusedView  = null;
			            }
			        }
			 };
		for (EditText eta: ETA){
			   eta.setOnFocusChangeListener(focusListener);
			}

        hundraB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (focusedView!=null)
					focusedView.setText("100");
			}
		});
		/*
		final EditText tagE = (EditText)popUpView.findViewById(R.id.tagE);
		avbryt.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				mpopup.dismiss();
				tagTextView.setText(origText);
			}
		})
		*/
		InputFilter rFilter = new InputFilter() {
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

			// Remove the string out of destination that is to be replaced
			String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
			// Add the new string in
			newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
			if (isLegal(newVal))
				return null;
			return "";
			}
		
		boolean isLegal(String newVal) {
			if (newVal.length()==0)
				return false;
			try {
				int input = Integer.parseInt(newVal.trim());
				return (input<=360);
			} catch (NumberFormatException nfe) {Log.d("nils","this is no number"); }
			return false;

		}
		};
		InputFilter aFilter = new InputFilter() {
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

			// Remove the string out of destination that is to be replaced
			String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
			// Add the new string in
			newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
			if (isLegal(newVal))
				return null;
			return "";
			}
		
		boolean isLegal(String newVal) {
			if (newVal.length()==0)
				return false;
			try {
				int input = Integer.parseInt(newVal.trim());
				return (input<=100);
			} catch (NumberFormatException nfe) {Log.d("nils","this is no number"); }
			return false;

		}
		};
		
		
		
		for (int i = 0; i<6;i++) {
			ETA[i].setFilters(new InputFilter[] { aFilter });
			ETR[i].setFilters(new InputFilter[] { rFilter });
		}
		avbrytB.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mpopup.dismiss();				
			}
		});
		
		sparaLB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String res="";
				for (int i = 0; i<MAX_DELPUNKTER;i++) {
					Editable a = ETA[i].getText();
					Editable r = ETR[i].getText();
					if (a==null||r==null)
						break;
					if (a.toString().isEmpty()||r.toString().isEmpty())
						break;
					res+=a.toString()+","+r.toString()+",";
				}
				if (!res.isEmpty())
					res = res.substring(0, res.length()-1);
				tagTextView.setText(res);
				Log.d("vortex","SETTTING TEXT TO "+res);
				createDelytorFromTable();
				dym.analyze();
				TagTemplate.this.updateAreaField();
				pyv.showDelytor(dym.getDelytor(),false);
				sparaB.setEnabled(true);
				mpopup.dismiss();
			}
		});
		final TextView headerTxt = popUpView.findViewById(R.id.headerT);
		final TextView subHeaderTxt = popUpView.findViewById(R.id.subHeaderT);

		rensaB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 0; i<MAX_DELPUNKTER;i++) {
					ETA[i].setText("");
					ETR[i].setText("");
				}
				subHeaderTxt.setText("");
			}
		});
		if (rawTag!=null&&!rawTag.isEmpty()&&!rawTag.startsWith("L")) {
			headerTxt.setText("Ändra tåg "+row);
			int i=0;			
			String[] rawTA = rawTag.split(",");
			Log.d("vortex","rawTA has "+rawTA.length+" elements" );
				boolean avst=true;
				for (String s:rawTA) {
					if (avst) {
						ETA[i].setText(s);
					} else {
						ETR[i].setText(s);
						i++;
					}
					avst=!avst;
						
				}
			subHeaderTxt.setText(rawTag);

		}

		else
			headerTxt.setText("Skapa nytt tåg");
		
		mpopup.showAtLocation(popUpView, Gravity.TOP, 0, 0);
	}





	private final List<Delyta> delytor = new ArrayList<Delyta>();


	private void fillTable() {
		Log.d("nils","In fillTable");
		gl.removeAllViews();
		delytor.clear();
		String tag="";
		int row =0; 
		for (Delyta dy:dym.getDelytor()) {	
			Log.d("vortex","row is "+row+" Tåg: "+dy.getTagPrettyPrint());
			if (!dy.isBackground()) {
				delytor.add(dy);
				tag = dy.getTagCommaPrint();
				LinearLayout tagView = (LinearLayout)gl.getChildAt(row);
				if (tagView == null) {
					tagView = (LinearLayout)inflater.inflate(R.layout.tag_table_row, null);
					gl.addView(tagView);
				} 
				TextView header = tagView.findViewById(R.id.tagHeader);
				row++;
				header.setText("Tåg "+row);
				String errorTxt="";
				if (errorArray[row-1]!=null) {
					((TextView)tagView.findViewById(R.id.tagBody)).setTextColor(Color.RED);
					errorTxt = "["+errorArray[row-1]+"]";
				}  else
					((TextView)tagView.findViewById(R.id.tagBody)).setTextColor(gs.getContext().getResources().getColor(R.color.blue_background,gs.getContext().getTheme()));
				((TextView)tagView.findViewById(R.id.tagBody)).setText(tag+errorTxt);

			}

		}
	}



	//	100|179|100|295|100|20|43|77|100|133
	/*
	private void createDelytorFromTable() {
		dym.clear();
		List<Coord> tc = new ArrayList<Coord>();
		tc.add(new Coord(100,179));
		tc.add(new Coord(100,295));
		tc.add(new Coord(100,20));
		tc.add(new Coord(43,77));
		tc.add(new Coord(100,133));
		dym.addUnknownTag(tc);


	}
	 */	 
	private final String[] errorArray = new String[MAX_TAG];
	
	private void createDelytorFromTable() {
		//Empty existing. 
		dym.clear();
		List<Coord> tagCoordinateList = new ArrayList<Coord>();
		for (int row = 0; row< MAX_TAG; row++) {
			errorArray[row]=null;
			LinearLayout tagView = (LinearLayout)gl.getChildAt(row);
			if (tagView == null) {
				Log.e("vortex","tagview was null for row "+row);
				break;
			}
			final TextView tagTextView = tagView.findViewById(R.id.tagBody);
			
			String tagT = tagTextView.getText().toString();
			if (tagT==null||tagT.length()==0) {
				tagTextView.setText("Lägg till tåg");
				Log.d("vortex","lenght 0 or null for row "+row);
				continue;
			}
			if (tagT.startsWith("L")) {
				Log.d("vortex","started with L!! "+row);
				continue;
			}
			//parse...find x,y
			String avst,rikt;
			int start = 0,co=0;
			boolean err=false;
			while (true) {
				co = tagT.indexOf(',', start);
				if (co==-1) {
					err=true;
					errorArray[row]=", syntax error (avst)";
					break;
				}
				avst = tagT.substring(start,co);
				Log.d("vortex","avst: "+avst);
				start = co+1;
				co = tagT.indexOf(',', start);
				if (co==-1) {
					rikt = tagT.substring(start,tagT.length());
					if (rikt.length()==0 && rikt.length()>3) {
						err=true;
						errorArray[row]=", syntax error (rikt)";
						break;
					}
					else
						tagCoordinateList.add(new Coord(Integer.parseInt(avst),Integer.parseInt(rikt)));
					break;
				}
				rikt = tagT.substring(start,co);
				Log.d("vortex","rikt: "+rikt);
				start = co+1;
				tagCoordinateList.add(new Coord(Integer.parseInt(avst),Integer.parseInt(rikt)));
			}
			Log.d("vortex","tåg "+row+" ErrorCode "+errorArray[row]);
			
			if (tagCoordinateList.size()<=1) {
				err=true;
				errorArray[row]=", för kort!";
			}
				
				if(!err) {
				DelyteManager.ErrCode ec = dym.addUnknownTag(tagCoordinateList);
				if (ec == null||ec!=DelyteManager.ErrCode.ok) {
					err=true;
					errorArray[row]=", "+ec.name();
				}
			}
			if (errorArray[row]!=null) {
				tagTextView.setTextColor(Color.RED);
				tagTextView.setText(tagT+errorArray[row]);
			} else 
				tagTextView.setTextColor(gs.getContext().getResources().getColor(R.color.blue_background,gs.getContext().getTheme()));
			tagCoordinateList.clear();
		}
		//Check if no delyta. In that case, add default.
		if (dym.getDelytor().isEmpty()) {
			Log.d("nils","Adding default in createdelytorfromtable");
			Toast.makeText(getActivity(), "Inga tåg satta", Toast.LENGTH_LONG).show();
			dym.addDefault();
		}
	}


	/*
private void drawTrains() {
	List<Delyta> delytor=new ArrayList<Delyta>();
	//Create train array.
	for (int i=1;i<numberOfColumns+1;i++) {
		int index = 0;
		List<Coord> coords = new ArrayList<Coord>();
		for (int j=1;j<MAX_ROWS+1;j++) {
			LinearLayout ll = (LinearLayout)(gl.getChildAt(j*(numberOfColumns+1)+i));
			EditText avst = ((EditText)(ll).findViewById(R.id.avst));
			EditText rikt = ((EditText)(ll).findViewById(R.id.rikt));
			String avS = avst.getText().toString();
			String riS = rikt.getText().toString();
			//If one of the values are empty - tag ends.
			if (empty(avS)||empty(riS)) {
				if (coords.size()>1) {
					Delyta delyta = new Delyta();
					ErrCode e = delyta.create(coords);
					if (e == ErrCode.ok) {
						delytor.add(delyta);

					} else 
						Log.e("nils","Couldnt create delyta: "+e.name());
				}
				break;
			}
			else {	
				coords.add(new Coord(Integer.parseInt(avS),Integer.parseInt(riS)));
				//t[i-1][index++] = Integer.parseInt(avS);
				//t[i-1][index++] = Integer.parseInt(riS);
			}
		}
	}


	//Draw.





}
	 */
	private boolean empty(String s) {
		return s==null||s.length()==0;
	}



	/*

		� F�rsta och sista punkten m�ste ligga p� cirkelprovytans periferi.	 
		� Delningspunkterna m�ste beskrivas medurs. 
		� F�rsta linjen i t�get f�r ej vara en cirkelb�ge. 
		� Om tv� delningspunkter mellan f�rsta och sista brytpunkt ligger p� periferin m�ste 
		  linjen mellan dem vara en cirkelb�ge. I annat fall m�ste en av punkterna flyttas in 
		  mot centrum 1 dm, s� att avst�ndet till punkten ej �r lika med ytradien. 
		� Antalet delningspunkter f�r vara h�gst 6 per delningst�g. 
		� Provytan f�r delas i h�gst 5 delar
	 */

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5 && prediction.name.equals("right")) {
				if (dym.hasUnsavedChanges()) {
					new AlertDialog.Builder(TagTemplate.this.getActivity())
					.setTitle("Vill du spara?")
					.setMessage("Du har gjort ändringar till tåget. Vill du spara dessa före du går iväg?")
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							if (save())
								getFragmentManager().popBackStackImmediate();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							getFragmentManager().popBackStackImmediate();
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				} else
					getFragmentManager().popBackStackImmediate();
			}
		}
	}

	@Override
	public void onEvent(Event e) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "TAGTEMPLATE ";
	}

	private boolean save() {
		if (!dym.save()) {
			new AlertDialog.Builder(TagTemplate.this.getActivity())
			.setTitle("Fel")
			.setMessage("Det finns fel i dina tåg. Symptomet är att åtminstone en småprovyta ligger utanför alla delytor. Om du anser att dina tåg är korrekta, rapportera bugg.")
			.setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { 

				}
			})			
			.setIcon(android.R.drawable.ic_dialog_alert)
			.show();


			return false;
		}
		return true;
	}




}
