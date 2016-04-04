package org.datadryad.submission;

import org.datadryad.rest.models.Manuscript;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailParserForEcoApp extends EmailParser {
	static {
		// corresponding author tags are parsed separately from the "contact author and address" field
		fieldToXMLTagMap.put("Contact Author and Address", "Corresponding_Author_Address");

		// optional XML fields
		fieldToXMLTagMap.put("journal editor", UNNECESSARY);
		fieldToXMLTagMap.put("journal senior editor", UNNECESSARY);
		fieldToXMLTagMap.put("journal admin email", UNNECESSARY);
		fieldToXMLTagMap.put("journal embargo period", UNNECESSARY);
	}

	@Override
	public void parseSpecificTags() {
		// Some journals convolute CORRESPONDING_AUTHOR and the EMAIL and various address fields; parse this.
		String corr_auth_address = (String) dataForXML.remove("Corresponding_Author_Address");
		if (corr_auth_address != null) {
			Matcher fieldpattern = Pattern.compile("^(.*?)[,;:]\\s*(.*)\\s*$").matcher(corr_auth_address);
			if (fieldpattern.find()) {
				dataForXML.put(Manuscript.CORRESPONDING_AUTHOR, fieldpattern.group(1));
				dataForXML.put(Manuscript.EMAIL, fieldpattern.group(2));
			}
		}
		return;
	}
}
