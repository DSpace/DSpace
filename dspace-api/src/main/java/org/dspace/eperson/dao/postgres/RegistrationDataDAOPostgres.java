/*
 * RegistrationDataDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.eperson.dao.postgres;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * @author James Rutherford
 */
public class RegistrationDataDAOPostgres extends RegistrationDataDAO
{
    public RegistrationDataDAOPostgres(Context context)
    {
        this.context = context;
    }

    @Override
    public RegistrationData create()
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "registrationdata");

            int id = row.getIntColumn("registrationdata_id");
            
            return new RegistrationData(id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public RegistrationData retrieve(int id)
    {
        return retrieve("registrationdata_id", Integer.toString(id));
    }

    @Override
    public RegistrationData retrieveByEmail(String email)
    {
        return retrieve("email", email);
    }

    @Override
    public RegistrationData retrieveByToken(String token)
    {
        return retrieve("token", token);
    }

    @Override
    public void update(RegistrationData rd)
    {
        try
        {
            TableRow row = DatabaseManager.find(context,
                    "registrationdata", rd.getID());

            // This is it -- we don't use the 'expires' column any more, though
            // the data model allows for it.
            row.setColumn("email", rd.getEmail());
            row.setColumn("token", rd.getToken());
            
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id)
    {
        super.delete(id);

        try
        {
            DatabaseManager.delete(context, "registrationdata", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(String token)
    {
        super.delete(token);

        try
        {
            DatabaseManager.deleteByValue(context, "registrationdata", "token",
                    token);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private RegistrationData retrieve(String field, String value)
    {
        try
        {
            TableRow row = null;
            if (field.equals("registrationdata_id"))
            {
                row = DatabaseManager.find(context, "registrationdata",
                        Integer.parseInt(value));
            }
            else
            {
                row = DatabaseManager.findByUnique(context, "registrationdata",
                        field, value);
            }

            if (row == null)
            {
                return null;
            }
            else
            {
                int id = row.getIntColumn("registrationdata_id");
                RegistrationData rd = new RegistrationData(id);

                rd.setEmail(row.getStringColumn("email"));
                rd.setToken(row.getStringColumn("token"));

                return rd;
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }
}
