/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import javax.servlet.http.HttpServletResponse;

/**
 * Definition of the additional HTTP Header tags that will be used in 
 * the SWORD protocol. 
 * 
 * @author Neil Taylor
 * @author Stuart Lewis
 *
 */
public interface HttpHeaders
{
	/**
	 * The HTTP Header label that specifies the MD5 label. 
	 */
	public static final String CONTENT_MD5 = "Content-MD5";
	  
	/**
	 * The HTTP Header label that specifies the MD5 label. 
	 */
	public static final String CONTENT_LENGTH = "Content-Length";
	  
	/**
	 * The HTTP Header label that specifies the On Behalf Of information.  
	 */
	public static final String X_ON_BEHALF_OF = "X-On-Behalf-Of";
  
    /**
     * The HTTP Header label that specifies the Packaging information.
     */
    public static final String X_PACKAGING = "X-Packaging";
  
    /**
     * The HTTP Header label that specifies the desired Verbose status. 
     */
    public static final String X_VERBOSE = "X-Verbose";
  
    /**
     * The HTTP Header label that specifies the desired NoOp status.
     */
    public static final String X_NO_OP = "X-No-Op";

    /**
     * An HTTP Header label that the server should not epect, and thus
     * created a corrupt header.
     */
    public static final String X_CORRUPT = "X-wibble";

    /**
     * The HTTP Header that specifies the error code information. 
     */
    public static final String X_ERROR_CODE = "X-Error-Code";
    
    /**
     * The user agent.
     */
    public static final String USER_AGENT = "User-Agent";
  
    /**
     * The Slug header.
     */
    public static final String SLUG = "Slug";
    
    /**
     * Submission created
     */
    public static final int CREATED = HttpServletResponse.SC_CREATED;
    
    /**
     * Submission accepted.
     */
    public static final int ACCEPTED = HttpServletResponse.SC_ACCEPTED; 
    
    /**
     * The HTTP Header that specifies the content disposition item. This is
     * used by the SWORD profile to identify the name for the deposit. 
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
}
