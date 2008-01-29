/*
 * ExternalIdentifierMint.java
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

import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Richard Jones
 */
public class ExternalIdentifierMint
{
    public static List<ExternalIdentifier> mintAll(Context context, DSpaceObject dso)
    {
        List<ExternalIdentifier> eids = new ArrayList<ExternalIdentifier>();
        IdentifierAssigner[] assigners = (IdentifierAssigner[]) PluginManager.getPluginSequence(IdentifierAssigner.class);
        for (int i = 0; i < assigners.length; i++)
        {
            ExternalIdentifier eid = assigners[i].mint(context, dso);
            eids.add(eid);
        }
        return eids;
    }

    public static ExternalIdentifierType getType(String namespace)
    {
        ExternalIdentifierType[] eits = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        for (int i = 0; i < eits.length; i++)
        {
            if (namespace.equals(eits[i].getNamespace()))
            {
                return eits[i];
            }
        }
        return null;
    }

    public static ExternalIdentifier get(Context context, String namespace, String value)
    {
        ExternalIdentifierType[] eits = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        for (int i = 0; i < eits.length; i++)
        {
            if (namespace.equals(eits[i].getNamespace()))
            {
                ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
                ExternalIdentifier eid = dao.retrieve(eits[i], value);
                return eid;
            }
        }
        return null;
    }

    public static ExternalIdentifier get(Context context, ExternalIdentifierType type, String value, ObjectIdentifier oid)
    {
        ExternalIdentifierType[] eits = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        for (int i = 0; i < eits.length; i++)
        {
            if (type.equals(eits[i]))
            {
                ExternalIdentifier eid = type.getInstance(value, oid);
                return eid;
            }
        }
        return null;
    }

    public static ExternalIdentifier parseCanonicalForm(Context context, String canonicalForm)
    {
        ExternalIdentifier[] eids = (ExternalIdentifier[]) PluginManager.getPluginSequence(ExternalIdentifier.class);
        for (int i = 0; i < eids.length; i++)
        {
            ExternalIdentifier eid = eids[i].parseCanonicalForm(canonicalForm);
            if (eid != null)
            {
                ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
                return dao.retrieve(eid.getType(), eid.getValue());
            }
        }
        return null;
    }

    public static ExternalIdentifier extractURLIdentifier(Context context, String path)
    {
        IdentifierResolver[] eids = (IdentifierResolver[]) PluginManager.getPluginSequence(IdentifierResolver.class);
        for (int i = 0; i < eids.length; i++)
        {
            ExternalIdentifier eid = eids[i].extractURLIdentifier(path);
            if (eid != null)
            {
                ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
                return dao.retrieve(eid.getType(), eid.getValue());
            }
        }
        return null;
    }

    public static DCValue getCanonicalField(ExternalIdentifierType type)
    {
        String cfg = "identifier.metadata.canonical-field." + type.getNamespace();
        DCValue dcv = ConfigurationManager.getMetadataProperty(cfg);
        return dcv;
    }

    public static DCValue getURLField(ExternalIdentifierType type)
    {
        String cfg = "identifier.metadata.url-field." + type.getNamespace();
        DCValue dcv = ConfigurationManager.getMetadataProperty(cfg);
        return dcv;
    }
}
