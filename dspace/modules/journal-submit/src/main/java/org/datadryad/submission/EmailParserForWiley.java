package org.datadryad.submission;

import org.datadryad.rest.models.Manuscript;

/**
 * Modifies EmailParserForManuscriptCentral for certain Wiley journals that use MS Dryad ID for
 * Dryad internal ms numbers instead of the standard MS Reference Number.
 */
public class EmailParserForWiley extends EmailParserForManuscriptCentral {
    static {
        fieldToXMLTagMap.put("ms reference number", UNNECESSARY);
        fieldToXMLTagMap.put("ms dryad id", Manuscript.MANUSCRIPT);
    }
}
