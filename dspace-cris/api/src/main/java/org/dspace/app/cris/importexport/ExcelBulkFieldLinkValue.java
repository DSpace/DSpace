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

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class ExcelBulkFieldLinkValue extends ExcelBulkFieldValue implements IBulkChangeFieldLinkValue {

	public static String REGEX_LINK = "\\[.*URL=([\\w\\p{P}\\p{S}]+).*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_LINK);

	private String linkURL;
	
	public ExcelBulkFieldLinkValue(Cell element, int position) {
		super(element, position);
		String val = element.getContents().split(ExcelBulkField.REGEX_REPEATABLE_SPLIT)[position];
		Matcher tagmatch = pattern.matcher(val);
		if (tagmatch.find()) {
			this.linkURL = tagmatch.group(1);
		} 

	}

	public String getLinkURL() {
		return linkURL;
	}
}
