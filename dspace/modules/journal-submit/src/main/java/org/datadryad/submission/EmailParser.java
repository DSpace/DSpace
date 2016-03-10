package org.datadryad.submission;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Address;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.CorrespondingAuthor;
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

    // Parsers that deal with specific and unneeded fields should assign fields to the UNNECESSARY tag:
    public static final String UNNECESSARY = "Unnecessary";

    // The field to XML-tag mapping table.
    protected static Map<String, String> fieldToXMLTagMap = new LinkedHashMap<String,String>();

    static {
        // commonly-used field names for required tags for Manuscript
        fieldToXMLTagMap.put("abstract", Manuscript.ABSTRACT);
        fieldToXMLTagMap.put("journal", Manuscript.JOURNAL);
        fieldToXMLTagMap.put("journal name",Manuscript.JOURNAL);
        fieldToXMLTagMap.put("journal code", Manuscript.JOURNAL_CODE);
        fieldToXMLTagMap.put("article status", Manuscript.ARTICLE_STATUS);
        fieldToXMLTagMap.put("manuscript number", Manuscript.MANUSCRIPT);
        fieldToXMLTagMap.put("ms dryad id", Manuscript.MANUSCRIPT);
        fieldToXMLTagMap.put("ms reference number", Manuscript.MANUSCRIPT);
        fieldToXMLTagMap.put("ms title", Manuscript.ARTICLE_TITLE);
        fieldToXMLTagMap.put("article title", Manuscript.ARTICLE_TITLE);
        fieldToXMLTagMap.put("ms authors", Manuscript.AUTHORS);
        fieldToXMLTagMap.put("all authors", Manuscript.AUTHORS);
        fieldToXMLTagMap.put("classification description", Manuscript.CLASSIFICATION);
        fieldToXMLTagMap.put("keywords", Manuscript.CLASSIFICATION);
        fieldToXMLTagMap.put("contact author", Manuscript.CORRESPONDING_AUTHOR);
        fieldToXMLTagMap.put("contact author email", Manuscript.EMAIL);
        fieldToXMLTagMap.put("contact author address 1", Manuscript.ADDRESS_LINE_1);
        fieldToXMLTagMap.put("contact author address 2", Manuscript.ADDRESS_LINE_2);
        fieldToXMLTagMap.put("contact author address 3", Manuscript.ADDRESS_LINE_3);
        fieldToXMLTagMap.put("contact author city", Manuscript.CITY);
        fieldToXMLTagMap.put("contact author state", Manuscript.STATE);
        fieldToXMLTagMap.put("contact author country", Manuscript.COUNTRY);
        fieldToXMLTagMap.put("contact author zip/postal code", Manuscript.ZIP);

        // commonly-used field names for optional XML tags
        fieldToXMLTagMap.put("ISSN", Manuscript.ISSN);
        fieldToXMLTagMap.put("ms dryad doi", Manuscript.DRYAD_DOI);
        fieldToXMLTagMap.put("contact author orcid", Manuscript.CORRESPONDING_AUTHOR_ORCID);

        // unnecessary fields
        fieldToXMLTagMap.put("Dryad author url", UNNECESSARY);
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

        // set a default status of ACCEPTED:
        dataForXML.put(Manuscript.ARTICLE_STATUS, Manuscript.STATUS_ACCEPTED);

        for (String line : message) {
            if (StringUtils.stripToNull(line) != null) {
                // match field names
                XMLValue thisLine = parseLineToXMLValue(line);
                if (thisLine.key == null) {
                    if (currValue.key != null) {
                        if (currValue.key.equals(Manuscript.ABSTRACT)) {
                            currValue.value = currValue.value + "\n" + thisLine.value;
                        } else {
                            currValue.value = currValue.value + " " + thisLine.value;
                        }
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
        if (dataForXML.get(Manuscript.MANUSCRIPT) != null) {
            dataForXML.put(Manuscript.MANUSCRIPT, dataForXML.get(Manuscript.MANUSCRIPT).split("\\s", 2)[0]);
        }
        if (dataForXML.get(Manuscript.JOURNAL_CODE) != null) {
            dataForXML.put(Manuscript.JOURNAL_CODE, dataForXML.get(Manuscript.JOURNAL_CODE).split("\\s", 2)[0]);
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

        manuscript.setAbstract((String) dataForXML.remove(Manuscript.ABSTRACT));
        String authorstring = (String) dataForXML.remove(Manuscript.AUTHORS);
        manuscript.setAuthorsFromList(parseAuthorList(authorstring));

        manuscript.setKeywords(parseClassificationList((String) dataForXML.remove(Manuscript.CLASSIFICATION)));
        manuscript.setManuscriptId((String) dataForXML.remove(Manuscript.MANUSCRIPT));
        manuscript.setStatus(dataForXML.remove(Manuscript.ARTICLE_STATUS).toLowerCase());
        manuscript.setTitle((String) dataForXML.remove(Manuscript.ARTICLE_TITLE));

        manuscript.getOrganization().organizationCode = dataForXML.remove(Manuscript.JOURNAL_CODE);
        manuscript.getOrganization().organizationName = dataForXML.remove(Manuscript.JOURNAL);

        CorrespondingAuthor correspondingAuthor = manuscript.getCorrespondingAuthor();
        correspondingAuthor.author = new Author((String) dataForXML.remove(Manuscript.CORRESPONDING_AUTHOR));
        correspondingAuthor.email = parseEmailAddress((String) dataForXML.remove(Manuscript.EMAIL));
        correspondingAuthor.author.identifier = (String) dataForXML.remove(Manuscript.CORRESPONDING_AUTHOR_ORCID);
        if (correspondingAuthor.author.identifier != null) {
            correspondingAuthor.author.identifierType = "ORCID";
        }

        correspondingAuthor.address = new Address();
        correspondingAuthor.address.addressLine1 = (String) dataForXML.remove(Manuscript.ADDRESS_LINE_1);
        correspondingAuthor.address.addressLine2 = (String) dataForXML.remove(Manuscript.ADDRESS_LINE_2);
        correspondingAuthor.address.addressLine3 = (String) dataForXML.remove(Manuscript.ADDRESS_LINE_3);
        correspondingAuthor.address.city = (String) dataForXML.remove(Manuscript.CITY);
        correspondingAuthor.address.state = (String) dataForXML.remove(Manuscript.STATE);
        correspondingAuthor.address.country = (String) dataForXML.remove(Manuscript.COUNTRY);
        correspondingAuthor.address.zip = (String) dataForXML.remove(Manuscript.ZIP);
        manuscript.setDryadDataDOI((String) dataForXML.remove(Manuscript.DRYAD_DOI));
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
                // although, if there was only one author and it was listed as lastname, firstname, it'd have a comma too...
                if (authors.length == 2) {
                    authorStrings.add(authors[1] + " " + authors[0]);
                    authors = new String[0];
                }
            }

            for (int i = 0; i < authors.length; i++) {
                if (authors[i] != null) {
                    authorStrings.add(authors[i]);
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
                authorList.add(new Author(s));
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
