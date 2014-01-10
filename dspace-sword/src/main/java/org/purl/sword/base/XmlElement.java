/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.List;
import java.util.Properties;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import org.apache.log4j.Logger;

/**
 * Parent class for all classes that represent an XML element. This provides
 * some common utility methods that are useful for marshalling and 
 * unmarshalling data. 
 * 
 * @author Neil Taylor
 */
public abstract class XmlElement
{

    /** Logger */
   private static Logger log = Logger.getLogger(XmlElement.class);


   /**
    *
    */
   protected XmlName xmlName;


   public XmlName getXmlName()
   {
       // FIXME - should this be a clone?
       return xmlName; 
   }

   /**
    * The name to use for the prefix. E.g. atom:title, atom is the prefix. 
    */
   //protected String prefix;
   
   /**
    * The local name of the element. E.g. atom:title, title is the local name. 
    */
   //protected String localName;
      
   /**
    * Create a new instance. Set the local name that will be used. 
    * 
    * @param localName The local name for the element. 
    */
   public XmlElement(String localName)
   {
      this("", localName);
   }
   
   /**
    * Create a new instance. Set the prefix and local name. 
    * 
    * @param prefix The prefix for the element. 
    * @param localName The local name for the element. 
    */
   public XmlElement(String prefix, String localName)
   {
      this.xmlName = new XmlName(prefix, localName, "");
   }

   /**
    * Create a new insatnce. Set the prefix, local name and the namespace URI.
    *
    * @param prefix       The prefix.
    * @param localName    The element's local name. 
    * @param namespaceUri The namespace URI.
    */
   public XmlElement(String prefix, String localName, String namespaceUri)
   {
       this.xmlName = new XmlName(prefix, localName, namespaceUri);
   }

   /**
    * 
    * @param name
    */
   public XmlElement(XmlName name)
   {
       xmlName = name; 
   }
   
   /**
    * The Date format that is used to parse dates to and from the ISO format 
    * in the XML data. 
    */
   protected static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   
   /**
    * Array of possible date formats that are permitted for date elements. 
    */
   protected static final String[] DATE_FORMATS = 
   {
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SZ",
      "yyyy-MM-dd'T'HH:mm:ss.Sz",
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "yyyy-MM-dd'T'HH:mm:ssz",
      "yyyy-MM-dd'T'HH:mmZZZZ",
      "yyyy-MM-dd'T'HH:mmzzzz",
      "yyyy-MM-dd'T'HHZZZZ",
      "yyyy-MM-dd'T'HHzzzz",
      "yyyy-MM-dd'T'HH:mm:ss.S",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd'T'HH:mm",
      "yyyy-MM-dd'T'HH",
      "yyyy-MM-dd",
      "yyyy-MM",
      "yyyy"
   };
   
   /**
    * Extract a boolean value from the specified element. The boolean value 
    * is represented as the string 'true' or 'false' as the only child
    * of the specified element. 
    * 
    * @param element The element that contains the boolean value. 
    * @return True or false, based on the string in the element's content. 
    * @throws UnmarshallException If the element does not contain a single child, or if
    * the child does not contain the value 'true' or 'false'. 
    */
   protected boolean unmarshallBoolean( Element element )
   throws UnmarshallException 
   {
	  if( element.getChildCount() != 1 )
      {
         throw new UnmarshallException("Missing Boolean Value", null);
      }
      
      // ok to get the single child element. This should be a text element.
      try
      {
         Node child = element.getChild(0);
         String value = child.getValue();
         if( "true".equals(value) )
         {
        	   return true;
         }
         else if( "false".equals(value))
         {
        	   return false;
         }
         else
         {
        	   throw new UnmarshallException("Illegal Value");
         }
      }
      catch( IndexOutOfBoundsException ex )
      {
         throw new UnmarshallException("Error accessing Boolean element", ex);
      }
   }

   /**
    * Extract a string value from the specified element. The value 
    * is the only child of the specified element. 
    * 
    * @param element The element that contains the string value. 
    * @return The string. 
    * @throws UnmarshallException If the element does not contain a single child. 
    */
   protected String unmarshallString( Element element )
   throws UnmarshallException
   {
       if( element.getChildCount() != 1 )
	   {
	      throw new UnmarshallException("Missing String Value", null);
	   }
	      
	   // ok to get the single child element. This should be a text element.
	   try
	   {
	      Node child = element.getChild(0);
	      return child.getValue();
	   }
	   catch( IndexOutOfBoundsException ex )
	   {
	      throw new UnmarshallException("Error accessing String element", ex);
	   } 
	   
   }
   
   /**
    * Extract an integer value from the specified element. The integer value 
    * is represented as a string in the only child
    * of the specified element. 
    * 
    * @param element The element that contains the integer. 
    * @return The integer. 
    * @throws UnmarshallException If the element does not contain a single child, or if
    * the child does not contain the valid integer. 
    */
   protected int unmarshallInteger( Element element )
   throws UnmarshallException
   {
	   if( element.getChildCount() != 1 )
	   {
	      throw new UnmarshallException("Missing Integer Value", null);
	   }
	      
	   // ok to get the single child element. This should be a text element.
	   try
	   {
	      Node child = element.getChild(0);
	      return Integer.parseInt( child.getValue() );
	   }
	   catch( IndexOutOfBoundsException ex )
	   {
	      throw new UnmarshallException("Error accessing Integer", ex);
	   } 
	   catch( NumberFormatException nfex )
	   {
	      throw new UnmarshallException("Error formatting the number", nfex);
	   }
   }
      
   /**
    * Determines if the specified element is an instance of the element name. If 
    * you are checking the name title in the ATOM namespace, then the local name
    * should be 'title' and the namespaceURI is the URI for the ATOM namespace. 
    * 
    * @param element      The specified element. 
    * @param localName    The local name for the element. 
    * @param namespaceURI The namespace for the element. 
    * @return True if the element matches the localname and namespace. Otherwise, false. 
    */
   protected boolean isInstanceOf(Element element, String localName, String namespaceURI )
   {
      return (localName.equals(element.getLocalName()) && 
              namespaceURI.equals(element.getNamespaceURI()) );
   }

   /**
    * 
    * @param element
    * @param xmlName
    */
   protected boolean isInstanceOf(Element element, XmlName xmlName)
   {
       return (xmlName.getLocalName().equals(element.getLocalName()) &&
               xmlName.getNamespace().equals(element.getNamespaceURI()));
   }
   
   /**
    * Retrieve the qualified name for this object. This uses the
    * prefix and local name stored in this object. 
    * 
    * @return A string of the format 'prefix:localName'
    */
   public String getQualifiedName()
   {
      return getQualifiedName(xmlName.getLocalName());
   }

   /**
    * Retrieve the qualified name. The prefix for this object is prepended 
    * onto the specified local name. 
    * 
    * @param name the specified local name. 
    * @return A string of the format 'prefix:name'
    */
   public String getQualifiedName(String name)
   {
      return xmlName.getQualifiedName();
 
   }
   
   /**
    * Get the qualified name for the given prefix and name
    * 
    * @param prefix the prefix
    * @param name the name
    * @return the qualified name
    */
   public String getQualifiedNameWithPrefix(String prefix, String name)
   {
	   return prefix + ":" + name;
   }


   public abstract SwordValidationInfo validate(Properties validationContext);

   protected void processUnexpectedAttributes(Element element, List<SwordValidationInfo> attributeItems)
   {
       int attributeCount = element.getAttributeCount();
       Attribute attribute = null;

       for( int i = 0; i < attributeCount; i++ )
       {
            attribute = element.getAttribute(i);
            XmlName attributeName = new XmlName(attribute.getNamespacePrefix(),
                       attribute.getLocalName(),
                       attribute.getNamespaceURI());

            SwordValidationInfo info = new SwordValidationInfo(xmlName, attributeName,
                       SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                       SwordValidationInfoType.INFO);
            info.setContentDescription(attribute.getValue());
            attributeItems.add(info);
       }
   }

   /**
    * Add the information to the unmarshall attribute section of the specified
    * info object.
    * 
    * @param element
    * @param info
    */
   protected void processUnexpectedAttributes(Element element, SwordValidationInfo info)
   {
       int attributeCount = element.getAttributeCount();
       Attribute attribute = null;

       for( int i = 0; i < attributeCount; i++ )
       {
            attribute = element.getAttribute(i);
            XmlName attributeName = new XmlName(attribute.getNamespacePrefix(),
                       attribute.getLocalName(),
                       attribute.getNamespaceURI());

            SwordValidationInfo item = new SwordValidationInfo(xmlName, attributeName,
                       SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                       SwordValidationInfoType.INFO);
            item.setContentDescription(attribute.getValue());
            info.addUnmarshallAttributeInfo(item);
       }
   }

   protected SwordValidationInfo handleIncorrectElement(Element element, Properties validationProperties)
   throws UnmarshallException
   {
       log.error("Unexpected element. Expected: " + getQualifiedName() + ". Got: " +
				   ((element != null) ? element.getQualifiedName() : "null" ));

       if( validationProperties != null )
       {
          SwordValidationInfo info = new SwordValidationInfo(
                    new XmlName(element.getNamespacePrefix(), element.getLocalName(), element.getNamespaceURI()),
                    "This is not the expected element. Received: " + element.getQualifiedName() + " for namespaceUri: " + element.getNamespaceURI(),
                    SwordValidationInfoType.ERROR
                    );
          return info;
       }
       else
       {
           throw new UnmarshallException( "Not a " + getQualifiedName() + " element" );
       }
   }

   protected SwordValidationInfo createValidAttributeInfo(String name, String content)
   {
      XmlName attributeName = new XmlName(xmlName.getPrefix(),
                       name,
                       xmlName.getNamespace());

      SwordValidationInfo item = new SwordValidationInfo(xmlName, attributeName);
      item.setContentDescription(content);
      //attributeItems.add(item);
      return item; 
   }

   
}
