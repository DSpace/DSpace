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
import jxl.CellFeatures;
import jxl.CellType;
import jxl.LabelCell;
import jxl.format.CellFormat;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ExcelBulkChange implements IBulkChange {
	protected Cell[] row;
	protected List<String> header;
	
	public ExcelBulkChange(Cell[] row, List<String> mainHeaders) {
		this.row = row;
		this.header = mainHeaders;
	}

	@Override
	public String getSourceID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_SOURCEID);
		return row[pos].getContents();
	}

	@Override
	public String getSourceRef() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_SOURCEREF);
		return row[pos].getContents();
	}

	@Override
	public String getCrisID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_CRISID);
		return row[pos].getContents();
	}

	@Override
	public String getUUID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_UUID);
		return row[pos].getContents();
	}

	@Override
	public String getAction() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_ACTION);
		return row[pos].getContents();
	}

	@Override
	public IBulkChangeField getFieldChanges(String field) {
		int index = -1;
		if(this.header.contains(field)) {
			index = this.header.indexOf(field);
			return new ExcelBulkField(row[index]);
		}
		return new ExcelBulkField(new EmptyCell());
	}

	@Override
	public IBulkChangeFieldLink getFieldLinkChanges(String field) {
		int index = -1;
		if(this.header.contains(field)) {
			index = this.header.indexOf(field);
			return new ExcelBulkFieldLink(row[index]);
		}		
		return new ExcelBulkFieldLink(new EmptyCell());
	}

    @Override
    public IBulkChangeFieldPointer getFieldPointerChanges(String field)
    {
        int index = -1;
        if (this.header.contains(field))
        {
            index = this.header.indexOf(field);
            return new ExcelBulkFieldPointer(row[index]);
        }        
        return new ExcelBulkFieldPointer(new EmptyCell());
    }

    @Override
    public boolean isANestedBulkChange()
    {
        return false;
    }

    @Override
    public IBulkChangeFieldFile getFieldFileChanges(String field)
    {
        int index = -1;
        if (this.header.contains(field))
        {
            index = this.header.indexOf(field);
            return new ExcelBulkFieldFile(row[index]);
        }
        return new ExcelBulkFieldFile(new EmptyCell());
    }
    
    class EmptyCell implements LabelCell {

		@Override
		public int getRow() {
			return -1;
		}

		@Override
		public int getColumn() {
			return -1;
		}

		@Override
		public CellType getType() {
			return CellType.EMPTY;
		}

		@Override
		public boolean isHidden() {
			return true;
		}

		@Override
		public String getContents() {
			return "";
		}

		@Override
		public CellFormat getCellFormat() {
			return null;
		}

		@Override
		public CellFeatures getCellFeatures() {
			return null;
		}

		@Override
		public String getString() {
			return "";
		}
    	
    }
}
