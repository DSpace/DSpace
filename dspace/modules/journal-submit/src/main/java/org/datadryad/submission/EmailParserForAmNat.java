package org.datadryad.submission;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class EmailParserForAmNat.
 * 
 * @author Akio Sone
 */
public class EmailParserForAmNat extends EmailParser {
	// static block
	/** The Pattern for email field. */
	static Pattern Pattern4EmailField = Pattern.compile("^[^:]+:");

	/** The list of field names to be matched. */
	static List<String> fieldNameList;

	/** The list of XML tag names corresponding field names. */
	static List<String> xmlTagNameList;

	/** The list of the child tag names under Submission_Metadata tag. */
	static List<String> xmlTagNameMetaSubList;

	/** The list of the child tag names under Authors tag. */
	static List<String> xmlTagNameAuthorSubList;

	/** The field to XML-tag mapping table. */
	static Map<String, String> fieldToXMLTagTable = new LinkedHashMap<String, String>();

	/** The Pattern4 sender email address. */
	static Pattern Pattern4SenderEmailAddress = Pattern
			.compile("(\"[^\"]*\"|)\\s*(<|)([^@]+@[^@>]+)(>|)");

	/** The pattern for separator lines */
	static Pattern Pattern4separatorLine = Pattern
			.compile("^(-|\\+|\\*|/|=|_){2,}+");

	/** The list of mail fields to be excluded */
	static List<String> tagsTobeExcluded;

	/** The set of mail fields to be excluded */
	static Set<String> tagsTobeExcludedSet;

	static {
		fieldNameList = Arrays.asList("JOURNAL", "Journal Code", "ISSN", "Journal URL",
				"SUBMISSION METADATA", "Manuscript Number", "Revision Number",
				"Article Title", "Article Type", "Classification Description",
				"All Authors", "Initial Date Submitted",
				"Date Revision Submitted", "Final Decision Date",
				"Date Final Disposition Set", "Publication Date",
				"Publication Volume Number", "Publication Issue Number",
				"CORRESPONDING AUTHOR INFORMATION", "Title", "First Name",
				"Middle Name", "Last Name", "Degree", "Primary Phone Number",
				"Fax Number", "E-mail Address", "Position", "Department",
				"Institution", "Address Line 1", "Address Line 2",
				"Address Line 3", "Address Line 4", "City", "State", "Zip",
				"Country", "Secondary Phone Number", "Short Title", "ABSTRACT");

		xmlTagNameList = Arrays.asList("Journal", "Journal_Code", "ISSN", "Journal_URL",
				"Submission_Metadata", "Manuscript", "Revision_Number",
				"Article_Title", "Article_Type", "Classification", "Authors",
				"Initial_Date_Submitted", "Date_Revision_Submitted",
				"Final_Decision_Date", "Date_Final_Disposition_Set",
				"Publication_Date", "Publication_Volume_Number",
				"Publication_Issue_Number", "Corresponding_Author", "Title",
				"First_Name", "Middle_Name", "Last_Name", "Degree",
				"Primary_Phone_Number", "Fax_Number", "Email", "Position",
				"Department", "Institution", "Address_Line_1",
				"Address_Line_2", "Address_Line_3", "Address_Line_4", "City",
				"State", "Zip", "Country", "Secondary_Phone_Number",
				"Short_Title", "Abstract");
		xmlTagNameAuthorSubList = Arrays.asList("Title", "First_Name",
				"Middle_Name", "Last_Name", "Degree", "Primary_Phone_Number",
				"Fax_Number", "email", "Position", "Department", "Institution",
				"Address_Line_1", "Address_Line_2", "Address_Line_3",
				"Address_Line_4", "City", "State", "Zip", "Country",
				"Secondary_Phone_Number");
		xmlTagNameMetaSubList = Arrays.asList("Manuscript", "Revision_Number",
				"Article_Title", "Article_Type");

		for (int i = 0; i < fieldNameList.size(); i++) {
			fieldToXMLTagTable.put(fieldNameList.get(i), xmlTagNameList.get(i));
		}

		tagsTobeExcluded = Arrays.asList("ADDITIONAL MANUSCRIPT DETAILS",
				"Text pages", "Est. printed pages", "Figures", "Color figures",
				"Online-only figures", "Online-only color figures",
				"Hard copy art to come", "Tables", "Online-only tables",
				"Page charge waiver requested", "Page charge waiver approved",
				"Page charge notes", "Publication agreement received",
				"Public Domain", "Erratum to MS #", "Resubmission of MS #",
				"SOURCE FILES", "Item Type", "Item Description", "File Name",
				"Appendixes", "Dryad author url");

		tagsTobeExcludedSet = new LinkedHashSet<String>(tagsTobeExcluded);
	}

	/** The field name whose value is to be used for an output file name. */
	static String outputFileField = "Manuscript Number";

	/** The logger setting. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(EmailParserForAmNat.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see submit.util.EmailParser#parseMessage(java.util.List)
	 */
	public ParsingResult parseMessage(List<String> message) {

		LOGGER.trace("***** start of parseEmailMessages() *****");

		int lineCounter = 0;
		String currentField = null;
		String lastField = null;
		String StoredLines = "";

		// String outputFileName = null;
		Map<String, String> dataForXml = new LinkedHashMap<String, String>();
		ParsingResult result = new ParsingResult();
		
		// parse each line
		parsingBlock: for (String line : message) {
			lineCounter++;
			LOGGER.trace(" raw line=" + line);

			// preprocessing before the parsing
			if (line.equals("SUBMISSION METADATA")) {
				// field-terminator-irregularity-fix
				// add a missing colon so that this token can be recognized
				// as an XML tag
				line = line + ": ";
			}
			else if (line.startsWith("SOURCE FILES")) {
				line = line + ": ";
			}
			else if (line
					.equals("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")) {
				// skip this separator line
				continue;
			}
			LOGGER.trace("line after filters=" + line);

			// match field names
			Matcher matcher = Pattern4EmailField.matcher(line);
			// note: url datum ("http:") is not a field token
			if (matcher.find() && !line.startsWith("http")) {
				// field candidate is found
				String matchedField = matcher.toMatchResult().group(0);

				String fieldName = null;

				// remove the separator (":") from this field
				int colonPosition = matchedField.indexOf(":");
				fieldName = matchedField.substring(0, colonPosition);

				// get the value of this field excluding ":"
				String fieldValue = line.substring(colonPosition + 1).trim();
				// processing block applicable only for the first line of
				// a new e-mail message
				if (fieldName.equals("From")) {
					Matcher me = Pattern4SenderEmailAddress.matcher(fieldValue);
					if (me.find()) {
						LOGGER.trace("how many groups=" + me.groupCount());
						LOGGER.trace("email address captured:" + me.group(3));
						result.setSenderEmailAddress(me.group(3));
					}

				}

				if (!fieldToXMLTagTable.containsKey(fieldName)) {
					// this field or field-look-like is not saved
					LOGGER.trace(fieldName
							+ " does not belong to the inclusion-tag-set");
					if (!tagsTobeExcludedSet.contains(fieldName)) {
						StoredLines = StoredLines + " " + line;
						LOGGER.trace("new stored line=" + StoredLines);
					}
					else {
						LOGGER.trace("\t*** line [" + line + "] is skipped");
					}
				}
				else {
					// this field is to be saved

					// new field is detected; if stored lines exist,
					// they should be saved now
					lastField = currentField;
					if (!StoredLines.equals("")) {
						// continuous lines were stored for the previous
						// field before
						// save these store lines for the last field
						if (fieldToXMLTagTable.containsKey(lastField)) {
							dataForXml.put(fieldToXMLTagTable.get(lastField),
									StoredLines);
							LOGGER.trace("lastField to be saved=" + lastField);
							LOGGER.trace("its value=" + StoredLines);
						}
						// clear the line-storage object
						StoredLines = "";
					}
					currentField = fieldName;
					LOGGER.trace("\tnew fieldName is: " + fieldName);
					LOGGER.trace("\tcurrent Field Name = " + currentField);
					if (currentField.equals("Abstract")) {
						LOGGER.info("reached last pasring field: "
								+ currentField);
					}

					LOGGER.trace(fieldToXMLTagTable.get(fieldName) + "="
							+ fieldValue);
					// if the field is outputFileField,
					// i.e., ="Manuscript Number"
					// assign outputFileField as the name for an XML file
					if (fieldName.equals(outputFileField)) {
						result.setSubmissionId(fieldValue);
						// outputFileName = fieldValue + ".xml";
						LOGGER.info("submissionId = " + fieldValue);
                                                Matcher mi = Pattern4MS_Dryad_ID.matcher(fieldValue);
                                                if(!mi.matches()) {
							result.setHasFlawedId(true);
							LOGGER.error("invalid submission id found="
									+ fieldValue);
						}
						else {
							LOGGER.trace("ID is valid");
						}
					}
					// tentatively save the data of this field
					// more lines may follow ...
					dataForXml.put(fieldToXMLTagTable.get(fieldName),
							fieldValue);
					StoredLines = fieldValue;
					LOGGER.trace("tentatively saved value so far:"
							+ StoredLines);
				}
			}
			else {
				// no colon-separated field matched
				// non-1st lines
				// append this line to the storage object (StoredLines)
				if ((line != null)) {

					if (!StoredLines.trim().equals("")) {
						Matcher m3 = Pattern4separatorLine.matcher(line);
						if (m3.find()) {
							LOGGER
									.info("separator line was found; ignore this.");

							if ((currentField != null)
									&& (currentField.equals("ABSTRACT"))) {
								break parsingBlock;
							}
						}
						else {
							StoredLines = StoredLines + "\n" + line;
							LOGGER.trace("StoredLines=" + StoredLines);
						}
					}
					else {
						StoredLines += line;
					}
				}
			}

		} // end of while

		// Exit-processing: if the last matched field is ABSTRACT,
		// its data are not saved and they must be saved here
		LOGGER.trace("currentField=" + currentField);
		if (currentField.equals("ABSTRACT")) {
			dataForXml.put(fieldToXMLTagTable.get("ABSTRACT"), StoredLines);
		}

		LOGGER.trace("***** end of separateEmailMessage() *****");
		result.setSubmissionData(BuildSubmissionDataAsXML(dataForXml));
		return result;
	}

	/**
	 * Builds the submission data as xml.
	 * 
	 * TODO: rewrite this so we're not constructing xml by appending strings!
	 * 
	 * @param emailData the email data
	 * 
	 * @return the string builder
	 */
	StringBuilder BuildSubmissionDataAsXML(Map<String, String> emailData) {
		StringBuilder sb = new StringBuilder();

		Set<Map.Entry<String, String>> keyvalue = emailData.entrySet();

		for (Iterator<Map.Entry<String, String>> it = keyvalue.iterator(); it
				.hasNext();) {
			Map.Entry<String, String> et = it.next();

			if (et.getKey().equals("Submission_Metadata")) {

				sb.append("<Submission_Metadata>\n");
				target1: while (true) {
					Map.Entry<String, String> etx = it.next();
					sb.append("\t<"
							+ etx.getKey()
							+ ">"
							+ getStrippedText(StringUtils
									.stripToEmpty(etx.getValue())) + "</"
							+ etx.getKey() + ">\n");
					if (etx.getKey().equals(
							xmlTagNameMetaSubList.get(xmlTagNameMetaSubList
									.size() - 1))) {
						sb.append("</Submission_Metadata>\n");
						break target1;
					}
				}
			}
			else if (et.getKey().equals("Classification")) {
				sb.append("<Classification>\n");
				String[] keywords = et.getValue().split(";");
				for (String kw : keywords) {
					sb.append("\t<keyword>"
							+ getStrippedText(StringUtils
									.stripToEmpty(kw)) + "</keyword>\n");
				}
				sb.append("</Classification>\n");
			}
			else if (et.getKey().equals("Authors")) {
				sb.append("<Authors>\n");
				String[] authors = et.getValue().split(";");
				for (String el : authors) {
					sb.append("\t<Author>"
							+ flipName(getStrippedText(StringUtils
									.stripToEmpty(el))) + "</Author>\n");
				}
				sb.append("</Authors>\n");
			}
			else if (et.getKey().equals("Corresponding_Author")) {
				sb.append("<Corresponding_Author>\n");
				target: while (true) {
					Map.Entry<String, String> etx = it.next();
					sb.append("\t<"
							+ etx.getKey()
							+ ">"
							+ flipName(getStrippedText(StringUtils
									.stripToEmpty(etx.getValue()))) + "</"
							+ etx.getKey() + ">\n");
					if (etx.getKey().equals(
							xmlTagNameAuthorSubList.get(xmlTagNameAuthorSubList
									.size() - 1))) {
						sb.append("</Corresponding_Author>\n");
						break target;
					}
				}
			}
			else {

				if (StringUtils.stripToEmpty(et.getValue()).equals("")){
                    sb.append("<"+et.getKey()+" />\n");
                } else {
                	sb.append("<"+et.getKey()+">");
                	sb.append(getStrippedText(et.getValue()));
                    sb.append("</"+et.getKey()+">\n");
                }
			}
		} // end of for
		return sb;
	}
}
