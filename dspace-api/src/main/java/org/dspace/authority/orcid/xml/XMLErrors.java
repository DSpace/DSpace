/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.dspace.authority.util.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLErrors {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(XMLErrors.class);

    private static final String ERROR_DESC = "/orcid-message/error-desc";

    /**
     * Evaluates whether a given xml document contains errors or not.
     *
     * @param xml The given xml document
     * @return true if the given xml document is null
     * or if it contains errors
     */
    public static boolean check(Document xml) {

        if (xml == null) {
            return true;
        }

        String textContent = null;

        try {
            textContent = XMLUtils.getTextContent(xml, ERROR_DESC);
        } catch (XPathExpressionException e) {
            log.error("Error while checking for errors in orcid message", e);
        }

        return textContent == null;
    }

    public static String getErrorMessage(Document xml) {

        if (xml == null) {
            return "Did not receive an XML document.";
        }

        String textContent = null;

        try {
            textContent = XMLUtils.getTextContent(xml, ERROR_DESC);
        } catch (XPathExpressionException e) {
            log.error("Error while checking for errors in orcid message", e);
        }

        return textContent;
    }

}
