/* DepositResult.java
 * 
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

package org.dspace.sword;

import org.dspace.content.Item;

/**
 * The DSpace class for representing the results of a deposit
 * request.  This class can be used to hold all of the relevant
 * components required to later build the SWORD response
 * 
 * @author Richard Jones
 *
 */
public class DepositResult
{
	/** the handle assigned to the item, if available */
	private String handle;
	
	/** the item created during deposit */
	private Item item;

	/** the verbose description string to be returned to the requester */
	private String verboseDescription;
	
	/**
	 * @return the item
	 */
	public Item getItem()
	{
		return item;
	}
	
	/**
	 * @param item the item to set
	 */
	public void setItem(Item item)
	{
		this.item = item;
	}

	/**
	 * @return	the handle
	 */
	public String getHandle() 
	{
		return handle;
	}

	/**
	 * @param handle	the item handle
	 */
	public void setHandle(String handle) 
	{
		this.handle = handle;
	}

	/**
	 * @return the verboseDescription
	 */
	public String getVerboseDescription()
	{
		return verboseDescription;
	}

	/**
	 * @param verboseDescription the verboseDescription to set
	 */
	public void setVerboseDescription(String verboseDescription)
	{
		this.verboseDescription = verboseDescription;
	}
}
