/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.dav;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdom.Namespace;


/**
 * Container for common constants and other shared static bits. It is only
 * abstract to signify that it never gets instantiated.
 */
abstract class DAV
{
    
    /** Internal value for depth of infinity in PROPFIND, etc. */
    protected static final int DAV_INFINITY = -1;

    /** WebDAV extension HTTP status codes not found in HttpServletResponse. */
    protected static final int SC_MULTISTATUS = 207;

    /** The Constant SC_NO_CONTENT. */
    protected static final int SC_NO_CONTENT = 204;

    /** The Constant SC_CONFLICT. */
    protected static final int SC_CONFLICT = 409;

    /** The Constant SC_LOCKED. */
    protected static final int SC_LOCKED = 423;

    /** The Constant SC_FAILED_DEPENDENCY. */
    protected static final int SC_FAILED_DEPENDENCY = 424;

    /** The Constant SC_INSUFFICIENT_STORAGE. */
    protected static final int SC_INSUFFICIENT_STORAGE = 507;

    /** The Constant SC_UNPROCESSABLE_ENTITY. */
    protected static final int SC_UNPROCESSABLE_ENTITY = 422;

    /** Namespaces of interest.  DAV: is WebDAV's namespace URI. */
    protected static final Namespace NS_DAV = Namespace.getNamespace("DAV:");

    /** DSpace XML namespace (for everything at the moment). */
    protected static final Namespace NS_DSPACE = Namespace.getNamespace(
            "dspace", "http://www.dspace.org/xmlns/dspace");

    /** PROPFIND operation types - our internal convention. */
    protected static final int PROPFIND_PROP = 1;

    /** The Constant PROPFIND_PROPNAME. */
    protected static final int PROPFIND_PROPNAME = 2;

    /** The Constant PROPFIND_ALLPROP. */
    protected static final int PROPFIND_ALLPROP = 3;

    /** PROPPATCH operation types - internal convention dictated by XML names:. */
    protected static final int PROPPATCH_SET = 1;

    /** The Constant PROPPATCH_REMOVE. */
    protected static final int PROPPATCH_REMOVE = 2;

    protected static String applyHttpDateFormat(Date thisDate)
    {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'").format(thisDate);
    }

}
