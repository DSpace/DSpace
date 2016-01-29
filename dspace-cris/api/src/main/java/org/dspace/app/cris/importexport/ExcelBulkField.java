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

public class ExcelBulkField implements IBulkChangeField {

	private Cell element;

	public static String REGEX_REPEATABLE_SPLIT = "\\|\\|\\|";

	public ExcelBulkField(Cell element) {
		this.element = element;
	}

	@Override
	public int size() {
		return element.getContents().split(REGEX_REPEATABLE_SPLIT).length;
	}

	@Override
	public IBulkChangeFieldValue get(int y) {
		return new ExcelBulkFieldValue(element, y);
	}

	public static String match(String value, Pattern pattern, int match) {
		Matcher tagmatch = pattern.matcher(value);
		while (tagmatch.find()) {
			return tagmatch.group(match);
		}
		return value;
	}

	public Cell getElement() {
		return element;
	}

}
