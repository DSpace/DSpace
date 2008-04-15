/*
 * HandleManager.java
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
package org.dspace.uri.handle;

import org.dspace.uri.IdentifierAssigner;
import org.dspace.uri.IdentifierResolver;
import org.dspace.uri.Identifiable;
import org.dspace.uri.IdentifierException;
import org.dspace.uri.handle.dao.HandleDAO;
import org.dspace.uri.handle.dao.HandleDAOFactory;
import org.dspace.uri.handle.dao.HandleStorageException;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Manager class which implements the IdentifierAssigner and IdentifierResolver
 * interfaces allowing it to assign and resolve Handles.
 *
 * @author Richard Jones
 */
public class HandleManager implements IdentifierAssigner<Handle>, IdentifierResolver<Handle>
{
    ///////////////////////////////////////////////////////////////////
    // IdentifierAssigner implementation
    //////////////////////////////////////////////////////////////////

    /**
     * Mint a new identifier for the passed Identifiable.  The identifier is
     * returned, and is NOT assigned to the given Identifiable
     *
     * @param context
     * @param dso
     * @return
     */
    public Handle mint(Context context, Identifiable dso)
            throws IdentifierException
    {
        try
        {
            HandleDAO dao = HandleDAOFactory.getInstance(context);
            int next = dao.getNextHandle();
            String prefix = ConfigurationManager.getProperty("handle.prefix");
            if (prefix == null || "".equals(prefix))
            {
                throw new IdentifierException("no configuration or configuration invalid for handle.prefix");
            }
            String fullHandle = prefix + "/" + Integer.toString(next);

            Handle handle = new Handle(fullHandle, dso.getIdentifier());
            return handle;
        }
        catch (HandleStorageException e)
        {
           throw new IdentifierException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // IdentifierResolver implementation
    ///////////////////////////////////////////////////////////////////

    /**
     * Given the URL path as an argument, extract the portion of the path which
     * is the handle and return an instance of the Handle object
     *
     * @param path
     * @return
     */
    public Handle extractURLIdentifier(String path)
            throws IdentifierException
    {
        String prefix = ConfigurationManager.getProperty("handle.prefix");
        if (prefix == null || "".equals(prefix))
        {
            throw new IdentifierException("no configuration, or configuration invalid for handle.prefix");
        }

        String hdlRX = ".*hdl/(" + prefix + "/[0-9]+).*";
        Pattern p = Pattern.compile(hdlRX);
        Matcher m = p.matcher(path);
        if (!m.matches())
        {
            return null;
        }
        String value = m.group(1);
        Handle handle = new Handle(value);
        return handle;
    }
}
