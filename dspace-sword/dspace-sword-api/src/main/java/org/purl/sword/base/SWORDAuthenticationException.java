/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

/**
 * Represents a SWORD exception to be thrown if bad authentication credentials
 * are passed to a repository.
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class SWORDAuthenticationException extends Exception 
{
   /**
    * Create a new instance and store the specified message and source data. 
    * 
    * @param message The message for the exception. 
    * @param source  The original exception that lead to this exception. This
    *                can be <code>null</code>. 
    */
   public SWORDAuthenticationException(String message, Exception source)
   {  
      super(message, source);  
   }
   
   /**
    * Create a new instance and store the specified message. 
    * 
    * @param message The message for the exception. 
    */
   public SWORDAuthenticationException(String message)
   {
      super(message);
   }
   
}
