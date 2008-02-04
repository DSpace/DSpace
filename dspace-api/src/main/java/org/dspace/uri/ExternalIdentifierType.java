/*
 * ExternalIdentifierType.java
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

/**
 * High level class to generally represent external identifier type definitions.  This allows all
 * implementations to use a common description framework for identifiers.  Implementations need
 * merely to extend the class and call super() in the constructor with the relevant parameters,
 * as well as implementing two methods specifically for instantiating objects of their type
 *
 * @author Richard Jones
 * @author James Rutherford
 */
public abstract class ExternalIdentifierType
{
    /** identifier implementation namespace */
    private String namespace;

    /** protocol for resolving identifier */
    private String protocol;

    /** base service/url for resolving identifiers */
    private String baseURI;

    /** string used between protocol and base url to initialise resolution.  Defaults to "://" */
    private String protocolActivator = "://";

    /** separator between the baseURI and the remainder of the identifier.  Defaults to "/" */
    private String baseSeparator = "/";

    /**
     * Construct a new instance of an ExternalIdentifierType using the full range of properties.
     *
     * Should only be used by implementing classes to save on implementation details
     * @param namespace
     * @param protocol
     * @param baseURI
     * @param protocolActivator
     * @param baseSeparator
     */
    protected ExternalIdentifierType(String namespace, String protocol, String baseURI,
                                  String protocolActivator, String baseSeparator)
    {
        this.protocol = protocol;
        this.namespace = namespace;
        this.baseURI = baseURI;
        this.protocolActivator = protocolActivator;
        this.baseSeparator = baseSeparator;
    }

    /**
     * Get the namespace (e.g. hdl for the Handle system)
     *
     * @return
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Get the protocol (e.g. http for the handle system)
     *
     * @return
     */
    public String getProtocol()
    {
        return protocol;
    }

    /**
     * Get the base URI (e.g. hdl.handle.net for the Handle system)
     * @return
     */
    public String getBaseURI()
    {
        return baseURI;
    }

    /**
     * Get the protocol activator (e.g. :// for http and virtually everything else)
     * @return
     */
    public String getProtocolActivator()
    {
        return protocolActivator;
    }

    /**
     * Get the character used to separatre the baseURI from the rest of the identifier (.e.g "/" for a URL)
     * @return
     */
    public String getBaseSeparator()
    {
        return baseSeparator;
    }

    /**
     * Get an instance of the ExternalIdentifier associated with this type using the given value
     * and ObjectIdentifier.  This is implementation specific.
     *
     * For example, the handle implementation of this method needs just to read:
     *
     * <code>return new Handle(value, oid);</code>
     *
     * @param value
     * @param oid
     * @return
     */
    public abstract ExternalIdentifier getInstance(String value, ObjectIdentifier oid);

    /**
     * Is the given identifier type the same as the current instance.  This should not compare their
     * in-memory equalness, but their value equalness.  For example, in:
     *
     * <code>
     * HandleType ht1 = new HandleType();
     * HandleType ht2 = new HandleType();
     * boolean equal = ht1.equals(ht2);
     * </code>
     *
     * the value of "equal" should be "true".  In implementation it should be sufficient to
     * return the value of (for example, using the Handle system):
     *
     * <code>type instanceof HandleType</code>
     *
     * @param type
     * @return
     */
    public abstract boolean equals(ExternalIdentifierType type);

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * Render class as a string.  For debugging only
     *
     * @return
     */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}