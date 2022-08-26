/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.dspace.content.Item.ANY;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.event.Event;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.dao.VersionDAO;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the EPerson object. This class is responsible for
 * all business logic calls for the EPerson object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EPersonServiceImpl extends DSpaceObjectServiceImpl<EPerson> implements EPersonService {

    /**
     * log4j logger
     */
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(EPersonServiceImpl.class);

    @Autowired(required = true)
    protected EPersonDAO ePersonDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired(required = true)
    CollectionRoleService collectionRoleService;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected SubscribeService subscribeService;
    @Autowired(required = true)
    protected VersionDAO versionDAO;
    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired
    protected OrcidTokenService orcidTokenService;

    protected EPersonServiceImpl() {
        super();
    }

    @Override
    public EPerson find(Context context, UUID id) throws SQLException {
        return ePersonDAO.findByID(context, EPerson.class, id);
    }

    @Override
    public EPerson findByIdOrLegacyId(Context context, String id) throws SQLException {
        if (StringUtils.isNumeric(id)) {
            return findByLegacyId(context, Integer.parseInt(id));
        } else {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public EPerson findByLegacyId(Context context, int legacyId) throws SQLException {
        return ePersonDAO.findByLegacyId(context, legacyId, EPerson.class);
    }

    @Override
    public EPerson findByEmail(Context context, String email) throws SQLException {
        if (email == null) {
            return null;
        }

        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        return ePersonDAO.findByEmail(context, email);
    }

    @Override
    public EPerson findByNetid(Context context, String netId) throws SQLException {
        if (netId == null) {
            return null;
        }

        return ePersonDAO.findByNetid(context, netId);
    }

    @Override
    public List<EPerson> search(Context context, String query) throws SQLException {
        if (StringUtils.isBlank(query)) {
            //If we don't have a query, just return everything.
            return findAll(context, EPerson.EMAIL);
        }
        return search(context, query, -1, -1);
    }

    @Override
    public List<EPerson> search(Context context, String query, int offset, int limit) throws SQLException {
        try {
            List<EPerson> ePerson = new ArrayList<>();
            EPerson person = find(context, UUID.fromString(query));
            if (person != null) {
                ePerson.add(person);
            }
            return ePerson;
        } catch (IllegalArgumentException e) {
            MetadataField firstNameField = metadataFieldService.findByElement(context, "eperson", "firstname", null);
            MetadataField lastNameField = metadataFieldService.findByElement(context, "eperson", "lastname", null);
            if (StringUtils.isBlank(query)) {
                query = null;
            }
            return ePersonDAO.search(context, query, Arrays.asList(firstNameField, lastNameField),
                    Arrays.asList(firstNameField, lastNameField), offset, limit);
        }
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
        MetadataField firstNameField = metadataFieldService.findByElement(context, "eperson", "firstname", null);
        MetadataField lastNameField = metadataFieldService.findByElement(context, "eperson", "lastname", null);
        if (StringUtils.isBlank(query)) {
            query = null;
        }
        return ePersonDAO.searchResultCount(context, query, Arrays.asList(firstNameField, lastNameField));
    }

    @Override
    public List<EPerson> findAll(Context context, int sortField) throws SQLException {
        return findAll(context, sortField, -1, -1);
    }

    @Override
    public List<EPerson> findAll(Context context, int sortField, int pageSize, int offset) throws SQLException {
        String sortColumn = null;
        MetadataField metadataFieldSort = null;
        switch (sortField) {
            case EPerson.ID:
                sortColumn = "eperson_id";
                break;

            case EPerson.EMAIL:
                sortColumn = "email";
                break;

            case EPerson.LANGUAGE:
                metadataFieldSort = metadataFieldService.findByElement(context, "eperson", "language", null);
                break;
            case EPerson.NETID:
                sortColumn = "netid";
                break;

            default:
                metadataFieldSort = metadataFieldService.findByElement(context, "eperson", "lastname", null);
        }
        return ePersonDAO.findAll(context, metadataFieldSort, sortColumn, pageSize, offset);
    }

    @Override
    public EPerson create(Context context) throws SQLException, AuthorizeException {
        // authorized?
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        // Create a table row
        EPerson e = ePersonDAO.create(context, new EPerson());

        log.info(LogHelper.getHeader(context, "create_eperson", "eperson_id="
                + e.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(),
                null, getIdentifiers(context, e)));

        return e;
    }

    @Override
    public void delete(Context context, EPerson ePerson) throws SQLException, AuthorizeException {
        try {
            delete(context, ePerson, true);
        } catch (AuthorizeException ex) {
            log.error("This AuthorizeException: " + ex + " occured while deleting Eperson with the ID: " +
                      ePerson.getID());
            throw new AuthorizeException(ex);
        } catch (IOException ex) {
            log.error("This IOException: " + ex + " occured while deleting Eperson with the ID: " + ePerson.getID());
            throw new AuthorizeException(ex);
        } catch (EPersonDeletionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Deletes an EPerson. The argument cascade defines whether all references
     * on an EPerson should be deleted as well (by either deleting the
     * referencing object - e.g. WorkspaceItem, ResourcePolicy - or by setting
     * the foreign key null - e.g. archived Items). If cascade is set to false
     * and the EPerson is referenced somewhere, this leads to an
     * AuthorizeException. EPersons may be referenced by Items, ResourcePolicies
     * and workflow tasks.
     *
     * @param context DSpace context
     * @param ePerson The EPerson to delete.
     * @param cascade Whether to delete references on the EPerson (cascade =
     * true) or to abort the deletion (cascade = false) if the EPerson is
     * referenced within DSpace.
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public void delete(Context context, EPerson ePerson, boolean cascade)
            throws SQLException, AuthorizeException, IOException, EPersonDeletionException {
        // authorized?
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }
        Set<Group> workFlowGroups = getAllWorkFlowGroups(context, ePerson);
        for (Group group: workFlowGroups) {
            List<EPerson> ePeople = groupService.allMembers(context, group);
            if (ePeople.size() == 1 && ePeople.contains(ePerson)) {
                throw new EmptyWorkflowGroupException(ePerson.getID(), group.getID());
            }
        }
        // check for presence of eperson in tables that
        // have constraints on eperson_id
        List<String> constraintList = getDeleteConstraints(context, ePerson);
        if (constraintList.size() > 0) {
            // Check if the constraints we found should be deleted
            if (cascade) {
                Iterator<String> constraintsIterator = constraintList.iterator();

                while (constraintsIterator.hasNext()) {
                    String tableName = constraintsIterator.next();
                    if (StringUtils.equals(tableName, "item") || StringUtils.equals(tableName, "workspaceitem")) {
                        Iterator<Item> itemIterator = itemService.findBySubmitter(context, ePerson, true);

                        VersionHistoryService versionHistoryService = VersionServiceFactory.getInstance()
                                                                      .getVersionHistoryService();
                        VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

                        while (itemIterator.hasNext()) {
                            Item item = itemIterator.next();

                            VersionHistory versionHistory = versionHistoryService.findByItem(context, item);
                            if (null != versionHistory) {
                                for (Version version : versioningService.getVersionsByHistory(context,
                                                                                              versionHistory)) {
                                    version.setePerson(null);
                                    versionDAO.save(context, version);
                                }
                            }
                            WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
                                                                        .getWorkspaceItemService();
                            WorkspaceItem wsi = workspaceItemService.findByItem(context, item);

                            if (null != wsi) {
                                workspaceItemService.deleteAll(context, wsi);
                            } else {
                                // we can do that as dc.provenance still contains
                                // information about who submitted and who
                                // archived an item.
                                item.setSubmitter(null);
                                itemService.update(context, item);
                            }
                        }
                    } else if (StringUtils.equals(tableName, "cwf_claimtask")) {
                         // Unclaim all XmlWorkflow tasks
                        ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory
                                                                .getInstance().getClaimedTaskService();
                        XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory
                                                                .getInstance().getXmlWorkflowService();
                        WorkflowRequirementsService workflowRequirementsService = XmlWorkflowServiceFactory
                                                                       .getInstance().getWorkflowRequirementsService();

                        List<ClaimedTask> claimedTasks = claimedTaskService.findByEperson(context, ePerson);

                        for (ClaimedTask task : claimedTasks) {
                            xmlWorkflowService.deleteClaimedTask(context, task.getWorkflowItem(), task);

                            try {
                                workflowRequirementsService.removeClaimedUser(context, task.getWorkflowItem(),
                                                                              ePerson, task.getStepID());
                            } catch (WorkflowConfigurationException ex) {
                                log.error("This WorkflowConfigurationException: " + ex +
                                          " occured while deleting Eperson with the ID: " + ePerson.getID());
                                throw new AuthorizeException(new EPersonDeletionException(Collections
                                                                                          .singletonList(tableName)));
                            }
                        }
                    } else if (StringUtils.equals(tableName, "resourcepolicy")) {
                        // we delete the EPerson, it won't need any rights anymore.
                        authorizeService.removeAllEPersonPolicies(context, ePerson);
                    } else if (StringUtils.equals(tableName, "cwf_pooltask")) {
                        PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
                        poolTaskService.deleteByEperson(context, ePerson);
                    } else if (StringUtils.equals(tableName, "cwf_workflowitemrole")) {
                        WorkflowItemRoleService workflowItemRoleService = XmlWorkflowServiceFactory.getInstance()
                                                                          .getWorkflowItemRoleService();
                        workflowItemRoleService.deleteByEPerson(context, ePerson);
                    } else {
                        log.warn("EPerson is referenced in table '" + tableName
                                + "'. Deletion of EPerson " + ePerson.getID() + " may fail "
                                + "if the database does not handle this "
                                + "reference.");
                    }
                }
            } else {
                throw new EPersonDeletionException(constraintList);
            }
        }
        context.addEvent(new Event(Event.DELETE, Constants.EPERSON, ePerson.getID(), ePerson.getEmail(),
                getIdentifiers(context, ePerson)));

        // XXX FIXME: This sidesteps the object model code so it won't
        // generate  REMOVE events on the affected Groups.
        // Remove any group memberships first
        // Remove any group memberships first
        Iterator<Group> groups = ePerson.getGroups().iterator();
        while (groups.hasNext()) {
            Group group = groups.next();
            groups.remove();
            group.getMembers().remove(ePerson);
        }

        orcidTokenService.deleteByEPerson(context, ePerson);

        // Remove any subscriptions
        subscribeService.deleteByEPerson(context, ePerson);

        // Remove ourself
        ePersonDAO.delete(context, ePerson);

        log.info(LogHelper.getHeader(context, "delete_eperson",
                "eperson_id=" + ePerson.getID()));
    }

    private Set<Group> getAllWorkFlowGroups(Context context, EPerson ePerson) throws SQLException {
        Set<Group> workFlowGroups = new HashSet<>();

        Set<Group> groups = groupService.allMemberGroupsSet(context, ePerson);
        for (Group group: groups) {
            List<CollectionRole> collectionRoles = collectionRoleService.findByGroup(context, group);
            if (!collectionRoles.isEmpty()) {
                workFlowGroups.add(group);
            }
        }
        return workFlowGroups;
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.EPERSON;
    }

    @Override
    public void setPassword(EPerson ePerson, String password) {
        PasswordHash hash = new PasswordHash(password);
        ePerson.setDigestAlgorithm(hash.getAlgorithm());
        ePerson.setSalt(Utils.toHex(hash.getSalt()));
        ePerson.setPassword(Utils.toHex(hash.getHash()));
    }

    @Override
    public void setPasswordHash(EPerson ePerson, PasswordHash password) {
        if (null == password) {
            ePerson.setDigestAlgorithm(null);
            ePerson.setSalt(null);
            ePerson.setPassword(null);
        } else {
            ePerson.setDigestAlgorithm(password.getAlgorithm());
            ePerson.setSalt(password.getSaltString());
            ePerson.setPassword(password.getHashString());
        }
    }

    @Override
    public PasswordHash getPasswordHash(EPerson ePerson) {
        PasswordHash hash = null;
        try {
            hash = new PasswordHash(ePerson.getDigestAlgorithm(),
                    ePerson.getSalt(),
                    ePerson.getPassword());
        } catch (DecoderException ex) {
            log.error("Problem decoding stored salt or hash:  " + ex.getMessage());
        }
        return hash;
    }

    @Override
    public boolean checkPassword(Context context, EPerson ePerson, String attempt) {
        PasswordHash myHash;
        try {
            myHash = new PasswordHash(
                    ePerson.getDigestAlgorithm(),
                    ePerson.getSalt(),
                    ePerson.getPassword());
        } catch (DecoderException ex) {
            log.error(ex.getMessage());
            return false;
        }
        boolean answer = myHash.matches(attempt);

        // If using the old unsalted hash, and this password is correct, update to a new hash
        if (answer && (null == ePerson.getDigestAlgorithm())) {
            log.info("Upgrading password hash for EPerson " + ePerson.getID());
            setPassword(ePerson, attempt);
            try {
                context.turnOffAuthorisationSystem();
                update(context, ePerson);
            } catch (SQLException | AuthorizeException ex) {
                log.error("Could not update password hash", ex);
            } finally {
                context.restoreAuthSystemState();
            }
        }

        return answer;
    }

    @Override
    public void update(Context context, EPerson ePerson) throws SQLException, AuthorizeException {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization()
                && ((context.getCurrentUser() == null) || (ePerson.getID() != context
                .getCurrentUser().getID()))) {
            authorizeService.authorizeAction(context, ePerson, Constants.WRITE);
        }

        super.update(context, ePerson);

        ePersonDAO.save(context, ePerson);

        log.info(LogHelper.getHeader(context, "update_eperson",
                "eperson_id=" + ePerson.getID()));

        if (ePerson.isModified()) {
            context.addEvent(new Event(Event.MODIFY, Constants.EPERSON,
                    ePerson.getID(), null, getIdentifiers(context, ePerson)));
            ePerson.clearModified();
        }
        if (ePerson.isMetadataModified()) {
            ePerson.clearDetails();
        }
    }

    @Override
    public List<String> getDeleteConstraints(Context context, EPerson ePerson) throws SQLException {
        List<String> tableList = new ArrayList<>();

        // check for eperson in item table
        Iterator<Item> itemsBySubmitter = itemService.findBySubmitter(context, ePerson, true);
        if (itemsBySubmitter.hasNext()) {
            tableList.add("item");
        }

        WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        List<WorkspaceItem> workspaceBySubmitter = workspaceItemService.findByEPerson(context, ePerson);
        if (workspaceBySubmitter.size() > 0) {
            tableList.add("workspaceitem");
        }

        ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        if (resourcePolicyService.find(context, ePerson).size() > 0) {
            tableList.add("resourcepolicy");
        }

        WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        List<String> workflowConstraints = workflowService.getEPersonDeleteConstraints(context, ePerson);
        tableList.addAll(workflowConstraints);

        // the list of tables can be used to construct an error message
        // explaining to the user why the eperson cannot be deleted.
        return tableList;
    }

    @Override
    public List<EPerson> findByGroups(Context c, Set<Group> groups) throws SQLException {
        //Make sure we at least have one group, if not don't even bother searching.
        if (CollectionUtils.isNotEmpty(groups)) {
            return ePersonDAO.findByGroups(c, groups);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<EPerson> findEPeopleWithSubscription(Context context) throws SQLException {
        return ePersonDAO.findAllSubscribers(context);
    }

    @Override
    public void updateLastModified(Context context, EPerson dso) throws SQLException {
        //Not used
    }

    @Override
    public String getMetadata(EPerson dso, String field) {
        String[] MDValue = getMDValueByLegacyField(field);
        return getMetadataFirstValue(dso, MDValue[0], MDValue[1], MDValue[2], Item.ANY);
    }

    @Override
    public List<EPerson> findUnsalted(Context context) throws SQLException {
        return ePersonDAO.findWithPasswordWithoutDigestAlgorithm(context);
    }

    @Override
    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException {
        return ePersonDAO.findNotActiveSince(context, date);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return ePersonDAO.countRows(context);
    }

    @Override
    public EPerson findByProfileItem(Context context, Item profile) throws SQLException {
        List<MetadataValue> owners = itemService.getMetadata(profile, "dspace", "object", "owner", ANY);
        if (CollectionUtils.isEmpty(owners)) {
            return null;
        }
        return find(context, UUIDUtils.fromString(owners.get(0).getAuthority()));
    }

    @Override
    public String getName(EPerson dso) {
        return dso.getName();
    }
}
