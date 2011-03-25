/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

/**
 * Represents a deposit response. This holds the SWORD Entry element. 
 * 
 * @author Stuart Lewis
 */
public class AtomDocumentResponse extends DepositResponse
{

  /**
    * Create a new response with the specified http code. 
    * 
    * @param httpResponse Response code. 
    */
   public AtomDocumentResponse(int httpResponse) 
   {
      super(httpResponse);
   }

}
