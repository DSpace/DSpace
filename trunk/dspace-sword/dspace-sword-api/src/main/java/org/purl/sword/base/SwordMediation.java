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
public class SwordMediation extends BasicBooleanContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "mediation", Namespaces.NS_SWORD);

    public SwordMediation()
    {
        super(XML_NAME.getPrefix(), XML_NAME.getLocalName(), XML_NAME.getNamespace());
    }

    public SwordMediation(boolean value)
    {
        this();
        setContent(value);
    }

    public static XmlName elementName()
    {
        return XML_NAME;
    }
}
