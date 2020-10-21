/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.crosswalks.FileNameDisseminator;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryRelatedItemConfiguration;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DSpaceRunnable} to export multiple items in the given format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkItemExport extends DSpaceRunnable<BulkItemExportScriptConfiguration<BulkItemExport>> {

    private static final int QUERY_PAGINATION_SIZE = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkItemExport.class);


    private CollectionService collectionService;

    private CommunityService communityService;

    private ItemService itemService;

    private DiscoveryConfigurationService discoveryConfigurationService;


    private String query;

    private String scope;

    private String configuration;

    private String[] filters;

    private String entityType;

    private String sort;

    private String exportFormat;

    private Context context;

    @Override
    public void setup() throws ParseException {

        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.communityService = ContentServiceFactory.getInstance().getCommunityService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.discoveryConfigurationService = new DSpace().getSingletonService(DiscoveryConfigurationService.class);

        this.query = commandLine.getOptionValue('q');
        this.scope = commandLine.getOptionValue('s');
        this.configuration = commandLine.getOptionValue('c');
        this.filters = parseSearchFilters(commandLine.getOptionValue("sf"));
        this.entityType = commandLine.getOptionValue('t');
        this.sort = commandLine.getOptionValue("so");
        this.exportFormat = commandLine.getOptionValue('f');
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();

        if (exportFormat == null) {
            throw new IllegalArgumentException("The export format must be provided");
        }

        StreamDisseminationCrosswalk streamDisseminationCrosswalk = getCrosswalkByName(exportFormat);
        if (streamDisseminationCrosswalk == null) {
            throw new IllegalArgumentException("No dissemination configured for format " + exportFormat);
        }

        try {
            Iterator<Item> itemsIterator = searchItemToExport();
            performExport(itemsIterator, streamDisseminationCrosswalk);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }

    }

    private void performExport(Iterator<Item> itemsIterator, StreamDisseminationCrosswalk crosswalk) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, itemsIterator, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        String name = getFileName(crosswalk);
        handler.writeFilestream(context, name, in, crosswalk.getMIMEType());
        handler.logInfo("Items exported successfully into file named " + name);
    }

    private Iterator<Item> searchItemToExport() throws SearchServiceException {
        IndexableObject<?, ?> scopeObject = resolveScope();
        DiscoveryConfiguration discoveryConfiguration = discoveryConfigurationService
            .getDiscoveryConfigurationByNameOrDso(configuration, scopeObject);

        boolean isRelatedItem = discoveryConfiguration != null &&
            discoveryConfiguration instanceof DiscoveryRelatedItemConfiguration;

        DiscoverQuery discoverQuery = buildDiscoveryQuery();

        if (isRelatedItem) {
            return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
        } else {
            return new DiscoverResultIterator<Item, UUID>(context, scopeObject, discoverQuery);
        }
    }

    private IndexableObject<?, ?> resolveScope() {
        IndexableObject<?, ?> scopeObj = null;
        if (StringUtils.isBlank(scope)) {
            return scopeObj;
        }

        try {

            UUID uuid = UUID.fromString(scope);
            scopeObj = new IndexableCommunity(communityService.find(context, uuid));
            if (scopeObj.getIndexedObject() == null) {
                scopeObj = new IndexableCollection(collectionService.find(context, uuid));
            }
            if (scopeObj.getIndexedObject() == null) {
                scopeObj = new IndexableItem(itemService.find(context, uuid));
            }

        } catch (IllegalArgumentException ex) {
            String message = "The given scope string " + trimToEmpty(scope) + " is not a UUID";
            handler.logWarning(message);
        } catch (SQLException ex) {
            String message = "Unable to retrieve DSpace Object with ID " + trimToEmpty(scope) + " from the database";
            handler.logWarning(message);
            LOGGER.warn(message, ex);
        }

        return scopeObj;
    }

    private DiscoverQuery buildDiscoveryQuery() {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(query);
        discoverQuery.setMaxResults(QUERY_PAGINATION_SIZE);
        return discoverQuery;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkItemExportScriptConfiguration<BulkItemExport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("bulk-item-export",
            BulkItemExportScriptConfiguration.class);
    }

    private String[] parseSearchFilters(String searchFilters) {
        return searchFilters != null ? searchFilters.split("&") : new String[] {};
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private String getFileName(StreamDisseminationCrosswalk streamDisseminationCrosswalk) {
        if (streamDisseminationCrosswalk instanceof FileNameDisseminator) {
            return ((FileNameDisseminator) streamDisseminationCrosswalk).getFileName();
        } else {
            return "export-result";
        }
    }

    private StreamDisseminationCrosswalk getCrosswalkByName(String name) {
        return (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
            .getNamedPlugin(StreamDisseminationCrosswalk.class, name);
    }

}
