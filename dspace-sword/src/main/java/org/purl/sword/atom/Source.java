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
import nu.xom.Element;
import nu.xom.Elements;

import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;
import org.purl.sword.base.XmlName;
import org.apache.log4j.Logger;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;

/**
 * Represents an ATOM Generator element. 
 * 
 * @author Neil Taylor
 */
public class Source extends XmlElement implements SwordElementInterface
{
	/**
	 * Local name for the element. 
	 */
   private static final XmlName XML_NAME = new XmlName(
           Namespaces.PREFIX_ATOM, "source", Namespaces.NS_ATOM);
   
   /**
    * The generator data for this object. 
    */
   private Generator generator; 

   /**
    * The log. 
    */
   private static Logger log = Logger.getLogger(Source.class);
   
   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'source'.  
    */
   public Source()
   {
      super(XML_NAME);
   }

   public static XmlName elementName()
   {
       return XML_NAME;
   }

   /**
    * Marshal the data stored in this object into Element objects. 
    * 
    * @return An element that holds the data associated with this object. 
    */
   public Element marshall()
   {
      Element source = new Element(getQualifiedName(), xmlName.getNamespace());
      
      if( generator != null )
      {
         source.appendChild(generator.marshall());
      }
      
      return source;
   }
   
   /**
    * Unmarshal the contents of the source element into the internal data objects
    * in this object. 
    * 
    * @param source The Source element to process. 
    *
    * @throws UnmarshallException If the element does not contain an ATOM Source
    *         element, or if there is a problem processing the element or any 
    *         sub-elements. 
    */
   public void unmarshall(Element source) 
   throws UnmarshallException 
   {
      unmarshall(source, null);
   }

   /**
    * 
    * @param source
    * @param validationProperties
    * @throws org.purl.sword.base.UnmarshallException
    */
   public SwordValidationInfo unmarshall(Element source, Properties validationProperties)
   throws UnmarshallException
   {
      if( ! isInstanceOf(source, xmlName.getLocalName(), Namespaces.NS_ATOM))
      {
         //throw new UnmarshallException( "Not an atom:source element" );
          return handleIncorrectElement(source, validationProperties);
      }

      ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
      ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();

      try
      {
         processUnexpectedAttributes(source, attributeItems);

         // retrieve all of the sub-elements
         Elements elements = source.getChildElements();
         Element element = null; 
         int length = elements.size();

         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            if( isInstanceOf(element, Generator.elementName()) )
            {
               generator = new Generator(); 
               generator.unmarshall(element);
            }
            else
            {
               SwordValidationInfo info = new SwordValidationInfo(new XmlName(element),
                       SwordValidationInfo.UNKNOWN_ELEMENT,
                       SwordValidationInfoType.INFO);
               info.setContentDescription(element.getValue());
               validationItems.add(info);
            }
         }
      }
      catch( Exception ex )
      {
         log.error("Unable to parse an element in Source: " + ex.getMessage());
         throw new UnmarshallException("Unable to parse an element in Source", ex);
      }

      SwordValidationInfo result = null;
      if( validationProperties != null )
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
       SwordValidationInfo result = new SwordValidationInfo(xmlName);
       
       
       result.addUnmarshallValidationInfo(elements, attributes);
       return result;
   }


   /**
    * Get the generator. 
    * 
    * @return The generator. 
    */
   public Generator getGenerator()
   {
      return generator;
   }

   /**
    * Set the generator. 
    * 
    * @param generator The generator. 
    */
   public void setGenerator(Generator generator)
   {
      this.generator = generator;
   }
}
