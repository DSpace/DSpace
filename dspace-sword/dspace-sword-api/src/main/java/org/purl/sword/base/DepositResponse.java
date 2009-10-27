/**
 * Copyright (c) 2009, Aberystwyth University
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Properties;
import org.apache.log4j.Logger;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

/**
 * Represents a deposit response. This holds the SWORD Entry element. 
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 *
 */
public class DepositResponse 
{
   /** The entry returned in the response */
   private SWORDEntry entry; 

   /** The HTTP Response code */
   private int httpResponse;
   
   /** The value to set in the Location header (typically the atom edit link) */
   private String location;

   /** Logger */
   private static Logger log = Logger.getLogger(DepositResponse.class);

   /**
    * Create a new response with the specified http code. 
    * 
    * @param httpResponse Response code. 
    */
   public DepositResponse( int httpResponse ) 
   {
      entry = new SWORDEntry();  
      this.httpResponse = httpResponse;
      location = null;
   }

   /**
    * Set the entry element for this response. 
    * 
    * @param entry The SWORD Entry. 
    */
   public void setEntry( SWORDEntry entry )
   {
      this.entry = entry;
   }

   /**
    * Get the SWORD Entry
    *
    * @return The entry
    */
   public SWORDEntry getEntry( )
   {
      return entry;
   }

   /**
    * Get the SWORD Entry as an error document
    *
    * @return The error document
    * @throws SWORDException If this DespositResponse does not contain a
    *                        SWORDErrorDocumentTest. If this is thrown, then
    *                        the document stores an Entry. 
    */
   public SWORDErrorDocument getErrorDocument( )
   throws SWORDException
   {
       if( entry instanceof SWORDErrorDocument)
       {
          return (SWORDErrorDocument)entry;
       }

       throw new SWORDException("Requested document is not an Error Document.");
   }

   /**
    * Retrieve the HTTP Response code. 
    * 
    * @return The response code. 
    */
   public int getHttpResponse() {
      return httpResponse;
   }

   /**
    * Set the HTTP Response code. 
    * 
    * @param httpResponse The code. 
    */
   public void setHttpResponse(int httpResponse) {
      this.httpResponse = httpResponse;
   }

   /**
    * Retrieve the Location header.
    * 
    * @return The Location header
    */
   public String getLocation() {
      return location;
   }

   /**
    * Set the HTTP Location header. 
    * 
    * @param location The Location header. 
    */
   public void setLocation(String location) {
      this.location = location;
   }

   /**
    * Marshall the data in the enclosed SWORD Entry. 
    * 
    * @return The string representation. Null if there was an error. 
    */
   public String marshall( )
   {
      try 
      {
         ByteArrayOutputStream stream = new ByteArrayOutputStream();
         Serializer serializer = new Serializer(stream, "UTF-8");
         serializer.setIndent(3);

         if( entry != null ) 
         {
            Document doc = new Document(entry.marshall());
            serializer.write(doc);  
            log.info(stream.toString());
            return stream.toString();
         }
      }
      catch (IOException ex) 
      {
         log.error(ex.getMessage()); 
      }

      return null;   // default return value. 
   }

   /**
    * Unmarshall the specified XML data into a SWORD Entry. 
    * 
    * @param xml The XML data as a string. 
    * @throws UnmarshallException If there was an error unmarshalling the data. 
    */
   public void unmarshall(String xml) throws UnmarshallException
   {
      unmarshall(xml, null);
   }

   public SwordValidationInfo unmarshall(String xml, Properties validationContext)
   throws UnmarshallException
   {
      try
      {  
         Builder builder = new Builder(); 
         Document doc = builder.build(xml, Namespaces.NS_ATOM);
         Element root = doc.getRootElement(); 

         entry = new SWORDEntry( );
         return entry.unmarshall(root, validationContext);
      }
      catch( ParsingException ex )
      {
         throw new UnmarshallException("Unable to parse the XML", ex );
      }
      catch( IOException ex )
      {
         throw new UnmarshallException("Error acessing the file?", ex);
      }	   
   }

   public void unmarshallErrorDocument(String xml)
   throws UnmarshallException
   {
      unmarshallErrorDocument(xml, null);
   }

   /**
    * Unmarshall the specified XML data into a SWORD error document. 
    * 
    * @param xml The XML data as a string. 
    * @throws UnmarshallException If there was an error unmarshalling the data. 
    */
   public SwordValidationInfo unmarshallErrorDocument(String xml,
                                       Properties validationContext )
   throws UnmarshallException
   {
      try
      {  
         Builder builder = new Builder(); 
         Document doc = builder.build(xml, Namespaces.NS_SWORD);
         Element root = doc.getRootElement(); 

         SWORDErrorDocument sed = new SWORDErrorDocument();
         SwordValidationInfo info = sed.unmarshall(root, validationContext);
         entry = sed;
         return info;
      }
      catch( ParsingException ex )
      {
         throw new UnmarshallException("Unable to parse the XML", ex );
      }
      catch( IOException ex )
      {
         throw new UnmarshallException("Error acessing the file?", ex);
      }	   
   }

   /**
    * Retrieve a string representation of this data. This is equivalent to 
    * calling unmarshall(). 
    * 
    * @return The marshalled data. 
    */
   public String toString() 
   {
      return marshall();
   }  

}
