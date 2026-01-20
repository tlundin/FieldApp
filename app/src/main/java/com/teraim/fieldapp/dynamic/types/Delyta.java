package com.teraim.fieldapp.dynamic.types;

import android.graphics.Color;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.non_generics.DelyteManager.Coord;
import com.teraim.fieldapp.non_generics.DelyteManager.ErrCode;

import java.util.ArrayList;
import java.util.List;


public class Delyta {
	private static final String TAG = "Delyta";


	public List<Segment> tag;
	private List<Coord> rawData=null;
	private static final int Rad = 100;
	private int delNr = -1;
	private float area = -1;
	private boolean background = false;
	public float mySouth, myWest;
	private static final Coord South = new Coord(100,180);
	private static final Coord West = new Coord(100,90);
	private static final float NO_VALUE = -9999999;
	private static final int[] DelytaColor = {Color.BLACK,
		Color.parseColor("#4d90fe"),Color.parseColor("#EE7600"),Color.parseColor("#8A9A5B"),Color.DKGRAY,Color.MAGENTA,Color.YELLOW};

	private float myNumX=NO_VALUE,myNumY=NO_VALUE;
	private final DelyteManager dym;

	public Delyta(DelyteManager dym) {
		this.dym=dym;
	}



	public ErrCode create(List<Coord> raw) {

		Log.d(TAG,"Got coordinates: "+raw.toString());
		String sue = "";
		if (raw == null||raw.size()==0)
			return null;
		for (Coord c:raw) {
			sue += "r: "+c.rikt+" a:"+c.avst+",";
		}
		Log.d(TAG,sue);

		if (raw.get(0).avst!=Rad||raw.get(raw.size()-1).avst!=Rad) {
			Log.d(TAG,"Start eller slutpunkt ligger inte på radien");
			return ErrCode.EndOrStartNotOnRadius;
		}
		if (raw.size()<2) {
			Log.d(TAG,"För kort tåg! Tåg måste ha fler än två punkter.");
			return ErrCode.TooShort;
		}
		tag = new ArrayList<Segment>();
		rawData  = new ArrayList<Coord>(raw);
		//First is never an arc.
		Coord start,end;
		boolean isArc=false,previousWasArc = true;
		for (int i =0 ; i<raw.size()-1;i++) {	
			Log.d(TAG,"start: ("+raw.get(i).avst+","+raw.get(i).rikt+")");
			Log.d(TAG,"end: ("+raw.get(i+1).avst+","+raw.get(i+1).rikt+")");
			start = raw.get(i);
			end = raw.get(i+1);	
			isArc = (start.avst==Rad && end.avst==Rad && !previousWasArc);
			previousWasArc = isArc;					
			if(rDist(start.rikt,end.rikt)>1||Math.abs(start.avst-end.avst)>0||(rDist(start.rikt,end.rikt)>0&&isArc))
				tag.add(new Segment(start,end,isArc));
			else
				Log.d(TAG,"Dist and avst same. Did not add tåg");
		}
		//close the loop End -> Start. IsArc is always true.
		if (rDist(raw.get(raw.size()-1).rikt,raw.get(0).rikt)>1) {
			tag.add(new Segment(raw.get(raw.size()-1),raw.get(0),true));
			Log.d(TAG,"Added ending arc from "+tag.get(tag.size()-1).start.rikt+" to "+tag.get(tag.size()-1).end.rikt);
		} else
			Log.e("nils","No ending arc added. Start & end are next to each other");
		//Calc area.
		calcStats();


		return ErrCode.ok;
	}
	private int containsSmaProv=-1;
	private List<Coord> delytePolygon=null;
	private boolean isSelected;

	//TODO: Change implementation so that delyta is only asked to check smaprovytor that remains & part of study
	private void checkIfContainsSmaProv() {
		final boolean abo = Constants.isAbo(dym.getPyID());
		final int[] smaRikt = {0,120,240};
		final int[] avstA = abo?new int[]{30,50,70}:new int[]{30};
		final int[] b = abo?new int[]{1,2,4,8,16,32,64,128,256}:new int[]{1,2,4};

		//avst to smaprov is always 6 meters.
		int i=0;
		Polygon.Builder builder = Polygon.Builder();
		

		for (Coord c:delytePolygon) {
			builder.addVertex(new Point(c.x,c.y));
		}
		Polygon p = builder.build();
		if (p==null) { 
			Log.e("vortex","Couldnt build polygon...something wrong with tåg");
			return;
		}
		i = 0;
		containsSmaProv=0;
		for (int avst:avstA) {
			for (int rikt:smaRikt) {
				if (contains(avst,rikt,p)) 
					containsSmaProv+=b[i];

				i++;
			}
		}
	}

	public int getSmaProv() {
		if (containsSmaProv==-1)
			checkIfContainsSmaProv();		
		return containsSmaProv;
	}


	private boolean isInside(Polygon polygon, Point point)
	{
		boolean contains = polygon.contains(point);
		System.out.println("The point:" + point.toString() + " is " + (contains ? "" : "not ") + "inside the polygon");
		return contains;
	}


	private boolean contains(int avst, int rikt, Polygon p) {



		Coord test = new Coord(avst,rikt);

		return isInside(p,new Point(test.x,test.y));

	}	/*
	      int i;
	      int j;
	      if (points == null) {
	    	  Log.e("nils","Points null in contains(), delyta");
	    	  return false;
	      } 
	      Coord test = new Coord(avst,rikt);
	      boolean result = false;
	      for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
	        if ((points.get(i).y > test.y) != (points.get(j).y > test.y) &&
	            (test.x < (points.get(j).x - points.get(i).x) * (test.y - points.get(i).y) / (points.get(j).y-points.get(i).y) + points.get(i).x)) {
	        	result = !result;
	         }
	      }
	      Log.d(TAG,"contains for smaprov at ["+test.x+","+test.y+"] are "+(result?"inside":"outside")+" delyta "+delNr);
	      return result;
	 */
	/*

	public boolean contains(Point test) {
	      int i;
	      int j;
	      boolean result = false;
	      for (i = 0, j = points.length - 1; i < points.length; j = i++) {
	        if ((points[i].y > test.y) != (points[j].y > test.y) &&
	            (test.x < (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y-points[i].y) + points[i].x)) {
	          result = !result;
	         }
	      }
	      return result;
	    }

	 */



	private void calcStats() {
		area = calcArea();
		mySouth = distance(South);
		myWest = distance(West);

	}

	public void createFromSegments(List<Segment> ls) {
		//TODO:Should this rather be a copy?
		tag = ls;
		//This piece is background.
		background = true;
		calcStats();
	}

	public boolean isBackground() {
		return background;
	}

	public float getArea() {
		return area;
	}
	//If segment is not an arc, the southermost point is on the line from south pole perpendicular to the segment
	//If segment is an arc, the arc is either covering the soutpole or not. If cover, done. If not, caluclate the line 
	//between the southmost coordinate and south pole. This is the distance.
	private float distance(Coord pole) {
		double r2,v2,d,d1,d2;
		final double DR = Math.PI/180;
		double min = 100000;
		double r1 = pole.avst;
		double v1 = pole.rikt*DR;

		for (Segment s:tag) {
			if (s.isArc) {
				int endToPoleDist = pDist(s.end.rikt,pole.rikt);
				int endToStartDist = pDist(s.end.rikt,s.start.rikt);
				if (endToPoleDist < endToStartDist) {				
					Log.d(TAG,"This arc goes through pole");
					min=0;
					break;
				}
			}
			v2 = s.start.rikt*DR;
			r2 = s.start.avst;
			d = calcDistance(r1,v1,r2,v2);
			if (d<min)
				min = d;
			Log.d(TAG,"DISTANCE TO "+(pole.equals(West)?"WEST":"SOUTH")+" for ("+s.start.avst+","+s.start.rikt+") to ("+pole.avst+","+pole.rikt+"): "+d);
		}
		return (float)min;
	}
	
	private double calcDistance(double r1, double v1, double r2, double v2) {
		return Math.sqrt( r1*r1 + r2*r2 - 2*r1*r2*Math.cos(v1 - v2) );
	}
	
	private float oldDistance(Coord Pole) {
		
		float Dx,Dy,x1y2,x2y1;
		float ret=-1,max=10000;
		Log.d(TAG,"In distance calc..");
		for (Segment s:tag) {
			Log.d(TAG,"Segment: "+DelyteManager.printSegment(s));
			if (s.isArc) {
				int endToPoleDist = pDist(s.end.rikt,Pole.rikt);
				int endToStartDist = pDist(s.end.rikt,s.start.rikt);
				if (endToPoleDist < endToStartDist) {				
					Log.d(TAG,"This arc goes through pole");
					max=0;
					break;
				}
				else {
					//d = sqrt [ (x2 - x1)^2 + (y2 - y1)^2 ]
					float dStart = (s.start.rikt-Pole.rikt);
					float dEnd = (s.end.rikt-Pole.rikt);
					if (dStart<0)
						dStart +=360;
					if (dEnd<0)
						dEnd +=360;
					Coord shortest = dStart<=dEnd?s.start:s.end;
					Dx = shortest.x-Pole.x;
					Dy = shortest.y-Pole.y;
					//Log.d(TAG,"dStart dEnd, shortest dx dy"+dStart+","+dEnd+","+shortest.rikt+","+Dx+","+Dy);
					ret = (float)Math.sqrt(Dx*Dx+Dy*Dy);
					
					Log.d(TAG,"DISTANCE TO "+(Pole.equals(West)?"WEST":"SOUTH")+" For ARC: "+ret);

				}

			} else {
				/*
				Dx = s.start.x-s.end.x;
				Dy = s.start.y-s.end.y;
				x1y2 = s.start.x*s.end.y;
				x2y1 = s.end.x*s.start.y;
				float hyp = (float)Math.sqrt(Dx*Dx+Dy*Dy);
				float d = Math.abs(Dy*Pole.x-Dx*Pole.y + x1y2 - x2y1);
				ret = d/hyp;
				float slope = Dy/Dx;
				*/
				
				Log.d(TAG,"DISTANCE TO "+(Pole.equals(West)?"WEST":"SOUTH")+" for SIDE: "+ret);
				

			}
			if (ret<max)
				max = ret;

		}
		return max;
	}

	private static int pDist(float from, float to) {
		if (to <= from)
			return (int)(from-to);
		else
			return (int)(from+360-to);
	}
	public static int rDist(float from, float to) {
		if (to >= from)
			return (int)(to-from);
		else
			return (int)(to+360-from);
	}

	public List<Segment> getSegments() {
		return tag;
	}

	private float calcArea() {
		List<Coord> areaC=new ArrayList<Coord>();
		//Area is calculated using Euler. 
		for (Segment s:tag) {
			//If not arc, add.
			if (!s.isArc) {
				areaC.add(s.start);
				areaC.add(s.end);
			}
			else
				addArcCoords(areaC,s.start,s.end);					
		}
		//Now use euler to calc area.
		float T=0; 
		int p,n;
		for (int i=0;i<areaC.size();i++) {
			p = i==0?areaC.size()-1:i-1;
			n = i==(areaC.size()-1)?0:i+1;
			T+= areaC.get(i).x*(areaC.get(n).y-areaC.get(p).y);
		}
		Log.d(TAG,"Area calculate to be "+T/2);
		delytePolygon = areaC;
		/*
		if (T/2>18000) {
			String pol="";
			for (int i=0;i<areaC.size();i++) 
				pol+="["+i+":{"+areaC.get(i).x+","+areaC.get(i).y+"}]";
			Log.e("nils",pol);
		}
		 */
		return T/2;
	}

	public List<Coord> getSpecial() {
		return delytePolygon;
	}

	//should be coordinates on the radius. with grad running 0..359. 
	private void addArcCoords(List<Coord> areaC, Coord start, Coord end) {
		int i = (int)start.rikt;
		Log.d(TAG,"Stratos: "+start.rikt+" endos: "+end.rikt);
		while (i!=end.rikt) {
			i=(i+1)%360;					
			areaC.add(new Coord(Rad,i));
			//Log.d(TAG,""+i);
		}
	}

	public void setId(int delyteID) {
		Log.d(TAG,"DelyteID set to "+delyteID);
		delNr=delyteID;
	}

	public int getId() {
		return delNr;
	}

	public void setNumberPos(float mX, float mY) {
		myNumX = mX;
		myNumY = mY;
	}

	public Point getNumberPos() {
		if (myNumX==NO_VALUE||myNumX==NO_VALUE)
			return null;
		//		Point p = new Point();
		//		p.set((int)myNumX,(int)myNumY);
		return new Point(myNumX,myNumY);
	}

	public String getTag() {
		String res="";
		if (rawData == null)
			return null;

		for (Coord c:rawData) {
			res = res + c.getAvst()+"|"+c.getRikt()+"|";
		}
		//remove superfl divider.
		if (rawData.size()>0)
			res = res.substring(0, res.length()-1);
		else
			Log.e("nils","Something is wrong...rawData empty");
		Log.d(TAG, "getTåg returns: ["+res+"]");
		return res;
	}

	public String getTagPrettyPrint() {
		String res="[ ";
		String splitToken = " | ";
		if (rawData == null||rawData.size()==0) {
			Log.e("nils","Something is wrong...rawData empty");
			return "";
		}
		for (Coord c:rawData) 
			res = res + c.getAvst()+","+c.getRikt()+splitToken;
		res = res.substring(0, res.length()-splitToken.length());

		return res+" ]";
	}
	
	public String getTagCommaPrint() {
		String res = "";
		for (Coord c:rawData) 
			res = res + c.getAvst()+","+c.getRikt()+",";
		res = res.substring(0, res.length()-1);
		return res;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean sel) {
		isSelected=sel;
	}

	public int getColor() {
		Log.d(TAG,"color: "+DelytaColor[this.getId()>0?this.getId():0]);
		return DelytaColor[delNr>=0&&delNr<6?delNr:0];
	}
	
	public List<Coord> getRawTag() {
		return rawData;
	}



	
}

