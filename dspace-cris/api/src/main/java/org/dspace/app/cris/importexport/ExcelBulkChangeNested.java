/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.List;

import jxl.Cell;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ExcelBulkChangeNested extends ExcelBulkChange implements IBulkChangeNested {

	public ExcelBulkChangeNested(Cell[] row, List<String> nestedHeaders)
    {
        super(row, nestedHeaders);
    }

    @Override
	public String getSourceID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_SOURCEID);
		return row[pos].getContents();
	}

	@Override
	public String getSourceRef() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_SOURCEREF);
		return row[pos].getContents();
	}

	@Override
	public String getCrisID() {
	    return "NO_CRISID_SUPPORTED";
	}

	@Override
	public String getUUID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_UUID);
		return row[pos].getContents();
	}

	@Override
	public String getAction() {
		return "NO_ACTION_SUPPORTED";
	}

    @Override
    public String getParentSourceID()
    {
        int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_SOURCEID_PARENT);
        return row[pos].getContents();
    }

    @Override
    public String getParentSourceRef()
    {
        int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_SOURCEREF_PARENT);
        return row[pos].getContents();
    }

    @Override
    public String getParentCrisID()
    {
        int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_NESTED_COLUMNS,ExcelBulkChanges.HEADER_CRISID_PARENT);
        return row[pos].getContents();
    }
    
    @Override
    public boolean isANestedBulkChange()
    {
        return true;
    }
}
