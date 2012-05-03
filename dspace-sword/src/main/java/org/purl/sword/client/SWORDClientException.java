/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

/** 
 * Represents an exception thrown by the SWORD Client. 
 * 
 * @author Neil Taylor
 */
public class SWORDClientException extends Exception
{
   /**
    * Create a new exception, without a message. 
    */
   public SWORDClientException()
   {
      super();
   }
   
   /**
    * Create a new exception with the specified message. 
    * 
    * @param message The message. 
    */
   public SWORDClientException( String message)
   {
      super(message);   
   }
   
   /** 
    * Create a new exception with the specified message and set
    * the exception that generated this error. 
    * 
    * @param message The message. 
    * @param cause The original exception. 
    */
   public SWORDClientException( String message, Exception cause)
   {
      super(message, cause);   
   }
}