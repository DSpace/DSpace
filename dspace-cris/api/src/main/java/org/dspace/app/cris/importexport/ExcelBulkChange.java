/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.dspace.app.cris.util.UtilsXLS;

public class ExcelBulkChange implements IBulkChange {
	protected Row row;
	protected List<String> header;
	
	public ExcelBulkChange(Row row, List<String> mainHeaders) {
		this.row = row;
		this.header = mainHeaders;
	}

	@Override
	public String getSourceID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_SOURCEID);
		if (row.getCell(pos) != null)
			return UtilsXLS.stringCellValue(row.getCell(pos));
		else
			return "";
	}

	@Override
	public String getSourceRef() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_SOURCEREF);
		if (row.getCell(pos) != null)
			return UtilsXLS.stringCellValue(row.getCell(pos));
		else
			return "";
	}

	@Override
	public String getCrisID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_CRISID);
		if (row.getCell(pos) != null)
			return UtilsXLS.stringCellValue(row.getCell(pos));
		else
			return "";
	}

	@Override
	public String getUUID() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_UUID);
		if (row.getCell(pos) != null)
			return UtilsXLS.stringCellValue(row.getCell(pos));
		else
			return "";
	}

	@Override
	public String getAction() {
		int pos = ArrayUtils.indexOf(ExcelBulkChanges.HEADER_COLUMNS,ExcelBulkChanges.HEADER_ACTION);
		if (row.getCell(pos) != null)
			return UtilsXLS.stringCellValue(row.getCell(pos));
		else
			return "";
	}

	@Override
	public IBulkChangeField getFieldChanges(String field) {
		int index = -1;
		if(this.header.contains(field)) {
			index = this.header.indexOf(field);
			if (index < row.getLastCellNum() + 1 && row.getCell(index) != null)
				return new ExcelBulkField(row.getCell(index));
			else
				return new ExcelBulkField(new EmptyCell());
		}
		return new ExcelBulkField(new EmptyCell());
	}

	@Override
	public IBulkChangeFieldLink getFieldLinkChanges(String field) {
		int index = -1;
		if(this.header.contains(field)) {
			index = this.header.indexOf(field);
			if (index < row.getLastCellNum() + 1 && row.getCell(index) != null)
			 	return new ExcelBulkFieldLink(row.getCell(index));
			else
				return new ExcelBulkFieldLink(new EmptyCell());
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
            if (index < row.getLastCellNum() + 1 && row.getCell(index) != null)
            	return new ExcelBulkFieldPointer(row.getCell(index));
            else
				return new ExcelBulkFieldPointer(new EmptyCell());
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
            if (row.getCell(index) != null)
            	return new ExcelBulkFieldFile(row.getCell(index));
            else
            	return new ExcelBulkFieldFile(new EmptyCell());
        }
        return new ExcelBulkFieldFile(new EmptyCell());
    }
    
    class EmptyCell implements Cell {
    	private XSSFRichTextString emptyString = new XSSFRichTextString();
    	
		@Override
		public int getColumnIndex() {
			return -1;
		}

		@Override
		public int getRowIndex() {
			return -1;
		}

		@Override
		public Sheet getSheet() {
			return null;
		}

		@Override
		public Row getRow() {
			return null;
		}

		@Override
		public void setCellType(int cellType) {
		}

		@Override
		public int getCellType() {
			return CELL_TYPE_BLANK;
		}

		@Override
		public int getCachedFormulaResultType() {
			return CELL_TYPE_STRING;
		}

		@Override
		public void setCellValue(double value) {
		}

		@Override
		public void setCellValue(Date value) {
		}

		@Override
		public void setCellValue(Calendar value) {
		}

		@Override
		public void setCellValue(RichTextString value) {
		}

		@Override
		public void setCellValue(String value) {
		}

		@Override
		public void setCellFormula(String formula) throws FormulaParseException {
		}

		@Override
		public String getCellFormula() {
			return null;
		}

		@Override
		public double getNumericCellValue() {
			return 0;
		}

		@Override
		public Date getDateCellValue() {
			return null;
		}

		@Override
		public RichTextString getRichStringCellValue() {
			return emptyString;
		}

		@Override
		public String getStringCellValue() {
			return "";
		}

		@Override
		public void setCellValue(boolean value) {
		}

		@Override
		public void setCellErrorValue(byte value) {
		}

		@Override
		public boolean getBooleanCellValue() {
			throw new IllegalStateException("EmptyCell is a not a boolean.");
		}

		@Override
		public byte getErrorCellValue() {
			throw new IllegalStateException("EmptyCell has no error cell value associated.");
		}

		@Override
		public void setCellStyle(CellStyle style) {
		}

		@Override
		public CellStyle getCellStyle() {
			// breaks rule: the cell's style. Always not-null. 
			return null;
		}

		@Override
		public void setAsActiveCell() {
		}

		@Override
		public void setCellComment(Comment comment) {
		}

		@Override
		public Comment getCellComment() {
			return null;
		}

		@Override
		public void removeCellComment() {
		}

		@Override
		public Hyperlink getHyperlink() {
			return null;
		}

		@Override
		public void setHyperlink(Hyperlink link) {			
		}
    }
}
