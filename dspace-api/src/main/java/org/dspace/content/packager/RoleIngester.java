/**
 * RoleIngester.java
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
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Create EPersons and Groups from a file of external representations.
 * 
 * @author mwood
 */
public class RoleIngester implements PackageIngester
{
    private static final Logger log = LoggerFactory
            .getLogger(RoleIngester.class);

    /**
     * Common code to ingest roles from a Document.
     * 
     * @param context
     * @param document
     * @throws SQLException
     * @throws AuthorizeException
     * @throws PackageException
     */
    static void ingestDocument(Context context, PackageParameters params,
            Document document) throws SQLException, AuthorizeException,
            PackageException
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
                    continue; // Cannot operate on my own EPerson!
                identity = email;
                collider = EPerson.findByEmail(context, identity);
                // collider = EPerson.find(context, userID);
            }
            else if (netids.getLength() > 0)
            {
                netid = netids.item(0).getTextContent();
                if (netid.equals(myNetid))
                    continue; // Cannot operate on my own EPerson!
                identity = netid;
                collider = EPerson.findByNetid(context, identity);
            }
            else
                throw new PackageException(
                        "EPerson has neither email nor netid.");

            if (null != collider)
                if (params.replaceModeEnabled()) // -r -f
                {
                    eperson = collider;
                }
                else if (params.keepExistingModeEnabled()) // -r -k
                {
                    log.warn("Existing EPerson {} was not restored from the package.",
                             identity);
                    continue;
                }
                else
                    throw new PackageException("EPerson " + identity
                            + " already exists.");
            else
            {
                eperson = EPerson.create(context);
                log.info("Created EPerson {}.", identity);
            }

            eperson.setEmail(email);
            eperson.setNetid(netid);

            NodeList data;

            data = user.getElementsByTagName(RoleDisseminator.FIRST_NAME);
            if (data.getLength() > 0)
                eperson.setFirstName(data.item(0).getTextContent());
            else
                eperson.setFirstName(null);

            data = user.getElementsByTagName(RoleDisseminator.LAST_NAME);
            if (data.getLength() > 0)
                eperson.setLastName(data.item(0).getTextContent());
            else
                eperson.setLastName(null);

            data = user.getElementsByTagName(RoleDisseminator.LANGUAGE);
            if (data.getLength() > 0)
                eperson.setLanguage(data.item(0).getTextContent());
            else
                eperson.setLanguage(null);

            data = user.getElementsByTagName(RoleDisseminator.CAN_LOGIN);
            eperson.setCanLogIn(data.getLength() > 0);

            data = user
                    .getElementsByTagName(RoleDisseminator.REQUIRE_CERTIFICATE);
            eperson.setRequireCertificate(data.getLength() > 0);

            data = user.getElementsByTagName(RoleDisseminator.SELF_REGISTERED);
            eperson.setSelfRegistered(data.getLength() > 0);

            data = user.getElementsByTagName(RoleDisseminator.PASSWORD_HASH);
            if (data.getLength() > 0)
                eperson.setPasswordHash(data.item(0).getTextContent());
            else
                eperson.setPasswordHash(null);

            // Do not update eperson!  Let subsequent problems roll it back.
        }

        // Now ingest the Groups
        NodeList groups = document.getElementsByTagName(RoleDisseminator.GROUP);

        // Create the groups and add their EPerson members
        for (int groupx = 0; groupx < groups.getLength(); groupx++)
        {
            Element group = (Element) groups.item(groupx);
            String name = group.getAttribute(RoleDisseminator.NAME);

            //Translate Group name back to internal ID format (e.g. COLLECTION_<ID>_ADMIN)
            // TODO: is this necessary? can we leave it in format with Handle in place of <ID>?
            // For now, this might be necessary, because we don't want to accidentally
            // create a new group COLLECTION_hdl:123/34_ADMIN, which is equivalent
            // to an existing COLLECTION_45_ADMIN group
            name = PackageUtils.crosswalkDefaultGroupName(context, name);
            
            Group groupObj; // The group to restore
            Group collider = Group.findByName(context, name); // Existing group?
            if (null != collider)
            { // Group already exists, so empty it
                if (params.replaceModeEnabled()) // -r -f
                {
                    for (Group member : collider.getMemberGroups())
                        collider.removeMember(member);
                    for (EPerson member : collider.getMembers())
                        collider.removeMember(member);
                    log.info("Existing Group {} was cleared.", name);
                    groupObj = collider;
                }
                else if (params.keepExistingModeEnabled()) // -r -k
                {
                    log.warn("Existing Group {} was not replaced from the package.",
                             name);
                    continue;
                }
                else
                    throw new PackageException("Group " + name
                            + " already exists");
            }
            else
            { // No such group exists
                groupObj = Group.create(context);
                groupObj.setName(name);
                log.info("Created Group {}.", name);
            }

            NodeList members = group
                    .getElementsByTagName(RoleDisseminator.MEMBER);
            for (int memberx = 0; memberx < members.getLength(); memberx++)
            {
                Element member = (Element) members.item(memberx);
                String memberName = member.getAttribute(RoleDisseminator.NAME);
                EPerson memberEPerson = EPerson.findByEmail(context, memberName);
                groupObj.addMember(memberEPerson);
            }
            // Do not groupObj.update! We want to roll back on subsequent
            // failures.
        }

        // Go back and add Group members, now that all groups exist
        for (int groupx = 0; groupx < groups.getLength(); groupx++)
        {
            Element group = (Element) groups.item(groupx);
            String name = group.getAttribute(RoleDisseminator.NAME);
            Group groupObj = Group.findByName(context, name);
            NodeList members = group
                    .getElementsByTagName(RoleDisseminator.MEMBER_GROUP);
            for (int memberx = 0; memberx < members.getLength(); memberx++)
            {
                Element member = (Element) members.item(memberx);
                String memberName = member.getAttribute(RoleDisseminator.NAME);
                Group memberGroup = Group.findByName(context, memberName);
                groupObj.addMember(memberGroup);
            }
            // Do not groupObj.update!
        }
    }

    /**
     * Ingest roles from an InputStream.
     * 
     * @param context
     * @param stream
     * @throws PackageException
     * @throws SQLException
     * @throws AuthorizeException
     */
    static public void ingestStream(Context context, PackageParameters params,
            InputStream stream) throws PackageException, SQLException,
            AuthorizeException
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
        ingestDocument(context, params, document);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dspace.content.packager.PackageIngester#ingest(org.dspace.core.Context
     * , org.dspace.content.DSpaceObject, java.io.File,
     * org.dspace.content.packager.PackageParameters, java.lang.String)
     */
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
        ingestDocument(context, params, document);

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
    public List<DSpaceObject> ingestAll(Context context, DSpaceObject parent,
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
    public List<DSpaceObject> replaceAll(Context context, DSpaceObject dso,
            File pkgFile, PackageParameters params) throws PackageException,
            UnsupportedOperationException, CrosswalkException,
            AuthorizeException, SQLException, IOException
    {
        throw new PackageException(
                "replaceAll() is not implemented, as replace() method already handles replacement of all roles from an external file.");
    }
}
