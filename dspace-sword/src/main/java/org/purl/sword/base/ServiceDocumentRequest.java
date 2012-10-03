/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
   
   /** The IP address */
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
    * @param username the authenticated UserName to set
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
    * @param IPAddress the IP address
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
    * @param location the location
    */
   public void setLocation(String location) {
	   this.location = location;
   }

}
