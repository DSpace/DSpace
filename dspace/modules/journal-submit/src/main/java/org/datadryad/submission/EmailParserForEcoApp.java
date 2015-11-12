package org.datadryad.submission;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailParserForEcoApp extends EmailParser {
	static {
		// corresponding author tags are parsed separately from the "contact author and address" field
		fieldToXMLTagMap.put("Contact Author and Address", "Corresponding_Author_Address");

		// optional XML fields
		fieldToXMLTagMap.put("journal editor", "Journal_Editor");
		fieldToXMLTagMap.put("journal senior editor", "Journal_Editor");
		fieldToXMLTagMap.put("journal admin email", "Journal_Editor_Email");
		fieldToXMLTagMap.put("journal embargo period", "Journal_Embargo_Period");

		// unnecessary fields
		fieldToXMLTagMap.put("Dryad author url", UNNECESSARY);
	}

	@Override
	public void parseSpecificTags() {
		// Some journals convolute CORRESPONDING_AUTHOR and the EMAIL and various address fields; parse this.
		String corr_auth_address = (String) dataForXML.remove("Corresponding_Author_Address");
		if (corr_auth_address != null) {
			Matcher fieldpattern = Pattern.compile("^(.*?)[,;:]\\s*(.*)\\s*$").matcher(corr_auth_address);
			if (fieldpattern.find()) {
				dataForXML.put(CORRESPONDING_AUTHOR, fieldpattern.group(1));
				dataForXML.put(EMAIL, fieldpattern.group(2));
			}
		}
		return;
	}
}
