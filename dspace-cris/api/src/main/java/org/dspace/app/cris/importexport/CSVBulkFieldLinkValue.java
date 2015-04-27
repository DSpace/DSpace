package org.dspace.app.cris.importexport;

import java.util.regex.Pattern;

import jxl.Cell;

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class CSVBulkFieldLinkValue extends CSVBulkFieldValue implements
		IBulkChangeFieldLinkValue {
	private Cell element;

	public static String REGEX_LINK = "\\[URL=(.*)\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_LINK);
	
	public CSVBulkFieldLinkValue(Cell element, int position) {
		super(element, position);
	}

	@Override
	protected String getInternalValue(String string) {
		return CSVBulkField.match(element.getContents(), pattern, 2);
	}
	
	@Override
	public String getLinkURL() {		
		return CSVBulkField.match(element.getContents(), pattern, 1);
	}
}
