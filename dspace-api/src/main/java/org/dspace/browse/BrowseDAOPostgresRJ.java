/*
 * BrowseDAOPostgresRJ.java
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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * This class is the PostgreSQL driver class for reading information from the
 * Browse tables.  It implements the BrowseDAO interface, and also has a
 * constructor of the form:
 * 
 * BrowseDAOPostgres(Context context)
 * 
 * As required by BrowseDAOFactory.  This class should only ever be loaded by
 * that Factory object.
 * 
 * @author Richard Jones
 *
 */
public class BrowseDAOPostgresRJ implements BrowseDAO
{
	// administrative attributes for this class
	
	/** a cache of the actual query to be executed */
	private String query = "";
	
	/** whether the query (above) needs to be regenerated */
	private boolean rebuildQuery = true;
	
	/** Log4j log */
    private static Logger log = Logger.getLogger(BrowseDAOPostgres.class);
    
    /** The DSpace context */
    private Context context;
    
    private BrowseDAOUtils utils;
    
    // SQL query related attributes for this class
    
    /** the values to place in the SELECT --- FROM bit */
	private String[] selectValues = { "*" };
	
	/** the values to place in the SELECT DISTINCT(---) bit */
	private String[] distinctValues;
	
	/** the values to place in the SELECT COUNT(---) bit */
	private String[] countValues;
	
	/** table to select from */
	private String table = null;
	
	/** field to look for focus value in */
	private String focusField = null;
	
	/** value to start browse from in focus field */
	private String focusValue = null;
	
	/** field to look for value in */
	private String valueField = null;
	
	/** value to restrict browse to (e.g. author name) */
	private String value = null;
	
	/** the table that defines the mapping for the relevant container */
	private String containerTable = null;
	
	/** the name of the field which contains the container id (e.g. collection_id) */
	private String containerIDField = null;
	
	/** the database id of the container we are constraining to */
	private int containerID = -1;
	
	/** the column that we are sorting results by */
	private String orderField = null;
	
	/** whether to sort results ascending or descending */
	private boolean ascending = true;
	
	/** the limit of number of results to return */
	private int limit = -1;
	
	/** the offset of the start point (avoid using) */
	private int offset = -1;
	
	/** whether to use the equals comparator in value comparisons */
	private boolean equalsComparator = true;
	
	/** whether this is a distinct browse or not */
	private boolean distinct = false;
	
	/**
	 * Required constructor for use by BrowseDAOFactory 
	 * 
	 * @param context	DSpace context
	 */
	public BrowseDAOPostgresRJ(Context context)
		throws BrowseException
	{
		this.context = context;
		
        // obtain the relevant Utils for this class
        utils = BrowseDAOFactory.getUtils(context);
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#doCountQuery()
	 */
	public int doCountQuery()
		throws BrowseException
	{
		String query = getQuery();
		log.debug(LogManager.getHeader(context, "executing_count_query", "query=" + query));

        TableRowIterator tri = null;
        
		try
		{
			// now run the query
			tri = DatabaseManager.query(context, query);
			
			if (tri.hasNext())
			{
				TableRow row = tri.next();
				return (int) row.getLongColumn("number");
			}
			else
			{
				return 0;
			}
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#doValueQuery()
	 */
	public List doValueQuery()
		throws BrowseException
	{
		String query = getQuery();
		log.debug(LogManager.getHeader(context, "executing_value_query", "query=" + query));
		
        TableRowIterator tri = null;
        
		try
		{
			// now run the query
			tri = DatabaseManager.query(context, query);
			
			// go over the query results and process
			List results = new ArrayList();
			while (tri.hasNext())
			{
				TableRow row = tri.next();
				String stringResult = row.getStringColumn("value");
				results.add(stringResult);
			}
			
			return results;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#doQuery()
	 */
	public List doQuery()
		throws BrowseException
	{
		String query = getQuery();
		log.debug(LogManager.getHeader(context, "executing_full_query", "query=" + query));
		
        TableRowIterator tri = null;
		try
		{
			// now run the query
			tri = DatabaseManager.query(context, query);
			
			// go over the query results and process
			List results = new ArrayList();
			while (tri.hasNext())
			{
				TableRow row = tri.next();
				BrowseItem browseItem = new BrowseItem(context, row.getIntColumn("item_id"));
				results.add(browseItem);
			}
			
			return results;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new BrowseException("problem with query: " + query, e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getQuery()
	 */
	public String getQuery()
		throws BrowseException
	{
		if ("".equals(query) || rebuild())
		{
			if (this.isDistinct())
			{
				query = buildDistinctQuery();
			}
			else
			{
				query = buildQuery();
			}
			setRebuildQuery(false);
		}
		return query;
	}
	
	/**
	 * Build a clause for selecting distinct values.  Will return something of the form
	 * 
	 * <code>
	 * DISTINCT( [value 1], [value 2] )
	 * </code>
	 * 
	 * @return the distinct clause
	 */
	private String distinctClause()
	{
		String selectArgs = "";
		if (distinctValues != null)
		{
			for (int i = 0; i < distinctValues.length; i++)
			{
				if (i == 0)
				{
					selectArgs = selectArgs + " DISTINCT(";
				}
				if (i > 0)
				{
					selectArgs = selectArgs + " , ";
				}
				selectArgs = selectArgs + escape(distinctValues[i]);
				if (i == distinctValues.length - 1)
				{
					selectArgs = selectArgs + ") ";
				}
			}
		}
		return selectArgs;
	}
	
	/**
	 * Build a clause for counting results.  Will return something of the form:
	 * 
	 * <code>
	 * COUNT( [value 1], [value 2] ) AS number
	 * </code>
	 * 
	 * @return 	the count clause
	 */
	private String countClause()
	{
		String selectArgs = "";
		if (countValues != null)
		{
			for (int i = 0; i < countValues.length; i++)
			{
				if (i == 0)
				{
					selectArgs = selectArgs + " COUNT(";
				}
				if (i > 0)
				{
					selectArgs = selectArgs + " , ";
				}
				selectArgs = selectArgs + escape(countValues[i]);
				if (i == countValues.length - 1)
				{
					selectArgs = selectArgs + ") AS number";
				}
			}
		}
		return selectArgs;
	}
	
	/**
	 * Prepare the list of values to be selected on.  Will return something of the form:
	 * 
	 * <code>
	 * [value 1], [value 2]
	 * </code>
	 * 
	 * @return	the select value list
	 */
	private String selectValues()
	{
		String selectArgs = "";
		if (selectValues != null)
		{
			for (int i = 0; i < selectValues.length; i++)
			{
				if ((!"".equals(selectArgs) && i == 0) || i > 0)
				{
					selectArgs = selectArgs + " , ";
				}
				selectArgs = selectArgs + escape(selectValues[i]);
			}
		}
		return selectArgs;
	}
	
	/**
	 * Get the comparator which should be used to compare focus values
	 * with values in the database.  This will return one of the 4 following
	 * possible values: <, >, <=, >=
	 * 
	 * @return		the focus comparator
	 */
	private String getFocusComparator()
	{
		// now decide whether we will use an equals comparator;
		String equals = "=";
		if (!useEqualsComparator())
		{
			equals = "";
		}
		
		// get the comparator for the match of the browsable index value
		// the rule is: if the scope has a value, then the comparator is always "="
		// if, the order is set to ascending then we want to use 
		// WHERE sort_value > <the value>
		// and when the order is descending then we want to use
		// WHERE sort_value < <the value>
		String focusComparator = "";
		if (isAscending())
		{
			focusComparator = ">" + equals;
		}
		else
		{
			focusComparator = "<" + equals;
		}
		
		return focusComparator;
	}
	
	/**
	 * Get the clause to get the browse to start from a given focus value.
	 * Will return something of the form:
	 * 
	 * <code>
	 * [field] (<[=] | >[=]) '[value]'
	 * </code>
	 * 
	 * such as:
	 * 
	 * <code>
	 * sort_value <= 'my text'
	 * </code>
	 * 
	 * @return	the focus clause
	 */
	private String getFocusClause()
	{
		// get the operator (<[=] | >[=]) which the focus of the browse will
		// be matched using
		String focusComparator = getFocusComparator();
		
		// assemble the focus clase if we are to have one
		// it will look like one of the following
		// - sort_value <= myvalue
		// - sort_1 >= myvalue
		String focusClause = "";
		if (focusField != null && focusValue != null)
		{
			focusClause = " " + escape(focusField) + " " + focusComparator + " '" + escape(focusValue) + "' ";
		}
		
		return focusClause;
	}
	
	/**
	 * Return the clause to constrain the browse to a specific value.
	 * Will return something of the form:
	 * 
	 * <code>
	 * [field] = '[value]'
	 * </code>
	 * 
	 * such as:
	 * 
	 * <code>
	 * sort_value = 'some author'
	 * </code>
	 * 
	 * @return	the value clause
	 */
	private String getValueClause()
	{
		// assemble the value clause if we are to have one
		String valueClause = "";
		if (value != null && valueField != null)
		{
			String valueComparator = "=";
			valueClause = " " + escape(valueField) + " " + valueComparator + " '" + escape(value) + "' ";
		}
		return valueClause;
	}
	
	/**
	 * assemble a WHERE clause with the given constraints.  This will return something
	 * of the form:
	 * 
	 * <code>
	 * WHERE [focus clause] [AND] [value clause] [AND] [container constraint]
	 * </code>
	 * 
	 * The container constraint is described in one of either getFullConstraint or 
	 * getDistinctConstraint, and the form of that section of the query can be
	 * found in their documentation.
	 * 
	 * If either of focusClause or valueClause is null, they will be duly omitted from
	 * the WHERE clause.
	 * 
	 * @param focusClause	the focus clause, as returned by getFocusClause
	 * @param valueClause	the value clause, as returned by getValue Clause
	 * @param useContainer	whether to constrain to a container
	 * @param isDistinct	whether this is a distinct browse
	 * @return	the WHERE clause
	 */
	private String getWhereClause(String focusClause, String valueClause, boolean useContainer, boolean isDistinct)
	{
		// assemble the where clause out of the two possible value clauses
		String whereClause = "";
		
		if (!"".equals(focusClause) && focusClause != null)
		{
			whereClause = whereClause + focusClause;
		}
		
		if (!"".equals(valueClause) && valueClause != null)
		{
			if (!"".equals(whereClause))
			{
				whereClause = whereClause + " AND ";
			}
			whereClause = whereClause + valueClause;
		}
		
		// add the constraint to community or collection if necessary
		// and desired
		if (useContainer)
		{
			String constraint = "";
			if (isDistinct)
			{
				constraint =  getDistinctConstraint();
			}
			else
			{
				constraint =  getFullConstraint();
			}
			
			if (!"".equals(constraint))
			{
				if (!"".equals(whereClause))
				{
					whereClause = whereClause + " AND ";
				}
				whereClause = whereClause + constraint;
			}
			
		}
		
		// now finalise the construction of the where clause
		if (!"".equals(whereClause))
		{
			whereClause = " WHERE " + whereClause;
		}
		
		return whereClause;
	}
	
	/**
	 * Get a sub-query to obtain the ids for a distinct browse within a given
	 * constraint.  This will produce something of the form:
	 * 
	 * <code>
	 * id IN (SELECT distinct_id FROM [container table] WHERE [container field] = [container id])
	 * </code>
	 * 
	 * This is for use inside the overall WHERE clause only
	 * 
	 * @return	the sub-query
	 */
	private String getDistinctConstraint()
	{
		String constraint = "";
		if (containerIDField != null && containerID != -1 && containerTable != null)
		{
			constraint = " id IN (SELECT distinct_id FROM " + escape(containerTable) + 
						" WHERE " + escape(containerIDField) + " = " + containerID + ") ";
		}
		return constraint;
	}
	
	/**
	 * Get a clause to obtain the ids for a full browse within a given
	 * constraint.  This will produce something of the form:
	 * 
	 * <code>
	 * [container field] = [container id]
	 * </code>
	 * 
	 * This is for use inside the overall WHERE clause only
	 * 
	 * @return	the constraint clause
	 */
	private String getFullConstraint()
	{
		String constraint = "";
		if (containerIDField != null && containerID != -1)
		{
			constraint = escape(containerIDField) + " = " + containerID + " ";
		}
		return constraint;
	}
	
	/**
	 * Get the clause to perform search result ordering.  This will
	 * return something of the form:
	 * 
	 * <code>
	 * ORDER BY [order field] (ASC | DESC)
	 * </code>
	 * 
	 * @return	the ORDER BY clause
	 */
	private String getOrderBy()
	{
		// assemble the order by field
		String orderBy = "";
		if (orderField != null)
		{
			orderBy = " " + escape(orderField);
			if (isAscending())
			{
				orderBy = orderBy + " ASC ";
			}
			else
			{
				orderBy = orderBy + " DESC ";
			}
			orderBy = " ORDER BY " + orderBy;
		}
		
		return orderBy;
	}
	
	/**
	 * Get the limit clause to perform search result truncation.  Will return
	 * something of the form:
	 * 
	 * <code>
	 * LIMIT [limit]
	 * </code>
	 * 
	 * @return	the limit clause
	 */
	private String getLimitClause()
	{
		// prepare the LIMIT clause
		String limitClause = "";
		if (limit != -1)
		{
			limitClause = " LIMIT " + Integer.toString(limit);
		}
		
		return limitClause;
	}
	
	/**
	 * Get the offset clause to offset the start point of search results
	 * 
	 * @return
	 * @deprecated
	 */
	private String getOffsetClause()
	{
		// prepare the OFFSET clause
		String offsetClause = "";
		if (offset != -1)
		{
			offsetClause = " OFFSET " + Integer.toString(offset);
		}
		
		return offsetClause;
	}
	
	/**
	 * Prepare the select clause using the pre-prepared arguments.  This will produce something
	 * of the form:
	 * 
	 * <code>
	 * SELECT [arguments] FROM [table]
	 * </code>
	 * 
	 * @param args	the string value obtained from distinctClause, countClause or selectValues
	 * @return	the SELECT part of the query
	 */
	private String selectClause(String args)
	{
		String selectFrom = "SELECT " + args + " FROM " + escape(table) + " ";
		return selectFrom;
	}
	
	/**
	 * Assemble a query from the various component parts passed in here.  If any of those
	 * parts are null, they will be duly omitted from the final query.  This will
	 * produce something of the form:
	 * 
	 * <code>
	 * [select] [where] [order by] [limit] [offset]
	 * </code>
	 * 
	 * @param selectFrom		SELECT part of the query
	 * @param whereClause		WHERE clause
	 * @param orderBy			ORDER BY clause
	 * @param limitClause		LIMIT clause
	 * @param offsetClause		OFFSET clause
	 * @return					the final query to be executed
	 * @throws BrowseException
	 */
	private String assembleQuery(String selectFrom, String whereClause, String orderBy, String limitClause, String offsetClause)
		throws BrowseException
	{
		if (selectFrom == null)
		{
			throw new BrowseException("Cannot generate query: the SELECT clause does not exist");
		}
		
		if (whereClause == null) { whereClause = ""; }
		if (orderBy == null) { orderBy = ""; }
		if (limitClause == null) { limitClause = ""; }
		if (offsetClause == null) { offsetClause = ""; }
		
		String query = selectFrom + whereClause + orderBy + limitClause + offsetClause;
		return query;
	}
	
	/**
	 * Build the query that will be used for a distinct select.  This incorporates
	 * only the parts of the parameters that are actually useful for this type
	 * of browse
	 * 
	 * @return		the query to be executed
	 * @throws BrowseException
	 */
	private String buildDistinctQuery()
		throws BrowseException
	{
		String selectArgs = countClause();
		if ("".equals(selectArgs) || selectArgs == null)
		{
			selectArgs = selectValues();
		}
		if ("".equals(selectArgs) || selectArgs == null)
		{
			throw new BrowseException("No arguments for SELECT statement");
		}
		String selectFrom = selectClause(selectArgs);
		
		// assemble the focus clase if we are to have one
		// it will look like one of the following, for example
		//     sort_value <= myvalue
		//     sort_1 >= myvalue
		String focusClause = getFocusClause();
		
		// assemble the where clause out of the two possible value clauses
		// and include container support
		String whereClause = getWhereClause(focusClause, null, true, true);
		
		// assemble the order by field
		String orderBy = getOrderBy();
		
		// prepare the LIMIT clause
		String limitClause = getLimitClause();
		
		// finally, put the query together and return it
		String query = assembleQuery(selectFrom, whereClause, orderBy, limitClause, null);
		return query;
	}
	
	/**
	 * Build the query that will be used for a full browse.
	 * 
	 * @return		the query to be executed
	 * @throws BrowseException
	 */
	private String buildQuery()
		throws BrowseException
	{
		// build the SELECT part of the query
		String selectArgs = countClause();
		if ("".equals(selectArgs) || selectArgs == null)
		{
			selectArgs = distinctClause();
			selectArgs = selectArgs + selectValues();
		}
		if ("".equals(selectArgs) || selectArgs == null)
		{
			throw new BrowseException("No arguments for SELECT statement");
		}
		String selectFrom = selectClause(selectArgs);
		
		// assemble the focus clase if we are to have one
		// it will look like one of the following, for example
		//     sort_value <= myvalue
		//     sort_1 >= myvalue
		String focusClause = getFocusClause();
		
		// assemble the value clause if we are to have one
		String valueClause = getValueClause();
		
		// assemble the where clause out of the two possible value clauses
		// and include container support
		String whereClause = getWhereClause(focusClause, valueClause, true, false);
		
		// assemble the order by field
		String orderBy = getOrderBy();
		
		// prepare the LIMIT clause
		String limitClause = getLimitClause();
		
		// prepare the OFFSET clause
		String offsetClause = getOffsetClause();
		
		// finally, put the query together and return it
		String query = assembleQuery(selectFrom, whereClause, orderBy, limitClause, offsetClause);
		return query;
	}
	
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#doMaxQuery(java.lang.String, java.lang.String, int)
	 */
	public String doMaxQuery(String column, String table, int itemID)
		throws BrowseException
	{
        TableRowIterator tri = null;
        
		try
		{
			String query = "SELECT max(" + column + ") FROM " + table + " WHERE item_id = ?";
			
			Object[] params = { new Integer(itemID) };
			tri = DatabaseManager.query(context, query, params);
			
			TableRow row;
			if (tri.hasNext())
			{
				row = tri.next();
				return row.getStringColumn("max");
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			throw new BrowseException(e);
		}
        finally
        {
            if (tri != null)
                tri.close();
        }
	}
	
	/**
	 * Tell the class that the query needs to be rebuilt again.  This should be
	 * called after any modification of the parameters.
	 * 
	 * @param bool	whether to rebuild the query again or not
	 */
	private void setRebuildQuery(boolean bool)
	{
		this.rebuildQuery = bool;
	}
	
	/**
	 * Should the query be rebuilt?
	 * @return
	 */
	private boolean rebuild()
	{
		return this.rebuildQuery;
	}
	
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#useEqualsComparator()
	 */
	public boolean useEqualsComparator()
	{
		return equalsComparator;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setEqualsComparator(boolean)
	 */
	public void setEqualsComparator(boolean equalsComparator)
	{
		this.equalsComparator = equalsComparator;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#isAscending()
	 */
	public boolean isAscending()
	{
		return ascending;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setAscending(boolean)
	 */
	public void setAscending(boolean ascending)
	{
		this.ascending = ascending;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getContainerID()
	 */
	public int getContainerID()
	{
		return containerID;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setContainerID(int)
	 */
	public void setContainerID(int containerID)
	{
		this.containerID = containerID;
		setRebuildQuery(true);
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getContainerIDField()
	 */
	public String getContainerIDField()
	{
		return containerIDField;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setContainerIDField(java.lang.String)
	 */
	public void setContainerIDField(String containerIDField)
	{
		this.containerIDField = containerIDField;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getFocusField()
	 */
	public String getFocusField()
	{
		return focusField;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setFocusField(java.lang.String)
	 */
	public void setFocusField(String focusField)
	{
		this.focusField = focusField;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getFocusValue()
	 */
	public String getFocusValue()
	{
		return focusValue;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setFocusValue(java.lang.String)
	 */
	public void setFocusValue(String focusValue)
	{
		this.focusValue = focusValue;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getLimit()
	 */
	public int getLimit()
	{
		return limit;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setLimit(int)
	 */
	public void setLimit(int limit)
	{
		this.limit = limit;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getOffset()
	 */
	public int getOffset()
	{
		return offset;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setOffset(int)
	 */
	public void setOffset(int offset)
	{
		this.offset = offset;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getOrderField()
	 */
	public String getOrderField()
	{
		return orderField;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setOrderField(java.lang.String)
	 */
	public void setOrderField(String orderField)
	{
		this.orderField = orderField;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getSelectValues()
	 */
	public String[] getSelectValues()
	{
		return selectValues;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setSelectValues(java.lang.String[])
	 */
	public void setSelectValues(String[] selectValues)
	{
		this.selectValues = selectValues;
		setRebuildQuery(true);
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#selectDistinctOn(java.lang.String[])
	 */
	public void selectDistinctOn(String[] fields)
	{
		this.distinctValues = fields;
		setRebuildQuery(true);
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getCountValues()
	 */
	public String[] getCountValues()
	{
		return this.countValues;
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getDistinctValues()
	 */
	public String[] getDistinctValues()
	{
		return this.distinctValues;
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setCountValues(java.lang.String[])
	 */
	public void setCountValues(String[] fields)
	{
		this.countValues = fields;
		setRebuildQuery(true);
	}
	
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getTable()
	 */
	public String getTable()
	{
		return table;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setTable(java.lang.String)
	 */
	public void setTable(String table)
	{
		this.table = table;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getValue()
	 */
	public String getValue()
	{
		return value;
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setValue(java.lang.String)
	 */
	public void setValue(String value)
	{
		this.value = value;
		setRebuildQuery(true);
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getValueField()
	 */
	public String getValueField()
	{
		return valueField;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setValueField(java.lang.String)
	 */
	public void setValueField(String valueField)
	{
		this.valueField = valueField;
		setRebuildQuery(true);
	}

	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setDistinct(boolean)
	 */
	public void setDistinct(boolean bool)
	{
		this.distinct = bool;
		setRebuildQuery(true);
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#isDistinct()
	 */
	public boolean isDistinct()
	{
		return this.distinct;
	}
	
	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#getContainerTable()
	 */
	public String getContainerTable()
	{
		return containerTable;
	}

	
	/* (non-Javadoc)
	 * @see org.dspace.browse.BrowseDAO#setContainerTable(java.lang.String)
	 */
	public void setContainerTable(String containerTable)
	{
		this.containerTable = containerTable;
	}

	/**
	 * Escape the passed string so that it is safe to run against the database.  We
	 * don't use the usual PreparedStatement because the query is too complicated
	 * to assemble in a way that makes that feasible.
	 * 
	 * @param source	the string to be escaped
	 * @return			the escaped string
	 */
	private String escape(String source)
	{
		// escape is simply the process of converting ' to \' (I think!) (which is double escaped in Java)
		String result = source.replace("'", "''");
		return result;
	}
	
}
