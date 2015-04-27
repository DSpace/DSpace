package org.dspace.app.cris.importexport;

import java.util.List;

import jxl.Cell;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class CSVBulkChange implements IBulkChange {
	private Cell[] row;
	private List<String> header;
	
	public CSVBulkChange(Cell[] row, List<String> mainHeaders) {
		this.row = row;
		this.header = mainHeaders;
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
		int index = -1;
		if(this.header.contains(field)) {
			index = this.header.indexOf(field);
		}
		return new CSVBulkField(row[index]);
	}

	@Override
	public IBulkChangeFieldLink getFieldLinkChanges(String shortName) {
		
		return null;
	}

}
