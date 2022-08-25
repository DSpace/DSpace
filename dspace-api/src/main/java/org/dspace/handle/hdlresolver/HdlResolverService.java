/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.hdlresolver;

import org.dspace.core.Context;

/**
 * Service used to for utilities involving {@code HdlResolverDTO} and its
 * resolution to handle URI and vice-versa.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
public interface HdlResolverService {

    /**
     * Method that creates an <code>HdlResovlerDTO</code> using the requestURI (full
     * requested handle URI) and the path (REST handler URI)
     * 
     * @param requestURI
     * @param path
     * @return <code>HdlResolverDTO</code>
     */
    HdlResolverDTO resolveBy(String requestURI, String path);

    /**
     * Converts the hdlResovler into URL fetching it from repository using the DSpace context
     * 
     * @param context
     * @param hdlResolver
     * @return URL found or null
     */
    String resolveToURL(Context context, HdlResolverDTO hdlResolver);

}
