/*
 * ExternalIdentifierDAO.java
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
package org.dspace.uri.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.dao.postgres.ExternalIdentifierDAOPostgres;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;

/**
 * @author James Rutherford
 */
public abstract class ExternalIdentifierDAO
{
    protected Logger log = Logger.getLogger(ExternalIdentifierDAOPostgres.class);

    protected Context context;

    protected final ExternalIdentifier[] pids = (ExternalIdentifier[])
            PluginManager.getPluginSequence(ExternalIdentifier.class);

    /**
     * Creates a persistent identifier of the defualt type.
     */
    public ExternalIdentifier create(DSpaceObject dso)
    {
        // The default is the first entry in the list.
        return create(dso, "", pids[0].getType());
    }

    public ExternalIdentifier create(DSpaceObject dso, String canonicalForm)
    {
        Object[] bits = parseCanonicalForm(canonicalForm);
        ExternalIdentifierType type = (ExternalIdentifierType) bits[0];
        String value = (String) bits[1];

        return create(dso, value, type);
    }

    public abstract ExternalIdentifier create(DSpaceObject dso, String value,
            ExternalIdentifierType type);

    public abstract ExternalIdentifier retrieve(String canonicalForm);
    public abstract ExternalIdentifier retrieve(ExternalIdentifierType type,
            String value);

    public abstract List<ExternalIdentifier> getExternalIdentifiers(DSpaceObject dso);

    public List<ExternalIdentifier>
        getExternalIdentifiers(ExternalIdentifierType type)
    {
        return getExternalIdentifiers(type, "");
    }

    public abstract List<ExternalIdentifier>
        getExternalIdentifiers(ExternalIdentifierType type, String prefix);

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * Returns an instantiated ExternalIdentifier of the desired type that
     * references the desired DSpaceObject.
     */
    protected ExternalIdentifier getInstance(DSpaceObject dso,
            ExternalIdentifierType type, String value)
    {
        try
        {
            ExternalIdentifier identifier = null;

            for (ExternalIdentifier pid : pids)
            {
                if (type.equals(pid.getType()))
                {
                    Class pidClass = pid.getClass();
                    Constructor c = pidClass.getDeclaredConstructor(
                            Context.class, DSpaceObject.class,
                            ExternalIdentifierType.class, String.class);
                    identifier = (ExternalIdentifier)
                        c.newInstance(context, dso, type, value);
                    break;
                }
            }

            if (identifier == null)
            {
                throw new RuntimeException("Not a valid identifier type.");
            }

            return identifier;
        }
        catch (NoSuchMethodException nsme)
        {
            throw new RuntimeException(nsme);
        }
        catch (InstantiationException ie)
        {
            throw new RuntimeException(ie);
        }
        catch (IllegalAccessException iae)
        {
            throw new RuntimeException(iae);
        }
        catch (InvocationTargetException ite)
        {
            throw new RuntimeException(ite);
        }
    }

    protected Object[] parseCanonicalForm(String canonicalForm)
    {
        canonicalForm = canonicalForm.trim();

        int pos = canonicalForm.indexOf(":");
        if (pos == -1)
        {
            throw new RuntimeException(canonicalForm + " isn't canonical form!");
        }

        ExternalIdentifierType type = null;
        String namespace = canonicalForm.substring(0, pos);
        String value = canonicalForm.substring(pos + 1);

        Object[] types =
                PluginManager.getPluginSequence(ExternalIdentifierType.class);
        if (types != null)
        {
            for (ExternalIdentifierType t : (ExternalIdentifierType[]) types)
            {
                if (t.getNamespace().equals(namespace))
                {
                    type = t;
                    break;
                }
            }
        }

        if (type == null)
        {
            return null;
//            throw new RuntimeException(namespace + " not supported");
        }

        // FIXME: This is filthy and horrid, but since java doesn't have
        // tuples, what's a guy to do?
        Object[] array = {type, value}; // Nooooooooooo!

        return array;
    }

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
