/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.log4j.Logger;

/**
 * Represents a text construct in the ATOM elements. This is a superclass of 
 * several elements within this implementation. 
 * 
 * @author Neil Taylor
 */
public class SwordAcceptPackaging extends XmlElement
implements SwordElementInterface
{
   /**
    * The content in the element. 
    */
	private String content;  

	/**
	 * The type of the element. 
	 */
	private QualityValue qualityValue;

	/**
	 * The log. 
	 */
	private static Logger log = Logger.getLogger(SwordAcceptPackaging.class);
	
	/** */ 
    public static final String ELEMENT_NAME = "acceptPackaging";

    protected static final XmlName ATTRIBUTE_Q_NAME = new XmlName(Namespaces.PREFIX_SWORD,
                                    "q",
                                    Namespaces.NS_SWORD);

    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, ELEMENT_NAME, Namespaces.NS_SWORD);

    public SwordAcceptPackaging()
    {
       this(null, new QualityValue());
    }

    public SwordAcceptPackaging(String name, float value)
    {
        this(name, new QualityValue(value));
    }

    public SwordAcceptPackaging(String name, QualityValue value)
    {
       super(XML_NAME.getPrefix(), XML_NAME.getLocalName(), XML_NAME.getNamespace());
       initialise();
       setContent(name);
       setQualityValue(value); 
    }

	public static XmlName elementName()
    {
        return XML_NAME;
    }

    protected final void initialise()
    {
        qualityValue = null;
        content = null; 
    }

	/**
	 * Marshal the data in this object to an Element object. 
	 * 
	 * @return The data expressed in an Element. 
	 */
   public Element marshall()
   {
      Element element = new Element(getQualifiedName(), xmlName.getNamespace());
      if( qualityValue != null )
      {
         Attribute qualityValueAttribute = new Attribute(ATTRIBUTE_Q_NAME.getLocalName(), qualityValue.toString());
         element.addAttribute(qualityValueAttribute);
      }
      
      if( content != null )
	  {
         element.appendChild(content);
	  }
	  return element;
   }

   /**
    * Unmarshal the text element into this object.
    * 
    * This unmarshaller only handles plain text content, although it can 
    * recognise the three different type elements of text, html and xhtml. This
    * is an area that can be improved in a future implementation, if necessary. 
    * 
    * @param acceptPackaging The text element.
    * @param validationProperties
    * 
    * @throws UnmarshallException If the specified element is not of
    *                             the correct type, where the localname is used
    *                             to specify the valid name. Also thrown 
    *                             if there is an issue accessing the data. 
    */
   public SwordValidationInfo unmarshall(Element acceptPackaging,
            Properties validationProperties) throws UnmarshallException
   {
	   if( ! isInstanceOf(acceptPackaging, xmlName) )
	   {
		   handleIncorrectElement(acceptPackaging, validationProperties);
	   }

       ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
       ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();

       try
	   {
		   // get the attributes
		   int attributeCount = acceptPackaging.getAttributeCount();
		   Attribute attribute = null;
           float qv = -1;

		   for( int i = 0; i < attributeCount; i++ )
		   {
			   attribute = acceptPackaging.getAttribute(i);
			   if( ATTRIBUTE_Q_NAME.getLocalName().equals(attribute.getQualifiedName()))
			   {
                   try
                   {
				      qv = Float.parseFloat(attribute.getValue());
                      qualityValue = new QualityValue(qv);

                      SwordValidationInfo attr = new SwordValidationInfo(xmlName, ATTRIBUTE_Q_NAME);
                      attr.setContentDescription("" + qv);
                      attributeItems.add(attr);
                   }
                   catch(NumberFormatException nfe )
                   {
                      SwordValidationInfo attr = new SwordValidationInfo(xmlName, ATTRIBUTE_Q_NAME,
                              nfe.getMessage(), SwordValidationInfoType.ERROR);
                      attr.setContentDescription(attribute.getValue());
                      attributeItems.add(attr);
                   }

               }
               else
               {
                   SwordValidationInfo attr = new SwordValidationInfo(xmlName,
                           new XmlName(attribute),
                           SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                       SwordValidationInfoType.INFO );
                   attr.setContentDescription(attribute.getValue());

                   attributeItems.add(attr);
               }
		   }

		   int length = acceptPackaging.getChildCount();
		   if( length > 0 )
		   {
               try
               {
			      content = unmarshallString(acceptPackaging);
               }
               catch( UnmarshallException  ume )
               {
                  log.error("Error accessing the content of the acceptPackaging element");
                  validationItems.add(new SwordValidationInfo(xmlName,
                                              "Error unmarshalling element: " + ume.getMessage(),
                                              SwordValidationInfoType.ERROR));
               }
		   }

	   }
	   catch( Exception ex )
	   {
		   log.error("Unable to parse an element in " + getQualifiedName() + ": " + ex.getMessage());
		   throw new UnmarshallException("Unable to parse an element in " + getQualifiedName(), ex);
	   }

       SwordValidationInfo result = null;
       if( validationProperties != null )
       {
           result = validate(validationItems, attributeItems, validationProperties);
       }
       return result;
   }

   public void unmarshall(Element element)
   throws UnmarshallException
   {
      unmarshall(element, null);
   }

   @Override
   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, null, validationContext);
   }

   /**
    *
    * @param existing
    * @param attributeItems
    */
   protected SwordValidationInfo validate(List<SwordValidationInfo> existing,
           List<SwordValidationInfo> attributeItems,
           Properties validationContext)
   {
      SwordValidationInfo result = new SwordValidationInfo(xmlName); 
      result.setContentDescription(content);
      
      // item specific rules
      if( content == null )
      {
          result.addValidationInfo(
                  new SwordValidationInfo(xmlName,
                                          SwordValidationInfo.MISSING_CONTENT,
                                          SwordValidationInfoType.WARNING));
      }
      else
      {
          // check that the content is one of the Sword types
          if( ! SwordContentPackageTypes.instance().isValidType(content) )
          {
              result.addValidationInfo(new SwordValidationInfo(xmlName,
                      "The URI is not one of the types specified in http://purl.org/NET/sword-types", SwordValidationInfoType.WARNING));
          }
      }

      result.addUnmarshallValidationInfo(existing, attributeItems);
      return result;
   }


   /**
    * Get the content in this TextConstruct. 
    * 
    * @return The content, expressed as a string. 
    */
   public final String getContent() {
	   return content;
   }

   /**
    * Set the content. This only supports text content.  
    *  
    * @param content The content. 
    */
   public final void setContent(String content)
   {
	   this.content = content;
	}
   
   /**
    * Get the type. 
    * 
    * @return The type. 
    */
   public final QualityValue getQualityValue()
   {
      return qualityValue;
   }
   
   /**
    * Set the type. 
    * 
    * @param value The type.
    */
   public final void setQualityValue(QualityValue value)
   {
      this.qualityValue = value;
   }
   
   /** 
    * Get a string representation. 
    * 
    * @return The string. 
    */
   @Override
   public String toString()
   {
      return "Summary - content: " + getContent() + " value: " + getQualityValue();
   }
}
