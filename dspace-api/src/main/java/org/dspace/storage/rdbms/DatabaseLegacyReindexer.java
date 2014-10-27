/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import org.dspace.browse.IndexBrowse;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.search.DSIndexer;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a FlywayCallback class which automatically reindexes Database
 * contents into your Legacy search/browse engine of choice. It is NOT needed
 * for Solr, but is necessary for Lucene / RDBMS indexes.
 * <P>
 * Reindexing is performed AFTER any database migration or repair. This
 * ensures that your search/browse indexes are auto-updated post-upgrade.
 * 
 * @author Tim Donohue
 */
public class DatabaseLegacyReindexer implements FlywayCallback
{
    /** logging category */
    private static final Logger log = LoggerFactory.getLogger(DatabaseLegacyReindexer.class);
    
    /**
     * Method to actually reindex all database contents. This method is "smart"
     * in that it determines which indexing consumer(s) you have enabled, 
     * and then ensures each is reindexed appropriately.
     * <P>
     * NOTE: However, because Solr is never running when the Database is initialized,
     * this reindexer only really works for Lucene / DBMS. Once those are obsolete,
     * this can be safely removed, along with the reference to it in
     * DatabaseUtils.setupFlyway()
     */
    private void reindex()
    {
        Context context = null;
        try
        {
            context = new Context();

            // What indexing consumer(s) are configured in this DSpace?
            ConfigurationService config = new DSpace().getConfigurationService();
            String consumers = config.getPropertyAsType("event.dispatcher.default.consumers", ""); // Avoid null pointer
            List<String> consumerList = Arrays.asList(consumers.split("\\s*,\\s*"));

            // If Discovery indexing is enabled
            if (consumerList.contains("discovery"))
            {
                // Do nothing
                // Because Solr is normally not running when the DatabaseManager initializes,
                // Discovery autoindexing takes place in DatabaseUtils.checkReindexDiscovery(),
                // which is automatically called when Discover initializes.
            }
            
            // If Lucene indexing is enabled
            if (consumerList.contains("search"))
            {
                log.info("Reindexing all content in Lucene search engine");
                // Clean and update Lucene index
                DSIndexer.cleanIndex(context);
                DSIndexer.updateIndex(context, true);
                log.info("Reindexing is complete");
            }
            
            // If traditional DBMS browse indexing is enabled
            if (consumerList.contains("browse"))
            {
                log.info("Reindexing all content in DBMS Browse tables");
                // Rebuild browse tables to perform a full index 
                // (recreating tables as needed)
                IndexBrowse indexer = new IndexBrowse(context);
                indexer.setRebuild(true);
                indexer.setExecute(true);
                indexer.initBrowse();
                // Since the browse index is in the DB, we must commit & close context
                context.complete();
                log.info("Reindexing is complete");
            }
            else
            {
                log.info("Checking for (and cleaning up) unused DBMS Browse tables");
                // If traditional browse tables are not being used, 
                // then clean them up from database (they can always be recreated later)
                IndexBrowse indexer = new IndexBrowse(context);
                indexer.clearDatabase();
                // Commit changes to browse tables & close context
                context.complete();
            }
        }
        catch(Exception e)
        {
            log.error("Error attempting to reindex all contents for search/browse", e);
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
        }
    }
    
    
    @Override
    public void afterClean(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void afterEachMigrate(Connection connection, MigrationInfo info)
    {
        // do nothing
    }
    
    @Override
    public void afterInfo(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void afterInit(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void afterMigrate(Connection connection)
    {
        // Reindex after a database migration (upgrade)
        reindex();
    }
    
    @Override
    public void afterRepair(Connection connection)
    {
        // Reindex after a database repair
        reindex();
    }
    
    @Override
    public void afterValidate(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeClean(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeEachMigrate(Connection connection, MigrationInfo info)
    {
        // do nothing
    }
    
    @Override
    public void beforeInfo(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeInit(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeMigrate(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeRepair(Connection connection)
    {
        // do nothing
    }
    
    @Override
    public void beforeValidate(Connection connection)
    {
        // do nothing
    }
    
}
