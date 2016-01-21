/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * This class holds information about authenticated users (both the
 * depositing user and the on-behalf-of user), and their associated
 * DSpace Context objects.
 *
 * Since this class will be used to make authentication requests on
 * both user types, it may hold up to 2 active DSpace Context objects
 * at any one time
 *
 * WARNING: failure to use the contexts used in this class in the
 * appropriate way may result in exceptions or data loss.  Unless
 * you are performing authentication processes, you should always
 * access the context under which to deposit content into the archive
 * from:
 *
 * getContext()
 *
 * and not from any of the other context retrieval methods in this
 * class
 *
 * @author Richard Jones
 *
 */
public class SwordContext
{
    /** The primary authenticated user for the request */
    private EPerson authenticated = null;

    /** The onBehalfOf user for the request */
    private EPerson onBehalfOf = null;

    /** The primary context, representing the on behalf of user if exists, and the authenticated user if not */
    private Context context;

    /** the context for the authenticated user, which may not, therefore, be the primary context also */
    private Context authenticatorContext;

    /**
     * @return the authenticated user
     */
    public EPerson getAuthenticated()
    {
        return authenticated;
    }

    /**
     * @param authenticated    the eperson to set
     */
    public void setAuthenticated(EPerson authenticated)
    {
        this.authenticated = authenticated;
    }

    /**
     * @return the onBehalfOf user
     */
    public EPerson getOnBehalfOf()
    {
        return onBehalfOf;
    }

    /**
     * @param onBehalfOf    the eperson to set
     */
    public void setOnBehalfOf(EPerson onBehalfOf)
    {
        this.onBehalfOf = onBehalfOf;
    }

    /**
     * Returns the most appropriate context for operations on the
     * database.  This is the on-behalf-of user's context if the
     * user exists, or the authenticated user's context otherwise.
     */
    public Context getContext()
    {
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    /**
     * Get the context of the user who authenticated.  This should only be
     * used for authentication purposes.  If there is an on-behalf-of user,
     * that context should be used to write database changes.  Use:
     *
     * getContext()
     *
     * on this class instead.
     */
    public Context getAuthenticatorContext()
    {
        return authenticatorContext;
    }

    public void setAuthenticatorContext(Context authenticatorContext)
    {
        this.authenticatorContext = authenticatorContext;
    }

    /**
     * Get the context of the on-behalf-of user.  This method should only
     * be used for authentication purposes.  In all other cases, use:
     *
     * getContext()
     *
     * on this class instead.  If there is no on-behalf-of user, this
     * method will return null.
     */
    public Context getOnBehalfOfContext()
    {
        // return the obo context if this is an obo deposit, else return null
        if (this.onBehalfOf != null)
        {
            return context;
        }
        return null;
    }

    /**
     * Abort all of the contexts held by this class.  No changes will
     * be written to the database
     */
    public void abort()
    {
        // abort both contexts
        if (context != null && context.isValid())
        {
            context.abort();
        }

        if (authenticatorContext != null && authenticatorContext.isValid())
        {
            authenticatorContext.abort();
        }
    }

    /**
     * Commit the primary context held by this class, and abort the authenticated
     * user's context if it is different.  This ensures that only changes written
     * through the appropriate user's context is persisted, and all other
     * operations are flushed.  You should, in general, not try to commit the contexts directly
     * when using the sword api.
     *
     * @throws DSpaceSwordException
     */
    public void commit()
            throws DSpaceSwordException
    {
        try
        {
            // commit the primary context
            if (context != null && context.isValid())
            {
                context.complete();
            }

            // the secondary context is for filtering permissions by only, and is
            // never committed, so we abort here
            if (authenticatorContext != null && authenticatorContext.isValid())
            {
                authenticatorContext.abort();
            }
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }
}
