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
import org.apache.log4j.Logger;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;
import org.purl.sword.base.XmlName;

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
	* Local name for the element. 
	*/
   @Deprecated
   public static final String ELEMENT_NAME = "author";
   
   /**
    * Label for the 'name' attribute. 
    */
   @Deprecated
   public static final String ELEMENT_AUTHOR_NAME = "name";
   
   /**
    * Label for the 'uri' attribute. 
    */
   @Deprecated
   public static final String ELEMENT_URI = "uri";
   
   /**
    * Label for the 'email' attribute. 
    */
   @Deprecated
   public static final String ELEMENT_EMAIL = "email";
   
   /**
    * The author's name. 
    */
   private Name name;

   /**
    * The author's URI.  
    */
   private Uri uri;

   /**
    * The author's email. 
    */
   private Email email;

   /**
    * The logger. 
    */
   private static Logger log = Logger.getLogger(Author.class);

   /**
    *
    */
   private static final XmlName XML_NAME =
           new XmlName(Namespaces.PREFIX_ATOM, "author", Namespaces.NS_ATOM);

   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'author'.  
    */
   public Author()
   {
      this(XML_NAME);
   }

   public Author(XmlName name)
   {
      super(name); 
   }

   /**
    * Create a new instance and set the element name. 
    * 
    * @param prefix The prefix to use when marshalling the data. 
    * @param localName The localName to use when marshalling the data. 
    */
   public Author(String prefix, String localName )
   {
      this(prefix, localName, XML_NAME.getNamespace());
   }

   /**
    *
    * @param prefix
    * @param localName
    * @param namespaceUri
    */
   public Author(String prefix, String localName, String namespaceUri)
   {
      super(prefix, localName, XML_NAME.getNamespace());
   }

   /**
     * Get the XmlName for this class.
     *
     * @return The prefix, localname and namespace for this element.
     */
    public static XmlName elementName()
    {
        return XML_NAME;
    }

   /**
    * Marshall the data in this object to a XOM Element. The element
    * will have the full name that is specified in the constructor. 
    * 
    * @return A XOM Element. 
    */
   public Element marshall()
   {
      Element element = new Element(getQualifiedName(), xmlName.getNamespace());

      if( name != null )
      {
         element.appendChild(name.marshall());
      }

      if( uri != null )
      {
         element.appendChild(uri.marshall());
      }

      if( email != null )
      {
         element.appendChild(email.marshall());
      }

      return element;
   }


   /**
    * Unmarshall the author details from the specified element. The element
    * is a XOM element.
    *
    * @param author The element to unmarshall.
    */
   public SwordValidationInfo unmarshall(Element author, Properties validationProperties)
   throws UnmarshallException
   {
      if( ! isInstanceOf( author, xmlName) )
      {
         handleIncorrectElement(author, validationProperties);
      }

      ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
      ArrayList<SwordValidationInfo> attributeItems = new ArrayList<SwordValidationInfo>();

      processUnexpectedAttributes(author, attributeItems);
      
      // retrieve all of the sub-elements
      Elements elements = author.getChildElements();
      Element element = null;
      int length = elements.size();

      for(int i = 0; i < length; i++ )
      {
         element = elements.get(i);

         if( isInstanceOf(element, Name.elementName() ))
         {
            name = new Name();
            validationItems.add(name.unmarshall(element, validationProperties));
         }
         else if( isInstanceOf(element, Uri.elementName()))
         {
            uri = new Uri();
            validationItems.add(uri.unmarshall(element, validationProperties));
            
         }
         else if( isInstanceOf(element, Email.elementName() ))
         {
            email = new Email();
            validationItems.add(email.unmarshall(element, validationProperties));
         }
         else if( validationProperties != null )
         {
            SwordValidationInfo info = new SwordValidationInfo(new XmlName(element),
                    SwordValidationInfo.UNKNOWN_ELEMENT,
                    SwordValidationInfoType.INFO);
            info.setContentDescription(element.getValue());
            validationItems.add(info);
         }

      } // for

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

       if( name == null )
       {
           SwordValidationInfo info = new SwordValidationInfo(Name.elementName(),
                   SwordValidationInfo.MISSING_ELEMENT_ERROR,
                   SwordValidationInfoType.ERROR);
           result.addValidationInfo(info);
       }
       else if( elements == null && name != null)
       {
          result.addValidationInfo(name.validate(validationContext));
       }

       if( elements == null && uri != null )
       {
           result.addValidationInfo(uri.validate(validationContext));
       }

       if( elements == null && email != null )
       {
           result.addValidationInfo(email.validate(validationContext));
       }

       result.addUnmarshallValidationInfo(elements, attributes);
       return result;
   }

   /**
    * Unmarshall the author details from the specified element. The element 
    * is a XOM element. 
    * 
    * @param author The element to unmarshall. 
    */
   public void unmarshall(Element author)
   throws UnmarshallException
   {
      unmarshall(author, null);
   }

   /**
    * Retrieve the author name. 
    * 
    * @return The name. 
    */
   public String getName() 
   {
      if( name == null )
      {
          return null;
      }
      return name.getContent();
   }

   /**
    * Set the author name. 
    * 
    * @param name The name. 
    */
   public void setName(String name) 
   {
      this.name = new Name(name);
   }

   /**
    * Get the author URI. 
    * 
    * @return The URI. 
    */
   public String getUri() 
   {
      if( uri == null )
      {
          return null;
      }
      return uri.getContent();
   }

   /**
    * Set the author URI. 
    * 
    * @param uri the URI. 
    */
   public void setUri(String uri) 
   {
      this.uri = new Uri(uri);
   }

   /**
    * Get the author email. 
    * 
    * @return The email. 
    */
   public String getEmail() 
   {
      if( email == null )
      {
          return null;
      }
      return email.getContent();
   }

   /**
    * Set the author email. 
    * 
    * @param email The email. 
    */
   public void setEmail(String email) 
   {
      this.email = new Email(email);
   } 
   
   /**
    * Return the string. 
    * @return String.
    */
   @Override
   public String toString()
   {
      return "name: " + getName() +
      " email: " + getEmail() + " uri: " + getUri();
   }

}
