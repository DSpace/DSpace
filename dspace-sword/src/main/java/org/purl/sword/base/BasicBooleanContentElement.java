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
public class BasicBooleanContentElement extends BasicContentElement
{
    private boolean content;

    private boolean isSet;

    public BasicBooleanContentElement(String prefix, String localName, String namespaceUri)
    {
        super(prefix, localName, namespaceUri);
    }

    public BasicBooleanContentElement(XmlName name)
    {
        super(name);
    }

    public boolean getContent()
    {
        return content;
    }

    public void setContent(boolean value)
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
       element.appendChild(Boolean.toString(content));
    }

    protected void unmarshallContent(Element element)
    throws UnmarshallException
    {
       setContent(unmarshallBoolean(element));
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
        return Boolean.toString(content);
    }


}
