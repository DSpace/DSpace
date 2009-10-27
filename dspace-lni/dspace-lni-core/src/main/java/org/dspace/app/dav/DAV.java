/*
 * DAV.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.dav;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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

    /** HTTP standard date format. */
    protected static final DateFormat HttpDateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss 'GMT'");
}
