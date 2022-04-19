package org.dspace.app.itemdbstatus;

import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD;
import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD_PREDB;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Created by kristof on 19/04/2022
 */
public class ItemDatabaseStatusCli extends DSpaceRunnable<ItemDatabaseStatusCliScriptConfiguration> {
    /* Log4j logger */
    private static final Logger log = Logger.getLogger(ItemDatabaseStatusCli.class);

    private SearchService searchService;
    private ItemService itemService;
    private IndexingService indexingService;
    private SolrSearchCore solrSearchCore;
    private IndexObjectFactoryFactory indexObjectServiceFactory;

    @Override
    public ItemDatabaseStatusCliScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("item-database-status", ItemDatabaseStatusCliScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        searchService = SearchUtils.getSearchService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
                            .getServiceByName(IndexingService.class.getName(), IndexingService.class);
        solrSearchCore = DSpaceServicesFactory.getInstance().getServiceManager()
                            .getServiceByName(SolrSearchCore.class.getName(), SolrSearchCore.class);
        indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();
    }

    @Override
    public void internalRun() throws Exception {
        logAndOut("Starting Item Database Status update...");

        Context context = new Context();

        try {
            context.turnOffAuthorisationSystem();
            performStatusUpdate(context);
        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }
    }

    private void performStatusUpdate(Context context) throws SearchServiceException, SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(STATUS_FIELD + ":" + STATUS_FIELD_PREDB);
        solrQuery.addFilterQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + IndexableItem.TYPE);
        solrQuery.addField(SearchUtils.RESOURCE_ID_FIELD);
        solrQuery.addField(SearchUtils.RESOURCE_UNIQUE_ID);
        QueryResponse response = solrSearchCore.getSolr().query(solrQuery, solrSearchCore.REQUEST_METHOD);

        if (response != null) {
            for (SolrDocument doc : response.getResults()) {
                String uuid = (String) doc.getFirstValue(SearchUtils.RESOURCE_ID_FIELD);
                String uniqueId = (String) doc.getFirstValue(SearchUtils.RESOURCE_UNIQUE_ID);
                logAndOut("Processing item with UUID: " + uuid);

                Optional<IndexableObject> indexableObject = Optional.empty();
                try {
                    indexableObject = indexObjectServiceFactory
                            .getIndexableObjectFactory(uniqueId).findIndexableObject(context, uuid);
                } catch (SQLException e) {
                    log.warn("An exception occurred when attempting to retrieve item with UUID \"" + uuid +
                            "\" from the database, removing related solr document", e);
                }

                try {
                    if (indexableObject.isPresent()) {
                        logAndOut("Item exists in DB, updating solr document");
                        updateItem(context, indexableObject.get());
                    } else {
                        logAndOut("Item doesn't exist in DB, removing solr document");
                        removeItem(context, uniqueId);
                    }
                } catch (SQLException | IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        indexingService.commit();
    }

    private void updateItem(Context context, IndexableObject indexableObject) throws SQLException {
        indexingService.indexContent(context, indexableObject, true);
    }

    private void removeItem(Context context, String uniqueId) throws IOException, SQLException {
        indexingService.unIndexContent(context, uniqueId);
    }

    private void logAndOut(String message) {
        log.info(message);
        System.out.println(message);
    }
}
