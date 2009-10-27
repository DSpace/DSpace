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

import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

import org.apache.log4j.Logger;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;
import org.purl.sword.base.XmlName;

/**
 * Represents a text construct in the ATOM elements. This is a superclass of 
 * several elements within this implementation. 
 * 
 * @author Neil Taylor
 */
public class TextConstruct extends XmlElement 
implements SwordElementInterface
{
   /**
    * The content in the element. 
    */
	private String content;  

	/**
	 * The type of the element. 
	 */
	private ContentType type;

	/**
	 * The log. 
	 */
	private static Logger log = Logger.getLogger(TextConstruct.class);
	
	/** 
	 * label for the type attribute. 
	 */
	public static final String ATTRIBUTE_TYPE = "type";
	
	/**
	 * Create a new instance, specifying the prefix and local name. 
	 * 
	 * @param prefix The prefix. 
	 * @param name   The local name. 
	 */
	public TextConstruct(String prefix, String name)
	{
	   this(prefix, name, Namespaces.NS_ATOM);
	}
	
	/**
	 * Create a new instance. Set the default type to TextConstructType.TEXT.
	 * 
	 * @param name The name that will be applied.
	 */
	public TextConstruct(String name)
	{
       this(Namespaces.PREFIX_ATOM, name);
	}

    /**
     * Create a new instance. Set the XML name for the element.
     *
     * @param name The name to set. 
     */
    public TextConstruct(XmlName name)
    {
        super(name); 
    }

    /**
     * 
     * @param prefix
     * @param name
     * @param namespaceUri
     */
    public TextConstruct(String prefix, String name, String namespaceUri)
    {
        super(prefix, name, namespaceUri);
        initialise();
    }

    /**
     * 
     */
    protected void initialise()
    {
        this.type = ContentType.TEXT;
        this.content = null; 
    }

	/**
	 * Marshall the data in this object to an Element object. 
	 * 
	 * @return The data expressed in an Element. 
	 */
   public Element marshall()
   {
      Element element = new Element(getQualifiedName(), Namespaces.NS_ATOM);
      if( type != null )
      {
         Attribute typeAttribute = new Attribute(ATTRIBUTE_TYPE, type.toString());
         element.addAttribute(typeAttribute);
      }
      
      if( content != null )
	   {
		   element.appendChild(content);
	   }
	   return element;
   }

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
   public void unmarshall(Element text)
   throws UnmarshallException
   {
       unmarshall(text, null);
   }

   /**
    * 
    * @param text
    * @param validate
    * @return
    * @throws org.purl.sword.base.UnmarshallException
    */
   public SwordValidationInfo unmarshall(Element text, Properties validationProperties)
   throws UnmarshallException
   {
	   if( ! isInstanceOf(text, xmlName))
	   {
		   return handleIncorrectElement(text, validationProperties);
	   }

       ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
       ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();

	   try
	   {
           initialise();
           
		   // get the attributes
		   int attributeCount = text.getAttributeCount();
		   Attribute attribute = null;
		   for( int i = 0; i < attributeCount; i++ )
		   {
			   attribute = text.getAttribute(i);
			   if( ATTRIBUTE_TYPE.equals(attribute.getQualifiedName()))
			   {
                   boolean success = true;
				   String value = attribute.getValue();
				   if( ContentType.TEXT.toString().equals(value) )
				   {
					   type = ContentType.TEXT;
				   }
				   else if( ContentType.HTML.toString().equals(value) )
				   {
					   type = ContentType.HTML;
				   }
				   else if( ContentType.XHTML.toString().equals(value) )
				   {
					   type = ContentType.XHTML;
				   }
				   else
				   {
					   log.error("Unable to parse extract type in " + getQualifiedName() );
                       SwordValidationInfo info = new SwordValidationInfo(xmlName,
                                  new XmlName(attribute),
                                  "Invalid content type has been specified",
                                  SwordValidationInfoType.ERROR);
                       info.setContentDescription(value);
                       attributeItems.add(info);
                       success = false;
				   }

                   if( success )
                   {
                       SwordValidationInfo info = new SwordValidationInfo(xmlName, new XmlName(attribute));
                       info.setContentDescription(type.toString());
                       attributeItems.add(info);
                   }
			   }
               else
               {
                   SwordValidationInfo info = new SwordValidationInfo(xmlName,
                              new XmlName(attribute),
                              SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                              SwordValidationInfoType.INFO);
                   info.setContentDescription(attribute.getValue());
                   attributeItems.add(info);
               }
		   }

		   // retrieve all of the sub-elements
		   int length = text.getChildCount();
		   if( length > 0 )
		   {
			   content = unmarshallString(text);
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
      boolean validateAll = (existing == null);

      SwordValidationInfo result = new SwordValidationInfo(xmlName); 
      result.setContentDescription(content);
      
      // item specific rules
      if( content == null )
      {
          result.addValidationInfo(
                  new SwordValidationInfo(xmlName, "Missing content for element",
                                          SwordValidationInfoType.WARNING));
      }

      if( validateAll )
      {
          SwordValidationInfo info = new SwordValidationInfo(xmlName,
                  new XmlName(xmlName.getPrefix(), ATTRIBUTE_TYPE, xmlName.getNamespace()));
          info.setContentDescription(type.toString());
          result.addAttributeValidationInfo(info);

      }

      result.addUnmarshallValidationInfo(existing, attributeItems);
      return result; 
   }


   /**
    * Get the content in this TextConstruct. 
    * 
    * @return The content, expressed as a string. 
    */
   public String getContent() {
	   return content;
   }

   /**
    * Set the content. This only supports text content.  
    *  
    * @param content The content. 
    */
   public void setContent(String content) 
   {
	   this.content = content;
	}
   
   /**
    * Get the type. 
    * 
    * @return The type. 
    */
   public ContentType getType() 
   {
      return type; 
   }
   
   /**
    * Set the type. 
    * 
    * @param type The type. 
    */
   public void setType(ContentType type)
   {
      this.type = type;
   }
   
   /** 
    * Get a string representation. 
    * 
    * @return The string. 
    */
   public String toString()
   {
      return "Summary - content: " + getContent() + " type: " + getType();
   }
}
