/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
     *
     * @param prefix     The namespace prefix.
     * @param localName  The element's local name. 
     * @param namespace  The element's namespace. 
     */
    public XmlName(String prefix, String localName, String namespace)
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
        if ( prefix != null && prefix.trim().length() > 0 )
        {
           qName = prefix + ":";
        }
        qName += localName;
        return qName;
    }

    @Override
    public boolean equals(Object other)
    {

        if ( other instanceof XmlName )
        {
            XmlName otherName = (XmlName) other;
            return StringUtils.equals(this.namespace, otherName.namespace) &&
                   StringUtils.equals(this.localName, otherName.localName);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(namespace).append(localName).hashCode();
    }
}
