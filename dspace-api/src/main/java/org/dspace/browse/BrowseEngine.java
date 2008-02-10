/*
 * BrowseEngine.java
 *
 * Version: $Revision: $
 *
 * Date: $Date: $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.sort.SortOption;
import org.dspace.sort.OrderFormat;

/**
 * This class does most of the actual grunt work of preparing a browse
 * result.  It takes in to a couple of available methods (depending on your
 * desired browse type) a BrowserScope object, and uses this to produce a
 * BrowseInfo object which is sufficient to describe to the User Interface
 * the results of the requested browse
 * 
 * @author Richard Jones
 *
 */
public class BrowseEngine
{
	/** the logger for this class */
	private static Logger log = Logger.getLogger(BrowseEngine.class);
	
	/** the browse scope which is the basis for our browse */
	private BrowserScope scope;
	
	/** the DSpace context */
	private Context context;
	
	/** The Data Access Object for the browse tables */
	private BrowseDAO dao;
	
	/** The Browse Index associated with the Browse Scope */
	private BrowseIndex browseIndex;
	
	/**
	 * Create a new instance of the Browse engine, using the given DSpace
	 * Context object.  This will automatically assign a Data Access Object
	 * for the Browse Engine, based on the dspace.cfg setting for db.name
	 * 
	 * @param context		the DSpace context
	 * @throws BrowseException
	 */
	public BrowseEngine(Context context)
		throws BrowseException
	{
		// set the context
		this.context = context;
		
		// prepare the data access object
		dao = BrowseDAOFactory.getInstance(context);
	}
	
	/**
	 * Perform a standard browse, which will return a BrowseInfo
	 * object that represents the results for the current page, the
	 * total number of results, the range, and information to construct
	 * previous and next links on any web page
	 * 
	 * @param bs	the scope of the browse
	 * @return		the results of the browse
	 * @throws BrowseException
	 */
	public BrowseInfo browse(BrowserScope bs)
		throws BrowseException
	{
		log.debug(LogManager.getHeader(context, "browse", ""));
		
		// first, load the browse scope into the object
		this.scope = bs;
		
		// since we use it so much, get the browse index out of the 
		// scope and store as a member
		browseIndex = scope.getBrowseIndex();
		
		// now make the decision as to how to browse
		if (browseIndex.isMetadataIndex() && !scope.isSecondLevel())
		{
			// this is a single value browse type that has not gone to
			// the second level (i.e. authors, not items by a given author)
			return browseByValue(scope);
		}
		else
		{
			// this is the full browse type or a browse that has gone to
			// the second level
			return browseByItem(scope);
		}
	}

	/**
	 * Perform a limited browse, which only returns the results requested, 
	 * without any extraneous information.  To perform a full browse, use
	 * BrowseEngine.browse() above.  This supports Item browse only, and does
	 * not currently support focus or values.  This method is used, for example,
	 * to generate the Recently Submitted Items results.
	 * 
	 * @param bs	the scope of the browse
	 * @return		the results of the browse
	 */
	public BrowseInfo browseMini(BrowserScope bs)
		throws BrowseException
	{
		log.info(LogManager.getHeader(context, "browse_mini", ""));
		
		// load the scope into the object
		this.scope = bs;
		
		// since we use it so much, get the browse index out of the 
		// scope and store as a member
		browseIndex = scope.getBrowseIndex();
		
		// get the table name that we are going to be getting our data from
        dao.setTable(browseIndex.getTableName());
		
		// tell the browse query whether we are ascending or descending on the value
		dao.setAscending(scope.isAscending());
		
		// define a clause for the WHERE clause which will allow us to constraine
		// our browse to a specified community or collection
		if (scope.inCollection() || scope.inCommunity())
		{
			if (scope.inCollection())
			{
				Collection col = (Collection) scope.getBrowseContainer();
                dao.setContainerTable("collection2item");
				dao.setContainerIDField("collection_id");
				dao.setContainerID(col.getID());
			}
			else if (scope.inCommunity())
			{
				Community com = (Community) scope.getBrowseContainer();
                dao.setContainerTable("communities2item");
				dao.setContainerIDField("community_id");
				dao.setContainerID(com.getID());
			}
		}
		
		// assemble the LIMIT clause
		dao.setLimit(scope.getResultsPerPage());
		
		// assemble the ORDER BY clause
        String orderBy = browseIndex.getSortField(scope.isSecondLevel());
		if (scope.getSortBy() > 0)
		{
			orderBy = "sort_" + Integer.toString(scope.getSortBy());
		}
		dao.setOrderField(orderBy);
		
		// now run the query
		List results = dao.doQuery();
		
		// construct the mostly empty BrowseInfo object to pass back
		BrowseInfo browseInfo = new BrowseInfo(results, 0, scope.getResultsPerPage(), 0);
		
		// add the browse index to the Browse Info
		browseInfo.setBrowseIndex(browseIndex);
		
		// set the sort option for the Browse Info
		browseInfo.setSortOption(scope.getSortOption());
		
		// tell the Browse Info which way we are sorting
		browseInfo.setAscending(scope.isAscending());
		
		// tell the browse info what the container for the browse was
		if (scope.inCollection() || scope.inCommunity())
		{
			browseInfo.setBrowseContainer(scope.getBrowseContainer());
		}
		
		browseInfo.setResultsPerPage(scope.getResultsPerPage());
		
        browseInfo.setEtAl(scope.getEtAl());
        
		return browseInfo;
	}
	
	/**
	 * Browse the archive by the full item browse mechanism.  This produces a
	 * BrowseInfo object which contains full BrowseItem objects as its result
	 * set.
	 * 
	 * @param bs		the scope of the browse
	 * @return			the results of the browse
	 * @throws BrowseException
	 */
	private BrowseInfo browseByItem(BrowserScope bs)
		throws BrowseException
	{
		log.info(LogManager.getHeader(context, "browse_by_item", ""));
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
		try
		{
            // get the table name that we are going to be getting our data from
            dao.setTable(browseIndex.getTableName());
			
			// tell the browse query whether we are ascending or descending on the value
			dao.setAscending(scope.isAscending());
			
			// prepare the parameters for the focus clause if we are to have one
			String focusValue = null;
			String rawFocusValue = null;
			String focusField = browseIndex.getSortField(scope.isSecondLevel());
			
			if (scope.hasJumpToItem() || scope.hasJumpToValue() || scope.hasStartsWith())
			{
				focusValue = getJumpToValue();
			
				// store the value to tell the Browse Info object which value we are browsing on
				// (we do it here because we may append a "%" in a moment)
				rawFocusValue = focusValue;
				
				// make sure the incoming value is normalised
				focusValue = normalizeJumpToValue(focusValue);
				
				// if the value was a "starts with" value, we need to append the
				// regular expression wildcard
				if (scope.hasStartsWith())
				{
					if (browseIndex.isDate())
					{
						focusValue = focusValue + "-32";
					}
				}
				
				log.debug("browsing using focus: " + focusValue);
				
				if (scope.getSortBy() > 0)
				{
					focusField = "sort_" + Integer.toString(scope.getSortBy());
				}
				
				// pass the values into the BrowseQuery
				dao.setJumpToField(focusField);
				dao.setJumpToValue(focusValue);
			}
			
            // assemble the value clause
			String value = null;
			String rawValue = null;
			if (scope.hasFilterValue() && scope.isSecondLevel())
			{
				value = scope.getFilterValue();
				rawValue = value;
				
				// make sure the incoming value is normalised
                value = OrderFormat.makeSortString(value, scope.getFilterValueLang(),
                            scope.getBrowseIndex().getDataType());
				
				// set the values in the Browse Query
				dao.setFilterValueField("sort_value");
				dao.setFilterValue(value);
                dao.setFilterValuePartial(scope.getFilterValuePartial());

                // to apply the filtering, we need the distinct and map tables for the index
                dao.setFilterMappingTables(browseIndex.getTableName(false, false, true, false),
                                           browseIndex.getTableName(false, false, false, true));
            }
			
			// define a clause for the WHERE clause which will allow us to constraine
			// our browse to a specified community or collection
			if (scope.inCollection() || scope.inCommunity())
			{
				if (scope.inCollection())
				{
					Collection col = (Collection) scope.getBrowseContainer();
                    dao.setContainerTable("collection2item");
					dao.setContainerIDField("collection_id");
					dao.setContainerID(col.getID());
				}
				else if (scope.inCommunity())
				{
					Community com = (Community) scope.getBrowseContainer();
                    dao.setContainerTable("communities2item");
					dao.setContainerIDField("community_id");
					dao.setContainerID(com.getID());
				}
			}
			
			// assemble the ORDER BY clause
            String orderBy = browseIndex.getSortField(scope.isSecondLevel());
			if (scope.getSortBy() > 0)
			{
				orderBy = "sort_" + Integer.toString(scope.getSortBy());
			}
			dao.setOrderField(orderBy);
			
			// assemble the LIMIT clause
			// NOTE: the limit clause is one larger than the desired amount because we use the + 1 to generate the
			// focus for the next browse page
			dao.setLimit(scope.getResultsPerPage() + 1);
			
			// now run the query
			List results = dao.doQuery();
			
			// now, if we don't have any results, we are at the end of the browse.  This will
			// be because a starts_with value has been supplied for which we don't have
			// any items.  To do this, we want to use the results acquired in getPreviousPageID
			// called below to assemble the new result set.
			boolean showLast = false;
			if (results.size() == 0)
			{
				showLast = true;
			}
			
			// next we take that set of results and remove the ones that aren't genuinely
			// necessary to display.  This will include those which are for the next or
			// previous queries
			
			// NEXT PAGE
//			BrowseItem next = null;
			Item next = null;
			int lastIndex = results.size() - 1;
			if (lastIndex >= scope.getResultsPerPage())
			{
//				next = (BrowseItem) results.get(lastIndex);
				next = (Item) results.get(lastIndex);
				results.remove(lastIndex);
			}
			
			// PREVIOUS PAGE
			// this involves some slightly complex messing around, so delegated to
			// its own method.  remember to only do this when there is a focus
			// value, otherwise we are just on the first page
//			BrowseItem prev = null;
			Item prev = null;
			if (scope.hasJumpToItem() || scope.hasJumpToValue() || scope.hasStartsWith())
			{
				int prevID = -1;
				if (showLast)
				{
					prevID = getPreviousPageID(results);
				}
				else
				{
					prevID = getPreviousPageID(null);
				}
				if (prevID != -1)
				{
                    prev = itemDAO.retrieve(prevID);
                    // If we are browsing the withdrawn index, create a 'withdrawn' browse item
                    // Otherwise, assume that the item is in the archive and not withdrawn
//                    if (bs.getBrowseIndex() == BrowseIndex.getWithdrawnBrowseIndex())
//                        prev = new BrowseItem(context, prevID, false, true);
//                    else
//                        prev = new BrowseItem(context, prevID, true, false);
                }
			}
			
			// now we need to process the position, total and offset for the results
			
			// first, offset.
			// offset is always zero, and exists as a hang over from a previous system
			int offset = 0;
			
			// second, total
			// this is the total number of results in answer to the query
			int total = getTotalResults();
			
			// third, position
			// this is the location in the index of the first part item in the browse
			int position = -1;

            // Only calculate the position if there are actually results to display
            if (total > 0)
            {
                if (showLast)
                {
                    // if we are into showing the last page by default (see above for PREVIOUS page link for
                    // more information), then we need to reduce the position by the number of results per
                    // page
                    position = total - scope.getResultsPerPage();

                    // we can't be at a position before the start of the index!
                    if (position < 0)
                        position = 0;
                }
                else
                {
                    // if this is just a normal every day browse that doesn't satisfy the conditions
                    // above we have to go away and look up the position
                    // position = getPosition(focusValue, value, false);
                    position = getPosition(false);
                }
            }

            // construct the BrowseInfo object to pass back
			BrowseInfo browseInfo = new BrowseInfo(results, position, total, offset);
			
			// set the int value of the next item
			if (next != null)
			{
				browseInfo.setNextItem(next.getID());
			}
			
			// set the string value for the previous page
			if (prev != null)
			{
				browseInfo.setPrevItem(prev.getID());
			}
			
			// add the browse index to the Browse Info
			browseInfo.setBrowseIndex(browseIndex);
			
			// set the sort option for the Browse Info
			browseInfo.setSortOption(scope.getSortOption());
			
			// tell the Browse Info which way we are sorting
			browseInfo.setAscending(scope.isAscending());
			
			// tell the Browse Info which level of browse we are at
			browseInfo.setBrowseLevel(scope.getBrowseLevel());
			
			// set the browse value if there is one
			browseInfo.setValue(rawValue);
			
			// set the focus value if there is one
			browseInfo.setFocus(rawFocusValue);
			
			if (scope.hasJumpToItem())
			{
				browseInfo.setFocusItem(scope.getJumpToItem());
			}
			
			// tell the browse info if it is working from a starts with parameter
			browseInfo.setStartsWith(scope.hasStartsWith());
			
			// tell the browse info what the container for the browse was
			if (scope.inCollection() || scope.inCommunity())
			{
				browseInfo.setBrowseContainer(scope.getBrowseContainer());
			}
			
			browseInfo.setResultsPerPage(scope.getResultsPerPage());

	        browseInfo.setEtAl(scope.getEtAl());
			
			return browseInfo;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException(e);
		}
	}
	
	/**
	 * Browse the archive by single values (such as the name of an author).  This
	 * produces a BrowseInfo object that contains Strings as the results of 
	 * the browse
	 * 
	 * @param bs		the scope of the browse
	 * @return			the results of the browse
	 * @throws BrowseException
	 */
	private BrowseInfo browseByValue(BrowserScope bs)
		throws BrowseException
	{
		log.info(LogManager.getHeader(context, "browse_by_value", "focus=" + bs.getJumpToValue()));
		
		try
		{
			// get the table name that we are going to be getting our data from
			// this is the distinct table constrained to either community or collection
			dao.setTable(browseIndex.getTableName(true, scope.inCommunity(), scope.inCollection()));
			
			// remind the DAO that this is a distinct value browse, so it knows what sort
			// of query to build
			dao.setDistinct(true);
			
			// tell the browse query whether we are ascending or descending on the value
			dao.setAscending(scope.isAscending());
			
			// assemble the focus clase if we are to have one
			// it will look like one of the following
			// - sort_value < myvalue
			// = sort_1 > myvalue
			dao.setJumpToField("sort_value");
			String focusValue = null;
			String rawFocusValue = null;
			if (scope.hasJumpToValue() || scope.hasStartsWith())
			{
				focusValue = getJumpToValue();
				
				// store the value to tell the Browse Info object which value we are browsing on
				rawFocusValue = focusValue;
                
                // make sure the incoming value is normalised
                focusValue = normalizeJumpToValue(focusValue);
				
				// if the value was a "starts with" and also a date, we need to
				// append -32, so that the sorted search works
				if (scope.hasStartsWith() && browseIndex.isDate())
				{
					focusValue = focusValue + "-32";
				}
				
				// set the value at which the browse will start
				dao.setJumpToValue(focusValue);
			}
			
			// set our constraints on community or collection
			if (scope.inCollection() || scope.inCommunity())
			{
                // Scoped browsing of distinct metadata requires the mapping
                // table to be specified.
                dao.setFilterMappingTables(null, browseIndex.getTableName(false, false, false, true));

				if (scope.inCollection())
				{
					Collection col = (Collection) scope.getBrowseContainer();
                    dao.setContainerTable("collection2item");
					dao.setContainerIDField("collection_id");
					dao.setContainerID(col.getID());
				}
				else if (scope.inCommunity())
				{
					Community com = (Community) scope.getBrowseContainer();
                    dao.setContainerTable("communities2item");
					dao.setContainerIDField("community_id");
					dao.setContainerID(com.getID());
				}
			}
			
			// set the ordering field (there is only one option)
			dao.setOrderField("sort_value");
			
			// assemble the LIMIT clause
			// NOTE: the limit clause is one larger than the desired amount because we use the + 1 to generate the
			// focus for the next browse page
			dao.setLimit(scope.getResultsPerPage() + 1);
			
			// now run the query
			List results = dao.doValueQuery();
			
			// now, if we don't have any results, we are at the end of the browse.  This will
			// be because a starts_with value has been supplied for which we don't have
			// any items.  To do this, we want to use the results acquired in getPreviousPageID
			// called below to assemble the new result set.
			boolean showLast = false;
			if (results.size() == 0)
			{
				showLast = true;
			}
			
			// next we take that set of results and remove the ones that aren't genuinely
			// necessary to display.  This will include those which are for the next or
			// previous queries
			
			// NEXT PAGE
			String next = null;
			int lastIndex = results.size() - 1;
			if (lastIndex >= scope.getResultsPerPage())
			{
				next = (String) results.get(lastIndex);
				results.remove(lastIndex);
			}
			
			// PREVIOUS PAGE
			// this requires some work, so delegating to its own method
			String prev = null;
			if (scope.hasJumpToValue() || scope.hasStartsWith())
			{
				if (showLast)
				{
					prev = getPreviousPageValue(results);
				}
				else
				{
					prev = getPreviousPageValue(null);
				}
			}
			
			// now we need to process the position, total and offset for the results
			
			// first, offset.
			// offset is always zero, and exists as a hang over from a previous system
			int offset = 0;
			
			// second, total
			// this is the total number of results in answer to the query (in this case
			// we want to count distinct values, so we pass in true)
			int total = getTotalResults(true);
			
			// third, position
			// this is the location in the index of the first part item in the browse
			// (in this case we want to count distinct values, so we pass in true)
			// int position = getPosition(focusValue, null, true);
			int position = getPosition(true);
			
			// construct the BrowseInfo object to pass back
			BrowseInfo browseInfo = new BrowseInfo(results, position, total, offset);
			
			// set the string value for the next page
			if (next != null)
			{
				browseInfo.setNextValue(next);
			}
			
			// set the string value for the previous page
			if (prev != null)
			{
				browseInfo.setPrevValue(prev);
			}
			
			// add the browse index to the Browse Info
			browseInfo.setBrowseIndex(browseIndex);
			
			// set the sort option for the Browse Info
			browseInfo.setSortOption(scope.getSortOption());
			
			// tell the Browse Info which way we are sorting
			browseInfo.setAscending(scope.isAscending());
			
			// tell the Browse Info which level of browse we are at
			browseInfo.setBrowseLevel(scope.getBrowseLevel());
			
			// set the browse value if there is one
			browseInfo.setFocus(rawFocusValue);
					
			// tell the browse info if it is working from a starts with parameter
			browseInfo.setStartsWith(scope.hasStartsWith());
			
			// tell the browse info what the container for the browse was
			if (scope.inCollection() || scope.inCommunity())
			{
				browseInfo.setBrowseContainer(scope.getBrowseContainer());
			}
			
			browseInfo.setResultsPerPage(scope.getResultsPerPage());
			
			return browseInfo;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException(e);
		}
	}
	
	/**
	 * Return the focus value.
	 * 
	 * @return	the focus value to use
	 * @throws BrowseException
	 */
	private String getJumpToValue()
		throws BrowseException
	{
		log.debug(LogManager.getHeader(context, "get_focus_value", ""));
		
		// if the focus is by value, just return it
		if (scope.hasJumpToValue())
		{
			log.debug(LogManager.getHeader(context, "get_focus_value_return", "return=" + scope.getJumpToValue()));
			return scope.getJumpToValue();
		}
		
		// if the focus is to start with, then we need to return the value of the starts with
		if (scope.hasStartsWith())
		{
			log.debug(LogManager.getHeader(context, "get_focus_value_return", "return=" + scope.getStartsWith()));
			return scope.getStartsWith();
		}
		
		// since the focus is not by value, we need to obtain it
		
		// get the id of the item to focus on
		int id = scope.getJumpToItem();
		
		// get the table name.  We don't really need to care about whether we are in a
		// community or collection at this point.  This is only for full or second
		// level browse, so there is no need to worry about distinct value browsing
		String tableName = browseIndex.getTableName();
		
		// we need to make sure that we select from the correct column.  If the sort option
		// is the 0th option then we use sort_value, but if it is one of the others we have
		// to select from that column instead.  Otherwise, we end up missing the focus value
		// to do comparisons in other columns.  The use of the focus value needs to be consistent
		// across the browse
        SortOption so = scope.getSortOption();
        if (so == null || so.getNumber() == 0)
        {
            if (browseIndex.getSortOption() != null)
                so = browseIndex.getSortOption();
        }

        String col = "sort_1";
        if (so.getNumber() > 0)
        {
            col = "sort_" + Integer.toString(so.getNumber());
        }
		
		// now get the DAO to do the query for us, returning the highest
		// string value in the given column in the given table for the 
		// item (I think)
		String max = dao.doMaxQuery(col, tableName, id);
		
		log.debug(LogManager.getHeader(context, "get_focus_value_return", "return=" + max));
		
		return max;
	}
    
    /**
     * Return a normalized focus value. If there is no normalization that can be performed,
     * return the focus value that is passed in.
     * 
     * @param value a focus value to normalize
     * @return  the normalized focus value
     * @throws BrowseException
     */
    private String normalizeJumpToValue(String value)
        throws BrowseException
    {
        // If the scope has a focus value (focus by value)
        if (scope.hasJumpToValue())
        {
            // Normalize it based on the specified language as appropriate for this index
            return OrderFormat.makeSortString(scope.getJumpToValue(), scope.setJumpToValueLang(), scope.getBrowseIndex().getDataType());
        }
        else if (scope.hasStartsWith())
        {
            // Scope has a starts with, so normalize that instead
            return OrderFormat.makeSortString(scope.getStartsWith(), null, scope.getBrowseIndex().getDataType());
        }
        
        // No focus value on the scope (ie. focus by id), so just return the passed focus value
        // This is useful in cases where we have pulled a focus value from the index
        // which will already be normalized, and avoids another DB lookup
        return value;
    }
    
	/**
	 * Get the total number of results for the browse.  This is the same as
	 * calling getTotalResults(false)
	 * 
	 * @return
	 * @throws SQLException
	 * @throws BrowseException
	 */
	private int getTotalResults()
		throws SQLException, BrowseException
	{
		return getTotalResults(false);
	}
	
	/**
	 * Get the total number of results.  The argument determines whether this is a distinct
	 * browse or not as this has an impact on how results are counted
	 * 
	 * @param distinct	is this a distinct browse or not
	 * @return			the total number of results available in this type of browse
	 * @throws SQLException
	 * @throws BrowseException
	 */
	private int getTotalResults(boolean distinct)
		throws SQLException, BrowseException
	{
		log.debug(LogManager.getHeader(context, "get_total_results", "distinct=" + distinct));
		
		// get the table name that we are going to be getting our data from
		String tableName = browseIndex.getTableName(distinct, scope.inCommunity(), scope.inCollection());
		
		// tell the browse query whether we are distinct
		dao.setDistinct(distinct);
		
		// ensure that the select is set to "*"
		String[] select = { "*" };
		dao.setCountValues(select);
		
		// FIXME: it would be nice to have a good way of doing this in the DAO
		// now reset all of the fields that we don't want to have constraining
		// our count, storing them locally to reinstate later
		String focusField = dao.getJumpToField();
		String focusValue = dao.getJumpToValue();
		String orderField = dao.getOrderField();
		int limit = dao.getLimit();
		int offset = dao.getOffset();
		
		dao.setJumpToField(null);
		dao.setJumpToValue(null);
		dao.setOrderField(null);
		dao.setLimit(-1);
		dao.setOffset(-1);
		
		// perform the query and get the result
		int count = dao.doCountQuery();
		
		// now put back the values we removed for this method
		dao.setJumpToField(focusField);
		dao.setJumpToValue(focusValue);
		dao.setOrderField(orderField);
		dao.setLimit(limit);
		dao.setOffset(offset);
		
		log.debug(LogManager.getHeader(context, "get_total_results_return", "return=" + count));
		
		return count;
	}
	
	/**
	 * Get the position of the current start point of the browse in the field of total
	 * objects relevant to this browse.  This is integrally related to how results are
	 * presented in pages to the User Interface.  The argument tells us whether this
	 * is a distinct browse or not, as this has an impact on how results are calculated
	 * 
	 * @param distinct		is this a distinct browse
	 * @return				the offset of the first result from the start of the set
	 * @throws SQLException
	 * @throws BrowseException
	 */
	private int getPosition(boolean distinct)
		throws SQLException, BrowseException
	{
		log.debug(LogManager.getHeader(context, "get_position", "distinct=" + distinct));
		
		// if there isn't a focus then we are at the start
		if (dao.getJumpToValue() == null)
		{
			log.debug(LogManager.getHeader(context, "get_position_return", "return=0"));
			return 0;
		}
		
		// get the table name that we are going to be getting our data from
		String tableName = browseIndex.getTableName(distinct, scope.inCommunity(), scope.inCollection());
		
		// ensure that the select is set to "*"
		String[] select = { "*" };
		dao.setCountValues(select);
		
		// FIXME: it would be nice to have a good way of doing this in the DAO
		// now reset all of the fields that we don't want to have constraining
		// our count, storing them locally to reinstate later
		boolean isAscending = dao.isAscending();
		boolean useEquals = dao.useEqualsComparator();
		String orderField = dao.getOrderField();
		int limit = dao.getLimit();
		int offset = dao.getOffset();
		
		// reverse the direction of the query, and remove the equal comparator
		// (if it exists), as well as nullifying a few other unnecessary parameters
		dao.setAscending(!isAscending);
		dao.setEqualsComparator(false);
		dao.setOrderField(null);
		dao.setLimit(-1);
		dao.setOffset(-1);
		
		// perform the query and get the result
		int count = dao.doCountQuery();
		
		// now put back the values we removed for this method
		dao.setAscending(isAscending);
		dao.setEqualsComparator(useEquals);
		dao.setOrderField(orderField);
		dao.setLimit(limit);
		dao.setOffset(offset);
		
		log.debug(LogManager.getHeader(context, "get_position_return", "return=" + count));
		
		return count;
	}
	
	/**
	 * Get the database id of the item at the top of what will be the previous page
	 * of results.  This is so that a "back" button can be generated by the User
	 * Interface when paging through results.  The callback argument is there so that
	 * if the caller wishes the actual results from the previous page can also be returned
	 * (this is useful, for example, if you are on the very last page of results, and need
	 * some previous results to pad out the full number of results per page).  If the
	 * callback is null, then no results are kept
	 * 
	 * @param callback	A List object for holding BrowseItem objects indexed numerically in the correct order
	 * @return			the database id of the top of the previous page
	 * @throws SQLException
	 * @throws BrowseException
	 */
	private int getPreviousPageID(List callback)
		throws SQLException, BrowseException
	{
		log.debug(LogManager.getHeader(context, "get_previous_page_id", ""));
		
		// do we want to capture the results?
		boolean capture = false;
		if (callback != null)
		{
			capture = true;
		}
		
		// the only thing we need to do is reverse the direction
		// of the query, and set it to not use the "=" part of the
		// comparator (i.e. < and > not <= and >=).
		boolean isAscending = dao.isAscending();
		dao.setAscending(!isAscending);
		
		boolean useEquals = dao.useEqualsComparator();
		dao.setEqualsComparator(false);
		
		// store in local scope the things that we are going to change
		// during this method
		int resultLimit = dao.getLimit();
		
		// if the database supports it (postgres does), we use limit
		// and offset to minimise the work it has to do.
	
		// the limit will be the size of a page, or double the size of the
		// page if we are capturing the result set (because the first page's worth
		// will be the results, so the previous link will need to come from the
		// page *before* that)
		int limit = scope.getResultsPerPage();
		if (capture)
		{
			limit *= 2;
		}
		dao.setLimit(limit);
		
		// now we have a query which is exactly the opposite of the
		// original query.  So lets execute it:
		List results = dao.doQuery();
		
		// before we continue, put back the variables we messed with
		dao.setAscending(isAscending);
		dao.setEqualsComparator(useEquals);
		dao.setLimit(resultLimit);
		
		Iterator itr = results.iterator();
		
		// work our way through the list, capturing if necessary, and finally
		// having the last result, which will be the top of the previous page
		int i = 0;
//		BrowseItem prev = null;
		Item prev = null;
		while (itr.hasNext())
		{
//			BrowseItem browseItem = (BrowseItem) itr.next();
			Item browseItem = (Item) itr.next();
			
			// we need to copy this, because of the scoping vs by-reference issue
			prev = browseItem;
			
			// if we need to capture the results in the call back, do it here.
			// Note that since the results will come in backwards, we place
			// each one at the start of the array so that it displaces the others
			// in the right direction.  By the end, they should be sorted correctly
			// we use the index i to be sure that we only grab one page's worth
			// of results (see note above about limit)
			if (capture && i < scope.getResultsPerPage())
			{
				callback.add(0, browseItem);
				i++;
			}
		}
		
		if (prev != null)
		{
			return prev.getID();
		}
		else
		{
			return -1;
		}
	}
	
	/**
	 * Get the value (for single value browses) for the result that appears at the top
	 * of the previous page, for when the User Interface is performing result set paging.
	 * The callback argument allows for this method to populate the List with the results
	 * obtained from the query, which can be useful when, for example, reaching the final
	 * browse page and needing additional results to pad up to the number of results per
	 * page
	 * 
	 * @param callback		A List object for holding String objects indexed numerically in the correct order
	 * @return				the value of the top of the previous page
	 * @throws SQLException
	 * @throws BrowseException
	 */
	private String getPreviousPageValue(List callback)
		throws SQLException, BrowseException
	{
		// do we want to capture the results?
		boolean capture = false;
		if (callback != null)
		{
			capture = true;
		}
		
		log.debug(LogManager.getHeader(context, "get_previous_page_value", "capture_results=" + capture));
		
		// the only thing we need to do is reverse the direction
		// of the query, and set it to not use the "=" part of the
		// comparator (i.e. < and > not <= and >=).
		boolean isAscending = dao.isAscending();
		dao.setAscending(!isAscending);
		
		boolean useEquals = dao.useEqualsComparator();
		dao.setEqualsComparator(false);
		
		// store in local scope the things that we are going to change
		// during this method
		int resultLimit = dao.getLimit();
		
		// if the database supports it (postgres does), we use limit
		// and offset to minimise the work it has to do.
	
		// the limit will be the size of a page, or double the size of the
		// page if we are capturing the result set (because the first page's worth
		// will be the results, so the previous link will need to come from the
		// page *before* that)
		int limit = scope.getResultsPerPage();
		if (capture)
		{
			limit *= 2;
		}
		dao.setLimit(limit);
		
		// now we have a query which is exactly the opposite of the
		// original query.  So lets execute it:
		List results = dao.doValueQuery();
		
		// before we continue, put back the variables we messed with
		dao.setAscending(isAscending);
		dao.setEqualsComparator(useEquals);
		dao.setLimit(resultLimit);
		
		Iterator itr = results.iterator();
		
		// work our way through the list, capturing if necessary, and finally
		// having the last result, which will be the top of the previous page
		int i = 0;
		String prev = null;
		while (itr.hasNext())
		{
			String value = (String) itr.next();
			
			// we need to copy this, because of the scoping vs by-reference issue
			prev = value;
			
			// if we need to capture the results in the call back, do it here.
			// Note that since the results will come in backwards, we place
			// each one at the start of the array so that it displaces the others
			// in the right direction.  By the end, they should be sorted correctly
			// we use the index i to be sure that we only grab one page's worth
			// of results (see note above about limit)
			if (capture && i < scope.getResultsPerPage())
			{
				callback.add(0, value);
				i++;
			}
		}
		
		log.debug(LogManager.getHeader(context, "get_previous_page_value_return", "return=" + prev));
		
		return prev;
	}
}
