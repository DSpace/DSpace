/*
 * EPersonDAOCore.java
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
package org.dspace.eperson.dao;

import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson.EPersonMetadataField;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ObjectIdentifierService;

/**
 * @author James Rutherford
 */
public class EPersonDAOCore extends EPersonDAO
{
    public EPersonDAOCore(Context context)
    {
        super(context);
    }

    public EPerson create() throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        EPerson eperson = childDAO.create();

        ObjectIdentifier oid = ObjectIdentifierService.mint(context, eperson);

        update(eperson);

        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                    + eperson.getID()));

        return eperson;
    }

    public EPerson retrieve(int id)
    {
        EPerson eperson = (EPerson) context.fromCache(EPerson.class, id);

        if (eperson == null)
        {
            eperson = childDAO.retrieve(id);
        }

        return eperson;
    }

    public EPerson retrieve(EPersonMetadataField field, String value)
    {
        if ((field != EPersonMetadataField.EMAIL) &&
                (field != EPersonMetadataField.NETID))
        {
            throw new IllegalArgumentException(field + " isn't allowed here");
        }

        if (value == null || "".equals(value))
        {
            return null;
        }

        return childDAO.retrieve(field, value);
    }

    public void update(EPerson eperson) throws AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization() && (
                    (context.getCurrentUser() == null) ||
                    !eperson.equals(context.getCurrentUser())))
        {
            AuthorizeManager.authorizeAction(context, eperson, Constants.WRITE);
        }

        // deal with the item identifier/uuid
        ObjectIdentifier oid = eperson.getIdentifier();
        if (oid == null)
        {
            oid = ObjectIdentifierService.mint(context, eperson);
        }
        oidDAO.update(eperson.getIdentifier());

        log.info(LogManager.getHeader(context, "update_eperson",
                "eperson_id=" + eperson.getID()));

        childDAO.update(eperson);
    }

    public void delete(int id) throws AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        EPerson eperson = retrieve(id);

        context.removeCached(eperson, id);

        // remove the object identifier
        oidDAO.delete(eperson);

        log.info(LogManager.getHeader(context, "delete_eperson",
                "eperson_id=" + id));

        childDAO.delete(id);
    }

    public List<EPerson> search(String query)
    {
        return search(query, -1, -1);
    }

    public List<EPerson> search(String query, int offset, int limit)
    {
        if (limit == 0)
        {
            return new ArrayList<EPerson>();
        }

        if (query == null || "".equals(query))
        {
            List<EPerson> epeople = getEPeople();

            if ((offset > -1) || (limit > -1))
            {
                int toIndex = epeople.size();

                if (offset < 0)
                {
                    offset = 0;
                }
                if (limit != -1)
                {
                    // If the limit is set to -1 that means there is no limit,
                    // and we use the toIndex from above, otherwise we just add
                    // the limit to the offset to get the toIndex.
                    if ((offset + limit) <= epeople.size())
                    {
                        toIndex = offset + limit;
                    }
                }

                return epeople.subList(offset, toIndex);
            }
            else
            {
                return epeople;
            }
        }

        return childDAO.search(query, offset, limit);
    }
}
