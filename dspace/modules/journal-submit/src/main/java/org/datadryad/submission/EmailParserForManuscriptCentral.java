package org.datadryad.submission;

/**
 * The Class EmailParserForManuscriptCentral. Rewritten by Daisie Huang.
 */
public class EmailParserForManuscriptCentral extends EmailParser {
    static {
        // optional XML tags
        fieldToXMLTagMap.put("print issn",ISSN);
        fieldToXMLTagMap.put("online issn","Online_ISSN");
        fieldToXMLTagMap.put("journal admin email","Journal_Admin_Email");
        fieldToXMLTagMap.put("journal editor","Journal_Editor");
        fieldToXMLTagMap.put("journal editor email", "Journal_Editor_Email");
        fieldToXMLTagMap.put("journal embargo period", "Journal_Embargo_Period");
        fieldToXMLTagMap.put("publication doi","Publication_DOI");

        // New fields for MolEcol resources GR Note
        fieldToXMLTagMap.put("article type", "Article_Type");
        fieldToXMLTagMap.put("ms citation title", "Citation_Title");
        fieldToXMLTagMap.put("ms citation authors", "Citation_Authors");

        // Accept 'Article type' for PLoS Biology
        fieldToXMLTagMap.put("article type", "Article_Type");

        fieldToXMLTagMap.put("editor in chief","Editor_In_Chief");
        fieldToXMLTagMap.put("editor in chief email","Editor_In_Chief_Email");
    }
}
