/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.purl.sword.base;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class SwordMaxUploadSize extends BasicIntegerContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "maxUploadSize", Namespaces.NS_SWORD);

    public SwordMaxUploadSize()
    {
        super(XML_NAME);
    }

    public SwordMaxUploadSize(int value)
    {
        this();
        setContent(value);
    }

    public static XmlName elementName()
    {
        return XML_NAME;
    }
}
