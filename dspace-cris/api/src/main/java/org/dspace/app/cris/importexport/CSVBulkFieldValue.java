package org.dspace.app.cris.importexport;

import java.util.regex.Pattern;

import jxl.Cell;

public class CSVBulkFieldValue implements IBulkChangeFieldValue {
	private Cell element;
	private int position;
	
	public static String REGEX_VALUE_AND_VISIBILITY = "(.*)###(.*)";
	private static Pattern pattern = Pattern.compile(REGEX_VALUE_AND_VISIBILITY); 
	
	public CSVBulkFieldValue(Cell element, int position) {
		this.element = element;
		this.position = position;
	}

	@Override
	public String getValue() {
		return getInternalValue(element.getContents().split(CSVBulkField.REGEX_REPEATABLE_SPLIT)[this.position]);
	}
	
	protected String getInternalValue(String string) {
		return CSVBulkField.match(string, pattern, 1);
	}

	@Override
	public String getVisibility() {
		return getInternalVisibility(element.getContents().split(CSVBulkField.REGEX_REPEATABLE_SPLIT)[this.position]);
	}

	protected String getInternalVisibility(String string) {
		return CSVBulkField.match(string, pattern, 2);
	}
}
