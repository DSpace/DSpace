/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an validation information item about
 * the elements/attributes.
 * 
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class SwordValidationInfo {

    /**
     * Name of the element. 
     */
    private XmlName elementName;

    /**
     * Name of the attribute for the specified element. This will
     * only be used when the information relates to an attribute.
     */
    private XmlName attributeName;

    /**
     * Optional description of the content of the element or attribute. 
     */
    private String contentDescription;

    /** The information message. This holds the actual error/warning message */
    private String message;

    /** The type of validation information */
    private SwordValidationInfoType type;

    /** List of nested validation info items */
    private List<SwordValidationInfo> elementInfo;

    private List<SwordValidationInfo> attributeInfo;

    /**
     * List of validation info notes that were generated during
     * an unmarshal stage.
     */
    private List<SwordValidationInfo> unmarshallElementInfo;

    /**
     * List of validation info notes that were generated during an
     * unmarshal stage.
     */
    private List<SwordValidationInfo> unmarshallAttributeInfo;

    public static final String UNKNOWN_ELEMENT = "This element is present, but it is not used as part of the SWORD profile";

    public static final String UNKNOWN_ATTRIBUTE = "This attribute is present, but it is not used as part of the SWORD profile";

    public static final String MISSING_ELEMENT_WARNING = "This element is not present, but it SHOULD be included.";

    public static final String MISSING_ATTRIBUTE_WARNING = "This attribute is not present, but it SHOULD be included.";

    public static final String DUPLICATE_ELEMENT = "This element has already been included earlier in this document. This element is ignored.";

    public static final String MISSING_CONTENT = "No content is defined. This element should have content.";

    public static final String MISSING_ELEMENT_ERROR = "This element is not present, but at least one MUST be included.";

    public static final String ERROR_WITH_CONTENT = "There is an error with the value.";
    /**
     * Create a new information object for the specified element. Sets the
     * default type to be VALID.
     *
     * @param element The element.
     */
    public SwordValidationInfo(XmlName element)
    {
       this(element, null, "", SwordValidationInfoType.VALID);
    }

    /**
     * Create a new information object for the specified element's
     * attribute. Sets the default type to be VALID.
     *
     * @param element   the element.
     * @param attribute the attribute. 
     */
    public SwordValidationInfo(XmlName element, XmlName attribute)
    {
        this(element, attribute, "", SwordValidationInfoType.VALID);
    }

    /**
     * Create a new instance of a validation information object that
     * reports on an element.
     *
     * @param element The element.
     * @param theMessage The information message.
     * @param theType    The type of message.
     */
    public SwordValidationInfo(XmlName element, 
                               String theMessage,
                               SwordValidationInfoType theType)
    {
       this(element, null, theMessage, theType);
    }

    /**
     * Create a new instance of a validation information object that
     * reports on an attribute in the specified element.
     *
     * @param element          The local name for the element.
     * @param attribute        The attribute.
     * @param theMessage       The information message.
     * @param theType          The type of message.
     */
    public SwordValidationInfo(XmlName element, XmlName attribute,
                               String theMessage,
                               SwordValidationInfoType theType)
    {
       this.elementName = element;
       this.attributeName = attribute;
       
       message = theMessage;
       type = theType;
       elementInfo = new ArrayList<SwordValidationInfo>();
       attributeInfo = new ArrayList<SwordValidationInfo>();
       unmarshallElementInfo = new ArrayList<SwordValidationInfo>();
       unmarshallAttributeInfo = new ArrayList<SwordValidationInfo>(); 
    }

    /**
     * Return the information message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the information message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Return the type of information.
     *
     * @return the type
     */
    public SwordValidationInfoType getType() {
        return type;
    }

    /**
     * Set the type of information
     *
     * @param type the type to set
     */
    public void setType(SwordValidationInfoType type) {
        this.type = type;
    }

    /**
     * Return the element that this information describes.
     *
     * @return the element
     */
    public XmlName getElement() {
        return elementName;
    }

    /**
     * Set the element that this information describes.
     *
     * @param element the element to set
     */
    public void setElement(XmlName element) {
        this.elementName = element;
    }

    /**
     * Return the attribute that this information describes.
     *
     * @return the attribute
     */
    public XmlName getAttribute() {
        return attributeName;
    }

    /**
     * Set the attribute that this information describes.
     * 
     * @param attribute the attribute to set
     */
    public void setAttribute(XmlName attribute) {
        this.attributeName = attribute;
    }

    /**
     * Add a related information item to this resource.
     * 
     * @param item The information item to store.
     */
    public void addValidationInfo(SwordValidationInfo item)
    {
        
       if( type.compareTo(item.getType()) < 0 )
       {
           type = item.getType();
       }

       elementInfo.add(item);
    }

    public void addAttributeValidationInfo(SwordValidationInfo attribute)
    {
        if( type.compareTo(attribute.getType()) < 0 )
        {
            type = attribute.getType();
        }
        attributeInfo.add(attribute); 
    }

    public void addUnmarshallElementInfo(SwordValidationInfo unmarshallElement)
    {
       if( unmarshallElement == null )
       {
           // not part of the validation process - end here
           return;
       }
       
       if( type.compareTo(unmarshallElement.getType()) < 0 )
       {
           type = unmarshallElement.getType();
       }
       unmarshallElementInfo.add(unmarshallElement);
    }

    public void addUnmarshallAttributeInfo(SwordValidationInfo unmarshallAttribute)
    {
       if( type.compareTo(unmarshallAttribute.getType()) < 0 )
       {
           type = unmarshallAttribute.getType();
       }
       unmarshallAttributeInfo.add(unmarshallAttribute);
    }

    /**
     * Clear the list of validation info items. 
     */
    public void clearValidationItems()
    {
        elementInfo.clear();
        attributeInfo.clear();
        resetType();
    }

    /**
     * Clear the list of unmarshalled info items.
     */
    public void clearUnmarshallItems()
    {
        unmarshallElementInfo.clear();
        unmarshallAttributeInfo.clear();
        resetType(); 
    }

    protected void resetType()
    {
        type = SwordValidationInfoType.VALID;
        
        resetType(getValidationElementInfoIterator());
        resetType(getValidationAttributeInfoIterator());
        resetType(getUnmarshallElementInfoIterator());
        resetType(getUnmarshallAttributeInfoIterator());
    }

    protected void resetType(Iterator<SwordValidationInfo> iterator)
    {
        SwordValidationInfo item = null;
        while( iterator.hasNext() )
        {
            item = iterator.next();
            if( item != null && type.compareTo(item.getType()) < 0 )
            {
                type = item.getType();
            }
        }
    }


    /**
     * Return an iterator to view the nested validation info objects.
     *
     * @return Iterator for the nested objects. 
     */
    public Iterator<SwordValidationInfo> getValidationElementInfoIterator()
    {
        return elementInfo.iterator();
    }

    /**
     *
     */
    public Iterator<SwordValidationInfo> getValidationAttributeInfoIterator()
    {
        return attributeInfo.iterator();
    }

    /**
     *
     */
    public Iterator<SwordValidationInfo> getUnmarshallElementInfoIterator()
    {
        return unmarshallElementInfo.iterator();
    }

    /**
     * 
     */
    public Iterator<SwordValidationInfo> getUnmarshallAttributeInfoIterator()
    {
        return unmarshallAttributeInfo.iterator();
    }

    /**
     * @return the contentDescription
     */
    public String getContentDescription() {
        return contentDescription;
    }

    /**
     * @param contentDescription the contentDescription to set
     */
    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    /**
     * 
     * @param elementItems
     * @param attributeItems
     */
    public void addUnmarshallValidationInfo(
            List<SwordValidationInfo> elementItems,
            List<SwordValidationInfo> attributeItems)
    {
       if( elementItems != null )
       {
           Iterator<SwordValidationInfo> items = elementItems.iterator();

           while( items.hasNext() )
           {
              addUnmarshallElementInfo(items.next());
           }
       }

       if( attributeItems != null )
       {
          Iterator<SwordValidationInfo> attributes = attributeItems.iterator();

           while( attributes.hasNext() )
           {
              addUnmarshallAttributeInfo(attributes.next());
           }
       }
   }

   public void addUnmarshallValidationInfo(SwordValidationInfo other)
   {
       addUnmarshallValidationInfo(other.elementInfo, other.attributeInfo);
   }

   @Override
   public String toString()
   {
       return "" + getType();
   }

   /**
     * Utility method that will recursively print out the list of items
     * for the specified validation info object.
     *
     * @param info   The validation info object to display.
     * @param indent The level of indent, expressed as a number of space characters.
     */
    public void createString(SwordValidationInfo info, StringBuffer buffer, String indent)
    {
       String prefix = info.getElement().getPrefix();
       buffer.append(indent);

       buffer.append("[");
       buffer.append(info.getType());
       buffer.append("]");

       if( prefix != null && prefix.trim().length() > 0 )
       {
          buffer.append(prefix);
          buffer.append(":");
       }

       buffer.append(info.getElement().getLocalName());
       buffer.append("  ");
       if (info.getAttribute() != null) {
          buffer.append(info.getAttribute().getLocalName());
          buffer.append("=\"");
          if( info.getContentDescription() != null )
          {
             buffer.append(info.getContentDescription());
          }
          buffer.append("\"");
       }
       else
       {
          if( info.getContentDescription() != null )
          {
                 buffer.append(" Value: '");
                 buffer.append(info.getContentDescription());
                 buffer.append("'");
          }
       }
       buffer.append("\n" + indent + "message: " );
       buffer.append(info.getMessage());
       buffer.append("\n");
       
       // process the list of attributes first
       Iterator<SwordValidationInfo> iterator = info.getValidationAttributeInfoIterator();
       while( iterator.hasNext())
       {
          createString(iterator.next(), buffer, "   " + indent);
       }

       iterator = info.getUnmarshallAttributeInfoIterator();
       while( iterator.hasNext())
       {
          createString(iterator.next(), buffer, "   " + indent);
       }

       // next, process the element messages
       iterator = info.getValidationElementInfoIterator();
       while( iterator.hasNext())
       {
          createString(iterator.next(), buffer, "   " + indent);
       }

       iterator = info.getUnmarshallElementInfoIterator();
       while( iterator.hasNext())
       {
          createString(iterator.next(), buffer, "   " + indent);
       }
    }


}
