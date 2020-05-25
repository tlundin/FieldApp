package com.teraim.fieldapp.dynamic.types;

public class ColumnDescriptor {

	public final String colName;
    public final boolean isHeader;

    public ColumnDescriptor(String name, boolean canEdit, boolean isHeader, boolean isOutput) {
		this.colName=name;
        this.isHeader=isHeader;
    }
}
