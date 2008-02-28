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
import nu.xom.Elements;

import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

/**
 * Represents an ATOM Link element. 
 * 
 * @author Neil Taylor
 */
public class Link extends XmlElement implements SwordElementInterface
{
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
    * Create a new instance and set prefix and local name to 'atom' and 'link', 
    * respectively. 
    */
   public Link()
   {
	   super("atom", "link");
   }
	
   /**
    * Mashall the data stored in this object into Element objects. 
    * 
    * @return An element that holds the data associated with this object. 
    */
   public Element marshall()
   {
	   Element element = new Element(getQualifiedName(), Namespaces.NS_ATOM);
       
	   if( content != null )
	   {
		   element.appendChild(content);
	   }
	   
	   if( href != null ) 
	   {
		   Attribute hrefAttribute = new Attribute("href", href);
		   element.addAttribute(hrefAttribute);
	   }
	   
	   if( rel != null ) 
	   {
		   Attribute relAttribute = new Attribute("rel", rel);
		   element.addAttribute(relAttribute);
	   }
	   
	   if( type != null ) 
	   {
		   Attribute typeAttribute = new Attribute("type", type);
		   element.addAttribute(typeAttribute);
	   }
	   
	   if( hreflang != null ) 
	   {
		   Attribute hreflangAttribute = new Attribute("hreflang", hreflang);
		   element.addAttribute(hreflangAttribute);
	   }
	   
	   if( title != null ) 
	   {
		   Attribute titleAttribute = new Attribute("title", title);
		   element.addAttribute(titleAttribute);
	   }
	   
	   if( length != null ) 
	   {
		   Attribute lengthAttribute = new Attribute("length", length);
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
      if( ! isInstanceOf(link, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException( "Not an atom:link element" );
      }
      
	   try
	   {
		   // get the attributes
		   int attributeCount = link.getAttributeCount();
		   Attribute attribute = null; 
		   for( int i = 0; i < attributeCount; i++ )
		   {
		      attribute = link.getAttribute(i);
		      if( "href".equals(attribute.getQualifiedName()))
	          {
	             href = attribute.getValue();
	          }
		      else if( "rel".equals(attribute.getQualifiedName()))
	          {
	             rel = attribute.getValue();
	          }
		      else if( "type".equals(attribute.getQualifiedName()))
	          {
		         type = attribute.getValue();
		      }
		      else if( "hreflang".equals(attribute.getQualifiedName()))
	          {
		    	 // FIXME - is this the correct element?
		         hreflang = attribute.getValue();
		      }
		      else if( "title".equals(attribute.getQualifiedName()))
	          {
		         title = attribute.getValue();
		      }
		      else if( "length".equals(attribute.getQualifiedName()))
	          {
		         length = attribute.getValue();
		      }
		   }
		   
		   // retrieve all of the sub-elements
		   Elements elements = link.getChildElements();
		   Element element = null; 
		   int length = elements.size();

		   for(int i = 0; i < length; i++ )
		   {
			   element = elements.get(i);
			   content = unmarshallString(element);
			   // FIXME - is this correct? 
			   
		   } // for 
	   }
	   catch( Exception ex )
	   {
		   InfoLogger.getLogger().writeError("Unable to parse an element in Link: " + ex.getMessage());
	      throw new UnmarshallException("Unable to parse element in link", ex);
	   }
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

}
