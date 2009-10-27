/*
 * ItemCountDAO.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;

/**
 * Interface for data access of cached community and collection item count
 * information
 * 
 * @author Richard Jones
 *
 */
public interface ItemCountDAO
{
	/**
	 * Set the DSpace Context to use during data access
	 * 
	 * @param context
	 * @throws ItemCountException
	 */
	public void setContext(Context context) throws ItemCountException;
	
	/**
	 * Set the given count as the number of items in the given community
	 * 
	 * @param community
	 * @param count
	 * @throws ItemCountException
	 */
	public void communityCount(Community community, int count) throws ItemCountException;
	
	/**
	 * Set the given count as the number of items in the given collection
	 * 
	 * @param collection
	 * @param count
	 * @throws ItemCountException
	 */
	public void collectionCount(Collection collection, int count) throws ItemCountException;
	
	/**
	 * Get the number of items in the given DSpaceObject container.  This method will
	 * only succeed if the DSpaceObject is an instance of either a Community or a
	 * Collection.  Otherwise it will throw an exception
	 * 
	 * @param dso
	 * @return
	 * @throws ItemCountException
	 */
	public int getCount(DSpaceObject dso) throws ItemCountException;
	
	/**
	 * Remove any cached data regarding the given DSpaceObject container.  This method will
	 * only succeed if the DSpaceObject is an instance of either a Community or a
	 * Collection.  Otherwise it will throw an exception
	 * 
	 * @param dso
	 * @throws ItemCountException
	 */
	public void remove(DSpaceObject dso) throws ItemCountException;
}
