/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.migration;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

/**
 * This Flyway Java migration deletes any legacy DBMS browse tables found in
 * the database. See https://jira.duraspace.org/browse/DS-2188.
 * 
 * @author Tim Donohue
 */
public class V6_0_2016_01_26__DS_2188_Remove_DBMS_Browse_Tables implements JdbcMigration, MigrationChecksumProvider
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(V6_0_2016_01_26__DS_2188_Remove_DBMS_Browse_Tables.class);
    
    /* The checksum to report for this migration (when successful) */
    private int checksum = -1;
    
    @Override
    public void migrate(Connection connection) throws Exception, SQLException
    {
        removeDBMSBrowseTables(connection);
    }

   /**
    * Delete all the existing DBMS browse tables. The DBMS browse system
    * has been replaced by Discovery / Solr.
    * <P>
    * NOTE: This method was based on the "clearDatabase()" method of the old
    * DSpace 5.x `IndexBrowse` class. As such, it is essentially performing
    * the same as the old "./dspace index-db-browse -f -d" command. But, it
    * also removes old, obsolete item_count tables and communities2item table
    * 
    * @param connection Database Connection
    * @throws BrowseException
    */
    private void removeDBMSBrowseTables(Connection connection)
		throws BrowseException
    {
        // Browse index tables start at index=1
        int i = 1;
        
        // Keep looping (incrementing our index by 1) until we've hit three index
        // tables that have not been found.
        // We don't actually know how many index tables will be in each database,
        // and there are no guarrantees it'll match the highest index of the site's 
        // existing "webui.browse.index.#" settings.
        // Since that's the case, we'll just keep searching for index tables,
        // until we encounter a total of three that are not found.
        int countTablesNotFound = 0;
        while(countTablesNotFound < 3)
        {
            String tableName = BrowseIndex.getTableName(i, false, false, false, false);
            String distinctTableName = BrowseIndex.getTableName(i, false, false, true, false);
            String distinctMapName = BrowseIndex.getTableName(i, false, false, false, true);
            String sequence = BrowseIndex.getSequenceName(i, false, false);
            String mapSequence = BrowseIndex.getSequenceName(i, false, true);
            String distinctSequence = BrowseIndex.getSequenceName(i, true, false);

            // These views have not been used for some time, but as we are
            // cleaning the database, they may exist and need to be removed
            String colViewName = BrowseIndex.getTableName(i, false, true, false, false);
            String comViewName = BrowseIndex.getTableName(i, true, false, false, false);
            String distinctColViewName = BrowseIndex.getTableName(i, false, true, false, true);
            String distinctComViewName = BrowseIndex.getTableName(i, true, false, false, true);

            if(DatabaseUtils.tableExists(connection, tableName, false))
            {
                // Found an index table. Deleting it & everything related to it

                // Drop table
                dropTable(connection, tableName);
                
                // Drop Sequence
                dropSequence(connection, sequence);
                
                // Drop Collection View
                dropView(connection, colViewName);
                
                // Drop Community View
                dropView(connection, comViewName); 
            }
            
            // Check for existence of "distinct table"
            if (DatabaseUtils.tableExists(connection, distinctTableName, false))
            {
                // Found. Need to delete all its resources

                // Drop table
                dropTable(connection, distinctTableName);
                
                // Drop Map table
                dropTable(connection, distinctMapName);
                
                // Drop sequence
                dropSequence(connection, distinctSequence);
                
                // Drop Map Sequence
                dropSequence(connection, mapSequence);
                
                // Drop Collection View
                dropView(connection, distinctColViewName);
                
                // Drop Community View
                dropView(connection, distinctComViewName);
            }
            else
            {
                // increment our "not found" count
                countTablesNotFound++;
            }
            
            // increment our table index
            i++;
        }

        // Drop all Item browse index tables
        dropItemTables(connection, BrowseIndex.getItemBrowseIndex());
        dropItemTables(connection, BrowseIndex.getWithdrawnBrowseIndex());
        dropItemTables(connection, BrowseIndex.getPrivateBrowseIndex());
        
        // Check for existence of "communities2item" table
        // This is no longer used, see DS-2578
        if (DatabaseUtils.tableExists(connection, "communities2item", false))
        {
            dropTable(connection, "communities2item");
            dropSequence(connection, "communities2item_seq");
        }
        
        // Check for existence of "community_item_count" table
        // This is no longer used, as item counts are now in Solr
        if (DatabaseUtils.tableExists(connection, "community_item_count", false))
        {
            dropTable(connection, "community_item_count");
        }
        
        // Check for existence of "collection_item_count" table
        // This is no longer used, as item counts are now in Solr
        if (DatabaseUtils.tableExists(connection, "collection_item_count", false))
        {
            dropTable(connection, "collection_item_count");
        }
        
        // NOTE: the old "community2item" View was already dropped by
        //   V6.0_2015.03.07__DS-2701_Hibernate_migration.sql
    }
    
    
    /**
     * Drop a table by name. If an error occurs, just log a warning, as its possible
     * some sites may have deleted the table manually.
     * 
     * @param connection Database Connection
     * @param tableName Table Name
     */
    private void dropTable(Connection connection, String tableName)
    {
        try
        {
            // Drop table & increment our flyway checksum
            this.checksum += MigrationUtils.dropDBTable(connection, tableName);
        }
        catch(SQLException sqe){
            // Ignore any errors (don't cause migration to fail), but log as warning
            log.warn("Database Table '" + tableName + " could not be dropped during migration. This warning may be ignored, if this table was already deleted.", sqe);
        }
    }
    
    /**
     * Drop a sequence by name. If an error occurs, just log a warning, as its possible
     * some sites may have deleted it manually.
     * 
     * @param connection Database Connection
     * @param sequenceName Sequence Name
     */
    private void dropSequence(Connection connection, String sequenceName)
    {
        try
        {
            // Drop sequence & increment our flyway checksum
            this.checksum +=  MigrationUtils.dropDBSequence(connection, sequenceName);
        }
        catch(SQLException sqe){
            // Ignore any errors (don't cause migration to fail), but log as warning
            log.warn("Database Sequence '" + sequenceName + " could not be dropped during migration. This warning may be ignored, if this sequence was already deleted.", sqe);
        }
    }
    
    /**
     * Drop a view by name. If an error occurs, just log a warning, as its possible
     * some sites may have deleted the view manually.
     * 
     * @param connection Database Connection
     * @param viewName View Name
     */
    private void dropView(Connection connection, String viewName)
    {
        try
        {
            // Drop view & increment our flyway checksum
            this.checksum +=  MigrationUtils.dropDBView(connection, viewName);
        }
        catch(SQLException sqe){
            // Ignore any errors (don't cause migration to fail), but log as warning
            log.warn("Database View '" + viewName + " could not be dropped during migration. This warning may be ignored, if this view was already deleted.", sqe);
        }
    }
    
    /**
     * drop the tables and related database entries for the internal
     * 'item' tables
     * @param connection Database Connection
     * @param bix BrowseIndex
     * @throws BrowseException
     */
    private void dropItemTables(Connection connection, BrowseIndex bix) throws BrowseException
    {
        if(DatabaseUtils.tableExists(connection, bix.getTableName()))
        {
            String tableName = bix.getTableName();
            String sequence = bix.getSequenceName(false, false);
            
            // Drop table
            dropTable(connection, tableName);
            
            // Drop sequence
            dropSequence(connection, sequence);

            // These views are no longer used, but as we are cleaning the database,
            // they may exist and need to be removed
            String colViewName = bix.getTableName(false, true, false, false);
            String comViewName = bix.getTableName(true, false, false, false);
            
            // Drop Collection View
            dropView(connection, colViewName);
            
            // Drop Community View
            dropView(connection, comViewName);
        }
    }
    
    @Override
    public Integer getChecksum() {
        return checksum;
    }
}
