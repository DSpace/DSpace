/*
 * BrowseCreateDAOOracle.java
 *
 * Version: $Revision: $
 *
 * Date: $Date:  $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * This class implements the BrowseCreateDAO interface for the Oracle database
 * as associated with the default DSpace installation. This class should not be
 * instantiated directly, but should be obtained via the BrowseDAOFactory:
 * 
 * <code>
 * Context context = new Context();
 * BrowseCreateDAO dao = BrowseDAOFactory.getCreateInstance(context);
 * </code>
 * 
 * This class will then be loaded if the appropriate configuration is made.
 * 
 * @author Graham Triggs
 */
public class BrowseCreateDAOOracle implements BrowseCreateDAO
{
    /** Log4j logger */
    private static Logger log = Logger.getLogger(BrowseCreateDAOOracle.class);

    /**
     * internal copy of the current DSpace context (including the database
     * connection)
     */
    private Context context;

    /** Database specific set of utils used when prepping the database */
    private BrowseDAOUtils utils;
    
    /**
     * Required constructor for classes implementing the BrowseCreateDAO
     * interface. Takes a DSpace context to use to connect to the database with.
     * 
     * @param context
     *            the DSpace context
     */
    public BrowseCreateDAOOracle(Context context)
    	throws BrowseException
    {
        this.context = context;
        
        // obtain the relevant Utils for this class
        utils = BrowseDAOFactory.getUtils(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.browse.BrowseCreateDAO#createCollectionView(java.lang.String,
     *      java.lang.String, boolean)
     */
    public String createCollectionView(String table, String view, boolean execute) throws BrowseException
    {
        try
        {
            String createColView = "CREATE VIEW " + view + " AS " +
                                   "SELECT Collection2Item.collection_id, " + table + ".* " +
                                   "FROM  " + table + ", Collection2Item " +
                                   "WHERE " + table + ".item_id = Collection2Item.item_id";
            
            if (execute)
            {
                DatabaseManager.updateQuery(context, createColView);
            }
            
            return createColView + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createCommunityView(java.lang.String, java.lang.String, boolean)
     */
    public String createCommunityView(String table, String view, boolean execute) throws BrowseException
    {
        try
        {
            String createComView = "CREATE VIEW " + view + " AS " +
                                   "SELECT Communities2Item.community_id, " + table + ".* " +
                                   "FROM  " + table + ", Communities2Item " +
                                   "WHERE " + table + ".item_id = Communities2Item.item_id";
            
            if (execute)
            {
                DatabaseManager.updateQuery(context, createComView);
            }
            return createComView + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDatabaseIndices(java.lang.String, boolean)
     */
    public String[] createDatabaseIndices(String table, List<Integer> sortCols, boolean value, boolean execute) throws BrowseException
    {
        try
        {
            ArrayList<String> array = new ArrayList<String>();
            
            array.add("CREATE INDEX " + table + "_item_id_idx ON " + table + "(item_id)");
    
            if (value)
                array.add("CREATE INDEX " + table + "_value_idx ON " + table + "(sort_value)");
    
            for (Integer i : sortCols)
            {
                array.add("CREATE INDEX " + table + "_s" + i + "_idx ON " + table + "(sort_" + i + ")");
            }
            
            if (execute)
            {
                for (String query : array)
                {
                    DatabaseManager.updateQuery(context, query);
                }
            }
            
            String[] arr = new String[array.size()];
            return array.toArray(arr);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDatabaseIndices(java.lang.String, boolean)
     */
    public String[] createMapIndices(String disTable, String mapTable, boolean execute) throws BrowseException
    {
        try
        {
            String[] arr = new String[3];
            arr[0] = "CREATE INDEX " + disTable + "_value_idx ON " + disTable + "(sort_value)";
            arr[1] = "CREATE INDEX " + mapTable + "_item_id_idx ON " + mapTable + "(item_id)";
            arr[2] = "CREATE INDEX " + mapTable + "_dist_idx ON " + mapTable + "(distinct_id)";
            
            if (execute)
            {
                for (String query : arr)
                {
                    DatabaseManager.updateQuery(context, query);
                }
            }
            
            return arr;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDistinctMap(java.lang.String, java.lang.String, boolean)
     */
    public String createDistinctMap(String table, String map, boolean execute) throws BrowseException
    {
        try
        {
            String create = "CREATE TABLE " + map + " (" +
                            "map_id NUMBER PRIMARY KEY, " +
                            "item_id NUMBER REFERENCES item(item_id), " +
                            "distinct_id NUMBER REFERENCES " + table + "(id)" +
                            ")";
            
            if (execute)
            {
                DatabaseManager.updateQuery(context, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#updateDistinctMapping(java.lang.String, int, int)
     */
    public boolean updateDistinctMappings(String table, int itemID, int[] distinctIDs) throws BrowseException
    {
        try
        {
            // Remove (set to -1) any duplicate distinctIDs
            for (int i = 0; i < distinctIDs.length; i++)
            {
                if (!isFirstOccurrence(distinctIDs, i))
                    distinctIDs[i] = -1;
            }

            // Find all existing mappings for this item
            TableRowIterator tri = DatabaseManager.queryTable(context, table, "SELECT * FROM " + table + " WHERE item_id=?", itemID);
            if (tri != null)
            {
                try
                {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();

                        // Check the item mappings to see if it contains this mapping
                        boolean itemIsMapped = false;
                        int trDistinctID = tr.getIntColumn("distinct_id");
                        for (int i = 0; i < distinctIDs.length; i++)
                        {
                            // Found this mapping
                            if (distinctIDs[i] == trDistinctID)
                            {
                                // Flag it, and remove (-1) from the item mappings
                                itemIsMapped = true;
                                distinctIDs[i] = -1;
                            }
                        }

                        // The item is no longer mapped to this community, so remove the database record
                        if (!itemIsMapped)
                            DatabaseManager.delete(context, tr);
                    }
                }
                finally
                {
                    tri.close();
                }
            }

            // Any remaining mappings need to be added to the database
            for (int i = 0; i < distinctIDs.length; i++)
            {
                if (distinctIDs[i] > -1)
                {
                    TableRow row = DatabaseManager.create(context, table);
                    row.setColumn("item_id", itemID);
                    row.setColumn("distinct_id", distinctIDs[i]);
                    DatabaseManager.update(context, row);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            String msg = "problem updating distinct mappings: table=" + table + ",item-id=" + itemID;
            throw new BrowseException(msg, e);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDistinctTable(java.lang.String, boolean)
     */
    public String createDistinctTable(String table, boolean execute) throws BrowseException
    {
        try
        {
            String create = "CREATE TABLE " + table + " (" +
                            "id INTEGER PRIMARY KEY, " + 
                            "value " + getValueColumnDefinition() + ", " +
                            "sort_value " + getSortColumnDefinition() +
                            ")";
            
            if (execute)
            {
                DatabaseManager.updateQuery(context, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    public String createPrimaryTable(String table, List sortCols, boolean execute) throws BrowseException
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            
            Iterator itr = sortCols.iterator();
            while (itr.hasNext())
            {
                Integer no = (Integer) itr.next();
                sb.append(", sort_");
                sb.append(no.toString());
                sb.append(getSortColumnDefinition());
            }
            
            String createTable = "CREATE TABLE " + table + " (" +
                                    "id INTEGER PRIMARY KEY," +
                                    "item_id INTEGER REFERENCES item(item_id)" +
                                    sb.toString() + 
                                    ")";
            if (execute)
            {
                DatabaseManager.updateQuery(context, createTable);
            }
            return createTable;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }       
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createSequence(java.lang.String, boolean)
     */
    public String createSequence(String sequence, boolean execute) throws BrowseException
    {
        try
        {
            String create = "CREATE SEQUENCE " + sequence;
            if (execute)
            {
                DatabaseManager.updateQuery(context, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#deleteByItemID(java.lang.String, int)
     */
    public void deleteByItemID(String table, int itemID) throws BrowseException
    {
        try
        {
            Object[] params = { new Integer(itemID) };
            String dquery = "DELETE FROM " + table + " WHERE item_id=?";
            DatabaseManager.updateQuery(context, dquery, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }

    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#deleteCommunityMappings(java.lang.String, int)
     */
    public void deleteCommunityMappings(int itemID)
        throws BrowseException
    {
        try
        {
            Object[] params = { new Integer(itemID) };
            String dquery = "DELETE FROM Communities2Item WHERE item_id = ?";
            DatabaseManager.updateQuery(context, dquery, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropIndexAndRelated(java.lang.String, boolean)
     */
    public String dropIndexAndRelated(String table, boolean execute) throws BrowseException
    {
        try
        {
            String dropper = "DROP TABLE " + table + " CASCADE CONSTRAINTS";
            if (execute)
            {
                DatabaseManager.updateQuery(context, dropper);
            }
            return dropper + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropSequence(java.lang.String, boolean)
     */
    public String dropSequence(String sequence, boolean execute) throws BrowseException
    {
        try
        {
            String dropSeq = "DROP SEQUENCE " + sequence;
            if (execute)
            {
                DatabaseManager.updateQuery(context, dropSeq);
            }
            return dropSeq + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropView(java.lang.String, boolean)
     */
    public String dropView(String view, boolean execute)
        throws BrowseException
    {
        if (view != null && !"".equals(view))
        {
            try
            {
                String dropView = "DROP VIEW " + view + " CASCADE CONSTRAINTS";
                if (execute)
                {
                    DatabaseManager.updateQuery(context, dropView);
                }
                
                return dropView + ";";
            }
            catch (SQLException e)
            {
                log.error("caught exception: ", e);

                // We can't guarantee a test for existence, or force Oracle
                // not to complain if it isn't there, so we just catch the exception
                // and pretend nothing is wrong
            }
        }
        
        return "";
    }
    
    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#getDistinctID(java.lang.String, java.lang.String, java.lang.String)
     */
    public int getDistinctID(String table, String value, String sortValue) throws BrowseException
    {
        TableRowIterator tri = null;
        
        if (log.isDebugEnabled())
        {
            log.debug("getDistinctID: table=" + table + ",value=" + value + ",sortValue=" + sortValue);
        }
        
        try
        {
            Object[] params = { value };
            String select = "SELECT id FROM " + table;
            
            if (isValueColumnClob())
                select = select + " WHERE TO_CHAR(value)=?";
            else
                select = select + " WHERE value=?";
               
            tri = DatabaseManager.query(context, select, params);
            int distinctID = -1;
            if (!tri.hasNext())
            {
                distinctID = insertDistinctRecord(table, value, sortValue);
            }
            else
            {
                distinctID = tri.next().getIntColumn("id");
            }

            if (log.isDebugEnabled())
            {
                log.debug("getDistinctID: return=" + distinctID);
            }

            return distinctID;
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
     * @see org.dspace.browse.BrowseCreateDAO#updateCommunityMappings(int)
     */
    public void updateCommunityMappings(int itemID) throws BrowseException
    {
        try
        {
            // Get all the communities for this item
            int[] commID = getAllCommunityIDs(itemID);

            // Remove (set to -1) any duplicate communities
            for (int i = 0; i < commID.length; i++)
            {
                if (!isFirstOccurrence(commID, i))
                    commID[i] = -1;
            }

            // Find all existing mappings for this item
            TableRowIterator tri = DatabaseManager.queryTable(context, "Communities2Item", "SELECT * FROM Communities2Item WHERE item_id=?", itemID);
            if (tri != null)
            {
                try
                {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();

                        // Check the item mappings to see if it contains this community mapping
                        boolean itemIsMapped = false;
                        int trCommID = tr.getIntColumn("community_id");
                        for (int i = 0; i < commID.length; i++)
                        {
                            // Found this community
                            if (commID[i] == trCommID)
                            {
                                // Flag it, and remove (-1) from the item mappings
                                itemIsMapped = true;
                                commID[i] = -1;
                            }
                        }

                        // The item is no longer mapped to this community, so remove the database record
                        if (!itemIsMapped)
                            DatabaseManager.delete(context, tr);
                    }
                }
                finally
                {
                    tri.close();
                }
            }

            // Any remaining mappings need to be added to the database
            for (int i = 0; i < commID.length; i++)
            {
                if (commID[i] > -1)
                {
                    TableRow row = DatabaseManager.create(context, "Communities2Item");
                    row.setColumn("item_id", itemID);
                    row.setColumn("community_id", commID[i]);
                    DatabaseManager.update(context, row);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#insertDistinctRecord(java.lang.String, java.lang.String, java.lang.String)
     */
    public int insertDistinctRecord(String table, String value, String sortValue) throws BrowseException
    {
        if (log.isDebugEnabled())
        {
            log.debug("insertDistinctRecord: table=" + table + ",value=" + value+ ",sortValue=" + sortValue);
        }
        
        try
        {
            TableRow dr = DatabaseManager.create(context, table);
            dr.setColumn("value", utils.truncateValue(value));
            dr.setColumn("sort_value", utils.truncateSortValue(sortValue));
            DatabaseManager.update(context, dr);
            int distinctID = dr.getIntColumn("id");
            
            if (log.isDebugEnabled())
            {
                log.debug("insertDistinctRecord: return=" + distinctID);
            }
            
            return distinctID;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    public void insertIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
        try
        {
            // create us a row in the index
            TableRow row = DatabaseManager.create(context, table);
            
            // set the primary information for the index
            row.setColumn("item_id", itemID);
            
            // now set the columns for the other sort values
            Iterator itra = sortCols.keySet().iterator();
            while (itra.hasNext())
            {
                Integer key = (Integer) itra.next();
                String nValue = (String) sortCols.get(key);
                row.setColumn("sort_" + key.toString(), utils.truncateSortValue(nValue));
            }
            
            DatabaseManager.update(context, row);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#updateIndex(java.lang.String, int, java.util.Map)
     */
    public boolean updateIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
        try
        {
            boolean rowUpdated = false;
            TableRow row = DatabaseManager.findByUnique(context, table, "item_id", itemID);

            // If the item does not exist in the table, return that it couldn't be found
            if (row == null)
                return false;

            // Iterate through all the sort values
            Iterator itra = sortCols.keySet().iterator();
            while (itra.hasNext())
            {
                Integer key = (Integer) itra.next();

                // Generate the appropriate column name
                String column = "sort_" + key.toString();

                // Create the value that will be written in to the column
                String newValue = utils.truncateSortValue( (String) sortCols.get(key) );

                // Check the column exists - if it doesn't, something has gone seriously wrong
                if (!row.hasColumn(column))
                    throw new BrowseException("Column '" + column + "' does not exist in table " + table);

                // Get the existing value from the column
                String oldValue = row.getStringColumn(column);

                // If the new value differs from the old value, update the column and flag that the row has changed
                if (oldValue != null && !oldValue.equals(newValue))
                {
                    row.setColumn(column, newValue);
                    rowUpdated = true;
                }
                else if (newValue != null && !newValue.equals(oldValue))
                {
                    row.setColumn(column, newValue);
                    rowUpdated = true;
                }
            }

            // We've updated the row, so save it back to the database
            if (rowUpdated)
                DatabaseManager.update(context, row);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }

        // Return that the original record was found
        return true;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#pruneDistinct(java.lang.String, java.lang.String)
     */
    public void pruneDistinct(String table, String map) throws BrowseException
    {
        try
        {
            String query = "DELETE FROM " + table + 
                            " WHERE id IN (SELECT id FROM " + table +
                            " MINUS SELECT distinct_id AS id FROM " + map + ")";
            
            DatabaseManager.updateQuery(context, query);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#pruneExcess(java.lang.String, java.lang.String)
     */
    public void pruneExcess(String table, String map, boolean withdrawn) throws BrowseException
    {
        try
        {
            String itemQuery = "SELECT item_id FROM item WHERE ";
            if (withdrawn)
                itemQuery += "withdrawn = 1";
            else
                itemQuery += "in_archive = 1 AND withdrawn = 0";
            
            String delete         = "DELETE FROM " + table + " WHERE item_id IN ( SELECT item_id FROM " + table + " MINUS " + itemQuery + ")";
            DatabaseManager.updateQuery(context, delete);

            if (map != null)
            {
                String deleteDistinct = "DELETE FROM " + map   + " WHERE item_id IN ( SELECT item_id FROM " + map   + " MINUS " + itemQuery + ")";
                DatabaseManager.updateQuery(context, deleteDistinct);
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#testTableExistance(java.lang.String)
     */
    public boolean testTableExistance(String table) throws BrowseException
    {
        // this method can kill the db connection, so we start up
        // our own private context to do it
        Context c = null;

        try
        {
            c = new Context();
            String testQuery = "SELECT * FROM " + table + " WHERE ROWNUM=1";
            DatabaseManager.query(c, testQuery);
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
        finally
        {
            if (c != null)
            {
                c.abort();
            }
        }
    }

    /**
     * Get the definition of the value column - CLOB if the size is greater than 4000 bytes
     * otherwise a VARCHAR2.
     * 
     * @return
     */
    private String getValueColumnDefinition()
    {
        if (getValueColumnMaxBytes() < 1 || getValueColumnMaxBytes() > 4000)
        {
            return " CLOB ";
        }
        
        return " VARCHAR2(" + getValueColumnMaxBytes() + ") ";
    }

    /**
     * Get the definition of the sort_value column - always a VARCHAR2
     * (required for ordering)
     * 
     * @return
     */
    private String getSortColumnDefinition()
    {
        return " VARCHAR2(" + getSortColumnMaxBytes() + ") ";
    }
    
    /**
     * Get the size in bytes of the value columns.
     * 
     * As the size is configured in chars, double the number of bytes
     * (to account for UTF-8)
     * 
     * @return
     */
    private int getValueColumnMaxBytes()
    {
        int chars = utils.getValueColumnMaxChars();
        
        if (chars > 2000 || chars < 1)
        {
            return 4000;
        }
       
        return chars * 2;
    }
    
    /**
     * Get the size in bytes of the sort columns.
     * MUST return a value between 1 and 4000.
     * 
     * As the size is configured in chars, double the number of bytes
     * (to account for UTF-8)
     * 
     * @return
     */
    private int getSortColumnMaxBytes()
    {
        int chars = utils.getSortColumnMaxChars();
        
        if (chars > 2000 || chars < 1)
        {
            return 4000;
        }
        
        return chars * 2;
    }

    /**
     * If getValueColumnDefinition() is returning a CLOB definition,
     * then this must return true.
     * 
     * @return
     */
    private boolean isValueColumnClob()
    {
        if (getValueColumnMaxBytes() < 1)
        {
            return true;
        }
        
        return false;
    }

    /**
     * perform a database query to get all the communities that this item belongs to,
     * including all mapped communities, and ancestors
     *
     * this is done here instead of using the Item api, because for reindexing we may
     * not have Item objects, and in any case this is *much* faster
     *
     * @param itemId
     * @return
     * @throws SQLException
     */
    private int[] getAllCommunityIDs(int itemId) throws SQLException
    {
        List<Integer> commIdList = new ArrayList<Integer>();

        TableRowIterator tri = null;

        try
        {
            tri = DatabaseManager.queryTable(context, "Community2Item",
                        "SELECT * FROM Community2Item WHERE item_id=?", itemId);

            while (tri.hasNext())
            {
                TableRow row = tri.next();
                int commId = row.getIntColumn("community_id");
                commIdList.add(commId);

                // Get the parent community, and continue to get all ancestors
                Integer parentId = getParentCommunityID(commId);
                while (parentId != null)
                {
                    commIdList.add(parentId);
                    parentId = getParentCommunityID(parentId);
                }
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }

        // Need to iterate the array as toArray will produce an array Integers,
        // not ints as we need.
        int[] cIds = new int[commIdList.size()];
        for (int i = 0; i < commIdList.size(); i++)
        {
            cIds[i] = commIdList.get(i);
        }

        return cIds;
    }

    /**
     * Get the id of the parent community. Returns Integer, as null is used to
     * signify that there are no parents (ie. top-level).
     *
     * @param commId
     * @return
     * @throws SQLException
     */
    private Integer getParentCommunityID(int commId) throws SQLException
    {
        TableRowIterator tri = null;

        try
        {
            tri = DatabaseManager.queryTable(context, "Community2Community",
                        "SELECT * FROM Community2Community WHERE child_comm_id=?", commId);

            if (tri.hasNext())
            {
                return tri.next().getIntColumn("parent_comm_id");
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }

        return null;
    }

    /**
     * Check to see if the integer at pos is the first occurrence of that value
     * in the array.
     *
     * @param ids
     * @param pos
     * @return
     */
    private boolean isFirstOccurrence(int[] ids, int pos)
    {
        if (pos < 0 || pos >= ids.length)
            return false;

        int id = ids[pos];
        for (int i = 0; i < pos; i++)
        {
            if (id == ids[i])
                return false;
        }

        return true;
    }
}
