/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.event.Event;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation for the EPerson object.
 * This class is responsible for all business logic calls for the EPerson object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EPersonServiceImpl extends DSpaceObjectServiceImpl<EPerson> implements EPersonService {

    /** log4j logger */
    private final Logger log = Logger.getLogger(EPersonServiceImpl.class);

    @Autowired(required = true)
    protected EPersonDAO ePersonDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected SubscribeService subscribeService;

    protected EPersonServiceImpl()
    {
        super();
    }

    @Override
    public EPerson find(Context context, UUID id) throws SQLException {
        return ePersonDAO.findByID(context, EPerson.class, id);
    }

    @Override
    public EPerson findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public EPerson findByLegacyId(Context context, int legacyId) throws SQLException {
        return ePersonDAO.findByLegacyId(context, legacyId, EPerson.class);
    }

    @Override
    public EPerson findByEmail(Context context, String email) throws SQLException {
        if (email == null)
        {
            return null;
        }

        // All email addresses are stored as lowercase, so ensure that the email address is lowercased for the lookup
        return ePersonDAO.findByEmail(context, email);
    }

    @Override
    public EPerson findByNetid(Context context, String netId) throws SQLException {
        if (netId == null)
        {
            return null;
        }

        return ePersonDAO.findByNetid(context, netId);
    }

    @Override
    public List<EPerson> search(Context context, String query) throws SQLException {
        if(StringUtils.isBlank(query))
        {
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
            if(person != null)
            {
                ePerson.add(person);
            }
            return ePerson;
        } catch(IllegalArgumentException e) {
            MetadataField firstNameField = metadataFieldService.findByElement(context, "eperson", "firstname", null);
            MetadataField lastNameField = metadataFieldService.findByElement(context, "eperson", "lastname", null);
            if (StringUtils.isBlank(query))
            {
                query = null;
            }
            return ePersonDAO.search(context, query, Arrays.asList(firstNameField, lastNameField), Arrays.asList(firstNameField, lastNameField), offset, limit);
        }
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
        MetadataField firstNameField = metadataFieldService.findByElement(context, "eperson", "firstname", null);
        MetadataField lastNameField = metadataFieldService.findByElement(context, "eperson", "lastname", null);
        if(StringUtils.isBlank(query)) query = null;
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
        switch (sortField)
        {
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
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        // Create a table row
        EPerson e = ePersonDAO.create(context, new EPerson());

        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                + e.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(),
                null, getIdentifiers(context, e)));

        return e;
    }

    @Override
    public void delete(Context context, EPerson ePerson) throws SQLException, AuthorizeException {
        // authorized?
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        // check for presence of eperson in tables that
        // have constraints on eperson_id
        List<String> constraintList = getDeleteConstraints(context, ePerson);

        // if eperson exists in tables that have constraints
        // on eperson, throw an exception
        if (constraintList.size() > 0)
        {
            throw new AuthorizeException(new EPersonDeletionException(constraintList));
        }

        context.addEvent(new Event(Event.DELETE, Constants.EPERSON, ePerson.getID(), ePerson.getEmail(), getIdentifiers(context, ePerson)));

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

        // Remove any subscriptions
        subscribeService.deleteByEPerson(context, ePerson);

        // Remove ourself
        ePersonDAO.delete(context, ePerson);

        log.info(LogManager.getHeader(context, "delete_eperson",
                "eperson_id=" + ePerson.getID()));
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
        if (null == password)
        {
            ePerson.setDigestAlgorithm(null);
            ePerson.setSalt(null);
            ePerson.setPassword(null);
        }
        else
        {
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
        try
        {
            myHash = new PasswordHash(
                    ePerson.getDigestAlgorithm(),
                    ePerson.getSalt(),
                    ePerson.getPassword());
        } catch (DecoderException ex)
        {
            log.error(ex.getMessage());
            return false;
        }
        boolean answer = myHash.matches(attempt);

        // If using the old unsalted hash, and this password is correct, update to a new hash
        if (answer && (null == ePerson.getDigestAlgorithm()))
        {
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
                .getCurrentUser().getID())))
        {
            authorizeService.authorizeAction(context, ePerson, Constants.WRITE);
        }

        super.update(context, ePerson);

        ePersonDAO.save(context, ePerson);

        log.info(LogManager.getHeader(context, "update_eperson",
                "eperson_id=" + ePerson.getID()));

        if (ePerson.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.EPERSON,
                    ePerson.getID(), null, getIdentifiers(context, ePerson)));
            ePerson.clearModified();
        }
        if (ePerson.isMetadataModified())
        {
            ePerson.clearDetails();
        }
    }

    @Override
    public List<String> getDeleteConstraints(Context context, EPerson ePerson) throws SQLException
    {
        List<String> tableList = new ArrayList<String>();

        // check for eperson in item table
        Iterator<Item> itemsBySubmitter = itemService.findBySubmitter(context, ePerson);
        if (itemsBySubmitter.hasNext())
        {
            tableList.add("item");
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
        if(CollectionUtils.isNotEmpty(groups))
        {
            return ePersonDAO.findByGroups(c, groups);
        }else{
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
    public void setMetadata(Context context, EPerson ePerson, String field, String value) throws SQLException {
        String[] MDValue = getMDValueByLegacyField(field);
        setMetadataSingleValue(context, ePerson, MDValue[0], MDValue[1], MDValue[2], null, value);
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
}
