/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.util.List;

/**
 * Interface to be extended for the configuration related to access item modes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface AccessItemMode {

    /**
     * Returns the configured security.
     *
     * @return the configured security
     */
    public CrisSecurity getSecurity();

    /**
     * Returns the configured groups for the CUSTOM security.
     *
     * @return the groups list
     */
    public List<String> getGroups();

    /**
     * Returns the configured users for the CUSTOM security.
     *
     * @return the users list
     */
    public List<String> getUsers();
}
