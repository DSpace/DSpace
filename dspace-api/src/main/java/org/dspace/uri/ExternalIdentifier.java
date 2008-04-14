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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * High level class to generally represent any possible external identifier in DSpace.  It provides significant
 * default functionality, so that extending and implementing classes need to do very little actual work.
 *
 * It represents all identifiers as a combination of a definition type, a value, and the object identifier
 * of the DSpaceObject that it actually represents.  The value is any given string, and the object identifier
 * must be the standard ObjectIdentifier class in DSpace.  The definition type is any implementation of the
 * ExternalIdentifierType abstract class.
 *
 * This class also implements ResolvableIdentifier so that it can be used to obtain DSpaceObjects directly.
 *
 * @author Richard Jones
 * @author James Rutherford
 */
public abstract class ExternalIdentifier implements ResolvableIdentifier
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(ExternalIdentifier.class);

    /** The value of the identifier */
    protected String value;

    /** the native object identifier the external identifier represents */
    protected ObjectIdentifier oid;

    /** the type definition of the external identifier */
    protected ExternalIdentifierType type;

    /**
     * DO NOT USE
     *
     * This is a required feature of the PluginManager, but should not actually be used.
     *
     * Instead refer to <code>ExternalIdentifierService.get()</code>
     */
    public ExternalIdentifier()
    {
        // DO NOT USE
    }

    /**
     * Create an instance of the external identifier given the type definition and the value.  This will
     * not resolve to an object (via its object identifier) until that identifier has been assigned
     *
     * This should only be used by implementing classes to save on implementation details
     *
     * @param type
     * @param value
     */
    protected ExternalIdentifier(ExternalIdentifierType type, String value)
    {
        this.type = type;
        this.value = value;
    }

    /**
     * Create an instance of the external identifier given the type definition, value, and object
     * identifier which we are representing
     *
     * This should only be used by implementing classes to save on implementation details
     *
     * @param type
     * @param value
     * @param oid
     */
    protected ExternalIdentifier(ExternalIdentifierType type, String value, ObjectIdentifier oid)
    {
        // create a real, full size identifier
        this.type = type;
        this.oid = oid;
        this.value = value;
    }

    /**
     * Set the value of the identifier
     *
     * @param value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * set the object identifier
     *
     * @param oid
     */
    public void setObjectIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    /**
     * set the external identifier's type definition
     *
     * @param type
     */
    public void setType(ExternalIdentifierType type)
    {
        this.type = type;
    }

    /**
     * get the type definition object for the implementation of the external identifier
     *
     * @return
     */
    public ExternalIdentifierType getType()
    {
        return type;
    }

    /**
     * Get the implementation's specific external formulation of the url.  This is explicitly
     * not the DSpace URL, but the url space controlled by the external identifier itself.
     * For example, calling this method on a handle will result in a URL like:
     *
     * <code>http://hdl.handle.net/123456789/100</code>
     * 
     * @return
     */
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

    /**
     * If the object represented by this external identifier should be removed, what behaviour
     * should the tombstoning mechanism take.  The default is to leave a tombstone, but individual
     * implementations may do otherwise.  If this method returns true, the identifier system will not
     * delete the record of the identifier, and subsequent requests to the URL will result in a
     * tombstone page.  If this method returns false, all record of the identifier will be removed
     * from the system.
     *
     * The default is true
     *
     * @return
     */
    public boolean leaveTombstone()
    {
        // default to true unless implementation overrides to the contrary
        return true;
    }

    /**
     * Get the value of the identifier
     * 
     * @return
     */
    public String getValue()
    {
        return value;
    }

    /////////////////////////////////////////////////////////////////////
    // ResolvableIdentifier methods
    /////////////////////////////////////////////////////////////////////

    /**
     * Get the context path segment of the URL which the implementation of the external identifier
     * will expose and will be able to subsequently resolve
     *
     * @return
     */
    public String getURLForm()
    {
        return type.getNamespace() + "/" + this.value;
    }

    /**
     * Get the canonical form of the identifier.  The default for this is to construct the following string:
     *
     * <code>[namespace]:[value]</code>
     *
     * For example, the canonical form for a handle will result in a string like
     *
     * <code>hdl:123456789/100</code>
     *
     * @return
     */
    public String getCanonicalForm()
    {
        // eg: hdl:1234/56
        return type.getNamespace() + ":" + value;
    }

    /**
     * Return a string representation of the identifier type; specifically for use when
     * working with ResolvableIdentifiers with no notion of whether the underlying identifier
     * is an ExternalIdentifier or an ObjectIdentifier
     *
     * @return
     */
    public String getIdentifierType()
    {
        return type.getNamespace();
    }

    /**
     * Get the native object identifier for the object we are representing
     *
     * @return
     */
    public ObjectIdentifier getObjectIdentifier()
    {
        if (oid == null)
        {
            throw new IllegalStateException("An external identifier with no ObjectIdentifier exists");
        }
        return oid;
    }

    /////////////////////////////////////////////////////////////////////
    // Abstract Methods
    /////////////////////////////////////////////////////////////////////

    /**
     * Parse the canonical form of the given string and return an ExternalIdentifier object for that
     * string.
     *
     * This remains abstract as it is impossible for this class to make any assumptions
     * about the nature of the canonical form.  The canonical form implementation above is
     * a default but may be overridden.  Furthermore, it cannot construct the relevant
     * type definition classes with which to construct new external identifier objects
     * based on the given string
     *
     * @param canonical
     * @return
     */
    public abstract ExternalIdentifier parseCanonicalForm(String canonical);

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * string representation of the class.  For debugging only.
     *
     * @return
     */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
