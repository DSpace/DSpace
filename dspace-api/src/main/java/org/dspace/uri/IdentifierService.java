/*
 * IdentifierService.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.uri;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author Richard Jones
 */
public class IdentifierService
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(IdentifierService.class);

    /** base of the URL path which internal identifiers will be derived from */
    private static final String RESOURCE_PATH_SEGMENT = "resource";

    /**
     * Resolve the passed string to a ResolvableIdentifier object.  This uses a number of mechanisms
     * to attempt to convert the given string into an identifier including:
     *
     * - assuming the string is of canonical form
     * - assuming the string is a url path segment which may contain additional characters
     *
     * If it succeeds in resolving to a valid identifier this is returned.  If not, it returns null
     * @param context
     * @param str
     * @return
     */
    public static ResolvableIdentifier resolve(Context context, String str)
    {
        ResolvableIdentifier dsi = null;

        if (dsi == null)
        {
            dsi = IdentifierService.resolveAsURLSubstring(context, str);
        }

        if (dsi == null)
        {
            dsi = IdentifierService.resolveCanonical(context, str);
        }

        // NOTE: we could add a few more clever tricks in here when the mood takes us

        return dsi;
    }

    /**
     * Resolve to a ResolvableIdentifier the string passed in, assuming that it is a URL
     * path or fragment which may or may not contain additional characters.  Unless it is known
     * for sure that the path fits this description, it is generally better to use
     *
     * <code>IdentifierService.resolve()</code>
     *
     * which calls this method among others it uses for resolution
     *
     * @param context
     * @param path
     * @return
     */
    public static ResolvableIdentifier resolveAsURLSubstring(Context context, String path)
    {
        ObjectIdentifier oi = ObjectIdentifier.extractURLIdentifier(path);
        ExternalIdentifier ei = null;

        if (oi == null)
        {
            ei = ExternalIdentifierService.extractURLIdentifier(context, path);
        }

        if (oi == null && ei == null)
        {
            return null;
        }
        else
        {
            if (oi != null)
            {
                return oi;
            }
            else
            {
                return ei;
            }
        }
    }

    /**
     * Resolve to a ResolvableIdentifier the string passed in, assuming that it is a correct
     * canonical form for one of the external identifier forms supported.  Unless it is known
     * for sure that the canonical form is the one being used it is generally better to use
     *
     * <code>IdentifierService.resolve()</code>
     *
     * which calls this method among others it uses for resolution
     * 
     * @param context
     * @param canonicalForm
     * @return
     */
    public static ResolvableIdentifier resolveCanonical(Context context, String canonicalForm)
    {
        ObjectIdentifier oi = ObjectIdentifier.parseCanonicalForm(canonicalForm);
        ExternalIdentifier ei = null;

        if (oi == null)
        {
            ei = ExternalIdentifierService.parseCanonicalForm(context, canonicalForm);
        }

        if (oi == null && ei == null)
        {
            return null;
        }
        else
        {
            if (oi != null)
            {
                return oi;
            }
            else
            {
                return ei;
            }
        }
    }

    /**
     * Get the system preferred url for the given ResolvableIdentifier.
     *
     * @param dsi
     * @return
     */
    public static URL getURL(ResolvableIdentifier dsi)
    {
        try
        {
            String base = ConfigurationManager.getProperty("dspace.url");
            String urlForm = dsi.getURLForm();

            if (base == null || "".equals(base))
            {
                throw new RuntimeException("No configuration, or configuration invalid for dspace.url");
            }

            if (urlForm == null)
            {
                throw new RuntimeException("Unable to assign URL: no identifier available");
            }

            String url = base + "/" + IdentifierService.RESOURCE_PATH_SEGMENT + "/" + urlForm;

            return new URL(url);
        }
        catch (MalformedURLException e)
        {
            log.error("caught exception: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the native formulation of the URL for the given DSpaceObject.  This will be of the form
     *
     * <code>[dspace.url]/[RESOURCE_PATH_SEGMENT]/uuid/[uuid]</code>
     *
     * For example:
     *
     * <code>http://localhost:8080/resource/uuid/12345678-1234-1234-1234-1234-1234567891011</code>
     *
     * @param dso
     * @return
     */
    public static URL getLocalURL(Identifiable dso)
    {
        URL url = null;
        ObjectIdentifier oid = dso.getIdentifier();
        if (oid == null)
        {
            return null;
        }
        url = IdentifierService.getURL(oid);
        return url;
    }

    /**
     * Get the context path segment of the native URL for the given DSpaceObject.  This will be
     * the path segment after <code>[dspace.url]</code> in <code>getURL()</code>. For example:
     *
     * <code>resource/hdl/123456789/100</code>
     *
     * @param dso
     * @return
     */
    public static String getContextPath(Identifiable dso)
    {
        ResolvableIdentifier ri = IdentifierService.getPreferredIdentifier(dso);
        return IdentifierService.RESOURCE_PATH_SEGMENT + "/" + ri.getURLForm();
    }

    /**
     * Get the preferred url form for the given DSpaceObject.  The actual URL generated will
     * depend on which of the identifiers (both external and native) are most desireable,
     * as well as any other features of the underlying selection mechanism.  The form of
     * the URLs generated cannot be assumed
     *
     * @param dso
     * @return
     */
    public static URL getURL(Identifiable dso)
    {
        ResolvableIdentifier ri = IdentifierService.getPreferredIdentifier(dso);
        return IdentifierService.getURL(ri);
    }

    /**
     * Get the preferred canonical form of the identifier for the given DSpaceObject.  The
     * actual identifier selected will depend on the configuration of most desireable
     * identifier and any other features of the underlying selection mechanism
     *
     * @param dso
     * @return
     */
    public static String getCanonicalForm(Identifiable dso)
    {
        ResolvableIdentifier ri = IdentifierService.getPreferredIdentifier(dso);
        String cf = ri.getCanonicalForm();
        return cf;
    }

    /**
     * Get the preferred identifier for the given DSpace object.  If a preferred
     * external identifier is not found then the native identifier will be used
     * by default
     *
     * @param dso
     * @return
     */
    public static ResolvableIdentifier getPreferredIdentifier(Identifiable dso)
    {
        String ns = ConfigurationManager.getProperty("identifier.preferred-namespace");
        if (!"".equals(ns) && ns != null)
        {
            ExternalIdentifierType type = ExternalIdentifierService.getType(ns);
            List<ExternalIdentifier> eids = dso.getExternalIdentifiers();
            for (ExternalIdentifier eid : eids)
            {
                if (eid.getType().equals(type))
                {
                    return eid;
                }
            }
        }

        ObjectIdentifier oid = dso.getIdentifier();
        return oid;
    }
}
