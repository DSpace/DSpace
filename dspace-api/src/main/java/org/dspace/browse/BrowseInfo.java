/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.sort.SortOption;

/**
 * The results of a Browse, including all the contextual information about
 * the query, as well as the results and associated information to create
 * pageable navigation.
 *
 * @author Richard Jones
 */
public class BrowseInfo
{
    /**
     * The results of the browse.
     * FIXME: Unable to generify due to mixed usage
     */
    private List results;

    /**
     * The position of the first element of results within the Browse index.
     * Positions begin with 0.
     */
    private int overallPosition;

    /**
     * The position of the requested object within the results. Offsets begin
     * with 0.
     */
    private int offset;

    /**
     * The total number of items in the browse index.
     */
    private int total;

    /**
     * True if this browse was cached.
     */
    private boolean cached;

    /** the browse index to which this pertains */
    private BrowseIndex browseIndex;

    /** the sort option being used */
    private SortOption sortOption;

    /** is the browse ascending or descending */
    private boolean ascending;

    /** what level of browse are we in?  full and single front pages are 0, single value browse is 1 */
    private int level = 0;

    /** the value browsed upon */
    private String value;

    /** the authority key browsed upon */
    private String authority;

    /** is this a "starts_with" browse? */
    private boolean startsWith = false;

    /** Collection we are constrained to */
    private Collection collection;

    /** Community we are constrained to */
	private Community community;

	/** offset of the item at the top of the next page */
	private int nextOffset = -1;

	/** offset of the item at the top of the previous page */
	private int prevOffset = -1;

	/** the value upon which we are focusing */
	private String focus;

	/** number of results to display per page */
	private int resultsPerPage = -1;

	/** database id of the item upon which we are focusing */
	private int focusItem = -1;

	/** number of metadata elements to display before truncating using "et al" */
	private int etAl = -1;

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Constructor
     * FIXME: Unable to generify due to mixed usage
     *
     * @param results
     *            A List of Browse results
     * @param overallPosition
     *            The position of the first returned item in the overall index
     * @param total
     *            The total number of items in the index
     * @param offset
     *            The position of the requested item in the set of results
     */
    public BrowseInfo(List results, int overallPosition, int total, int offset)
    {
        if (results == null)
        {
            throw new IllegalArgumentException("Null result list not allowed");
        }

        this.results = Collections.unmodifiableList(results);
        this.overallPosition = overallPosition;
        this.total = total;
        this.offset = offset;
    }

    /**
     * @return	the number of metadata fields at which to truncate with "et al"
     */
    public int getEtAl()
    {
    	return etAl;
    }

    /**
     * set the number of metadata fields at which to truncate with "et al"
     *
     * @param etAl
     */
    public void setEtAl(int etAl)
    {
    	this.etAl = etAl;
    }

    /**
	 * @return Returns the focusItem.
	 */
	public int getFocusItem()
	{
		return focusItem;
	}

	/**
	 * @param focusItem The focusItem to set.
	 */
	public void setFocusItem(int focusItem)
	{
		this.focusItem = focusItem;
	}

	/**
	 * Does this browse have an item focus (as opposed to one of: no focus,
	 * a value focus)
	 *
	 * @return	true if item focus, false if not
	 */
	public boolean hasItemFocus()
	{
		if (focusItem == -1)
		{
			return false;
		}
		return true;
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
		this.resultsPerPage = resultsPerPage;
	}

	/**
	 * Is there a value associated with this browse
	 *
	 * @return	true if a value, false if not
	 */
	public boolean hasValue()
    {
    	if (this.value != null)
    	{
    		return true;
    	}
    	return false;
    }

	/**
	 * Is there an authority key associated with this browse
	 *
	 * @return	true if  an authority key, false if not
	 */
	public boolean hasAuthority()
    {
    	if (this.authority != null)
    	{
    		return true;
    	}
    	return false;
    }

	/**
	 * Are there results for this browse, or was the result set empty?
	 *
	 * @return	true if results, false if not
	 */
    public boolean hasResults()
    {
    	if (results.size() > 0)
    	{
    		return true;
    	}
    	return false;
    }

    /**
     * @param focus		the value to focus the browse around
     */
    public void setFocus(String focus)
    {
    	this.focus = focus;
    }

    /**
     * @return		the value to focus the browse around
     */
    public String getFocus()
    {
    	return this.focus;
    }

    /**
     * Set the DSpaceObject that is the container for this browse.  If this
     * is not of type Collection or Community, this method will throw an
     * exception
     *
     * @param dso		the container object; a Community or Collection
     * @throws BrowseException if browse error
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
     * @param offset	the database id of the item at the top of the next page
     */
    public void setNextOffset(int offset)
    {
    	this.nextOffset = offset;
    }

    /**
     * @return		the database id of the item at the top of the next page
     */
    public int getNextOffset()
    {
    	return this.nextOffset;
    }

   /**
	 * @return Returns the ascending.
	 */
	public boolean isAscending()
	{
		return ascending;
	}

	/**
	 * @param ascending The ascending to set.
	 */
	public void setAscending(boolean ascending)
	{
		this.ascending = ascending;
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
	 * @return Returns the prevItem.
	 */
	public int getPrevOffset()
	{
		return prevOffset > -1 ? prevOffset : 0;
	}

	/**
	 * @param prevOffset The prevOffset to set.
	 */
	public void setPrevOffset(int prevOffset)
	{
		this.prevOffset = prevOffset;
	}

	/**
	 * @return Returns the sortOption.
	 */
	public SortOption getSortOption()
	{
		return sortOption;
	}

	/**
	 * @param sortOption The sortOption to set.
	 */
	public void setSortOption(SortOption sortOption)
	{
		this.sortOption = sortOption;
	}

	/**
	 * @return Returns the startsWith.
	 */
	public boolean isStartsWith()
	{
		return startsWith;
	}

	/**
	 * @param startsWith The startsWith to set.
	 */
	public void setStartsWith(boolean startsWith)
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
	 * @return Returns the authority key.
	 */
	public String getAuthority()
	{
		return authority;
	}

	/**
	 * @param authority The authority key to set.
	 */
	public void setAuthority(String authority)
	{
		this.authority = authority;
	}

	/**
	 * is this a top level (0) browse?  Examples of this are a full item
	 * browse or a single browse.  Other browse types are considered
	 * second level (1)
	 *
	 * @return	true if top level, false if not
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
	 * Is this a second level (1) browse?  Examples of this are a single
	 * value browse (e.g. all items by a given author)
	 *
	 * @return	true if second level, false if not
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
     * The results of the Browse. Each member of the list is either a String array
     * (for the authors browse: first element the value, second element the authority key)
     * or an {@link org.dspace.content.Item}(for the
     * other browses).
     *
     * @return Result list. This list cannot be modified.
     */
    public List<Item> getResults()
    {
        return results;
    }

    /**
     * Return the results of the Browse as an array of String array.
     * The first element (i.e. index 0) is the value, the second is the authority key
     *
     * @return The results of the Browse as a String array.
     */
    public String[][] getStringResults()
    {
        return (String[][]) results.toArray(new String[results.size()][2]);
    }

    /**
     * @deprecated
     * @return an empty array of Item.
     */
    public Item[] getItemResults()
    {
    	return new Item[0];
    }

    /**
     * Return the results of the Browse as a BrowseItem array
     *
     * @return		the results of the browse as a BrowseItem array
     */
    public List<Item> getBrowseItemResults()
    {
        return results;
    }

    /**
     * Return the number of results.
     *
     * @return The number of results.
     */
    public int getResultCount()
    {
        return results.size();
    }

    /**
     * Return the position of the results in index being browsed. This is 0 for
     * the start of the index.
     *
     * @return The position of the results in index being browsed.
     */
    public int getOverallPosition()
    {
        return overallPosition;
    }

    /**
     * Return the total number of items in the index.
     *
     * @return The total number of items in the index.
     */
    public int getTotal()
    {
        return total;
    }

    /**
     * Return the position of the requested item or value in the set of results.
     *
     * @return The position of the requested item or value in the set of results
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * True if there are no previous results from the browse.
     *
     * @return True if there are no previous results from the browse
     */
    public boolean isFirst()
    {
        return overallPosition == 0;
    }

    /**
     * True if these are the last results from the browse.
     *
     * @return True if these are the last results from the browse
     */
    public boolean isLast()
    {
        return (overallPosition + getResultCount()) == total;
    }

    /**
     * True if this browse was cached.
     * @return true/false
     */
    public boolean wasCached()
    {
        return cached;
    }

    /**
     * Set whether this browse was cached.
     */
    void setCached(boolean cached)
    {
        this.cached = cached;
    }

    /**
     * are we browsing within a Community container?
     *
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
     * are we browsing within a Collection container
     *
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
	 * Are there further results for the browse that haven't been returned yet?
	 *
	 * @return	true if next page, false if not
	 */
	public boolean hasNextPage()
	{
		if (nextOffset > -1)
		{
			return true;
		}
		return false;
	}

	/**
	 * Are there results prior to these that haven't been returned here?
	 *
	 * @return	true if previous page, false if not
	 */
	public boolean hasPrevPage()
	{
		if (offset > 0)
		{
			return true;
		}
		return false;
	}

	/**
	 * Does this browse have a focus?
	 *
	 * @return	true if focus, false if not
	 */
	public boolean hasFocus()
	{
		if ("".equals(focus) || focus == null)
		{
			return false;
		}
		return true;
	}

	/**
	 * Get an integer representing the number within the total set of results which
	 * marks the position of the first result in the current sub-set
	 *
	 * @return	the start point of the browse page
	 */
	public int getStart()
	{
		return overallPosition + 1;
	}

	/**
	 * Get an integer representing the number within the total set of results which
	 * marks the position of the last result in the current sub-set
	 *
	 * @return	the end point of the browse page
	 */
	public int getFinish()
	{
		return overallPosition + results.size();
	}

	/**
	 * Utility method for obtaining a string representation of the browse.  This is
	 * useful only for debug
         * @return String representation
	 */
    public String toString()
    {
    	try
    	{
    		StringBuffer sb = new StringBuffer();

    		// calculate the range for display
    		String from = Integer.toString(overallPosition + 1);
    		String to = Integer.toString(overallPosition + results.size());
    		String of = Integer.toString(total);

    		// report on the positional information of the browse
    		sb.append("BrowseInfo String Representation: ");
    		sb.append("Browsing " + from + " to " + to + " of " + of + " ");

    		// insert the information about which index
    		sb.append("in index: " + browseIndex.getName() +
    				" (data type: " + browseIndex.getDataType() +
    				", display type: " + browseIndex.getDisplayType() + ") ");

    		sb.append("||");

    		// report on the browse scope container
    		String container = "all of DSpace";
    		DSpaceObject theContainer = null;
    		if (inCollection())
    		{
    			container = "collection";
    			theContainer = this.collection;
    		}
    		else if (inCommunity())
    		{
    			container = "community";
    			theContainer = this.community;
    		}

    		String containerID = "no id available/necessary";
    		if (theContainer != null)
    		{
    			containerID = theContainer.getID().toString() + " (" + theContainer.getHandle() + ")";
    		}

    		sb.append("Browsing in " + container + ": " + containerID);
    		sb.append("||");

    		// load the item list display configuration
    		ItemListConfig config = new ItemListConfig();

    		// some information about the columns to be displayed
    		if (browseIndex.isItemIndex())
    		{
    			sb.append("Listing over " + Integer.toString(config.numCols()) + " columns: ");
    			for (int k = 1; k <= config.numCols(); k++)
    			{
    				if (k > 1)
    				{
    					sb.append(",");
    				}
    				String[] meta = config.getMetadata(k);
    				sb.append(meta[0] + "." + meta[1] + "." + meta[2]);
    			}

    			if (value != null)
    			{
    				sb.append(" on value: ").append(value);
    			}

    			if (isStartsWith())
    			{
    				sb.append(" sort column starting with: ").append(focus);
    			}
    			else if (hasFocus())
    			{
    				sb.append(" sort column focus: ").append(focus);
    			}
    		}
    		else if (browseIndex.isMetadataIndex())
    		{
    			sb.append("Listing single column: ").append(browseIndex.getMetadata());
    			if (isStartsWith())
    			{
    				sb.append(" sort column starting with: ").append(focus);
    			}
    			else if (hasFocus())
    			{
    				sb.append(" sort column focus: ").append(focus);
    			}
    		}

    		sb.append("||");

    		// some information about how the data is sorted
    		String direction = (ascending ? "ASC" : "DESC");
    		sb.append("Sorting by: " + sortOption.getMetadata() + " " + direction +
    				" (option " + Integer.toString(sortOption.getNumber()) + ")");
    		sb.append("||");

    		// output the results
    		if (browseIndex.isMetadataIndex() && !isSecondLevel())
    		{
    			sb.append(valueListingString());
    		}
    		else if (browseIndex.isItemIndex() || isSecondLevel())
    		{
    			sb.append(fullListingString(config));
    		}

    		sb.append("||");

    		// tell us what the next and previous values are going to be
    		sb.append("Top of next page: ");
    		if (hasNextPage())
    		{
				sb.append("offset: ").append(Integer.toString(this.nextOffset));
    		}
    		else
    		{
    			sb.append("n/a");
    		}
    		sb.append(";");

    		sb.append("Top of previous page: ");
    		if (hasPrevPage())
    		{
				sb.append("offset: ").append(Integer.toString(this.prevOffset));
    		}
    		else
    		{
    			sb.append("n/a");
    		}

    		sb.append("||");

    		return sb.toString();
    	}
    	catch (SQLException e)
    	{
    		return e.getMessage();
    	}
    	catch (BrowseException e)
    	{
    		return e.getMessage();
    	}
    }

    /**
     * A utility method for generating a string to represent a single item's
     * entry in the browse
     *
     * @param config
     * @return
     * @throws SQLException if database error
     */
    private String fullListingString(ItemListConfig config)
    	throws SQLException
    {
    	// report on all the results contained herein
    	StringBuffer sb = new StringBuffer();

		Iterator itr = results.iterator();
		while (itr.hasNext())
		{
			Item bi = (Item) itr.next();
			if (bi == null)
			{
				sb.append("{{ NULL ITEM }}");
				break;
			}
			sb.append("{{Item ID: " + bi.getID().toString() + " :: ");

			for (int j = 1; j <= config.numCols(); j++)
			{
				String[] md = config.getMetadata(j);
				if (md == null)
    			{
    				sb.append("{{ NULL METADATA }}");
    				break;
    			}
				List<MetadataValue> values = itemService.getMetadata(bi, md[0], md[1], md[2], Item.ANY);
				StringBuffer value = new StringBuffer();
				if (values != null)
				{
					for (int i = 0; i < values.size(); i++)
					{
						if (i > 0)
						{
							value.append(",");
						}
						value.append(values.get(i).getValue());
					}
				}
				else
				{
					value.append("-");
				}
				String metadata = "[" + md[0] + "." + md[1] + "." + md[2] + ":" + value.toString() + "]";
				sb.append(metadata);
			}

			sb.append("}}");
		}

		return sb.toString();
    }

    /**
     * A utility method for representing a single value in the browse
     *
     * @return
     */
    private String valueListingString()
    {
    	// report on all the results contained herein
    	StringBuffer sb = new StringBuffer();

		Iterator itr = results.iterator();
		while (itr.hasNext())
		{
			String theValue = (String) itr.next();
			if (theValue == null)
			{
				sb.append("{{ NULL VALUE }}");
				break;
			}
			sb.append("{{Value: " + theValue + "}}");
		}

		return sb.toString();
    }
}
