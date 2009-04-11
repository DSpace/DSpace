/**
 * Copyright (c) 2008-2009, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.atom;

import java.util.ArrayList;
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
    * Marshall the data stored in this object into Element objects. 
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
    * Unmarshall the contents of the source element into the internal data objects
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
    * @param validate
    * @return
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

   public SwordValidationInfo validate(ArrayList<SwordValidationInfo> elements,
           ArrayList<SwordValidationInfo> attributes,
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
