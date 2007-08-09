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
                                   "SELECT Community2Item.community_id, " + table + ".* " +
                                   "FROM  " + table + ", Community2Item " +
                                   "WHERE " + table + ".item_id = Community2Item.item_id";
            
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
                array.add("CREATE INDEX " + table + "_value_index ON " + table + "(sort_value)");
    
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
     * @see org.dspace.browse.BrowseCreateDAO#createDistinctMapping(java.lang.String, int, int)
     */
    public void createDistinctMapping(String table, int itemID, int distinctID) throws BrowseException
    {
        try
        {
            TableRow tr = DatabaseManager.create(context, table);
            tr.setColumn("item_id",     itemID);
            tr.setColumn("distinct_id", distinctID);
            DatabaseManager.update(context, tr);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            String msg = "problem creating distinct mapping: table=" + table + ",item-id=" + itemID + ",distinct_id=" + distinctID;
            throw new BrowseException(msg, e);
        }

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
     * @see org.dspace.browse.BrowseCreateDAO#createPrimaryTable(java.lang.String, java.util.List, boolean)
     */
    public String createSecondaryTable(String table, List sortCols, boolean execute) throws BrowseException
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            sb.append("sort_value ");
            sb.append(getSortColumnDefinition());
            
            Iterator itr = sortCols.iterator();
            while (itr.hasNext())
            {
                Integer no = (Integer) itr.next();
                sb.append(", sort_");
                sb.append(no.toString());
                sb.append(getSortColumnDefinition());
            }
            
            String createTable = "CREATE TABLE " + table + " (" +
                                    "id integer PRIMARY KEY," +
                                    "item_id NUMBER REFERENCES item(item_id)," +
                                    "value " + getValueColumnDefinition() + ", " + 
                                    sb.toString() + 
                                    ")";
            if (execute)
            {
                DatabaseManager.updateQuery(context, createTable);
            }
            return createTable + ";";
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
            tri.close();
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
     * @see org.dspace.browse.BrowseCreateDAO#insertIndex(java.lang.String, int, java.lang.String, java.lang.String, java.util.Map)
     */
    public void insertIndex(String table, int itemID, String value, String sortValue, Map sortCols)
            throws BrowseException
    {
        try
        {
            // create us a row in the index
            TableRow row = DatabaseManager.create(context, table);
            
            // set the primary information for the index
            row.setColumn("item_id", itemID);
            row.setColumn("value", utils.truncateValue(value));
            row.setColumn("sort_value", utils.truncateSortValue(sortValue));
            
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
     * @see org.dspace.browse.BrowseCreateDAO#pruneDistinct(java.lang.String, java.lang.String)
     */
    public void pruneDistinct(String table, String map) throws BrowseException
    {
        try
        {
            String query = "DELETE FROM " + table + 
                            " WHERE id NOT IN " +
                            "(SELECT distinct_id FROM " + map + ")";
            
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
        TableRowIterator tri = null;
        
        try
        {
            String query = "SELECT item_id FROM " + table + " WHERE item_id NOT IN ( SELECT item_id FROM item WHERE in_archive = 1 AND withdrawn = " +
                            (withdrawn ? "0" : "1") + ")";
            tri = DatabaseManager.query(context, query);
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                String delete = "DELETE FROM " + table + " WHERE item_id = " + Integer.toString(row.getIntColumn("item_id"));
                String deleteDistinct = "DELETE FROM " + map + " WHERE item_id = " + Integer.toString(row.getIntColumn("item_id"));
                DatabaseManager.updateQuery(context, delete);
                DatabaseManager.updateQuery(context, deleteDistinct);
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
}
