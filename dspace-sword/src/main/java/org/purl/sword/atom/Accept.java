/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

import org.purl.sword.base.*;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class Accept extends BasicStringContentElement
{
    /**
     * The XmlName representation for this element. 
     */
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_APP, "accept", Namespaces.NS_APP);

    public Accept()
    {
        super(XML_NAME.getPrefix(), XML_NAME.getLocalName(), XML_NAME.getNamespace());
    }

    public Accept(String version)
    {
        this();
        setContent(version); 
    }

    /**
     * Get the XmlName for this class.
     *
     * @return The prefix, localname and namespace for this element.
     */
    public static XmlName elementName()
    {
        return XML_NAME;
    }
}
