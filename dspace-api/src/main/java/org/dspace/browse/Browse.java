/*
 * Browse.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemComparator;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * API for Browsing Items in DSpace by title, author, or date. Browses only
 * return archived Items.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class Browse
{
    // Browse types
    static final int AUTHORS_BROWSE = 0;

    static final int ITEMS_BY_TITLE_BROWSE = 1;

    static final int ITEMS_BY_AUTHOR_BROWSE = 2;

    static final int ITEMS_BY_DATE_BROWSE = 3;

    static final int SUBJECTS_BROWSE = 4;

    static final int ITEMS_BY_SUBJECT_BROWSE = 5;

    
    /** Log4j log */
    private static Logger log = Logger.getLogger(Browse.class);

    /**
     * Constructor
     */
    private Browse()
    {
    }

    /**
     * Return distinct Authors in the given scope. Author refers to a Dublin
     * Core field with element <em>contributor</em> and qualifier
     * <em>author</em>.
     * 
     * <p>
     * Results are returned in alphabetical order.
     * </p>
     * 
     * @param scope
     *            The BrowseScope
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getAuthors(BrowseScope scope) throws SQLException
    {
        scope.setBrowseType(AUTHORS_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(null);

        return doBrowse(scope);
    }
    /**
     * Return distinct Subjects in the given scope. Subjects refers to a Dublin
     * Core field with element <em>subject</em> and qualifier
     * <em>*</em>.
     *
     * <p>
     * Results are returned in alphabetical order.
     * </p>
     *
     * @param scope
     *            The BrowseScope
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getSubjects(BrowseScope scope) throws SQLException
    {
        scope.setBrowseType(SUBJECTS_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(null);

        return doBrowse(scope);
    }

    /**
     * Return Items indexed by title in the given scope. Title refers to a
     * Dublin Core field with element <em>title</em> and no qualifier.
     * 
     * <p>
     * Results are returned in alphabetical order; that is, the Item with the
     * title which is first in alphabetical order will be returned first.
     * </p>
     * 
     * @param scope
     *            The BrowseScope
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getItemsByTitle(BrowseScope scope)
            throws SQLException
    {
        scope.setBrowseType(ITEMS_BY_TITLE_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(null);

        return doBrowse(scope);
    }

    /**
     * Return Items indexed by date in the given scope. Date refers to a Dublin
     * Core field with element <em>date</em> and qualifier <em>issued</em>.
     * 
     * <p>
     * If oldestfirst is true, the dates returned are the ones after the focus,
     * ordered from earliest to latest. Otherwise the dates are the ones before
     * the focus, and ordered from latest to earliest. For example:
     * </p>
     * 
     * <p>
     * For example, if the focus is <em>1995</em>, and oldestfirst is true,
     * the results might look like this:
     * </p>
     * 
     * <code>1993, 1994, 1995 (the focus), 1996, 1997.....</code>
     * 
     * <p>
     * While if the focus is <em>1995</em>, and oldestfirst is false, the
     * results would be:
     * </p>
     * 
     * <code>1997, 1996, 1995 (the focus), 1994, 1993 .....</code>
     * 
     * @param scope
     *            The BrowseScope
     * @param oldestfirst
     *            If true, the dates returned are the ones after focus, ordered
     *            from earliest to latest; otherwise the dates are the ones
     *            before focus, ordered from latest to earliest.
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getItemsByDate(BrowseScope scope,
            boolean oldestfirst) throws SQLException
    {
        scope.setBrowseType(ITEMS_BY_DATE_BROWSE);
        scope.setAscending(oldestfirst);
        scope.setSortByTitle(null);

        return doBrowse(scope);
    }

    /**
     * <p>
     * Return Items in the given scope by the author (exact match). The focus of
     * the BrowseScope is the author to use; using a BrowseScope without a focus
     * causes an IllegalArgumentException to be thrown.
     * </p>
     * 
     * <p>
     * Author refers to a Dublin Core field with element <em>contributor</em>
     * and qualifier <em>author</em>.
     * </p>
     * 
     * @param scope
     *            The BrowseScope
     * @param sortByTitle
     *            If true, the returned items are sorted by title; otherwise
     *            they are sorted by date issued.
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getItemsByAuthor(BrowseScope scope,
            boolean sortByTitle) throws SQLException
    {
        if (!scope.hasFocus())
        {
            throw new IllegalArgumentException(
                    "Must specify an author for getItemsByAuthor");
        }

        if (!(scope.getFocus() instanceof String))
        {
            throw new IllegalArgumentException(
                    "The focus for getItemsByAuthor must be a String");
        }

        scope.setBrowseType(ITEMS_BY_AUTHOR_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(sortByTitle ? Boolean.TRUE : Boolean.FALSE);
        scope.setTotalAll();

        return doBrowse(scope);
    }

	/**
     * <p>
     * Return Items in the given scope by the subject (exact match). The focus of
     * the BrowseScope is the subject to use; using a BrowseScope without a focus
     * causes an IllegalArgumentException to be thrown.
     * </p>
     *
     * <p>
     * Subject refers to a Dublin Core field with element <em>subject</em>
     * and qualifier <em>*</em>.
     * </p>
     *
     * @param scope
     *            The BrowseScope
     * @param sortByTitle
     *            If true, the returned items are sorted by title; otherwise
     *            they are sorted by date issued.
     * @return A BrowseInfo object, the results of the browse
     * @exception SQLException
     *                If a database error occurs
     */
    public static BrowseInfo getItemsBySubject(BrowseScope scope,
            boolean sortByTitle) throws SQLException
    {
        if (!scope.hasFocus())
        {
            throw new IllegalArgumentException(
                    "Must specify a subject for getItemsBySubject");
        }

        if (!(scope.getFocus() instanceof String))
        {
            throw new IllegalArgumentException(
                    "The focus for getItemsBySubject must be a String");
        }

        scope.setBrowseType(ITEMS_BY_SUBJECT_BROWSE);
        scope.setAscending(true);
        scope.setSortByTitle(sortByTitle ? Boolean.TRUE : Boolean.FALSE);
        scope.setTotalAll();

        return doBrowse(scope);
    }

    /**
     * Returns the last items submitted to DSpace in the given scope.
     * 
     * @param scope
     *            The Browse Scope
     * @return A List of Items submitted
     * @exception SQLException
     *                If a database error occurs
     */
    public static List getLastSubmitted(BrowseScope scope) throws SQLException
    {
        Context context = scope.getContext();
        String sql = getLastSubmittedQuery(scope);

        if (log.isDebugEnabled())
        {
            log.debug("SQL for last submitted is \"" + sql + "\"");
        }

        List results = DatabaseManager.query(context, sql).toList();

        return getLastSubmittedResults(context, results);
    }

    /**
     * Return the SQL used to determine the last submitted Items for scope.
     * 
     * @param scope
     * @return String query string
     */
    private static String getLastSubmittedQuery(BrowseScope scope)
    {
        String table = getLastSubmittedTable(scope);

        String query = "SELECT * FROM " + table
                + getScopeClause(scope, "where")
                + " ORDER BY date_accessioned DESC";

        if (!scope.hasNoLimit())
        {
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                // Oracle version of LIMIT...OFFSET - must use a sub-query and
                // ROWNUM
                query = "SELECT * FROM (" + query + ") WHERE ROWNUM <="
                        + scope.getTotal();
            }
            else
            {
                // postgres, use LIMIT
                query = query + " LIMIT " + scope.getTotal();
            }
        }

        return query;
    }

    /**
     * Return the name of the Browse index table to query for last submitted
     * items in the given scope.
     * 
     * @param scope
     * @return name of table
     */
    private static String getLastSubmittedTable(BrowseScope scope)
    {
        if (scope.isCommunityScope())
        {
            return "CommunityItemsByDateAccession";
        }
        else if (scope.isCollectionScope())
        {
            return "CollectionItemsByDateAccession";
        }

        return "ItemsByDateAccessioned";
    }

    /**
     * Transform the query results into a List of Items.
     * 
     * @param context
     * @param results
     * @return list of items
     * @throws SQLException
     */
    private static List getLastSubmittedResults(Context context, List results)
            throws SQLException
    {
        if ((results == null) || (results.isEmpty()))
        {
            return Collections.EMPTY_LIST;
        }

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
     * @param context
     *            The current DSpace context
     * @param id
     *            The id of the item which has been removed
     * @exception SQLException
     *                If a database error occurs
     */
    public static void itemRemoved(Context context, int id) throws SQLException
    {
        String sql = "delete from {0} where item_id = " + id;

        String[] browseTables = BrowseTables.tables();

        for (int i = 0; i < browseTables.length; i++)
        {
            String query = MessageFormat.format(sql, browseTables[i]);
            DatabaseManager.updateQuery(context, query);
        }
    }

    /**
     * This method should be called whenever an item has changed. Changes
     * include:
     * 
     * <ul>
     * <li>DC values are added, removed, or modified
     * <li>the value of the in_archive flag changes
     * </ul>
     * 
     * @param context -
     *            The database context
     * @param item -
     *            The item which has been added
     * @exception SQLException -
     *                If a database error occurs
     */
    public static void itemChanged(Context context, Item item)
            throws SQLException
    {
        // This is a bit heavy-weight, but without knowing what
        // changed, it's easiest just to replace the values
        // en masse.
        itemRemoved(context, item.getID());

        if (!item.isArchived())
        {
            return;
        }

        itemAdded(context, item);
    }

    /**
     * This method should be called whenever an item is added.
     * 
     * @param context
     *            The current DSpace context
     * @param item
     *            The item which has been added
     * @exception SQLException
     *                If a database error occurs
     */
    public static void itemAdded(Context context, Item item)
            throws SQLException
    {
        // add all parent communities to communities2item table
        Community[] parents = item.getCommunities();

        for (int j = 0; j < parents.length; j++)
        {
            TableRow row = DatabaseManager.create(context, "Communities2Item");
            row.setColumn("item_id", item.getID());
            row.setColumn("community_id", parents[j].getID());
            DatabaseManager.update(context, row);
        }

        // get the metadata fields to index in the title and date tables
        
        // get the date, title and author fields
        String dateField = ConfigurationManager.getProperty("webui.browse.index.date");
        if (dateField == null)
        {
            dateField = "dc.date.issued";
        }
        
        String titleField = ConfigurationManager.getProperty("webui.browse.index.title");
        if (titleField == null)
        {
            titleField = "dc.title";
        }
        
        String authorField = ConfigurationManager.getProperty("webui.browse.index.author");
        if (authorField == null)
        {
            authorField = "dc.contributor.*";
        }
        
        String subjectField = ConfigurationManager.getProperty("webui.browse.index.subject");
        if (subjectField == null)
        {
            subjectField = "dc.subject.*";
        }
        
        // get the DC values for each of these fields
        DCValue[] titleArray = getMetadataField(item, titleField);
        DCValue[] dateArray = getMetadataField(item, dateField);
        DCValue[] authorArray = getMetadataField(item, authorField);
        DCValue[] subjectArray = getMetadataField(item, subjectField);
        
        // now build the data map
        Map table2dc = new HashMap();
        table2dc.put("ItemsByTitle", titleArray);
        table2dc.put("ItemsByAuthor", authorArray);
        table2dc.put("ItemsByDate", dateArray);
        table2dc.put("ItemsByDateAccessioned", item.getDC("date",
                "accessioned", Item.ANY));
        table2dc.put("ItemsBySubject", subjectArray);

        for (Iterator iterator = table2dc.keySet().iterator(); iterator
                .hasNext();)
        {
            String table = (String) iterator.next();
            DCValue[] dc = (DCValue[]) table2dc.get(table);

            for (int i = 0; i < dc.length; i++)
            {
                TableRow row = DatabaseManager.create(context, table);
                row.setColumn("item_id", item.getID());

                String value = dc[i].value;

                if ("ItemsByDateAccessioned".equals(table))
                {
                    row.setColumn("date_accessioned", value);
                }
                else if ("ItemsByDate".equals(table))
                {
                    row.setColumn("date_issued", value);
                }
                else if ("ItemsByAuthor".equals(table))
                {
                    // author name, and normalized sorting name
                    // (which for now is simple lower-case)
                    row.setColumn("author", value);
                    row.setColumn("sort_author", value.toLowerCase());
                }
                else if ("ItemsByTitle".equals(table))
                {
                    String title = NormalizedTitle.normalize(value,
                            dc[i].language);
                    row.setColumn("title", value);
                    row.setColumn("sort_title", title.toLowerCase());
                }
                else if ("ItemsBySubject".equals(table))
                {
                    row.setColumn("subject", value);
                    row.setColumn("sort_subject", value.toLowerCase());
                }


                DatabaseManager.update(context, row);
            }
        }
    }

    /**
     * Index all items in DSpace. This method may be resource-intensive.
     * 
     * @param context
     *            Current DSpace context
     * @return The number of items indexed.
     * @exception SQLException
     *                If a database error occurs
     */
    public static int indexAll(Context context) throws SQLException
    {
        indexRemoveAll(context);

        int count = 0;
        ItemIterator iterator = Item.findAll(context);

        while (iterator.hasNext())
        {
            itemAdded(context, iterator.next());
            count++;
        }

        return count;
    }

    /**
     * Remove all items in DSpace from the Browse index.
     * 
     * @param context
     *            Current DSpace context
     * @return The number of items removed.
     * @exception SQLException
     *                If a database error occurs
     */
    public static int indexRemoveAll(Context context) throws SQLException
    {
        int total = 0;

        String[] browseTables = BrowseTables.tables();

        for (int i = 0; i < browseTables.length; i++)
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
     * 
     * @param title
     * @param lang
     * @return title
     */
    public static String getNormalizedTitle(String title, String lang)
    {
        return NormalizedTitle.normalize(title, lang);
    }

    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////

    private static DCValue[] getMetadataField(Item item, String md)
    {
        StringTokenizer dcf = new StringTokenizer(md, ".");
        
        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().toLowerCase().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];
        
        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = item.getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = item.getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = item.getMetadata(schema, element, qualifier, Item.ANY);
        }
        
        return values;
    }
    
    /**
     * Workhorse method for browse functionality.
     * 
     * @param scope
     * @return BrowseInfo
     * @throws SQLException
     */
    private static BrowseInfo doBrowse(BrowseScope scope) throws SQLException
    {
        // Check for a cached browse
        BrowseInfo cachedInfo = BrowseCache.get(scope);

        if (cachedInfo != null)
        {
            return cachedInfo;
        }

        // Run the Browse queries
        // If the focus is an Item, this returns the value
        String itemValue = getItemValue(scope);
        List results = new ArrayList();
        results.addAll(getResultsBeforeFocus(scope, itemValue));

        int beforeFocus = results.size();
        results.addAll(getResultsAfterFocus(scope, itemValue, beforeFocus));

        // Find out the total in the index, and the number of
        // matches for the query
        int total = countTotalInIndex(scope, results.size());
        int matches = countMatches(scope, itemValue, total, results.size());

        if (log.isDebugEnabled())
            {
            log.debug("Number of matches " + matches);
        }

        int position = getPosition(total, matches, beforeFocus);

        sortResults(scope, results);

            BrowseInfo info = new BrowseInfo(results, position, total,
                    beforeFocus);

        logInfo(info);

        BrowseCache.add(scope, info);

        return info;
    }

    /**
     * If focus refers to an Item, return a value for the item (its title,
     * author, accession date, etc). Otherwise return null.
     * 
     * In general, the queries for these values look like this: select
     * max(date_issued) from ItemsByDate where item_id = 7;
     * 
     * The max operator ensures that only one value is returned.
     * 
     * If limiting to a community or collection, we add a clause like:
     * community_id = 7 collection_id = 201
     * 
     * @param scope
     * @return desired value for the item
     * @throws SQLException
     */
    protected static String getItemValue(BrowseScope scope) throws SQLException
    {
        if (!scope.focusIsItem())
        {
            return null;
        }

        PreparedStatement statement = null;
        ResultSet results = null;

        try
        {
            String tablename = BrowseTables.getTable(scope);
            String column = BrowseTables.getValueColumn(scope);

            String itemValueQuery = new StringBuffer().append("select ")
                    .append("max(").append(column).append(") from ").append(
                            tablename).append(" where ").append(" item_id = ")
                    .append(scope.getFocusItemId()).append(
                            getScopeClause(scope, "and")).toString();

            statement = createStatement(scope, itemValueQuery);
            results = statement.executeQuery();

            String itemValue = results.next() ? results.getString(1) : null;

            if (log.isDebugEnabled())
            {
                log.debug("Subquery value is " + itemValue);
            }

            return itemValue;
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }

            if (results != null)
            {
                results.close();
            }
        }
    }

    /**
     * Run a database query and return results before the focus.
     * 
     * @param scope
     *            The Browse Scope
     * @param itemValue
     *            If the focus is an Item, this is its value in the index (its
     *            title, author, etc).
     * @return list of Item results
     * @throws SQLException
     */
    protected static List getResultsBeforeFocus(BrowseScope scope,
            String itemValue) throws SQLException
    {
        // Starting from beginning of index
        if (!scope.hasFocus())
        {
            return Collections.EMPTY_LIST;
        }

        // No previous results desired
        if (scope.getNumberBefore() == 0)
        {
            return Collections.EMPTY_LIST;
        }

        // ItemsByAuthor. Since this is an exact match, it
        // does not make sense to return values before the
        // query.
        if (scope.getBrowseType() == ITEMS_BY_AUTHOR_BROWSE
                || scope.getBrowseType() == ITEMS_BY_SUBJECT_BROWSE)
        {
            return Collections.EMPTY_LIST;
        }

        PreparedStatement statement = createSql(scope, itemValue, false, false);

        List qresults = DatabaseManager.queryPrepared(statement).toList();
        int numberDesired = scope.getNumberBefore();
        List results = getResults(scope, qresults, numberDesired);

        if (!results.isEmpty())
        {
            Collections.reverse(results);
        }

        return results;
    }

    /**
     * Run a database query and return results after the focus.
     * 
     * @param scope
     *            The Browse Scope
     * @param itemValue
     *            If the focus is an Item, this is its value in the index (its
     *            title, author, etc).
     * @param count
     * @return list of results after the focus
     * @throws SQLException
     */
    protected static List getResultsAfterFocus(BrowseScope scope,
            String itemValue, int count) throws SQLException
    {
        // No results desired
        if (scope.getTotal() == 0)
        {
            return Collections.EMPTY_LIST;
        }

        PreparedStatement statement = createSql(scope, itemValue, true, false);

        List qresults = DatabaseManager.queryPrepared(statement).toList();

        // The number of results we want is either -1 (everything)
        // or the total, less the number already retrieved.
        int numberDesired = -1;

        if (!scope.hasNoLimit())
        {
            numberDesired = Math.max(scope.getTotal() - count, 0);
        }

        return getResults(scope, qresults, numberDesired);
    }

    /*
     * Return the total number of values in an index.
     * 
     * <p> We total Authors with SQL like: select count(distinct author) from
     * ItemsByAuthor; ItemsByAuthor with: select count(*) from ItemsByAuthor
     * where author = ?; and every other index with: select count(*) from
     * ItemsByTitle; </p>
     * 
     * <p> If limiting to a community or collection, we add a clause like:
     * community_id = 7 collection_id = 201 </p>
     */
    protected static int countTotalInIndex(BrowseScope scope,
            int numberOfResults) throws SQLException
    {
        int browseType = scope.getBrowseType();

        // When finding Items by Author, it often happens that
        // we find every single Item (eg, the Author only published
        // 2 works, and we asked for 15), and so can skip the
        // query.
        if ((browseType == ITEMS_BY_AUTHOR_BROWSE)
                && (scope.hasNoLimit() || (scope.getTotal() > numberOfResults)))
        {
            return numberOfResults;
        }

        PreparedStatement statement = null;
        Object obj = scope.getScope();

        try
        {
            String table = BrowseTables.getTable(scope);

            StringBuffer buffer = new StringBuffer().append("select count(")
                    .append(getTargetColumns(scope)).append(") from ").append(
                            table);

            boolean hasWhere = false;

            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            {
                hasWhere = true;
                buffer.append(" where sort_author = ?");
            }

            if (browseType == ITEMS_BY_SUBJECT_BROWSE)
            {
                hasWhere = true;
                buffer.append(" where sort_subject = ?");
            }

            String connector = hasWhere ? "and" : "where";
            String sql = buffer.append(getScopeClause(scope, connector))
                    .toString();

            if (log.isDebugEnabled())
            {
                log.debug("Total sql: \"" + sql + "\"");
            }

            statement = createStatement(scope, sql);

            if (browseType == ITEMS_BY_AUTHOR_BROWSE)
            {
                statement.setString(1, (String) scope.getFocus());
            }

            if (browseType == ITEMS_BY_SUBJECT_BROWSE)
            {
                statement.setString(1, (String) scope.getFocus());
            }

            return getIntValue(statement);
        }
        finally
        {
            if (statement != null)
            {
                statement.close();
            }
        }
    }

    /**
     * Return the number of matches for the browse scope.
     * 
     * @param scope
     * @param itemValue
     *            item value we're looking for
     * @param totalInIndex
     *            FIXME ??
     * @param numberOfResults
     *            FIXME ??
     * @return number of matches
     * @throws SQLException
     */
    protected static int countMatches(BrowseScope scope, String itemValue,
            int totalInIndex, int numberOfResults) throws SQLException
    {
        // Matched everything
        if (numberOfResults == totalInIndex)
        {
            return totalInIndex;
        }

        // Scope matches everything in the index
        // Note that this only works when the scope is all of DSpace,
        // since the Community and Collection index tables
        // include Items in other Communities/Collections
        if ((!scope.hasFocus()) && scope.isAllDSpaceScope())
        {
            return totalInIndex;
        }

        PreparedStatement statement = null;
        try {
        	statement = createSql(scope, itemValue, true, true);
        	return getIntValue(statement);
        }
        finally {
        	if(statement != null) {
        		try {
        			statement.close();
        		}
        		catch(SQLException e) {
        			log.error("Problem releasing statement", e);
        		}
        	}
        }
    }

    private static int getPosition(int total, int matches, int beforeFocus)
    {
        // Matched everything, so position is at start (0)
        if (total == matches)
        {
            return 0;
        }

        return total - matches - beforeFocus;
    }

    /**
     * Sort the results returned from the browse if necessary. The list of
     * results is sorted in-place.
     * 
     * @param scope
     * @param results
     */
    private static void sortResults(BrowseScope scope, List results)
    {
        // Currently we only sort ItemsByAuthor, Advisor, Subjects browses
        if ((scope.getBrowseType() != ITEMS_BY_AUTHOR_BROWSE)
                && (scope.getBrowseType() != ITEMS_BY_SUBJECT_BROWSE))
        {
            return;
        }

        ItemComparator ic = scope.getSortByTitle().booleanValue() ? new ItemComparator(
                "title", null, Item.ANY, true)
                : new ItemComparator("date", "issued", Item.ANY, true);

        Collections.sort(results, ic);
    }

    /**
     * Transform the results of the query (TableRow objects_ into a List of
     * Strings (for getAuthors()) or Items (for all the other browses).
     * 
     * @param scope
     *            The Browse Scope
     * @param results
     *            The results of the query
     * @param max
     *            The maximum number of results to return
     * @return FIXME ??
     * @throws SQLException
     */
    private static List getResults(BrowseScope scope, List results, int max)
            throws SQLException
    {
        if (results == null)
        {
            return Collections.EMPTY_LIST;
        }

        List theResults = new ArrayList();
        boolean hasLimit = !scope.hasNoLimit();
        boolean isAuthorsBrowse = scope.getBrowseType() == AUTHORS_BROWSE;
        boolean isSubjectsBrowse = scope.getBrowseType() == SUBJECTS_BROWSE;

        for (Iterator iterator = results.iterator(); iterator.hasNext();)
        {
            TableRow row = (TableRow) iterator.next();
            Object theValue = null;

			if (isAuthorsBrowse)
                theValue = (Object) row.getStringColumn("author");
            else if (isSubjectsBrowse)
                theValue = (Object) row.getStringColumn("subject");
            else
                theValue = (Object) new Integer(row.getIntColumn("item_id"));

            // Should not happen
            if (theValue == null)
            {
                continue;
            }

            // Exceeded limit
            if (hasLimit && (theResults.size() >= max))
            {
                break;
            }

            theResults.add(theValue);

            if (log.isDebugEnabled())
            {
                log.debug("Adding result " + theValue);
            }
        }

        return (isAuthorsBrowse||isSubjectsBrowse)
				 ? theResults : toItems(scope.getContext(), theResults);
    }

    /**
     * Create a PreparedStatement to run the correct query for scope.
     * 
     * @param scope
     *            The Browse scope
     * @param subqueryValue
     *            If the focus is an item, this is its value in the browse index
     *            (its title, author, date, etc). Otherwise null.
     * @param after
     *            If true, create SQL to find the items after the focus.
     *            Otherwise create SQL to find the items before the focus.
     * @param isCount
     *            If true, create SQL to count the number of matches for the
     *            query. Otherwise just the query.
     * @return a prepared statement
     * @throws SQLException
     */
    private static PreparedStatement createSql(BrowseScope scope,
            String subqueryValue, boolean after, boolean isCount)
            throws SQLException
    {
        String sqli = createSqlInternal(scope, subqueryValue, isCount);
        String sql = formatSql(scope, sqli, subqueryValue, after);
        PreparedStatement statement = createStatement(scope, sql);

        // Browses without a focus have no parameters to bind
        if (scope.hasFocus())
        {
        	String value = subqueryValue;
        	if (value == null && scope.getFocus() instanceof String)
        	{
        		value = (String)scope.getFocus();
        	}

        	statement.setString(1, value);

        	// Binds the parameter in the subquery clause
        	if (subqueryValue != null)
        	{
        		statement.setString(2, value);
        	}
        }

        if (log.isDebugEnabled())
        {
            log.debug("Created SQL \"" + sql + "\"");
        }

        return statement;
    }

    /**
     * Create a SQL string to run the correct query.
     * 
     * @param scope
     * @param itemValue
     *            FIXME ??
     * @param isCount
     * @return
     */
    private static String createSqlInternal(BrowseScope scope,
            String itemValue, boolean isCount)
    {
        String tablename = BrowseTables.getTable(scope);
        String column = BrowseTables.getValueColumn(scope);

        int browseType = scope.getBrowseType();

        StringBuffer sqlb = new StringBuffer();
        sqlb.append("select ");
        sqlb.append(isCount ? "count(" : "");
        sqlb.append(getTargetColumns(scope));

        /**
         * This next bit adds another column to the query, so authors don't show
         * up lower-case
         */
        if ((browseType == AUTHORS_BROWSE) && !isCount)
        {
            sqlb.append(",sort_author");
        }

        if ((browseType == SUBJECTS_BROWSE) && !isCount)
        {
            sqlb.append(",sort_subject");
        }

        sqlb.append(isCount ? ")" : "");

        sqlb.append(" from (SELECT DISTINCT * ");

        sqlb.append(" from ");
        sqlb.append(tablename);
        sqlb.append(" ) distinct_view");

        // If the browse uses items (or item ids) instead of String values
        // make a subquery.
        // We use a separate query to make sure the subquery works correctly
        // when item values are the same. (this is transactionally
        // safe because we set the isolation level).
        // If we're NOT searching from the start, add some clauses
        boolean addedWhereClause = false;

        if (scope.hasFocus())
        {
            String subquery = null;

            if (scope.focusIsItem())
            {
                subquery = new StringBuffer().append(" or ( ").append(column)
                        .append(" = ? and item_id {0}  ").append(
                                scope.getFocusItemId()).append(")").toString();
            }

            if (log.isDebugEnabled())
            {
                log.debug("Subquery is \"" + subquery + "\"");
            }

            sqlb.append(" where ").append("(").append(column).append(" {1} ")
                    .append("?").append(scope.focusIsItem() ? subquery : "")
                    .append(")");

            addedWhereClause = true;
        }

        String connector = addedWhereClause ? " and " : " where ";
        sqlb.append(getScopeClause(scope, connector));

        // For counting, skip the "order by" and "limit" clauses
        if (isCount)
        {
            return sqlb.toString();
        }

        // Add an order by clause -- a parameter
        sqlb
                .append(" order by ")
                .append(column)
                .append("{2}")
                .append(
                        ((scope.focusIsString() && (scope.getBrowseType() != ITEMS_BY_DATE_BROWSE))
                                || (scope.getBrowseType() == AUTHORS_BROWSE) || (scope
                                .getBrowseType() == SUBJECTS_BROWSE)) ? ""
                                : ", item_id{2}");

        String myquery = sqlb.toString();

        // A limit on the total returned (Postgres extension)
        if (!scope.hasNoLimit())
        {
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            {
                myquery = "SELECT * FROM (" + myquery
                        + ") WHERE ROWNUM <= {3} ";
            }
            else
            {
                // postgres uses LIMIT
                myquery = myquery + " LIMIT {3} ";
            }
        }

        return myquery;
    }

    /**
     * Format SQL according to the browse type.
     * 
     * @param scope
     * @param sql
     * @param subqueryValue
     *            FIXME ??
     * @param after
     * @return
     * 
     *  
     */
    private static String formatSql(BrowseScope scope, String sql,
            String subqueryValue, boolean after)
    {
        boolean before = !after;
        int browseType = scope.getBrowseType();
        boolean ascending = scope.getAscending();
        int numberDesired = before ? scope.getNumberBefore() : scope.getTotal();

        // Search operator
        // Normal case: before is less than, after is greater than or equal
        String beforeOperator = "<";
        String afterOperator = ">=";

        // For authors, only equality is relevant
        if (browseType == ITEMS_BY_AUTHOR_BROWSE)
        {
            afterOperator = "=";
        }

        if (browseType == ITEMS_BY_SUBJECT_BROWSE)
        {
            afterOperator = "=";
        }

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

        if (browseType == ITEMS_BY_DATE_BROWSE)
        {
            if (!ascending)
            {
                beforeOperator = ">";
                afterOperator = "<";
            }
            else
            {
                beforeOperator = "<";
                afterOperator = ">";
            }
        }

        String beforeSubqueryOperator = "<";
        String afterSubqueryOperator = ">=";

        // For authors, only equality is relevant
        if (browseType == ITEMS_BY_AUTHOR_BROWSE
                || browseType == ITEMS_BY_SUBJECT_BROWSE)
        {
            afterSubqueryOperator = "=";
        }

        if (!ascending)
        {
            beforeSubqueryOperator = ">";
            afterSubqueryOperator = "<=";
        }

        String order = before ? " desc" : "";

        if (!ascending)
        {
            order = before ? "" : " desc";
        }

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
     * Log a message about the results of a browse.
     * 
     * @param info
     */
    private static void logInfo(BrowseInfo info)
    {
        if (!log.isDebugEnabled())
        {
            return;
        }

        log.debug("Number of Results: " + info.getResultCount()
                + " Overall position: " + info.getOverallPosition() + " Total "
                + info.getTotal() + " Offset " + info.getOffset());

        int lastIndex = (info.getOverallPosition() + info.getResultCount());
        boolean noresults = (info.getTotal() == 0)
                || (info.getResultCount() == 0);

        if (noresults)
        {
            log.debug("Got no results");
        }

        log.debug("Got results: " + info.getOverallPosition() + " to "
                + lastIndex + " out of " + info.getTotal());
    }

    /**
     * Return the name or names of the column(s) to query for a browse.
     * 
     * @param scope
     *            The current browse scope
     * @return The name or names of the columns to query
     */
    private static String getTargetColumns(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();

        if (browseType == AUTHORS_BROWSE)
            return "distinct author";
        else if (browseType == SUBJECTS_BROWSE)
            return "distinct subject";
        else
            return "*";
    }

    /**
     * <p>
     * Return a scoping clause.
     * </p>
     * 
     * <p>
     * If scope is ALLDSPACE_SCOPE, return the empty string.
     * </p>
     * 
     * <p>
     * Otherwise, the SQL clause which is generated looks like:
     * </p>
     * CONNECTOR community_id = 7 CONNECTOR collection_id = 203
     * 
     * <p>
     * CONNECTOR may be empty, or it may be a SQL keyword like <em>where</em>,
     * <em>and</em>, and so forth.
     * </p>
     * 
     * @param scope
     * @param connector
     *            FIXME ??
     * @return
     */
    static String getScopeClause(BrowseScope scope, String connector)
    {
        if (scope.isAllDSpaceScope())
        {
            return "";
        }

        boolean isCommunity = scope.isCommunityScope();
        Object obj = scope.getScope();

        int id = (isCommunity) ? ((Community) obj).getID() : ((Collection) obj)
                .getID();

        String column = (isCommunity) ? "community_id" : "collection_id";

        return new StringBuffer().append(" ").append(connector).append(" ")
                .append(column).append(" = ").append(id).toString();
    }

    /**
     * Create a PreparedStatement with the given sql.
     * 
     * @param scope
     *            The current Browse scope
     * @param sql
     *            SQL query
     * @return A PreparedStatement with the given SQL
     * @exception SQLException
     *                If a database error occurs
     */
    private static PreparedStatement createStatement(BrowseScope scope,
            String sql) throws SQLException
    {
        Connection connection = scope.getContext().getDBConnection();

        return connection.prepareStatement(sql);
    }

    /**
     * Return a single int value from the PreparedStatement.
     * 
     * @param statement
     *            A PreparedStatement for a query which returns a single value
     *            of INTEGER type.
     * @return The integer value from the query.
     * @exception SQLException
     *                If a database error occurs
     */
    private static int getIntValue(PreparedStatement statement)
            throws SQLException
    {
        ResultSet results = null;

        try
        {
            results = statement.executeQuery();

            return results.next() ? results.getInt(1) : (-1);
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }
    }

    /**
     * Convert a list of item ids to full Items.
     * 
     * @param context
     *            The current DSpace context
     * @param ids
     *            A list of item ids. Each member of the list is an Integer.
     * @return A list of Items with the given ids.
     * @exception SQLException
     *                If a database error occurs
     */
    private static List toItems(Context context, List ids) throws SQLException
    {
        //  FIXME Again, this is probably a more general need
        List results = new ArrayList();

        for (Iterator iterator = ids.iterator(); iterator.hasNext();)
        {
            Integer id = (Integer) iterator.next();
            Item item = Item.find(context, id.intValue());

            if (item != null)
            {
                results.add(item);
            }
        }

        return results;
    }
}

class NormalizedTitle
{
    private static String[] STOP_WORDS = new String[] { "A", "An", "The" };

    /**
     * Returns a normalized String corresponding to TITLE.
     * 
     * @param title
     * @param lang
     * @return
     */
    public static String normalize(String title, String lang)
    {
        if (lang == null)
        {
            return title;
        }

        return (lang.startsWith("en")) ? normalizeEnglish(title) : title;
    }

    /**
     * Returns a normalized String corresponding to TITLE. The normalization is
     * effected by:
     *  + first removing leading spaces, if any + then removing the first
     * leading occurences of "a", "an" and "the" (in any case). + removing any
     * whitespace following an occurence of a stop word
     * 
     * This simple strategy is only expected to be used for English words.
     * 
     * @param oldtitle
     * @return
     */
    public static String normalizeEnglish(String oldtitle)
    {
        // Corner cases
        if (oldtitle == null)
        {
            return null;
        }

        if (oldtitle.length() == 0)
        {
            return oldtitle;
        }

        // lower case, stupid! (sorry, just a rant about bad contractors)
        String title = oldtitle.toLowerCase();

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
            boolean found = title.toLowerCase().startsWith(stop.toLowerCase(),
                    startAt)
                    && ( //   The title must be longer than whitespace plus the
                         // stop word
                    title.length() >= (startAt + stoplen + 1)) &&
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
                {
                    startAt = firstw;
                }

                // Only process a single stop word
                break;
            }
        }

        // If we didn't change anything, just return the title as-is
        if (!modified)
        {
            return title;
        }

        // If we just stripped white space, return a substring
        if (!usedStopWord)
        {
            return title.substring(startAt);
        }

        // Otherwise, return the substring with the stop word appended
        return new StringBuffer(title.substring(startAt)).append(", ").append(
                stop).toString();
    }

    /**
     * Return the index of the first non-whitespace character in the String.
     * 
     * @param title
     * @return
     */
    private static int firstWhitespace(String title)
    {
        return firstWhitespace(title, 0);
    }

    /**
     * Return the index of the first non-whitespace character in the character
     * array.
     * 
     * @param title
     * @return
     */
    private static int firstWhitespace(char[] title)
    {
        return firstWhitespace(title, 0);
    }

    /**
     * Return the index of the first non-whitespace character in the String,
     * starting at position STARTAT.
     * 
     * @param title
     * @param startAt
     * @return
     */
    private static int firstWhitespace(String title, int startAt)
    {
        return firstWhitespace(title.toCharArray(), startAt);
    }

    /**
     * Return the index of the first letter or number in the character array,
     * starting at position STARTAT.
     * 
     * @param title
     * @param startAt
     * @return
     */
    private static int firstWhitespace(char[] title, int startAt)
    {
        int first = 0;

        for (int j = startAt; j < title.length; j++)
        {
            //if (Character.isWhitespace(title[j]))
            // Actually, let's skip anything that's not a letter or number
            if (!Character.isLetterOrDigit(title[j]))
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

    private static Map cache = new WeakHashMap();

    // Everything in the cache is held via Weak References, and is
    // subject to being gc-ed at any time.
    // The dateCache holds normal references, so anything in it
    // will stay around.
    private static SortedMap dateCache = new TreeMap();

    private static final int CACHE_MAXIMUM = 30;

    /**
     * Look for cached Browse data corresponding to KEY.
     * 
     * @param key
     * @return
     */
    public static BrowseInfo get(BrowseScope key)
    {
        if (log.isDebugEnabled())
        {
            log
                    .debug("Checking browse cache with " + cache.size()
                            + " objects");
        }

        BrowseInfo cachedInfo = (BrowseInfo) cache.get(key);

        try
        {
            // Index has never been calculated
            if (getMaximum(key) == -1)
            {
                updateIndexData(key);
            }

            if (cachedInfo == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Not in browse cache");
                }

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
                {
                    log.debug("Index has changed");
                }

                cache.remove(key);

                return null;
            }
        }
        catch (SQLException sqle)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught SQLException: " + sqle, sqle);
            }

            return null;
        }

        // Cached object
        if (log.isDebugEnabled())
        {
            log.debug("Using cached browse");
        }

        cachedInfo.setCached(true);

        return cachedInfo;
    }

    /**
     * Return true if an index has changed
     * 
     * @param key
     * @return
     * @throws SQLException
     */
    public static boolean indexHasChanged(BrowseScope key) throws SQLException
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
                // use getIntColumn for Oracle count data
                if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
                {
                    count = results.getIntColumn("count");              
                }
                else  //getLongColumn works for postgres
                {
                    count = results.getLongColumn("count");
                }
                max = results.getIntColumn("max");
            }

            context.complete();

            // Same?
            if ((count == getCount(key)) && (max == getMaximum(key)))
            {
                return false;
            }

            // Update 'em
            setMaximum(key, max);
            setCount(key, count);

            // The index has in fact changed
            return true;
        }
        catch (SQLException sqle)
        {
            if (context != null)
            {
                context.abort();
            }

            throw sqle;
        }
    }

    /**
     * Compute and save the values for the number of values in the index, and
     * the maximum such value.
     * 
     * @param key
     */
    public static void updateIndexData(BrowseScope key)
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
                //use getIntColumn for Oracle count data
                if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
                {
                    count = results.getIntColumn("count");              
                }
                else  //getLongColumn works for postgres
                {
                    count = results.getLongColumn("count");
                }
                max = results.getIntColumn("max");
            }

            context.complete();

            setMaximum(key, max);
            setCount(key, count);
        }
        catch (Exception e)
        {
            if (context != null)
            {
                context.abort();
            }

            e.printStackTrace();
        }
    }

    /**
     * Retrieve the values for count and max
     * 
     * @param context
     * @param scope
     * @return
     * @throws SQLException
     */
    public static TableRow countAndMax(Context context, BrowseScope scope)
            throws SQLException
    {
        // The basic idea here is that we'll check an indexes
        // maximum id and its count: if the maximum id has changed,
        // then there are new values, and if the count has changed,
        // then some items may have been removed. We assume that
        // values never change.
        String sql = new StringBuffer().append(
                "select count({0}) as count, max({0}) as max from ").append(
                BrowseTables.getTable(scope)).append(
                Browse.getScopeClause(scope, "where")).toString();

        // Format it to use the correct columns
        String countColumn = BrowseTables.getIndexColumn(scope);
        Object[] args = new Object[] { countColumn, countColumn };
        String SQL = MessageFormat.format(sql, args);

        // Run the query
        if (log.isDebugEnabled())
        {
            log.debug("Running SQL to check whether index has changed: \""
                    + SQL + "\"");
        }
        
        return DatabaseManager.querySingle(context, SQL);
    }

    /**
     * Add info to cache, using key.
     * 
     * @param key
     * @param info
     */
    public static void add(BrowseScope key, BrowseInfo info)
    {
        // Don't bother caching browses with no results, they are
        // fairly cheap to calculate
        if (info.getResultCount() == 0)
        {
            return;
        }

        // Add the info to the cache
        // Since the object is passed in to us (and thus the caller
        // may change it), we make a copy.
        cache.put(key.clone(), info);

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
        synchronized (dateCache)
        {
            // Plenty of room!
            if (dateCache.size() < CACHE_MAXIMUM)
            {
                return;
            }

            // Remove the oldest object
            dateCache.remove(dateCache.firstKey());
        }
    }

    /**
     * Return the maximum value
     * 
     * @param scope
     * @return
     */
    private static int getMaximum(BrowseScope scope)
    {
        String table = BrowseTables.getTable(scope);
        Integer value = (Integer) tableMax.get(table);

        return (value == null) ? (-1) : value.intValue();
    }

    private static long getCount(BrowseScope scope)
    {
        String table = BrowseTables.getTable(scope);
        Long value = (Long) tableSize.get(table);

        return (value == null) ? (-1) : value.longValue();
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
    private static final String[] BROWSE_TABLES = new String[] {
            "Communities2Item", "ItemsByAuthor", "ItemsByDate",
            "ItemsByDateAccessioned", "ItemsByTitle", "ItemsBySubject" };

    /**
     * Return the browse tables. This only returns true tables, views are
     * ignored.
     * 
     * @return
     */
    public static String[] tables()
    {
        return BROWSE_TABLES;
    }

    /**
     * Return the browse table or view for scope.
     * 
     * @param scope
     * @return
     */
    public static String getTable(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();
        boolean isCommunity = scope.isCommunityScope();
        boolean isCollection = scope.isCollectionScope();

        if ((browseType == Browse.AUTHORS_BROWSE)
                || (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE))
        {
            if (isCommunity)
            {
                return "CommunityItemsByAuthor";
            }

            if (isCollection)
            {
                return "CollectionItemsByAuthor";
            }

            return "ItemsByAuthor";
        }

        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
        {
            if (isCommunity)
            {
                return "CommunityItemsByTitle";
            }

            if (isCollection)
            {
                return "CollectionItemsByTitle";
            }

            return "ItemsByTitle";
        }

        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
        {
            if (isCommunity)
            {
                return "CommunityItemsByDate";
            }

            if (isCollection)
            {
                return "CollectionItemsByDate";
            }

            return "ItemsByDate";
        }

        if ((browseType == Browse.SUBJECTS_BROWSE)
                || (browseType == Browse.ITEMS_BY_SUBJECT_BROWSE))
        {
            if (isCommunity)
            {
                return "CommunityItemsBySubject";
            }

            if (isCollection)
            {
                return "CollectionItemsBySubject";
            }

            return "ItemsBySubject";
        }

        throw new IllegalArgumentException(
                "No table for browse and scope combination");
    }

    /**
     * Return the name of the column that holds the index.
     * 
     * @param scope
     * @return
     */
    public static String getIndexColumn(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();

        if (browseType == Browse.AUTHORS_BROWSE)
        {
            return "items_by_author_id";
        }

        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
        {
            return "items_by_author_id";
        }

        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
        {
            return "items_by_date_id";
        }

        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
        {
            return "items_by_title_id";
        }
        
        if (browseType == Browse.SUBJECTS_BROWSE)
        {
            return "items_by_subject_id";
        }

        if (browseType == Browse.ITEMS_BY_SUBJECT_BROWSE)
        {
            return "items_by_subject_id";
        }

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }

    /**
     * Return the name of the column that holds the Browse value (the title,
     * author, date, etc).
     * 
     * @param scope
     * @return
     */
    public static String getValueColumn(BrowseScope scope)
    {
        int browseType = scope.getBrowseType();

        if (browseType == Browse.AUTHORS_BROWSE)
        {
            return "sort_author";
        }

        if (browseType == Browse.ITEMS_BY_AUTHOR_BROWSE)
        {
            return "sort_author";
        }

        if (browseType == Browse.ITEMS_BY_DATE_BROWSE)
        {
            return "date_issued";
        }

        // Note that we use the normalized form of the title
        if (browseType == Browse.ITEMS_BY_TITLE_BROWSE)
        {
            return "sort_title";
        }

        if (browseType == Browse.SUBJECTS_BROWSE)
        {
            return "sort_subject";
        }

        if (browseType == Browse.ITEMS_BY_SUBJECT_BROWSE)
        {
            return "sort_subject";
        }

        throw new IllegalArgumentException("Unknown browse type: " + browseType);
    }
}
