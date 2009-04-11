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
import nu.xom.Attribute;
import nu.xom.Element;

import nu.xom.Node;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

import org.apache.log4j.Logger;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;
import org.purl.sword.base.XmlName;

/**
 * Represents an ATOM Content element. 
 * 
 * @author Neil Taylor
 */
public class Content extends XmlElement implements SwordElementInterface
{
   /**
    * The identifier for the src attribute. 
    */
   public static final String ATTRIBUTE_SRC = "src";
   
   /**
    * The identifier for the type attribute. 
    */
   public static final String ATTRIBUTE_TYPE = "type";
   
   /**
    * The data for the type attribute. 
    */
   private String type; 
   
   /**
    * The data for the source attribute. 
    */
   private String source; 

   /**
    * The log.
    */
   private static Logger log = Logger.getLogger(Content.class);

   /**
    * 
    */
   private static final XmlName XML_NAME =
           new XmlName(Namespaces.PREFIX_ATOM, "content", Namespaces.NS_ATOM);

   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'content'.  
    */
   public Content()
   {
      super(XML_NAME);
   }

   public static XmlName elementName()
   {
       return XML_NAME;
   }
   
   /**
    * Get the Source. 
    * 
    * @return The Source. 
    */
   public String getSource()
   {
      return source;
   }

   /**
    * Set the Source. 
    * 
    * @param source The source. 
    */
   public void setSource(String source)
   {
      this.source = source;
   }

   /**
    * Get the type. 
    * 
    * @return The type. 
    */
   public String getType()
   {
      return type;
   }

   /**
    * Set the type for the content. This should match the pattern 
    * ".* /.*" [Note, there is no space before the /, this has been added
    * to allow this text to be written in a Java comment.]. 
    * 
    * An example of the type is <code>application/zip</code>. 
    * 
    * @param type The specified type. 
    * @throws InvalidMediaTypeException If the specified type is null or
    * it does not match the specified pattern. 
    */
   public void setType(String type)
   throws InvalidMediaTypeException
   {
      if( type == null || ! type.matches(".*/.*") )
      {
         throw new InvalidMediaTypeException("Type: '" + type + "' does not match .*/.*");
      }
      
      this.type = type;
   }

   /**
    * Marshall the data in this object to an Element object.
    * 
    * @return A XOM Element that holds the data for this Content element. 
    */
   public Element marshall()
   {
      Element content = new Element(getQualifiedName(), Namespaces.NS_ATOM);
         
      if( type != null )
      {
         Attribute typeAttribute = new Attribute(ATTRIBUTE_TYPE, type);
         content.addAttribute(typeAttribute);
      }
       
      if( source != null )
      {
         Attribute typeAttribute = new Attribute(ATTRIBUTE_SRC, source);
         content.addAttribute(typeAttribute);
      }
      
      return content;
   }

   public void unmarshall(Element content)
   throws UnmarshallException
   {
      unmarshall(content, null);
   }

   /**
    * Unmarshall the content element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             content element or if there are problems
    *                             accessing the data. 
    */
   public SwordValidationInfo unmarshall(Element content, Properties validationProperties)
   throws UnmarshallException 
   {

      if( ! isInstanceOf( content, xmlName.getLocalName(), Namespaces.NS_ATOM))
      {
         return handleIncorrectElement(content, validationProperties);
      }

      ArrayList<SwordValidationInfo> elements = new ArrayList<SwordValidationInfo>();
      ArrayList<SwordValidationInfo> attributes = new ArrayList<SwordValidationInfo>();
      
      try
      {
         // get the attributes
         int attributeCount = content.getAttributeCount();
         Attribute attribute = null; 
         for( int i = 0; i < attributeCount; i++ )
         {
            attribute = content.getAttribute(i);
            String name = attribute.getQualifiedName();
            if( ATTRIBUTE_TYPE.equals(name))
            {
                type = attribute.getValue();
                if( validationProperties != null )
                {
                   attributes.add(createValidAttributeInfo(ATTRIBUTE_TYPE, type));
                }
            }
            else if( ATTRIBUTE_SRC.equals(name) )
            {
               source = attribute.getValue();
               if( validationProperties != null )
               {
                  attributes.add(createValidAttributeInfo(ATTRIBUTE_SRC, source));
               }
            }
            else
            {
               SwordValidationInfo info = new SwordValidationInfo(xmlName,
                       new XmlName(attribute),
                       SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                       SwordValidationInfoType.INFO );
                info.setContentDescription(attribute.getValue());
                attributes.add(info);
            }
         }

         // check if there is any content. If there is, add a simple message to
         // say that there are sub elements that are not used in this profile
         if( content.getChildCount() > 0 )
         {
            elements.add(new SwordValidationInfo(xmlName,
                    "This element has child elements. These are not expected as part of the SWORD profile",
                    SwordValidationInfoType.INFO));
         }

      }
      catch( Exception ex )
      {
         log.error("Unable to parse an element in Content: " + ex.getMessage());
         throw new UnmarshallException("Error parsing Content", ex);
      }

      SwordValidationInfo result = null;
      if( validationProperties != null )
      {
          result = validate(elements, attributes, validationProperties);
      }
      return result;
   }

   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, null, validationContext);
   }

   /**
    * 
    * @param elements
    * @param attributes
    * @return
    */
   protected SwordValidationInfo validate(ArrayList<SwordValidationInfo> elements,
           ArrayList<SwordValidationInfo> attributes,
           Properties validationContext)
   {
       SwordValidationInfo info = new SwordValidationInfo(xmlName);

       if( source == null )
       {
           XmlName attributeName = new XmlName(xmlName.getPrefix(),
                       ATTRIBUTE_SRC,
                       xmlName.getNamespace());

          SwordValidationInfo item = new SwordValidationInfo(xmlName, attributeName,
                  SwordValidationInfo.MISSING_ATTRIBUTE_WARNING,
                  SwordValidationInfoType.ERROR);
          info.addValidationInfo(item);
       }

       info.addUnmarshallValidationInfo(elements, attributes);
       return info; 
   }
   
   /**
    * Get a string representation. 
    * 
    * @return String
    */
   @Override
   public String toString()
   {
      return "Content - source: " + getSource() + " type: " + getType();
   }
}
