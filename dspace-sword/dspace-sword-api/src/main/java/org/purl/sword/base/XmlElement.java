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
package org.purl.sword.base;

/**
 *   Author   : $Author: nst $
 *   Date     : $Date: 2007/09/21 15:18:55 $
 *   Revision : $Revision: 1.3 $
 *   Name     : $Name:  $
 */

import nu.xom.Element;
import nu.xom.Node;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Parent class for all classes that represent an XML element. This provides
 * some common utility methods that are useful for marshalling and 
 * unmarshalling data. 
 * 
 * @author Neil Taylor
 */
public class XmlElement 
{
   /**
    * The name to use for the prefix. 
    */
   protected String prefix; 
   
   /**
    * 
    */
   protected String localName; 
   
   /**
    * 
    * @param localName
    */
   public XmlElement(String localName)
   {
      this.localName = localName;
   }
   
   /**
    * Create a new instance. 
    */
   public XmlElement(String prefix, String localName)
   {
      this.prefix = prefix;
      this.localName = localName;
   }
   
   
   /**
    * The Date format that is used to parse dates to and from the ISO format 
    * in the XML data. 
    */
   protected static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   
   /**
    * Extract a boolean value from the specified element. The boolean value 
    * is represented as the string 'true' or 'false' as the only child
    * of the specified element. 
    * 
    * @param element The element that contains the boolean value. 
    * @return True or false, based on the string in the element's content. 
    * @throws UnmarshallException If the element does not contain a single child, or if
    * the child does not contain the value 'true' or 'false'. 
    */
   protected boolean unmarshallBoolean( Element element )
   throws UnmarshallException 
   {
	   if( element.getChildCount() != 1 )
      {
         throw new UnmarshallException("Missing Boolean Value", null);
      }
      
      // ok to get the single child element. This should be a text element.
      try
      {
         Node child = element.getChild(0);
         String value = child.getValue();
         if( "true".equals(value) )
         {
        	   return true;
         }
         else if( "false".equals(value))
         {
        	   return false;
         }
         else
         {
        	   throw new UnmarshallException("Illegal Value");
         }
      }
      catch( IndexOutOfBoundsException ex )
      {
         throw new UnmarshallException("Error accessing Boolean element", ex);
      }
      
   }

   /**
    * Extract a string value from the specified element. The value 
    * is the only child of the specified element. 
    * 
    * @param element The element that contains the string value. 
    * @return The string. 
    * @throws UnmarshallException If the element does not contain a single child. 
    */
   protected String unmarshallString( Element element )
   throws UnmarshallException
   {
      if( element.getChildCount() != 1 )
	   {
	      throw new UnmarshallException("Missing String Value", null);
	   }
	      
	   // ok to get the single child element. This should be a text element.
	   try
	   {
	      Node child = element.getChild(0);
	      return child.getValue();
	   }
	   catch( IndexOutOfBoundsException ex )
	   {
	      throw new UnmarshallException("Error accessing Boolean element", ex);
	   } 
	   
   }
   
   /**
    * Extract an integer value from the specified element. The integer value 
    * is represented as a string in the only child
    * of the specified element. 
    * 
    * @param element The element that contains the integer. 
    * @return The integer. 
    * @throws UnmarshallException If the element does not contain a single child, or if
    * the child does not contain the valid integer. 
    */
   protected int unmarshallInteger( Element element )
   throws UnmarshallException
   {
	   if( element.getChildCount() != 1 )
	   {
	      throw new UnmarshallException("Missing Integer Value", null);
	   }
	      
	   // ok to get the single child element. This should be a text element.
	   try
	   {
	      Node child = element.getChild(0);
	      return Integer.parseInt( child.getValue() );
	   }
	   catch( IndexOutOfBoundsException ex )
	   {
	      throw new UnmarshallException("Error accessing Boolean element", ex);
	   } 
	   catch( NumberFormatException nfex )
	   {
	      throw new UnmarshallException("Error fomratting the number", nfex);	   
	   }
   }
   
   /**
    * Extract an date value from the specified element. The date value 
    * is represented as a string in the only child of the element. 
    * 
    * @param element The element that contains the date. 
    * @return The date. 
    * @throws UnmarshallException If the element does not contain a single child, or if
    * the child does not contain the valid date. 
    */
   protected Date unmarshallDate(Element element)
   throws UnmarshallException 
   {
	   try
	   {
	      String content = unmarshallString(element);
	      return stringToDate(content);
	   }
	   catch( UnmarshallException ue )
	   {
	      throw new UnmarshallException("Error accessing the date.", ue);
	   } 
	   catch (ParseException pe)
      {
	      throw new UnmarshallException("Error accessing the date.", pe);
      }
   }
   
   /**
    * Convert the date to a string. If the date is null,
    * the result will result to a default date of 1st January 1970. 
    * FIXME - is this sensible?  
    * 
    * @param date The Date object. 
    * @return The Date, expressed as a string in the format 
    * yyyy-MM-ddTHH:mm:ssZ.
    */
   protected String dateToString(Date date)
   {
	  if( date == null )
	  {
		  GregorianCalendar cal = new GregorianCalendar(1970, 0, 1, 0, 0, 0);
		  date = cal.getTime();
	  }
      SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
      return formatter.format(date);
   }
   
   /**
    * Convert the string into a Date object. 
    * 
    * @param date The date, represented as a string. 
    * @return A Date. 
    * @throws ParseException If the string does not match the format 
    * of yyyy-MM-ddTHH:mm:ssZ.
    */
   protected Date stringToDate(String date)
   throws ParseException 
   {
      SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
      return formatter.parse(date);
   }
   
   /**
    * Determines if the specified element is an instance of the element name. If 
    * you are checking the name title in the ATOM namespace, then the local name
    * should be 'title' and the namespaceURI is the URI for the ATOM namespace. 
    * 
    * @param element      The specified element. 
    * @param localName    The local name for the element. 
    * @param namespaceURI The namespace for the element. 
    * @return True if the element matches the localname and namespace. Otherwise, false. 
    */
   protected boolean isInstanceOf(Element element, String localName, String namespaceURI )
   {
      return (localName.equals(element.getLocalName()) && 
              namespaceURI.equals(element.getNamespaceURI()) );
   }
   
   /**
    * Retrieve the qualified name for this object. This uses the
    * prefix and local name stored in this object. 
    * 
    * @return A string of the format 'prefix:localName'
    */
   public String getQualifiedName()
   {
      return getQualifiedName(localName);
   }

   /**
    * Retrieve the qualified name. The prefix for this object is prepended 
    * onto the specified local name. 
    * 
    * @param name the specified local name. 
    * @return A string of the format 'prefix:name'
    */
   public String getQualifiedName(String name)
   {
      String p = prefix; 
      if( p != null )
      {
         p += ":";
      }
      return p + name;
   }
}
