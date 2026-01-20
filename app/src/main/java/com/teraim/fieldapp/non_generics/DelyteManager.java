package com.teraim.fieldapp.non_generics;

import android.util.Log;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Delyta;
import com.teraim.fieldapp.dynamic.types.Segment;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.utils.DbHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


//SLU specific class.



public class DelyteManager {
	private static final String TAG = "DelyteManager";


	private static DelyteManager instance;
	private boolean hasUnsaved = false;
	private final GlobalState gs;
	private final VariableConfiguration al;
	private final VariableCache varCache;
	private final List<Delyta> myDelytor = new ArrayList<Delyta>();
	private static int myPyID;
	private List<Segment> answer;
	private final static float r = 100;

	public static final int MAX_DELYTEID = 7;



	//helper classes
	public static class Coord {
		public final float x;
		public final float y;
		public final float avst;
		public float rikt;
		private final static float r = 100;
		public Coord(int avst,int rikt) {
			int riktC = rikt-90;
			if (riktC<0)
				riktC +=360;
			double phi = 0.0174532925*(riktC);
			x = (float)(avst * Math.cos(phi));
			y = (float)(avst * Math.sin(phi));	
			//
			this.avst=avst;
			this.rikt=rikt;
		}
		public Coord(float x,float y, float avst) {
			this.x = x;
			this.y = y;
			this.avst = avst;
			//avst = (int)Math.sqrt((x)*(x) + (y)*(y));
			rikt = Math.round((float)Math.toDegrees(Math.atan2(y,x)));
			//always positive values please;
			if (rikt<0)
				rikt +=360;
			//Log.d(TAG,"Avstånd: "+avst+" Rikt: "+rikt+" from X "+this.x+"Y "+this.y);
		}

		public int getAvst() {
			return Math.round(avst);
		}
		public int getRikt() {
			return Math.round(rikt);
		}


	}



	public enum ErrCode {
		TooShort,
		EndOrStartNotOnRadius,
		ok
	}


	public void analyze() {
		//Find the background
		calcRemainingYta();		
		//Store the number to database
		saveNoOfDelytor();
		//Sort south - west and give each a delyteID
		if (!assignDelyteId()) {
			clear();
			Toast.makeText(gs.getContext(), "Tilldelning av delyteID misslyckades. Något är fel med linjetågen.", Toast.LENGTH_LONG).show();			
		} else {
			//Find coordinates where to put the numbers.
			findNumberCoord();
			//update delyteID for each sm�provyta in database
			//save delytor to database if needed.
			if (hasUnsaved)
				save();
		}
	}

	private void saveNoOfDelytor() {
		int size = this.getDelytor()==null?0:this.getDelytor().size();
		varCache.getVariable(al.createProvytaKeyMap(), "noOfDelytor").setValue(size+"");
	}

	private boolean updateSmaProvToDelytaAssociations() {
		Variable nyUtlagg = varCache.getVariable(gs.getVariableConfiguration().createProvytaKeyMap(), NamedVariables.NYUTLAGG);
		//Only do the association if nyutlägg.
		Map<String, String> key;
		DbHelper db = gs.getDb();
		final int[] b = (Constants.isAbo(myPyID)?new int[]{1,2,4,8,16,32,64,128,256}:new int[]{1,2,4});

		Set<Delyta> hasSmaS = new HashSet<Delyta>();

		//select those delytor that have småprov
		int chkSum = 0;
		for  (Delyta d:myDelytor) 
			if ((d.getSmaProv())!=0) {
				hasSmaS.add(d);
				chkSum+=d.getSmaProv();
				Log.d(TAG,"Checksum added "+d.getSmaProv()+" for delyta "+d.getId());
			} else 
				Log.d(TAG,"Delyta "+d.getId()+" did not have a småyta");
		int allSum=0;
		for (int i=0;i<b.length;i++)
			allSum+=b[i];
		if (chkSum!=allSum) {
			Log.d(TAG,"UPS! CHECKSUM: "+chkSum+" ALLSUM: "+allSum);
			return false;
		}
		//erase existing.
		db.eraseSmaProvyDelytaAssoc(al.getCurrentRuta(),al.getCurrentProvyta());
		//update associations
		for (Delyta d:hasSmaS) {
			for (int i=0;i<b.length;i++) 
				if ((b[i]&d.getSmaProv())!=0) {
					Log.d(TAG,"adding Delyta ID "+d.getId()+" to småprovyta "+(i+1));
					key = al.createProvytaKeyMap();
					key.put("smaprovyta", (i+1)+"");
					Variable tmp = varCache.getVariable(key, NamedVariables.BeraknadInomDelyta);
					if (tmp!=null)
						tmp.setValue(d.getId()+"");				
					//db.fastInsert(key, NamedVariables.BeraknadInomDelyta, d.getId()+"");					
					if (nyUtlagg.getValue()!=null) {
						tmp = varCache.getVariable(key, NamedVariables.InomDelyta);
						if (tmp!=null)
							tmp.setValue(d.getId()+"");
					}


				}
		}


		return true;


	}

	private void findNumberCoord() {
		//Find where to put the number.

		List<Segment> cTag;
		for (Delyta d:myDelytor) {


			cTag = d.getSegments();
			//Just a simple arc?
			Set<Segment>arcs=new HashSet<Segment>();
			for (Segment s:cTag) {
				if (s.isArc)
					arcs.add(s);
			}
			if (!arcs.isEmpty()) {
				Iterator<Segment> it = arcs.iterator();
				//if (arcs.size()==1) {				
				//Put in middle of pyramid.
				Segment arc=it.next();
				int dist;
				int arcMid,nyR;
				dist = Delyta.rDist(arc.start.rikt,arc.end.rikt);
				Log.d(TAG,"Delyta: "+d.getId());
				Log.d(TAG,"Start: "+arc.start.rikt+" End: "+arc.end.rikt+ "Dist: "+dist);
				arcMid =  (dist/2);
				Log.d(TAG," Medeldistans "+arcMid);
				nyR = (int)(arc.start.rikt+arcMid);
				Log.d(TAG," nyR "+nyR);
				if (nyR>360)
					nyR=nyR-360;
				if (arc.start.rikt>arc.end.rikt) 
					Log.d(TAG,"Start more: "+nyR);
				else
					Log.d(TAG,"Start less: "+nyR);

				Log.d(TAG,"DIST: "+dist);
				Coord m;
				m = new Coord(85,nyR);

				d.setNumberPos(m.x,m.y);
			} else {
				Log.e("nils","NO ARC FOUND!");
				for (Segment s:cTag)
					Log.d(TAG,"Start: "+s.start.rikt+" End: "+s.end.rikt);
			}
			/*} else if (arcs.size()==2) {

				Segment arc1=it.next();
				Segment arc2=it.next();

				int dist1 = Delyta.rDist(arc1.start.rikt,arc1.end.rikt);
				int dist2 = Delyta.rDist(arc2.start.rikt,arc2.end.rikt);
				int mid1 = dist1/2;
				int mid2 = dist2/2;

				int nyR1 = arc1.start.rikt+mid1;
				if (nyR1>360)
					nyR1=nyR1-360;
				int nyR2 = arc2.start.rikt+mid2;
				if (nyR2>360)
					nyR2=nyR2-360;

				int dist = Delyta.rDist(nyR1,nyR2);
				int arcMid =  (dist/2);
				int nyR = nyR1+arcMid;
				if (nyR>360)
					nyR=nyR-360;
				Coord m = new Coord(0,nyR1);
				//double x = 100 * Math.cos(nyR);
				//double y = 100 * Math.sin(nyR1);
				d.setNumberPos(m.x,m.y);
			}

			 */

		}


		//If its a normal arc, put it in the center of the Pyramid.

	}
	private float midP(float s,float e) {
		return (s+e)/2;
	}

	private boolean assignDelyteId() {
		if (myDelytor.size()==1) {
			Log.d(TAG,"Only one delyta...already has id. Exit asignDelyteId");
			return true;
		}

		int delyteIdC = 1;
		int backId = -1;
		SortedSet<Delyta> s = new TreeSet<Delyta>(new Comparator<Delyta>() {

			@Override
			public int compare(Delyta lhs, Delyta rhs) {
				return ((lhs.mySouth==rhs.mySouth?lhs.myWest-rhs.myWest:lhs.mySouth-rhs.mySouth)<0?-1:1);
				/*
				if (cmp == 0) {
					Log.d(TAG,"COMP RET 0 for ls "+lhs.mySouth+" rs "+rhs.mySouth+" lw "+lhs.myWest+" rw "+rhs.myWest);
				}
				return cmp;
				 */
			}});

		Log.d(TAG,"IN SORTOS");
		printDelytor();
		s.addAll(myDelytor);


		Log.d(TAG,"Mydelytor has "+myDelytor.size()+" delytor");
		Log.d(TAG,"Sorted ytor according to south/west has "+s.size()+" delytor");
		if (myDelytor.size()!=s.size()) {
			return false;
		}
		for (Delyta d:s) {
			d.setId(delyteIdC);
			if (d.isBackground()) {
				Log.d(TAG,"found background piece...");
				if (backId!=-1)
					d.setId(backId);
				else
					backId = delyteIdC;
			}
			Log.d(TAG,
					"Assigned ID "+d.getId()+" to delyta with first segment S:"+d.getSegments().get(0).start.rikt+" E:"+d.getSegments().get(0).end.rikt+" isArc: "+d.getSegments().get(0).isArc);

			delyteIdC++;
		}
		return true;
	}


	private void calcRemainingYta() {
		List<Segment> freeArcs = new ArrayList<Segment>();
		List<Segment> bgPoly;

		SortedSet<Segment> sortedArcs = new TreeSet<Segment>(new Comparator<Segment>(){
			@Override
			public int compare(Segment lhs, Segment rhs) {
				return (int)(lhs.start.rikt-rhs.start.rikt);
			}});
		//Sort arcs. Save in set.
		for(Delyta d:myDelytor)
			for (Segment s:d.tag) {
				if (!s.isArc)
					continue;
				else {
					Log.d(TAG,"Adding existing arc piece. S:"+s.start.rikt+" E:"+s.end.rikt+" Delyta "+d.getId());
					sortedArcs.add(s);
				}
			}
		if (sortedArcs.isEmpty()) {
			Log.d(TAG,"NO FREE ARC!");
		} else {
			/*			//A free arc is an arc that stretches between the end of an existing arc, and the beginning of the next.
			Segment x = sortedArcs.last();
			Log.d(TAG,"number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				freeArcs.add(new Segment(x.end,s.start,true));
				Log.d(TAG,"Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
				x=s;
			}
		}
			 */

			//A free arc is an arc that spans a gap between two existing arcs

			Segment x = sortedArcs.last();

			Log.d(TAG,"number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				Log.d(TAG,"checking arc: "+x.end.rikt+","+s.start.rikt);
				if (Delyta.rDist(x.end.rikt, s.start.rikt)<1) {
					Log.d(TAG,"skipping "+x.end.rikt+","+s.start.rikt);

				} else {
					Segment potential = new Segment(x.end,s.start,true);
					//check that this arc is not already covered.
					if (!hasSegment(sortedArcs, potential)) {
						freeArcs.add(new Segment(x.end,s.start,true));
						Log.d(TAG,"Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
					}
				}
				x=s;
			}
		}

		//Extract all segments that are not arcs.
		for(Delyta d:myDelytor) {
			for (Segment s:d.tag) {
				if (s.isArc)
					continue;
				else {//Add reverse of all sides.
					freeArcs.add(new Segment(s.end,s.start,false));
					Log.d(TAG,"added side piece S:"+s.end.rikt+" E:"+s.start.rikt);
				}
			}
		}

		Log.d(TAG,"Frearcs has "+freeArcs.size()+" elements");

		Set<List<Segment>> bgPollies= new HashSet<List<Segment>>();

		//Find all connected loops.
		//loop needs at least 3 pieces & one arc.

		while (findPoly(new ArrayList<Segment>(),freeArcs)) {
			Log.d(TAG,"background poly:");
			for(Segment s:answer) {
				printSegment(s);
				if (!freeArcs.remove(s))
					Log.e("vortex","remove failed!");
			}
			bgPollies.add(answer);
		}
		//Should look for more. 


		for (List<Segment> bgp:bgPollies) {
			if (bgp.size()>0) {
				Delyta d = new Delyta(this);
				d.createFromSegments(bgp);
				myDelytor.add(d);
			}
		}

		Log.d(TAG,"myDelytor now contains "+myDelytor.size()+" delytor.");
		printDelytor();


	}

	//Finds a closed polygon built from the segments in freeArcs. If not, returns false
	private boolean findPoly(List<Segment> bgPoly, List<Segment> freeArcs) {

		Log.d(TAG,"freearcsize: "+freeArcs.size());
		Log.d(TAG,"bgpoly size: "+bgPoly.size());

		int polySize = bgPoly.size();
		if (polySize == 0) {
			if (freeArcs.isEmpty()) {
				Log.d(TAG,"freearcs empty and polysize 0, fail!");
				return false;
			}
			bgPoly.add(freeArcs.remove(0));
			return findPoly(bgPoly,freeArcs);

		}

		//If first and last touches, and there is more than one segment, this is a poly!
		if (polySize>1&&isConnected(bgPoly.get(0),bgPoly.get(polySize-1))) {
			if (bgPoly.get(0).isArc || bgPoly.get(polySize-1).isArc) {
				Log.d(TAG,"found touch between "+printSegment(bgPoly.get(polySize-1))+" and "+ printSegment(bgPoly.get(0)));
				answer = new ArrayList<Segment>();
				answer.addAll(bgPoly);
				Log.d(TAG,"ansewr has "+answer.size()+" elements!");
				return true;
			} else {
				Log.e("vortex","a loop between two sides...not allowed");
				return false;
			}
		}

		Iterator<Segment> it = freeArcs.iterator();
		Segment lastP = bgPoly.get(bgPoly.size()-1);
		while (it.hasNext()) {
			Segment currentP = it.next();
			if (isTouching(lastP,currentP)) {
				Log.d(TAG,"YES!");
				List<Segment> altPoly = new ArrayList<Segment>(bgPoly);
				altPoly.add(currentP);
				List<Segment> altFreeArcs = new ArrayList<Segment>(freeArcs);
				altFreeArcs.remove(currentP);
				//bgPoly.add(currentP);
				//freeArcs.remove(currentP);
				if (findPoly(altPoly,altFreeArcs))
					return true;
				else {
					Log.d(TAG,"fail..."+printSegment(currentP)+" trying next..");
				}
			}
		}
		Log.d(TAG,"no touch found...removing segment "+printSegment(lastP));
		bgPoly.remove(lastP);
		return findPoly(bgPoly,freeArcs);


	}

	public float getArea(int delyteId) {
		float area=0;
		for(Delyta d:myDelytor) {
			if (d.getId()==delyteId)
				area += d.getArea();
		}
		return area;
	}
	
	public static String printSegment(Segment c) {
		return "[ s:("+c.start.avst+","+c.start.rikt+"),e:("+c.end.avst+","+c.end.rikt+") ]";
	}

	private boolean hasArc(List<Segment> segments) {
		for (Segment s:segments)
			if (s.isArc)
				return true;
		return false;
	}

	private boolean startsOnEdge(Segment s) {
		return s.start.avst==r;
	}

	private boolean isTouching(Segment a, Segment b) {
		Log.d(TAG,"isTouching "+printSegment(a)+" and "+printSegment(b)+" ?");
		if (a.isArc&&b.isArc) {
			Log.e("vortex","Touch between arcs, not allowed");
			return false;
		}
		return a.end.rikt == b.start.rikt && a.end.avst==b.start.avst;
	}
	private boolean isConnected(Segment a, Segment b) {
		return a.start.rikt == b.end.rikt && a.start.avst==b.end.avst;

	}


	public void calcRemainingYtaOld() {
		List<Segment> freeArcs = new ArrayList<Segment>();
		List<Segment> bgPoly;

		SortedSet<Segment> sortedArcs = new TreeSet<Segment>(new Comparator<Segment>(){
			@Override
			public int compare(Segment lhs, Segment rhs) {
				return (int)(lhs.start.rikt-rhs.start.rikt);
			}});
		//Sort arcs. Save in set.
		for(Delyta d:myDelytor)
			for (Segment s:d.tag) {
				if (!s.isArc)
					continue;
				else {
					Log.d(TAG,"Adding existing arc piece. S:"+s.start.rikt+" E:"+s.end.rikt+" Delyta "+d.getId());
					sortedArcs.add(s);
				}
			}
		if (sortedArcs.isEmpty()) {
			Log.d(TAG,"NO FREE ARC!");
		} else {
			/*			//A free arc is an arc that stretches between the end of an existing arc, and the beginning of the next.
			Segment x = sortedArcs.last();
			Log.d(TAG,"number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				freeArcs.add(new Segment(x.end,s.start,true));
				Log.d(TAG,"Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
				x=s;
			}
		}
			 */

			//A free arc is an arc that spans a gap between two existing arcs

			Segment x = sortedArcs.last();

			Log.d(TAG,"number of arcs: "+sortedArcs.size());
			for (Segment s:sortedArcs) {
				Log.d(TAG,"checking arc: "+x.end.rikt+","+s.start.rikt);
				if (Delyta.rDist(x.end.rikt, s.start.rikt)<1) {
					Log.d(TAG,"skipping "+x.end.rikt+","+s.start.rikt);
					x=s;
					continue;
				}
				Segment potential = new Segment(x.end,s.start,true);
				//check that this arc is not already covered.
				if (!hasSegment(sortedArcs, potential)) {
					freeArcs.add(new Segment(x.end,s.start,true));
					Log.d(TAG,"Adding free arc piece. S:"+x.end.rikt+" E:"+s.start.rikt);
				}
				x=s;
			}
		}

		int startP = freeArcs.size();
		//Extract all segments that are not arcs.
		for(Delyta d:myDelytor) {
			for (Segment s:d.tag) {
				if (s.isArc)
					continue;
				else
					freeArcs.add(new Segment(s.end,s.start,false));
			}
		}
		Log.d(TAG,"Freacrs has "+freeArcs.size()+" elements");
		//starting piece should not be an arc. Use first other. If no other, there are no free pieces.
		Set<List<Segment>> bgPollies= new HashSet<List<Segment>>();
		int previousSize=0;

		while(freeArcs.size()>startP && freeArcs.size()!=previousSize) {
			previousSize = freeArcs.size();
			//if (freeArcs.size()>startP) {
			bgPoly = new ArrayList<Segment>();
			Segment c = freeArcs.get(startP);
			bgPoly.add(c);
			//sort.
			boolean found = true;
			while (freeArcs.size() > 0 && found) {
				found = false;
				for (Segment n:freeArcs) {
					Log.d(TAG,"Comparing segment (("+c.start.rikt+","+c.start.avst+"),("+c.end.rikt+","+c.end.avst+")) to (("+n.start.rikt+","+n.start.avst+"),("+n.end.rikt+","+n.end.avst+"))");	
					if (n.start.rikt==c.end.rikt && n.start.avst == c.end.avst) {
						bgPoly.add(n);
						freeArcs.remove(c);
						c=n;
						found = true;
						break;						
					} 

				}

			}
			Log.d(TAG,"bgpoly found of size "+bgPoly.size());
			for(Segment s:bgPoly) 
				Log.d(TAG,"start: "+s.start.rikt+"end: "+s.end.rikt+" isarc: "+s.isArc);
			bgPollies.add(bgPoly);
		}
		/*
		for(Delyta d:myDelytor) {
			for (Segment s:d.tag) {
				if (s.isArc)
					continue;
				else
					noArcs.add(s);
			}
		}

		Log.d(TAG,"Free arcs: ");

		for(Segment s:freeArcs) {
			Log.d(TAG,"S: "+s.start.rikt+" E:"+s.end.rikt);
			bgPoly = new ArrayList<Segment>();
			//Add the free arc but reversed! 
			bgPoly.add(new Segment(s.start,s.end,true));
			//Begin from the end piece (in reverse = start)
			Coord currentCoord = s.end;
			//we want to find the line that  ends here and goes to our start. when we reach there it is done.
			Coord end = s.start;
			boolean notDone = true;
			int emergencyC = 15;
			while (notDone) {
				//Find corresponding "real" Segment.
				boolean noLuck = true;
				Log.d(TAG,"noArcs has "+noArcs.size()+" elements");
				for (Segment se:noArcs) {
					Log.d(TAG,"Comparing current: "+currentCoord.rikt+" with "+se.end.rikt);
					if (currentCoord.rikt == se.end.rikt) {
						Log.d(TAG,"Found END MATCH!");
						//Add the reversed segment.
						bgPoly.add(new Segment(se.end,se.start,se.isArc));

						if (se.isArc) {
							Log.e("nils","In DelyteManager,calcRemYta..segment seems to be arc...should not happen");
							Toast.makeText(gs.getContext(), "Tåget är feldefinierat. Kontrollera punkterna", Toast.LENGTH_LONG).show();;
						}
						Log.d(TAG,"SE SEGMENT has start rikt: "+se.start.rikt+" and end rikt: "+se.end.rikt);
						currentCoord = se.start;
						if (currentCoord.rikt==end.rikt) {
							Log.d(TAG,"Done, found end.");
							Log.d(TAG,"CurrentCoord.rikt: "+currentCoord.rikt+" DOES match end.rikt: "+end.rikt);
							missingPieces.add(bgPoly);
							notDone = false;
							noLuck = false;
							break;
						} else {	
							Log.d(TAG,"CurrentCoord.rikt: "+currentCoord.rikt+" does not match end.rikt: "+end.rikt);
							noLuck=(emergencyC--<0);						

						}

					} 

				}
				if (noLuck) {
					notDone = false;
					if(noArcs.size()>0) {
						Log.e("nils","went thorough all without finding coord...should not happen.");					
						Toast.makeText(gs.getContext(), "Tåget är feldefinierat. Kontrollera punkterna", Toast.LENGTH_LONG).show();;
					}
				}
			}
		}

		//Here we should have all missing polygons.
		Log.d(TAG,"found "+missingPieces.size()+" polygons");
		//Build delytor.

		for (List<Segment> ls:missingPieces) {
			Delyta d = new Delyta(this);
			d.createFromSegments(ls);
			myDelytor.add(d);
		}

		 * 
		 */
		for (List<Segment> bgp:bgPollies) {
			if (bgp.size()>0) {
				Delyta d = new Delyta(this);
				d.createFromSegments(bgp);
				myDelytor.add(d);
			}
		}

		Log.d(TAG,"myDelytor now contains "+myDelytor.size()+" delytor.");
		printDelytor();


		//Using the free arcs as starting point, build the missing delyta.

	}





	private boolean hasSegment(SortedSet<Segment> sortedArcs, Segment p) {
		for (Segment s:sortedArcs) {
			if (p.start.rikt==s.start.rikt) // && p.end.rikt == s.end.rikt)
				return true;
		}
		return false;
	}

	private void printDelytor() {
		for (Delyta d:myDelytor) {
			Log.d(TAG,"DELYTA ID: "+d.getId()+" isbg: "+d.isBackground()+" WEST: "+d.myWest+" SOUTH: "+d.mySouth);
			for(Segment s:d.getSegments()) {
				Log.d(TAG,"S: "+s.start.rikt+" E:"+s.end.rikt+" isArc: "+s.isArc);
			}
		}
	}

	private DelyteManager(GlobalState gs,int pyId) {
		long t0 = System.currentTimeMillis();
		this.gs = gs;
		myPyID=pyId;
		al = gs.getVariableConfiguration();
		varCache = gs.getVariableCache();
		init();
		Log.d(TAG,"dymtime: "+(System.currentTimeMillis()-t0));
	}

	public static DelyteManager getInstance() {
		GlobalState gs = GlobalState.getInstance();
		String py = gs.getVariableConfiguration().getCurrentProvyta();
		if (py == null) {
			Log.e("vortex","Failed to create delytemanager. No current provyta.");
			return null;
		}
		int currentProvyteId = Integer.parseInt(py);
		if (instance == null || myPyID != currentProvyteId) {
				instance = new DelyteManager(gs,currentProvyteId);
		}
		return instance;
	}


	



	private void loadKnownDelytor() {

		Variable v;Map<String,String> keys;
		Set<String>rawTags = new HashSet<String>();
		Map<String, String> baseKey = al.createProvytaKeyMap();
		if (baseKey==null) {
			Log.e("nils","provyta or ruta null in loadKnownDelytor, DelyteManager");
			return;
		}

		for (int delyteID=1;delyteID<=MAX_DELYTEID;delyteID++) {
			baseKey.put("delyta", delyteID+"");
			v = varCache.getVariable(baseKey,NamedVariables.DELNINGSTAG);
			Log.d(TAG,"Found tåg under delyteID "+delyteID+" :"+v.getValue());
			if (v.getValue()!=null&&v.getValue().length()>0)
				rawTags.add(v.getValue());			
		}

		if (rawTags.size()==0 && !isNyUtlagg()) {
			hasUnsaved = true;
			Log.d(TAG,"No current values for tag. Trying historical");
			for (int delyteID=1;delyteID<=MAX_DELYTEID;delyteID++) {
				baseKey.put("delyta", delyteID+"");
				v = varCache.getVariable(baseKey,NamedVariables.DELNINGSTAG);
				String rawTag = v.getHistoricalValue();
				if (rawTag!=null && rawTag.length()>0) {
					Log.d(TAG,"Found historical tåg under delyteID "+delyteID+" :"+rawTag);
					//Save under this year.
					v.setValueNoSync(rawTag);
					rawTags.add(rawTag);
				}

			}

		}
		if (rawTags.size()==0) {
			Log.d(TAG,"Nyutlägg eller tom historia");
			addDefault();	
		}
		else {			
			for (String rawTag:rawTags) {
				if (rawTag != null && rawTag.length()>0) {
					String[] tagElems = rawTag.split("\\|");
					Delyta delyta = createDelyta(tagElems);
					if (delyta!=null) 					
						myDelytor.add(delyta);

				}
			}
		}


		Log.d(TAG,"found "+myDelytor.size()+" delytor");



	}

	public ErrCode addUnknownTag(List<Coord> tagCoordinateList) {
		Delyta delyta = new Delyta(this);
		ErrCode ec = delyta.create(tagCoordinateList);
		if (ec==ErrCode.ok) {
			hasUnsaved = true;
			myDelytor.add(delyta);
		}
		return ec;
	}

	public void clear() {
		hasUnsaved = false;
		myDelytor.clear();
	}


	private Delyta createDelyta(String[] tagElems) {
		if (tagElems!=null) {
			for (String s:tagElems) {
				Log.d(TAG,"tagElem: "+s);
			}
			float avstf=-1,riktf=-1;
			List<Coord> tagCoordinateList = new ArrayList<Coord>();
			for (int j=0;j<tagElems.length-1;j+=2) {
				avstf =Float.parseFloat(tagElems[j]);
				riktf =Float.parseFloat(tagElems[j+1]);
				tagCoordinateList.add(new Coord((int)avstf,(int)riktf));

			}
			Delyta delyta = new Delyta(this);
			ErrCode ec = delyta.create(tagCoordinateList);
			if (ec==ErrCode.ok)
				return delyta;
			else 
				Log.e("nils","Failed to create delyta. error code: "+ec);
		}
		return null;
	}


	public List<Delyta> getDelytor() {
		return myDelytor;
	}

	public void init() {
		myDelytor.clear();
		//gs.setKeyHash(al.createDelytaKeyMap());
		loadKnownDelytor();
		analyze();
	}

	public boolean save() {

		//Check if it is possible to determine småprovytans location. If not, return false
		if(!updateSmaProvToDelytaAssociations())
			return false;		
		Map<String, String> baseKey = al.createProvytaKeyMap();
		Variable tempVar;
		//Copy new
		for (Delyta d:myDelytor) {
			if (d!=null) {
				int id = d.getId();
				Log.d(TAG,"added delyta = "+id+" to base key");				
				baseKey.put("delyta", id+"");
				if (id!=0) {					
					String tag = d.getTag();
					tempVar = varCache.getVariable(baseKey, "Delningstag");
					tempVar.setValue(tag);
					//Contains smaprovyta? In that case, set delytaId in variable
					//tillhorDelytaMedID
				}
			}
		}	

		hasUnsaved = false;
		return true;
	}

	public void addDefault() {
		Log.d(TAG,"No delytor found. Creating default delyta 0");
		Delyta delyta = createDelyta(new String[] {"100","359","100","0"});
		delyta.setId(0);
		myDelytor.add(delyta);
	}

	private boolean isNyUtlagg() {
		String isNy = varCache.getVariableValue(al.createProvytaKeyMap(), NamedVariables.NYUTLAGG);
		return isNy!=null;
	}

	public boolean hasUnsavedChanges() {
		return hasUnsaved;
	}

	public int getPyID() {
		return myPyID;
	}

	public void setSelected(int i) {
		for(Delyta d:myDelytor) {
			if (i == d.getId())
				d.setSelected(true);
			else
				d.setSelected(false);
		}
	}

	public Delyta getDelyta(int id) {
		for(Delyta d:myDelytor) {
			if (d.getId()==id)
				return d;
		}
		return null;
	}

	public boolean isObsolete() {
		Map<String, String> baseKey = al.createProvytaKeyMap();
		Variable tempVar;
		//Copy new
		for (Delyta d:myDelytor) {
			if (d!=null) {
				int id = d.getId();
				Log.d(TAG,"added delyta = "+id+" to base key");				
				baseKey.put("delyta", id+"");
				if (id!=0) {					
					tempVar = varCache.getVariable(baseKey, "Delningstag");
					if (tempVar.isInvalidated()) {
						Log.d(TAG,"I think my DYM IS OBSOLETE!!!!");
						return true;
					}
					//Contains smaprovyta? In that case, set delytaId in variable

				}
			}
		}
		return false;
	}







}
