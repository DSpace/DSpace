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

import nu.xom.Element;
import nu.xom.Elements;

import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

/**
 * Represents an Author type, as used in ATOM. This class is used as the 
 * base class for the different areas of ATOM that represent information 
 * about people. This includes the atom:author and atom:contributor
 * elements. 
 * 
 * @author Neil Taylor
 */
public class Author extends XmlElement implements SwordElementInterface
{
   /**
    * The author's name. 
    */
   private String name; 
   
   /**
    * The author's URI.  
    */
   private String uri;
   
   /**
    * The author's email. 
    */
   private String email; 
   
   
   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'author'.  
    */
   public Author()
   {
      this("atom", "author");   
   }
   
   /**
    * Create a new instance and set the element name. 
    * 
    * @param prefix The prefix to use when marshalling the data. 
    * @param localName The localName to use when marshalling the data. 
    */
   public Author(String prefix, String localName )
   {
      super(prefix, localName);
   }
      
   /**
    * Marshall the data in this object to a XOM Element. The element
    * will have the full name that is specified in the constructor. 
    * 
    * @return A XOM Element. 
    */
   public Element marshall()
   {
	   Element element = new Element(getQualifiedName(), Namespaces.NS_ATOM);
	         
	   if( name != null )
	   {
	      Element nameElement = new Element(getQualifiedName("name"), Namespaces.NS_ATOM);
	      nameElement.appendChild(name);
	      element.appendChild(nameElement);
	   }
	   
	   if( uri != null )
	   {
	      Element uriElement = new Element(getQualifiedName("uri"), Namespaces.NS_ATOM);
	      uriElement.appendChild(uri);
	      element.appendChild(uriElement);
	   }
	   
	   if( email != null )
	   {
	      Element emailElement = new Element(getQualifiedName("email"), Namespaces.NS_ATOM);
	      emailElement.appendChild(email);
	      element.appendChild(emailElement);
	   }
	   
	   return element;
   }
   

   /**
    * Unmarshall the author details from the specified element. The element 
    * is a XOM element. 
    * 
    */
   public void unmarshall(Element author)
   throws UnmarshallException
   {
      if( ! isInstanceOf( author, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException("Element is not of the correct type");
      }
      
	  try
	  {
		 // retrieve all of the sub-elements
		 Elements elements = author.getChildElements();
		 Element element = null; 
		 int length = elements.size();

		 for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            
    		if( isInstanceOf(element, "name", Namespaces.NS_ATOM ))
			{
			    name = unmarshallString(element);
			}
    		if( isInstanceOf(element, "uri", Namespaces.NS_ATOM ))
			{
			    uri = unmarshallString(element);
			}
    		if( isInstanceOf(element, "email", Namespaces.NS_ATOM ))
			{
			    email = unmarshallString(element);
			}
			else
			{
			   // unknown element type
			   //counter.other++; 
			}
         } // for 
	  }
	  catch( UnmarshallException ex )
	  {
         InfoLogger.getLogger().writeError("Unable to parse an element in " + getQualifiedName() + ": " + ex.getMessage());
         throw ex;
	  }
      
   }

   /**
    * Retrieve the author name. 
    * 
    * @return The name. 
    */
   public String getName() 
   {
	  return name;
   }

   /**
    * Set the author name. 
    * 
    * @param name The name. 
    */
   public void setName(String name) 
   {
	  this.name = name;
   }

   /**
    * Get the author URI. 
    * 
    * @return The URI. 
    */
   public String getUri() 
   {
	  return uri;
   }

   /**
    * Set the author URI. 
    * 
    * @param uri the URI. 
    */
   public void setUri(String uri) 
   {
	  this.uri = uri;
   }

   /**
    * Get the author email. 
    * 
    * @return The email. 
    */
   public String getEmail() 
   {
	  return email;
   }

   /**
    * Set the author email. 
    * 
    * @param email The email. 
    */
   public void setEmail(String email) 
   {
	  this.email = email;
   } 

}
