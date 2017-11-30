/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

/**
 * An invalid media type has been detected during parsing. 
 * 
 * @author Neil Taylor
 */
public class InvalidMediaTypeException extends Exception
{
   /**
    * Create a new instance and store the message. 
    * 
    * @param message The exception's message. 
    */
   public InvalidMediaTypeException( String message )
   {
      super(message);
   }
}
