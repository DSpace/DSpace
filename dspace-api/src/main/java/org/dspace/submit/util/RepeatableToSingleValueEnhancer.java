/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.submit.lookup.EnhancerSubmissionLookup;

public class RepeatableToSingleValueEnhancer implements
		EnhancerSubmissionLookup {
	private String separator = ",";
	private boolean separatorWhitespaceAfter = true;
	private List<String> singleValues;

	@Override
	public List<String> getValues(SubmissionLookupPublication publication) {
		List<String> values = new ArrayList<String>();
		for (String s : singleValues) {
			if (publication.getValues(s) != null) {
				values.addAll(publication.getValues(s));
			}
		}
		if (values.size() > 0) {
			String v = StringUtils.join(values.iterator(), separator
					+ (separatorWhitespaceAfter ? " " : ""));
			List<String> res = new ArrayList<String>();
			res.add(v);
			return res;
		} else {
			return null;
		}

	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
	    if ("\\n".equals(separator))
	    {
	        this.separator = "\n";
	    }
	    else
	    {
	        this.separator = separator;
	    }
	}

	public boolean isSeparatorWhitespaceAfter() {
		return separatorWhitespaceAfter;
	}

	public void setSeparatorWhitespaceAfter(boolean separatorWhitespaceAfter) {
		this.separatorWhitespaceAfter = separatorWhitespaceAfter;
	}

	public List<String> getSingleValues() {
		return singleValues;
	}

	public void setSingleValues(List<String> singleValues) {
		this.singleValues = singleValues;
	}
}
