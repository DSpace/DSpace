/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import org.dspace.identifier.service.DOIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Extends the DOIURIGenerator but uses handles as fallback to DOIs.
 * @author pbecker
 */
public class DOIHandleURIGenerator
extends DOIURIGenerator
implements URIGenerator
{
    protected static URIGenerator fallback;

    @Required
    public static void setFallback(URIGenerator fallback) {
        DOIURIGenerator.fallback = fallback;
    }
    
    @Autowired(required=true)
    public void setDoiService(DOIService doiService) {
        this.doiService = doiService;
    }
    
}
