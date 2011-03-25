/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class SwordPackaging extends BasicStringContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "packaging", Namespaces.NS_SWORD);

    public SwordPackaging()
    {
        super(XML_NAME);
    }

    public SwordPackaging(String version)
    {
        this();
        setContent(version); 
    }

    public static XmlName elementName()
    {
        return XML_NAME;
    }
}
