/*
 * Context
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

package org.dspace.core;

import java.sql.Connection;
import java.sql.SQLException;

import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.eperson.EPerson;

/**
 * Class representing the context of a particular DSpace operation.  This
 * stores information such as the current authenticated user and the
 * database connection being used.
 * <P>
 * Typical use of the context object will involve constructing one, and setting
 * the current user if one is authenticated.  Several operations may be
 * performed using the context object.  If all goes well, <code>complete</code>
 * is called to commit the changes and free up any resources used by the
 * context.  If anything has gone wrong, <code>abort</code> is called to
 * roll back any changes and free up the resources.
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class Context
{
    /** Database connection */
    private Connection connection;
    
    /** Current user */
    private EPerson currentUser;

    /** Extra log info */
    private String extraLogInfo;

    /**
     * Construct a new context object.  A database connection is opened.
     * No user is authenticated.
     *
     * @exception SQLException
     *         if there was an error obtaining a database connection
     */
    public Context()
        throws SQLException
    {
        // Obtain a non-auto-committing connection
        connection = DatabaseManager.getConnection();
        connection.setAutoCommit(false);

        currentUser = null;
        extraLogInfo = "";
    }
    

    /**
     * Get the database connection associated with the context
     *
     * @return  the database connection
     */
    public Connection getDBConnection()
    {
        return connection;
    }
    
    /**
     * Set the current user.  Authentication must have been performed by the
     * caller - this call does not attempt any authentication.
     *
     * @param  user   the new current user, or <code>null</code> if no user is
     *                authenticated
     */
    public void setCurrentUser(EPerson user)
    {
        currentUser = user;
    }

    /**
     * Get the current (authenticated) user
     *
     * @return  the current user, or <code>null</code> if no user is
     *          authenticated
     */
    public EPerson getCurrentUser()
    {
        return currentUser;
    }
    
    /**
     * Set extra information that should be added to any message logged in the
     * scope of this context.  An example of this might be the session ID
     * of the current Web user's session:
     * <P>
     * <code>setExtraLogInfo("session_id="+request.getSession().getId());</code>
     *
     * @param info  the extra information to log
     */
    public void setExtraLogInfo(String info)
    {
        extraLogInfo = info;
    }
    
    /**
     * Get extra information to be logged with message logged in the scope of
     * this context.
     *
     * @return  the extra log info - guaranteed non-<code>null</code>
     */
    public String getExtraLogInfo()
    {
        return extraLogInfo;
    }

    /**
     * Close the context object after all of the operations performed in the
     * context have completed succesfully.  Any transaction with the database
     * is committed.
     *
     * @exception SQLException
     *     if there was an error completing the database transaction or closing
     *     the connection
     */
    public void complete()
        throws SQLException
    {
        // FIXME: Might be good not to do a commit() if nothing has actually
        // been written using this connection

        // Commit any changes made as part of the transaction
       connection.commit();

       // Free the connection
       DatabaseManager.freeConnection(connection);
       connection = null;
    }
    
    /**
     * Close the context, without committing any of the changes performed using
     * this context.  The database connection is freed.  No exception is thrown
     * if there is an error freeing the database connection, since this method
     * may be called as part of an error-handling routine where an SQLException
     * has already been thrown.
     */
    public void abort()
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException se)
        {
            // Do nothing; we may be here when a database error has already
            // occurred.  In any case, nothing will be written.
        }
        finally
        {
            DatabaseManager.freeConnection(connection);
            connection = null;
        }
    }
}
