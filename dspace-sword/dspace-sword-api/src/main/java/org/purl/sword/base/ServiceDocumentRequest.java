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
package org.purl.sword.base;

/**
 * Represents a ServiceDocumentRequest. 
 * 
 * @author Stuart Lewis 
 * 
 */
public class ServiceDocumentRequest 
{
   /** The username */
   private String username;
   
   /** The password */
   private String password;
   
   /** The onBehalf of name */
   private String onBehalfOf;
   
   /** The IP Address */
   private String  IPAddress;
   
   /** The location */
   private String location;
   

   /**
    * Retrieve the username. 
    * 
    * @return the authenticatedUserName
    */
   public String getUsername() {
      return username;
   }

   /**
    * Set the username. 
    * 
    * @param authenticatedUserName the authenticatedUserName to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * Get the password. 
    * 
    * @return the authenticatedUserPassword
    */
   public String getPassword() {
      return password;
   }

   /**
    * Set the password. 
    * 
    * @param password the password to set
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /**
    * Get the onBehalfOf name. 
    * 
    * @return the onBehalfOf
    */
   public String getOnBehalfOf() {
      return onBehalfOf;
   }

   /**
    * Set the onBehalfOf name. 
    * 
    * @param onBehalfOf the onBehalfOf to set
    */
   public void setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
   }
   
   /**
    * Get the IP address of the user
    * 
    * @return the the IP address
    */
   public String getIPAddress() {
	   return IPAddress;
   }
   
   /**
    * Set the IP address of the user
    *
    * @param String the IP address
    */
   public void setIPAddress(String IPAddress) {
	   this.IPAddress = IPAddress;
   }
   
   /**
    * Get the location of the service document
    * 
    * @return the location of the service document
    */
   public String getLocation() {
	   return location;
   }
   
   /**
    * Set the location of the service document
    *
    * @param String the location
    */
   public void setLocation(String location) {
	   this.location = location;
   }

}
