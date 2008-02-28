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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

/**
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 *
 */
public class DepositResponse 
{
   private SWORDEntry entry; 
   
   private int httpResponse;

   public DepositResponse( int httpResponse ) 
   {
      entry = new SWORDEntry();  
      this.httpResponse = httpResponse;
   }
      
   /**
    * 
    * @param entry
    */
   public void setEntry( SWORDEntry entry )
   {
      this.entry = entry;
   }
   
   /**
    * 
    * @param entry
    */
   public SWORDEntry getEntry( )
   {
      return entry;
   }
   
   public int getHttpResponse() {
	   return httpResponse;
   }
   
   public void setHttpResponse(int httpResponse) {
	   this.httpResponse = httpResponse;
   }
   
   /**
    * 
    * @return
    */
   public String marshall( )
   {
	   try 
	   {
		   ByteArrayOutputStream stream = new ByteArrayOutputStream();
		   Serializer serializer = new Serializer(stream, "UTF-8");
		   serializer.setIndent(3);
		   serializer.setMaxLength(64);

		   if( entry != null ) 
		   {
		      Document doc = new Document(entry.marshall());
		      serializer.write(doc);  
		      System.out.println(stream.toString());
    		   return stream.toString();
		   }
	   }
	   catch (IOException ex) 
	   {
		   System.err.println(ex); 
	   }

	   return null;   // default return value. 
   }
   
   /**
    * 
    * @param xml
    * @throws UnmarshallException
    */
   public void unmarshall(String xml)
   throws UnmarshallException
   {
	   try
	   {  
		   Builder builder = new Builder(); 
		   Document doc = builder.build(xml, "http://something.com/here");
		   Element root = doc.getRootElement(); 

		   entry = new SWORDEntry( );
		   entry.unmarshall(root);
		   
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
   
   public String toString() 
   {
      return marshall();
   }  

}
