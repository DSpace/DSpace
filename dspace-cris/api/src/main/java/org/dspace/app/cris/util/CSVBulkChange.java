package org.dspace.app.cris.util;

import jxl.Cell;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.importexport.IBulkChange;
import org.dspace.app.cris.importexport.IBulkChangeField;
import org.dspace.app.cris.importexport.IBulkChangeFieldLink;

public class CSVBulkChange implements IBulkChange {
	private Cell[] row;
	
	public CSVBulkChange(Cell[] row) {
		this.row = row;
	}

	@Override
	public String getSourceID() {
		int pos = ArrayUtils.indexOf(CSVBulkChanges.HEADER_COLUMNS,CSVBulkChanges.HEADER_SOURCEID);
		return row[pos].getContents();
	}

	@Override
	public String getSourceRef() {
		int pos = ArrayUtils.indexOf(CSVBulkChanges.HEADER_COLUMNS,CSVBulkChanges.HEADER_SOURCEREF);
		return row[pos].getContents();
	}

	@Override
	public String getCrisID() {
		int pos = ArrayUtils.indexOf(CSVBulkChanges.HEADER_COLUMNS,CSVBulkChanges.HEADER_CRISID);
		return row[pos].getContents();
	}

	@Override
	public String getUUID() {
		int pos = ArrayUtils.indexOf(CSVBulkChanges.HEADER_COLUMNS,CSVBulkChanges.HEADER_UUID);
		return row[pos].getContents();
	}

	@Override
	public String getAction() {
		int pos = ArrayUtils.indexOf(CSVBulkChanges.HEADER_COLUMNS,CSVBulkChanges.HEADER_ACTION);
		return row[pos].getContents();
	}

	@Override
	public IBulkChangeField getFieldChanges(String field) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBulkChangeFieldLink getFieldLinkChanges(String shortName) {
		// TODO Auto-generated method stub
		return null;
	}

}
