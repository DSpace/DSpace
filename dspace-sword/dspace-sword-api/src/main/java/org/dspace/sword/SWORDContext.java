/* SWORDContext.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * This class represents the users who are involved in the
 * deposit or service document request process.  This is the
 * authenticated primary user and the potentially null
 * onBehalfOf user.
 * 
 * It can then answer various authorisation questions regarding
 * those users.
 * 
 * @author Richard Jones
 *
 */
public class SWORDContext 
{
	/** The primary authenticated user for the request */
	private EPerson authenticated = null;
	
	/** The onBehalfOf user for the request */
	private EPerson onBehalfOf = null;

	/**
	 * @return	the authenticated user
	 */
	public EPerson getAuthenticated() 
	{
		return authenticated;
	}

	/**
	 * @param authenticated	the eperson to set
	 */
	public void setAuthenticated(EPerson authenticated) 
	{
		this.authenticated = authenticated;
	}

	/**
	 * @return	the onBehalfOf user
	 */
	public EPerson getOnBehalfOf() 
	{
		return onBehalfOf;
	}

	/**
	 * @param onBehalfOf	the eperson to set
	 */
	public void setOnBehalfOf(EPerson onBehalfOf) 
	{
		this.onBehalfOf = onBehalfOf;
	}
	
	/**
	 * Is the authenticated user a DSpace administrator?  This translates
	 * as asking the question of whether the given eperson is a member
	 * of the special DSpace group Administrator, with id 1
	 * 
	 * @param eperson
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isUserAdmin(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			if (this.authenticated != null)
			{
				Group admin = Group.find(context, 1);
				return admin.isMember(this.authenticated);
			}
			return false;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	/**
	 * Is the given onBehalfOf user DSpace administrator?  This translates
	 * as asking the question of whether the given eperson is a member
	 * of the special DSpace group Administrator, with id 1
	 * 
	 * @param eperson
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isOnBehalfOfAdmin(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			if (this.onBehalfOf != null)
			{
				Group admin = Group.find(context, 1);
				return admin.isMember(this.onBehalfOf);
			}
			return false;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	/**
	 * Is the authenticated user a member of the given group
	 * or one of its sub groups?
	 * 
	 * @param group
	 * @return
	 */
	public boolean isUserInGroup(Group group)
	{
		if (this.authenticated != null)
		{
			return isInGroup(group, this.authenticated);
		}
		return false;
	}
	
	/**
	 * Is the onBehalfOf user a member of the given group or
	 * one of its sub groups
	 * 
	 * @param group
	 * @return
	 */
	public boolean isOnBehalfOfInGroup(Group group)
	{
		if (this.onBehalfOf != null)
		{
			return isInGroup(group, this.onBehalfOf);
		}
		return false;
	}
	
	/**
	 * Is the given eperson in the given group, or any of the groups
	 * that are also members of that group.  This method recurses
	 * until it has exhausted the tree of groups or finds the given
	 * eperson
	 * 
	 * @param group
	 * @param eperson
	 * @return	true if in group, false if not
	 */
	public boolean isInGroup(Group group, EPerson eperson)
	{
		EPerson[] eps = group.getMembers();
		Group[] groups = group.getMemberGroups();
		
		// is the user in the current group
		for (int i = 0; i < eps.length; i++)
		{
			if (eperson.getID() == eps[i].getID())
			{
				return true;
			}
		}
		
		// is the eperson in the sub-groups (recurse)
		if (groups != null && groups.length > 0)
		{
			for (int j = 0; j < groups.length; j++)
			{
				if (isInGroup(groups[j], eperson))
				{
					return true;
				}
			}
		}
		
		// ok, we didn't find you
		return false;
	}
	
	/**
	 * Get an array of all the collections that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 * 
	 * @param context
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public Collection[] getAllowedCollections(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			// locate the collections to which the authenticated user has ADD rights
			Collection[] cols = Collection.findAuthorized(context, null, Constants.ADD);

			// if there is no onBehalfOf user, just return the list
			if (this.getOnBehalfOf() == null)
			{
				return cols;
			}
			
			// if the onBehalfOf user is an administrator, return the list
			if (this.isOnBehalfOfAdmin(context))
			{
				return cols;
			}
			
			// if we are here, then we have to filter the list of collections
			List<Collection> colList = new ArrayList<Collection>();
			
			for (int i = 0; i < cols.length; i++)
			{
				// we check each collection to see if the onBehalfOf user
				// is permitted to deposit
				
				// urgh, this is so inefficient, but the authorisation API is
				// a total hellish nightmare
				Group subs = cols[i].getSubmitters();
				if (isOnBehalfOfInGroup(subs))
				{
					colList.add(cols[i]);
				}
			}
			
			// now create the new array and return that
			Collection[] newCols = new Collection[colList.size()];
			newCols = colList.toArray((Collection[]) newCols);
			return newCols;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	/**
	 * Can the current SWORD Context permit deposit into the given 
	 * collection in the given DSpace Context
	 * 
	 * @param context
	 * @param collection
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public boolean canSubmitTo(Context context, Collection collection)
		throws DSpaceSWORDException
	{
		Group subs = collection.getSubmitters();
		if (isUserAdmin(context))
		{
			if (this.onBehalfOf != null)
			{
				if (isOnBehalfOfAdmin(context))
				{
					return true;
				}
				return isOnBehalfOfInGroup(subs);
			}
			return true;
		}
		else
		{
			if (isUserInGroup(subs))
			{
				if (this.onBehalfOf != null)
				{
					if (isOnBehalfOfAdmin(context))
					{
						return true;
					}
					return isOnBehalfOfInGroup(subs);
				}
				return true;
			}
			return false;
		}
		
	}
}
