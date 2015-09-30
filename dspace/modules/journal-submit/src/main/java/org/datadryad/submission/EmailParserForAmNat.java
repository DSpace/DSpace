package org.datadryad.submission;

import org.apache.log4j.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

/**
 * The Class EmailParserForAmNat. Rewritten by Daisie Huang
 * 
 * @author Akio Sone
 * @author Daisie Huang
 */
public class EmailParserForAmNat extends EmailParser {

	static {
		// corresponding author information is parsed in a separate chunk
		fieldToXMLTagMap.put("first name", "Corr_Auth_First_Name");
		fieldToXMLTagMap.put("middle name", "Corr_Auth_Middle_Name");
		fieldToXMLTagMap.put("last name", "Corr_Auth_Last_Name");
		fieldToXMLTagMap.put("Address Line 1", ADDRESS_LINE_1);
		fieldToXMLTagMap.put("Address Line 2", ADDRESS_LINE_2);
		fieldToXMLTagMap.put("Address Line 3", ADDRESS_LINE_3);
		fieldToXMLTagMap.put("City",CITY);
		fieldToXMLTagMap.put("State",STATE);
		fieldToXMLTagMap.put("Country",COUNTRY);
		fieldToXMLTagMap.put("Zip",ZIP);
		fieldToXMLTagMap.put("E-mail Address",EMAIL);


		// AmNat gives us a lot of unnecessary fields.
		fieldToXMLTagMap.put("Access Type",UNNECESSARY);
		fieldToXMLTagMap.put("ADDITIONAL MANUSCRIPT DETAILS",UNNECESSARY);
		fieldToXMLTagMap.put("Address Line 4",UNNECESSARY);
		fieldToXMLTagMap.put("Article Type",UNNECESSARY);
		fieldToXMLTagMap.put("Co-Author",UNNECESSARY);
		fieldToXMLTagMap.put("Color figures",UNNECESSARY);
		fieldToXMLTagMap.put("CORRESPONDING AUTHOR INFORMATION",UNNECESSARY);
		fieldToXMLTagMap.put("Date Final Disposition Set",UNNECESSARY);
		fieldToXMLTagMap.put("Date Revision Submitted",UNNECESSARY);
		fieldToXMLTagMap.put("Degree",UNNECESSARY);
		fieldToXMLTagMap.put("Department",UNNECESSARY);
		fieldToXMLTagMap.put("Erratum to MS #",UNNECESSARY);
		fieldToXMLTagMap.put("Est. printed pages",UNNECESSARY);
		fieldToXMLTagMap.put("Fax Number",UNNECESSARY);
		fieldToXMLTagMap.put("Figure Number",UNNECESSARY);
		fieldToXMLTagMap.put("Figures",UNNECESSARY);
		fieldToXMLTagMap.put("File Name",UNNECESSARY);
		fieldToXMLTagMap.put("Final Decision Date",UNNECESSARY);
		fieldToXMLTagMap.put("Funding Information",UNNECESSARY);
		fieldToXMLTagMap.put("Hard copy art to come",UNNECESSARY);
		fieldToXMLTagMap.put("Initial Date Submitted",UNNECESSARY);
		fieldToXMLTagMap.put("Institution",UNNECESSARY);
		fieldToXMLTagMap.put("ISNI",UNNECESSARY);
		fieldToXMLTagMap.put("Item Description",UNNECESSARY);
		fieldToXMLTagMap.put("Item Type",UNNECESSARY);
		fieldToXMLTagMap.put("Journal URL",UNNECESSARY);
		fieldToXMLTagMap.put("Online-only color figures",UNNECESSARY);
		fieldToXMLTagMap.put("Online-only figures",UNNECESSARY);
		fieldToXMLTagMap.put("Online-only tables",UNNECESSARY);
		fieldToXMLTagMap.put("ORCID Authenticated",UNNECESSARY);
		fieldToXMLTagMap.put("Page charge notes",UNNECESSARY);
		fieldToXMLTagMap.put("Page charge waiver approved",UNNECESSARY);
		fieldToXMLTagMap.put("Page charge waiver requested",UNNECESSARY);
		fieldToXMLTagMap.put("Position",UNNECESSARY);
		fieldToXMLTagMap.put("Primary Phone Number",UNNECESSARY);
		fieldToXMLTagMap.put("Production Notes",UNNECESSARY);
		fieldToXMLTagMap.put("Public Domain",UNNECESSARY);
		fieldToXMLTagMap.put("Publication agreement received",UNNECESSARY);
		fieldToXMLTagMap.put("Publication Date",UNNECESSARY);
		fieldToXMLTagMap.put("Publication Issue Number",UNNECESSARY);
		fieldToXMLTagMap.put("Publication Volume Number",UNNECESSARY);
		fieldToXMLTagMap.put("PubMed Author ID",UNNECESSARY);
		fieldToXMLTagMap.put("Resubmission of MS #",UNNECESSARY);
		fieldToXMLTagMap.put("Revision Number",UNNECESSARY);
		fieldToXMLTagMap.put("Scopus Author ID",UNNECESSARY);
		fieldToXMLTagMap.put("Secondary Phone Number",UNNECESSARY);
		fieldToXMLTagMap.put("Short Title",UNNECESSARY);
		fieldToXMLTagMap.put("Tables",UNNECESSARY);
		fieldToXMLTagMap.put("Text pages",UNNECESSARY);
		fieldToXMLTagMap.put("Title",UNNECESSARY);

		// optional XML fields
		fieldToXMLTagMap.put("ORCID",ORCID);
		fieldToXMLTagMap.put("ResearcherID",RESEARCHER_ID);

	}

	@Override
	public void parseSpecificTags() {
		String firstname = (String) dataForXML.remove("Corr_Auth_First_Name");
		String middlename = (String) dataForXML.remove("Corr_Auth_Middle_Name");
		String lastname = (String) dataForXML.remove("Corr_Auth_Last_Name");
		if (firstname == null) { firstname = ""; }
		if (middlename == null) { middlename = ""; }
		if (lastname == null) { lastname = ""; }
		String corr_author = firstname + " " + middlename + " " + lastname;
		dataForXML.put(CORRESPONDING_AUTHOR, corr_author);
		return;
	}
}
