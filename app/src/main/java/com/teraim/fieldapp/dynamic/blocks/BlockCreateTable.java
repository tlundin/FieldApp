package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;

/**
 * Created by Terje on 2016-05-17.
 */
public class BlockCreateTable extends Block {

    private final String name;
    private final String container;
    private final String label;
    private boolean isVisible = true;

    public BlockCreateTable(String id, String type, String tableName, String containerId, String label) {

        this.blockId=id;
        this.name=tableName;
        this.container=containerId;
        this.label=label;
        //set to always true.
        this.isVisible = true;
    }

    public void create(WF_Context myContext) {

        LayoutInflater inflater = (LayoutInflater)myContext.getContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View tableView = inflater.inflate(R.layout.table_view, null);

        Log.d("vortex","creating table.");
        WF_Table myTable = new WF_Table(name, label, isVisible, myContext, tableView);
        Container myContainer;
        if (container!=null) {
            myContainer = myContext.getContainer(container);
            if (myContainer!=null) {
                myContainer.add(myTable);
                myContext.addTable(myTable);
                Log.d("vortex","table "+name+" added to container "+container);
                return;
            }
        }
        Log.e("vortex","failed to find container "+container);
        o = GlobalState.getInstance().getLogger();
        o.addRow("");
        o.addRedText("Could not attach table "+name+" since container "+container+" doesn't exist.");
    }
}
