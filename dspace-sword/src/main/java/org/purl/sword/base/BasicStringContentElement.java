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
public class BasicStringContentElement extends BasicContentElement
{
    private String content;

    public BasicStringContentElement(String prefix, String localName, String namespaceUri)
    {
        super(prefix, localName, namespaceUri);
    }

    public BasicStringContentElement(XmlName name)
    {
        super(name); 
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String value)
    {
        content = value; 
    }

    protected void marshallContent(Element element)
    {
        if ( content != null )
        {
            element.appendChild(content);
        }
    }

    protected void unmarshallContent(Element element)
    throws UnmarshallException
    {
       setContent(unmarshallString(element));
    }

    protected SwordValidationInfo validateContent(Properties validationContext)
    {
        SwordValidationInfo result = null;
        if ( content == null )
        {
            result = new SwordValidationInfo(xmlName,
                SwordValidationInfo.MISSING_CONTENT,
                SwordValidationInfoType.WARNING);
        }
        return result; 
    }


    protected String getContentAsString()
    {
        return content;
    }
}
