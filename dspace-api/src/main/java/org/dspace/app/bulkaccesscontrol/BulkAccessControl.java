/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.authorize.ResourcePolicy.TYPE_INHERITED;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkaccesscontrol.exception.BulkAccessControlException;
import org.dspace.app.bulkaccesscontrol.model.AccessCondition;
import org.dspace.app.bulkaccesscontrol.model.AccessConditionBitstream;
import org.dspace.app.bulkaccesscontrol.model.AccessControl;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.dspace.app.bulkaccesscontrol.service.BulkAccessConditionConfigurationService;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DSpaceRunnable} to perform a bulk access control via json file.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class BulkAccessControl extends DSpaceRunnable<BulkAccessControlScriptConfiguration<BulkAccessControl>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkAccessControl.class);

    private DSpaceObjectUtils dSpaceObjectUtils;

    private SearchService searchService;

    private IndexObjectFactoryFactory indexObjectServiceFactory;

    private CommunityService communityService;

    private CollectionService collectionService;

    private ConfigurationService configurationService;

    private ItemService itemService;

    private AuthorizeService authorizeService;

    private String filename;

    private String[] uuids;

    private String [] targetUuids;

    private Context context;

    private BundleService bundleService;

    private BitstreamService bitstreamService;

    private BulkAccessConditionConfigurationService bulkAccessConditionConfigurationService;

    private ResourcePolicyService resourcePolicyService;

    private Map<String, AccessConditionOption> itemAccessConditions;

    private Map<String, AccessConditionOption> uploadAccessConditions;

    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {

        this.searchService = SearchUtils.getSearchService();
        this.indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();
        this.communityService = ContentServiceFactory.getInstance().getCommunityService();
        this.collectionService = ContentServiceFactory.getInstance().getCollectionService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.bundleService = ContentServiceFactory.getInstance().getBundleService();
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        this.resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.bulkAccessConditionConfigurationService = new DSpace().getServiceManager().getServiceByName(
            "bulkAccessConditionConfigurationService", BulkAccessConditionConfigurationService.class);
        this.dSpaceObjectUtils = new DSpace().getServiceManager().getServiceByName(
            DSpaceObjectUtilsImpl.class.getName(), DSpaceObjectUtilsImpl.class);

        BulkAccessConditionConfiguration bulkAccessConditionConfiguration =
            bulkAccessConditionConfigurationService.getBulkAccessConditionConfiguration("default");

        itemAccessConditions = bulkAccessConditionConfiguration
            .getItemAccessConditionOptions()
            .stream()
            .collect(Collectors.toMap(AccessConditionOption::getName, Function.identity()));

        uploadAccessConditions = bulkAccessConditionConfiguration
            .getBitstreamAccessConditionOptions()
            .stream()
            .collect(Collectors.toMap(AccessConditionOption::getName, Function.identity()));

        filename = commandLine.getOptionValue('f');
        uuids = commandLine.getOptionValues('u');
    }

    @Override
    public void internalRun() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AccessControl accessControl;
        context = new Context(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        context.turnOffAuthorisationSystem();

        if (uuids == null) {
            handler.logError("A target uuid must be provided (run with -h flag for details)");
            throw new IllegalArgumentException("A target uuid must be provided");
        } else if (uuids.length <= 0) {
            handler.logError("A target uuid must be provided with at least on uuid");
            throw new IllegalArgumentException("A target uuid must be provided with at least on uuid");
        }

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        try {
            accessControl = mapper.readValue(inputStream, AccessControl.class);
        } catch (IOException e) {
            handler.logError("Error parsing json file");
            throw new IllegalArgumentException("Error parsing json file", e);
        }

        try {
            validate(accessControl);
            updateItemsAndBitstreamsPolices(accessControl);
            context.complete();
            context.restoreAuthSystemState();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    private void validate(AccessControl accessControl) {

        if (Objects.isNull(accessControl.getItem()) && Objects.isNull(accessControl.getBitstream())) {
            handler.logError("item or bitstream nodes must be provided");
        }

        if (Objects.nonNull(accessControl.getItem())) {
            if (StringUtils.isNotEmpty(accessControl.getItem().getMode()) &&
                !(accessControl.getItem().getMode().equals("add") ||
                accessControl.getItem().getMode().equals("replace"))) {
                handler.logError("wrong value for item mode<" + accessControl.getItem().getMode() + ">");
            }

            for (AccessCondition accessCondition : accessControl.getItem().getAccessConditions()) {
                validateAccessCondition(accessCondition);
            }
        }

        if (Objects.nonNull(accessControl.getBitstream())) {
            if (StringUtils.isNotEmpty(accessControl.getBitstream().getMode()) &&
                !(accessControl.getBitstream().getMode().equals("add") ||
                accessControl.getBitstream().getMode().equals("replace"))) {
                handler.logError("wrong value for bitstream mode<" + accessControl.getBitstream().getMode() + ">");
            }

            for (AccessCondition accessCondition : accessControl.getBitstream().getAccessConditions()) {
                validateAccessCondition(accessCondition);
            }
        }
    }

    private void validateAccessCondition(AccessCondition accessCondition) {

        if (!itemAccessConditions.containsKey(accessCondition.getName())) {
            handler.logError("wrong access condition <" + accessCondition.getName() + ">");
            throw new IllegalArgumentException("wrong access condition <" + accessCondition.getName() + ">");
        }

        try {
            itemAccessConditions.get(accessCondition.getName()).validateResourcePolicy(
                context, accessCondition.getName(), accessCondition.getStartDate(), accessCondition.getEndDate());
        } catch (Exception e) {
            handler.logError("invalid access condition");
            handler.handleException(e);
        }
    }

    public void updateItemsAndBitstreamsPolices(AccessControl accessControl)
        throws SQLException, SearchServiceException, AuthorizeException {

        int counter = 0;
        int start = 0;
        int limit = 20;

        String query = buildSolrQuery(uuids);

        Iterator<Item> itemIterator = findItems(query, start, limit);

        while (itemIterator.hasNext()) {

            Item item = itemIterator.next();

            if (Objects.nonNull(accessControl.getItem())) {
                updateItemPolicies(item, accessControl);
            }

            if (Objects.nonNull(accessControl.getBitstream())) {
                updateBitstreamsPolicies(item, accessControl);
            }

            context.commit();
            context.uncacheEntity(item);

            counter++;

            if (counter == limit) {
                start += limit;
                itemIterator = findItems(query, start, limit);
            }
        }
    }

    private String buildSolrQuery(String[] uuids) throws SQLException {
        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
        String [] query = new String[uuids.length];
        for (int i = 0 ; i < query.length ; i++) {
            DSpaceObject dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuids[i]));
            if (dso.getType() == Constants.COMMUNITY) {
                query[i] = "location.comm:" + dso.getID();
            } else if (dso.getType() == Constants.COLLECTION) {
                query[i] = "location.coll:" + dso.getID();
            } else if (dso.getType() == Constants.ITEM) {
                query[i] = "search.resourceid:" + dso.getID();
            }
        }
        return StringUtils.joinWith(" OR ", query);
    }

    private Iterator<Item> findItems(String query, int start, int limit)
        throws SearchServiceException {

        DiscoverQuery discoverQuery = buildDiscoveryQuery(query, start, limit);

        return searchService.search(context, discoverQuery)
                            .getIndexableObjects()
                            .stream()
                            .map(indexableObject ->
                                ((IndexableItem) indexableObject).getIndexedObject())
                            .collect(Collectors.toList())
                            .iterator();
    }

    private DiscoverQuery buildDiscoveryQuery(String query, int start, int limit) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(query);
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);

        return discoverQuery;
    }

    private void updateItemPolicies(Item item, AccessControl accessControl) throws SQLException, AuthorizeException {

        if ("replace".equals(accessControl.getItem().getMode())) {
            removeReadPolicies(item, TYPE_CUSTOM);
            removeReadPolicies(item, TYPE_INHERITED);
        }

        setItemPolicies(item, accessControl);
    }

    private void setItemPolicies(Item item, AccessControl accessControl) throws SQLException, AuthorizeException {
        if (isAppendModeDisabled() && item.isArchived()) {
            // change to add
            itemService.adjustItemPolicies(context, item, item.getOwningCollection());
        }

        accessControl
            .getItem()
            .getAccessConditions()
            .forEach(accessCondition -> createResourcePolicy(item, accessCondition,
                itemAccessConditions.get(accessCondition.getName())));
    }

    private void updateBitstreamsPolicies(Item item, AccessControl accessControl) {

        if (containsConstraints(accessControl)) {
            findMatchedBitstreams(item, accessControl.getBitstream().getConstraint().getUuids())
                .forEach(bitstream ->
                    updateBitstreamPolicies(bitstream, item, accessControl));
        } else {
            item.getBundles()
                .stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .forEach(bitstream ->
                    updateBitstreamPolicies(bitstream, item, accessControl));
        }
    }

    private boolean containsConstraints(AccessControl accessControl) {
        AccessConditionBitstream controlBitstream = accessControl.getBitstream();

        return Objects.nonNull(controlBitstream) &&
            Objects.nonNull(controlBitstream.getConstraint()) &&
            isNotEmpty(controlBitstream.getConstraint().getUuids());
    }

    private List<Bitstream> findMatchedBitstreams(Item item, List<String> uuids) {
        return item.getBundles().stream()
                   .flatMap(bundle -> bundle.getBitstreams().stream())
                   .filter(bitstream -> uuids.contains(bitstream.getID().toString()))
                   .collect(Collectors.toList());
    }

    private void updateBitstreamPolicies(Bitstream bitstream, Item item, AccessControl accessControl) {

        if ("replace".equals(accessControl.getBitstream().getMode())) {
            removeReadPolicies(bitstream, TYPE_CUSTOM);
            removeReadPolicies(bitstream, TYPE_INHERITED);
        }

        try {
            setBitstreamPolicies(bitstream, item, accessControl);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

    }

    private void removeReadPolicies(DSpaceObject dso, String type) {
        try {
            resourcePolicyService.removePolicies(context, dso, type, Constants.READ);
        } catch (SQLException | AuthorizeException e) {
            throw new BulkAccessControlException(e);
        }
    }

    private void setBitstreamPolicies(Bitstream bitstream, Item item, AccessControl accessControl)
        throws SQLException, AuthorizeException {
        if (isAppendModeDisabled() && item.isArchived()) {
            itemService.adjustBitstreamPolicies(context, item, item.getOwningCollection(), bitstream);
        }

        accessControl.getBitstream()
                     .getAccessConditions()
                     .forEach(accessCondition -> createResourcePolicy(bitstream, accessCondition,
                         uploadAccessConditions.get(accessCondition.getName())));
    }

    private void createResourcePolicy(DSpaceObject obj, AccessCondition accessCondition,
                                      AccessConditionOption AccessConditionOption) {

        String name = accessCondition.getName();
        String description = accessCondition.getDescription();
        Date startDate = accessCondition.getStartDate();
        Date endDate = accessCondition.getEndDate();

        try {
            AccessConditionOption.createResourcePolicy(context, obj, name, description, startDate, endDate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    private void rollback() {
//        try {
//            context.rollback();
//        } catch (SQLException e) {
//            throw new SQLRuntimeException(e);
//        }
//    }

    private void assignCurrentUserInContext() throws SQLException {
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

    private Date parseDate(String date) {
        return MultiFormatDateParser.parse(date);
    }

    private boolean isAppendModeDisabled() {
        return !configurationService.getBooleanProperty(
            "core.authorization.installitem.inheritance-read.append-mode");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkAccessControlScriptConfiguration<BulkAccessControl> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("bulk-access-control",BulkAccessControlScriptConfiguration.class);
    }

}
