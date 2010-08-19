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
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Plugin to export all Group and EPerson objects in XML, perhaps for reloading.
 * 
 * @author Mark Wood
 */
public class RoleDisseminator implements PackageDisseminator
{
    public static final String DSPACE_ROLES = "DSpaceRoles";
    public static final String ID = "ID";
    public static final String GROUPS = "Groups";
    public static final String GROUP = "Group";
    public static final String NAME = "Name";
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
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageDisseminator#disseminate(org.dspace
     * .core.Context, org.dspace.content.DSpaceObject,
     * org.dspace.content.packager.PackageParameters, java.io.File)
     */
    public void disseminate(Context context, DSpaceObject object,
                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        boolean emitPasswords = params.containsKey("passwords");

        try
        {
            //open file stream for writing
            FileOutputStream fileOut = new FileOutputStream(pkgFile);
            writeToStream(context, fileOut, emitPasswords);

            //close file stream & save
            fileOut.close();
        }
        catch (XMLStreamException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Make serialized users and groups available on an InputStream, for code
     * which wants to read one.
     * 
     * @return the stream of XML representing users and groups.
     * @throws IOException
     *             if a PipedOutputStream or PipedInputStream cannot be created.
     */
    InputStream asStream(Context context, boolean emitPasswords)
            throws IOException
    {
        // Create a PipedOutputStream to which to write some XML
        PipedOutputStream outStream = new PipedOutputStream();
        PipedInputStream inStream = new PipedInputStream(outStream);

        // Create a new Thread to push serialized objects into the pipe
        Serializer serializer = new Serializer(context, outStream,
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
        private OutputStream stream;
        private boolean emitPasswords;
        
        @SuppressWarnings("unused")
        private Serializer() {}

        /**
         * @param context
         * @param stream receives serialized user and group objects.  Will be
         *          closed when serialization is complete.
         * @param emitPasswords true if password hashes should be included.
         */
        Serializer(Context context, OutputStream stream, boolean emitPasswords)
        {
            this.context = context;
            this.stream = stream;
            this.emitPasswords = emitPasswords;
        }

        public void run()
        {
            try
            {
                writeToStream(context, stream, emitPasswords);
                stream.close();
            }
            catch (XMLStreamException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
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
    private void writeToStream(Context context, OutputStream stream,
            boolean emitPasswords)
    throws XMLStreamException, SQLException
    {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer;

        writer = factory.createXMLStreamWriter(stream, "UTF-8");
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement(DSPACE_ROLES);

        writer.writeStartElement(GROUPS);
        for (Group group : Group.findAll(context, Group.NAME))
            writeGroup(group, writer);
        writer.writeEndElement(); // GROUPS

        writer.writeStartElement(EPERSONS);
        for (EPerson eperson : EPerson.findAll(context, EPerson.EMAIL))
            writeEPerson(eperson, writer, emitPasswords);
        writer.writeEndElement(); // EPERSONS

        writer.writeEndElement(); // DSPACE_ROLES
        writer.writeEndDocument();
        writer.close();
    }

    /* (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageDisseminator#disseminateAll(org.dspace
     * .core.Context, org.dspace.content.DSpaceObject,
     * org.dspace.content.packager.PackageParameters, java.io.File)
     */
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
    public String getMIMEType(PackageParameters params)
    {
        return "application/xml";
    }

    /**
     * Emit XML describing a single Group.
     * 
     * @param group
     *            the Group to describe
     * @param write
     *            the description to this stream
     */
    private void writeGroup(Group group, XMLStreamWriter writer)
            throws XMLStreamException
    {
        writer.writeStartElement(GROUP);
        writer.writeAttribute(ID, String.valueOf(group.getID()));
        writer.writeAttribute(NAME, group.getName());

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
            writer.writeAttribute(NAME, member.getName());
        }
        writer.writeEndElement();

        writer.writeEndElement();
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
}
