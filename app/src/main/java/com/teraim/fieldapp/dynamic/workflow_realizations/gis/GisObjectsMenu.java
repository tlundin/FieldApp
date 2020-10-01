package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.TabButton;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisPolyType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.Shape;
import com.teraim.fieldapp.gis.GisImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A view for showing buttons, selecting GPS menu objects
 *
 * @author Terje
 *
 */

public class GisObjectsMenu extends View {
	private static int PaddingX,PaddingY,InnerPadding,spacingAroundTabs;
	private static int NoOfButtonsPerRow;
	private static final int MAX_ROWS = 5;
	private static int SpaceBetweenHeaderAndButton;
	private static int scale;
	private  Map<String,LinkedHashMap<GisPolyType,Set<FullGisObjectConfiguration>>> myPalettes;
	private Paint headerTextP;
    private Paint tabTextP;
    private GisImageView myGis;
	private Paint thinBlackEdgeP;
	private Paint blackTextP;


	private MenuButton oldB = null;
    private int ColW;
	private int w=0;
    private Paint thinWhiteEdgeP;
	private Paint whiteTextP;
	private Paint tabEdgePaint,selectedTabPaint,notSelectedTabPaint,transparentPaint;
	private WF_Gis_Map myMap;
	//default palette
	private String currentPalette=null;
	public final static String Default = "default";


	public GisObjectsMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public GisObjectsMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public GisObjectsMenu(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		//Mypalettes contains the palettes used to show gis objects.
		myPalettes = new HashMap<String,LinkedHashMap<GisPolyType,Set<FullGisObjectConfiguration>>>();


		int large= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				18, getResources().getDisplayMetrics());
		int small= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				12, getResources().getDisplayMetrics());


		scale = (int)getResources().getDisplayMetrics().density;

		PaddingX = 10*scale;
		PaddingY=15*scale;
		InnerPadding = 5*scale;
		NoOfButtonsPerRow = 5;
		SpaceBetweenHeaderAndButton = 10*scale;
		spacingAroundTabs = 4*scale;
		int scaledSize = getResources().getDimensionPixelSize(R.dimen.text_size_large);
		tabTextP = new Paint();
		tabTextP.setColor(Color.WHITE);
		tabTextP.setTextSize(scaledSize);

		Log.d("alfa","scale on device is "+scale);
		tabTextP.setStyle(Paint.Style.STROKE);
		tabTextP.setTextAlign(Paint.Align.CENTER);

		tabEdgePaint = new Paint();
		tabEdgePaint.setColor(context.getColor(R.color.primary_light));
		tabEdgePaint.setStyle(Paint.Style.STROKE);
		tabEdgePaint.setStrokeWidth(0);
		selectedTabPaint = new Paint();
		selectedTabPaint.setColor(context.getColor(R.color.primary_dark));
		notSelectedTabPaint = new Paint();
		notSelectedTabPaint.setColor(context.getColor(R.color.primary_light));
		//selectedTabPaint.setStrokeWidth(50);

		transparentPaint=new Paint();
		transparentPaint.setAlpha(0);

		float radius = scale*6f;
		CornerPathEffect corEffect = new CornerPathEffect(radius);
		tabEdgePaint.setPathEffect(corEffect);
		selectedTabPaint.setPathEffect(corEffect);
		notSelectedTabPaint.setPathEffect(corEffect);
		headerTextP = new Paint();
		headerTextP.setColor(Color.WHITE);
		headerTextP.setTextSize(scale*15);
		headerTextP.setStyle(Paint.Style.STROKE);
		headerTextP.setTextAlign(Paint.Align.CENTER);

		blackTextP = new Paint();
		blackTextP.setColor(Color.BLACK);
		blackTextP.setTextSize(scale*15);
		blackTextP.setStyle(Paint.Style.STROKE);
		blackTextP.setTextAlign(Paint.Align.CENTER);


		whiteTextP = new Paint();
		whiteTextP.setColor(Color.WHITE);
		whiteTextP.setTextSize(scale*15);
		whiteTextP.setStyle(Paint.Style.STROKE);
		whiteTextP.setTextAlign(Paint.Align.CENTER);

        Paint gopButtonBackgroundP = new Paint();
		gopButtonBackgroundP.setColor(Color.WHITE);
		gopButtonBackgroundP.setStyle(Paint.Style.FILL);

        Paint gopButtonBackgroundSP = new Paint();
		gopButtonBackgroundSP.setColor(Color.BLACK);
		gopButtonBackgroundSP.setStyle(Paint.Style.FILL);

        Paint gopButtonEdgeP = new Paint();
		gopButtonEdgeP.setColor(Color.BLACK);
		gopButtonEdgeP.setStyle(Paint.Style.STROKE);
		gopButtonEdgeP.setStrokeWidth(2*scale);

		thinBlackEdgeP = new Paint();
		thinBlackEdgeP.setColor(Color.BLACK);
		thinBlackEdgeP.setStyle(Paint.Style.STROKE);
		thinBlackEdgeP.setStrokeWidth(0);

		thinWhiteEdgeP = new Paint();
		thinWhiteEdgeP.setColor(Color.WHITE);
		thinWhiteEdgeP.setStyle(Paint.Style.STROKE);
		thinWhiteEdgeP.setStrokeWidth(0);



		this.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						Log.e("vortex"," evX: "+event.getX()+" NOBu: "+NoOfButtonsPerRow+" ColW: "+ColW);
						int clickedColumn = Math.round((event.getX()-(PaddingX + ColW/2))/ColW);
						if (clickedColumn<0||clickedColumn>=NoOfButtonsPerRow) {
							Log.d("vortex","click in column "+clickedColumn+". Outside allowed range");

						} else {
							Log.d("vortex","Clicked column: "+clickedColumn);
							//check for hit on all buttons in given column.
							for (int row = 0;row<MAX_ROWS;row++) {
								MenuButton currB = menuButtonArray[clickedColumn][row];
								if (currB!=null) {
									RectF r = currB.myRect;
									if (r.contains(event.getX(),event.getY())) {
										Log.e("vortex","CLICK HIT!!");
										currB.toggleSelected();
										oldB=currB;
										GisObjectsMenu.this.invalidate();
									}

								}
							}

						}
						break;
					case MotionEvent.ACTION_UP:
						if (tabButtonArray!=null) {
							for (TabButton tb : tabButtonArray) {
								if (tb.clickInside(event.getX(), event.getY())) {
									Log.d("vortex", "Click inside tab button: " + tb.fullText);
									currentPalette = tb.fullText;
									userSelectedPalette = tb.fullText;
									generateMenu();
									GisObjectsMenu.this.invalidate();
								}
							}
						}
						if (oldB!=null) {
							oldB.toggleSelected();
							GisObjectsMenu.this.invalidate();
							if (myMap!=null)
								myMap.startGisObjectCreation(oldB.myMenuItem);
							oldB=null;
						}

						v.performClick();
						break;
					default:
						break;
				}
				return true;
			}
		});

	}

	@Override
	public boolean performClick() {
		// Calls the super implementation, which generates an AccessibilityEvent
		// and calls the onClick() listener on the view, if any
		super.performClick();

		Log.e("vortex","In performclick, GisObjectsMenu");

		return true;
	}




	private class MenuButton {

		final RectF myRect;
		final FullGisObjectConfiguration myMenuItem;
		boolean isSelected=false;

		MenuButton(FullGisObjectConfiguration menuItem, RectF rf) {
			myRect = rf;
			myMenuItem=menuItem;
		}

		void toggleSelected() {
			isSelected=!isSelected;
		}


	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d("vortex","inSizeChange!");
		this.w=w;
		//First time called?
		if (oldw == 0)
			generateMenu();
		super.onSizeChanged(w, h, oldw, oldh);
	}

	//Before opening the menu, make sure all palettes are in place.
    private String userSelectedPalette = null;

	public void setMenuItems(Map<String,List<FullGisObjectConfiguration>> myMenuItems, GisImageView gis, WF_Gis_Map map) {
		//Create Menu items.
		myGis=gis;
		myMap=map;
		if (myMenuItems!=null && !myMenuItems.keySet().isEmpty()) {
			//Last entry is first in tab order
			String firstEntry=null;
			for (String key:myMenuItems.keySet()) {
				firstEntry=key;
			}
			//String firstEntry = myMenuItems.keySet().iterator().next();
			currentPalette= userSelectedPalette==null?firstEntry:userSelectedPalette;
			//Make sure userSelectedPalette is not from previous collection.
			if (!myMenuItems.keySet().contains(currentPalette))
				currentPalette=firstEntry;
			for (String paletteName : myMenuItems.keySet()) {
				List<FullGisObjectConfiguration> myMenuItemsForPalette = myMenuItems.get(paletteName);
				LinkedHashMap<GisPolyType, Set<FullGisObjectConfiguration>> menuGroupsM = myPalettes.get(paletteName);
				if (menuGroupsM == null) {
					Log.d("vortex", "creating new menugroup for palette " + paletteName);
					menuGroupsM = new LinkedHashMap<GisPolyType, Set<FullGisObjectConfiguration>>();
					myPalettes.put(paletteName, menuGroupsM);
				}
				//Sort the menuitems according to geojson type.
				for (FullGisObjectConfiguration item : myMenuItemsForPalette) {
					Set<FullGisObjectConfiguration> itemSet = menuGroupsM.get(item.getGisPolyType());
					if (itemSet == null) {
						itemSet = new LinkedHashSet<FullGisObjectConfiguration>();
						menuGroupsM.put(item.getGisPolyType(), itemSet);
					}
					Log.d("vortex", "Adding " + item.getName() + " to " + paletteName);
					itemSet.add(item);
				}
			}
		}
	}
	private final int Max_Tabs = 15;
	private List<TabButton> tabButtonArray;
	private MenuButton[][] menuButtonArray;


    private void generateMenu() {

		int col = 0;
		int row = 0;

		Log.d("vortex","In generateMenu ");
        int buttonWidth = (w - PaddingX * 2 - (InnerPadding * (NoOfButtonsPerRow - 1))) / NoOfButtonsPerRow;
        int rowH = InnerPadding + buttonWidth;
		ColW = rowH;

		//Padding needs to be greater than height of main header.
		int totalHeaderHeight = 0;

		int i=0;


		//marginal between buttons and between edge of button and text.
		int marginal = 15*scale;


        int tabRowHeight = 0;
        if (myPalettes.size()>0) {
			tabRowHeight = scale*30;
			int totalWidth=0,fulltextBoundsWidth=0;
			boolean aggressive = false;
			do {
				totalWidth = 0;
				tabButtonArray = new ArrayList<TabButton>();
				if (aggressive)
					Log.d("vortex","trying again with aggressive...");
				for (String tabText : myPalettes.keySet()) {
					//Draw header and tabs
					int calcWidthOfTabButton = 0, newEnd = tabText.length();
					String textOnButton = "";
					do {
						if (newEnd <= 0)
							break;
						else
							Log.d("vortex","newEnd: "+newEnd);

						textOnButton = tabText.substring(0, newEnd--);

						tabTextP.getTextBounds(textOnButton, 0, textOnButton.length(), textBounds);
						if (newEnd==(tabText.length()-1))
							fulltextBoundsWidth = textBounds.width();
						calcWidthOfTabButton = textBounds.width() + marginal;
						Log.d("Vortex", "calcW: "+calcWidthOfTabButton+"w_myps: "+w/myPalettes.size()+" w: "+w+" myPalS: "+myPalettes.size());
					} while (aggressive && ((calcWidthOfTabButton*myPalettes.size() + (marginal*(myPalettes.size()-1))) > w ));
					Rect tabButtonRect = new Rect(spacingAroundTabs*2+PaddingX + totalWidth, PaddingY, spacingAroundTabs*2+PaddingX + totalWidth + textBounds.width() + marginal, PaddingY + tabRowHeight);
					Rect fullRect = new Rect(spacingAroundTabs*2+PaddingX + totalWidth, PaddingY, spacingAroundTabs*2+PaddingX + totalWidth + fulltextBoundsWidth + marginal, PaddingY + tabRowHeight);
					totalWidth += calcWidthOfTabButton+marginal;
					tabButtonArray.add(new TabButton(textOnButton, tabButtonRect, fullRect,textBounds.exactCenterY(),tabText));
				}
				//If it fails, try again, this time with aggressive length setting.
				//Log.d("vorr","Aggressive: "+aggressive+" totalWidth: "+totalWidth+" w: "+w);
				if (aggressive)
					break;
				else
					aggressive = true;

			} while(totalWidth>w );
		} else
			tabRowHeight = 0;


		i=0;
		menuButtonArray= new MenuButton[NoOfButtonsPerRow][MAX_ROWS];
        String[] menuHeaderArray = new String[MAX_ROWS];
		//for (String paletteName:myPalettes.keySet()) {
		LinkedHashMap<GisPolyType, Set<FullGisObjectConfiguration>> menuGroupsM = myPalettes.get(currentPalette);
		Log.d("vortex","palette is "+currentPalette);
		for (GisPolyType type : menuGroupsM.keySet()) {
			Set<FullGisObjectConfiguration> itemSet = menuGroupsM.get(type);
			Iterator<FullGisObjectConfiguration> it = itemSet.iterator();

			menuHeaderArray[i++] = type.name();
			//
			while (it.hasNext()) {
				//Left padding + numer of buttons + number of spaces in between.
				FullGisObjectConfiguration fop = it.next();
				int left = col * ColW + PaddingX;
				int top = row * rowH + totalHeaderHeight+ tabRowHeight +PaddingY+PaddingX-(10*scale*row);
				RectF r = new RectF(left, top, left + buttonWidth, top + buttonWidth);
				menuButtonArray[col][row] = new MenuButton(fop, r);
				col++;
				if (col == NoOfButtonsPerRow) {
					col = 0;
					row++;
					i++;
					totalHeaderHeight += headerTextP.getTextSize() + SpaceBetweenHeaderAndButton;
				}

			}
			if (col!=0) {
				totalHeaderHeight += headerTextP.getTextSize() + SpaceBetweenHeaderAndButton;
				col = 0;
				row++;
			}

		}

	}

	private final Rect textBounds = new Rect();

	@Override
	protected void onDraw(Canvas canvas) {

		int totalHeaderHeight = 0;
		//int yFactor = (int)tabTextP.getTextSize()/2;
		//draw Tab Buttons if any

		if (tabButtonArray!=null) {
			TabButton selectedTabButton = null;
			for (TabButton tb : tabButtonArray) {
				boolean selected = tb.fullText.equals(currentPalette);
				canvas.drawRect(tb.r, transparentPaint);
				Path path = new Path();
				if (selected) {
					selectedTabButton = tb;

				} else {
					path.moveTo(tb.r.left - spacingAroundTabs, tb.r.top);
					path.lineTo(tb.r.left - spacingAroundTabs*2, tb.r.top + tb.r.height() + spacingAroundTabs*2);
					path.lineTo(tb.r.right + spacingAroundTabs, tb.r.top + tb.r.height() + spacingAroundTabs*2);
					path.lineTo(tb.r.right + spacingAroundTabs, tb.r.top);
					path.close();
					canvas.drawPath(path, notSelectedTabPaint);
					canvas.drawPath(path, tabEdgePaint);
					canvas.drawText(tb.getShortedText(), tb.r.exactCenterX(), tb.r.exactCenterY() - tb.centY, tabTextP);
				}

			}
			if (selectedTabButton != null) {
				Path path = new Path();
                path.moveTo(selectedTabButton.fr.left - spacingAroundTabs, selectedTabButton.fr.top - spacingAroundTabs*2);
				path.lineTo(selectedTabButton.fr.left - spacingAroundTabs*2, selectedTabButton.fr.top + selectedTabButton.r.height() + spacingAroundTabs);
				path.lineTo(PaddingX, selectedTabButton.fr.top + selectedTabButton.fr.height() + spacingAroundTabs);
				path.lineTo(PaddingX, getHeight() - PaddingY);
				path.lineTo(getWidth() - PaddingX, getHeight() - PaddingY);
				path.lineTo(getWidth() - PaddingX, selectedTabButton.fr.top + selectedTabButton.fr.height() + spacingAroundTabs);
				path.lineTo(selectedTabButton.fr.right + spacingAroundTabs*2, selectedTabButton.fr.top + selectedTabButton.fr.height() + spacingAroundTabs);
				path.lineTo(selectedTabButton.fr.right + spacingAroundTabs, selectedTabButton.fr.top - spacingAroundTabs*2);
				//path.lineTo(600, 300);
				//path.lineTo(400, 400);
				//path.lineTo(20, 400);
				path.close();
				canvas.drawPath(path, selectedTabPaint);
				canvas.drawPath(path, tabEdgePaint);

				canvas.drawText(selectedTabButton.fullText, selectedTabButton.fr.exactCenterX(), selectedTabButton.fr.exactCenterY() - selectedTabButton.centY-spacingAroundTabs, tabTextP);
			}
		}
		//Draw header and tabs
		for (int row = 0 ; row < MAX_ROWS; row++) {
			//if (menuHeaderArray[row]!=null)
			//	canvas.drawText(menuHeaderArray[row]+" types", w/2, row*RowH+PaddingY+totalHeaderHeight+ SpaceBetweenHeaderAndButton+tabRowHeight+headerTextP.getTextSize(), headerTextP);
			MenuButton currB=null;
			Rect bounds = new Rect();
			headerTextP.getTextBounds("a", 0, 1, bounds);
			for (int col = 0; col < NoOfButtonsPerRow;col++) {
				currB = menuButtonArray[col][row];
				if (currB == null) {
					//if first element is null in a row, we are done.
					if (col==0)
						return;
					else
						break;
				}
				RectF r = currB.myRect;
				FullGisObjectConfiguration fop = currB.myMenuItem;
				//canvas.drawRoundRect(r,5f,5f,currB.isSelected?gopButtonBackgroundSP:gopButtonBackgroundP);
				//canvas.drawRoundRect(r,5f,5f,gopButtonEdgeP);
				//Draw symbol or icon inside Rect.
				int iconPadding=5*scale;
				RectF rect = new RectF(r.left+iconPadding, r.top+iconPadding, r.right-iconPadding, r.bottom-iconPadding);

				if (fop.getIcon()==null) {
					if (fop.getShape()== Shape.circle) {

						//draw circle at rect mid.
						canvas.drawCircle(r.left+r.width()/2, r.top+r.height()/2, r.width()/2-iconPadding*2,(fop.getStyle()==Style.FILL? (currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP):myGis.createPaint(fop.getColor(),fop.getStyle())) );
						//since background is white, add a black edge.
						//if (fop.getStyle()==Style.FILL)
						//	canvas.drawCircle(r.left+r.width()/2, r.top+r.height()/2, r.width()/2-iconPadding*2, currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP);
					} else if (fop.getShape()== Shape.rect){

						canvas.drawRect(rect, myGis.createPaint(fop.getColor(),fop.getStyle()));
						//since background is white, add a black edge.
						if (fop.getStyle()==Style.FILL)
							canvas.drawRect(rect, currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP);
					} else if (fop.getShape()== Shape.triangle) {

						myGis.drawTriangle(canvas, fop.getColor(),fop.getStyle(),
								r.width()/2-iconPadding*2,(int) (r.left+r.width()/2), (int)(r.top+r.height()/2));
					}
				} else {
					//Paint tst = new Paint();
					//tst.setColor(Color.WHITE);
					//tst.setAlpha(100);

					canvas.drawBitmap(fop.getIcon(),null , rect, null);
				}
				canvas.drawText(fop.getName(), r.left+r.width() / 2, rect.bottom+blackTextP.getTextSize(),currB.isSelected?blackTextP:whiteTextP);

			}
			totalHeaderHeight += bounds.height()+SpaceBetweenHeaderAndButton;
		}

		super.onDraw(canvas);
	}




}
