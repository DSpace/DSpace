/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login;

import org.dspace.core.Context;

/**
 * Interface for classes that need to perform some operations after the user
 * login.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface PostLoggedInAction {

    /**
     * Perform some operations after the user login.
     *
     * @param context the DSpace context
     */
    public void loggedIn(Context context);
}
