/**
 * Copyright (c) 2009, Aberystwyth University
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

/**
 * Represents a generic SWORD exception. If this thrown by a repository,
 * it would result in a SWORD Error Document being thrown.
 * 
 * @author Stuart Lewis
 */
public class SWORDErrorException extends Exception 
{
	/** The error URI (defined in ErrorCodes class) */
	private String errorURI;
	
	/** The HTTP error code (defined in HTTPServletResponse class) */
	private int status; 
	
	/** The error message given by the repository */
	private String description;
	
	
	/**
     * Create a new instance and store the specified data. 
     * 
     * @param errorURI The errorURI of the exception being thrown 
     * @param description  A description of the error thrown.
     */
    public SWORDErrorException(String errorURI, String description)
    {  
       super(description); 
       this.errorURI = errorURI;
       this.description = description;
       
       if (errorURI.equals(ErrorCodes.ERROR_BAD_REQUEST)) { status = 400; }
       else if (errorURI.equals(ErrorCodes.ERROR_CHECKSUM_MISMATCH)) { status = 412; }
       else if (errorURI.equals(ErrorCodes.ERROR_CONTENT)) { status = 415; }
       else if (errorURI.equals(ErrorCodes.MAX_UPLOAD_SIZE_EXCEEDED)) { status = 413; }
       else if (errorURI.equals(ErrorCodes.MEDIATION_NOT_ALLOWED)) { status = 412; }
       else if (errorURI.equals(ErrorCodes.TARGET_OWNER_UKNOWN)) { status = 401; }
       else { status = 400; }
    }


	/**
	 * @return the errorURI
	 */
	public String getErrorURI() {
		return errorURI;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}
	
	/**
	 * Set the status
	 * 
	 * @param status The HTTP status code
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}    
}