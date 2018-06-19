/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.util.List;

import org.dspace.content.Item;


/**
 * Basic class for representing the set of items which are recent submissions
 * to the archive
 * 
 * @author Richard Jones
 *
 */
public class RecentSubmissions
{
	/** The set of items being represented */
	private List<Item> items;
	
	/**
	 * Construct a new RecentSubmissions object to represent the passed
	 * array of items
	 * 
	 * @param items
	 */
	public RecentSubmissions(List<Item> items)
	{
		this.items = items;
	}

	/**
	 * obtain the number of recent submissions available
	 * 
	 * @return	the number of items
	 */
	public int count()
	{
		return items.size();
	}
	
	/**
	 * Obtain the array of items
	 * 
	 * @return	an array of items
	 */
	public List<Item> getRecentSubmissions()
	{
		return items;
	}
	
	/**
	 * Get the item which is in the i'th position.  Therefore i = 1 gets the
	 * most recently submitted item, while i = 3 gets the 3rd most recently
	 * submitted item
	 * 
	 * @param i		the position of the item to retrieve
	 * @return		the Item
	 */
	public Item getRecentSubmission(int i)
	{
		if (i < items.size())
		{
			return items.get(i);
		}
		else
		{
			return null;
		}
	}
}
