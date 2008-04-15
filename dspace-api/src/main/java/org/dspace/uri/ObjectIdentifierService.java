/*
 * ObjectIdentifierService.java
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

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;
import org.dspace.uri.dao.ObjectIdentifierStorageException;
import org.dspace.uri.dao.ExternalIdentifierStorageException;
import org.apache.log4j.Logger;

import java.util.UUID;

/**
 * General static library of methods which offer services to the native identifier mechanism.  It
 * encapsulates access to configuration and to the data access layer, and therefore insulates
 * all calling code dealing with identifiers from having to deal with either of these issues
 *
 * @author Richard Jones
 */
public class ObjectIdentifierService
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(ObjectIdentifierService.class);

    /**
     * Mint the appropriate ObjectIdentifier for the given DSpaceObject
     *
     * @param context
     * @param dso
     * @return
     */
    public static ObjectIdentifier mint(Context context, DSpaceObject dso)
    {
        UUID uuid = UUID.randomUUID();
        ObjectIdentifier oid = new ObjectIdentifier(uuid, dso.getType(), dso.getID());
        dso.setIdentifier(oid);
        return oid;
    }

    /**
     * Mint a SimpleIdentifier.
     *
     * @return
     */
    public static SimpleIdentifier mintSimple()
    {
        UUID uuid = UUID.randomUUID();
        SimpleIdentifier sid = new SimpleIdentifier(uuid);
        return sid;
    }

    /**
     * Get the ObjectIdentifier (if it exists) associated with the given resource
     * type and storage layer id
     *
     * @param context
     * @param type
     * @param id
     * @return
     */
    public static ObjectIdentifier get(Context context, int type, int id)
            throws IdentifierException
    {
        try
        {
            ObjectIdentifierDAO dao = ObjectIdentifierDAOFactory.getInstance(context);
            ObjectIdentifier oid = dao.retrieve(type, id);
            return oid;
        }
        catch (ObjectIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new IdentifierException(e);
        }
    }

    /**
     * Get the ObjectIdentifier (if it exitsts) associated with the given UUID
     * 
     * @param context
     * @param uuid
     * @return
     */
    public static ObjectIdentifier get(Context context, UUID uuid)
            throws IdentifierException
    {
        try
        {
            ObjectIdentifierDAO dao = ObjectIdentifierDAOFactory.getInstance(context);
            ObjectIdentifier oid = dao.retrieve(uuid);
            return oid;
        }
        catch (ObjectIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new IdentifierException(e);
        }
    }
}
