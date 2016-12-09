/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

import org.purl.sword.base.Namespaces;
import org.purl.sword.base.XmlName;

/**
 * Represents an ATOM Contributor. 
 * 
 * @author Neil Taylor
 */
public class Contributor extends Author
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_ATOM, "contributor", Namespaces.NS_ATOM);
    
    /**
     * Create a new instance and set the prefix to 
     * 'atom' and the local name to 'contributor'. 
     */
    public Contributor()
    {
        super(XML_NAME);
    }
 
    /**
     * Get the element name for this Xml Element.
     *
     * @return The details of prefix, localname and namespace. 
     */
    public static XmlName elementName()
    {
        return XML_NAME;
    }
}
