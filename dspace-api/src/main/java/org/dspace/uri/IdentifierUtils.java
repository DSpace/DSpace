/*
 * IdentifierUtils.java
 *
 * Version: $Revision:$
 *
 * Date: $Date:$
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
package org.dspace.uri;

import org.apache.log4j.Logger;

import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * This class is just a collection of identifier-related utilities.
 */
public class IdentifierUtils
{
    private static Logger log = Logger.getLogger(IdentifierUtils.class);

    /**
     * Given a canonical form URI, we attempt to first associate this with
     * either an internal identifier (mostl likely a UUID), or one of the
     * locally supported external identifiers. If we find a match, we return
     * an ObjectIdentifier that points to the object that was associated with
     * the given URI.
     *
     * @param c Context
     * @param uri The URI in canonical form (eg: hdl:1234/56)
     * @return The ObjectIdentifier corresponding to the given URI
     */
    public static ObjectIdentifier fromString(Context c, String uri)
    {
        ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(c);
        ExternalIdentifier eid = dao.retrieve(uri);
        ObjectIdentifier oid = null;

        if (eid != null)
        {
            oid = eid.getObjectIdentifier();
        }
        else
        {
            oid = ObjectIdentifier.fromString(uri);
        }

        if (oid == null)
        {
            log.warn(LogManager.getHeader(c, "uri_not_found", uri));
        }

        return oid;
    }
}