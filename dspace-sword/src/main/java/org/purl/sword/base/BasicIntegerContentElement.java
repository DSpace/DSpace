/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.Properties;
import nu.xom.Element;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class BasicIntegerContentElement extends BasicContentElement
{
    private int content = 0;

    private boolean isSet;

    public BasicIntegerContentElement(String prefix, String localName, String namespaceUri)
    {
        super(prefix, localName, namespaceUri);
    }

    public BasicIntegerContentElement(XmlName name)
    {
        super(name);
    }

    public int getContent()
    {
        return content;
    }

    public void setContent(int value)
    {
        isSet = true; 
        content = value; 
    }

    public boolean isSet()
    {
        return isSet;
    }

    protected void marshallContent(Element element)
    {
       element.appendChild(Integer.toString(content));
    }

    protected void unmarshallContent(Element element)
    throws UnmarshallException
    {
       setContent(unmarshallInteger(element));
    }

    protected SwordValidationInfo validateContent(Properties validationContext)
    {
        SwordValidationInfo result = null;
        if ( ! isSet )
        {
           result = new SwordValidationInfo(xmlName,
               SwordValidationInfo.MISSING_CONTENT,
               SwordValidationInfoType.WARNING);
        }
        return result;
    }

    protected String getContentAsString()
    {
        return Integer.toString(content);
    }
}
