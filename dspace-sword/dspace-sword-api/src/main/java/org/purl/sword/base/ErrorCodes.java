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

/**
 * Definition of the error codes that will be used in 
 * the SWORD protocol (in X-Error-Code).
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
	public static final String ERROR_CONTENT = "ErrorContent";
 
	/**
	 * ErrorChecksumMismatch - where the checksum of the file recevied does 
	 * not match the checksum given in the header
	 */
	public static final String ERROR_CHECKSUM_MISMATCH = "ErrorChecksumMismatch";
	
	/**
	 * ErrorBadRequest - where parameters are not understood
	 */
	public static final String ERROR_BAD_REQUEST = "ErrorBadRequest";
	
	/**
	 * TargetOwnerUnknown - where the server cannot identify the specified
	 * TargetOwner
	 */
	public static final String TARGET_OWNER_UKNOWN = "TargetOwnerUnknown";
	
	/**
	 * MediationNotAllowed - where a client has attempted a mediated deposit,
	 * but this is not supported by the server
  	 */
	public static final String MEDIATION_NOT_ALLOWED = "MediationNotAllowed";
}
