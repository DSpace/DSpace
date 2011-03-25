/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

/**
 * Definition of the error codes that will be used in 
 * SWORD error documents.
 * 
 * @see SWORDErrorDocument
 * 
 * @author Stuart Lewis
 */
public interface ErrorCodes
{
	/**
     * ErrorContent - where the supplied format is not the same as that 
     * identified in the X-Format-Namespace and/or that supported by the
     * server
     */
	public static final String ERROR_CONTENT = "http://purl.org/net/sword/error/ErrorContent";
 
	/**
	 * ErrorChecksumMismatch - where the checksum of the file received does 
	 * not match the checksum given in the header
	 */
	public static final String ERROR_CHECKSUM_MISMATCH = "http://purl.org/net/sword/error/ErrorChecksumMismatch";
	
	/**
	 * ErrorBadRequest - where parameters are not understood
	 */
	public static final String ERROR_BAD_REQUEST = "http://purl.org/net/sword/error/ErrorBadRequest";
	
	/**
	 * TargetOwnerUnknown - where the server cannot identify the specified
	 * TargetOwner
	 */
	public static final String TARGET_OWNER_UKNOWN = "http://purl.org/net/sword/error/TargetOwnerUnknown";
	
	/**
	 * MediationNotAllowed - where a client has attempted a mediated deposit,
	 * but this is not supported by the server
  	 */
	public static final String MEDIATION_NOT_ALLOWED = "http://purl.org/net/sword/error/MediationNotAllowed";
	
	/**
	 * MediationNotAllowed - where a client has attempted a mediated deposit,
	 * but this is not supported by the server
  	 */
	public static final String MAX_UPLOAD_SIZE_EXCEEDED = "http://purl.org/net/sword/error/MAX_UPLOAD_SIZE_EXCEEDED";
}
