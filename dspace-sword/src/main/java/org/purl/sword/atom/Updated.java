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
public class Updated extends BasicStringContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_ATOM, "updated", Namespaces.NS_ATOM);

    public Updated()
    {
        super(XML_NAME);
    }

    public Updated(String uri)
    {
        this();
        setContent(uri);
    }

    public static XmlName elementName()
    {
        return XML_NAME; 
    }
}
