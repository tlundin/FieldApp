package com.teraim.fieldapp.dynamic.types;

import androidx.fragment.app.Fragment;
import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.Block;
import com.teraim.fieldapp.dynamic.blocks.PageDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.StartBlock;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Workflow
public class Workflow implements Serializable {
	private static final long serialVersionUID = -8806673639097744372L;
	private List<Block> blocks;
	private String name;
	private transient DB_Context mContext=null;
	private int blockP = 0;
	private transient PageDefineBlock myPDefBl = null;
	private transient boolean called = false;
	public List<Block> getBlocks() {
		return blocks;
	}
	public List<Block> getCopyOfBlocks() {
		if (blocks==null)
			return null;
		return new ArrayList<Block>(blocks);
	}
	public void saveBlockPointer(int blockP) {
		this.blockP=blockP;
	}
	public void setPageDefineBlock(PageDefineBlock p) {
		myPDefBl = p;
	}
	public int getBlockPointer() {
		return blockP;
	}
	public void addBlocks(List<Block> _blocks) {
		blocks = _blocks;
	}
	public String getName() {
		if (name==null) {
			if (blocks!=null && blocks.size()>0)
				name = ((StartBlock)blocks.get(0)).getName();

		}
		return name;
	}
	public String getLabel() {
		if (myPDefBl==null)
			getMyPageDefineBlock();
		if (myPDefBl!=null)
			return myPDefBl.getPageLabel();
		return null;
	}
	public Fragment createFragment(String templateName) {
		Fragment f = null;
		try {
			Class<?> cs = Class.forName("com.teraim.fieldapp.dynamic.templates."+templateName);
			f = (Fragment)cs.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e3) {
			e3.printStackTrace();
		}
		return f;
	}
	public String getTemplate() {
		if (myPDefBl==null)
			getMyPageDefineBlock();
		if (myPDefBl!=null)
			return myPDefBl.getPageType();
		Log.d("vortex", "Could not find a PageDefineBlock for workflow " + this.getName());
		return null;
	}



	public PageDefineBlock getMyPageDefineBlock() {
		if (called)
			return myPDefBl;
			Log.d("vortex","In getmypagedefine with "+blocks.size()+" blocks.");
			for (Block b : blocks) {
				if (b instanceof PageDefineBlock) {
					myPDefBl = (PageDefineBlock) b;

				}
			}
		called = true;
		return myPDefBl;
	}




	public List<EvalExpr> getContext() {
		if (blocks!=null && blocks.size()>0) {
			StartBlock bl = ((StartBlock)blocks.get(0));
			if (bl==null) {
				Log.e("vortex","Missing Startblock...context will remain same.");
				return null;
			}
			else
				return bl.getWorkFlowContext();
		} 

		Log.e("vortex","startblock missing");
		return null;
	}

	
}





