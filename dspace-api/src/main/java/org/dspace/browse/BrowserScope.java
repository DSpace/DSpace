/*
 * BrowserScope.java
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

import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * A class which represents the initial request to the browse system.
 * When passed into the BrowseEngine, this will cause the creation
 * of a BrowseInfo object
 * 
 * @author Richard Jones
 *
 */
public class BrowserScope
{
	/** the DSpace context */
	private Context context;
	
	/** the current browse index */
	private BrowseIndex browseIndex;
	
	/** the order in which to display results */
	private String order = "ASC";
	
	/** the field upon which to sort */
	private int sortBy;
	
	/** the value to restrict the browse to */
	private String value;

    /** the language of the value to restrict the browse to */
    private String valueLang;
    
	/** the item id focus of the browse */
	private int focus;
	
	/** the string value to start with */
	private String startsWith;
	
	/** the number of results per page to display */
	private int resultsPerPage = 20;
	
	/** the Collection to which to restrict */
	private Collection collection;
	
	/** the Community to which to restrict */
	private Community community;
	
	/** the sort option being used */
	private SortOption sortOption;
	
	/** the value upon which to focus */
	private String vFocus;

    /** the language of the value upon which to focus */
    private String vFocusLang;
    
	/** the browse level */
	private int level = 0;
	
	/**
	 * Construct a new BrowserScope using the given Context 
	 * 
	 * @param context	the DSpace Context
	 */
	public BrowserScope(Context context)
	{
		this.context = context;
	}

	/**
     * Set the DSpaceObject that is the container for this browse.  If this
     * is not of type Collection or Community, this method will throw an
     * exception
     * 
     * @param dso		the container object; a Community or Collection
     * @throws BrowseException
     */
	public void setBrowseContainer(DSpaceObject dso)
		throws BrowseException
	{
		if (dso instanceof Collection)
		{
			this.collection = (Collection) dso;
		}
		else if (dso instanceof Community)
		{
			this.community = (Community) dso;
		}
		else
		{
			throw new BrowseException("The container must be a community or a collection");
		}
	}
	
	/**
     * Obtain a DSpaceObject that represents the container object.  This will be
     * a Community or a Collection
     * 
     * @return	A DSpaceObject representing a Community or a Collection
     */
	public DSpaceObject getBrowseContainer()
	{
		if (this.collection != null)
		{
			return this.collection;
		}
		if (this.community != null)
		{
			return this.community;
		}
		return null;
	}
	
	/**
	 * @param level		the browse level
	 */
	public void setBrowseLevel(int level)
	{
		this.level = level;
	}
	
	/**
	 * @return	the browse level
	 */
	public int getBrowseLevel()
	{
		return this.level;
	}
	
	/**
	 * @return true if top level browse, false if not
	 */
	public boolean isTopLevel()
	{
		if (this.level == 0)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return	true if second level browse, false if not
	 */
	public boolean isSecondLevel()
	{
		if (this.level == 1)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return Returns the browseIndex.
	 */
	public BrowseIndex getBrowseIndex()
	{
		return browseIndex;
	}

	/**
	 * @param browseIndex The browseIndex to set.
	 */
	public void setBrowseIndex(BrowseIndex browseIndex)
	{
		this.browseIndex = browseIndex;
	}

	/**
	 * @return Returns the collection.
	 */
	public Collection getCollection()
	{
		return collection;
	}

	/**
	 * @param collection The collection to set.
	 */
	public void setCollection(Collection collection)
	{
		this.collection = collection;
	}

	/**
	 * @return Returns the community.
	 */
	public Community getCommunity()
	{
		return community;
	}

	/**
	 * @param community The community to set.
	 */
	public void setCommunity(Community community)
	{
		this.community = community;
	}

	/**
	 * @return Returns the context.
	 */
	public Context getContext()
	{
		return context;
	}

	/**
	 * @param context The context to set.
	 */
	public void setContext(Context context)
	{
		this.context = context;
	}

	/**
	 * @return Returns the focus.
	 */
	public int getFocus()
	{
		return focus;
	}

	/**
	 * @param focus The focus to set.
	 */
	public void setFocus(int focus)
	{
		this.focus = focus;
	}

	/**
	 * @return	the value to focus on
	 */
	public String getValueFocus()
	{
		return vFocus;
	}
	
	/**
	 * @param value		the value to focus on
	 */
	public void setValueFocus(String value)
	{
		this.vFocus = value;
	}

    /**
     * @return  the language of the value to focus on
     */
    public String getValueFocusLang()
    {
        return vFocusLang;
    }
    
    /**
     * @param valueLang     the language of the value to focus on
     */
    public void setValueFocusLang(String valueLang)
    {
        this.vFocusLang = valueLang;
    }
	
	/**
	 * @return Returns the order.
	 */
	public String getOrder()
	{
		return order;
	}

	/**
	 * @param order The order to set.
	 */
	public void setOrder(String order)
	{
		if (order != null && !"".equals(order))
			this.order = order;
	}

	/**
	 * @return Returns the resultsPerPage.
	 */
	public int getResultsPerPage()
	{
		return resultsPerPage;
	}

	/**
	 * @param resultsPerPage The resultsPerPage to set.
	 */
	public void setResultsPerPage(int resultsPerPage)
	{
		if (resultsPerPage > -1)
			this.resultsPerPage = resultsPerPage;
	}

	/**
	 * @return Returns the sortBy.
	 */
	public int getSortBy()
	{
		return sortBy;
	}

	/**
	 * @param sortBy The sortBy to set.
	 */
	public void setSortBy(int sortBy)
	{
		this.sortBy = sortBy;
	}

	/**
	 * Obtain the sort option
	 * 
	 * @return	the sort option
	 * @throws BrowseException
	 */
	public SortOption getSortOption()
		throws BrowseException
	{
		if (sortOption == null)
		{
			if (browseIndex != null)
			{
				if (sortBy <= 0)
				{
					String dataType = browseIndex.getDataType();
					String type = ("date".equals(dataType) ? "date" : "text");
					sortOption = new SortOption(0, browseIndex.getName(), browseIndex.getMetadata(), type);
				}
				else
				{
					Map map = browseIndex.getSortOptions();
					SortOption so = (SortOption) map.get(new Integer(sortBy));
					sortOption = so;
				}
			}
		}
		return sortOption;
	}
	
	/**
	 * @return Returns the startsWith.
	 */
	public String getStartsWith()
	{
		return startsWith;
	}

	/**
	 * @param startsWith The startsWith to set.
	 */
	public void setStartsWith(String startsWith)
	{
		this.startsWith = startsWith;
	}

	/**
	 * @return Returns the value.
	 */
	public String getValue()
	{
		return value;
	}

    /**
     * @param value The value to set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return Returns the language.
     */
    public String getValueLang()
    {
        return valueLang;
    }

    /**
     * @param valueLang The language to set.
     */
    public void setValueLang(String valueLang)
    {
        this.valueLang = valueLang;
    }
    
	/**
	 * @return	true if in community, false if not
	 */
	public boolean inCommunity()
	{
		if (this.community != null)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return	true if in collection, false if not
	 */
	public boolean inCollection()
	{
		if (this.collection != null)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return	true if ascending, false if not
	 */
	public boolean isAscending()
	{
		if ("ASC".equals(order))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return	true if has value, false if not
	 */
	public boolean hasValue()
	{
		if (value == null || "".equals(value))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return	true if has item focus, false if not
	 */
	public boolean hasFocus()
	{
		if (focus == -1)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @return	true if has value focus, false if not
	 */
	public boolean hasValueFocus()
	{
		if (this.vFocus != null)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return	true if has starts with value, false if not
	 */
	public boolean hasStartsWith()
	{
		if (this.startsWith != null)
		{
			return true;
		}
		return false;
	}
}
