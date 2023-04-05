/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.PasswordHash;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.jdom2.Namespace;

/**
 * Plugin to export all Group and EPerson objects in XML, perhaps for reloading.
 *
 * @author Mark Wood
 */
public class RoleDisseminator implements PackageDisseminator {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RoleDisseminator.class);

    /**
     * DSpace Roles XML Namespace in JDOM form.
     */
    public static final Namespace DSROLES_NS =
        Namespace.getNamespace("dsroles", "http://www.dspace.org/xmlns/dspace/dspace-roles");

    public static final String DSPACE_ROLES = "DSpaceRoles";
    public static final String ID = "ID";
    public static final String GROUPS = "Groups";
    public static final String GROUP = "Group";
    public static final String NAME = "Name";
    public static final String TYPE = "Type";
    public static final String MEMBERS = "Members";
    public static final String MEMBER = "Member";
    public static final String MEMBER_GROUPS = "MemberGroups";
    public static final String MEMBER_GROUP = "MemberGroup";
    public static final String EPERSONS = "People";
    public static final String EPERSON = "Person";
    public static final String EMAIL = "Email";
    public static final String NETID = "Netid";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";
    public static final String LANGUAGE = "Language";
    public static final String PASSWORD_HASH = "PasswordHash";
    public static final String PASSWORD_DIGEST = "digest";
    public static final String PASSWORD_SALT = "salt";
    public static final String CAN_LOGIN = "CanLogin";
    public static final String REQUIRE_CERTIFICATE = "RequireCertificate";
    public static final String SELF_REGISTERED = "SelfRegistered";

    // Valid type values for Groups (only used when Group is associated with a Community or Collection)
    public static final String GROUP_TYPE_ADMIN = "ADMIN";
    public static final String GROUP_TYPE_SUBMIT = "SUBMIT";
    public static final String GROUP_TYPE_WORKFLOW_STEP_1 = "WORKFLOW_STEP_1";
    public static final String GROUP_TYPE_WORKFLOW_STEP_2 = "WORKFLOW_STEP_2";
    public static final String GROUP_TYPE_WORKFLOW_STEP_3 = "WORKFLOW_STEP_3";

    protected final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.content.packager.PackageDisseminator#disseminate(org.dspace
     * .core.Context, org.dspace.content.DSpaceObject,
     * org.dspace.content.packager.PackageParameters, java.io.File)
     */
    @Override
    public void disseminate(Context context, DSpaceObject object,
                            PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
        AuthorizeException, SQLException, IOException {
        boolean emitPasswords = params.containsKey("passwords");

        FileOutputStream fileOut = null;
        try {
            //open file stream for writing
            fileOut = new FileOutputStream(pkgFile);
            writeToStream(context, object, fileOut, emitPasswords);
        } finally {
            //close file stream & save
            if (fileOut != null) {
                fileOut.close();
            }
        }
    }

    /**
     * Make serialized users and groups available on an InputStream, for code
     * which wants to read one.
     *
     * @param emitPasswords true if password hashes should be included.
     * @return the stream of XML representing users and groups.
     * @throws IOException if IO error
     *                     if a PipedOutputStream or PipedInputStream cannot be created.
     */
    InputStream asStream(Context context, DSpaceObject object, boolean emitPasswords)
        throws IOException {
        // Create a PipedOutputStream to which to write some XML
        PipedOutputStream outStream = new PipedOutputStream();
        PipedInputStream inStream = new PipedInputStream(outStream);

        // Create a new Thread to push serialized objects into the pipe
        Serializer serializer = new Serializer(context, object, outStream,
                                               emitPasswords);
        new Thread(serializer).start();

        return inStream;
    }

    /**
     * Embody a thread for serializing users and groups.
     *
     * @author mwood
     */
    protected class Serializer implements Runnable {
        private Context context;
        private DSpaceObject object;
        private OutputStream stream;
        private boolean emitPasswords;

        @SuppressWarnings("unused")
        private Serializer() {
        }

        /**
         * @param context
         * @param object        the DSpaceObject
         * @param stream        receives serialized user and group objects.  Will be
         *                      closed when serialization is complete.
         * @param emitPasswords true if password hashes should be included.
         */
        Serializer(Context context, DSpaceObject object, OutputStream stream, boolean emitPasswords) {
            this.context = context;
            this.object = object;
            this.stream = stream;
            this.emitPasswords = emitPasswords;
        }

        @Override
        public void run() {
            try {
                writeToStream(context, object, stream, emitPasswords);
                stream.close();
            } catch (IOException e) {
                log.error(e);
            } catch (PackageException e) {
                log.error(e);
            }
        }
    }

    /**
     * Serialize users and groups to a stream.
     *
     * @param context       current Context
     * @param object        DSpaceObject
     * @param stream        receives the output.  Is not closed by this method.
     * @param emitPasswords true if password hashes should be included.
     * @throws PackageException if error
     */
    protected void writeToStream(Context context, DSpaceObject object, OutputStream stream,
                                 boolean emitPasswords)
        throws PackageException {
        try {
            //First, find all Groups/People associated with our current Object
            List<Group> groups = findAssociatedGroups(context, object);
            List<EPerson> people = findAssociatedPeople(context, object);

            //Only continue if we've found Groups or People which we need to disseminate
            if ((groups != null && groups.size() > 0) ||
                (people != null && people.size() > 0)) {
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter writer;

                writer = factory.createXMLStreamWriter(stream, "UTF-8");
                writer.setDefaultNamespace(DSROLES_NS.getURI());
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement(DSPACE_ROLES);

                //Only disseminate a <Groups> element if some groups exist
                if (groups != null) {
                    writer.writeStartElement(GROUPS);

                    for (Group group : groups) {
                        writeGroup(context, object, group, writer);
                    }

                    writer.writeEndElement(); // GROUPS
                }

                //Only disseminate an <People> element if some people exist
                if (people != null) {
                    writer.writeStartElement(EPERSONS);

                    for (EPerson eperson : people) {
                        writeEPerson(eperson, writer, emitPasswords);
                    }

                    writer.writeEndElement(); // EPERSONS
                }

                writer.writeEndElement(); // DSPACE_ROLES
                writer.writeEndDocument();
                writer.close();
            } //end if Groups or People exist
        } catch (Exception e) {
            throw new PackageException(e);
        }
    }

    /* (non-Javadoc)
     *
     * @see
     * org.dspace.content.packager.PackageDisseminator#disseminateAll(org.dspace
     * .core.Context, org.dspace.content.DSpaceObject,
     * org.dspace.content.packager.PackageParameters, java.io.File)
     */
    @Override
    public List<File> disseminateAll(Context context, DSpaceObject dso,
                                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
        AuthorizeException, SQLException, IOException {
        throw new PackageException(
            "disseminateAll() is not implemented, as disseminate() method already handles dissemination of all roles " +
                "to an external file.");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.content.packager.PackageDisseminator#getMIMEType(org.dspace
     * .content.packager.PackageParameters)
     */
    @Override
    public String getMIMEType(PackageParameters params) {
        return "application/xml";
    }

    /**
     * Emit XML describing a single Group.
     *
     * @param context       the DSpace Context
     * @param relatedObject the DSpaceObject related to this group (if any)
     * @param group         the Group to describe
     * @param writer        the description to this stream
     * @throws XMLStreamException if XML error
     * @throws PackageException   if packaging error
     */
    protected void writeGroup(Context context, DSpaceObject relatedObject, Group group, XMLStreamWriter writer)
        throws XMLStreamException, PackageException {
        //Translate the Group name for export.  This ensures that groups with Internal IDs in their names
        // (e.g. COLLECTION_1_ADMIN) are properly translated using the corresponding Handle or external identifier.
        String exportGroupName = PackageUtils.translateGroupNameForExport(context, group.getName());

        //If translated group name is returned as "null", this means the Group name
        // had an Internal Collection/Community ID embedded, which could not be
        // translated properly to a Handle.  We will NOT export these groups,
        // as they could cause conflicts or data integrity problems if they are
        // imported into another DSpace system.
        if (exportGroupName == null) {
            return;
        }

        writer.writeStartElement(GROUP);
        writer.writeAttribute(ID, String.valueOf(group.getID()));
        writer.writeAttribute(NAME, exportGroupName);

        String groupType = getGroupType(context, relatedObject, group);
        if (groupType != null && !groupType.isEmpty()) {
            writer.writeAttribute(TYPE, groupType);
        }

        //Add People to Group (if any belong to this group)
        if (group.getMembers().size() > 0) {
            writer.writeStartElement(MEMBERS);
            for (EPerson member : group.getMembers()) {
                writer.writeEmptyElement(MEMBER);
                writer.writeAttribute(ID, String.valueOf(member.getID()));
                if (null != member.getName()) {
                    writer.writeAttribute(NAME, member.getName());
                }
            }
            writer.writeEndElement();
        }

        //Add Groups as Member Groups (if any belong to this group)
        if (group.getMemberGroups().size() > 0) {
            writer.writeStartElement(MEMBER_GROUPS);
            for (Group member : group.getMemberGroups()) {
                String exportMemberName = PackageUtils.translateGroupNameForExport(context, member.getName());
                //Only export member group if its name can be properly translated for export.  As noted above,
                // we don't want groups that are *unable* to be accurately translated causing issues on import.
                if (exportMemberName != null) {
                    writer.writeEmptyElement(MEMBER_GROUP);
                    writer.writeAttribute(ID, String.valueOf(member.getID()));
                    writer.writeAttribute(NAME, exportMemberName);
                }
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    /**
     * Return a Group Type string (see RoleDisseminator.GROUP_TYPE_* constants)
     * which describes the type of group and its relation to the given object.
     * <P>
     * As a basic example, if the Group is a Collection Administration group,
     * the Group Type string returned should be "ADMIN"
     * <P>
     * If type string cannot be determined, null is returned.
     *
     * @param dso   the related DSpaceObject
     * @param group the group
     * @return a group type string or null
     */
    protected String getGroupType(Context context, DSpaceObject dso, Group group) {
        if (dso == null || group == null) {
            return null;
        }

        if (dso.getType() == Constants.COMMUNITY) {
            Community community = (Community) dso;

            //Check if this is the ADMIN group for this community
            if (group.equals(community.getAdministrators())) {
                return GROUP_TYPE_ADMIN;
            }
        } else if (dso.getType() == Constants.COLLECTION) {
            Collection collection = (Collection) dso;

            if (group.equals(collection.getAdministrators())) {
                //Check if this is the ADMIN group for this collection
                return GROUP_TYPE_ADMIN;
            } else if (group.equals(collection.getSubmitters())) {
                //Check if Submitters group
                return GROUP_TYPE_SUBMIT;
            } else if (group.equals(collection.getWorkflowStep1(context))) {
                //Check if workflow step 1 group
                return GROUP_TYPE_WORKFLOW_STEP_1;
            } else if (group.equals(collection.getWorkflowStep2(context))) {
                //check if workflow step 2 group
                return GROUP_TYPE_WORKFLOW_STEP_2;
            } else if (group.equals(collection.getWorkflowStep3(context))) {
                //check if workflow step 3 group
                return GROUP_TYPE_WORKFLOW_STEP_3;
            }
        }

        //by default, return null
        return null;
    }

    /**
     * Emit XML describing a single EPerson.
     *
     * @param eperson      the EPerson to describe
     * @param writer       the description to this stream
     * @param emitPassword do not export the password hash unless true
     * @throws XMLStreamException if XML error
     */
    protected void writeEPerson(EPerson eperson, XMLStreamWriter writer,
                                boolean emitPassword) throws XMLStreamException {
        writer.writeStartElement(EPERSON);
        writer.writeAttribute(ID, String.valueOf(eperson.getID()));

        if (eperson.getEmail() != null) {
            writer.writeStartElement(EMAIL);
            writer.writeCharacters(eperson.getEmail());
            writer.writeEndElement();
        }

        if (eperson.getNetid() != null) {
            writer.writeStartElement(NETID);
            writer.writeCharacters(eperson.getNetid());
            writer.writeEndElement();
        }

        if (eperson.getFirstName() != null) {
            writer.writeStartElement(FIRST_NAME);
            writer.writeCharacters(eperson.getFirstName());
            writer.writeEndElement();
        }

        if (eperson.getLastName() != null) {
            writer.writeStartElement(LAST_NAME);
            writer.writeCharacters(eperson.getLastName());
            writer.writeEndElement();
        }

        if (eperson.getLanguage() != null) {
            writer.writeStartElement(LANGUAGE);
            writer.writeCharacters(eperson.getLanguage());
            writer.writeEndElement();
        }

        if (emitPassword) {
            PasswordHash password = ePersonService.getPasswordHash(eperson);
            if (null != password) {
                writer.writeStartElement(PASSWORD_HASH);

                String algorithm = password.getAlgorithm();
                if (null != algorithm) {
                    writer.writeAttribute(PASSWORD_DIGEST, algorithm);
                }

                String salt = password.getSaltString();
                if (null != salt) {
                    writer.writeAttribute(PASSWORD_SALT, salt);
                }

                writer.writeCharacters(password.getHashString());
                writer.writeEndElement();
            }
        }

        if (eperson.canLogIn()) {
            writer.writeEmptyElement(CAN_LOGIN);
        }

        if (eperson.getRequireCertificate()) {
            writer.writeEmptyElement(REQUIRE_CERTIFICATE);
        }

        if (eperson.getSelfRegistered()) {
            writer.writeEmptyElement(SELF_REGISTERED);
        }

        writer.writeEndElement();
    }

    /**
     * Find all Groups associated with this DSpace Object.
     * <P>
     * If object is SITE, all groups are returned.
     * <P>
     * If object is COMMUNITY or COLLECTION, only groups associated with
     * those objects are returned (if any).
     * <P>
     * For all other objects, null is returned.
     *
     * @param context The DSpace context
     * @param object  the DSpace object
     * @return array of all associated groups
     * @throws SQLException if database error
     */
    protected List<Group> findAssociatedGroups(Context context, DSpaceObject object)
        throws SQLException {
        if (object.getType() == Constants.SITE) {
            // TODO FIXME -- if there was a way to ONLY export Groups which are NOT
            // associated with a Community or Collection, we should be doing that instead!
            return groupService.findAll(context, null);
        } else if (object.getType() == Constants.COMMUNITY) {
            Community community = (Community) object;

            ArrayList<Group> list = new ArrayList<Group>();

            //check for admin group
            if (community.getAdministrators() != null) {
                list.add(community.getAdministrators());
            }

            // FINAL CATCH-ALL -> Find any other groups where name begins with "COMMUNITY_<ID>_"
            // (There should be none, but this code is here just in case)
            List<Group> matchingGroups = groupService.search(context, "COMMUNITY\\_" + community.getID() + "\\_");
            for (Group g : matchingGroups) {
                if (!list.contains(g)) {
                    list.add(g);
                }
            }

            if (list.size() > 0) {
                return list;
            }
        } else if (object.getType() == Constants.COLLECTION) {
            Collection collection = (Collection) object;

            ArrayList<Group> list = new ArrayList<Group>();

            //check for admin group
            if (collection.getAdministrators() != null) {
                list.add(collection.getAdministrators());
            }
            //check for submitters group
            if (collection.getSubmitters() != null) {
                list.add(collection.getSubmitters());
            }
            //check for workflow step 1 group
            if (collection.getWorkflowStep1(context) != null) {
                list.add(collection.getWorkflowStep1(context));
            }
            //check for workflow step 2 group
            if (collection.getWorkflowStep2(context) != null) {
                list.add(collection.getWorkflowStep2(context));
            }
            //check for workflow step 3 group
            if (collection.getWorkflowStep3(context) != null) {
                list.add(collection.getWorkflowStep3(context));
            }

            // FINAL CATCH-ALL -> Find any other groups where name begins with "COLLECTION_<ID>_"
            // (Necessary because the old XMLUI allowed you to generate a 'COLLECTION_<ID>_DEFAULT_READ' group)
            List<Group> matchingGroups = groupService.search(context, "COLLECTION\\_" + collection.getID() + "\\_");
            for (Group g : matchingGroups) {
                if (!list.contains(g)) {
                    list.add(g);
                }
            }

            if (list.size() > 0) {
                return list;
            }
        }

        //by default, return nothing
        return null;
    }


    /**
     * Find all EPeople associated with this DSpace Object.
     * <P>
     * If object is SITE, all people are returned.
     * <P>
     * For all other objects, null is returned.
     *
     * @param context The DSpace context
     * @param object  the DSpace object
     * @return array of all associated EPerson objects
     * @throws SQLException if database error
     */
    protected List<EPerson> findAssociatedPeople(Context context, DSpaceObject object)
        throws SQLException {
        if (object.getType() == Constants.SITE) {
            return ePersonService.findAll(context, EPerson.EMAIL);
        }

        //by default, return nothing
        return null;
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
    public String getParameterHelp() {
        return "* passwords=[boolean]      " +
            "If true, user password hashes are also exported (so that they can be later restored).  If false, user " +
            "passwords are not exported. (Default is false)";
    }

}
