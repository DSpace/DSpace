/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import org.apache.commons.codec.DecoderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.PasswordHash;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Create EPersons and Groups from a file of external representations.
 * 
 * @author mwood
 */
public class RoleIngester implements PackageIngester
{
    private static final Logger log = LoggerFactory
            .getLogger(RoleIngester.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Common code to ingest roles from a Document.
     * 
     * @param context
     *          DSpace Context
     * @param parent
     *          the Parent DSpaceObject
     * @param document
     *          the XML Document
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     * @throws PackageException if packaging error
     */
    void ingestDocument(Context context, DSpaceObject parent,
            PackageParameters params, Document document)
            throws SQLException, AuthorizeException, PackageException
    {
        String myEmail = context.getCurrentUser().getEmail();
        String myNetid = context.getCurrentUser().getNetid();

        // Ingest users (EPersons) first so Groups can use them
        NodeList users = document
                .getElementsByTagName(RoleDisseminator.EPERSON);
        for (int i = 0; i < users.getLength(); i++)
        {
            Element user = (Element) users.item(i);
            // int userID = Integer.valueOf(user.getAttribute("ID")); // FIXME
            // no way to set ID!
            NodeList emails = user.getElementsByTagName(RoleDisseminator.EMAIL);
            NodeList netids = user.getElementsByTagName(RoleDisseminator.NETID);
            EPerson eperson;
            EPerson collider;
            String email = null;
            String netid = null;
            String identity;
            if (emails.getLength() > 0)
            {
                email = emails.item(0).getTextContent();
                if (email.equals(myEmail))
                {
                    continue; // Cannot operate on my own EPerson!
                }
                identity = email;
                collider = ePersonService.findByEmail(context, identity);
                // collider = EPerson.find(context, userID);
            }
            else if (netids.getLength() > 0)
            {
                netid = netids.item(0).getTextContent();
                if (netid.equals(myNetid))
                {
                    continue; // Cannot operate on my own EPerson!
                }
                identity = netid;
                collider = ePersonService.findByNetid(context, identity);
            }
            else
            {
                throw new PackageException("EPerson has neither email nor netid.");
            }

            if (null != collider)
                if (params.replaceModeEnabled()) // -r -f
                {
                    eperson = collider;
                }
                else if (params.keepExistingModeEnabled()) // -r -k
                {
                    log.warn("Existing EPerson {} was not restored from the package.", identity);
                    continue;
                }
                else
                {
                    throw new PackageException("EPerson " + identity + " already exists.");
                }
            else
            {
                eperson = ePersonService.create(context);
                log.info("Created EPerson {}.", identity);
            }

            eperson.setEmail(email);
            eperson.setNetid(netid);

            NodeList data;

            data = user.getElementsByTagName(RoleDisseminator.FIRST_NAME);
            if (data.getLength() > 0)
            {
                eperson.setFirstName(context, data.item(0).getTextContent());
            }
            else
            {
                eperson.setFirstName(context, null);
            }

            data = user.getElementsByTagName(RoleDisseminator.LAST_NAME);
            if (data.getLength() > 0)
            {
                eperson.setLastName(context, data.item(0).getTextContent());
            }
            else
            {
                eperson.setLastName(context, null);
            }

            data = user.getElementsByTagName(RoleDisseminator.LANGUAGE);
            if (data.getLength() > 0)
            {
                eperson.setLanguage(context, data.item(0).getTextContent());
            }
            else
            {
                eperson.setLanguage(context, null);
            }

            data = user.getElementsByTagName(RoleDisseminator.CAN_LOGIN);
            eperson.setCanLogIn(data.getLength() > 0);

            data = user.getElementsByTagName(RoleDisseminator.REQUIRE_CERTIFICATE);
            eperson.setRequireCertificate(data.getLength() > 0);

            data = user.getElementsByTagName(RoleDisseminator.SELF_REGISTERED);
            eperson.setSelfRegistered(data.getLength() > 0);

            data = user.getElementsByTagName(RoleDisseminator.PASSWORD_HASH);
            if (data.getLength() > 0)
            {
                Node element = data.item(0);
                NamedNodeMap attributes = element.getAttributes();

                Node algorithm = attributes.getNamedItem(RoleDisseminator.PASSWORD_DIGEST);
                String algorithmText;
                if (null != algorithm)
                    algorithmText = algorithm.getNodeValue();
                else
                    algorithmText = null;

                Node salt = attributes.getNamedItem(RoleDisseminator.PASSWORD_SALT);
                String saltText;
                if (null != salt)
                    saltText = salt.getNodeValue();
                else
                    saltText = null;

                PasswordHash password;
                try {
                    password = new PasswordHash(algorithmText, saltText, element.getTextContent());
                } catch (DecoderException ex) {
                    throw new PackageValidationException("Unable to decode hexadecimal password hash or salt", ex);
                }
                ePersonService.setPasswordHash(eperson, password);
            }
            else
            {
                ePersonService.setPasswordHash(eperson, null);
            }

            // Actually write Eperson info to DB
            // NOTE: this update() doesn't call a commit(). So, Eperson info
            // may still be rolled back if a subsequent error occurs
            ePersonService.update(context, eperson);
        }

        // Now ingest the Groups
        NodeList groups = document.getElementsByTagName(RoleDisseminator.GROUP);

        // Create the groups and add their EPerson members
        for (int groupx = 0; groupx < groups.getLength(); groupx++)
        {
            Element group = (Element) groups.item(groupx);
            String name = group.getAttribute(RoleDisseminator.NAME);
            log.debug("Processing group {}", name);

            try
            {
                //Translate Group name back to internal ID format (e.g. COLLECTION_<ID>_ADMIN)
                // TODO: is this necessary? can we leave it in format with Handle in place of <ID>?
                // For now, this is necessary, because we don't want to accidentally
                // create a new group COLLECTION_hdl:123/34_ADMIN, which is equivalent
                // to an existing COLLECTION_45_ADMIN group
                name = PackageUtils.translateGroupNameForImport(context, name);
            }
            catch(PackageException pe)
            {
                // If an error is thrown, then this Group corresponds to a
                // Community or Collection that doesn't currently exist in the
                // system.  So, log a warning & skip it for now.
                log.warn("Skipping group named '" + name + "' as it seems to correspond to a Community or Collection that does not exist in the system.  " +
                         "If you are performing an AIP restore, you can ignore this warning as the Community/Collection AIP will likely create this group once it is processed.");
                continue;
            }
            log.debug("Translated group name:  {}", name);

            Group groupObj = null; // The group to restore
            Group collider = groupService.findByName(context, name); // Existing group?
            if (null != collider)
            { // Group already exists, so empty it
                if (params.replaceModeEnabled()) // -r -f
                {
                    // Get a *copy* of our group list to avoid ConcurrentModificationException
                    // when we remove these groups from the parent Group obj
                    List<Group> groupRemovalList = new ArrayList<>(collider.getMemberGroups());
                    Iterator<Group> groupIterator = groupRemovalList.iterator();
                    while(groupIterator.hasNext())
                    {
                        Group member = groupIterator.next();
                        groupService.removeMember(context, collider, member);
                    }

                    // Get a *copy* of our eperson list to avoid ConcurrentModificationException
                    // when we remove these epersons from the parent Group obj
                    List<EPerson> epersonRemovalList = new ArrayList<>(collider.getMembers());
                    Iterator<EPerson> epersonIterator = epersonRemovalList.iterator();
                    while(epersonIterator.hasNext())
                    {
                        EPerson member = epersonIterator.next();
                        // Remove all group members *EXCEPT* we don't ever want
                        // to remove the current user from the list of Administrators
                        // (otherwise remainder of ingest will fail)
                        if(!(collider.equals(groupService.findByName(context, Group.ADMIN)) &&
                             member.equals(context.getCurrentUser())))
                        {
                            groupService.removeMember(context, collider, member);
                        }
                    }
                    log.info("Existing Group {} was cleared. Its members will be replaced.", name);
                    groupObj = collider;
                }
                else if (params.keepExistingModeEnabled()) // -r -k
                {
                    log.warn("Existing Group {} was not replaced from the package.",
                             name);
                    continue;
                }
                else
                {
                    throw new PackageException("Group " + name + " already exists");
                }
            }
            else
            { // No such group exists  -- so, we'll need to create it!

                DSpaceObjectService<DSpaceObject> dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(parent);
                log.debug("Creating group for a {}", dsoService.getTypeText(parent));
                // First Check if this is a "typed" group (i.e. Community or Collection associated Group)
                // If so, we'll create it via the Community or Collection
                String type = group.getAttribute(RoleDisseminator.TYPE);
                log.debug("Group type is {}", type);
                if(type!=null && !type.isEmpty() && parent!=null)
                {
                    //What type of dspace object is this group associated with
                    if(parent.getType()==Constants.COLLECTION)
                    {
                        Collection collection = (Collection) parent;

                        // Create this Collection-associated group, based on its group type
                        if(type.equals(RoleDisseminator.GROUP_TYPE_ADMIN))
                        {
                            groupObj = collectionService.createAdministrators(context, collection);
                        }
                        else if(type.equals(RoleDisseminator.GROUP_TYPE_SUBMIT))
                        {
                            groupObj = collectionService.createSubmitters(context, collection);
                        }
                        else if(type.equals(RoleDisseminator.GROUP_TYPE_WORKFLOW_STEP_1))
                        {
                            groupObj = collectionService.createWorkflowGroup(context, collection, 1);
                        }
                        else if(type.equals(RoleDisseminator.GROUP_TYPE_WORKFLOW_STEP_2))
                        {
                            groupObj = collectionService.createWorkflowGroup(context, collection, 2);
                        }
                        else if(type.equals(RoleDisseminator.GROUP_TYPE_WORKFLOW_STEP_3))
                        {
                            groupObj = collectionService.createWorkflowGroup(context, collection, 3);
                        }
                    }
                    else if(parent.getType()==Constants.COMMUNITY)
                    {
                        Community community = (Community) parent;

                        // Create this Community-associated group, based on its group type
                        if(type.equals(RoleDisseminator.GROUP_TYPE_ADMIN))
                        {
                            groupObj = communityService.createAdministrators(context, community);
                        }
                    }
                    //Ignore all other dspace object types
                }

                //If group not yet created, create it with the given name
                if(groupObj==null)
                {
                    groupObj = groupService.create(context);
                }

                // Always set the name:  parent.createBlop() is guessing
                groupService.setName(groupObj, name);

                log.info("Created Group {}.", groupObj.getName());
            }

            // Add EPeople to newly created Group
            NodeList members = group.getElementsByTagName(RoleDisseminator.MEMBER);
            for (int memberx = 0; memberx < members.getLength(); memberx++)
            {
                Element member = (Element) members.item(memberx);
                String memberName = member.getAttribute(RoleDisseminator.NAME);
                EPerson memberEPerson = ePersonService.findByEmail(context, memberName);
                if (null != memberEPerson)
                    groupService.addMember(context, groupObj, memberEPerson);
                else
                    throw new PackageValidationException("EPerson " + memberName
                            + " not found, not added to " + name);
            }

            // Actually write Group info to DB
            // NOTE: this update() doesn't call a commit(). So, Group info
            // may still be rolled back if a subsequent error occurs
            groupService.update(context, groupObj);

        }

        // Go back and add Group members, now that all groups exist
        for (int groupx = 0; groupx < groups.getLength(); groupx++)
        {
            Element group = (Element) groups.item(groupx);
            String name = group.getAttribute(RoleDisseminator.NAME);
            log.debug("Processing group {}", name);
            try
            {
                // Translate Group name back to internal ID format (e.g. COLLECTION_<ID>_ADMIN)
                name = PackageUtils.translateGroupNameForImport(context, name);
                log.debug("Translated group name:  {}", name);
            }
            catch(PackageException pe)
            {
                // If an error is thrown, then this Group corresponds to a
                // Community or Collection that doesn't currently exist in the
                // system.  So,skip it for now.
                // (NOTE: We already logged a warning about this group earlier as
                //  this is the second time we are looping through all groups)
                continue;
            }

            // Find previously created group
            Group groupObj = groupService.findByName(context, name);
            log.debug("Looked up the group and found {}", groupObj);
            NodeList members = group
                    .getElementsByTagName(RoleDisseminator.MEMBER_GROUP);
            for (int memberx = 0; memberx < members.getLength(); memberx++)
            {
                Element member = (Element) members.item(memberx);
                String memberName = member.getAttribute(RoleDisseminator.NAME);
                //Translate Group name back to internal ID format (e.g. COLLECTION_<ID>_ADMIN)
                memberName = PackageUtils.translateGroupNameForImport(context, memberName);
                // Find previously created group
                Group memberGroup = groupService.findByName(context, memberName);
                groupService.addMember(context, groupObj, memberGroup);
            }
            // Actually update Group info in DB
            // NOTE: Group info may still be rolled back if a subsequent error occurs
            groupService.update(context, groupObj);
        }
    }

    /**
     * Ingest roles from an InputStream.
     *
     * @param context DSpace Context
     * @param parent the Parent DSpaceObject
     * @param params package params
     * @throws PackageException if packaging error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void ingestStream(Context context, DSpaceObject parent,
            PackageParameters params, InputStream stream)
            throws PackageException, SQLException, AuthorizeException
    {
        Document document;

        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setCoalescing(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(stream);
        }
        catch (ParserConfigurationException e)
        {
            throw new PackageException(e);
        }
        catch (SAXException e)
        {
            throw new PackageException(e);
        }
        catch (IOException e)
        {
            throw new PackageException(e);
        }
        /*
         * TODO ? finally { close(stream); }
         */
        ingestDocument(context, parent, params, document);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageIngester#ingest(org.dspace.core.Context
     * , org.dspace.content.DSpaceObject, java.io.File,
     * org.dspace.content.packager.PackageParameters, java.lang.String)
     */
    @Override
    public DSpaceObject ingest(Context context, DSpaceObject parent,
            File pkgFile, PackageParameters params, String license)
            throws PackageException, CrosswalkException, AuthorizeException,
            SQLException, IOException
    {
        Document document;

        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setCoalescing(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(pkgFile);
        }
        catch (ParserConfigurationException e)
        {
            throw new PackageException(e);
        }
        catch (SAXException e)
        {
            throw new PackageException(e);
        }
        ingestDocument(context, parent, params, document);

        /* Does not create a DSpaceObject */
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageIngester#ingestAll(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject, java.io.File,
     * org.dspace.content.packager.PackageParameters, java.lang.String)
     */
    @Override
    public List<String> ingestAll(Context context, DSpaceObject parent,
            File pkgFile, PackageParameters params, String license)
            throws PackageException, UnsupportedOperationException,
            CrosswalkException, AuthorizeException, SQLException, IOException
    {
        throw new PackageException(
                "ingestAll() is not implemented, as ingest() method already handles ingestion of all roles from an external file.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageIngester#replace(org.dspace.core.Context
     * , org.dspace.content.DSpaceObject, java.io.File,
     * org.dspace.content.packager.PackageParameters)
     */
    @Override
    public DSpaceObject replace(Context context, DSpaceObject dso,
            File pkgFile, PackageParameters params) throws PackageException,
            UnsupportedOperationException, CrosswalkException,
            AuthorizeException, SQLException, IOException
    {
        //Just call ingest() -- this will perform a replacement as necessary
        return ingest(context, dso, pkgFile, params, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageIngester#replaceAll(org.dspace.core
     * .Context, org.dspace.content.DSpaceObject, java.io.File,
     * org.dspace.content.packager.PackageParameters)
     */
    @Override
    public List<String> replaceAll(Context context, DSpaceObject dso,
            File pkgFile, PackageParameters params) throws PackageException,
            UnsupportedOperationException, CrosswalkException,
            AuthorizeException, SQLException, IOException
    {
        throw new PackageException(
                "replaceAll() is not implemented, as replace() method already handles replacement of all roles from an external file.");
    }

    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    @Override
    public String getParameterHelp()
    {
        return "No additional options available.";
    }
}
