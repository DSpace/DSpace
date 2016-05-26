/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @param <T> type
 */
public abstract class Converter<T> {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Converter.class);


    protected void processError(Document xml) {
        String errorMessage = XMLErrors.getErrorMessage(xml);
        log.error("The orcid-message reports an error: " + errorMessage);
    }

    public abstract T convert(Document document);
}
