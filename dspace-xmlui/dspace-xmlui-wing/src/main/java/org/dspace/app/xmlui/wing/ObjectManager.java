/*
 * ObjectManager.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/03/20 22:39:04 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.wing;

import java.util.HashMap;

/**
 * The object manager is a class that must be implemented by each specific repository 
 * implementation that identifies refrenced objects. Since the DRI document includes 
 * refrences to external resources implementers of this class must know how objects 
 * are refrenced.
 * 
 * The specefic implementation of ObjectManager that is used is determened by the
 * WingComponent that is creating the refrence.
 * 
 * @author Scott Phillips
 */

public interface ObjectManager
{	
    /**
     * Determine if the supplied object is manageable by this implementation of 
     * ObjectManager. If the object is manageable then manage it, and return true.
     * 
     * @param object
     *            The object to be managed.
     * @return true if the object can be managed, otherwise false.
     */
    public boolean manageObject(Object object) throws WingException;  
	
	/**
	 * Return a url refrencing the object's metadata. If this is unabvailable 
	 * return null.
	 * 
	 * @param object The object being managed.
	 */
	public String getObjectURL(Object object) throws WingException;
	
	/**
	 * Return a descriptive, repository specfic, type for the object. If 
	 * this is unabvailable return null.
	 * 
	 * @param object The object being managed.
	 */
	public String getObjectType(Object object) throws WingException;
	
	/**
	 * Return a unique identifier of the repository this object is contained 
	 * in. If this is unabvailable return null.
	 * 
	 * @param object The object being managed.
	 */
	public String getRepositoryIdentifier(Object object) throws WingException;
	

	/**
	 * Return a list of all repositories managed by this manager. The 
	 * hash should be of the form repository identifier as the key, 
	 * and the value for each key is a metadata URL.
	 */
	public HashMap<String,String> getAllManagedRepositories() throws WingException;
}
