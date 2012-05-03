/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

/**
 * Interface for a client. This contains a single method that allows the factory 
 * to pass a set of command line options to the client. 
 *
 * @author Neil Taylor
 */
public interface ClientType
{
   /**
    * Run the client, processing the specified options.
    *  
    * @param options The options extracted from the command line. 
    */
   public void run( ClientOptions options ); 
}
