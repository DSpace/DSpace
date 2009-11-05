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
import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.log4j.Logger;
import org.purl.sword.atom.Author;
import org.purl.sword.atom.Title;

/**
 * Extension of the SWORD Entry class, specialized for Error Documents. 
 * 
 * @author Stuart Lewis (sdl@aber.ac.uk)
 * @author Neil Taylor (nst@aber.ac.uk)
 */ 
public class SWORDErrorDocument extends SWORDEntry
{
	/**
	* Local name for the element. 
	*/
   @Deprecated
   public static final String ELEMENT_NAME = "error";
   
   /**
    * The logger.
    */
   private static Logger log = Logger.getLogger(SWORDErrorDocument.class);

   private static final XmlName XML_NAME =
           new XmlName(Namespaces.PREFIX_SWORD, "error", Namespaces.NS_SWORD);

   private static final XmlName ATTRIBUTE_HREF_NAME =
           new XmlName(Namespaces.PREFIX_SWORD, "href", Namespaces.NS_SWORD);

   /**
    * The Error URI
    */
   private String errorURI;

   /**
    * Create the error document (intended to be used when unmarshalling an error document
    * as this will set the errorURI)
    */
   public SWORDErrorDocument() {
	   super(XML_NAME.getPrefix(),
             XML_NAME.getLocalName(),
             XML_NAME.getNamespace());
   }

   /**
    * Create the error document
    * 
    * @param errorURI The URI of the error
    */
   public SWORDErrorDocument(String errorURI) {
	   this();
	   this.errorURI = errorURI;
   }

   /**
    * Get the element name.
    * 
    * @return
    */
   public static XmlName elementName()
   {
      return XML_NAME; 
   }

   /**
    * 
    */
   protected void initialise()
   {
       super.initialise();
   }
   
   /**
    * Overrides the marshall method in the parent SWORDEntry. This will 
    * call the parent marshall method and then add the additional 
    * elements that have been added in this subclass.  
    */
   public Element marshall()
   {
	   Element entry = new Element(getQualifiedName(), Namespaces.NS_SWORD);
	   entry.addNamespaceDeclaration(Namespaces.PREFIX_SWORD, Namespaces.NS_SWORD);
	   entry.addNamespaceDeclaration(Namespaces.PREFIX_ATOM, Namespaces.NS_ATOM);
	   Attribute error = new Attribute("href", errorURI);
       entry.addAttribute(error);
	   super.marshallElements(entry);
       return entry;
   }

   /**
    * Overrides the unmarshall method in the parent SWORDEntry. This will 
    * call the parent method to parse the general Atom elements and
    * attributes. This method will then parse the remaining sword
    * extensions that exist in the element. 
    * 
    * @param entry The entry to parse. 
    * 
    * @throws UnmarshallException If the entry is not an atom:entry 
    *              or if there is an exception extracting the data. 
    */
   public void unmarshall(Element entry) throws UnmarshallException
   {
       unmarshall(entry, null);
   }

   /**
    * 
    * @param entry
    * @param validationProperties
    * @return
    * @throws org.purl.sword.base.UnmarshallException
    */
   public SwordValidationInfo unmarshall(Element entry, Properties validationProperties)
   throws UnmarshallException
   {
      SwordValidationInfo result = super.unmarshall(entry, validationProperties);
      result.clearValidationItems();

      errorURI = entry.getAttributeValue(ATTRIBUTE_HREF_NAME.getLocalName());
      
      if( validationProperties != null )
      {
         result = validate(result, validationProperties);
      }
      
      return result;
   }

   /**
    * This method overrides the XmlElement definition so that it can allow
    * the definition of the href attribute. All other attributes are
    * shown as 'Unknown Attribute' info elements.
    *
    * @param element The element that contains the attributes
    * @param info    The info object that will hold the validation info. 
    */
   @Override
   protected void processUnexpectedAttributes(Element element, SwordValidationInfo info)
   {
       int attributeCount = element.getAttributeCount();
       Attribute attribute = null;

       for( int i = 0; i < attributeCount; i++ )
       {
            attribute = element.getAttribute(i);
            if( ! ATTRIBUTE_HREF_NAME.getLocalName().equals(attribute.getQualifiedName()) )
            {

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
   }

   /**
    *
    * @param elementName
    * @return
    */
   protected boolean isElementChecked(XmlName elementName)
   {
       return super.isElementChecked(elementName);
   }

   /**
    *
    * @return
    */
   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, validationContext);
   }

   /**
    * 
    * @param elements
    * @param attributes
    * @return
    */
   protected SwordValidationInfo validate(SwordValidationInfo info, Properties validationContext)
   {
      
      if( errorURI == null )
      {
         info.addValidationInfo(new SwordValidationInfo(xmlName, ATTRIBUTE_HREF_NAME,
                 SwordValidationInfo.MISSING_ATTRIBUTE_WARNING,
                 SwordValidationInfoType.WARNING));
      }
      else
      {
         boolean validUri = true;
         if(errorURI.startsWith("http://purl.org/net/sword/error/"))
         {
             // check that the list of codes
             if( ! (errorURI.equals(ErrorCodes.ERROR_CONTENT) ||
                    errorURI.equals(ErrorCodes.ERROR_CHECKSUM_MISMATCH) ||
                    errorURI.equals(ErrorCodes.ERROR_BAD_REQUEST) ||
                    errorURI.equals(ErrorCodes.TARGET_OWNER_UKNOWN) ||
                    errorURI.equals(ErrorCodes.MEDIATION_NOT_ALLOWED)) )
             {
                 info.addValidationInfo(new SwordValidationInfo(xmlName,
                         ATTRIBUTE_HREF_NAME,
                         "Errors in the SWORD namespace are reserved and legal values are enumerated in the SWORD 1.3 specification. Implementations MAY define their own errors, but MUST use a different namespace to do so.",
                         SwordValidationInfoType.ERROR));
                 validUri = false; 
             }
         }

         if( validUri )
         {
             SwordValidationInfo item = new SwordValidationInfo(xmlName, ATTRIBUTE_HREF_NAME);
             item.setContentDescription(errorURI);
             info.addAttributeValidationInfo(item);
         }
      }
      return info; 
   }

   
   /**
    * Get the error URI
    * 
    * @return the error URI
    */
   public String getErrorURI()
   {
       return errorURI;
   }

   /**
    * set the error URI
    * 
    * @param error the error URI
    */
   public void setErrorURI(String error)
   {
       errorURI = error;
   }

   /**
    * Main method to perform a brief test of the class
    * 
    * @param args
    */
   /*public static void main(String[] args)
   {
	   SWORDErrorDocumentTest sed = new SWORDErrorDocumentTest(ErrorCodes.MEDIATION_NOT_ALLOWED);
	   sed.setNoOp(true);
	   sed.setTreatment("Short back and shine");
	   sed.setId("123456789");
	   Title t = new Title();
	   t.setContent("My first book");
	   sed.setTitle(t);
	   Author a = new Author();
	   a.setName("Lewis, Stuart");
	   a.setEmail("stuart@example.com");
	   sed.addAuthors(a);
	   
	   System.out.println(sed.marshall().toXML());
   }
    */
}