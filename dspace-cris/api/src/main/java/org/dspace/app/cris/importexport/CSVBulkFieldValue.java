package org.dspace.app.cris.importexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;

public class CSVBulkFieldValue implements IBulkChangeFieldValue {
	private Cell element;
	private int position;
	private String value;
	private String visibility;

	public static String REGEX_VALUE_AND_VISIBILITY = "\\[.*visibility=(.*)\\s?.*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_VALUE_AND_VISIBILITY);

	public CSVBulkFieldValue(Cell element, int position) {
		this.element = element;
		this.position = position;
		String val = element.getContents().split(CSVBulkField.REGEX_REPEATABLE_SPLIT)[this.position];

		Matcher tagmatch = pattern.matcher(val);
		if (tagmatch.find()) {
			this.value = tagmatch.group(2);
			this.visibility = tagmatch.group(1);
		} else {
			this.value = val;
		}

	}

	@Override
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String getVisibility() {
		return visibility;
	}

	public Cell getElement() {
		return element;
	}

}
