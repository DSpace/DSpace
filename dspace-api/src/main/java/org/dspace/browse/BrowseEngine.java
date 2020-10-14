/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.sort.OrderFormat;
import org.dspace.sort.SortOption;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    private static final Logger log = Logger.getLogger(BrowseEngine.class);

    /** the browse scope which is the basis for our browse */
    private BrowserScope scope;

    /** the DSpace context */
    private final Context context;

    /** The Data Access Object for the browse tables */
    private final BrowseDAO dao;

    /** The Browse Index associated with the Browse Scope */
    private BrowseIndex browseIndex;

    /**
     * Create a new instance of the Browse engine, using the given DSpace
     * Context object.  This will automatically assign a Data Access Object
     * for the Browse Engine, based on the brand of the provided DBMS.
     *
     * @param context       the DSpace context
     * @throws BrowseException if browse error
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
     * @param bs    the scope of the browse
     * @return      the results of the browse
     * @throws BrowseException if browse error
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
     * @param bs    the scope of the browse
     * @return      the results of the browse
     * @throws BrowseException if browse error
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

        // define a clause for the WHERE clause which will allow us to constrain
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

        dao.setOffset(scope.getOffset());
        dao.setLimit(scope.getResultsPerPage());

        // assemble the ORDER BY clause
        String orderBy = browseIndex.getSortField(scope.isSecondLevel());
        if (scope.getSortBy() > 0)
        {
            orderBy = "sort_" + Integer.toString(scope.getSortBy());
        }
        dao.setOrderField(orderBy);

        // now run the query
        List<Item> results = dao.doQuery();

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
     * @param bs        the scope of the browse
     * @return          the results of the browse
     * @throws BrowseException if browse error
     */
    private BrowseInfo browseByItem(BrowserScope bs)
        throws BrowseException
    {
        log.info(LogManager.getHeader(context, "browse_by_item", ""));
        try
        {
            // get the table name that we are going to be getting our data from
            dao.setTable(browseIndex.getTableName());

            // tell the browse query whether we are ascending or descending on the value
            dao.setAscending(scope.isAscending());

            // assemble the value clause
            String rawValue = null;
            if (scope.hasFilterValue() && scope.isSecondLevel())
            {
                String value = scope.getFilterValue();
                rawValue = value;

                // make sure the incoming value is normalised
                value = OrderFormat.makeSortString(value, scope.getFilterValueLang(),
                            scope.getBrowseIndex().getDataType());

                dao.setAuthorityValue(scope.getAuthorityValue());

                // set the values in the Browse Query
                if (scope.isSecondLevel())
                {
                    dao.setFilterValueField("value");
                    dao.setFilterValue(rawValue);    
                }
                else
                {
	                dao.setFilterValueField("sort_value");
	                dao.setFilterValue(value);
                }
                dao.setFilterValuePartial(scope.getFilterValuePartial());

                // to apply the filtering, we need the distinct and map tables for the index
                dao.setFilterMappingTables(browseIndex.getDistinctTableName(),
                                           browseIndex.getMapTableName());
            }

            // define a clause for the WHERE clause which will allow us to constrain
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

            // this is the total number of results in answer to the query
            int total = getTotalResults();

            // assemble the ORDER BY clause
            String orderBy = browseIndex.getSortField(scope.isSecondLevel());
            if (scope.getSortBy() > 0)
            {
                orderBy = "sort_" + Integer.toString(scope.getSortBy());
            }
            dao.setOrderField(orderBy);

            int offset = scope.getOffset();
            String rawFocusValue = null;
            if (offset < 1 && (scope.hasJumpToItem() || scope.hasJumpToValue() || scope.hasStartsWith()))
            {
                // We need to convert these to an offset for the actual browse query.
                // First, get a value that we can look up in the ordering field
                rawFocusValue = getJumpToValue();

                // make sure the incoming value is normalised
                String focusValue = normalizeJumpToValue(rawFocusValue);

                log.debug("browsing using focus: " + focusValue);

                // Convert the focus value into an offset
                offset = getOffsetForValue(focusValue);
            }

            dao.setOffset(offset);

            // assemble the LIMIT clause
            dao.setLimit(scope.getResultsPerPage());

            // Holder for the results
            List<Item> results = null;

            // Does this browse have any contents?
            if (total > 0)
            {
                // now run the query
                results = dao.doQuery();

                // now, if we don't have any results, we are at the end of the browse.  This will
                // be because a starts_with value has been supplied for which we don't have
                // any items.
                if (results.size() == 0)
                {
                    // In this case, we will calculate a new offset for the last page of results
                    offset = total - scope.getResultsPerPage();
                    if (offset < 0)
                    {
                        offset = 0;
                    }

                    // And rerun the query
                    dao.setOffset(offset);
                    results = dao.doQuery();
                }
            }
            else
            {
                // No records, so make an empty list
                results = new ArrayList<>();
            }

            // construct the BrowseInfo object to pass back
//            BrowseInfo browseInfo = new BrowseInfo(results, position, total, offset);
            BrowseInfo browseInfo = new BrowseInfo(results, offset, total, offset);

            if (offset + scope.getResultsPerPage() < total)
            {
                browseInfo.setNextOffset(offset + scope.getResultsPerPage());
            }

            if (offset - scope.getResultsPerPage() > -1)
            {
                browseInfo.setPrevOffset(offset - scope.getResultsPerPage());
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

            // set the browse authority key if there is one
            browseInfo.setAuthority(scope.getAuthorityValue());

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
     * @param bs        the scope of the browse
     * @return          the results of the browse
     * @throws BrowseException if browse error
     */
    private BrowseInfo browseByValue(BrowserScope bs)
        throws BrowseException
    {
        log.info(LogManager.getHeader(context, "browse_by_value", "focus=" + bs.getJumpToValue()));

        try
        {
            // get the table name that we are going to be getting our data from
            // this is the distinct table constrained to either community or collection
            dao.setTable(browseIndex.getDistinctTableName());
            dao.setStartsWith("0".equals(scope.getStartsWith()) && !scope.getOrder().equals("ASC") ? "9" : StringUtils.lowerCase(scope.getStartsWith()));
            // remind the DAO that this is a distinct value browse, so it knows what sort
            // of query to build
            dao.setDistinct(true);

            // tell the browse query whether we are ascending or descending on the value
            dao.setAscending(scope.isAscending());

            // inform dao about the display frequencies flag
            dao.setEnableBrowseFrequencies(browseIndex.isDisplayFrequencies());
            
            // if we want to display frequencies, we need to pass the map table
            if (browseIndex.isDisplayFrequencies()){
            	dao.setFilterMappingTables(null, browseIndex.getMapTableName());
            }
            
            // set our constraints on community or collection
            if (scope.inCollection() || scope.inCommunity())
            {
            	// Scoped browsing of distinct metadata requires the mapping
                // table to be specified.
            	if (!browseIndex.isDisplayFrequencies())
            		dao.setFilterMappingTables(null, browseIndex.getMapTableName());
                
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

            // this is the total number of results in answer to the query
            int total = getTotalResults(true);

            // set the ordering field (there is only one option)
            dao.setOrderField("sort_value");

            // assemble the focus clause if we are to have one
            // it will look like one of the following
            // - sort_value < myvalue
            // = sort_1 > myvalue
            dao.setJumpToField("sort_value");
            int offset = scope.getOffset();
            int limit = scope.getResultsPerPage();
            List<String[]> results = null;

            String rawFocusValue = null;
            if (offset < 1 && scope.hasJumpToValue() || scope.hasStartsWith()) {
                // store the value to tell the Browse Info object which value we are browsing on
                rawFocusValue = getJumpToValue();
            }
            if ("0".equals(scope.getStartsWith())) {
                int currentSW = scope.getOrder().equals("ASC") ? 0 : 9;
                results = new ArrayList<String[]>();
                // While we haven't reached results or the end
                while (results.size() < scope.getResultsPerPage() && currentSW <= 9 && currentSW >= 0) {
                    List<String[]> thisNumResults = dao.doValueQuery();
                    // If set contains our results
                    if (offset < thisNumResults.size()) {
                        // If we have found the rest, add and exit loop
                        if (offset + limit < thisNumResults.size()) {
                            results.addAll(thisNumResults.subList(offset, offset + limit));
                            break;
                        } else { // Else add all we can and query again
                            thisNumResults = thisNumResults.subList(offset, thisNumResults.size());
                            results.addAll(thisNumResults);
                            offset = 0;
                            limit -= thisNumResults.size();
                        }
                    } else {
                        offset -= thisNumResults.size();
                    }
                    dao.setStartsWith(String.valueOf(scope.getOrder().equals("ASC") ? ++currentSW : --currentSW));
                }

                offset = scope.getOffset();
            } else {
                // assemble the offset and limit
                dao.setOffset(offset);
                dao.setLimit(limit);

                // Does this browse have any contents?
                if (total > 0) {
                    // now run the query
                    results = dao.doValueQuery();

                    // now, if we don't have any results, we are at the end of the browse.  This will
                    // be because a starts_with value has been supplied for which we don't have
                    // any items.
                    if (results.size() == 0) {
                        // In this case, we will calculate a new offset for the last page of results
                        offset = total - scope.getResultsPerPage();
                        if (offset < 0) {
                            offset = 0;
                        }

                        // And rerun the query
                        dao.setOffset(offset);
                        results = dao.doValueQuery();
                    }
                } else {
                    // No records, so make an empty list
                    results = new ArrayList<String[]>();
                }
            }

            // construct the BrowseInfo object to pass back
            BrowseInfo browseInfo = new BrowseInfo(results, offset, total, offset);

            if (offset + scope.getResultsPerPage() < total)
            {
                browseInfo.setNextOffset(offset + scope.getResultsPerPage());
            }

            if (offset - scope.getResultsPerPage() > -1)
            {
                browseInfo.setPrevOffset(offset - scope.getResultsPerPage());
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
     * @return  the focus value to use
     * @throws BrowseException if browse error
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
            {
                so = browseIndex.getSortOption();
            }
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
     * Convert the value into an offset into the table for this browse
     *
     * @param value value
     * @return  the focus value to use
     * @throws BrowseException if browse error
     */
    private int getOffsetForValue(String value)
        throws BrowseException
    {
        // we need to make sure that we select from the correct column.  If the sort option
        // is the 0th option then we use sort_value, but if it is one of the others we have
        // to select from that column instead.  Otherwise, we end up missing the focus value
        // to do comparisons in other columns.  The use of the focus value needs to be consistent
        // across the browse
        SortOption so = scope.getSortOption();
        if (so == null || so.getNumber() == 0)
        {
            if (browseIndex.getSortOption() != null)
            {
                so = browseIndex.getSortOption();
            }
        }

        String col = "sort_1";
        if (so.getNumber() > 0)
        {
            col = "sort_" + Integer.toString(so.getNumber());
        }

        // now get the DAO to do the query for us, returning the highest
        // string value in the given column in the given table for the
        // item (I think)
        return dao.doOffsetQuery(col, value, scope.isAscending());
    }

    /**
     * Convert the value into an offset into the table for this browse
     *
     * @param value value
     * @return  the focus value to use
     * @throws BrowseException if browse error
     */
    private int getOffsetForDistinctValue(String value)
        throws BrowseException
    {
        if (!browseIndex.isMetadataIndex())
        {
            throw new IllegalArgumentException("getOffsetForDistinctValue called when not a metadata index");
        }

        // now get the DAO to do the query for us, returning the highest
        // string value in the given column in the given table for the
        // item (I think)
        return dao.doDistinctOffsetQuery("sort_value", value, scope.isAscending());
    }

    /**
     * Return a normalized focus value. If there is no normalization that can be performed,
     * return the focus value that is passed in.
     *
     * @param value a focus value to normalize
     * @return  the normalized focus value
     * @throws BrowseException if browse error
     */
    private String normalizeJumpToValue(String value)
        throws BrowseException
    {
        // If the scope has a focus value (focus by value)
        if (scope.hasJumpToValue())
        {
            // Normalize it based on the specified language as appropriate for this index
            return OrderFormat.makeSortString(scope.getJumpToValue(), scope.getJumpToValueLang(), scope.getBrowseIndex().getDataType());
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
     * @return total
     * @throws SQLException if database error
     * @throws BrowseException if browse error
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
     * @param distinct  is this a distinct browse or not
     * @return          the total number of results available in this type of browse
     * @throws SQLException if database error
     * @throws BrowseException if browse error
     */
    private int getTotalResults(boolean distinct)
        throws SQLException, BrowseException
    {
        log.debug(LogManager.getHeader(context, "get_total_results", "distinct=" + distinct));

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
        if ("0".equals(dao.getStartsWith())) {
            for (int x = 1; x <= 9; x++) {
                dao.setStartsWith(String.valueOf(x));
                count += dao.doCountQuery();
            }
            dao.setStartsWith("0");
        } else if ("9".equals(dao.getStartsWith())) {
            for (int x = 8; x >= 0; x--) {
                dao.setStartsWith(String.valueOf(x));
                count += dao.doCountQuery();
            }
            dao.setStartsWith("9");
        }

        // now put back the values we removed for this method
        dao.setJumpToField(focusField);
        dao.setJumpToValue(focusValue);
        dao.setOrderField(orderField);
        dao.setLimit(limit);
        dao.setOffset(offset);
        dao.setCountValues(null);

        log.debug(LogManager.getHeader(context, "get_total_results_return", "return=" + count));

        return count;
    }
}
