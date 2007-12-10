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

import java.net.URISyntaxException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * @author James Rutherford
 */
public class ExternalIdentifier
{
    private static final Logger log = Logger.getLogger(ExternalIdentifier.class);

    protected Context context;
    protected String value;
    protected ObjectIdentifier oid;
    protected ExternalIdentifierType type;

    public ExternalIdentifier()
    {
    }

    protected ExternalIdentifier(ExternalIdentifierType type)
    {
        this.type = type;
    }

    public ExternalIdentifier(Context context, DSpaceObject dso,
            ExternalIdentifierType type, String value)
    {
        this(type);
        this.context = context;
        this.value = value;

        oid = dso.getIdentifier();
    }

    public ObjectIdentifier getObjectIdentifier()
    {
        return oid;
    }

    public ExternalIdentifierType getType()
    {
        return type;
    }

    public URI getURI()
    {
        try
        {
            // eg: http + :// + hdl.handle.net + / + 1234/56
            return new URI(type.getProtocol() + "://" + type.getBaseURI() +
                    "/" + value);
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

    @Deprecated
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
