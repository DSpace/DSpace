/*
 * EPerson.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
package org.dspace.eperson;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.event.Event;

/**
 * Class representing an e-person.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class EPerson extends DSpaceObject
{
    private static Logger log = Logger.getLogger(EPerson.class);

    private EPersonDAO dao;

    /** See EPersonMetadataField. */
    private Map<EPersonMetadataField, String> metadata;

    private boolean selfRegistered;
    private boolean canLogin;
    private boolean requireCertificate;

    /** Sort fields */
    public static final int EMAIL = 1;
    public static final int LASTNAME = 2;
    public static final int ID = 3;
    public static final int NETID = 4;
    public static final int LANGUAGE = 5;

    /** Flag set when data is modified, for events */
    private boolean modified;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;
    
    public enum EPersonMetadataField
    {
        FIRSTNAME ("firstname"),
        LASTNAME ("lastname"),
        PASSWORD ("password"),
        EMAIL ("email"),
        PHONE ("phone"),
        NETID ("netid"),
        LANGUAGE ("language");

        private String name;

        private EPersonMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }

        public static EPersonMetadataField fromString(String name)
        {
            for (EPersonMetadataField f : values())
            {
                if (f.toString().equals(name))
                {
                    return f;
                }
            }

            throw new IllegalArgumentException(name +
                    " isn't a valid metadata field for EPeople.");
        }
    }

    public EPerson(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = EPersonDAOFactory.getInstance(context);

        metadata = new EnumMap<EPersonMetadataField,
            String>(EPersonMetadataField.class);

        context.cache(this, id);
                
        modified = modifiedMetadata = false;
        clearDetails();
    }

     public String getLanguage()
     {
         return metadata.get(EPersonMetadataField.LANGUAGE);
     }

     public void setLanguage(String language)
     {
         if (language != null)
         {
             language = language.toLowerCase();
         }

         metadata.put(EPersonMetadataField.LANGUAGE, language);
     }

    public String getEmail()
    {
        return metadata.get(EPersonMetadataField.EMAIL);
    }

    public void setEmail(String email)
    {
        if (email != null)
        {
            email = email.toLowerCase();
        }

        metadata.put(EPersonMetadataField.EMAIL, email);
        modified = true;
        modified = true;
    }

    public String getNetid()
    {
        return metadata.get(EPersonMetadataField.NETID);
    }

    public void setNetid(String netid)
    {
        if (netid != null)
        {
            netid = netid.toLowerCase();
        }

        metadata.put(EPersonMetadataField.NETID, netid);
        modified = true;
        modified = true;
    }

    public String getName()
    {
        return getEmail();
    }

    /**
     * Get the e-person's full name, combining first and last name in a
     * displayable string.
     *
     * @return their full name
     */
    public String getFullName()
    {
        String firstName = metadata.get(EPersonMetadataField.FIRSTNAME);
        String lastName = metadata.get(EPersonMetadataField.LASTNAME);

        if ((lastName == null) && (firstName == null))
        {
            return getEmail();
        }
        else if (firstName == null)
        {
            return lastName;
        }
        else
        {
            return (firstName + " " + lastName);
        }
    }

    public String getFirstName()
    {
        return metadata.get(EPersonMetadataField.FIRSTNAME);
    }

    public void setFirstName(String firstName)
    {
        metadata.put(EPersonMetadataField.FIRSTNAME, firstName);
        modified = true;
        modified = true;
    }

    public String getLastName()
    {
        return metadata.get(EPersonMetadataField.LASTNAME);
    }

    public void setLastName(String lastName)
    {
        metadata.put(EPersonMetadataField.LASTNAME, lastName);
        modified = true;
        modified = true;
    }

    public void setCanLogIn(boolean canLogin)
    {
        this.canLogin = canLogin;
        modified = true;
        modified = true;
    }

    public boolean canLogIn()
    {
        return canLogin;
    }

    public void setRequireCertificate(boolean requireCertificate)
    {
        this.requireCertificate = requireCertificate;
        modified = true;
        modified = true;
    }

    public boolean getRequireCertificate()
    {
        return requireCertificate;
    }

    public void setSelfRegistered(boolean selfRegistered)
    {
        this.selfRegistered = selfRegistered;
        modified = true;
        modified = true;
    }

    public boolean getSelfRegistered()
    {
        return selfRegistered;
    }

    public String getMetadata(EPersonMetadataField field)
    {
        return metadata.get(field);
    }

    public void setMetadata(EPersonMetadataField field, String value)
    {
        metadata.put(field, value);
    }

    @Deprecated
    public String getMetadata(String field)
    {
        return metadata.get(EPersonMetadataField.fromString(field));
    }

    @Deprecated
    public void setMetadata(String field, String value)
    {
        metadata.put(EPersonMetadataField.fromString(field), value);
        modifiedMetadata = true;
        addDetails(field);
        modifiedMetadata = true;
        addDetails(field);
    }

    public void setPassword(String password)
    {
        metadata.put(EPersonMetadataField.PASSWORD, Utils.getMD5(password));
        modified = true;
        modified = true;
    }

    public boolean checkPassword(String attempt)
    {
        String encoded = Utils.getMD5(attempt);

        return (encoded.equals(metadata.get(EPersonMetadataField.PASSWORD)));
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.EPERSON;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public static EPerson[] findAll(Context context, int sortField)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.getEPeople(sortField);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    @Deprecated
    public static EPerson find(Context context, int id)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.retrieve(id);
    }

    @Deprecated
    public static EPerson[] search(Context context, String query)
    {
        return search(context, query, -1, -1);
    }

    @Deprecated
    public static EPerson[] search(Context context, String query,
            int offset, int limit)
	{
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.search(query, offset, limit);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
	}

    @Deprecated
    public static EPerson findByEmail(Context context, String email)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.retrieve(EPersonMetadataField.EMAIL, email);
    }

    @Deprecated
    public static EPerson findByNetid(Context context, String netid)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.retrieve(EPersonMetadataField.NETID, netid);
    }

    @Deprecated
    public static EPerson create(Context context) throws AuthorizeException
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);

        return dao.create();
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);

        if (modified)
        {
            context.addEvent(new Event(Event.MODIFY, Constants.EPERSON, getID(), null));
            modified = false;
        }
        if (modifiedMetadata)
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.EPERSON, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    @Deprecated
    public void delete() throws AuthorizeException, EPersonDeletionException
    {
        dao.delete(getID());
        context.addEvent(new Event(Event.DELETE, Constants.EPERSON, getID(), getEmail()));

        
    }
}
