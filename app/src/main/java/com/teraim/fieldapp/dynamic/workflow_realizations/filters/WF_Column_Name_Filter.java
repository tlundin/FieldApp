package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.log.LogRepository;

import java.util.Iterator;
import java.util.List;

//Specialized filter. Will filter a list on Prefix.
public class WF_Column_Name_Filter extends WF_Filter {

	private String myPrefix = "";
	String filterColumn;
	private final String columnToMatch;
	private final FilterType filterType;
	private boolean totMatch=false;

	public enum FilterType{
		exact,
		sets, prefix
	}

	public WF_Column_Name_Filter(String id,String filterCh,String columnToMatch,FilterType type) {
		super(id);
		myPrefix = filterCh;
		this.columnToMatch=columnToMatch;
		filterType = type;
	}
	
	@Override
	public void filter(List<? extends Listable> list) {
		String key;
		Iterator<? extends Listable> it = list.iterator();
		//Log.d("filterz","filtering with type "+filterType.name());
		while(it.hasNext()) {
			Listable l = it.next();

			//Log.d("filterz","l "+l+" key "+l.getSortableField(columnToMatch)+" coltomatch "+columnToMatch);
			key = l.getSortableField(columnToMatch);
			if (key==null)
				continue;
			if (match(key)) {
				it.remove();
				//if (!key.isEmpty())
				//  Log.d("filterz", "filter REMOVES element " + l.getKey()+" Label: "+l.getLabel());// + " because " + key.charAt(0) + " doesn't match " + myPrefix);

			}
			else {
				//Log.d("filterz", "filter KEEPS element " + l.getKey()+" Label: "+l.getLabel()+" column: "+key);// + " because " + key.charAt(0) + " doesn't match " + myPrefix);
				//Log.d("nils","filter match for element "+key+" because "+key.charAt(0)+" match "+myPrefix);
				totMatch = true;
			}

		}
		if (!totMatch) {
			o = LogRepository.getInstance();
			o.addText("");
			o.addYellowText("No matches found in Column filter. Column used: ["+columnToMatch+"]");
		}

    }

	@Override
	public boolean isRemovedByFilter(Listable l) {
		return match(l.getSortableField(columnToMatch));
	}


	private boolean match(String key) {

		boolean match = false;

		if (filterType == FilterType.prefix) {
			if (key.isEmpty()) {
				match=false;
			}

			for (int i=0;i<myPrefix.length();i++) {
				if (Character.toLowerCase(key.charAt(0))==Character.toLowerCase(myPrefix.charAt(i))) {
					match = true;

					break;
				}
			}
		} else if (filterType == FilterType.exact) {
				match = true;
				if (myPrefix.length()!=key.length()) {
					match = false;
				}
				else {
					for (int i=0;i<myPrefix.length();i++) {
						if (Character.toLowerCase(key.charAt(i))!=Character.toLowerCase(myPrefix.charAt(i))) {
							match = false;
							break;
						}
					}
				}
		}
		else if (filterType == FilterType.sets) {
			//check if the key is one of the facets in the cell.
			if (key == null || key.isEmpty()) {
				match = false;
			} else {
				String[] facets = key.split("\\|");
				for (String facet:facets) {

					match=true;
					if (facet.length()!=myPrefix.length()) {
						//Log.d("vortex","found NO match for key"+key+" facet "+facet+" and myPrefix: "+myPrefix);
						match = false;
						continue;
					}
					for (int i = 0; i<facet.length();i++) {
						if (Character.toLowerCase(facet.charAt(i))!=Character.toLowerCase(myPrefix.charAt(i))) {
							match = false;
							break;
						} else
							continue;
					}
					if (match) {
						break;
					}

				}
			}



		}
		return !match;
	}

}