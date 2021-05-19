/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.script2externalservices;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.CollectionServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.LiveImportDataProvider;
import org.dspace.external.service.ExternalDataService;
import org.dspace.external.service.impl.ExternalDataServiceImpl;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.hibernate.LazyInitializationException;

/**
 * Implementation of {@link DSpaceRunnable}
 * to import Publications from external service as Scopus and Web Of Science.
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

    @SuppressWarnings("rawtypes")
    private WorkflowService workflowService;

    @Override
    public void setup() throws ParseException {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        ServiceManager serviceManager = new DSpace().getServiceManager();
        itemService = serviceManager.getServiceByName(ItemServiceImpl.class.getName(), ItemServiceImpl.class);
        collectionService = serviceManager
                            .getServiceByName(CollectionServiceImpl.class.getName(),CollectionServiceImpl.class);
        externalDataService = serviceManager
                             .getServiceByName(ExternalDataServiceImpl.class.getName(), ExternalDataServiceImpl.class);
        nameToProvider.put("scopus", serviceManager.getServiceByName("scopusLiveImportDataProvider",
                                         LiveImportDataProvider.class));
        nameToProvider.put("wos", serviceManager.getServiceByName("wosLiveImportDataProvider",
                                      LiveImportDataProvider.class));
        workflowService = WorkflowServiceFactory.getInstance()
            .getWorkflowService();
        this.service = commandLine.getOptionValue('s');
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context();
        context.setCurrentUser(findEPerson());
        if (service == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }

        LiveImportDataProvider dataProvider = nameToProvider.get(service);
        if (dataProvider == null) {
            throw new IllegalArgumentException("The " + this.service + " provider does not exist");
        }

        UUID collectionUUID = getCollectionUUID();
        if (Objects.isNull(collectionUUID)) {
            this.collection = getPublicationCollection();
        } else {
            this.collection = collectionService.find(context, collectionUUID);
        }
        if (Objects.isNull(this.collection)) {
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

    private UUID getCollectionUUID() {
        switch (this.service) {
            case "scopus":
                return getUuid("scopus.importworkspaceitem.collection-id");
            case "wos":
                return getUuid("wos.importworkspaceitem.collection-id");
            default:
        }
        return null;
    }

    private UUID getUuid(final String property) {
        final String propertyValue = configurationService.getProperty(property);
        return StringUtils.isBlank(propertyValue) ? null : UUID.fromString(propertyValue);
    }

    private EPerson findEPerson() throws SQLException {
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        String email = commandLine.getOptionValue('e');
        if (StringUtils.isNotBlank(email)) {
            EPerson byEmail = ePersonService.findByEmail(context, email);
            if (Objects.nonNull(byEmail)) {
                return byEmail;
            }
        }
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            return ePersonService.find(context, uuid);
        }
        return null;
    }

    private void performCreatingOfWorkspaceItems(Context context,LiveImportDataProvider dataProvider) {

        int totalRecordWorked = 0;
        int countItemsProcessed = 0;
        try {
            Iterator<Item> itemIterator = findItems(context);
            handler.logInfo("Update start");
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                String id = buildID(item);
                if (StringUtils.isNotBlank(id)) {
                    int currentRecord = 0;
                    int recordsFound = dataProvider.getNumberOfResults(id);
                    int userPublicationsProcessed = 0;
                    int iterations = recordsFound <= 0 ? 0 : (recordsFound / LIMIT) + 1;
                    for (int i = 1; i <= iterations; i++) {
                        userPublicationsProcessed += fillWorkspaceItems(context, currentRecord, dataProvider, item, id);
                        currentRecord += LIMIT;
                    }
                    totalRecordWorked += userPublicationsProcessed;
                    if (userPublicationsProcessed >= 20) {
                        context.commit();
                        // to ensure that collection's template item is fully initialized
                        reloadCollectionIfNeeded();
                    }
                }
                countItemsProcessed++;
                if (countItemsProcessed == 20) {
                    context.commit();
                    countItemsProcessed = 0;
                    // to ensure that collection's template item is fully initialized
                    reloadCollectionIfNeeded();
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

    /**
     * utility method to check that we have collection's item fully loaded with template item fully initialized,
     * in order to avoid lazy initialization exceptions.
     *
     * @throws SQLException
     */
    private void reloadCollectionIfNeeded() throws SQLException {
        boolean needsReload;
        try {
            needsReload = Objects.isNull(this.collection.getTemplateItem()) ||
                CollectionUtils.isEmpty(this.collection.getTemplateItem().getMetadata());
        } catch (LazyInitializationException e) {
            log.warn(e.getMessage());
            needsReload = true;
        }
        if (needsReload) {
            log.debug("Reloading collection");
            this.collection = this.context.reloadEntity(this.collection);
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
            case "wos":
                String orcidId = itemService.getMetadataFirstValue(item, "person", "identifier", "orcid", Item.ANY);
                String rid = itemService.getMetadataFirstValue(item, "person", "identifier", "rid", Item.ANY);
                if (StringUtils.isNotBlank(orcidId) && StringUtils.isNotBlank(rid)) {
                    id.append("AI=(").append(orcidId).append(" OR ").append(rid).append(")");
                } else if (StringUtils.isNotBlank(orcidId)) {
                    id.append("AI=(").append(orcidId).append(")");
                } else if (StringUtils.isNotBlank(rid)) {
                    id.append("AI=(").append(rid).append(")");
                }
                break;
            default:
        }
        return id.toString();
    }

    private int fillWorkspaceItems(Context context, int record, LiveImportDataProvider dataProvider,
            Item item, String id) throws SQLException {
        int countDataObjects = 0;
        try {
            for (ExternalDataObject dataObject : dataProvider.searchExternalDataObjects(id, record, LIMIT)) {
                if (!exist(dataObject.getMetadata())) {
                    WorkspaceItem wsItem = externalDataService.createWorkspaceItemFromExternalDataObject(context,
                                                               dataObject, this.collection);
                    for (List<MetadataValueDTO> metadataList : metadataValueToAdd(wsItem.getItem())) {
                        addMetadata(wsItem.getItem(), metadataList);
                    }
                    workflowService.start(context, wsItem);
                }
                countDataObjects++;
            }
        } catch (AuthorizeException | IOException | WorkflowException e) {
            log.error(e.getMessage(), e);
        }
        return countDataObjects;
    }

    private void addMetadata(Item item, List<MetadataValueDTO> metadataList) throws SQLException {
        for (MetadataValueDTO metadataValue : metadataList) {
            itemService.addMetadata(context, item, metadataValue.getSchema(), metadataValue.getElement(),
                metadataValue.getQualifier(), null, metadataValue.getValue());
        }
    }

    private boolean exist(List<MetadataValueDTO> metadatas) throws SQLException {
        if (metadatas.size() == 0) {
            return false;
        }
        MetadataValueDTO metadata = getMetadataToChech();
        for (MetadataValueDTO mv : metadatas) {
            String schema = mv.getSchema();
            String element = mv.getElement();
            String qualifier = mv.getQualifier();
            if (StringUtils.equals(schema, metadata.getSchema()) && StringUtils.equals(element, metadata.getElement())
                    && StringUtils.equals(qualifier, metadata.getQualifier())) {

                String value = (mv.getValue()).replaceAll(":", "");
                StringBuilder filter = new StringBuilder();
                filter.append(metadata.getSchema()).append(".").append(metadata.getElement());
                if (StringUtils.isNotBlank(metadata.getQualifier())) {
                    filter.append(".").append(metadata.getQualifier()).append(":").append(value);
                } else {
                    filter.append(":").append(value);
                }
                try {
                    Iterator<Item> itemIterator = findItemsInDSpace(context, filter.toString());
                    if (itemIterator.hasNext()) {
                        return true;
                    }
                } catch (SearchServiceException e) {
                    log.error(e.getMessage(), e);
                }
                return false;
            }
        }
        return false;
    }

    private MetadataValueDTO getMetadataToChech() {
        MetadataValueDTO metadata = new MetadataValueDTO();
        switch (this.service) {
            case "scopus":
                metadata.setSchema("dc");
                metadata.setElement("identifier");
                metadata.setQualifier("scopus");
                break;
            case "wos":
                metadata.setSchema("dc");
                metadata.setElement("identifier");
                metadata.setQualifier("isi");
                break;
            default:
        }
        return metadata;
    }

    private Iterator<Item> findItemsInDSpace(Context context, String filter)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        discoverQuery.addFilterQueries(filter);
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
        if ("wos".equals(service)) {
            discoverQuery.addFilterQueries("person.identifier.orcid:* OR person.identifier.rid:*");
        }
    }

    private List<List<MetadataValueDTO>> metadataValueToAdd(Item item) {
        switch (this.service) {
            case "scopus":
                return Collections.singletonList(metadataList(item, "scopus-author-id"));
            case "wos":
                return Arrays.asList(
                    metadataList(item, "orcid"),
                    metadataList(item, "rid")
                );
            default:
                return Collections.emptyList();
        }
    }

    private List<MetadataValueDTO> metadataList(Item item, String identifier) {
        return itemService.getMetadata(item, "person", "identifier", identifier, Item.ANY)
            .stream().sorted(Comparator.comparingInt(MetadataValue::getPlace))
            .map(md -> new MetadataValueDTO("cris", "author", identifier, null,
                md.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CreateWorkspaceItemWithExternalSourceScriptConfiguration<CreateWorkspaceItemWithExternalSource>
        getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("import-publications",
                                                CreateWorkspaceItemWithExternalSourceScriptConfiguration.class);
    }

    private Collection getPublicationCollection() {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableCollection.TYPE);
        discoverQuery.setMaxResults(1);
        discoverQuery.addFilterQueries("search.entitytype:Publication");
        Iterator<Collection> collections = new DiscoverResultIterator<Collection, UUID>(context, discoverQuery);
        while (collections.hasNext()) {
            return collections.next();
        }
        return null;
    }

    public Map<String, LiveImportDataProvider> getNameToProvider() {
        return nameToProvider;
    }

    public void setNameToProvider(Map<String, LiveImportDataProvider> nameToProvider) {
        this.nameToProvider = nameToProvider;
    }

}
