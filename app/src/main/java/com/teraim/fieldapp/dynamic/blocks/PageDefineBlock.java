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
	private boolean goBackAllowed=true;
	private final List<EvalExpr>pageLabelE;


	public String getPageName() {
		return pageName;
	}
	public String getPageType() {
		if (pageType.equals("TableDefaultTemplate"))
			return "PageWithTable";
		return pageType;
	}
	public String getPageLabel() {
		return Expressor.analyze(pageLabelE);
	}
	public boolean hasGPS() {
		return hasGPS;
	}
	
	
	public PageDefineBlock(String id,String pageName,String pageType,String pageLabel,boolean hasGPS, boolean goBackAllowed) {
		this.pageName =pageName;
		this.pageType = pageType;
		this.pageLabelE=Expressor.preCompileExpression(pageLabel);
		this.blockId=id;
		this.hasGPS=hasGPS;
		this.goBackAllowed = goBackAllowed;

	}
	public boolean goBackAllowed() {
		return goBackAllowed;
	}
}
