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
    // Browse scopes
    static final int ALLDSPACE_SCOPE = 0;
    static final int COMMUNITY_SCOPE = 1;
    static final int COLLECTION_SCOPE = 2;

    // Browse types
    static final int AUTHORS_BROWSE = 0;
    static final int ITEMS_BY_TITLE_BROWSE = 1;
    static final int ITEMS_BY_AUTHOR_BROWSE = 2;
    static final int ITEMS_BY_DATE_BROWSE = 3;

    private static Logger log = Logger.getLogger(Browse.class);

    /**
     * Return distinct Authors in the given scope.
     * Results are returned in alphabetical order.
     *
     * @param scope - The BrowseScope
     * @return - A BrowseInfo object, the results of the browse
     * @exception SQLException - If a database error occurs
     */
    public static BrowseInfo getAuthors(BrowseScope scope)
        throws SQLException
    {
        return getSomethingInternal(scope, AUTHORS_BROWSE, true, null);
    }

    /**
     * Return Items indexed by title in the given scope.
     *
     * @param scope - The BrowseScope
     * @return - A BrowseInfo object, the results of the browse
     * @exception SQLException - If a database error occurs
     */
    public static BrowseInfo getItemsByTitle(BrowseScope scope)
        throws SQLException
    {
        return getSomethingInternal(scope, ITEMS_BY_TITLE_BROWSE, true, null);
    }

    /**
     * Return Items indexed by date of issue in the given scope.
     *
     * If DATESAFTER is true, the dates returned are the ones
     * AFTER date; otherwise the dates are the ones before DATE.
     * Results will be ordered in increasing order (ie, earliest to
     * latest) if DATESAFTER is true; in decreasing order otherwise.
     *
     * @param scope - The BrowseScope
     * @return - A BrowseInfo object, the results of the browse
     * @exception SQLException - If a database error occurs
     */
    public static BrowseInfo getItemsByDate(BrowseScope scope,
                                            boolean datesAfter)
        throws SQLException
    {
        return getSomethingInternal(scope, ITEMS_BY_DATE_BROWSE, datesAfter, null);
    }

    /**
     * Return Items in the given scope by Author (exact match).
     *
     * If SORTBYTITLE is true, then the returned items are sorted
     * by title; otherwise, they are sorted by date issued.
     *
     * @param scope - The BrowseScope
     * @return - A BrowseInfo object, the results of the browse
     * @exception SQLException - If a database error occurs
     */
    public static BrowseInfo getItemsByAuthor(BrowseScope scope,
                                              boolean sortByTitle)
        throws SQLException
    {
        return getSomethingInternal
            (scope,
             ITEMS_BY_AUTHOR_BROWSE,
             true,
             sortByTitle ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Returns the last N items submitted through DSpace
     * If N is -1, returns ALL Items submitted.
     *
     * @param n - Number of Items to return.
     * @return - A List of Items
     * @exception SQLException - If a database error occurs
     */
    public static List getLastSubmitted(BrowseScope scope)
        throws SQLException
    {
        Object obj = scope.getScope();
        int type = getScope(obj);
        int total = scope.getTotal();

        boolean isCommunity = (type == COMMUNITY_SCOPE);
        boolean isCollection = (type == COLLECTION_SCOPE);

        // Sanity checks
        // No null collections
        if (isCollection && (obj == null))
            throw new IllegalArgumentException("Collection is null");
        if (isCollection && !(obj instanceof Collection))
            throw new IllegalArgumentException("Not a Collection");
        // No null communities
        if (isCommunity && (obj == null))
            throw new IllegalArgumentException("Community is null");
        if (isCommunity && !(obj instanceof Community))
            throw new IllegalArgumentException("Not a Community");

        // Choose the correct table
        String table = null;

        if (isCommunity)       table = "CommunityItemsByDateAccessioned";
        else if (isCollection) table = "CollectionItemsByDateAccessioned";
        else                   table = "ItemsByDateAccessioned";

        int community_id = (isCommunity) ? ((Community) obj).getID() : -1;
        int collection_id = (isCollection) ? ((Collection) obj).getID() : -1;

        // Generate the SQL
        String sql = new StringBuffer()
            .append("select * from ")
            .append(table)
            .append(isCommunity ? " where community_id = " : "")
            .append(isCommunity ? Integer.toString(community_id) : "")
            .append(isCollection ? " where collection_id = " : "")
            .append(isCollection ? Integer.toString(collection_id) : "")
            // Postgres-specific function
            .append(total == -1 ? "" : " LIMIT ")
            .append(total == -1 ? "" : Integer.toString(total))
            .toString();

        Context context = scope.getContext();
        List results = DatabaseManager.query(context, table, sql).toList();

        // Skip processing if no results
        if ((results == null) || (results.isEmpty()))
            return Collections.EMPTY_LIST;

        // Form a list of items
        List items = new ArrayList();

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

        for (Iterator iterator = BrowseTables.tables().iterator();
             iterator.hasNext(); )
        {
            String table= (String) iterator.next();
            String query = MessageFormat.format(sql, new String[] { table });
            DatabaseManager.updateQuery(context, query);
        }
    }

    /**
     * This method should be called whenever an item has changed:
     *   + DC values are added, removed, or modified
     *   + the value of the in_archive flag changes
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
     * @param context - The database context
     * @return - The number of items indexed.
     * @exception SQLException - If a database error occurs
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
     * @param context - The database context
     * @exception SQLException - If a database error occurs
     */
    public static void indexRemoveAll(Context context)
        throws SQLException
    {
        for (Iterator iterator = BrowseTables.tables().iterator();
             iterator.hasNext(); )
        {
            String table= (String) iterator.next();
            DatabaseManager.updateQuery(context, "delete from " + table);
        }
    }

    ////////////////////////////////////////
    // Other methods
    ////////////////////////////////////////
    public static String getNormalizedTitle(String title, String lang)
    {
        return NormalizedTitle.normalize(title, lang);
    }

    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////

    /*
     * Internal implementation method.
     *
     * @param scope - The Browse scope
     * @param browseType - The type of Browse
     * @param ascending - If true, results will be in lexographic order;
     *   otherwise, results will be in reverse lexographic order.
     * @param sort - If true, results will be sorted by title;
     *   otherwise, results will be sorted by date of issue.
     * @return - A BrowseInfo object, the results of the browse
     * @exception SQLException - If a database error occurs
     */
    private static BrowseInfo getSomethingInternal(BrowseScope scope,
                                                   int browseType,
                                                   boolean ascending,
                                                   Boolean sort)
        throws SQLException
    {
        Context context  = scope.getContext();
        Object obj       = scope.getScope();
        int scopeType    = getScope(obj);
        Object value     = scope.getFocus();
        int numberBefore = scope.getNumberBefore();
        int total        = scope.getTotal();

        ////////////////////
        // Sanity checks
        ////////////////////
        if ((scopeType == COLLECTION_SCOPE) && (obj == null))
            throw new IllegalArgumentException("Collection is null");
        if ((scopeType == COMMUNITY_SCOPE) && (obj == null))
            throw new IllegalArgumentException("Community is null");

        ////////////////////
        // Check for cached browses
        ////////////////////
        BrowseKey key = new BrowseKey
            (obj, value, scopeType, browseType,
             numberBefore, total, ascending, sort);
        BrowseInfo cachedInfo = (BrowseInfo) BrowseCache.get(key);

        if (cachedInfo != null)
        {
            cachedInfo.setCached(true);
            return cachedInfo;
        }

        ////////////////////
        // Convenience booleans
        ////////////////////
        // True if the value is....
        boolean valueIsString = (value instanceof String);
        boolean valueIsItem = (value instanceof Item);
        boolean valueIsInteger = (value instanceof Integer);
        // True if we want to search from the start
        // This is true when the client passed a null value
        boolean searchFromStart = (value == null);
        // True if we want ALL values
        boolean nolimit = (total == -1);
        // True if we need a subquery
        boolean needsSubquery = valueIsInteger || valueIsItem;
        // True IF we are not looking for any previous results
        // This happens when:
        //    * the client explicitly did not ask for any
        //    * we are searching from the start of an index
        //
        // Good to know, since we can potentially skip the query...
        boolean needsNoBefore = (numberBefore == 0) || searchFromStart;

        ////////////////////
        // DB setup
        ////////////////////

        Connection connection = context.getDBConnection();
        // Multiple SQL statements can be made transactionally
        // safe by setting the transaction level appropriately.
        // Essentially, the database guarantees that the application's
        // view of the database is isolated from changes by anyone else.
        // In our case, we are only doing multiple queries, so there are
        // no updating issues.
        //
        // See http://www.postgresql.org/idocs/index.php?xact-serializable.html
        //
        // Performance hit unknown...
        int transactionIsolation = connection.getTransactionIsolation();

        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

        PreparedStatement statement = null;

        ////////////////////
        // The SQL string
        ////////////////////
        // Run a subquery, if necessary
        String subqueryValue = needsSubquery ?
            doSubquery(connection, obj,
                       scopeType, browseType,
                       getItemId(value)) : null;

        if (needsSubquery && log.isInfoEnabled())
            log.info("Got subquery value: \"" + subqueryValue + "\"");

        String sql = createSql(obj, value, scopeType,
                browseType, total,
                subqueryValue, false);

        if (log.isInfoEnabled())
            log.info("Created sql: \"" + sql + "\"");

        // Loop through twice -- once to get items _before_ the value,
        // and once for those after.
        try
        {
            List theResults = new ArrayList();
            int offset = -1;

            for (int i = 0; i < 2; i++)
            {
                boolean before = (i == 0);

                // Short-circuit if we are not looking for any results
                if (before && needsNoBefore)
                {
                    offset = 0;

                    if (log.isDebugEnabled())
                        log.debug("Skipping before query, numberBefore is " + numberBefore + " searchFromStart is " + searchFromStart + " value is " + value);

                    continue;
                }

                // Corner case -- no results desired
                if ((!before) && (total == 0))
                    continue;

                // Format the SQL
                String SQL = formatSql(sql,
                        browseType,
                        before ? numberBefore : total,
                        subqueryValue,
                        before,
                        ascending);

                // Bread crumbs
                if (log.isInfoEnabled())
                    log.info("Formatted " + (before ? "before" : "after") +
                        " sql: \"" + SQL + "\"");

                // Create a PreparedStatement
                statement = connection.prepareStatement(SQL);

                /////////////////////////
                // Bind statement parameters
                /////////////////////////
                String bindValue = ((!searchFromStart) && needsSubquery) ?
                    subqueryValue : (String) value;

                bindParameters(statement, bindValue, searchFromStart, needsSubquery);

                // Run a query, get results
                String table = BrowseTables.getTable(scopeType, browseType);
                List results = DatabaseManager.query(table, statement).toList();

                // Cleanup the statement
                statement.close();

                // Reverse the order of before statements
                if (before)
                    Collections.reverse(results);

                    // Capture the results
                int count = 0;

                if (results != null)
                {
                    for (Iterator iterator = results.iterator(); iterator.hasNext();)
                    {
                        TableRow row = (TableRow) iterator.next();
                        // get the value
                        Object theValue = (browseType == AUTHORS_BROWSE) ?
                            (Object) row.getStringColumn("author") :
                            (Object) new Integer(row.getIntColumn("item_id"));

                        // Add the result
                        //   First make sure it's non-null (this should always be true)
                        if (theValue != null)
                        {
                            // And that we haven't returned more than the desired amount
                            if (nolimit || (theResults.size() < total))
                            {
                                theResults.add(theValue);
                                if (log.isDebugEnabled())
                                    log.debug("Adding result " + theValue);
                            }
                        }

                        count++;
                    }
                }

                // This is the offset of the requested item
                if (before)
                    offset = count;
            }

            // Convert items to fullitems
            if (browseType != AUTHORS_BROWSE)
                 theResults = toItems(context, theResults);

            int resultSize = theResults.size();

            int theTotal = getTotalInIndex(obj, scopeType, browseType, connection, value, total, resultSize);

            // OK, we've counted everything.
            // Now figure out how many things match the specific query.
            // There are times when we can skip this query:
            //   * When we search from the start of an index
            //   * When we got ALL the possible results
            boolean onlyCountTotal = searchFromStart &&
                ((scopeType == ALLDSPACE_SCOPE) && (subqueryValue == null));

            // For all item searches, we need a specific query
            if (valueIsItem || valueIsInteger)
                onlyCountTotal = false;

            int overallPosition = onlyCountTotal ? 0 : -1;

            // We have all the results, no need to query
            if (resultSize >= total)
            {
                onlyCountTotal = true;
                overallPosition = 0;
            }

            if (!onlyCountTotal)
            {
                String countSql = createSql(obj, value, scopeType,
                                            browseType, total,
                                            subqueryValue, true);

                if (log.isInfoEnabled())
                    log.info("Count sql: \"" + countSql + "\"");

                String SQL = formatSql(countSql,
                        browseType,
                        total,
                        subqueryValue,
                        false,
                        ascending);

                if (log.isInfoEnabled())
                    log.info("Formatted count sql: \"" + SQL + "\"");

                statement = connection.prepareStatement(SQL);

                //////////
                // Bind PreparedStatement parameters
                //////////
                String svalue = valueIsString ? (String) value : subqueryValue;

                bindParameters(statement, svalue, searchFromStart, needsSubquery);

                //////////
                // Run the query, extract results
                //////////
                int matches = getIntValue(statement);

                // The overall position is the total minus the number that
                // matched the query minus the number of previous matches.
                overallPosition = (total == matches) ? 0 : (total - matches - offset);

                if (log.isInfoEnabled())
                    log.info("Matches: Got " + matches + " matches for query");

                // FIXME: I think this fixes the symptom, not the problem
                if (overallPosition < 0)
                    overallPosition = 0;

                    // Cleanup the statement
                statement.close();
            }

            //  FIXME -- sorting is busted
            BrowseInfo info = new BrowseInfo
                (theResults, overallPosition,
                 theTotal, offset);

            logInfo(info);

            BrowseCache.add(key, info);
            return info;
        }
        finally
        {
            if (statement != null)
                statement.close();
            if (connection != null)
            {
                // Commit the connection - this tells the DB that we're
                // done with queries. We need to do this so that the
                // transactionIsolation level will be restored.
                // connection.commit();

                // Restore settings
                // Need to do this because "closing" the connection simply
                // returns it to the pool
                connection.setTransactionIsolation(transactionIsolation);
                // connection.close();
            }
        }
    }

    /**
     * Create a SQL string to run the correct query.
     * The string is parameterized in a bazillion ways; this is admittedly
     * quite difficult to follow.
     */
    private static String createSql(Object obj,
        Object value,
        int scope,
        int browseType,
        int total,
        String subqueryValue,
        boolean isCount
    )
        throws SQLException
    {
        String tablename = BrowseTables.getTable(scope, browseType);
        String column = BrowseTables.getValueColumn(browseType);

        StringBuffer sqlb = new StringBuffer()
            .append("select ")
            .append(isCount ? "count(" : "")
            .append(getTargetColumns(browseType))
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
        if (value != null)
        {
            boolean needsSubquery = (value instanceof Integer) ||
                (value instanceof Item);
            String subquery = (subqueryValue == null ? null :
                               getSubqueryClause(column, getItemId(value)));

            if (log.isDebugEnabled())
                log.debug("Subquery is \"" + subquery + "\"");

            sqlb
                .append(" where ")
                .append("(")
                .append(column)
                // Operator is a parameter
                .append(" {1} ")
                .append("?")
                .append(needsSubquery ? subquery : "")
                .append(")");

            addedWhereClause = true;
        }

        addScopeClause(sqlb, scope, obj,
                       addedWhereClause ? " and " : " where ");

        // For counting, skip the "order by" and "limit" clauses
        if (isCount)
            return sqlb.toString();

        // Add an order by clause -- a parameter
        sqlb.append(" order by ").append(column).append("{2}")
            // If an item, make sure it's ordered by item_id as well
        .append(((value instanceof String) || (browseType == AUTHORS_BROWSE))
            ? "" : ", item_id");

        // A limit on the total returned (Postgres extension)
        // This is a parameter
        if (total != -1)
            sqlb.append(" LIMIT {3} ");

        return sqlb.toString();
    }

    /**
     * Return the total number of values in an index.
     * Although this may look a bit dizzying, the basic idea is
     * straightforward:
     *
     * We total Authors with SQL like:
     *    select count(distinct author) from ItemsByAuthor;
     * ItemsByAuthor with:
     *    select count(*) from ItemsByAuthor where author = ?;
     * and every other index with:
     *    select count(*) from ItemsByTitle;
     *
     * If limiting to a community or collection, we add a clause like:
     *    community_id = 7
     *    collection_id = 201
     */
    private static int getTotalInIndex(Object obj,
                                       int scopeType,
                                       int browseType,
                                       Connection connection,
                                       Object value,
                                       int total,
                                       int resultSize)
        throws SQLException
    {
        if ((browseType == ITEMS_BY_AUTHOR_BROWSE) && (total > resultSize))
            return resultSize;

        PreparedStatement statement = null;

        try
        {
            String table = BrowseTables.getTable(scopeType, browseType);

            StringBuffer buffer = new StringBuffer()
                .append("select count(")
                .append(getTargetColumns(browseType))
                .append(") from ")
                .append(table);

            boolean hasWhere = false;
            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            {
                hasWhere = true;
                buffer.append(" where author = ?");
            }

            String sql = addScopeClause(buffer, scopeType, obj,
                                        hasWhere ? "and" : "where")
                .toString();

            if (log.isInfoEnabled())
                log.info("Total sql: \"" + sql + "\"");

            statement = connection.prepareStatement(sql);

            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
                statement.setString(1, (String) value);

            return getIntValue(statement);
        }
        finally
        {
            if (statement != null)
                statement.close();
        }
    }

    /**
     * Format SQL according to BROWSETYPE.
     *
     * The different browses use different operators.
     *
     */
    private static String formatSql(String sql,
        int browseType,
        int numberDesired,
        String subqueryValue,
        boolean before,
        boolean ascending)
    {
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
     * Bind PreparedStatement parameters
     */
    private static void bindParameters(PreparedStatement statement,
                                       String value,
                                       boolean searchFromStart,
                                       boolean needsSubquery
    )
        throws SQLException
    {
        if (searchFromStart)
            return;

        if (needsSubquery)
        {
            if (log.isDebugEnabled())
                log.debug("Binding subquery value \"" + value + "\"");

            statement.setString(1, value);
            statement.setString(2, value);
        }
        else
        {
            if (log.isDebugEnabled())
                log.debug("Binding value \"" + value + "\"");

            statement.setString(1, (String) value);
        }
    }

    /**
     * Run a query to find a browse index value for item_id.
     *
     * In general, these queries look like this:
     *   select max(date_issued) from ItemsByDate where item_id = 7;
     *
     * The max operator ensures that only one value is returned.
     *
     * If limiting to a community or collection, we add a clause like:
     *    community_id = 7
     *    collection_id = 201
     */
    private static String doSubquery(Connection connection,
        Object obj,
        int scope,
        int browseType,
        int item_id
    )
        throws SQLException
    {
        PreparedStatement statement = null;
        ResultSet results = null;

        try
        {
            String tablename = BrowseTables.getTable(scope, browseType);
            String column = BrowseTables.getValueColumn(browseType);

            StringBuffer buffer = new StringBuffer()
                .append("select ")
                .append("max(")
                .append(column)
                .append(") from ")
                .append(tablename)
                .append(" where ")
                .append(" item_id = ")
                .append(item_id);

            String itemValueQuery = addScopeClause(buffer, scope, obj, "and")
                .toString();

            statement = connection.prepareStatement(itemValueQuery);
            results = statement.executeQuery();
            return results.next() ? results.getString(1) : null;
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
    private static String getTargetColumns(int browseType)
    {
        return (browseType == AUTHORS_BROWSE) ? "distinct author" : "*";
    }

    /**
     * Return a subquery clause.
     * The SQL looks like this:
     *   or (sort_title = ? and item_id {0} 3)
     */
    private static String getSubqueryClause(String column, int item_id)
    {
        return new StringBuffer()
            .append(" or ( ")
            .append(column)
            // Item id must be before or after the desired item
            .append(" = ? and item_id {0}  ")
            .append(item_id)
            .append(")")
            .toString();
    }

    /**
     * Return the id from VALUE, which is either an Item or an Integer
     */
    static int getItemId(Object value)
    {
        if (value instanceof Integer)
            return ((Integer) value).intValue();
        else if (value instanceof Item)
            return ((Item) value).getID();

        throw new IllegalArgumentException("Value must be Integer or Item");
    }

    /**
     * Add a scoping clause to BUFFER.
     *
     * If scope is ALLDSPACE_SCOPE, nothing is added.
     *
     * Otherwise, the SQL clause which is appended looks like:
     *    PREFIX community_id = 7
     *    PREFIX collection_id = 203
     *
     * PREFIX may be empty, or it may be a SQL keyword like "where", "and",
     * and so forth.
     */
    static StringBuffer addScopeClause(StringBuffer buffer,
                                       int scope,
                                       Object obj,
                                       String prefix)
    {
        if (scope == ALLDSPACE_SCOPE)
            return buffer;

        int id = (obj instanceof Community) ?
            ((Community) obj).getID() : ((Collection) obj).getID();

        String column = (scope == COMMUNITY_SCOPE) ?
            "community_id" : "collection_id";

        return buffer
            .append(prefix)
            .append(" ")
            .append(column)
            .append(" = ")
            .append(id);
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
     * Convert a list of item ids to full Items
     */
    private static List toItems(Context context, List ids)
        throws SQLException
    {
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

    /**
     * Return the browse scope.
     */
    private static int getScope(Object scopeObj)
    {
        if (scopeObj instanceof Community)
            return COMMUNITY_SCOPE;
        if (scopeObj instanceof Collection)
            return COLLECTION_SCOPE;

        return ALLDSPACE_SCOPE;
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

        return (lang.startsWith("en")) ? normalizeEnglish(title)
            : title;
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
     *
     * The main program shows simple results of this algorithm.
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

//  FIXME Use BrowseScope instead?
class BrowseKey
{
    public Object obj;
    public Object value;
    public int scope;
    public int browseType;
    public int numberBefore;
    public int total;
    public boolean ascending;
    public Boolean sort;

    /**
     * Constructor
     */
    public BrowseKey
    (
     Object obj,
     Object value,
    int scope,
    int browseType,
    int numberBefore,
    int total,
    boolean ascending,
    Boolean sort
    )
    {
        this.obj = obj;
        this.value = value;
        this.scope = scope;
        this.browseType = browseType;
        this.numberBefore = numberBefore;
        this.total = total;
        this.ascending = ascending;
        this.sort = sort;
    }

    /*
     * Return true if this object is equal to OTHER, false otherwise
     */
    public boolean equals(Object other)
    {
        if (! (other instanceof BrowseKey))
            return false;

        BrowseKey theOther = (BrowseKey) other;

        return
            (obj != null ? obj.equals(theOther.obj) : theOther.obj == null) &&
            (value != null ? value.equals(theOther.value) : theOther.value == null) &&
            (scope == theOther.scope) &&
            (browseType == theOther.browseType) &&
            (numberBefore == theOther.numberBefore) &&
            (total == theOther.total) &&
            (ascending == theOther.ascending) &&
            (sort != null ? sort.equals(theOther.sort) : theOther.sort == null)
            ;
    }

    /*
     * Return a hashCode for this object.
     */
    public int hashCode()
    {
        return new StringBuffer()
            .append(obj)
            .append(value)
            .append(scope)
            .append(browseType)
            .append(numberBefore)
            .append(total)
            .append(ascending)
            .append(sort)
            .toString().hashCode();
    }
}

class BrowseCache
{
    private static int[] MAXIMUM  = new int[BrowseTables.count()];
    private static long[] COUNT   = new long[BrowseTables.count()];

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
    public static BrowseInfo get(BrowseKey key)
    {
        if (log.isDebugEnabled())
            log.debug("Checking browse cache with " + cache.size() + " objects");
        BrowseInfo cachedInfo = (BrowseInfo) cache.get(key);

        try
        {
            if (firstTimeThrough(key))
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

        return cachedInfo;
    }

    /**
     * Return true if an index has changed
     */
    public static boolean indexHasChanged(BrowseKey key)
        throws SQLException
    {
        Context context = null;
        try
        {
            int tableid  = BrowseTables.getTableId(key.scope, key.browseType);

            context = new Context();
            TableRow results = countAndMax(context, key);
            long count = results != null ? results.getLongColumn("count") : -1;
            int max    = results != null ? results.getIntColumn("max")    : -1;
            context.complete();

            // Same?
            if ((count == COUNT[tableid]) && (max == MAXIMUM[tableid]))
                return false;

            // Update the counts
            MAXIMUM[tableid] = max;
            COUNT[tableid]   = count;

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
     * Return true if the index has never been calculated
     */
    public static boolean firstTimeThrough(BrowseKey key)
        throws SQLException
    {
        return MAXIMUM[BrowseTables.getTableId(key.scope, key.browseType)] == 0;
    }

    /**
     * Return true if an index has changed
     */
    public static void updateIndexData(BrowseKey key)
        throws SQLException
    {
        Context context = null;

        try
        {
            int tableid  = BrowseTables.getTableId(key.scope, key.browseType);

            context = new Context();
            TableRow results = countAndMax(context, key);
            long count = results != null ? results.getLongColumn("count") : -1;
            int max    = results != null ? results.getIntColumn("max")    : -1;
            context.complete();

            MAXIMUM[tableid] = max;
            COUNT[tableid]   = count;
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();

            e.printStackTrace();
        }
    }

    /**
     * Return the values for count and max
     */
    public static TableRow countAndMax(Context context, BrowseKey key)
        throws SQLException
    {
        Object obj     = key.obj;
        int scope      = key.scope;
        int browseType = key.browseType;

        // The basic idea here is that we'll check an indexes
        // maximum id and its count: if the maximum id has changed,
        // then there are new values, and if the count has changed,
        // then some items may have been removed. We assume that
        // values never change.

        StringBuffer buffer = new StringBuffer()
            .append("select count({0}) as count, max({0}) as max from ")
            .append(BrowseTables.getTable(scope, browseType));

        String sql = Browse.addScopeClause(buffer, scope, obj, "where")
            .toString();

        // Format it to use the correct columns
        String countColumn = BrowseTables.getIndexColumn(browseType);
        Object[] args = new Object[] {countColumn, countColumn};
        String SQL = MessageFormat.format(sql, args);

        // Run the query
        if (log.isDebugEnabled())
            log.debug("Running SQL to check whether index has changed: \"" + SQL + "\"");

        return DatabaseManager.querySingle(context, null, SQL);
    }

    /**
     * Add INFO to cache, using KEY
     */
    public static void add(BrowseKey key, BrowseInfo info)
    {
        // Don't bother caching browses with no results, they are
        // fairly cheap to calculate
        if (info.getResultCount() == 0)
            return;

        // Add the info to the cache
        cache.put(key, info);
        // Make sure the date cache is up to date
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
}

// Encapsulates browse table info:
//   * Each scope and browsetype has a corresponding table or view
//   * Each browse table or view has a value column
//   * Some of the browse tables are true tables, others are views.
//     The index maintenance code needs to know the true tables.
//     The true tables have index columns, which can be used for caching
class BrowseTables
{
    static String[][] TABLENAME = new String[3][4];

    static
    {
        TABLENAME[Browse.ALLDSPACE_SCOPE][Browse.AUTHORS_BROWSE] = "ItemsByAuthor";
        TABLENAME[Browse.COMMUNITY_SCOPE][Browse.AUTHORS_BROWSE] = "CommunityItemsByAuthor";
        TABLENAME[Browse.COLLECTION_SCOPE][Browse.AUTHORS_BROWSE] = "CollectionItemsByAuthor";

        TABLENAME[Browse.ALLDSPACE_SCOPE][Browse.ITEMS_BY_TITLE_BROWSE] = "ItemsByTitle";
        TABLENAME[Browse.COMMUNITY_SCOPE][Browse.ITEMS_BY_TITLE_BROWSE] = "CommunityItemsByTitle";
        TABLENAME[Browse.COLLECTION_SCOPE][Browse.ITEMS_BY_TITLE_BROWSE] = "CollectionItemsByTitle";

        TABLENAME[Browse.ALLDSPACE_SCOPE][Browse.ITEMS_BY_AUTHOR_BROWSE] = "ItemsByAuthor";
        TABLENAME[Browse.COMMUNITY_SCOPE][Browse.ITEMS_BY_AUTHOR_BROWSE] = "CommunityItemsByAuthor";
        TABLENAME[Browse.COLLECTION_SCOPE][Browse.ITEMS_BY_AUTHOR_BROWSE] = "CollectionItemsByAuthor";

        TABLENAME[Browse.ALLDSPACE_SCOPE][Browse.ITEMS_BY_DATE_BROWSE] = "ItemsByDate";
        TABLENAME[Browse.COMMUNITY_SCOPE][Browse.ITEMS_BY_DATE_BROWSE] = "CommunityItemsByDate";
        TABLENAME[Browse.COLLECTION_SCOPE][Browse.ITEMS_BY_DATE_BROWSE] = "CollectionItemsByDate";
    }

    private static final String[] ALL_TABLES =
    {
        "ItemsByTitle",
        "CommunityItemsByTitle",
        "CollectionItemsByTitle",
        "ItemsByAuthor",
        "CommunityItemsByAuthor",
        "CollectionItemsByAuthor",
        "ItemsByDate",
        "CommunityItemsByDate",
        "CollectionItemsByDate"
    };

    public static final List BROWSE_TABLES =
        Collections.unmodifiableList(Arrays.asList( new String[]
        {
            "ItemsByDateAccessioned",
            "ItemsByDate",
            "ItemsByTitle",
            "ItemsByAuthor"
        }));

    /**
     * Return the total number of browse tables and views
     */
    public static int count()
    {
        return ALL_TABLES.length;
    }

    /**
     * Return an index between 0 and count() for TABLENAME.
     */
    public static int getTableId(int scope, int browseType)
    {
        return getTableId(getTable(scope, browseType));
    }

    /**
     * Return an index between 0 and count() for TABLENAME.
     */
    public static int getTableId(String tablename)
    {
        for (int i = 0; i < ALL_TABLES.length; i++ )
        {
            if (ALL_TABLES[i].equals(tablename))
                return i;
        }

        return -1;
    }

    /**
     * Return the browse tables. This only returns true tables,
     * views are ignored.
     * The returned List is unmodifiable.
     * Each element of the returned List is a String.
     */
    public static List tables()
    {
        return BROWSE_TABLES;
    }


    /**
     * Return the total number of browse tables only
     */
    public static int tableCount()
    {
        return BROWSE_TABLES.size();
    }

    /**
     * Return the browse table or view for SCOPE and TYPE.
     */
    public static String getTable(int scope, int browseType)
    {
        return TABLENAME[scope][browseType];
    }

    /**
     * Return the name of the column that holds the index
     */
    public static String getIndexColumn(int browseType)
    {
        if (browseType == Browse.AUTHORS_BROWSE)
            return "items_by_author_id";
        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
            return "items_by_title_id";
        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
            return "items_by_author_id";
        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
            return "items_by_date_id";

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }

    /**
     * Return the name of the column that holds the Browse value
     * (the title, author, date, etc).
     */
    public static String getValueColumn(int browseType)
    {
        if (browseType == Browse.AUTHORS_BROWSE)
            return "author";
        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
            return "sort_title";
        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
            return "author";
        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
            return "date_issued";

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }
}
