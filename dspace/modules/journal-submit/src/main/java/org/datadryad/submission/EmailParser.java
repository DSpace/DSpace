package org.datadryad.submission;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Address;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The interface of submission e-mail parsing classes
 * 
 * @author Akio Sone
 * @author Kevin S. Clarke
 * @author Peter E. Midford
 * @author Dan Leehr
 * @author Daisie Huang
 */
public class EmailParser {

	protected static Logger LOGGER = Logger.getLogger(EmailParser.class);

    protected Manuscript manuscript;
    protected Map<String, String> dataForXML = new LinkedHashMap<String, String>();

    // required XML tags that correspond to parts of Manuscript
    public static final String JOURNAL_CODE = "Journal_Code";
    public static final String JOURNAL = "Journal";
    public static final String ABSTRACT = "Abstract";
    public static final String AUTHORS = "Authors";
    public static final String ARTICLE_STATUS = "Article_Status";
    public static final String MANUSCRIPT = "Manuscript_ID";
    public static final String ARTICLE_TITLE = "Article_Title";
    public static final String CORRESPONDING_AUTHOR = "Corresponding_Author";
    public static final String EMAIL = "Email";
    public static final String ADDRESS_LINE_1 = "Address_Line_1";
    public static final String ADDRESS_LINE_2 = "Address_Line_2";
    public static final String ADDRESS_LINE_3 = "Address_Line_3";
    public static final String CITY = "City";
    public static final String STATE = "State";
    public static final String COUNTRY = "Country";
    public static final String ZIP = "Zip";
    public static final String CLASSIFICATION = "Classification";

    // commonly-used XML tags:
    public static final String ISSN = "ISSN";
    public static final String ORCID = "ORCID";
    public static final String RESEARCHER_ID = "Researcher_ID";
    public static final String DRYAD_DOI = "Dryad_DOI";

    // Parsers that deal with specific and unneeded fields should assign fields to the UNNECESSARY tag:
    public static final String UNNECESSARY = "Unnecessary";

    // The field to XML-tag mapping table.
    protected static Map<String, String> fieldToXMLTagMap = new LinkedHashMap<String,String>();

    static {
        // commonly-used field names for required tags for Manuscript
        fieldToXMLTagMap.put("abstract", ABSTRACT);
        fieldToXMLTagMap.put("journal", JOURNAL);
        fieldToXMLTagMap.put("journal name",JOURNAL);
        fieldToXMLTagMap.put("journal code", JOURNAL_CODE);
        fieldToXMLTagMap.put("article status", ARTICLE_STATUS);
        fieldToXMLTagMap.put("manuscript number", MANUSCRIPT);
        fieldToXMLTagMap.put("ms dryad id",MANUSCRIPT);
        fieldToXMLTagMap.put("ms reference number",MANUSCRIPT);
        fieldToXMLTagMap.put("ms title",ARTICLE_TITLE);
        fieldToXMLTagMap.put("article title", ARTICLE_TITLE);
        fieldToXMLTagMap.put("ms authors",AUTHORS);
        fieldToXMLTagMap.put("all authors", AUTHORS);
        fieldToXMLTagMap.put("classification description", CLASSIFICATION);
        fieldToXMLTagMap.put("keywords",CLASSIFICATION);
        fieldToXMLTagMap.put("contact author",CORRESPONDING_AUTHOR);
        fieldToXMLTagMap.put("contact author email",EMAIL);
        fieldToXMLTagMap.put("contact author address 1",ADDRESS_LINE_1);
        fieldToXMLTagMap.put("contact author address 2",ADDRESS_LINE_2);
        fieldToXMLTagMap.put("contact author address 3",ADDRESS_LINE_3);
        fieldToXMLTagMap.put("contact author city",CITY);
        fieldToXMLTagMap.put("contact author state",STATE);
        fieldToXMLTagMap.put("contact author country",COUNTRY);
        fieldToXMLTagMap.put("contact author zip/postal code",ZIP);

        // commonly-used field names for optional XML tags
        fieldToXMLTagMap.put("ISSN", ISSN);
        fieldToXMLTagMap.put("ms dryad doi", DRYAD_DOI);
    }

    /** The Pattern for dryad_ id. */
    /** Valid characters for Manuscript IDs, per DataCite conventions
     * A-Z, a-z, 0-9
     * - (dash)
     * . (dot)
     * _ (underscore)
     * : (colon)
     * / (slash, but only if followed by alphanumeric)
     */

    protected static Pattern Pattern4MS_Dryad_ID = Pattern.compile("(([a-zA-Z0-9-._:]|(/[a-zA-Z0-9]))+)(\\s*)(.*)");

	public void parseMessage(List<String> message) {
        XMLValue currValue = new XMLValue();
        for (String line : message) {
            if (StringUtils.stripToNull(line) != null) {
                // match field names
                XMLValue thisLine = parseLineToXMLValue(line);
                if (thisLine.key == null) {
                    if (currValue.key != null) {
                        currValue.value = currValue.value + " " + thisLine.value;
                        dataForXML.put(currValue.key,currValue.value);
                    }
                } else {
                    currValue = thisLine;
                    dataForXML.put(currValue.key, currValue.value);
                }
            }
        }
        // remove any unnecessary tags
        dataForXML.remove(UNNECESSARY);

        // remove empty tags
        ArrayList<String> removeTags = new ArrayList<String>();
        for (String key : dataForXML.keySet()) {
            if (StringUtils.stripToNull(dataForXML.get(key)) == null) {
                removeTags.add(key);
            }
        }

        for (String key : removeTags) {
            dataForXML.remove(key);
        }

        // some tags should only have one word in them:
        if (dataForXML.get(MANUSCRIPT) != null) {
            dataForXML.put(MANUSCRIPT, dataForXML.get(MANUSCRIPT).split("\\s", 2)[0]);
        }
        if (dataForXML.get(JOURNAL_CODE) != null) {
            dataForXML.put(JOURNAL_CODE, dataForXML.get(JOURNAL_CODE).split("\\s", 2)[0]);
        }
        // for debugging
        for (String s : dataForXML.keySet()) {
            LOGGER.debug(">>" + s + ":" + dataForXML.get(s));
        }

        parseSpecificTags();
        createManuscript();
        return;
    }

    // This method is to be overwritten by subclasses that need to do extra processing of specific tags.
    protected void parseSpecificTags() {
        return;
    }

    private void createManuscript() {
        manuscript = new Manuscript();

        manuscript.manuscript_abstract = (String) dataForXML.remove(ABSTRACT);
        String authorstring = (String) dataForXML.remove(AUTHORS);
        manuscript.authors.author = parseAuthorList(authorstring);

        manuscript.dryadDataDOI = null;
        manuscript.keywords.keyword.addAll(parseClassificationList((String) dataForXML.remove(CLASSIFICATION)));
        manuscript.manuscriptId = (String) dataForXML.remove(MANUSCRIPT);
        manuscript.status = dataForXML.remove(ARTICLE_STATUS).toLowerCase();
        manuscript.title = (String) dataForXML.remove(ARTICLE_TITLE);
        manuscript.publicationDOI = null;
        manuscript.publicationDate = null;
        manuscript.dataReviewURL = null;
        manuscript.dataAvailabilityStatement = null;

        manuscript.organization.organizationCode = dataForXML.remove(JOURNAL_CODE);
        manuscript.organization.organizationName = dataForXML.remove(JOURNAL);

        manuscript.correspondingAuthor.author = parseAuthor((String) dataForXML.remove(CORRESPONDING_AUTHOR));
        manuscript.correspondingAuthor.email = parseEmailAddress((String) dataForXML.remove(EMAIL));

        manuscript.correspondingAuthor.address = new Address();
        manuscript.correspondingAuthor.address.addressLine1 = (String) dataForXML.remove(ADDRESS_LINE_1);
        manuscript.correspondingAuthor.address.addressLine2 = (String) dataForXML.remove(ADDRESS_LINE_2);
        manuscript.correspondingAuthor.address.addressLine3 = (String) dataForXML.remove(ADDRESS_LINE_3);
        manuscript.correspondingAuthor.address.city = (String) dataForXML.remove(CITY);
        manuscript.correspondingAuthor.address.state = (String) dataForXML.remove(STATE);
        manuscript.correspondingAuthor.address.country = (String) dataForXML.remove(COUNTRY);
        manuscript.correspondingAuthor.address.zip = (String) dataForXML.remove(ZIP);
        manuscript.dryadDataDOI = (String) dataForXML.remove(DRYAD_DOI);
        manuscript.optionalProperties = dataForXML;
    }

    public Manuscript getManuscript() {
        return manuscript;
    }

    public void setManuscript(Manuscript ms) {
        manuscript = ms;
    }


    public static String parseEmailAddress(String emailString) {
        String email = emailString;
        if (email != null) {
            String[] emails = emailString.split("[;,]\\s*", 2);
            email = emails[0];
            // sometimes email addresses come in the format "name <email@address>"
            Matcher namepattern = Pattern.compile(".*<(.+@.+)>.*").matcher(emailString);
            if (namepattern.find()) {
                email = namepattern.group(1);
            }
        }
        return email;
    }

    public static Author parseAuthor(String authorString) {
        Author author = new Author();
        // initialize to empty strings, in case there isn't actually anything in the authorString.
        author.givenNames = "";
        author.familyName = "";
        String suffix = "";

        if (authorString != null) {
            authorString = StringUtils.stripToEmpty(authorString);
            // Remove any leading title, like Dr.
            authorString = authorString.replaceAll("^[D|M]+rs*\\.*\\s*","");
            // is there a comma in the name?
            // it could either be lastname, firstname, or firstname lastname, title
            Matcher namepattern = Pattern.compile("^(.+),\\s*(.*)$").matcher(authorString);
            if (namepattern.find()) {
                if (namepattern.group(2).matches("(Jr\\.*|Sr\\.*|III)")) {
                    // if it's a suffix situation, then group 2 will say something like "Jr"
                    // if this is the case, it's actually a firstname lastname situation.
                    // the last name will be the last word in group 1 + ", " + suffix.
                    suffix = ", " + namepattern.group(2);
                    authorString = namepattern.group(1);
                } else if (namepattern.group(2).matches("(Ph|J)\\.*D\\.*|M\\.*[DAS]c*\\.*")) {
                    // if it's a title situation, group 2 will say something like "PhD" or "MD"
                    // there are probably more titles that might happen here, but we can't deal with that.
                    // throw this away.
                    authorString = namepattern.group(1);
                } else {
                    author.givenNames = namepattern.group(2);
                    author.familyName = namepattern.group(1);
                    return author;
                }
            }

            // if it's firstname lastname
            namepattern = Pattern.compile("^(.+) +(.*)$").matcher(authorString);
            if (namepattern.find()) {
                author.givenNames = namepattern.group(1);
                author.familyName = namepattern.group(2) + suffix;
            }
        }
        return author;
    }

    /**
     * Author lists generally come as either last, first; last, first
     * or as first last, first last and first last (sometimes with the Oxford comma before the 'and' token)
     */
    public static List<Author> parseAuthorList (String authorString) {
        ArrayList<Author> authorList = new ArrayList<Author>();

        if (authorString != null) {
            ArrayList<String> authorStrings = new ArrayList<String>();

            // first check to see if the authors are semicolon-separated
            String[] authors = authorString.split("\\s*;\\s*");

            // if it didn't have semicolons, it must have commas
            if (authors.length == 1) {
                authors = authorString.split("\\s*,\\s*");
            }

            // although, if there was only one author and it was listed as lastname, firstname, it'd have a comma too...
            if (authors.length == 2) {
                authorStrings.add(authors[1] + " " + authors[0]);
            } else {
                for (int i = 0; i < authors.length; i++) {
                    if (authors[i] != null) {
                        authorStrings.add(authors[i]);
                    }
                }
            }

            // check to see if the last element was actually two names separated by "and"
            // or is actually the last name prefixed by "and" (because of an Oxford comma)
            String lastElement = authorStrings.remove(authorStrings.size()-1);
            Matcher namepattern = Pattern.compile("^\\s*(.*?)\\s*and\\s+(.*)\\s*$").matcher(lastElement);
            if (namepattern.find()) {
                if (!StringUtils.stripToEmpty(namepattern.group(1)).isEmpty()) {
                    authorStrings.add(namepattern.group(1));
                }
                if (!StringUtils.stripToEmpty(namepattern.group(2)).isEmpty()) {
                    authorStrings.add(namepattern.group(2));
                }
            } else {
                authorStrings.add(lastElement);
            }

            // process these authors
            for (String s : authorStrings) {
                authorList.add(parseAuthor(s));
            }
        }

        return authorList;
    }

    public static List<String> parseClassificationList(String classification) {
        ArrayList<String> classificationList = new ArrayList<String>();

        if (classification != null) {
            // first check to see if the keywords are semicolon-separated
            String[] classifications = classification.split("\\s*;\\s*");

            // if it didn't have semicolons, it must have commas
            if (classifications.length == 1) {
                classifications = classification.split("\\s*,\\s*");
            }

            for (int i = 0; i < classifications.length; i++) {
                if (classifications[i] != null) {
                    classificationList.add(classifications[i]);
                }
            }
        }
        return classificationList;
    }

    // given a line, checks to see if line starts with a defined field. If so, returns an XMLValue with a key and value
    // if there are no regex matches, it must be a line that belongs to the previous key. Return an XMLValue with a null key.
    private XMLValue parseLineToXMLValue (String line) {
        XMLValue result = new XMLValue();
        result.key = null;
        result.value = StringUtils.stripToEmpty(line);

        //first, clean up lines of characters that we don't deal with well, like '*' for formatting
        result.value = result.value.replaceAll("\\*", "");

        //if the line has a URL, replace the colon for now.
        result.value = result.value.replaceAll("://","*");

        Matcher fieldpattern = Pattern.compile("^\\s*(.*?):\\s*(.*)$").matcher(result.value);
        if (fieldpattern.find()) {
            // there is a colon in the line. Let's see if it matches any of our fields.
            for (String field : fieldToXMLTagMap.keySet()) {
                // try regex for each field
                String XMLkey = fieldToXMLTagMap.get(field);

                if (fieldpattern.group(1).toLowerCase().equalsIgnoreCase(field.toLowerCase())) {
                    result.key = XMLkey;
                    result.value = StringUtils.stripToEmpty(fieldpattern.group(2));
                    break;
                }
            }
            if (result.key == null) {
                LOGGER.warn("Found unknown field: <<" + fieldpattern.group(1) + ">>");
            }
        }
        result.value = result.value.replaceAll("\\*","://");
        return result;
    }

    protected class XMLValue {
        String key;
        String value;
    }
}
