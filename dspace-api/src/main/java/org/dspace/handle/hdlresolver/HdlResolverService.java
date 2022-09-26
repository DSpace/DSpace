/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.hdlresolver;

import java.util.List;

import org.dspace.core.Context;

/**
 * Service used to for utilities involving {@code HdlResolverDTO} and its
 * resolution to handle URI and vice-versa.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
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

    /**
     * List all available prefixes for this installation
     * 
     * @return `List<String>` of Handle prefixes
     */
    List<String> listPrefixes();

    /**
     * List all available handles with `prefix`
     * 
     * @param context DSpace context
     * @param prefix prefix to search
     * @return `List<String>` of handles
     */
    List<String> listHandles(Context context, String prefix);

    /**
     * Verifies status of handle controller
     * 
     * @return `true` if enabled, `false` otherwise
     */
    boolean isListhandlesEnabled();

}
