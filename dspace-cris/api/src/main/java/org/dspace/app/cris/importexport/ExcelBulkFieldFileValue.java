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

public class ExcelBulkFieldFileValue extends ExcelBulkFieldValue implements IBulkChangeFieldFileValue {

	public static String REGEX_LINK = "\\[.*LOCAL=([\\w\\p{P}]+).*\\](.*)";
	private static Pattern pattern = Pattern.compile(REGEX_LINK);
	public static String REGEX_DELETE = "\\[.*DELETE=([\\w\\p{P}]+).*\\](.*)";
	private static Pattern patterndelete = Pattern.compile(REGEX_DELETE);
	private boolean local = false;
	private boolean delete = false;
	public ExcelBulkFieldFileValue(Cell element, int position) {
		super(element, position);
		String val = element.getContents().split(ExcelBulkField.REGEX_REPEATABLE_SPLIT)[position];
		Matcher tagmatch = pattern.matcher(val);
		if (tagmatch.find()) {
			this.local = Boolean.parseBoolean(tagmatch.group(1));
		} 
        
        Matcher tagmatchd = patterndelete.matcher(val);
        if (tagmatchd.find()) {
            this.delete = Boolean.parseBoolean(tagmatchd.group(1));
        } 
	}

    @Override
    public boolean isLocal()
    {
        return local;
    }

    @Override
    public boolean isDelete()
    {
        return delete;
    }

}
