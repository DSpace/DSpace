package org.dspace.app.cris.importexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class CSVBulkFieldLinkValue extends CSVBulkFieldValue implements IBulkChangeFieldLinkValue {

	public static String REGEX_LINK = "\\[.*URL=([\\w\\p{P}]+).*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_LINK);

	private String linkURL;
	
	public CSVBulkFieldLinkValue(Cell element, int position) {
		super(element, position);
		String val = element.getContents().split(CSVBulkField.REGEX_REPEATABLE_SPLIT)[position];
		Matcher tagmatch = pattern.matcher(val);
		if (tagmatch.find()) {
			this.linkURL = tagmatch.group(1);
		} 

	}

	public String getLinkURL() {
		return linkURL;
	}
}
