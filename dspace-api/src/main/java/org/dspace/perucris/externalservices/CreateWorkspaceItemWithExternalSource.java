/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.LiveImportDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.external.service.impl.ExternalDataServiceImpl;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CreateWorkspaceItemWithExternalSource extends DSpaceRunnable<
       CreateWorkspaceItemWithExternalSourceScriptConfiguration<CreateWorkspaceItemWithExternalSource>> {

    private static final Logger log = LogManager.getLogger(CreateWorkspaceItemWithExternalSource.class);

    private static final int LIMIT = 10;

    private String service;

    private Context context;

    private ItemServiceImpl itemService;

    private CollectionServiceImpl collectionService;

    private ExternalDataService externalDataService;

    private Collection collection;

    private ConfigurationService configurationService;

    private Map<String, LiveImportDataProvider> nameToProvider = new HashMap<String, LiveImportDataProvider>();

    @Override
    public void setup() throws ParseException {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = new DSpace().getServiceManager()
                                  .getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        collectionService = new DSpace().getServiceManager()
                            .getServiceByName(CollectionServiceImpl.class.getName(),CollectionServiceImpl.class);
        externalDataService = new DSpace().getServiceManager()
                             .getServiceByName(ExternalDataServiceImpl.class.getName(), ExternalDataServiceImpl.class);
        nameToProvider.put("scopus", new DSpace().getServiceManager().getServiceByName("scopusLiveImportDataProvider",
                                         LiveImportDataProvider.class));
        this.service = commandLine.getOptionValue('s');
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        assignCurrentUserInContext();
        if (service == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }

        LiveImportDataProvider dataProvider = nameToProvider.get(service);
        if (dataProvider == null) {
            throw new IllegalArgumentException("The " + this.service + " provider does not exist");
        }

        UUID collectionUUID = UUID.fromString(configurationService.getProperty("importworkspaceitem.collection-id"));
        if (Objects.isNull(collectionUUID)) {
            throw new RuntimeException("The UUID of Collection is null.");
        }

        this.collection = collectionService.find(context, collectionUUID);
        if (Objects.isNull(collectionUUID)) {
            throw new RuntimeException("Collection with uuid" + collectionUUID + "does not exist!");
        }

        try {
            context.turnOffAuthorisationSystem();
            performCreatingOfWorkspaceItems(context, dataProvider);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void performCreatingOfWorkspaceItems(Context context,LiveImportDataProvider dataProvider) {
        int currentRecord = 0;
        int totalRecordWorked = 0;
        int countItemsProcessed = 0;
        try {
            Iterator<Item> itemIterator = findItems(context);
            handler.logInfo("Update start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                String id = buildID(item);
                if (StringUtils.isNotBlank(id)) {
                    int recordsFound = dataProvider.getNumberOfResults(id);
                    while (recordsFound == -1 || totalRecordWorked < recordsFound) {
                        totalRecordWorked += fillWorkspaceItems(context, currentRecord, dataProvider, id);
                        currentRecord += LIMIT;
                    }
                }
                countItemsProcessed++;
                if (countItemsProcessed == 20) {
                    context.commit();
                    countItemsProcessed = 0;
                }
            }
            context.commit();
            handler.logInfo("Processed " + totalRecordWorked + " records");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String buildID(Item item) {
        StringBuilder id = new StringBuilder();
        switch (this.service) {
            case "scopus":
                String scopusId = itemService.getMetadataFirstValue(
                                  item, "person", "identifier", "scopus-author-id", Item.ANY);
                if (StringUtils.isNotBlank(scopusId)) {
                    id.append("AU-ID(").append(scopusId).append(")");
                }
                break;
            default:
        }
        return id.toString();
    }

    private int fillWorkspaceItems(Context context, int record, LiveImportDataProvider dataProvider, String id)
            throws SQLException {
        int countDataObjects = 0;
        try {
            for (ExternalDataObject dataObject : dataProvider.searchExternalDataObjects(id, record, LIMIT)) {
                if (!exist(dataObject.getMetadata())) {
                    externalDataService.createWorkspaceItemFromExternalDataObject(context, dataObject, this.collection);
                }
                countDataObjects++;
            }
        } catch (AuthorizeException e) {
            log.error(e.getMessage(), e);
        } catch (SearchServiceException e1) {
            log.error(e1.getMessage(), e1);
        }
        return countDataObjects;
    }

    private boolean exist(List<MetadataValueDTO> metadatas) throws SQLException, SearchServiceException {
        boolean exist = false;
        if (metadatas.size() > 0) {
            switch (this.service) {
                case "scopus":
                    exist = checkScopus(metadatas);
                    break;
                default:
            }
        }
        return exist;
    }

    private boolean checkScopus(List<MetadataValueDTO> metadatas) throws SQLException, SearchServiceException {
        for (MetadataValueDTO mv : metadatas) {
            String element = mv.getElement();
            String qualifier = mv.getQualifier();
            if (StringUtils.equals(element, "identifier") && StringUtils.equals(qualifier, "scopus")) {
                StringBuilder filter = new StringBuilder();
                filter.append("dc.identifier.scopus:").append(mv.getValue());
                Iterator<Item> itemIterator = findItemsByCollection(context, filter.toString());
                if (itemIterator.hasNext()) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private Iterator<Item> findItemsByCollection(Context context, String filter)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries(filter);
        discoverQuery.addFilterQueries("location.coll:" + this.collection.getID());
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private Iterator<Item> findItems(Context context)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        setFilter(discoverQuery, this.service);
        discoverQuery.addFilterQueries("search.entitytype:Person");
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void setFilter(DiscoverQuery discoverQuery, String service) {
        if ("scopus".equals(service)) {
            discoverQuery.addFilterQueries("person.identifier.scopus-author-id:*");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CreateWorkspaceItemWithExternalSourceScriptConfiguration<CreateWorkspaceItemWithExternalSource>
        getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("create-wsi",
                                                CreateWorkspaceItemWithExternalSourceScriptConfiguration.class);
    }
}