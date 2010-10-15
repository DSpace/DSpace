/**
 * Copyright (c) 2008, Aberystwyth University
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
package org.purl.sword.client;

/**
 * Details for a destination.  This is used to represent a destination. If 
 * expressed as a string, the destination looks like: 
 * <pre>
 * <user>[<onBehalfOf>]:<password>@<url>
 * </pre>
 * 
 * @author Neil Taylor
 */
public class PostDestination
{
   /**
    * URL for the post destination. 
    */
   private String url; 
   
   /** 
    * The username. 
    */
   private String username;
   
   /**
    * The password. 
    */
   private String password;
   
   /**
    * The onBehalfOf ID. 
    */
   private String onBehalfOf;
   
   /**
    * Create a new instance. 
    */
   public PostDestination()
   {
      // No-Op
   }
   
   /**
    * Create a new instance. 
    * 
    * @param url          The url. 
    * @param username     The username. 
    * @param password     The password. 
    * @param onBehalfOf   The onBehalfOf id. 
    */
   public PostDestination(String url, String username, String password, String onBehalfOf)
   {
      this.url = url; 
      this.username = username; 
      this.password = password;
      this.onBehalfOf = onBehalfOf;
   }

   /**
    * @return the url
    */
   public String getUrl()
   {
      return url;
   }

   /**
    * @param url the url to set
    */
   public void setUrl(String url)
   {
      this.url = url;
   }

   /**
    * @return the username
    */
   public String getUsername()
   {
      return username;
   }

   /**
    * @param username the username to set
    */
   public void setUsername(String username)
   {
      this.username = username;
   }

   /**
    * @return the password
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(String password)
   {
      this.password = password;
   }

   /**
    * @return the onBehalfOf
    */
   public String getOnBehalfOf()
   {
      return onBehalfOf;
   }

   /**
    * @param onBehalfOf the onBehalfOf to set
    */
   public void setOnBehalfOf(String onBehalfOf)
   {
      this.onBehalfOf = onBehalfOf;
   }
   
   /** 
    * Create a string representation of this object. 
    * 
    * @return The string. 
    */
   public String toString()
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append(username);
      if( onBehalfOf != null )
      {
         buffer.append("[");
         buffer.append(onBehalfOf);
         buffer.append("]");
      }
      
      if( password != null )
      {
         buffer.append(":******");
      }
      buffer.append("@");
      buffer.append(url);
      
      return buffer.toString(); 
   }
}
