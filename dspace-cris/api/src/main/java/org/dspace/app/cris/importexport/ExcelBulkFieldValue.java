/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;

public class ExcelBulkFieldValue implements IBulkChangeFieldValue {
	private Cell element;
	private int position;
	private String value;
	private String visibility;

	public static String REGEX_VALUE_AND_VISIBILITY = "\\[.*visibility=([\\w\\p{P}]+).*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_VALUE_AND_VISIBILITY);

	public ExcelBulkFieldValue(Cell element, int position) {
		this.element = element;
		this.position = position;
		String val = element.getContents().split(ExcelBulkField.REGEX_REPEATABLE_SPLIT)[this.position];

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
