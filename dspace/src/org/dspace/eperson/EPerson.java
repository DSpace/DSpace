/*
 * EPerson
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Class representing an e-person.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class EPerson
{
    /** Our context */
    private Context myContext;

    /** The row in the table representing this format */
    private TableRow myRow;


    /**
     * Construct an EPerson
     * @param context  the context this object exists in
     * @param row      the corresponding row in the table
     */
    EPerson( Context context, TableRow row )
    {
        myContext = context;
        myRow = row;
    }

    /**
     * Get an EPerson from the database.
     *
     * @param  context  DSpace context object
     * @param  id       ID of the EPerson
     *
     * @return  the EPerson format, or null if the ID is invalid.
     */
    public static EPerson find(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.find( context, "eperson", id );

        if ( row == null )
        {
            return null;
        }
        else
        {
            return new EPerson( context, row );
        }
    }

    /**
     * Find the eperson by their email address
     *
     * @ return EPerson
     */
    public static EPerson findByEmail(Context context, String email)
        throws SQLException, AuthorizeException
    {
        TableRow row = DatabaseManager.findByUnique( context, "eperson", "email", email );

        if ( row == null )
        {
            return null;
        }
        else
        {
            return new EPerson( context, row );
        }
    }


    /**
     * Create a new eperson
     *
     * @param  context  DSpace context object
     */
    public static EPerson create(Context context)
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation

        // Create a table row
        TableRow row = DatabaseManager.create(context, "eperson");
        return new EPerson(context, row);
    }


    /**
     * Delete an eperson
     *
     */
    public void delete()
        throws SQLException
    {
        // FIXME: authorizations

       	// Remove any group memberships first
       	DatabaseManager.updateQuery(myContext,
            "DELETE FROM EPersonGroup2EPerson WHERE eperson_id=" +
               	getID() );

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);
    }


    /**
     * Get the e-person's internal identifier
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return myRow.getIntColumn("eperson_id");
    }


    /**
     * Get the e-person's email address
     *
     * @return  their email address
     */
    public String getEmail()
    {
        return myRow.getStringColumn("email");
    }


    /**
     * Set the EPerson's email
     *
     * @param  s   the new email
     */
    public void setEmail(String s)
    {
        myRow.setColumn("email", s);
    }


    /**
     * Get the e-person's full name, combining first and last name
     * in a displayable string.
     *
     * @return  their full name
     */
    public String getFullName()
    {
        // FIXME: make this work.  ;-)
        String t = myRow.getStringColumn("firstname") + " " + myRow.getStringColumn("lastname");

        return t;
    }


    /**
     * Get the eperson's first name.
     *
     * @return their first name
     */
    public String getFirstName()
    {
        return myRow.getStringColumn("firstname");
    }


    /**
     * Set the eperson's first name
     *
     * @param firstname the person's first name
     */
    public void setFirstName(String firstname)
    {
        myRow.setColumn("firstname", firstname);
    }


    /**
     * Get the eperson's last name.
     *
     * @return their last name
     */
    public String getLastName()
    {
        return myRow.getStringColumn("lastname");
    }


    /**
     * Set the eperson's last name
     *
     * @param lastname the person's last name
     */
    public void setLastName(String lastname)
    {
        myRow.setColumn("lastname", lastname);
    }


    /**
     * Set active/inactive
     *
     * @param isactive boolean yes/no
     */
    public void setActive(boolean isactive)
    {
        myRow.setColumn("active", isactive);
    }

    /**
     * Get active/inactive
     *
     * @return isactive boolean, yes/no
     */
    public boolean getActive()
    {
        return myRow.getBooleanColumn("active");
    }


    /**
     * Set require cert yes/no
     *
     * @param isrequired boolean yes/no
     */
    public void setRequireCertificate(boolean isrequired)
    {
        myRow.setColumn("requirecertificate", isrequired);
    }


    /**
     * Get active/inactive
     *
     * @return isactive boolean, yes/no
     */
    public boolean getRequireCertificate()
    {
        return myRow.getBooleanColumn("requirecertificate");
    }


    /**
     * Get the value of a metadata field
     *
     * @param  field   the name of the metadata field to get
     *
     * @return  the value of the metadata field
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public String getMetadata(String field)
    {
       	return myRow.getStringColumn(field);
    }


    /**
     * Set a metadata value
     *
     * @param  field   the name of the metadata field to get
     * @param  value   value to set the field to
     *
     * @exception IllegalArgumentException   if the requested metadata
     *            field doesn't exist
     */
    public void setMetadata(String field, String value)
    {
       	myRow.setColumn(field, value);
    }


    // lastname, active, requirecertificate, phone


    /**
     * Set the EPerson's password
     *
     * @param  s   the new email
     */
    public void setPassword(String s)
    {
        // FIXME:  encoding
        String encoded = encodePassword(s);

        myRow.setColumn("password", encoded);
    }


    /**
     * Check EPerson's password
     *
     * @param attempt the password attempt
     * @return boolean successful/unsuccessful
     */
    public boolean checkPassword(String attempt)
    {
        String encoded = encodePassword( attempt);

        if(attempt.equals(myRow.getStringColumn("password")))
            return true;

        return false;
    }


    /**
     * Update the EPerson
     */
    public void update()
        throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation

        DatabaseManager.update(myContext, myRow);
    }

    //--------- private methods ------------------

    /**
     * encodePassword() is a utility function to encode a password,
     *
     * @param cleartextpw   cleartext password to encode
     */

    private static String encodePassword(String cleartextpw)
    {
        return Utils.getMD5(cleartextpw);
    }
}
