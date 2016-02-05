package org.datadryad.submission;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.datadryad.rest.models.Manuscript;

/**
 * The Class EmailParserForBmcEvoBio.
 * Modified from EmailParserForManuscriptCentral by pmidford 7 July 2011
 */
public class EmailParserForBmcEvoBio extends EmailParserForManuscriptCentral {
    
    // static block
    static {
        fieldToXMLTagMap.put("Author Name", CORRESPONDING_AUTHOR);
        fieldToXMLTagMap.put("Author Email", EMAIL);
    }
}
