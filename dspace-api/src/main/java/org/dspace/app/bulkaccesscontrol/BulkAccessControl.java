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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.bulkaccesscontrol.exception.BulkAccessControlException;
import org.dspace.app.bulkaccesscontrol.model.AccessCondition;
import org.dspace.app.bulkaccesscontrol.model.AccessConditionBitstream;
import org.dspace.app.bulkaccesscontrol.model.AccessConditionItem;
import org.dspace.app.bulkaccesscontrol.model.AccessControl;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.dspace.app.bulkaccesscontrol.service.BulkAccessConditionConfigurationService;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.model.AccessConditionOption;
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

    private ConfigurationService configurationService;

    private ItemService itemService;

    private String filename;

    private List<String> uuids;

    private Context context;

    private BulkAccessConditionConfigurationService bulkAccessConditionConfigurationService;

    private ResourcePolicyService resourcePolicyService;

    private Map<String, AccessConditionOption> itemAccessConditions;

    private Map<String, AccessConditionOption> uploadAccessConditions;

    private final String ADD_MODE = "add";

    private final String REPLACE_MODE = "replace";

    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {

        this.searchService = SearchUtils.getSearchService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
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
        uuids = commandLine.hasOption('u') ? Arrays.asList(commandLine.getOptionValues('u')) : null;
    }

    @Override
    public void internalRun() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        AccessControl accessControl;
        context = new Context(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        context.turnOffAuthorisationSystem();

        if (uuids == null || uuids.size() == 0) {
            handler.logError("A target uuid must be provided with at least on uuid (run with -h flag for details)");
            throw new IllegalArgumentException("At least one target uuid must be provided");
        }

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        try {
            accessControl = mapper.readValue(inputStream, AccessControl.class);
        } catch (IOException e) {
            handler.logError("Error parsing json file " + e.getMessage());
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

    private void validate(AccessControl accessControl) throws SQLException {

        AccessConditionItem item = accessControl.getItem();
        AccessConditionBitstream bitstream = accessControl.getBitstream();

        if (Objects.isNull(item) && Objects.isNull(bitstream)) {
            handler.logError("item or bitstream node must be provided");
            throw new BulkAccessControlException("item or bitstream node must be provided");
        }

        if (Objects.nonNull(item)) {
            validateItemNode(item);
        }

        if (Objects.nonNull(bitstream)) {
            validateBitstreamNode(bitstream);
        }
    }

    private void validateItemNode(AccessConditionItem item) {
        String mode = item.getMode();
        List<AccessCondition> accessConditions = item.getAccessConditions();

        if (StringUtils.isEmpty(mode)) {
            handler.logError("item mode node must be provided");
            throw new BulkAccessControlException("item mode node must be provided");
        } else if (!(StringUtils.equalsAny(mode, ADD_MODE, REPLACE_MODE))) {
            handler.logError("wrong value for item mode<" + mode + ">");
            throw new BulkAccessControlException("wrong value for item mode<" + mode + ">");
        } else if (ADD_MODE.equals(mode) && CollectionUtils.isEmpty(accessConditions)) {
            handler.logError("accessConditions of item must be provided with mode<" + ADD_MODE + ">");
            throw new BulkAccessControlException(
                "accessConditions of item must be provided with mode<" + ADD_MODE + ">");
        }

        for (AccessCondition accessCondition : accessConditions) {
            validateAccessCondition(accessCondition);
        }
    }

    private void validateBitstreamNode(AccessConditionBitstream bitstream) throws SQLException {
        String mode = bitstream.getMode();
        List<AccessCondition> accessConditions = bitstream.getAccessConditions();

        if (StringUtils.isEmpty(mode)) {
            handler.logError("bitstream mode node must be provided");
            throw new BulkAccessControlException("bitstream mode node must be provided");
        } else if (!(StringUtils.equalsAny(mode, ADD_MODE, REPLACE_MODE))) {
            handler.logError("wrong value for bitstream mode<" + mode + ">");
            throw new BulkAccessControlException("wrong value for bitstream mode<" + mode + ">");
        } else if (ADD_MODE.equals(mode) && CollectionUtils.isEmpty(accessConditions)) {
            handler.logError("accessConditions of bitstream must be provided with mode<" + ADD_MODE + ">");
            throw new BulkAccessControlException(
                "accessConditions of bitstream must be provided with mode<" + ADD_MODE + ">");
        }

        validateConstraint(bitstream);

        for (AccessCondition accessCondition : bitstream.getAccessConditions()) {
            validateAccessCondition(accessCondition);
        }
    }

    private void validateConstraint(AccessConditionBitstream bitstream) throws SQLException {
        if (uuids.size() > 1  && containsConstraints(bitstream)) {
            handler.logError("constraint isn't supported when multiple uuids are provided");
            throw new BulkAccessControlException("constraint isn't supported when multiple uuids are provided");
        } else if (uuids.size() == 1 && containsConstraints(bitstream)) {
            DSpaceObject dso =
                dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuids.get(0)));

            if (Objects.nonNull(dso) && dso.getType() != Constants.ITEM) {
                handler.logError("constraint is not supported when uuid isn't an Item");
                throw new BulkAccessControlException("constraint is not supported when uuid isn't an Item");
            }
        }
    }

    private void validateAccessCondition(AccessCondition accessCondition) {

        if (!itemAccessConditions.containsKey(accessCondition.getName())) {
            handler.logError("wrong access condition <" + accessCondition.getName() + ">");
            throw new BulkAccessControlException("wrong access condition <" + accessCondition.getName() + ">");
        }

        try {
            itemAccessConditions.get(accessCondition.getName()).validateResourcePolicy(
                context, accessCondition.getName(), accessCondition.getStartDate(), accessCondition.getEndDate());
        } catch (Exception e) {
            handler.logError("invalid access condition, " + e.getMessage());
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

            Item item = context.reloadEntity(itemIterator.next());

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
                counter = 0;
                start += limit;
                itemIterator = findItems(query, start, limit);
            }
        }
    }

    private String buildSolrQuery(List<String> uuids) throws SQLException {
        String [] query = new String[uuids.size()];

        for (int i = 0 ; i < query.length ; i++) {
            DSpaceObject dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuids.get(i)));

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

        if (REPLACE_MODE.equals(accessControl.getItem().getMode())) {
            removeReadPolicies(item, TYPE_CUSTOM);
            removeReadPolicies(item, TYPE_INHERITED);
        }

        setItemPolicies(item, accessControl);
    }

    private void setItemPolicies(Item item, AccessControl accessControl) throws SQLException, AuthorizeException {
        AccessConditionItem itemControl = accessControl.getItem();
        accessControl
            .getItem()
            .getAccessConditions()
            .forEach(accessCondition -> createResourcePolicy(item, accessCondition,
                itemAccessConditions.get(accessCondition.getName())));

        itemService.adjustItemPolicies(context, item, item.getOwningCollection());
    }

    private void updateBitstreamsPolicies(Item item, AccessControl accessControl) {

        if (containsConstraints(accessControl.getBitstream())) {
            findMatchedBitstreams(item, accessControl.getBitstream().getConstraints().getUuid())
        item.getBundles(Constants.CONTENT_BUNDLE_NAME).stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .filter(bitstream -> uuids == null ||
                    uuids.size() == 0 ||
                    uuids.contains(bitstream.getID().toString()))
                .forEach(bitstream -> updateBitstreamPolicies(bitstream, item, accessControl));
    }

    private boolean containsConstraints(AccessConditionBitstream bitstream) {
        return Objects.nonNull(bitstream) &&
            Objects.nonNull(bitstream.getConstraints()) &&
            isNotEmpty(bitstream.getConstraints().getUuid());
    }

    private void updateBitstreamPolicies(Bitstream bitstream, Item item, AccessControl accessControl) {

        if (REPLACE_MODE.equals(accessControl.getBitstream().getMode())) {
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
        AccessConditionBitstream bitstreamControl = accessControl.getBitstream();
        accessControl.getBitstream()
                     .getAccessConditions()
                     .forEach(accessCondition -> createResourcePolicy(bitstream, accessCondition,
                         uploadAccessConditions.get(accessCondition.getName())));
        itemService.adjustBitstreamPolicies(context, item, item.getOwningCollection(), bitstream);
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
            throw new BulkAccessControlException(e);
        }
    }

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

    @Override
    @SuppressWarnings("unchecked")
    public BulkAccessControlScriptConfiguration<BulkAccessControl> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("bulk-access-control",BulkAccessControlScriptConfiguration.class);
    }

}
