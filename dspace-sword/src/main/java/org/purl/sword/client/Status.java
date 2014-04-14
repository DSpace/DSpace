/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

/**
 * Representation of the status code and message. 
 * 
 * @author Neil Taylor
 */
public class Status 
{
   /**
    * The status code. 
    */
   private int code; 
   
   /**
    * The status message. 
    */
   private String message; 
   
   /**
    * Create a new status message. 
    * 
    * @param code    The code. 
    * @param message The message. 
    */
   public Status( int code, String message)
   {
      this.code = code;
      this.message = message;
   }
   
   /**
    * Retrieve the code. 
    * 
    * @return The code. 
    */
   public int getCode( )
   {
      return code; 
   }
   
   /**
    * Get the message.
    *  
    * @return The message. 
    */
   public String getMessage()
   {
	   return message; 
   }
   
   /**
    * Get a string representation of the status. 
    */
   public String toString()
   {
      return "Code: " + code + ", Message: '" + message + "'";   
   }
}