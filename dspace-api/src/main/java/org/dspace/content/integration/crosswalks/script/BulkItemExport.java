/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.crosswalks.FileNameDisseminator;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultItemIterator;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.configuration.DiscoveryRelatedItemConfiguration;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    private static final String FILTER_OPERATOR_SEPARATOR = ",";

    private static final String SORT_SEPARATOR = ",";

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkItemExport.class);


    private CollectionService collectionService;

    private CommunityService communityService;

    private ItemService itemService;

    private DiscoveryConfigurationService discoveryConfigurationService;

    private ConfigurationService configurationService;

    private AuthorizeService authorizeService;

    private GroupService groupService;

    private DiscoverQueryBuilder queryBuilder;

    private String query;

    private String scope;

    private String configuration;

    private String searchFilters;

    private String entityType;

    private String sort;

    private String exportFormat;

    private String selectedItems;

    private Context context;

    private Integer limit;

    private Integer offset;

    @Override
    public void setup() throws ParseException {

        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.communityService = ContentServiceFactory.getInstance().getCommunityService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.discoveryConfigurationService = new DSpace().getSingletonService(DiscoveryConfigurationService.class);
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.groupService = EPersonServiceFactory.getInstance().getGroupService();
        this.queryBuilder = SearchUtils.getQueryBuilder();

        this.query = commandLine.getOptionValue('q');
        this.scope = commandLine.getOptionValue('s');
        this.configuration = commandLine.getOptionValue('c');
        this.searchFilters = commandLine.getOptionValue("sf");
        this.entityType = commandLine.getOptionValue('t');
        this.sort = commandLine.getOptionValue("so");
        this.exportFormat = commandLine.getOptionValue('f');
        this.selectedItems = commandLine.getOptionValue("si");

        if (StringUtils.isNotBlank(commandLine.getOptionValue("o"))) {
            this.offset = Integer.valueOf(commandLine.getOptionValue("o"));
        }

        if (StringUtils.isNotBlank(commandLine.getOptionValue("l"))) {
            this.limit = Integer.valueOf(commandLine.getOptionValue("l"));
        }
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context(Context.Mode.READ_ONLY);
        assignCurrentUserInContext(context);
        assignSpecialGroupsInContext();
        assignHandlerLocaleInContext();

        if (StringUtils.isBlank(exportFormat)) {
            handler.logError("The export format must be provided");
            throw new IllegalArgumentException("The export format must be provided");
        }

        StreamDisseminationCrosswalk streamDisseminationCrosswalk = getCrosswalkByType(exportFormat);
        if (streamDisseminationCrosswalk == null) {
            handler.logError("No dissemination configured for format " + exportFormat);
            throw new IllegalArgumentException("No dissemination configured for format " + exportFormat);
        }

        int maxResults = maxResults();
        if (maxResults == 0) {
            throw new AuthorizeException("You are not allowed to run the export process");
        }
        try {
            String[] items = StringUtils.isNotBlank(this.selectedItems) ? selectedItems.split(";") : null;
            this.query = Objects.isNull(items) || items.length == 0 ? this.query : buildQuery(items);
            if (maxResults > 0) {
                handler.logInfo("Export will be limited to " + maxResults + " items.");
            }
            DiscoverResultItemIterator itemsIterator = searchItemsToExport(maxResults,
                    streamDisseminationCrosswalk.isPubliclyReadable());
            handler.logInfo("Found " + Math.min(itemsIterator.getTotalSearchResults(),
                                                maxResults > 0 ? maxResults : Integer.MAX_VALUE) +
                                " items to export");

            performExport(itemsIterator, streamDisseminationCrosswalk);

            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    private String buildQuery(String[] items) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (StringUtils.isNotBlank(query.toString())) {
                query.append(" OR ");
            }
            query.append("search.uniqueid:Item-").append(items[i]);
        }
        return query.toString();
    }

    private void assignHandlerLocaleInContext() {
        if (
                this.handler != null &&
                this.context != null &&
                this.handler.getLocale() != null &&
                !this.handler.getLocale().equals(this.context.getCurrentLocale())
        ) {
            this.context.setCurrentLocale(this.handler.getLocale());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkItemExportScriptConfiguration<BulkItemExport> getScriptConfiguration() {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        return serviceManager.getServiceByName("bulk-item-export", BulkItemExportScriptConfiguration.class);
    }

    private void performExport(Iterator<Item> itemsIterator, StreamDisseminationCrosswalk crosswalk) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, itemsIterator, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        String name = getFileName(crosswalk);
        context.setMode(Context.Mode.READ_WRITE);
        handler.writeFilestream(context, name, in, crosswalk.getMIMEType(), crosswalk.isPubliclyReadable());
        handler.logInfo("Items exported successfully into file named " + name);
    }

    private DiscoverResultItemIterator searchItemsToExport(int maxResults, boolean onlyPublic)
            throws SearchServiceException, SQLException {
        IndexableObject<?, ?> scopeObject = resolveScope();
        DiscoveryConfiguration discoveryConfiguration = discoveryConfigurationService
            .getDiscoveryConfigurationByNameOrIndexableObject(context, configuration, scopeObject);

        boolean isRelatedItem = discoveryConfiguration != null &&
            discoveryConfiguration instanceof DiscoveryRelatedItemConfiguration;

        DiscoverQuery discoverQuery = buildDiscoveryQuery(discoveryConfiguration, scopeObject, onlyPublic);

        if (isRelatedItem) {
            return new DiscoverResultItemIterator(context, discoverQuery, maxResults);
        } else {
            return new DiscoverResultItemIterator(context, scopeObject, discoverQuery, maxResults);
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

    private DiscoverQuery buildDiscoveryQuery(DiscoveryConfiguration discoveryConfiguration,
        IndexableObject<?, ?> scope, boolean onlyPublic) throws SQLException, SearchServiceException {

        List<String> dsoTypes = List.of(IndexableItem.TYPE, IndexableWorkspaceItem.TYPE, IndexableWorkflowItem.TYPE,
            IndexablePoolTask.TYPE, IndexableClaimedTask.TYPE);

        String sortBy = null;
        String sortOrder = null;
        if (StringUtils.isNotBlank(sort)) {
            String[] sortSections = sort.split(SORT_SEPARATOR);
            sortBy = sortSections[0];
            sortOrder = sortSections.length > 1 ? sortSections[1] : null;
        }

        List<QueryBuilderSearchFilter> filters = parseSearchFilters();

        DiscoverQuery discoverQuery =
            queryBuilder.buildQuery(
                context, scope, discoveryConfiguration, query, filters,
                dsoTypes, QUERY_PAGINATION_SIZE,
                Optional.ofNullable(this.offset).map(Long::valueOf).orElse(null),
                sortBy, sortOrder
            );

        if (onlyPublic) {
            Group anonymous = null;
            try {
                anonymous = groupService.findByName(context, Group.ANONYMOUS);
            } catch (SQLException e) {
                throw new RuntimeException("Cannot find anonymous group!", e);
            }
            discoverQuery.addFilterQueries("read:g" + anonymous.getID().toString());
        }

        if (entityType != null) {
            discoverQuery.addFilterQueries("search.entitytype:" + entityType);
        }

        return discoverQuery;
    }

    private List<QueryBuilderSearchFilter> parseSearchFilters() {

        List<QueryBuilderSearchFilter> queryBuilderSearchFilters = new ArrayList<>();

        if (searchFilters == null) {
            return queryBuilderSearchFilters;
        }

        String[] filters = searchFilters.split("&");
        for (String filter : filters) {
            String[] filterSections = filter.split("=");
            if (filterSections.length != 2) {
                handler.logError("Invalid filter: " + filter);
                throw new IllegalArgumentException("Invalid filter: " + filter);
            }

            String name = filterSections[0];
            String filterValue = substringBeforeLast(filterSections[1], FILTER_OPERATOR_SEPARATOR);
            String operator = substringAfterLast(filterSections[1], FILTER_OPERATOR_SEPARATOR);

            queryBuilderSearchFilters.add(new QueryBuilderSearchFilter(name, operator, filterValue));

        }

        return queryBuilderSearchFilters;

    }

    protected void assignCurrentUserInContext(Context context) throws SQLException, ParseException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    private String getFileName(StreamDisseminationCrosswalk streamDisseminationCrosswalk) {
        if (streamDisseminationCrosswalk instanceof FileNameDisseminator) {
            return ((FileNameDisseminator) streamDisseminationCrosswalk).getFileName();
        } else {
            return "export-result";
        }
    }

    private StreamDisseminationCrosswalk getCrosswalkByType(String type) {
        return new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class).getByType(type);
    }

    private int maxResults() throws SQLException {

        StringBuilder property = new StringBuilder("bulk-export.limit.");
        if (authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context)) {
            property.append("admin");
        } else {
            property.append(Optional.ofNullable(context.getCurrentUser()).map(ignored -> "loggedIn")
                                .orElse("notLoggedIn"));
        }
        int maxByUserCategory = configurationService.getIntProperty(property.toString(), -1);
        if (maxByUserCategory > 0 && this.limit != null && this.limit > 0) {
            return Optional.ofNullable(this.limit)
                .map(l -> Math.min(l, maxByUserCategory))
                .orElse(maxByUserCategory);
        } else if (maxByUserCategory == -1 && this.limit != null && this.limit > 0) {
            return this.limit;
        } else {
            return maxByUserCategory;
        }
    }
}
