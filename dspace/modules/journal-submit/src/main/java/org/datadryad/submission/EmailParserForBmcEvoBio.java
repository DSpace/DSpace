package org.datadryad.submission;

import org.datadryad.rest.models.Manuscript;

/**
 * The Class EmailParserForBmcEvoBio.
 * Modified from EmailParserForManuscriptCentral by pmidford 7 July 2011
 */
public class EmailParserForBmcEvoBio extends EmailParserForManuscriptCentral {
    
    // static block
    static {
        fieldToXMLTagMap.put("Author Name",  Manuscript.CORRESPONDING_AUTHOR);
        fieldToXMLTagMap.put("Author Email",  Manuscript.EMAIL);
    }
}
