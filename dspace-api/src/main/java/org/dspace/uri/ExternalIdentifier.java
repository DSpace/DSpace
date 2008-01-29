/*
 * ExternalIdentifier.java
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Richard Jones
 * @author James Rutherford
 */
public abstract class ExternalIdentifier implements ResolvableIdentifier
{
    private static final Logger log = Logger.getLogger(ExternalIdentifier.class);

    protected String value;
    protected ObjectIdentifier oid;
    protected ExternalIdentifierType type;

    public ExternalIdentifier()
    {
        // create a blank identifier to be populated
    }

    /*
    public ExternalIdentifier(ExternalIdentifierType type)
    {
        this.type = type;
    }
*/
    
    public ExternalIdentifier(ExternalIdentifierType type, String value)
    {
        this.type = type;
        this.value = value;
    }

    public ExternalIdentifier(ExternalIdentifierType type, String value, ObjectIdentifier oid)
    {
        // create a real, full size identifier
        this.type = type;
        this.oid = oid;
        this.value = value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public void setObjectIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    public void setType(ExternalIdentifierType type)
    {
        this.type = type;
    }

    /* FIXME: do we need a replacement for this?
    public ExternalIdentifier(Context context, DSpaceObject dso,
            ExternalIdentifierType type, String value)
    {
        this(type);
        // this.context = context;
        this.value = value;

        oid = dso.getIdentifier();
    }*/

    public DSpaceObject getObject(Context context)
    {
        return this.getObjectIdentifier().getObject(context);
    }

    public ObjectIdentifier getObjectIdentifier()
    {
        if (oid == null)
        {
            throw new IllegalStateException("An external identifier with no ObjectIdentifier exists");
        }
        return oid;
    }

    public ExternalIdentifierType getType()
    {
        return type;
    }

    public String getURLForm()
    {
        return type.getNamespace() + "/" + this.value;        
    }

    public URI getURI()
    {
        try
        {
            // eg: http + :// + hdl.handle.net + / + 1234/56
            return new URI(type.getProtocol() + type.getProtocolActivator() + type.getBaseURI() + type.getBaseSeparator() + value);
        }
        catch (URISyntaxException urise)
        {
            throw new RuntimeException(urise);
        }
    }

    public String getCanonicalForm()
    {
        // eg: hdl:1234/56
        return type.getNamespace() + ":" + value;
    }

    public boolean leaveTombstone()
    {
        // default to true unless implementation overrides to the contrary
        return true;
    }

    public abstract ExternalIdentifier parseCanonicalForm(String canonical);

    /*
    public static ExternalIdentifier parseCanonicalForm(String canonical)
    {
        // first find out if it's the right form
        int colon = canonical.indexOf(":");
        if (colon == -1)
        {
            return null;
        }

        // obtain the two components of the canonical form
        String ns = canonical.substring(0, colon);
        String value = canonical.substring(colon + 1);

        // see if we can tie a type to the namespace
        ExternalIdentifierType type = null;
        ExternalIdentifierType[] types = (ExternalIdentifierType[]) PluginManager.getPluginSequence(ExternalIdentifierType.class);
        if (types != null)
        {
            for (ExternalIdentifierType t : (ExternalIdentifierType[]) types)
            {
                if (t.getNamespace().equals(ns))
                {
                    type = t;
                    break;
                }
            }
        }
        if (type == null)
        {
            return null;
        }

        // assemble and return
        ExternalIdentifier eid = new ExternalIdentifier(type, value);
        return eid;
    }*/

    public String getValue()
    {
        return value;
    }

    /**
     * We can't say anything about the default behaviour here. This won't
     * always be true for getObject(), but for now, it's easiest to leave the
     * implementation in the Handle subclass.
     */
    public Map<String, List<String>> getMetadata()
    {
        return null;
    }

    public List<String> getMetadata(String field)
    {
        return null;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

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
