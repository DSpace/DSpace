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
public class DcAbstract extends BasicStringContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_DC_TERMS, "abstract", Namespaces.NS_DC_TERMS);

    public DcAbstract()
    {
        super(XML_NAME);
    }

    public DcAbstract(String version)
    {
        this();
        setContent(version); 
    }

    public static XmlName elementName()
    {
        return XML_NAME;
    }

}
