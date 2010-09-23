/**
 * RoleDisseminator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2010, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.jdom.Namespace;

/**
 * Plugin to export all Group and EPerson objects in XML, perhaps for reloading.
 * 
 * @author Mark Wood
 */
public class RoleDisseminator implements PackageDisseminator
{

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
    public static final String EPERSONS = "EPersons";
    public static final String EPERSON = "EPerson";
    public static final String EMAIL = "Email";
    public static final String NETID = "Netid";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";
    public static final String LANGUAGE = "Language";
    public static final String PASSWORD_HASH = "PasswordHash";
    public static final String CAN_LOGIN = "CanLogin";
    public static final String REQUIRE_CERTIFICATE = "RequireCertificate";
    public static final String SELF_REGISTERED = "SelfRegistered";

    // Valid type values for Groups (only used when Group is associated with a Community or Collection)
    public static final String GROUP_TYPE_ADMIN = "ADMIN";
    public static final String GROUP_TYPE_SUBMIT = "SUBMIT";
    public static final String GROUP_TYPE_WORKFLOW_STEP_1 = "WORKFLOW_STEP_1";
    public static final String GROUP_TYPE_WORKFLOW_STEP_2 = "WORKFLOW_STEP_2";
    public static final String GROUP_TYPE_WORKFLOW_STEP_3 = "WORKFLOW_STEP_3";

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
               AuthorizeException, SQLException, IOException
    {
        boolean emitPasswords = params.containsKey("passwords");

        //open file stream for writing
        FileOutputStream fileOut = new FileOutputStream(pkgFile);
        writeToStream(context, object, fileOut, emitPasswords);

        //close file stream & save
        fileOut.close();
    }

    /**
     * Make serialized users and groups available on an InputStream, for code
     * which wants to read one.
     * 
     * @param emitPasswords true if password hashes should be included.
     * @return the stream of XML representing users and groups.
     * @throws IOException
     *             if a PipedOutputStream or PipedInputStream cannot be created.
     */
    InputStream asStream(Context context, DSpaceObject object, boolean emitPasswords)
            throws IOException
    {
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
    private class Serializer implements Runnable
    {
        private Context context;
        private DSpaceObject object;
        private OutputStream stream;
        private boolean emitPasswords;
        
        @SuppressWarnings("unused")
        private Serializer() {}

        /**
         * @param context
         * @param object the DSpaceObject
         * @param stream receives serialized user and group objects.  Will be
         *          closed when serialization is complete.
         * @param emitPasswords true if password hashes should be included.
         */
        Serializer(Context context, DSpaceObject object, OutputStream stream, boolean emitPasswords)
        {
            this.context = context;
            this.object = object;
            this.stream = stream;
            this.emitPasswords = emitPasswords;
        }

        public void run()
        {
            try
            {
                writeToStream(context, object, stream, emitPasswords);
                stream.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (PackageException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Serialize users and groups to a stream.
     * 
     * @param context
     * @param stream receives the output.  Is not closed by this method.
     * @param emitPasswords true if password hashes should be included.
     * @throws XMLStreamException
     * @throws SQLException
     */
    private void writeToStream(Context context, DSpaceObject object, OutputStream stream,
            boolean emitPasswords)
    throws PackageException
    {
        try
        {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer;

            writer = factory.createXMLStreamWriter(stream, "UTF-8");
            writer.setDefaultNamespace(DSROLES_NS.getURI());
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement(DSPACE_ROLES);

            writer.writeStartElement(GROUPS);
            Group[] groups = findAssociatedGroups(context, object);
            if(groups!=null)
            {
                for (Group group : groups)
                    writeGroup(context, object, group, writer);
            }
            writer.writeEndElement(); // GROUPS

            writer.writeStartElement(EPERSONS);
            EPerson[] people = findAssociatedPeople(context, object);
            if(people!=null)
            {
                for (EPerson eperson : people)
                    writeEPerson(eperson, writer, emitPasswords);
            }
            writer.writeEndElement(); // EPERSONS

            writer.writeEndElement(); // DSPACE_ROLES
            writer.writeEndDocument();
            writer.close();
        }
        catch (Exception e)
        {
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
               AuthorizeException, SQLException, IOException
    {
        throw new PackageException("disseminateAll() is not implemented, as disseminate() method already handles dissemination of all roles to an external file.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageDisseminator#getMIMEType(org.dspace
     * .content.packager.PackageParameters)
     */
    @Override
    public String getMIMEType(PackageParameters params)
    {
        return "application/xml";
    }

    /**
     * Emit XML describing a single Group.
     *
     * @param context
     *            the DSpace Context
     * @parm relatedObject
     *            the DSpaceObject related to this group (if any)
     * @param group
     *            the Group to describe
     * @param write
     *            the description to this stream
     */
    private void writeGroup(Context context, DSpaceObject relatedObject, Group group, XMLStreamWriter writer)
            throws XMLStreamException, PackageException
    {
        writer.writeStartElement(GROUP);
        writer.writeAttribute(ID, String.valueOf(group.getID()));
        writer.writeAttribute(NAME, PackageUtils.crosswalkDefaultGroupName(context, group.getName()));

        String groupType = getGroupType(relatedObject, group);
        if(groupType!=null && !groupType.isEmpty())
            writer.writeAttribute(TYPE, groupType);

        writer.writeStartElement(MEMBERS);
        for (EPerson member : group.getMembers())
        {
            writer.writeEmptyElement(MEMBER);
            writer.writeAttribute(ID, String.valueOf(member.getID()));
            writer.writeAttribute(NAME, member.getName());
        }
        writer.writeEndElement();

        writer.writeStartElement(MEMBER_GROUPS);
        for (Group member : group.getMemberGroups())
        {
            writer.writeEmptyElement(MEMBER_GROUP);
            writer.writeAttribute(ID, String.valueOf(member.getID()));
            writer.writeAttribute(NAME, PackageUtils.crosswalkDefaultGroupName(context, member.getName()));
        }
        writer.writeEndElement();

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
     * @param dso
     *          the related DSpaceObject
     * @param group
     *          the group
     * @return a group type string or null
     */
    private String getGroupType(DSpaceObject dso, Group group)
    {
        if(dso.getType()==Constants.COMMUNITY)
        {
            Community community = (Community) dso;

            //Check if this is the ADMIN group for this community
            if(community.getAdministrators()!=null &&
               community.getAdministrators().equals(group))
                return GROUP_TYPE_ADMIN;
        }
        else if(dso.getType() == Constants.COLLECTION)
        {
            Collection collection = (Collection) dso;

            //Check if this is the ADMIN group for this collection
            if(collection.getAdministrators()!=null &&
               collection.getAdministrators().equals(group))
                return GROUP_TYPE_ADMIN;
            //Check if Submitters group
            else if(collection.getSubmitters()!=null &&
                    collection.getSubmitters().equals(group))
                return GROUP_TYPE_SUBMIT;
            //Check if workflow step 1 group
            else if(collection.getWorkflowGroup(1)!=null &&
                    collection.getWorkflowGroup(1).equals(group))
                return GROUP_TYPE_WORKFLOW_STEP_1;
            //check if workflow step 2 group
            else if(collection.getWorkflowGroup(2)!=null &&
                    collection.getWorkflowGroup(2).equals(group))
                return GROUP_TYPE_WORKFLOW_STEP_2;
            //check if workflow step 3 group
            else if(collection.getWorkflowGroup(3)!=null &&
                    collection.getWorkflowGroup(3).equals(group))
                return GROUP_TYPE_WORKFLOW_STEP_3;
        }

        //by default, return null
        return null;
    }

    /**
     * Emit XML describing a single EPerson.
     * 
     * @param eperson
     *            the EPerson to describe
     * @param write
     *            the description to this stream
     * @param emitPassword
     *            do not export the password hash unless true
     */
    private void writeEPerson(EPerson eperson, XMLStreamWriter writer,
            boolean emitPassword) throws XMLStreamException
    {
        writer.writeStartElement(EPERSON);
        writer.writeAttribute(ID, String.valueOf(eperson.getID()));

        writer.writeStartElement(EMAIL);
        writer.writeCharacters(eperson.getEmail());
        writer.writeEndElement();

        writer.writeStartElement(NETID);
        writer.writeCharacters(eperson.getNetid());
        writer.writeEndElement();

        writer.writeStartElement(FIRST_NAME);
        writer.writeCharacters(eperson.getFirstName());
        writer.writeEndElement();

        writer.writeStartElement(LAST_NAME);
        writer.writeCharacters(eperson.getLastName());
        writer.writeEndElement();

        writer.writeStartElement(LANGUAGE);
        writer.writeCharacters(eperson.getLanguage());
        writer.writeEndElement();

        if (emitPassword)
        {
            writer.writeStartElement(PASSWORD_HASH);
            writer.writeCharacters(eperson.getPasswordHash());
            writer.writeEndElement();
        }

        if (eperson.canLogIn())
            writer.writeEmptyElement(CAN_LOGIN);

        if (eperson.getRequireCertificate())
            writer.writeEmptyElement(REQUIRE_CERTIFICATE);

        if (eperson.getSelfRegistered())
            writer.writeEmptyElement(SELF_REGISTERED);

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
     * @param object the DSpace object
     * @return array of all associated groups
     */
    private Group[] findAssociatedGroups(Context context, DSpaceObject object)
            throws SQLException
    {
        if(object.getType()==Constants.SITE)
        {
            // @TODO FIXME -- if there was a way to ONLY export Groups which are NOT
            // associated with a Community or Collection, we should be doing that instead!
            return Group.findAll(context, Group.NAME);
        }
        else if(object.getType()==Constants.COMMUNITY)
        {
            Community community = (Community) object;

            ArrayList<Group> list = new ArrayList<Group>();

            //check for admin group
            if(community.getAdministrators()!=null)
                list.add(community.getAdministrators());

            // FINAL CATCH-ALL -> Find any other groups where name begins with "COMMUNITY_<ID>_"
            // (There should be none, but this code is here just in case)
            Group[] matchingGroups = Group.search(context, "COMMUNITY_" + community.getID() + "_");
            for(Group g : matchingGroups)
            {
                if(!list.contains(g))
                    list.add(g);
            }

            if(list.size()>0)
            {
                Group[] groupArray = new Group[list.size()];
                groupArray = (Group[]) list.toArray(groupArray);
                return groupArray;
            }
        }
        else if(object.getType()==Constants.COLLECTION)
        {
            Collection collection = (Collection) object;
            
            ArrayList<Group> list = new ArrayList<Group>();
            
            //check for admin group
            if(collection.getAdministrators()!=null)
                list.add(collection.getAdministrators());
            //check for submitters group
            if(collection.getSubmitters()!=null)
                list.add(collection.getSubmitters());
            //check for workflow step 1 group
            if(collection.getWorkflowGroup(1)!=null)
                list.add(collection.getWorkflowGroup(1));
            //check for workflow step 2 group
            if(collection.getWorkflowGroup(2)!=null)
                list.add(collection.getWorkflowGroup(2));
            //check for workflow step 3 group
            if(collection.getWorkflowGroup(3)!=null)
                list.add(collection.getWorkflowGroup(3));

            // FINAL CATCH-ALL -> Find any other groups where name begins with "COLLECTION_<ID>_"
            // (Necessary cause XMLUI allows you to generate a 'COLLECTION_<ID>_DEFAULT_READ' group)
            Group[] matchingGroups = Group.search(context, "COLLECTION_" + collection.getID() + "_");
            for(Group g : matchingGroups)
            {
                if(!list.contains(g))
                    list.add(g);
            }

            if(list.size()>0)
            {
                Group[] groupArray = new Group[list.size()];
                groupArray = (Group[]) list.toArray(groupArray);
                return groupArray;
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
     * @param object the DSpace object
     * @return array of all associated EPerson objects
     */
    private EPerson[] findAssociatedPeople(Context context, DSpaceObject object)
            throws SQLException
    {
        if(object.getType()==Constants.SITE)
        {
            return EPerson.findAll(context, EPerson.EMAIL);
        }

        //by default, return nothing
        return null;
    }

}
