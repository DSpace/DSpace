/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.log4j.Logger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;
import org.purl.sword.base.XmlName;

/**
 * Represents an ATOM Link element. 
 * 
 * @author Neil Taylor
 */
public class Link extends XmlElement implements SwordElementInterface
{
    /**
     * Label for the href attribute. 
     */
    public static final String ATTRIBUTE_HREF = "href";

    /**
     * Label for the rel attribute. 
     */
    public static final String ATTRIBUTE_REL = "rel";

    /**
     * Label for the type attribute. 
     */
    public static final String ATTRIBUTE_TYPE = "type";

    /**
     * Label for the hreflang attribute. 
     */
    public static final String ATTRIBUTE_HREF_LANG = "hreflang";

    /**
     * Label for the title attribute. 
     */
    public static final String ATTRIBUTE_TITLE = "title";

    /**
     * Label for the length attribute.
     */
    public static final String ATTRIBUTE_LENGTH = "length";
    
    /**
     * Local name for the element. 
     */
    @Deprecated
    public static final String ELEMENT_NAME = "link";
    
    /**
     * Stores the href. 
     */
    private String href; 
 
    /**
     * Stores the Rel attribute. 
     */
    private String rel; 
 
    /**
     * Stores the type. 
     */
    private String type; 
 
    /**
     * Stores the HREF lang. 
     */
    private String hreflang;
 
    /**
     * Stores the title. 
     */
    private String title; 
 
    /**
     * Stores the length. 
     */
    private String length;
 
    /**
     * Stores the content. 
     */
    private String content; 
 
    /**
     * The logger. 
     */
    private static Logger log = Logger.getLogger(Link.class);
 
    private static final XmlName XML_NAME = new XmlName(
            Namespaces.PREFIX_ATOM, "link", Namespaces.NS_ATOM);
 
    /**
     * Create a new instance and set prefix and local name to 'atom' and 'link', 
     * respectively. 
     */
    public Link()
    {
        super(XML_NAME);
    }
 
    public static XmlName elementName()
    {
        return XML_NAME;
    }
 
    /**
     * Mashall the data stored in this object into Element objects. 
     * 
     * @return An element that holds the data associated with this object. 
     */
    public Element marshall()
    {
        Element element = new Element(getQualifiedName(), xmlName.getNamespace());
 
        if ( content != null )
        {
            element.appendChild(content);
        }
 
        if ( href != null ) 
        {
            Attribute hrefAttribute = new Attribute(ATTRIBUTE_HREF, href);
            element.addAttribute(hrefAttribute);
        }
 
        if ( rel != null ) 
        {
            Attribute relAttribute = new Attribute(ATTRIBUTE_REL, rel);
            element.addAttribute(relAttribute);
        }
 
        if ( type != null ) 
        {
            Attribute typeAttribute = new Attribute(ATTRIBUTE_TYPE, type);
            element.addAttribute(typeAttribute);
        }
 
        if ( hreflang != null ) 
        {
            Attribute hreflangAttribute = new Attribute(ATTRIBUTE_HREF_LANG, hreflang);
            element.addAttribute(hreflangAttribute);
        }
 
        if ( title != null ) 
        {
            Attribute titleAttribute = new Attribute(ATTRIBUTE_TITLE, title);
            element.addAttribute(titleAttribute);
        }
 
        if ( length != null ) 
        {
            Attribute lengthAttribute = new Attribute(ATTRIBUTE_LENGTH, length);
            element.addAttribute(lengthAttribute);
        }
 
        return element;
    }
 
    /**
     * Unmarshall the contents of the Link element into the internal data objects
     * in this object. 
     * 
     * @param link The Link element to process. 
     *
     * @throws UnmarshallException If the element does not contain an ATOM link
     *         element, or if there is a problem processing the element or any 
     *         subelements. 
     */
    public void unmarshall(Element link)
    throws UnmarshallException 
    {
       unmarshall(link, null);
    }
 
    
    public SwordValidationInfo unmarshall(Element link, Properties validationProperties)
    throws UnmarshallException
    {
        if ( ! isInstanceOf(link, xmlName) )
        {
            return handleIncorrectElement(link, validationProperties);
        }
 
        ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
        ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();
 
        try
        {
            // get the attributes
            int attributeCount = link.getAttributeCount();
            Attribute attribute = null; 
            for ( int i = 0; i < attributeCount; i++ )
            {
                attribute = link.getAttribute(i);
                if ( ATTRIBUTE_HREF.equals(attribute.getQualifiedName()))
                {
                    href = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_HREF, href));
                    }
                }
                else if ( ATTRIBUTE_REL.equals(attribute.getQualifiedName()))
                {
                    rel = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_REL, rel));
                    }
                }
                else if ( ATTRIBUTE_TYPE.equals(attribute.getQualifiedName()))
                {
                    type = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_TYPE, type));
                    }
                }
                else if ( ATTRIBUTE_HREF_LANG.equals(attribute.getQualifiedName()))
                {
                    hreflang = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_HREF_LANG, hreflang));
                    }
                }
                else if ( ATTRIBUTE_TITLE.equals(attribute.getQualifiedName()))
                {
                    title = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_TITLE, title));
                    }
                }
                else if ( ATTRIBUTE_LENGTH.equals(attribute.getQualifiedName()))
                {
                    length = attribute.getValue();
                    if ( validationProperties != null)
                    {
                        attributeItems.add(createValidAttributeInfo(ATTRIBUTE_LENGTH, length));
                    }
                }
                else
                {
                    XmlName attributeName = new XmlName(attribute);
   
                    SwordValidationInfo unknown = new SwordValidationInfo(xmlName,
                        attributeName, 
                        SwordValidationInfo.UNKNOWN_ATTRIBUTE, 
                        SwordValidationInfoType.INFO);
                    unknown.setContentDescription(attribute.getValue());
                    attributeItems.add(unknown);
                }
            }
  
            if ( link.getChildCount() > 0 )
            {
                SwordValidationInfo content = new SwordValidationInfo(xmlName,
                    "This element has content, but it is not used by SWORD",
                    SwordValidationInfoType.INFO);
                validationItems.add(content);
            }
  
        }
        catch ( Exception ex )
        {
             log.error("Unable to parse an element in Link: " + ex.getMessage());
             throw new UnmarshallException("Unable to parse element in link", ex);
        }
 
        SwordValidationInfo result = null;
        if ( validationProperties != null )
        {
             result = validate(validationItems, attributeItems, validationProperties);
        }
        return result; 
    }
 
    public SwordValidationInfo validate(Properties validationContext)
    {
       return validate(null, null, validationContext);
    }
 
    public SwordValidationInfo validate(List<SwordValidationInfo> elements,
            List<SwordValidationInfo> attributes,
            Properties validationContext)
    {
        boolean validateAll = (elements == null);
 
        SwordValidationInfo result = new SwordValidationInfo(xmlName);
 
        if ( href == null )
        {
            XmlName attributeName = new XmlName(xmlName.getPrefix(),
                ATTRIBUTE_HREF,
                xmlName.getNamespace());
  
            SwordValidationInfo item = new SwordValidationInfo(xmlName, attributeName,
                SwordValidationInfo.MISSING_ATTRIBUTE_WARNING,
                SwordValidationInfoType.ERROR);
            result.addAttributeValidationInfo(item);
        }
 
        if ( validateAll )
        {
            if ( href != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_HREF, href));
            }
 
            if ( rel != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_REL, rel));
            }
 
            if ( type != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_TYPE, type));
            }
 
            if ( hreflang != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_HREF_LANG, hreflang));
            }
 
            if ( title != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_TITLE, title));
            }
 
            if ( length != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_LENGTH, length));
            }
            
        }
 
        result.addUnmarshallValidationInfo(elements, attributes);
        return result; 
    }
 
    /**
     * Get the HREF attribute. 
     * 
     * @return The HREF. 
     */
    public String getHref() {
        return href;
    }
 
    /**
     * Set the HREF attribute. 
     * 
     * @param href The href. 
     */
    public void setHref(String href) {
        this.href = href;
    }
 
    /**
     * Get the Rel attribute. 
     * 
     * @return The Rel. 
     */
    public String getRel() {
        return rel;
    }
 
    /**
     * Set the Rel attribute. 
     * 
     * @param rel The Rel. 
     */
    public void setRel(String rel) {
        this.rel = rel;
    }
 
    /**
     * Get the type. 
     * 
     * @return The type. 
     */
    public String getType() {
        return type;
    }
 
    /**
     * Set the type. 
     * @param type The type. 
     */
    public void setType(String type) {
        this.type = type;
    }
 
    /**
     * Get the HREF Lang attribute. 
     * 
     * @return The HREF Lang. 
     */
    public String getHreflang() {
        return hreflang;
    }
 
    /**
     * Set the HREF Lang attribute. 
     * 
     * @param hreflang The HREF Lang. 
     */
    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }
 
    /**
     * Get the title. 
     * 
     * @return The title. 
     */
    public String getTitle() {
        return title;
    }
 
    /**
     * Set the title. 
     * 
     * @param title The title. 
     */
    public void setTitle(String title) {
        this.title = title;
    }
 
    /**
     * Get the length. 
     * 
     * @return The length. 
     */
    public String getLength() {
        return length;
    }
 
    /**
     * Set the length. 
     * 
     * @param length The length. 
     */
    public void setLength(String length) {
        this.length = length;
    }
 
    /**
     * Get the content. 
     * 
     * @return The content. 
     */
    public String getContent() {
        return content;
    }
 
    /**
     * Set the content. 
     * 
     * @param content The content. 
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    public String toString()
    {
        return "Link -" + 
        " href: " + getHref() + 
        " hreflang: " + getHreflang() + 
        " title: " + getTitle() + 
        " rel: " + getRel() + 
        " content: " + getContent() + 
        " type: " + getType() + 
        " length: " + getLength();
    }

}
