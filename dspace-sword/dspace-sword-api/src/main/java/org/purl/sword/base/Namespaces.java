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
 * List of the namespaces that are used by SWORD. 
 * 
 * Last updated on: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 * 
 * @author Neil Taylor
 * @version $Revision: 3705 $
 *
 */
public interface Namespaces {

	/**
	 * Atom Publishing Protocol (APP) Namespace. 
	 */
	public static final String NS_APP = "http://www.w3.org/2007/app";
	
	/**
    * APP Prefix. 
    */
   public static final String PREFIX_APP = "app";
	
	/**
	 * ATOM Namespace.
	 */
	public static final String NS_ATOM = "http://www.w3.org/2005/Atom";
	
	/**
	 * ATOM Prefix. 
	 */
	public static final String PREFIX_ATOM = "atom";
	
	/**
	 * Sword Namespace. 
	 */
	public static final String NS_SWORD = "http://purl.org/net/sword/";
	
	/**
    * SWORD Prefix. 
    */
   public static final String PREFIX_SWORD = "sword";
   
   /**
    * DC Terms Namespace.
    */
	public static final String NS_DC_TERMS = "http://purl.org/dc/terms/";
	
	/**
    * DC Terms Prefix. 
    */
   public static final String PREFIX_DC_TERMS = "dcterms";
   
}
