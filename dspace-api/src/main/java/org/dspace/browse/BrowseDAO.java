/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.content.Item;

import java.util.List;
import java.util.UUID;

/**
 * Interface for any class wishing to interact with the Browse storage layer for
 * Read Only operations.  If you wish to modify the contents of the browse indices
 * or create and destroy index tables you should look at implementations for
 * BrowseCreateDAO.
 *
 * If you implement this class, and you wish it to be loaded via the BrowseDAOFactory
 * you must supply a constructor of the form:
 *
 * public BrowseDAOImpl(Context context) {}
 *
 * Where Context is the DSpace Context object
 *
 * Where tables are referred to in this class, they can be obtained from the BrowseIndex
 * class, which will answer queries given the context of the request on which table
 * is the relevant target.
 *
 * @author Richard Jones
 *
 */
public interface BrowseDAO
{
    // Objects implementing this interface should also include
    // a constructor which takes the DSpace Context as an argument
    //
    // public BrowseDAOImpl(Context context) ...

    /**
     * This executes a query which will count the number of results for the
     * parameters you set.
     *
     * @return      the integer value of the number of results found
     * @throws BrowseException if browse error
     */
    public int doCountQuery() throws BrowseException;

    /**
     * This executes a query which returns a List object containing String
     * values which represent the results of a single value browse (for
     * example, the list of all subject headings).  This is most
     * commonly used with a Distinct browse type.
     *
     * @return  List of Strings representing the single value query results
     * @throws BrowseException if browse error
     */
    public List<String[]> doValueQuery() throws BrowseException;

    /**
     * This executes a query which returns a List object containing BrowseItem objects
     * representing the results of a full item browse.
     *
     * @return  List of BrowseItem objects
     * @throws BrowseException if browse error
     */
    public List<Item> doQuery() throws BrowseException;

    /**
     * This executes a query which returns the value of the "highest" (max) value
     * in the given table's column for the given item id.
     *
     * @param column    the column to interrogate
     * @param table     the table to query
     * @param itemID    the item id
     * @return          String representing the max value in the given column
     * @throws BrowseException if browse error
     */
    public String doMaxQuery(String column, String table, int itemID) throws BrowseException;

    /**
     * This executes a query which returns the offset where the value (or nearest greater
     * equivalent) can be found in the specified table ordered by the column.
     *
     * @param column    the column to interrogate
     * @param value     the item id
     * @param isAscending browsing in ascending or descending order
     * @return          the offset into the table
     * @throws BrowseException if browse error
     */
    public int doOffsetQuery(String column, String value, boolean isAscending) throws BrowseException;

    /**
     * This executes a query which returns the offset where the value (or nearest greater
     * equivalent) can be found in the specified table ordered by the column.
     *
     * @param column    the column to interrogate
     * @param value     the item id
     * @param isAscending browsing in ascending or descending order
     * @return          the offset into the table
     * @throws BrowseException if browse error
     */
    public int doDistinctOffsetQuery(String column, String value, boolean isAscending) throws BrowseException;

    /**
     * Does the query use the equals comparator when doing less than or greater than
     * comparisons.  @see setEqualsComparator
     *
     * Default value is true
     *
     * @return  true if using it, false if not
     */
    public boolean useEqualsComparator();

    /**
     * Set whether the query should use an equals comparator when doing less than or
     * greater than comparisons.  That is, if true then comparisons will be made
     * using the equivalent of {@code <=} and {@code >=}, while if false it will use the
     * equivalent of {@code <} and {@code >}
     *
     * @param equalsComparator  true to use, false to not.
     */
    public void setEqualsComparator(boolean equalsComparator);

    /**
     * Is the sort order ascending or descending?
     *
     * Default value is true
     *
     * @return  true for ascending, false for descending
     */
    public boolean isAscending();

    /**
     * Set whether the results should be sorted in ascending order (on the given sort column)
     * or descending order.
     *
     * @param ascending     true to ascend, false to descend
     */
    public void setAscending(boolean ascending);

    /**
     * Get the database ID of the container object.  The container object will be a
     * Community or a Collection.
     *
     * @return  the database id of the container, or -1 if none is set
     */
    public UUID getContainerID();

    /**
     * Set the database id of the container object.  This should be the id of a
     * Community or Collection.  This will constrain the results of the browse
     * to only items or values within items that appear in the given container.
     *
     * @param containerID
     *     community/collection internal ID (UUID)
     */
    public void setContainerID(UUID containerID);

    /**
     * get the name of the field in which to look for the container id.  This is
     * principally for use internal to the DAO.
     *
     * @return  the name of the container id field.  For example "collection_id" or
     *          "community_id"
     */
    public String getContainerIDField();

    /**
     * set the name of the field in which to look for the container id.
     *
     * @param containerIDField  the name of the container id field.
     *          For example "collection_id" or "community_id"
     */
    public void setContainerIDField(String containerIDField);

    /**
     * Get the field in which we will match a focus value from which to start
     * the browse.  This will either be the "sort_value" field or one of the
     * additional sort fields defined by configuration
     *
     * @return  the name of the focus field
     */
    public String getJumpToField();

    /**
     * Set the focus field upon which we will match a value from which to start
     * the browse.  This will either be the "sort_value" field or one of the
     * additional sort fields defined by configuration
     *
     * @param focusField     the name of the focus field
     */
    public void setJumpToField(String focusField);

    /**
     * Get the value at which the browse will start.  The value supplied here will
     * be the top result on the page of results.
     *
     * @return      the value to start browsing on
     */
    public String getJumpToValue();

    /**
     * Set the value upon which to start the browse from.  The value supplied here
     * will be the top result on the page of results
     *
     * @param focusValue    the value in the focus field on which to start browsing
     */
    public void setJumpToValue(String focusValue);

    /**
     * get the integer number which is the limit of the results that will be returned
     * by any query.  The default is -1, which means unlimited results.
     *
     * @return  the maximum possible number of results allowed to be returned
     */
    public int getLimit();

    /**
     * Set the limit for how many results should be returned.  This is generally
     * for use in paging or limiting the number of items be be displayed.  The default
     * is -1, meaning unlimited results.  Note that if the number of results of the
     * query is less than this number, the size of the result set will be smaller
     * than this limit.
     *
     * @param limit     the maximum number of results to return.
     */
    public void setLimit(int limit);

    /**
     * Get the offset from the first result from which to return results.  This
     * functionality is present for backwards compatibility, but is ill advised.  All
     * normal browse operations can be completed without it.  The default is -1, which
     * means do not offset.
     *
     * @return      paging offset
     */
    public int getOffset();

    /**
     * Get the offset from the first result from which to return results.  This
     * functionality is present for backwards compatibility, but is ill advised.  All
     * normal browse operations can be completed without it.  The default is -1, which
     * means do not offset.
     *
     * @param offset
     *     paging offset
     */
    public void setOffset(int offset);

    /**
     * Get the database field which will be used to do the sorting of result sets on.
     *
     * @return      the field by which results will be sorted
     */
    public String getOrderField();

    /**
     * Set the database field which will be used to sort result sets on
     *
     * @param orderField    the field by which results will be sorted
     */
    public void setOrderField(String orderField);

    /**
     * Get the array of values that we will be selecting on.  The default is
     * to select all of the values from a given table
     *
     * @return  an array of values to select on
     */
    public String[] getSelectValues();

    /**
     * Set the array of values to select on.  This should be a list of the columns
     * available in the target table, or the SQL wildcards.  The default is
     * single element array with the standard wildcard (*)
     *
     * @param selectValues  the values to select on
     */
    public void setSelectValues(String[] selectValues);

    /**
     * Get the array of fields that we will be counting on.
     *
     * @return  an array of fields to be counted over
     */
    public String[] getCountValues();

    /**
     * Set the array of columns that we will be counting over.  In general, the
     * wildcard (*) will suffice
     *
     * @param fields    an array of fields to be counted over
     */
    public void setCountValues(String[] fields);

    /**
     * get the name of the table that we are querying
     *
     * @return  the name of the table
     */
    public String getTable();

    /**
     * Set the name of the table to query
     *
     * @param table     the name of the table
     */
    public void setTable(String table);

    /**
     * Set the name of the mapping tables to use for filtering
     * @param tableDis    the name of the table holding the distinct values
     * @param tableMap    the name of the table holding the mappings
     */
    public void setFilterMappingTables(String tableDis, String tableMap);

    /**
     * Get the value which we are constraining all our browse results to contain.
     *
     * @return  the value to which to constrain results
     */
    public String getFilterValue();

    /**
     * Set the value to which all our browse results should be constrained.  For
     * example, if you are listing all of the publications by a single author
     * your value would be the author name.
     *
     * @param value the value to which to constrain results
     */
    public void setFilterValue(String value);

    /**
     * Sets whether we will treat the filter value as partial (like match), or exact
     *
     * @param part true if partial, false if exact
     */
    public void setFilterValuePartial(boolean part);

    /**
     * Get the name of the field in which the value to constrain results is
     * contained
     *
     * @return  the name of the field
     */
    public String getFilterValueField();

    /**
     * Set he name of the field in which the value to constrain results is
     * contained
     *
     * @param valueField    the name of the field
     */
    public void setFilterValueField(String valueField);

    /**
     * Set whether this is a distinct value browse or not
     *
     * @param bool  true if distinct value, false if not
     */
    public void setDistinct(boolean bool);

    /**
     * Is this a distinct value browse?
     *
     * @return  true if distinct, false if not
     */
    public boolean isDistinct();

    /**
     * If we have specified a container id and container field, we must also specify
     * a container table.  This is the name of the table that maps the item onto
     * the distinct value.  Since we are in a container, this value will actually be
     * the view which allows us to select only items which are within a given container
     *
     * @param containerTable    the name of the container table mapping
     */
    public void setContainerTable(String containerTable);

    /**
     * Get the name of the container table that is being used to map items to distinct
     * values when in a container constrained browse
     *
     * @return  the name of the table
     */
    public String getContainerTable();

    public void setAuthorityValue(String value);

    public String getAuthorityValue();
    
    public boolean isEnableBrowseFrequencies();

	public void setEnableBrowseFrequencies(boolean enableBrowseFrequencies);
}
