/*
 * Browse.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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


import java.sql.*;
import java.text.MessageFormat;
// Do not import java.util.* in order to avoid confusing
// java.util.Collection and org.dspace.content.Collection
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import org.dspace.storage.rdbms.*;
import org.dspace.content.*;
import org.dspace.core.Context;


/**
 * API for Browsing Items in DSpace by title, author, or date.
 * Browses only return archived Items.
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class Browse
{
    // Browse types
    static final int AUTHORS_BROWSE = 0;
    static final int ITEMS_BY_TITLE_BROWSE = 1;
    static final int ITEMS_BY_AUTHOR_BROWSE = 2;
    static final int ITEMS_BY_DATE_BROWSE = 3;

    /** Log4j log */
    private static Logger log = Logger.getLogger(Browse.class);

    /**
     * Constructor
     */
    private Browse() {}

    /**
     * Return distinct Authors in the given scope.
     * Results are returned in alphabetical order.
     *
     * @param scope The BrowseScope
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException If a database error occurs
     */
    public static BrowseInfo getAuthors(BrowseScope scope)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called getAuthors");

        scope.setBrowseType(AUTHORS_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(null);

        return getSomethingInternal(scope);
    }

    /**
     * Return Items indexed by title in the given scope.
     * Results are returned in alphabetical order.
     *
     * @param scope The BrowseScope
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException If a database error occurs
     */
    public static BrowseInfo getItemsByTitle(BrowseScope scope)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called getItemsByTitle");

        scope.setBrowseType(ITEMS_BY_TITLE_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(null);

        return getSomethingInternal(scope);
    }

    /**
     * Return Items indexed by date of issue in the given scope.
     *
     * <p>
     * If datesafter is true, the dates returned are the ones
     * after the focus; otherwise the dates are the ones before the
     * focus. Results will be ordered in increasing order (ie, earliest
     * to latest) if datesafter is true; in decreasing order otherwise.
     * </p>
     *
     * @param scope The BrowseScope
     * @param datesafter If true, the dates returned are the ones
     * after focus; otherwise the dates are the ones before focus.
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException If a database error occurs
     */
    public static BrowseInfo getItemsByDate(BrowseScope scope,
                                            boolean datesAfter)
        throws SQLException
    {
        if (log.isDebugEnabled())
            log.debug("Called getItemsByDate");

        scope.setBrowseType(ITEMS_BY_DATE_BROWSE);
        scope.setAscending(datesAfter);
        scope.setSortByTitle(null);

        return getSomethingInternal(scope);
    }

    /**
     * Return Items in the given scope by Author (exact match).
     *
     * @param scope The BrowseScope
     * @param sortbytitle If true, the returned items are sorted by title;
     * otherwise they are sorted by date issued.
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException If a database error occurs
     */
    public static BrowseInfo getItemsByAuthor(BrowseScope scope,
                                              boolean sortByTitle)
        throws SQLException
    {
        if (! scope.hasFocus())
            throw new IllegalArgumentException("Must specify an author for getItemsByAuthor");

        if (log.isDebugEnabled())
            log.debug("Called getItemsByAuthor");

        scope.setBrowseType(ITEMS_BY_AUTHOR_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(sortByTitle ? Boolean.TRUE : Boolean.FALSE);

        return getSomethingInternal(scope);
    }

    /**
     * Returns the last items submitted to DSpace in the given scope.
     *
     * @param scope The Browse Scope
     * @return A List of Items submitted
     * @exception SQLException If a database error occurs
     */
    public static List getLastSubmitted(BrowseScope scope)
        throws SQLException
    {
        Context context = scope.getContext();
        String sql = getLastSubmittedQuery(scope);

        if (log.isDebugEnabled())
            log.debug("SQL for last submitted is \"" + sql + "\"");

        List results = DatabaseManager.query(context, sql).toList();
        return getLastSubmittedResults(context, results);
    }

    /**
     * Return the SQL used to determine the last submitted Items
     * for scope.
     */
    private static String getLastSubmittedQuery(BrowseScope scope)
    {
        String table = getLastSubmittedTable(scope);

        StringBuffer buffer = new StringBuffer("select * from ")
            .append(table)
            .append(getScopeClause(scope, "where"));

        // NOTE: Postgres-specific function
        if (! scope.hasNoLimit())
            buffer.append(" LIMIT ").append(scope.getTotal());

        return buffer.toString();
    }

    /**
     * Return the name of the Browse index table to query for
     * last submitted items in the given scope.
     */
    private static String getLastSubmittedTable(BrowseScope scope)
    {
        if (scope.isCommunityScope())
            return "CommunityItemsByDateAccessioned";
        else if (scope.isCollectionScope())
            return "CollectionItemsByDateAccessioned";

        return "ItemsByDateAccessioned";
    }

    /**
     * Transform the query results into a List of Items.
     */
    private static List getLastSubmittedResults(Context context, List results)
        throws SQLException
    {
        if ((results == null) || (results.isEmpty()))
            return Collections.EMPTY_LIST;

        List items = new ArrayList();

        //  FIXME This seems like a very common need, so might
        //  be factored out at some point.
        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            TableRow row = (TableRow) iterator.next();
            Item item = Item.find(context, row.getIntColumn("item_id"));
            items.add(item);
        }

        return items;
    }

    ////////////////////////////////////////
    // Index maintainence methods
    ////////////////////////////////////////

    /**
     * This method should be called whenever an item is removed.
     *
     * @param context - The database context
     * @param id - The id of the item which has been removed
     * @exception SQLException - If a database error occurs
     */
    public static void itemRemoved(Context context, int id)
        throws SQLException
    {
        String sql = "delete from {0} where item_id = " + id;

        String[] browseTables = BrowseTables.tables();

        for (int i = 0; i < browseTables.length; i++ )
        {
            String query = MessageFormat.format
                (sql, new String[] { browseTables[i] });
            DatabaseManager.updateQuery(context, query);
        }
    }

    /**
     * This method should be called whenever an item has changed.
     * Changes include:
     *
     *   <ul>
     *    <li>DC values are added, removed, or modified
     *    <li>the value of the in_archive flag changes
     *   </ul>
     *
     * @param context - The database context
     * @param item - The item which has been added
     * @exception SQLException - If a database error occurs
     */
    public static void itemChanged(Context context, Item item)
        throws SQLException
    {
        // This is a bit heavy-weight, but without knowing what
        // changed, it's easiest just to replace the values
        // en masse.

        itemRemoved(context, item.getID());

        if (! item.isArchived())
            return;

        itemAdded(context, item);
    }

    /**
     * This method should be called whenever an item is added.
     *
     * @param context - The database context
     * @param item - The item which has been added
     * @exception SQLException - If a database error occurs
     */
    public static void itemAdded(Context context, Item item)
        throws SQLException
    {
        Map table2dc = new HashMap();
        table2dc.put("ItemsByTitle",
                     item.getDC("title", null, Item.ANY));
        table2dc.put("ItemsByAuthor",
                     item.getDC("contributor", "author", Item.ANY));
        table2dc.put("ItemsByDate",
                     item.getDC("date", "issued", Item.ANY));
        table2dc.put("ItemsByDateAccessioned",
                     item.getDC("date", "accessioned", Item.ANY));

        for (Iterator iterator = table2dc.keySet().iterator();
             iterator.hasNext(); )
        {
            String table = (String) iterator.next();
            DCValue[] dc = (DCValue[]) table2dc.get(table);

            for (int i = 0; i < dc.length; i++ )
            {
                TableRow row = DatabaseManager.create(context, table);
                row.setColumn("item_id", item.getID());

                String value = dc[i].value;
                if ("ItemsByDateAccessioned".equals(table))
                    row.setColumn("date_accessioned", value);
                else if ("ItemsByDate".equals(table))
                    row.setColumn("date_issued", value);
                else if ("ItemsByAuthor".equals(table))
                    row.setColumn("author", value);
                else if ("ItemsByTitle".equals(table))
                {
                    String title = NormalizedTitle.normalize(value, dc[i].language);
                    row.setColumn("title", value);
                    row.setColumn("sort_title", title);
                }

                DatabaseManager.update(context, row);
            }
        }
    }

    /**
     * Index all items in DSpace. This method may be resource-intensive.
     *
     * @param context Current DSpace context
     * @return The number of items indexed.
     * @exception SQLException If a database error occurs
     */
    public static int indexAll(Context context)
        throws SQLException
    {
        indexRemoveAll(context);

        int count = 0;
        ItemIterator iterator = Item.findAll(context);

        while (iterator.hasNext())
        {
            itemAdded(context, (Item) iterator.next());
            count++;
        }

        return count;
    }

    /**
     * Remove all items in DSpace from the Browse index.
     *
     * @param context Current DSpace context
     * @return The number of items removed.
     * @exception SQLException If a database error occurs
     */
    public static int indexRemoveAll(Context context)
        throws SQLException
    {
        int total = 0;

        String[] browseTables = BrowseTables.tables();

        for (int i = 0; i < browseTables.length; i++ )
        {
            String sql = "delete from " + browseTables[i];
            total += DatabaseManager.updateQuery(context, sql);
        }

        return total;
    }

    ////////////////////////////////////////
    // Other methods
    ////////////////////////////////////////

    /**
     * Return the normalized form of title.
     */
    public static String getNormalizedTitle(String title, String lang)
    {
        return NormalizedTitle.normalize(title, lang);
    }

    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////

    private static BrowseInfo getSomethingInternal(BrowseScope params)
        throws SQLException
    {
        // Check for a cached browse
        BrowseInfo cachedInfo = (BrowseInfo) BrowseCache.get(params);
        if (cachedInfo != null)
            return cachedInfo;

        int transactionIsolation = setTransactionIsolation(params);

        try
        {
            // Run the Browse queries
            // If the focus is an Item, this returns the value
            String itemValue = getItemValue(params);
            List results = new ArrayList();
            results.addAll(getResultsBeforeFocus(params, itemValue));
            int beforeFocus = results.size();
            results.addAll(getResultsAfterFocus(params, itemValue, beforeFocus));

            // Find out the total in the index, and the number of
            // matches for the query
            int total = countTotalInIndex(params, results.size());
            int matches = countMatches(params, itemValue, total, results.size());

            int position = (total == matches) ? 0 :
                Math.max(total - matches - beforeFocus, 0);

            sortResults(params, results);

            BrowseInfo info = new BrowseInfo
                (results, position, total, beforeFocus);

            logInfo(info);

            BrowseCache.add(params, info);
            return info;
        }
        finally
        {
            restoreTransactionIsolation(params, transactionIsolation);
        }
    }

    /**
     * If focus refers to an Item, return a value for the item (its
     * title, author, accession date, etc). Otherwise return null.
     *
     * In general, the queries for these values look like this:
     *   select max(date_issued) from ItemsByDate where item_id = 7;
     *
     * The max operator ensures that only one value is returned.
     *
     * If limiting to a community or collection, we add a clause like:
     *    community_id = 7
     *    collection_id = 201
     */
    protected static String getItemValue(BrowseScope params)
        throws SQLException
    {
        if (! params.focusIsItem())
            return null;

        PreparedStatement statement = null;
        ResultSet results = null;

        try
        {
            String tablename = BrowseTables.getTable(params);
            String column = BrowseTables.getValueColumn(params);

            String itemValueQuery = new StringBuffer()
                .append("select ")
                .append("max(")
                .append(column)
                .append(") from ")
                .append(tablename)
                .append(" where ")
                .append(" item_id = ")
                .append(params.getFocusItemId())
                .append(getScopeClause(params, "and"))
                .toString();

            statement = createStatement(params, itemValueQuery);
            results = statement.executeQuery();
            String itemValue = results.next() ? results.getString(1) : null;

            if (log.isDebugEnabled())
                log.debug("Subquery value is " + itemValue);

            return itemValue;
        }
        finally
        {
            if (statement != null)
                statement.close();
            if (results != null)
                results.close();
        }
    }

    /**
     * Run a database query and return results before the focus.
     *
     * @param params The Browse Scope
     * @param itemValue If the focus is an Item, this is its value
     * in the index (its title, author, etc).
     */
    protected static List getResultsBeforeFocus(BrowseScope params,
                                                String itemValue)
        throws SQLException
    {
        // Starting from beginning of index
        if (! params.hasFocus())
            return Collections.EMPTY_LIST;
        // No previous results desired
        if (params.getNumberBefore() == 0)
            return Collections.EMPTY_LIST;
        // ItemsByAuthor. Since this is an exact match, it
        // does not make sense to return values before the
        // query.
        if (params.getBrowseType() == ITEMS_BY_AUTHOR_BROWSE)
            return Collections.EMPTY_LIST;

        PreparedStatement statement = createSql(params, itemValue, false, false);

        List qresults = DatabaseManager.query(statement).toList();
        int numberDesired = params.getNumberBefore();
        List results = getResults(params, qresults, numberDesired);

        if (! results.isEmpty())
            Collections.reverse(results);
        return results;
    }

    /**
     * Run a database query and return results after the focus.
     *
     * @param params The Browse Scope
     * @param itemValue If the focus is an Item, this is its value
     * in the index (its title, author, etc).
     */
    protected static List getResultsAfterFocus(BrowseScope params,
                                               String itemValue,
                                               int count)
        throws SQLException
    {
        // No results desired
        if (params.getTotal() == 0)
            return Collections.EMPTY_LIST;

        PreparedStatement statement = createSql(params, itemValue, true, false);

        List qresults = DatabaseManager.query(statement).toList();

        // The number of results we want is either -1 (everything)
        // or the total, less the number already retrieved.
        int numberDesired = -1;
        if (! params.hasNoLimit())
            numberDesired = Math.max(params.getTotal() - count, 0);

        return getResults(params, qresults, numberDesired);
    }

    /*
     * Return the total number of values in an index.
     *
     * <p>
     * We total Authors with SQL like:
     *    select count(distinct author) from ItemsByAuthor;
     * ItemsByAuthor with:
     *    select count(*) from ItemsByAuthor where author = ?;
     * and every other index with:
     *    select count(*) from ItemsByTitle;
     * </p>
     *
     * <p>
     * If limiting to a community or collection, we add a clause like:
     *    community_id = 7
     *    collection_id = 201
     * </p>
     */
    protected static int countTotalInIndex(BrowseScope params,
                                           int numberOfResults)
        throws SQLException
    {
        int browseType = params.getBrowseType();

        // When finding Items by Author, it often happens that
        // we find every single Item (eg, the Author only published
        // 2 works, and we asked for 15), and so can skip the
        // query.
        if ((browseType == ITEMS_BY_AUTHOR_BROWSE) &&
            (params.hasNoLimit() ||
             ((params.getTotal() > numberOfResults))))
            return numberOfResults;

        PreparedStatement statement = null;
        Object obj    = params.getScope();

        try
        {
            String table = BrowseTables.getTable(params);

            StringBuffer buffer = new StringBuffer()
                .append("select count(")
                .append(getTargetColumns(params))
                .append(") from ")
                .append(table);

            boolean hasWhere = false;
            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            {
                hasWhere = true;
                buffer.append(" where author = ?");
            }

            String connector = hasWhere ? "and" : "where";
            String sql = buffer.append(getScopeClause(params, connector))
                .toString();

            if (log.isDebugEnabled())
                log.debug("Total sql: \"" + sql + "\"");

            statement = createStatement(params, sql);

            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
                statement.setString(1, (String) params.getFocus());

            return getIntValue(statement);
        }
        finally
        {
            if (statement != null)
                statement.close();
        }
    }

    /**
     * Return the number of matches for the browse scope.
     */
    protected static int countMatches(BrowseScope params,
                                      String itemValue,
                                      int totalInIndex,
                                      int numberOfResults)
        throws SQLException
    {
        // Matched everything
        if (numberOfResults == totalInIndex)
            return totalInIndex;

        // Scope matches everything in the index
        // Note that this only works when the scope is all of DSpace,
        // since the Community and Collection index tables
        // include Items in other Communities/Collections
        if ((! params.hasFocus()) && params.isAllDSpaceScope())
            return totalInIndex;

        boolean direction = params.getAscending();
        PreparedStatement statement = createSql(params, itemValue, direction, true);

        return getIntValue(statement);
    }

    /**
     * Sort the results returned from the browse if necessary.
     * The list of results is sorted in-place.
     */
    private static void sortResults(BrowseScope params, List results)
    {
        // Currently we only sort ItemsByAuthor browses
        if (params.getBrowseType() != ITEMS_BY_AUTHOR_BROWSE)
            return;

        ItemComparator ic = params.getSortByTitle().booleanValue() ?
            new ItemComparator("title", null,     Item.ANY, true) :
            new ItemComparator("date",  "issued", Item.ANY, true);

        Collections.sort(results, ic);
    }

    /**
     * Transform the results of the query (TableRow objects_
     * into a List of Strings (for getAuthors()) or
     * Items (for all the other browses).
     *
     * @param scope The Browse Scope
     * @param results The results of the query
     * @param max The maximum number of results to return
     */
    private static List getResults(BrowseScope scope,
                                   List results,
                                   int max)
        throws SQLException
    {
        if (results == null)
            return Collections.EMPTY_LIST;

        List theResults = new ArrayList();
        boolean hasLimit = ! scope.hasNoLimit();
        boolean isAuthorsBrowse = scope.getBrowseType() == AUTHORS_BROWSE;

        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            TableRow row = (TableRow) iterator.next();
            Object theValue = (isAuthorsBrowse) ?
                (Object) row.getStringColumn("author") :
                (Object) new Integer(row.getIntColumn("item_id"));

            // Should not happen
            if (theValue == null)
                continue;

            // Exceeded limit
            if ( hasLimit &&  (theResults.size() >= max))
                break;

            theResults.add(theValue);

            if (log.isDebugEnabled())
                log.debug("Adding result " + theValue);
        }

        return isAuthorsBrowse ?
            theResults : toItems(scope.getContext(), theResults);
    }

    /**
     * Create a PreparedStatement to run the correct query for params.
     *
     * @param params The Browse scope
     * @param subqueryValue If the focus is an item, this is its value
     * in the browse index (its title, author, date, etc). Otherwise null.
     * @param after If true, create SQL to find the items after the focus.
     * Otherwise create SQL to find the items before the focus.
     * @param isCount If true, create SQL to count the number of matches
     * for the query. Otherwise just the query.
     */
    private static PreparedStatement createSql(BrowseScope params,
                                               String subqueryValue,
                                               boolean after,
                                               boolean isCount)
        throws SQLException
    {
        String sqli = createSqlInternal(params, subqueryValue, isCount);
        String sql = formatSql(params, sqli, subqueryValue, after);
        PreparedStatement statement = createStatement(params, sql);

        // Browses without a focus have no parameters to bind
        if (params.hasFocus())
        {
            String value = subqueryValue != null ?
                subqueryValue : (String) params.getFocus();

            statement.setString(1, value);
            // Binds the parameter in the subquery clause
            if (subqueryValue != null)
                statement.setString(2, value);
        }

        if (log.isDebugEnabled())
            log.debug("Created SQL \"" + sql + "\"");

        return statement;
    }

    /**
     * Create a SQL string to run the correct query.
     */
    private static String createSqlInternal(BrowseScope params,
                                            String itemValue,
                                            boolean isCount)
        throws SQLException
    {
        String tablename = BrowseTables.getTable(params);
        String column = BrowseTables.getValueColumn(params);

        StringBuffer sqlb = new StringBuffer()
            .append("select ")
            .append(isCount ? "count(" : "")
            .append(getTargetColumns(params))
            .append(isCount ? ")" : "")
            .append(" from ")
            .append(tablename);

        // If the browse uses items (or item ids) instead of String values
        // make a subquery.
        // We use a separate query to make sure the subquery works correctly
        // when item values are the same. (this is transactionally
        // safe because we set the isolation level).

        // If we're NOT searching from the start, add some clauses
        boolean addedWhereClause = false;
        if (params.hasFocus())
        {
            String subquery = null;
            if (params.focusIsItem())
                subquery = new StringBuffer()
                    .append(" or ( ")
                    .append(column)
                    // Item id must be before or after the desired item
                    .append(" = ? and item_id {0}  ")
                    .append(params.getFocusItemId())
                    .append(")")
                    .toString();

            if (log.isDebugEnabled())
                log.debug("Subquery is \"" + subquery + "\"");

            sqlb
                .append(" where ")
                .append("(")
                .append(column)
                // Operator is a parameter
                .append(" {1} ")
                .append("?")
                .append(params.focusIsItem() ? subquery : "")
                .append(")");

            addedWhereClause = true;
        }

        String connector = addedWhereClause ? " and " : " where ";
        sqlb.append(getScopeClause (params, connector));

        // For counting, skip the "order by" and "limit" clauses
        if (isCount)
            return sqlb.toString();

        // Add an order by clause -- a parameter
        sqlb.append(" order by ").append(column).append("{2}")
        // If an item, make sure it's ordered by item_id as well
        .append((params.focusIsString() ||
                 (params.getBrowseType() == AUTHORS_BROWSE))
            ? "" : ", item_id");

        // A limit on the total returned (Postgres extension)
        if (! params.hasNoLimit())
            sqlb.append(" LIMIT {3} ");

        return sqlb.toString();
    }

    /**
     * Format SQL according to BROWSETYPE.
     *
     * The different browses use different operators.
     *
     */
    private static String formatSql(BrowseScope params,
                                    String sql,
                                    String subqueryValue,
                                    boolean after)
    {
        boolean before = ! after;
        int browseType = params.getBrowseType();
        boolean ascending = params.getAscending();
        int numberDesired = before ?
            params.getNumberBefore() : params.getTotal();

        // Search operator
        // Normal case: before is less than, after is greater than or equal
        String beforeOperator = "<";
        String afterOperator = ">=";

        // For authors, only equality is relevant
        if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            afterOperator = "=";
        // Subqueries add a clause which checks for the item specifically,
        // so we do not check for equality here
        if (subqueryValue != null)
        {
            beforeOperator = "<";
            afterOperator = ">";
        }
        if (!ascending)
        {
            beforeOperator = ">";
            afterOperator = "<=";
        }

        String beforeSubqueryOperator = "<";
        String afterSubqueryOperator = ">=";

        // For authors, only equality is relevant
        if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            afterSubqueryOperator = "=";
        if (!ascending)
        {
            beforeSubqueryOperator = ">";
            afterSubqueryOperator = "<=";
        }

        String order = before ? " desc" : "";

        if (!ascending)
            order = before ? "" : " desc";

        // Note that it's OK to have unused arguments in the array;
        // see the javadoc of java.text.MessageFormat
        // for the whole story.
        List args = new ArrayList();

        args.add(before ? beforeSubqueryOperator : afterSubqueryOperator);
        args.add(before ? beforeOperator : afterOperator);
        args.add(order);
        args.add(new Integer(numberDesired));

        return MessageFormat.format(sql, args.toArray());
    }

    /**
     * Write out a log4j message
     */
    private static void logInfo(BrowseInfo info)
    {
        if (! log.isInfoEnabled())
            return;

        log.info("Number of Results: " + info.getResultCount() +
                 " Overall position: " + info.getOverallPosition() +
                 " Total " + info.getTotal() +
                 " Offset " + info.getOffset());
        int lastIndex = (info.getOverallPosition() + info.getResultCount());
        boolean noresults = (info.getTotal() == 0) || (info.getResultCount() == 0);

        if (noresults)
            log.info("Got no results");

        log.info("Got results: " +
                 info.getOverallPosition() +
                 " to " +
                 lastIndex +
                 " out of " + info.getTotal());
    }

    /**
     * Return the columns according to browseType.
     */
    private static String getTargetColumns(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();
        return (browseType == AUTHORS_BROWSE) ? "distinct author" : "*";
    }

    /**
     * Return a scoping clause.
     *
     * If scope is ALLDSPACE_SCOPE, return the empty string.
     *
     * Otherwise, the SQL clause which is generated looks like:
     *    CONNECTOR community_id = 7
     *    CONNECTOR collection_id = 203
     *
     * CONNECTOR may be empty, or it may be a SQL keyword like "where", "and",
     * and so forth.
     */
    static String getScopeClause(BrowseScope params, String connector)
    {
        if (params.isAllDSpaceScope())
            return "";

        boolean isCommunity = params.isCommunityScope();
        Object obj = params.getScope();

        int id = (isCommunity) ?
            ((Community) obj).getID() : ((Collection) obj).getID();

        String column = (isCommunity) ? "community_id" : "collection_id";

        return new StringBuffer()
            .append(" ")
            .append(connector)
            .append(" ")
            .append(column)
            .append(" = ")
            .append(id)
            .toString();
    }

    /**
     * Create a PreparedStatement with the given sql.
     */
    private static PreparedStatement createStatement(BrowseScope params,
                                                     String sql)
        throws SQLException
    {
        Connection connection = params.getContext().getDBConnection();
        return connection.prepareStatement(sql);
    }


    private static int setTransactionIsolation(BrowseScope params)
        throws SQLException
    {
        Connection connection = params.getContext().getDBConnection();
        int level = connection.getTransactionIsolation();
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return level;
    }

    private static void restoreTransactionIsolation(BrowseScope params, int level)
        throws SQLException
    {
        Connection connection = params.getContext().getDBConnection();
        connection.setTransactionIsolation(level);
    }


    /**
     * Return a single int value from STATEMENT.
     */
    private static int getIntValue(PreparedStatement statement)
        throws SQLException
    {
        ResultSet results = null;

        try
        {
            results = statement.executeQuery();
            return results.next() ? results.getInt(1) : -1;
        }
        finally
        {
            if (results != null)
                results.close();
        }
    }

    /**
     * Convert a list of item ids to full Items.
     */
    private static List toItems(Context context, List ids)
        throws SQLException
    {
        //  FIXME Again, this is probably a more general need
        List results = new ArrayList();

        for (Iterator iterator = ids.iterator(); iterator.hasNext(); )
        {
            Integer id = (Integer) iterator.next();
            Item item = Item.find(context, id.intValue());
            if (item != null)
                results.add(item);
        }

        return results;
    }
}

class NormalizedTitle
{
    private static String[] STOP_WORDS = new String[]{ "A", "An", "The" };

    /**
     * Returns a normalized String corresponding to TITLE.
     */
    public static String normalize(String title, String lang)
    {
        if (lang == null)
            return title;

        return (lang.startsWith("en")) ?
            normalizeEnglish(title) : title;
    }

    /**
     * Returns a normalized String corresponding to TITLE.
     * The normalization is effected by:
     *
     *   + first removing leading spaces, if any
     *   + then removing the first leading occurences of "a", "an"
     *     and "the" (in any case).
     *   + removing any whitespace following an occurence of a stop word
     *
     * This simple strategy is only expected to be used for
     * English words.
     */
    public static String normalizeEnglish(String title)
    {
        // Corner cases
        if (title == null)
            return null;
        if (title.length() == 0)
            return title;

        // State variables
        // First find leading whitespace, if any
        int startAt = firstWhitespace(title);
        boolean modified = (startAt != 0);
        boolean usedStopWord = false;
        String stop = null;

        // Examine each stop word
        for (int i = 0; i < STOP_WORDS.length; i++)
        {
            stop = STOP_WORDS[i];
            int stoplen = stop.length();

            //   The title must start with the stop word (skipping white space
            //     and ignoring case).
            boolean found = title.toLowerCase().startsWith(stop.toLowerCase(), startAt) &&
                //   The title must be longer than whitespace plus the stop word
                title.length() >= (startAt + stoplen + 1) &&
                //   The stop word must be followed by white space
                Character.isWhitespace(title.charAt(startAt + stoplen));

            if (found)
            {
                modified = true;
                usedStopWord = true;

                startAt += stoplen;

                // Strip leading whitespace again, if any
                int firstw = firstWhitespace(title, startAt);

                if (firstw != 0)
                    startAt = firstw;

                    // Only process a single stop word
                break;
            }
        }

        // If we didn't change anything, just return the title as-is
        if (!modified)
            return title;

        // If we just stripped white space, return a substring
        if (!usedStopWord)
            return title.substring(startAt);

        // Otherwise, return the substring with the stop word appended
        return new StringBuffer(title.substring(startAt))
            .append(", ")
            .append(stop).toString();
    }

    /**
     * Return the index of the first non-whitespace character in the String.
     */
    private static int firstWhitespace(String title)
    {
        return firstWhitespace(title, 0);
    }

    /**
     * Return the index of the first non-whitespace character
     * in the character array.
     */
    private static int firstWhitespace(char[] title)
    {
        return firstWhitespace(title, 0);
    }

    /**
     * Return the index of the first non-whitespace character in
     * the String, starting at position STARTAT.
     */
    private static int firstWhitespace(String title, int startAt)
    {
        return firstWhitespace(title.toCharArray(), startAt);
    }

    /**
     * Return the index of the first non-whitespace character in
     * the character array, starting at position STARTAT.
     */
    private static int firstWhitespace(char[] title, int startAt)
    {
        int first = 0;

        for (int j = startAt; j < title.length; j++)
        {
            if (Character.isWhitespace(title[j]))
            {
                first = j + 1;
                continue;
            }

            break;
        }

        return first;
    }
}

class BrowseCache
{
    private static Map tableMax = new HashMap();
    private static Map tableSize = new HashMap();

    /** log4j object */
    private static Logger log = Logger.getLogger(BrowseCache.class);

    private static Map cache     = new WeakHashMap();
    // Everything in the cache is held via Weak References, and is
    // subject to being gc-ed at any time.
    // The dateCache holds normal references, so anything in it
    // will stay around.
    private static SortedMap dateCache = new TreeMap();
    private static final int CACHE_MAXIMUM = 30;

    /**
     * Look for cached Browse data corresponding to KEY.
     */
    public static BrowseInfo get(BrowseScope key)
    {
        if (log.isDebugEnabled())
            log.debug("Checking browse cache with " + cache.size() + " objects");
        BrowseInfo cachedInfo = (BrowseInfo) cache.get(key);

        try
        {
            // Index has never been calculated
            if (getMaximum(key) == -1)
                updateIndexData(key);

            if (cachedInfo == null)
            {
                if (log.isDebugEnabled())
                    log.debug("Not in browse cache");

                return null;
            }

            // If we found an object, make sure that the browse indexes
            // have not changed.
            //
            // The granularity for this is quite large;
            // any change to the index and we will calculate from scratch.,
            // Thus, the cache works well when few changes are made, or
            // when changes are spaced widely apart.
            if (indexHasChanged(key))
            {
                if (log.isDebugEnabled())
                    log.debug("Index has changed");

                cache.remove(key);
                return null;
            }
        }
        catch (SQLException sqle)
        {
            if (log.isDebugEnabled())
                log.debug("Caught SQLException: " + sqle, sqle);

            return null;
        }

        // Cached object
        if (log.isDebugEnabled())
            log.debug("Using cached browse");

        cachedInfo.setCached(true);
        return cachedInfo;
    }

    /**
     * Return true if an index has changed
     */
    public static boolean indexHasChanged(BrowseScope key)
        throws SQLException
    {
        Context context = null;
        try
        {
            context = new Context();
            TableRow results = countAndMax(context, key);
            long count = -1;
            int max = -1;

            if (results != null)
            {
                count = results.getLongColumn("count");
                max   = results.getIntColumn("max");
            }

            context.complete();

            // Same?
            if ((count == getCount(key)) && (max == getMaximum(key)))
                return false;

            // Update 'em
            setMaximum(key, max);
            setCount(key, count);

            // The index has in fact changed
            return true;
        }
        catch (SQLException sqle)
        {
            if (context != null)
                context.abort();

            throw sqle;
        }
    }

    /**
     * Compute and save the values for the number of values in the
     * index, and the maximum such value.
     */
    public static void updateIndexData(BrowseScope key)
        throws SQLException
    {
        Context context = null;

        try
        {
            context = new Context();
            TableRow results = countAndMax(context, key);
            long count = -1;
            int max = -1;

            if (results != null)
            {
                count = results.getLongColumn("count");
                max   = results.getIntColumn("max");
            }

            context.complete();

            setMaximum(key, max);
            setCount(key, count);
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();

            e.printStackTrace();
        }
    }

    /**
     * Retrieve the values for count and max
     */
    public static TableRow countAndMax(Context context,
                                       BrowseScope params)
        throws SQLException
    {
        // The basic idea here is that we'll check an indexes
        // maximum id and its count: if the maximum id has changed,
        // then there are new values, and if the count has changed,
        // then some items may have been removed. We assume that
        // values never change.

        String sql = new StringBuffer()
            .append("select count({0}) as count, max({0}) as max from ")
            .append(BrowseTables.getTable(params))
            .append(Browse.getScopeClause(params, "where"))
            .toString();

        // Format it to use the correct columns
        String countColumn = BrowseTables.getIndexColumn(params);
        Object[] args = new Object[] {countColumn, countColumn};
        String SQL = MessageFormat.format(sql, args);

        // Run the query
        if (log.isDebugEnabled())
            log.debug("Running SQL to check whether index has changed: \"" + SQL + "\"");

        return DatabaseManager.querySingle(context, null, SQL);
    }

    /**
     * Add info to cache, using key.
     */
    public static void add(BrowseScope key, BrowseInfo info)
    {
        // Don't bother caching browses with no results, they are
        // fairly cheap to calculate
        if (info.getResultCount() == 0)
            return;

        // Add the info to the cache
        // Since the object is passed in to us (and thus the caller
        // may change it), we make a copy.
        cache.put((BrowseScope) key.clone(), info);
        // Make sure the date cache is current
        cleanDateCache();
        // Save a new entry into the date cache
        dateCache.put(new java.util.Date(), key);
    }

    /**
     * Remove entries from the date cache
     */
    private static void cleanDateCache()
    {
        synchronized(dateCache)
        {
            // Plenty of room!
            if (dateCache.size() < CACHE_MAXIMUM)
                return;

            // Remove the oldest object
            dateCache.remove(dateCache.firstKey());
        }
    }

    /**
     * Return the maximum value
     */
    private static int getMaximum(BrowseScope scope)
    {
        String table = BrowseTables.getTable(scope);
        Integer value = (Integer) tableMax.get(table);

        return value == null ? -1 : value.intValue();
    }

    private static long getCount(BrowseScope scope)
    {
        String table = BrowseTables.getTable(scope);
        Long value = (Long) tableSize.get(table);

        return value == null ? -1 : value.longValue();
    }

    private static void setMaximum(BrowseScope scope, int max)
    {
        String table = BrowseTables.getTable(scope);
        tableMax.put(table, new Integer(max));
    }

    private static void setCount(BrowseScope scope, long count)
    {
        String table = BrowseTables.getTable(scope);
        tableSize.put(table, new Long(count));
    }

}

// Encapsulates browse table info:
//   * Each scope and browsetype has a corresponding table or view
//   * Each browse table or view has a value column
//   * Some of the browse tables are true tables, others are views.
//     The index maintenance code needs to know the true tables.
//     The true tables have index columns, which can be used for caching
class BrowseTables
{
    private static final String[] BROWSE_TABLES = new String[]
    {
        "ItemsByAuthor",
        "ItemsByDate",
        "ItemsByDateAccessioned",
        "ItemsByTitle",
    };

    /**
     * Return the browse tables. This only returns true tables,
     * views are ignored.
     */
    public static String[] tables()
    {
        return BROWSE_TABLES;
    }

    /**
     * Return the browse table or view for scope.
     */
    public static String getTable(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();
        boolean isCommunity = scope.isCommunityScope();
        boolean isCollection = scope.isCollectionScope();

        if ((browseType == Browse.AUTHORS_BROWSE) ||
            (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE))
        {
            if (isCommunity)
                return "CommunityItemsByAuthor";
            if (isCollection)
                return "CollectionItemsByAuthor";

            return "ItemsByAuthor";
        }

        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
        {
            if (isCommunity)
                return "CommunityItemsByTitle";
            if (isCollection)
                return "CollectionItemsByTitle";

            return "ItemsByTitle";
        }

        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
        {
            if (isCommunity)
                return "CommunityItemsByDate";
            if (isCollection)
                return "CollectionItemsByDate";

            return "ItemsByDate";
        }

        throw new IllegalArgumentException("No table for browse and scope combination");
    }

    /**
     * Return the name of the column that holds the index.
     */
    public static String getIndexColumn(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();

        if (browseType == Browse.AUTHORS_BROWSE)
            return "items_by_author_id";
        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
            return "items_by_author_id";
        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
            return "items_by_date_id";
        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
            return "items_by_title_id";

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }

    /**
     * Return the name of the column that holds the Browse value
     * (the title, author, date, etc).
     */
    public static String getValueColumn(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();

        if (browseType == Browse.AUTHORS_BROWSE)
            return "author";
        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
            return "author";
        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
            return "date_issued";
        // Note that we use the normalized form of the title
        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
            return "sort_title";

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }
}
