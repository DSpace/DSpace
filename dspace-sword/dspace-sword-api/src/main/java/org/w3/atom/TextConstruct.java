/**
 * Copyright (c) 2007, Aberystwyth University
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
package org.w3.atom;

import nu.xom.Attribute;
import nu.xom.Element;

import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

/**
 * Represents a text construct in the ATOM elements. This is a superclass of 
 * several elements within this implementation. 
 * 
 * @author Neil Taylor
 */
public class TextConstruct extends XmlElement implements SwordElementInterface
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
	 * Create a new instance, specifying the prefix and local name. 
	 * 
	 * @param prefix The prefix. 
	 * @param name   The local name. 
	 */
	public TextConstruct(String prefix, String name)
	{
	   super(prefix, name);   
	}
	
	/**
	 * Create a new instance. Set the default type to TextConstructType.TEXT.
	 * 
	 * @param name The name that will be applied.
	 */
	public TextConstruct(String name)
	{
       super(name);
       this.type = ContentType.TEXT;
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
         Attribute typeAttribute = new Attribute("type", type.toString());
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
      if( ! isInstanceOf(text, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException( "Not a " + getQualifiedName() + " element" );
      }
      try
	   {
         // get the attributes
         int attributeCount = text.getAttributeCount();
         Attribute attribute = null; 
         for( int i = 0; i < attributeCount; i++ )
         {
            attribute = text.getAttribute(i);
            if( "type".equals(attribute.getQualifiedName()))
            {
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
                   InfoLogger.getLogger().writeError("Unable to parse extract type in " + getQualifiedName() );
                   // FIXME - check error handling here
                }
            }
         }
         
		   // retrieve all of the sub-elements
		   int length = text.getChildCount();
         if( length > 0 )
         {
            content = unmarshallString(text);
         }
         // FIXME - the above only handles plain text content. 
	   }
	   catch( Exception ex )
	   {
		   InfoLogger.getLogger().writeError("Unable to parse an element in " + getQualifiedName() + ": " + ex.getMessage());
	      throw new UnmarshallException("Unable to parse an element in " + getQualifiedName(), ex);
	   }
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
   public void setContent(String content) {
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
}
