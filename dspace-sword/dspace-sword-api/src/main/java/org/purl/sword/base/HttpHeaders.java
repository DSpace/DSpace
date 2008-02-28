/**
 * Copyright (c) 2007, Aberystwyth University
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

import javax.servlet.http.HttpServletResponse;

/**
 * Definition of the additional HTTP Header tags that will be used in 
 * the SWORD protocol. 
 * 
 * @author Neil Taylor
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
     * The HTTP Header label that specifies the Format Namespace information.
     */
    public static final String X_FORMAT_NAMESPACE = "X-Format-Namespace";
  
    /**
     * The HTTP Header label that specifies the desired Verbose status. 
     */
    public static final String X_VERBOSE = "X-Verbose";
  
    /**
     * The HTTP Header label that specifies the desired NoOp status.  
     */
    public static final String X_NO_OP = "X-No-Op";
  
    /**
     * The HTTP Header that specifies the error code information. 
     */
    public static final String X_ERROR_CODE = "X-Error-Code";
  
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
