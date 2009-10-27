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
package org.purl.sword.base;

import java.util.ArrayList;
import java.util.Properties;
import nu.xom.Element;


import org.apache.log4j.Logger;
import org.purl.sword.base.XmlName;

/**
 * Represents a text construct in the ATOM elements. This is a superclass of 
 * several elements within this implementation. 
 * 
 * @author Neil Taylor
 */
public abstract class BasicContentElement extends XmlElement
implements SwordElementInterface
{
   /**
	 * The log. 
	 */
	private static Logger log = Logger.getLogger(BasicContentElement.class);
	
	public BasicContentElement(String prefix, String name, String namespaceUri)
    {
        super(prefix, name, namespaceUri);
    }

    public BasicContentElement(XmlName name)
    {
        super(name);
    }

    /**
	 * Marshall the data in this object to an Element object. 
	 * 
	 * @return The data expressed in an Element. 
	 */
    public Element marshall()
    {
      Element element = new Element(getQualifiedName(), xmlName.getNamespace());
      marshallContent(element); 
	  return element;
   }

   protected abstract void marshallContent(Element element);

   /**
    * Unmarshall the text element into this object.
    * 
    * This unmarshaller only handles plain text content, although it can 
    * recognise the three different type elements of text, html and xhtml. This
    * is an area that can be improved in a future implementation, if necessary. 
    * 
    * @param text The text element. 
    * 
    * @throws UnmarshallException If the specified element is not of
    *                             the correct type, where the localname is used
    *                             to specify the valid name. Also thrown 
    *                             if there is an issue accessing the data. 
    */
   public SwordValidationInfo unmarshall(Element element, Properties validationProperties)
   throws UnmarshallException
   {
	   if( ! isInstanceOf(element, xmlName) )
	   {
           return handleIncorrectElement(element, validationProperties);
	   }

       ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
       ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();

       try
	   {
           processUnexpectedAttributes(element, attributeItems);
		   
           int length = element.getChildCount();
		   if( length > 0 )
		   {
               try
               {
                  unmarshallContent(element); 
               }
               catch( UnmarshallException ume )
               {
                  log.error("Error accessing the content of the " + xmlName.getQualifiedName() + "  element");
                  if( validationProperties == null  )
                  {
                      throw ume;
                  }
                  else
                  {
                      SwordValidationInfo info = new SwordValidationInfo(xmlName,
                              SwordValidationInfo.ERROR_WITH_CONTENT, SwordValidationInfoType.ERROR);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
		   }

	   }
	   catch( Exception ex )
	   {
		   log.error("Unable to parse an element in " + getQualifiedName() + ": " + ex.getMessage());
           if( validationProperties == null )
           {
		     throw new UnmarshallException("Unable to parse an element in " + getQualifiedName(), ex);
	       }
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
    * @return
    */
   protected SwordValidationInfo validate(ArrayList<SwordValidationInfo> existing,
           ArrayList<SwordValidationInfo> attributeItems,
           Properties validationContext)
   {
      SwordValidationInfo result = new SwordValidationInfo(xmlName); 
      result.setContentDescription(getContentAsString());

      SwordValidationInfo contentResult = validateContent(validationContext);
      if( contentResult != null )
      {
         result.addValidationInfo(contentResult);
      }
      
      result.addUnmarshallValidationInfo(existing, attributeItems);
      return result;
   }


   protected abstract void unmarshallContent(Element element)
   throws UnmarshallException;


   protected abstract SwordValidationInfo validateContent(Properties validationContext);


   protected abstract String getContentAsString();
   
}
