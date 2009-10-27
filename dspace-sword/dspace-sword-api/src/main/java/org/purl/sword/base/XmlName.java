/**
 * Copyright (c) 2009, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.purl.sword.base;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class XmlName {

    /** Prefix for the name */
    private String prefix;

    /** Local name */
    private String localName;

    /** The namespace for the element */ 
    private String namespace;

    
    /**
     * Create a new instance with the specified prefix and local name.
     * @param prefix     The namespace prefix.
     * @param localName  The element's local name. 
     */
    public XmlName(String prefix, String localName, String namespace )
    {
        this.prefix = prefix;
        this.localName = localName;
        this.namespace = namespace;
    }

    public XmlName(Element element)
    {
        this.prefix = element.getNamespacePrefix();
        this.localName = element.getLocalName();
        this.namespace = element.getNamespaceURI();
    }

    public XmlName(Attribute attribute)
    {
        this.prefix = attribute.getNamespacePrefix();
        this.localName = attribute.getLocalName();
        this.namespace = attribute.getNamespaceURI();
    }

    /**
     * Get the prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the prefix.
     *
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Get the local name.
     *
     * @return the localName
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Set the local name.
     * 
     * @param localName the localName to set
     */
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /**
     * Get the current namespace value.
     * 
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Set the namespace value.
     *
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getQualifiedName()
    {
        String qName = "";
        if( prefix != null && prefix.trim().length() > 0 )
        {
           qName = prefix + ":";
        }
        qName += localName;
        return qName;
    }

    @Override
    public boolean equals(Object other)
    {

        if( other instanceof XmlName )
        {
            XmlName otherName = (XmlName) other;
            try
            {
               return (this.namespace.equals(otherName.namespace) &&
                       this.localName.equals(otherName.localName));

            }
            catch(Exception ex) 
            {
                // fall through to the default return case
            }
        }

        return false;
    }

}
