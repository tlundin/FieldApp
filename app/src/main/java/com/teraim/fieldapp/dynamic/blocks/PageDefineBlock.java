package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.util.List;

/**
 * Page Definition block
 * @author Terje
 *
 */
public  class PageDefineBlock extends Block {


	private String pageName="",pageType=null;
	private final boolean hasGPS;
	private final String gpsPriority;
	private boolean goBackAllowed=true;
	private final String gisMode;
	private final List<EvalExpr>pageLabelE;


	public String getPageName() {
		return pageName;
	}
	public String getPageType() {
		if (pageType != null && pageType.equals("TableDefaultTemplate"))
			return "PageWithTable";
		return pageType;
	}
	public String getPageLabel() {
		return Expressor.analyze(pageLabelE);
	}
	public boolean hasGPS() {
		return hasGPS;
	}

	public String gpsPriority() { return gpsPriority; }

	/** "detailed" = two-circle glow for team icons; "normal" or absent = needle icons. */
	public String getGisMode() { return gisMode != null ? gisMode : "normal"; }

	public PageDefineBlock(String id,String pageName,String pageType,String pageLabel,boolean hasGPS, String gpsPriority, boolean goBackAllowed) {
		this(id, pageName, pageType, pageLabel, hasGPS, gpsPriority, goBackAllowed, "normal");
	}

	public PageDefineBlock(String id,String pageName,String pageType,String pageLabel,boolean hasGPS, String gpsPriority, boolean goBackAllowed, String gisMode) {
		this.pageName =pageName;
		this.pageType = pageType;
		this.pageLabelE=Expressor.preCompileExpression(pageLabel);
		this.blockId=id;
		this.hasGPS=hasGPS;
		this.goBackAllowed = goBackAllowed;
		this.gpsPriority = gpsPriority;
		this.gisMode = (gisMode != null && gisMode.equalsIgnoreCase("detailed")) ? "detailed" : "normal";
	}
	public boolean goBackAllowed() {
		return goBackAllowed;
	}
}
