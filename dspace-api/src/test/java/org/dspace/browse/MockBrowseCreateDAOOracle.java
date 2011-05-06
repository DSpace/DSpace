/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mockit.Mock;
import mockit.MockClass;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Mocks some methods of BrowseCreateDAOOracle to enable compatibility with H2
 * @author pvillega
 */
@MockClass(realClass=BrowseCreateDAOOracle.class)
public class MockBrowseCreateDAOOracle
{

    /** log4j category */
    private static Logger log = Logger.getLogger(MockBrowseCreateDAOOracle.class);

    /**
     * internal copy of the current DSpace context (including the database
     * connection)
     */
    protected Context internalContext;

    /** Database specific set of utils used when prepping the database */
    protected BrowseDAOUtils utils;

    /**
     * Constructor
     */
    @Mock
    public void $init(Context ctx)
    {        

    }
        
    protected void cleanContext()
    {
        try
        {
            if(internalContext != null && internalContext.isValid())
            {
                internalContext.complete();
            }
        }
        catch (SQLException ex)
        {
            log.error("SQL Exception cleaning Mock BrowseCreateDAOOracle",ex);
        }
    }

    /**
     * Due to how the classloader works, we need to create custom context every time
     * this mock is called.
     */
    private void checkContext()
    {
       try
        {
            if(internalContext == null || !internalContext.isValid())
            {
                 internalContext = new Context();
            }            
            // obtain the relevant Utils for this class
            utils = BrowseDAOFactory.getUtils(internalContext);
        }
        catch (SQLException ex)
        {
            log.error("SQL Exception checkContext BrowseCreateDAOOracle",ex);
        }
        catch (BrowseException ex)
        {
            log.error("Browse Exception checkContext BrowseCreateDAOOracle",ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.browse.BrowseCreateDAO#createCollectionView(java.lang.String,
     *      java.lang.String, boolean)
     */
    @Mock
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
                DatabaseManager.updateQuery(internalContext, createColView);
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
    @Mock
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
                DatabaseManager.updateQuery(internalContext, createComView);
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
    @Mock
    public String[] createDatabaseIndices(String table, List<Integer> sortCols, boolean value, boolean execute) throws BrowseException
    {
        try
        {
            checkContext();
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
                    DatabaseManager.updateQuery(internalContext, query);
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
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDatabaseIndices(java.lang.String, boolean)
     */
    @Mock
    public String[] createMapIndices(String disTable, String mapTable, boolean execute) throws BrowseException
    {
        try
        {
            checkContext();
            String[] arr = new String[5];
            arr[0] = "CREATE INDEX " + disTable + "_svalue_idx ON " + disTable + "(sort_value)";
            arr[1] = "CREATE INDEX " + disTable + "_value_idx ON " + disTable + "(value)";
            arr[2] = "CREATE INDEX " + disTable + "_uvalue_idx ON " + disTable + "(value)";
            arr[3] = "CREATE INDEX " + mapTable + "_item_id_idx ON " + mapTable + "(item_id)";
            arr[4] = "CREATE INDEX " + mapTable + "_dist_idx ON " + mapTable + "(distinct_id)";

            if (execute)
            {                
                for (String query : arr)
                {
                    DatabaseManager.updateQuery(internalContext, query);
                }
            }

            return arr;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDistinctMap(java.lang.String, java.lang.String, boolean)
     */
    @Mock
    public String createDistinctMap(String table, String map, boolean execute) throws BrowseException
    {
        try
        {
            checkContext();
            String create = "CREATE TABLE " + map + " (" +
                            "map_id NUMBER PRIMARY KEY, " +
                            "item_id NUMBER REFERENCES item(item_id), " +
                            "distinct_id NUMBER REFERENCES " + table + "(id)" +
                            ")";

            if (execute)
            {                
                DatabaseManager.updateQuery(internalContext, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    @Mock
    public MappingResults updateDistinctMappings(String table, int itemID, Set<Integer> distinctIDs) throws BrowseException
    {
        BrowseMappingResults results = new BrowseMappingResults();
        try
        {
            checkContext();
            Set<Integer> addDistinctIDs = null;

            // Find all existing mappings for this item
            TableRowIterator tri = DatabaseManager.queryTable(internalContext, table, "SELECT * FROM " + table + " WHERE item_id=?", itemID);
            if (tri != null)
            {
                addDistinctIDs = (Set<Integer>)((HashSet<Integer>)distinctIDs).clone();
                try
                {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();

                        // Check the item mappings to see if it contains this mapping
                        boolean itemIsMapped = false;
                        int trDistinctID = tr.getIntColumn("distinct_id");
                        if (distinctIDs.contains(trDistinctID))
                        {
                            // Found this mapping
                            results.addRetainedDistinctId(trDistinctID);
                            // Flag it, and remove (-1) from the item mappings
                            itemIsMapped = true;
                            addDistinctIDs.remove(trDistinctID);
                        }

                        // The item is no longer mapped to this community, so remove the database record
                        if (!itemIsMapped)
                        {
                            results.addRemovedDistinctId(trDistinctID);
                            DatabaseManager.delete(internalContext, tr);
                        }
                    }
                }
                finally
                {
                    tri.close();
                }
            }
            else
            {
                addDistinctIDs = distinctIDs;
            }

            // Any remaining mappings need to be added to the database
            for (int distinctID : addDistinctIDs)
            {
                if (distinctID > -1)
                {
                    TableRow row = DatabaseManager.row(table);
                    row.setColumn("item_id", itemID);
                    row.setColumn("distinct_id", distinctID);
                    DatabaseManager.insert(internalContext, row);
                    results.addAddedDistinctId(distinctID);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            String msg = "problem updating distinct mappings: table=" + table + ",item-id=" + itemID;
            throw new BrowseException(msg, e);
        }
        finally
        {
            cleanContext();
        }

        return results;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createDistinctTable(java.lang.String, boolean)
     */
    @Mock
    public String createDistinctTable(String table, boolean execute) throws BrowseException
    {
        try
        {            
            String create = "CREATE TABLE " + table + " (" +
                            "id INTEGER PRIMARY KEY, " +
                            "authority VARCHAR2(100), " +
                            "value " + getValueColumnDefinition() + ", " +
                            "sort_value " + getSortColumnDefinition() +
                            ")";

            if (execute)
            {
                checkContext();
                DatabaseManager.updateQuery(internalContext, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    @Mock
    public String createPrimaryTable(String table, List<Integer> sortCols, boolean execute) throws BrowseException
    {
        try
        {            
            StringBuilder sb = new StringBuilder();

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
                checkContext();
                DatabaseManager.updateQuery(internalContext, createTable);
            }
            return createTable;
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#createSequence(java.lang.String, boolean)
     */
    @Mock
    public String createSequence(String sequence, boolean execute) throws BrowseException
    {
        try
        {            
            String create = "CREATE SEQUENCE " + sequence;
            if (execute)
            {
                checkContext();
                DatabaseManager.updateQuery(internalContext, create);
            }
            return create + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#deleteByItemID(java.lang.String, int)
     */
    @Mock
    public void deleteByItemID(String table, int itemID) throws BrowseException
    {
        try
        {
            checkContext();
            Object[] params = { new Integer(itemID) };
            String dquery = "DELETE FROM " + table + " WHERE item_id=?";
            DatabaseManager.updateQuery(internalContext, dquery, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#deleteCommunityMappings(java.lang.String, int)
     */
    @Mock
    public void deleteCommunityMappings(int itemID)
        throws BrowseException
    {
        try
        {
            checkContext();
            Object[] params = { new Integer(itemID) };
            String dquery = "DELETE FROM Communities2Item WHERE item_id = ?";
      
            DatabaseManager.updateQuery(internalContext, dquery, params);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropIndexAndRelated(java.lang.String, boolean)
     */
    @Mock
    public String dropIndexAndRelated(String table, boolean execute) throws BrowseException
    {
        try
        {
            checkContext();
            String dropper = "DROP TABLE " + table + " CASCADE CONSTRAINTS";
            if (execute)
            {
                DatabaseManager.updateQuery(internalContext, dropper);
            }
            return dropper + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropSequence(java.lang.String, boolean)
     */
    @Mock
    public String dropSequence(String sequence, boolean execute) throws BrowseException
    {
        try
        {
            checkContext();
            String dropSeq = "DROP SEQUENCE " + sequence;
            if (execute)
            {
                DatabaseManager.updateQuery(internalContext, dropSeq);
            }
            return dropSeq + ";";
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#dropView(java.lang.String, boolean)
     */
    @Mock
    public String dropView(String view, boolean execute)
        throws BrowseException
    {
        if (view != null && !"".equals(view))
        {
            try
            {
                checkContext();
                String dropView = "DROP VIEW " + view + " CASCADE CONSTRAINTS";
                if (execute)
                {
                    DatabaseManager.updateQuery(internalContext, dropView);
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
            finally
            {
                cleanContext();
            }
        }

        return "";
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#getDistinctID(java.lang.String, java.lang.String, java.lang.String)
     */
    @Mock
    public int getDistinctID(String table, String value, String authority, String sortValue) throws BrowseException
    {
        TableRowIterator tri = null;

        if (log.isDebugEnabled())
        {
            log.debug("getDistinctID: table=" + table + ",value=" + value + ",authority=" + authority + ",sortValue=" + sortValue);
        }

        try
        {
            checkContext();
            Object[] params;
            String select = "SELECT id FROM " + table;

            if (ConfigurationManager.getBooleanProperty("webui.browse.metadata.case-insensitive", false))
            {
                if (isValueColumnClob())
                    select = select + " WHERE TO_CHAR(value)=?";
                else
                    select = select + " WHERE value=?";
            }
            else
            {
                if (isValueColumnClob())
                    select = select + " WHERE TO_CHAR(value)=?";
                else
                    select = select + " WHERE value=?";
            }

			if (authority != null)
            {
                select += " AND authority = ?";
                params = new Object[]{ value, authority };
            }
   			else
            {
                select += " AND authority IS NULL";
                params = new Object[]{ value };
            }

            tri = DatabaseManager.query(internalContext, select, params);
            int distinctID = -1;
            if (!tri.hasNext())
            {
                distinctID = insertDistinctRecord(table, value, authority, sortValue);
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
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#updateCommunityMappings(int)
     */
    @Mock
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

            checkContext();
            // Find all existing mappings for this item
            TableRowIterator tri = DatabaseManager.queryTable(internalContext, "Communities2Item", "SELECT * FROM Communities2Item WHERE item_id=?", itemID);
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
                            DatabaseManager.delete(internalContext, tr);
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
                    TableRow row = DatabaseManager.row("Communities2Item");
                    row.setColumn("item_id", itemID);
                    row.setColumn("community_id", commID[i]);
                    DatabaseManager.insert(internalContext, row);
                }
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#insertDistinctRecord(java.lang.String, java.lang.String, java.lang.String)
     */
    @Mock
    public int insertDistinctRecord(String table, String value, String authority, String sortValue) throws BrowseException
    {
        if (log.isDebugEnabled())
        {
            log.debug("insertDistinctRecord: table=" + table + ",value=" + value+ ",sortValue=" + sortValue);
        }

        try
        {
            checkContext();
            TableRow dr = DatabaseManager.row(table);
            dr.setColumn("value", utils.truncateValue(value));
            dr.setColumn("sort_value", utils.truncateSortValue(sortValue));
            if (authority != null)
            {
                dr.setColumn("authority", utils.truncateValue(authority,100));
            }
            DatabaseManager.insert(internalContext, dr);
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
        finally
        {
            cleanContext();
        }
    }

    @Mock
    public void insertIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
        try
        {
            checkContext();
            // create us a row in the index
            TableRow row = DatabaseManager.row(table);

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

            DatabaseManager.insert(internalContext, row);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#updateIndex(java.lang.String, int, java.util.Map)
     */
    @Mock
    public boolean updateIndex(String table, int itemID, Map sortCols)
            throws BrowseException
    {
        try
        {
            checkContext();
            boolean rowUpdated = false;
            TableRow row = DatabaseManager.findByUnique(internalContext, table, "item_id", itemID);

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
                DatabaseManager.update(internalContext, row);
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }

        // Return that the original record was found
        return true;
    }

    @Mock
    public List<Integer> deleteMappingsByItemID(String mapTable, int itemID) throws BrowseException
    {
        System.out.println("Map: " + mapTable + ", item: " + itemID);
        List<Integer> distinctIds = new ArrayList<Integer>();
        TableRowIterator tri = null;
        try
        {
            checkContext();
            try
            {
                tri = DatabaseManager.queryTable(internalContext, mapTable, "SELECT * FROM " + mapTable + " WHERE item_id=?", itemID);
                if (tri != null)
                {
                    while (tri.hasNext())
                    {
                        TableRow tr = tri.next();
                        distinctIds.add(tr.getIntColumn("distinct_id"));
                        DatabaseManager.delete(internalContext, tr);
                    }
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
        finally
        {
            cleanContext();
        }

        return distinctIds;
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#pruneDistinct(java.lang.String, java.lang.String)
     */
    @Mock
    public void pruneDistinct(String table, String map, List<Integer> distinctIds) throws BrowseException
    {
        try
        {
            checkContext();
            StringBuilder query = new StringBuilder();
            query.append("DELETE FROM ").append(table).append(" WHERE NOT EXISTS (SELECT 1 FROM ");
            query.append(map).append(" WHERE ").append(map).append(".distinct_id = ").append(table).append(".id)");

            if (distinctIds != null && distinctIds.size() > 0)
            {
                query.append(" AND ").append(table).append(".id=?");
                PreparedStatement stmt = null;
                try
                {
                    stmt = internalContext.getDBConnection().prepareStatement(query.toString());
                    for (Integer distinctId : distinctIds)
                    {
                        stmt.setInt(1, distinctId);
                        stmt.execute();
                        stmt.clearParameters();
                    }
                }
                finally
                {
                    if (stmt != null)
                    {
                        stmt.close();
                    }
                }
            }
            else
            {
                DatabaseManager.updateQuery(internalContext, query.toString());
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#pruneExcess(java.lang.String, java.lang.String)
     */
    @Mock
    public void pruneExcess(String table, boolean withdrawn) throws BrowseException
    {
        try
        {
            checkContext();
            StringBuilder query = new StringBuilder();

            query.append("DELETE FROM ").append(table).append(" WHERE NOT EXISTS (SELECT 1 FROM item WHERE item.item_id=");
            query.append(table).append(".item_id AND ");
            if (withdrawn)
            {
                query.append("item.withdrawn = 1");
            }
            else
            {
                query.append("item.in_archive = 1 AND item.withdrawn = 0");
            }
            query.append(")");
            DatabaseManager.updateQuery(internalContext, query.toString());
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    @Mock
    public void pruneMapExcess(String map, boolean withdrawn, List<Integer> distinctIds) throws BrowseException
    {
        try
        {
            checkContext();
            StringBuilder query = new StringBuilder();

            query.append("DELETE FROM ").append(map).append(" WHERE NOT EXISTS (SELECT 1 FROM item WHERE item.item_id=");
            query.append(map).append(".item_id AND ");
            if (withdrawn)
            {
                query.append("item.withdrawn = 1");
            }
            else
            {
                query.append("item.in_archive = 1 AND item.withdrawn = 0");
            }
            query.append(")");
            if (distinctIds != null && distinctIds.size() > 0)
            {
                query.append(" AND ").append(map).append(".distinct_id=?");
                PreparedStatement stmt = null;
                try
                {
                    stmt = internalContext.getDBConnection().prepareStatement(query.toString());
                    for (Integer distinctId : distinctIds)
                    {
                        stmt.setInt(1, distinctId);
                        stmt.execute();
                        stmt.clearParameters();
                    }
                }
                finally
                {
                    if (stmt != null)
                    {
                        stmt.close();
                    }
                }
            }
            else
            {
                DatabaseManager.updateQuery(internalContext, query.toString());
            }
        }
        catch (SQLException e)
        {
            log.error("caught exception: ", e);
            throw new BrowseException(e);
        }
        finally
        {
            cleanContext();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.browse.BrowseCreateDAO#testTableExistence(java.lang.String)
     */
    @Mock
    public boolean testTableExistence(String table) throws BrowseException
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
            if (c != null && c.isValid())
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
    @Mock
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
    @Mock
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
    @Mock
    private int getValueColumnMaxBytes()
    {
        checkContext();
        int chars = utils.getValueColumnMaxChars();

        if (chars > 2000 || chars < 1)
        {
            return 4000;
        }
        cleanContext();

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
    @Mock
    private int getSortColumnMaxBytes()
    {
        checkContext();
        int chars = utils.getSortColumnMaxChars();

        if (chars > 2000 || chars < 1)
        {
            return 4000;
        }
        cleanContext();

        return chars * 2;
    }

    /**
     * If getValueColumnDefinition() is returning a CLOB definition,
     * then this must return true.
     *
     * @return
     */
    @Mock
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
    @Mock
    private int[] getAllCommunityIDs(int itemId) throws SQLException
    {
        List<Integer> commIdList = new ArrayList<Integer>();

        TableRowIterator tri = null;

        try
        {
            checkContext();
            tri = DatabaseManager.queryTable(internalContext, "Community2Item",
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
            cleanContext();
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
    @Mock
    private Integer getParentCommunityID(int commId) throws SQLException
    {
        TableRowIterator tri = null;

        try
        {
            checkContext();
            tri = DatabaseManager.queryTable(internalContext, "Community2Community",
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
    @Mock
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

    private static class BrowseMappingResults implements MappingResults
    {
        private List<Integer> addedDistinctIds    = new ArrayList<Integer>();
        private List<Integer> retainedDistinctIds = new ArrayList<Integer>();
        private List<Integer> removedDistinctIds  = new ArrayList<Integer>();

        private void addAddedDistinctId(int id)
        {
            addedDistinctIds.add(id);
        }

        private void addRetainedDistinctId(int id)
        {
            retainedDistinctIds.add(id);
        }

        private void addRemovedDistinctId(int id)
        {
            removedDistinctIds.add(id);
        }

        public List<Integer> getAddedDistinctIds()
        {
            return addedDistinctIds;
        }

        public List<Integer> getRetainedDistinctIds()
        {
            return retainedDistinctIds;
        }

        public List<Integer> getRemovedDistinctIds()
        {
            return Collections.unmodifiableList(removedDistinctIds);
        }
    }
}
