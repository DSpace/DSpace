/*
 * BrowseDAOOracle.java
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
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * This class is the Oracle driver class for reading information from the Browse
 * tables. It implements the BrowseDAO interface, and also has a constructor of
 * the form:
 * 
 * BrowseDAOOracle(Context context)
 * 
 * As required by BrowseDAOFactory. This class should only ever be loaded by
 * that Factory object.
 * 
 * @author Graham Triggs
 * 
 */
public class BrowseDAOOracle implements BrowseDAO
{
    /** Log4j log */
    private static Logger log = Logger.getLogger(BrowseDAOOracle.class);
    
    /** The DSpace context */
    private Context context;
    
    /** Database specific set of utils used when prepping the database */
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

    /** exact or partial matching of the value */
    private boolean valuePartial = false;
    
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
    
    // administrative attributes for this class
    
    /** a cache of the actual query to be executed */
    private String    querySql    = "";
    private ArrayList queryParams = new ArrayList();
    
    private String whereClauseOperator = "";
    
    /** whether the query (above) needs to be regenerated */
    private boolean rebuildQuery = true;

    // FIXME Would be better to join to item table and get the correct values
    /** flags for what the items represent */
    private boolean itemsInArchive = true;
    private boolean itemsWithdrawn = false;
    
    public BrowseDAOOracle(Context context)
    	throws BrowseException
    {
        this.context = context;
        
        // obtain the relevant Utils for this class
        utils = BrowseDAOFactory.getUtils(context);
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#doCountQuery()
     */
    public int doCountQuery() throws BrowseException
    {
        String   query  = getQuery();
        Object[] params = getQueryParams();
        
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "executing_count_query", "query=" + query));
        }

        TableRowIterator tri = null;
        
        try
        {
            // now run the query
            tri = DatabaseManager.query(context, query, params);
            
            if (tri.hasNext())
            {
                TableRow row = tri.next();
                return (int) row.getLongColumn("num");
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
            {
                tri.close();
            }
        }
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
            String query = "SELECT MAX(" + column + ") AS max_value FROM " + table + " WHERE item_id=?";
            
            Object[] params = { new Integer(itemID) };
            tri = DatabaseManager.query(context, query, params);
            
            TableRow row;
            if (tri.hasNext())
            {
                row = tri.next();
                return row.getStringColumn("max_value");
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
            {
                tri.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#doQuery()
     */
    public List doQuery() throws BrowseException
    {
        String query = getQuery();
        Object[] params = getQueryParams();
        
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "executing_full_query", "query=" + query));
        }
        
        TableRowIterator tri = null;
        try
        {
            // now run the query
            tri = DatabaseManager.query(context, query, params);
            
            // go over the query results and process
            List results = new ArrayList();
            ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
            while (tri.hasNext())
            {
                TableRow row = tri.next();
//                BrowseItem browseItem = new BrowseItem(context, row.getIntColumn("item_id"),
//                                                  itemsInArchive,
//                                                  itemsWithdrawn);
//                results.add(browseItem);
                results.add(itemDAO.retrieve(row.getIntColumn("item_id")));
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
            {
                tri.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#doValueQuery()
     */
    public List doValueQuery() throws BrowseException
    {
        String query = getQuery();
        Object[] params = getQueryParams();
        
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "executing_value_query", "query=" + query));
        }
        
        TableRowIterator tri = null;
        
        try
        {
            // now run the query
            tri = DatabaseManager.query(context, query, params);
            
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
            {
                tri.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getContainerID()
     */
    public int getContainerID()
    {
        return containerID;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getContainerIDField()
     */
    public String getContainerIDField()
    {
        return containerIDField;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getContainerTable()
     */
    public String getContainerTable()
    {
        return containerTable;
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
     * @see org.dspace.browse.BrowseDAO#getFocusField()
     */
    public String getJumpToField()
    {
        return focusField;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getFocusValue()
     */
    public String getJumpToValue()
    {
        return focusValue;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getLimit()
     */
    public int getLimit()
    {
        return limit;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getOffset()
     */
    public int getOffset()
    {
        return offset;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getOrderField()
     */
    public String getOrderField()
    {
        return orderField;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getSelectValues()
     */
    public String[] getSelectValues()
    {
        return selectValues;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getTable()
     */
    public String getTable()
    {
        return table;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getValue()
     */
    public String getFilterValue()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getValueField()
     */
    public String getFilterValueField()
    {
        return valueField;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#isAscending()
     */
    public boolean isAscending()
    {
        return ascending;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#isDistinct()
     */
    public boolean isDistinct()
    {
        return this.distinct;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#selectDistinctOn(java.lang.String[])
     */
    public void selectDistinctOn(String[] fields)
    {
        this.distinctValues = fields;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setAscending(boolean)
     */
    public void setAscending(boolean ascending)
    {
        this.ascending = ascending;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setContainerID(int)
     */
    public void setContainerID(int containerID)
    {
        this.containerID = containerID;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setContainerIDField(java.lang.String)
     */
    public void setContainerIDField(String containerIDField)
    {
        this.containerIDField = containerIDField;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setContainerTable(java.lang.String)
     */
    public void setContainerTable(String containerTable)
    {
        this.containerTable = containerTable;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setCountValues(java.lang.String[])
     */
    public void setCountValues(String[] fields)
    {
        this.countValues = fields;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setDistinct(boolean)
     */
    public void setDistinct(boolean bool)
    {
        this.distinct = bool;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setEqualsComparator(boolean)
     */
    public void setEqualsComparator(boolean equalsComparator)
    {
        this.equalsComparator = equalsComparator;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setFocusField(java.lang.String)
     */
    public void setJumpToField(String focusField)
    {
        this.focusField = focusField;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setFocusValue(java.lang.String)
     */
    public void setJumpToValue(String focusValue)
    {
        this.focusValue = focusValue;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setLimit(int)
     */
    public void setLimit(int limit)
    {
        this.limit = limit;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setOffset(int)
     */
    public void setOffset(int offset)
    {
        this.offset = offset;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setOrderField(java.lang.String)
     */
    public void setOrderField(String orderField)
    {
        this.orderField = orderField;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setSelectValues(java.lang.String[])
     */
    public void setSelectValues(String[] selectValues)
    {
        this.selectValues = selectValues;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setTable(java.lang.String)
     */
    public void setTable(String table)
    {
        this.table = table;

        // FIXME Rather than assume from the browse table, join the query to item to get the correct values
        // Check to see if this is the withdrawn browse index - if it is,
        // we need to set the flags appropriately for when we create the BrowseItems
        if (table.equals(BrowseIndex.getWithdrawnBrowseIndex().getTableName()))
        {
            itemsInArchive = false;
            itemsWithdrawn = true;
        }
        else
        {
            itemsInArchive = true;
            itemsWithdrawn = false;
        }

        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setValue(java.lang.String)
     */
    public void setFilterValue(String value)
    {
        this.value = value;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setFilterValuePartial(boolean)
     */
    public void setFilterValuePartial(boolean part)
    {
        this.valuePartial = part;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#setValueField(java.lang.String)
     */
    public void setFilterValueField(String valueField)
    {
        this.valueField = valueField;
        this.rebuildQuery = true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#useEqualsComparator()
     */
    public boolean useEqualsComparator()
    {
        return equalsComparator;
    }

    // PRIVATE METHODS

    /**
     * Build the query that will be used for a distinct select.  This incorporates
     * only the parts of the parameters that are actually useful for this type
     * of browse
     * 
     * @return      the query to be executed
     * @throws BrowseException
     */
    private String buildDistinctQuery(List params) throws BrowseException
    {
        StringBuffer queryBuf = new StringBuffer();
        
        if (!buildSelectListCount(queryBuf))
        {
            if (!buildSelectListValues(queryBuf))
            {
                throw new BrowseException("No arguments for SELECT statement");
            }
        }

        buildSelectStatement(queryBuf);
        buildWhereClauseOpReset();
        
        // assemble the focus clase if we are to have one
        // it will look like one of the following, for example
        //     sort_value <= myvalue
        //     sort_1 >= myvalue
        buildWhereClauseJumpTo(queryBuf, params);
        
        // assemble the where clause out of the two possible value clauses
        // and include container support
        buildWhereClauseDistinctConstraints(queryBuf, params);
        
        // assemble the order by field
        buildOrderBy(queryBuf);
        
        // prepare the LIMIT clause
        buildRowLimit(queryBuf, params);
        
        return queryBuf.toString();
    }

    /**
     * Build the query that will be used for a full browse.
     * 
     * @return      the query to be executed
     * @throws BrowseException
     */
    private String buildQuery(List params) throws BrowseException
    {
        StringBuffer queryBuf = new StringBuffer();
        
        if (!buildSelectListCount(queryBuf))
        {
            boolean hasSelectList = false;
            
            hasSelectList |= buildSelectListDistinctValues(queryBuf);
            hasSelectList |= buildSelectListValues(queryBuf);
            
            if (!hasSelectList)
            {
                throw new BrowseException("No arguments for SELECT statement");
            }
        }

        buildSelectStatement(queryBuf);
        buildWhereClauseOpReset();
        
        // assemble the focus clase if we are to have one
        // it will look like one of the following, for example
        //     sort_value <= myvalue
        //     sort_1 >= myvalue
        buildWhereClauseJumpTo(queryBuf, params);
        
        // assemble the value clause if we are to have one
        buildWhereClauseFilterValue(queryBuf, params);
        
        // assemble the where clause out of the two possible value clauses
        // and include container support
        buildWhereClauseFullConstraints(queryBuf, params);
        
        // assemble the order by field
        buildOrderBy(queryBuf);
        
        // prepare the LIMIT clause
        buildRowLimit(queryBuf, params);
        
        // prepare the OFFSET clause
        buildRowOffset(queryBuf, params);
        
        return queryBuf.toString();
    }
    
    /**
     * Get the clause to perform search result ordering.  This will
     * return something of the form:
     * 
     * <code>
     * ORDER BY [order field] (ASC | DESC)
     * </code>
     */
    private void buildOrderBy(StringBuffer queryBuf)
    {
        if (orderField != null)
        {
            queryBuf.append(" ORDER BY ");
            queryBuf.append(orderField);
            if (isAscending())
            {
                queryBuf.append(" ASC ");
            }
            else
            {
                queryBuf.append(" DESC ");
            }
        }
    }
    
    /**
     * Get the limit clause to perform search result truncation.  Will return
     * something of the form:
     * 
     * <code>
     * LIMIT [limit]
     * </code>
     */
    private void buildRowLimit(StringBuffer queryBuf, List params)
    {
        // prepare the LIMIT clause
        if (limit != -1)
        {
            queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");

            queryBuf.append(") rec WHERE rownum<=? ");
            
            params.add(new Integer(limit));
        }
    }
    
    /**
     * Get the offset clause to offset the start point of search results
     * 
     * @return
     * @deprecated
     */
    private void buildRowOffset(StringBuffer queryBuf, List params)
    {
        // prepare the OFFSET clause
        if (offset != -1)
        {
            if (limit == -1)
            {
                queryBuf.insert(0, "SELECT rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") rec");
            }

            queryBuf.insert(0, "SELECT * FROM (");
            queryBuf.append(") WHERE rnum>?");
            
            params.add(new Integer(offset));
        }
    }
    
    /**
     * Build a clause for counting results.  Will return something of the form:
     * 
     * <code>
     * COUNT( [value 1], [value 2] ) AS number
     * </code>
     * 
     * @return  the count clause
     */
    private boolean buildSelectListCount(StringBuffer queryBuf)
    {
        if (countValues != null && countValues.length > 0)
        {
            queryBuf.append(" COUNT(");
            queryBuf.append(countValues[0]);
            for (int i = 1; i < countValues.length; i++)
            {
                queryBuf.append(", ");
                queryBuf.append(countValues[i]);
            }

            queryBuf.append(") AS num");
            return true;
        }
        
        return false;
    }

    
    /**
     * Prepare the list of values to be selected on.  Will return something of the form:
     * 
     * <code>
     * [value 1], [value 2]
     * </code>
     * 
     * @return  the select value list
     */
    private boolean buildSelectListValues(StringBuffer queryBuf)
    {
        if (selectValues != null && selectValues.length > 0)
        {
            queryBuf.append(selectValues[0]);
            for (int i = 1; i < selectValues.length; i++)
            {
                queryBuf.append(", ");
                queryBuf.append(selectValues[i]);
            }
            
            return true;
        }
        
        return false;
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
    private boolean buildSelectListDistinctValues(StringBuffer queryBuf)
    {
        if (distinctValues != null && distinctValues.length > 0)
        {
            queryBuf.append(" DISTINCT(");
            for (int i = 1; i < distinctValues.length; i++)
            {
                queryBuf.append(", ");
                queryBuf.append(selectValues[i]);
            }
            queryBuf.append(") ");
            return true;
        }
        
        return false;
    }
    
    /**
     * Prepare the select clause using the pre-prepared arguments.  This will produce something
     * of the form:
     * 
     * <code>
     * SELECT [arguments] FROM [table]
     * </code>
     */
    private void buildSelectStatement(StringBuffer queryBuf) throws BrowseException
    {
        if (queryBuf.length() == 0)
            throw new BrowseException("No arguments for SELECT statement");
        
        if (table == null || "".equals(table))
            throw new BrowseException("No table for SELECT statement");

        // queryBuf already contains what we are selecting,
        // so insert the statement at the beginning
        queryBuf.insert(0, "SELECT ");

        // Then append the table
        queryBuf.append(" FROM ");
        queryBuf.append(table);
        queryBuf.append(" ");
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
     */
    private void buildWhereClauseDistinctConstraints(StringBuffer queryBuf, List params)
    {
        // add the constraint to community or collection if necessary
        // and desired
        if (containerIDField != null && containerID != -1 && containerTable != null)
        {
            buildWhereClauseOpInsert(queryBuf);
            
            queryBuf.append(" id IN (SELECT distinct_id FROM ");
            queryBuf.append(containerTable);
            queryBuf.append(" WHERE ");
            queryBuf.append(containerIDField);
            queryBuf.append("=?) ");
            
            params.add(new Integer(containerID));
        }
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
     */
    private void buildWhereClauseFullConstraints(StringBuffer queryBuf, List params)
    {
        // add the constraint to community or collection if necessary
        // and desired
        if (containerIDField != null && containerID != -1)
        {
            buildWhereClauseOpInsert(queryBuf);
            queryBuf.append(containerIDField);
            queryBuf.append("=? ");
            
            params.add(new Integer(containerID));
        }
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
     */
    private void buildWhereClauseJumpTo(StringBuffer queryBuf, List params)
    {
        // get the operator (<[=] | >[=]) which the focus of the browse will
        // be matched using
        String focusComparator = getFocusComparator();
        
        // assemble the focus clase if we are to have one
        // it will look like one of the following
        // - sort_value <= myvalue
        // - sort_1 >= myvalue
        if (focusField != null && focusValue != null)
        {
            buildWhereClauseOpInsert(queryBuf);
            queryBuf.append(" ");
            queryBuf.append(focusField);
            queryBuf.append(focusComparator);
            queryBuf.append("? ");

            if (focusField.startsWith("sort_"))
            {
                params.add(utils.truncateSortValue(focusValue));
            }
            else
            {
                params.add(utils.truncateValue(focusValue));
            }
        }
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
     */
    private void buildWhereClauseFilterValue(StringBuffer queryBuf, List params)
    {
        // assemble the value clause if we are to have one
        if (value != null && valueField != null)
        {
            buildWhereClauseOpInsert(queryBuf);
            queryBuf.append(" ");
            queryBuf.append(valueField);
            if (valuePartial)
            {
                queryBuf.append(" LIKE ? ");

                if (valueField.startsWith("sort_"))
                {
                    params.add("%" + utils.truncateSortValue(value) + "%");
                }
                else
                {
                    params.add("%" + utils.truncateValue(value) + "%");
                }
            }
            else
            {
                queryBuf.append("=? ");

                if (valueField.startsWith("sort_"))
                {
                    params.add(utils.truncateSortValue(value));
                }
                else
                {
                    params.add(utils.truncateValue(value));
                }
            }
        }
    }
    
    /**
     * Insert an operator into the where clause, and reset to ' AND '
     */
    private void buildWhereClauseOpInsert(StringBuffer queryBuf)
    {
        queryBuf.append(whereClauseOperator);
        whereClauseOperator = " AND ";
    }

    /**
     * Reset the where clause operator for initial use
     */
    private void buildWhereClauseOpReset()
    {
        // Use sneaky trick to insert the WHERE by defining it as the first operator
        whereClauseOperator = " WHERE ";
    }
    
    /**
     * Get the comparator which should be used to compare focus values
     * with values in the database.  This will return one of the 4 following
     * possible values: <, >, <=, >=
     * 
     * @return      the focus comparator
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
     * Return a string representation (the SQL) of the query that would be executed
     * using one of doCountQuery, doValueQuery, doMaxQuery or doQuery
     * 
     * @return  String representation of the query (SQL)
     * @throws BrowseException
     */
    private String getQuery() throws BrowseException
    {
        if ("".equals(querySql) || rebuildQuery)
        {
            queryParams.clear();
            if (this.isDistinct())
            {
                querySql = buildDistinctQuery(queryParams);
            }
            else
            {
                querySql = buildQuery(queryParams);
            }

            this.rebuildQuery = false;
        }
        return querySql;
    }
    
    /**
     * Return the parameters to be bound to the query
     * 
     * @return  Object[] query parameters
     * @throws BrowseException
     */
    private Object[] getQueryParams() throws BrowseException
    {
        // Ensure that the query has been built
        if ("".equals(querySql) || rebuildQuery)
        {
            getQuery();
        }
        
        return queryParams.toArray();
    }
}
