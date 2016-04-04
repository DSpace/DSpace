package org.datadryad.submission;

import org.datadryad.rest.models.Manuscript;

/**
 * The Class EmailParserForManuscriptCentral. Rewritten by Daisie Huang.
 */
public class EmailParserForManuscriptCentral extends EmailParser {
    static {
        // optional XML tags
        fieldToXMLTagMap.put("print issn", Manuscript.ISSN);
        fieldToXMLTagMap.put("publication doi", Manuscript.PUBLICATION_DOI);
        fieldToXMLTagMap.put("online issn",UNNECESSARY);
        fieldToXMLTagMap.put("journal admin email",UNNECESSARY);
        fieldToXMLTagMap.put("journal editor",UNNECESSARY);
        fieldToXMLTagMap.put("journal editor email",UNNECESSARY);
        fieldToXMLTagMap.put("journal embargo period",UNNECESSARY);
        fieldToXMLTagMap.put("editor in chief",UNNECESSARY);
        fieldToXMLTagMap.put("editor in chief email",UNNECESSARY);

        // New fields for MolEcol resources GR Note
        fieldToXMLTagMap.put("article type", "Article_Type");
        fieldToXMLTagMap.put("ms citation title", "Citation_Title");
        fieldToXMLTagMap.put("ms citation authors", "Citation_Authors");

        // Accept 'Article type' for PLoS Biology
        fieldToXMLTagMap.put("article type", "Article_Type");

    }
}
