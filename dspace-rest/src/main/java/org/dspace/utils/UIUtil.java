/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.utils;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.utils.DSpace;

public class UIUtil extends Util
{

    /** log4j category */
    public static final Logger log = Logger.getLogger(UIUtil.class);

    /**
     * Get the current collection location, that is, where the user "is". This
     * returns null if there is no collection location, i.e. the location is
     * "all of DSpace" or a community.
     *
     * @param request
     *            current HTTP request
     *
     * @return the current collection location, or null
     */
    public static Collection getCollectionLocation(HttpServletRequest request)
    {
        return ((Collection) request.getAttribute("dspace.collection"));
    }

        /**
     * Get the current community location, that is, where the user "is". This
     * returns <code>null</code> if there is no location, i.e. "all of DSpace"
     * is the location.
     *
     * @param request
     *            current HTTP request
     *
     * @return the current community location, or null
     */
    public static Community getCommunityLocation(HttpServletRequest request)
    {
        return ((Community) request.getAttribute("dspace.community"));
    }

}