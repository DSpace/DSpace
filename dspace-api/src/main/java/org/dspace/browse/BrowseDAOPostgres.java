/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
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
 * @author Graham Triggs
 */
public class BrowseDAOPostgres implements BrowseDAO
{
    /** Log4j log */
    private static Logger log = Logger.getLogger(BrowseDAOPostgres.class);

    /** The DSpace context */
    private Context context;

    /** Database specific set of utils used when prepping the database */
    private BrowseDAOUtils utils;

    // SQL query related attributes for this class

    /** the values to place in the SELECT --- FROM bit */
    private String[] selectValues = { "*" };

    /** the values to place in the SELECT COUNT(---) bit */
    private String[] countValues;

    /** table(s) to select from */
    private String table = null;
    private String tableDis = null;
    private String tableMap = null;

    /** field to look for focus value in */
    private String focusField = null;

    /** value to start browse from in focus field */
    private String focusValue = null;

    /** field to look for value in */
    private String valueField = null;

    /** value to restrict browse to (e.g. author name) */
    private String value = null;

    private String authority = null;

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

    /** the offset of the start point */
    private int offset = 0;

    /** whether to use the equals comparator in value comparisons */
    private boolean equalsComparator = true;

    /** whether this is a distinct browse or not */
    private boolean distinct = false;

    // administrative attributes for this class

    /** a cache of the actual query to be executed */
    private String querySql    = "";
    private List<Serializable> queryParams = new ArrayList<Serializable>();

    /** whether the query (above) needs to be regenerated */
    private boolean rebuildQuery = true;

    private String whereClauseOperator = "";

    // FIXME Would be better to join to item table and get the correct values
    /** flags for what the items represent */
    private boolean itemsInArchive = true;
    private boolean itemsWithdrawn = false;
    private boolean itemsDiscoverable = true;

    private boolean enableBrowseFrequencies = true;
    
    /**
     * Required constructor for use by BrowseDAOFactory
     *
     * @param context   DSpace context
     */
    public BrowseDAOPostgres(Context context)
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
        String query    = getQuery();
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
            String query = "SELECT max(" + column + ") FROM " + table + " WHERE item_id = ?";

            Object[] params = { Integer.valueOf(itemID) };
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
            {
                tri.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#doOffsetQuery(java.lang.String, java.lang.String, java.lang.String)
     */
    public int doOffsetQuery(String column, String value, boolean isAscending)
            throws BrowseException
    {
        TableRowIterator tri = null;

        if (column == null || value == null)
        {
            return 0;
        }

        try
        {
            List<Serializable> paramsList = new ArrayList<Serializable>();
            StringBuffer queryBuf = new StringBuffer();

            queryBuf.append("COUNT(").append(column).append(") AS offset ");

            buildSelectStatement(queryBuf, paramsList);
            if (isAscending)
            {
                queryBuf.append(" WHERE ").append(column).append("<?");
                paramsList.add(value);
            }
            else
            {
                queryBuf.append(" WHERE ").append(column).append(">?");
                paramsList.add(value + Character.MAX_VALUE);
            }

            if (containerTable != null || (value != null && valueField != null && tableDis != null && tableMap != null))
            {
                queryBuf.append(" AND ").append("mappings.item_id=");
                queryBuf.append(table).append(".item_id");
            }

            tri = DatabaseManager.query(context, queryBuf.toString(), paramsList.toArray());

            TableRow row;
            if (tri.hasNext())
            {
                row = tri.next();
                return (int)row.getLongColumn("offset");
            }
            else
            {
                return 0;
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
     * @see org.dspace.browse.BrowseDAO#doDistinctOffsetQuery(java.lang.String, java.lang.String, java.lang.String)
     */
    public int doDistinctOffsetQuery(String column, String value, boolean isAscending)
            throws BrowseException
    {
        TableRowIterator tri = null;

        try
        {
            List<Serializable> paramsList = new ArrayList<Serializable>();
            StringBuffer queryBuf = new StringBuffer();

            queryBuf.append("COUNT(").append(column).append(") AS offset ");

            buildSelectStatementDistinct(queryBuf, paramsList);
            if (isAscending)
            {
                queryBuf.append(" WHERE ").append(column).append("<?");
                paramsList.add(value);
            }
            else
            {
                queryBuf.append(" WHERE ").append(column).append(">?");
                paramsList.add(value + Character.MAX_VALUE);
            }

            if (containerTable != null && tableMap != null)
            {
                queryBuf.append(" AND ").append("mappings.distinct_id=");
                queryBuf.append(table).append(".id");
            }

            tri = DatabaseManager.query(context, queryBuf.toString(), paramsList.toArray());

            TableRow row;
            if (tri.hasNext())
            {
                row = tri.next();
                return (int)row.getLongColumn("offset");
            }
            else
            {
                return 0;
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
    public List<BrowseItem> doQuery()
        throws BrowseException
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
            List<BrowseItem> results = new ArrayList<BrowseItem>();
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                BrowseItem browseItem = new BrowseItem(context, row.getIntColumn("item_id"),
                                                  itemsInArchive,
                                                  itemsWithdrawn,
                                                  itemsDiscoverable);
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
            {
                tri.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#doValueQuery()
     */
    public List<String[]> doValueQuery()
        throws BrowseException
    {
        String query = getQuery();
        
        Object[] params = getQueryParams();
        log.debug(LogManager.getHeader(context, "executing_value_query", "query=" + query));

        TableRowIterator tri = null;

        try
        {
            // now run the query
            tri = DatabaseManager.query(context, query, params);

            // go over the query results and process
            List<String[]> results = new ArrayList<String[]>();
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                String valueResult = row.getStringColumn("value");
                String authorityResult = row.getStringColumn("authority");
                if (enableBrowseFrequencies){
                    long frequency = row.getLongColumn("num");
                    results.add(new String[]{valueResult,authorityResult, String.valueOf(frequency)});
                }
                else
                    results.add(new String[]{valueResult,authorityResult, ""});
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
        return (String[]) ArrayUtils.clone(this.countValues);
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
        return (String[]) ArrayUtils.clone(selectValues);
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
        this.countValues = (String[]) ArrayUtils.clone(fields);
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
        this.selectValues = (String[]) ArrayUtils.clone(selectValues);
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
            itemsDiscoverable = true;
        }
        else if (table.equals(BrowseIndex.getPrivateBrowseIndex().getTableName()))
        {
        	itemsInArchive = true;
            itemsWithdrawn = false;
        	itemsDiscoverable = false;
        }
        else 
        {
            itemsInArchive = true;
            itemsWithdrawn = false;
            itemsDiscoverable = true;
        }


        this.rebuildQuery = true;
    }

    public void setFilterMappingTables(String tableDis, String tableMap)
    {
        this.tableDis = tableDis;
        this.tableMap = tableMap;

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
    private String buildDistinctQuery(List<Serializable> params)
        throws BrowseException
    {
        StringBuffer queryBuf = new StringBuffer();

        if (!buildSelectListCount(queryBuf))
        {
            if (!buildSelectListValues(queryBuf))
            {
                throw new BrowseException("No arguments for SELECT statement");
            }
        }

        buildSelectStatementDistinct(queryBuf, params);
        buildWhereClauseOpReset();

        // assemble the focus clause if we are to have one
        // it will look like one of the following, for example
        //     sort_value <= myvalue
        //     sort_1 >= myvalue
        buildWhereClauseJumpTo(queryBuf, params);

        // assemble the where clause out of the two possible value clauses
        // and include container support
        buildWhereClauseDistinctConstraints(queryBuf, params);

        // assemble the order by field
        buildOrderBy(queryBuf);

        // prepare the limit and offset clauses
        buildRowLimitAndOffset(queryBuf, params);

        //If we want frequencies and this is not a count query, enchance the query accordingly
        if (isEnableBrowseFrequencies() && countValues==null){
            String before = "SELECT count(*) AS num, dvalues.value, dvalues.authority FROM (";
            String after = ") dvalues , "+tableMap+" WHERE dvalues.id = "+tableMap+".distinct_id GROUP BY "+tableMap+
                    ".distinct_id, dvalues.value, dvalues.authority, dvalues.sort_value";

            queryBuf.insert(0, before);
            queryBuf.append(after);
            buildOrderBy(queryBuf);
        }
        
        return queryBuf.toString();
    }

    /**
     * Build the query that will be used for a full browse.
     *
     * @return      the query to be executed
     * @throws BrowseException
     */
    private String buildQuery(List<Serializable> params)
        throws BrowseException
    {
        StringBuffer queryBuf = new StringBuffer();

        if (!buildSelectListCount(queryBuf))
        {
            if (!buildSelectListValues(queryBuf))
            {
                throw new BrowseException("No arguments for SELECT statement");
            }
        }

        buildSelectStatement(queryBuf, params);
        buildWhereClauseOpReset();

        // assemble the focus clause if we are to have one
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

        // prepare the limit and offset clauses
        buildRowLimitAndOffset(queryBuf, params);

        return queryBuf.toString();
    }

    /**
     * Get the clause to perform search result ordering.  This will
     * return something of the form:
     *
     * <code>
     * ORDER BY [order field] (ASC | DESC)
     * </code>
     *
     * @return  the ORDER BY clause
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
            queryBuf.append(" NULLS LAST ");
        }
    }

    /**
     * Get the limit clause to perform search result truncation.  Will return
     * something of the form:
     *
     * <code>
     * LIMIT [limit]
     * </code>
     *
     * @return  the limit clause
     */
    private void buildRowLimitAndOffset(StringBuffer queryBuf, List<Serializable> params)
    {
        // prepare the LIMIT clause
        if (limit > 0)
        {
            queryBuf.append(" LIMIT ? ");

            params.add(Integer.valueOf(limit));
        }

        // prepare the OFFSET clause
        if (offset > 0)
        {
            queryBuf.append(" OFFSET ? ");

            params.add(Integer.valueOf(offset));
        }
    }

    /**
     * Build the clauses required for the view used in focused or scoped queries.
     *
     * @param queryBuf
     * @param params
     */
    private void buildFocusedSelectClauses(StringBuffer queryBuf, List<Serializable> params)
    {
        if (tableMap != null && tableDis != null)
        {
            queryBuf.append(tableMap).append(".distinct_id=").append(tableDis).append(".id");
            queryBuf.append(" AND ");
            if (authority == null)
            {
                queryBuf.append(tableDis).append(".authority IS NULL");
            queryBuf.append(" AND ");
                queryBuf.append(tableDis).append(".").append(valueField);

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
            else
            {
                queryBuf.append(tableDis).append(".authority=?");
                params.add(utils.truncateValue(authority,100));
            }
        }

        if (containerTable != null && containerIDField != null && containerID != -1)
        {
            if (tableMap != null)
            {
                if (tableDis != null)
                {
                    queryBuf.append(" AND ");
                }

                queryBuf.append(tableMap).append(".item_id=")
                        .append(containerTable).append(".item_id AND ");
            }

            queryBuf.append(containerTable).append(".").append(containerIDField);
            queryBuf.append("=? ");

            params.add(Integer.valueOf(containerID));
        }
    }

    /**
     * Build the table list for the view used in focused or scoped queries.
     *
     * @param queryBuf
     */
    private void buildFocusedSelectTables(StringBuffer queryBuf)
    {
        if (containerTable != null)
        {
            queryBuf.append(containerTable);
        }

        if (tableMap != null)
        {
            if (containerTable != null)
            {
                queryBuf.append(", ");
            }

            queryBuf.append(tableMap);

            if (tableDis != null)
            {
                queryBuf.append(", ").append(tableDis);
            }
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
            if ("*".equals(countValues[0]))
            {
                queryBuf.append(countValues[0]);
            }
            else
            {
                queryBuf.append(table).append(".").append(countValues[0]);
            }

            for (int i = 1; i < countValues.length; i++)
            {
                queryBuf.append(", ");
                if ("*".equals(countValues[i]))
                {
                    queryBuf.append(countValues[i]);
                }
                else
                {
                    queryBuf.append(table).append(".").append(countValues[i]);
                }
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
            queryBuf.append(table).append(".").append(selectValues[0]);
            for (int i = 1; i < selectValues.length; i++)
            {
                queryBuf.append(", ");
                queryBuf.append(table).append(".").append(selectValues[i]);
            }

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
     *
     * @param queryBuf  the string value obtained from distinctClause, countClause or selectValues
     * @return  the SELECT part of the query
     */
    private void buildSelectStatement(StringBuffer queryBuf, List<Serializable> params) throws BrowseException
    {
        if (queryBuf.length() == 0)
        {
            throw new BrowseException("No arguments for SELECT statement");
        }

        if (table == null || "".equals(table))
        {
            throw new BrowseException("No table for SELECT statement");
        }

        // queryBuf already contains what we are selecting,
        // so insert the statement at the beginning
        queryBuf.insert(0, "SELECT ");

        // Then append the table
        queryBuf.append(" FROM ");
        queryBuf.append(table);
        if (containerTable != null || (value != null && valueField != null && tableDis != null && tableMap != null))
        {
            queryBuf.append(", (SELECT ");
            if (containerTable != null)
            {
                queryBuf.append(containerTable).append(".item_id");
            }
            else
            {
                queryBuf.append("DISTINCT ").append(tableMap).append(".item_id");
            }
            queryBuf.append(" FROM ");
            buildFocusedSelectTables(queryBuf);
            queryBuf.append(" WHERE ");
            buildFocusedSelectClauses(queryBuf, params);
            queryBuf.append(") mappings");
        }
        queryBuf.append(" ");
    }

    /**
     * Prepare the select clause using the pre-prepared arguments.  This will produce something
     * of the form:
     *
     * <code>
     * SELECT [arguments] FROM [table]
     * </code>
     *
     * @param queryBuf  the string value obtained from distinctClause, countClause or selectValues
     * @return  the SELECT part of the query
     */
    private void buildSelectStatementDistinct(StringBuffer queryBuf, List<Serializable> params) throws BrowseException
    {
        if (queryBuf.length() == 0)
        {
            throw new BrowseException("No arguments for SELECT statement");
        }

        if (table == null || "".equals(table))
        {
            throw new BrowseException("No table for SELECT statement");
        }

        // queryBuf already contains what we are selecting,
        // so insert the statement at the beginning
        queryBuf.insert(0, "SELECT ");

        // Then append the table
        queryBuf.append(" FROM ");
        queryBuf.append(table);
        if (containerTable != null && tableMap != null)
        {
            queryBuf.append(", (SELECT DISTINCT ").append(tableMap).append(".distinct_id ");
            queryBuf.append(" FROM ");
            buildFocusedSelectTables(queryBuf);
            queryBuf.append(" WHERE ");
            buildFocusedSelectClauses(queryBuf, params);
            queryBuf.append(") mappings");
        }
        queryBuf.append(" ");
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
     * @return  the sub-query
     */
    private void buildWhereClauseDistinctConstraints(StringBuffer queryBuf, List<Serializable> params)
    {
        // add the constraint to community or collection if necessary
        // and desired
        if (containerIDField != null && containerID != -1 && containerTable != null)
        {
            buildWhereClauseOpInsert(queryBuf);
            queryBuf.append(" ").append(table).append(".id=mappings.distinct_id ");
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
     *
     * @return  the focus clause
     */
    private void buildWhereClauseJumpTo(StringBuffer queryBuf, List<Serializable> params)
    {
        // get the operator (<[=] | >[=]) which the focus of the browse will
        // be matched using
        String focusComparator = getFocusComparator();

        // assemble the focus clause if we are to have one
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
     * Get a clause to obtain the ids for a full browse within a given
     * constraint.  This will produce something of the form:
     *
     * <code>
     * [container field] = [container id]
     * </code>
     *
     * This is for use inside the overall WHERE clause only
     *
     * @return  the constraint clause
     */
    private void buildWhereClauseFullConstraints(StringBuffer queryBuf, List<Serializable> params)
    {
        // add the constraint to community or collection if necessary
        // and desired
        if (tableDis == null || tableMap == null)
        {
            if (containerIDField != null && containerID != -1)
            {
                buildWhereClauseOpInsert(queryBuf);
                queryBuf.append(" ").append(table).append(".item_id=mappings.item_id ");
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
     *
     * @return  the value clause
     */
    private void buildWhereClauseFilterValue(StringBuffer queryBuf, List<Serializable> params)
    {
        // assemble the value clause if we are to have one
        if (value != null && valueField != null)
        {
            buildWhereClauseOpInsert(queryBuf);
            queryBuf.append(" ");
            if (tableDis != null && tableMap != null)
            {
                queryBuf.append(table).append(".item_id=mappings.item_id ");
            }
            else
            {
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

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseDAO#getQuery()
     */
    private String getQuery()
        throws BrowseException
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

    public void setAuthorityValue(String value) {
        authority = value;
    }

    public String getAuthorityValue() {
        return authority;
    }
    
    public boolean isEnableBrowseFrequencies() {
        return enableBrowseFrequencies;
    }

    public void setEnableBrowseFrequencies(boolean enableBrowseFrequencies) {
        this.enableBrowseFrequencies = enableBrowseFrequencies;
    }
}