/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.CrisSecurity;

/**
 * Implementation of {@link AccessItemMode} to configure the item correction
 * modes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CorrectItemMode implements AccessItemMode {

    /**
     * Defines the users enabled to use this correction configuration
     */
    private CrisSecurity security;

    /**
     * Contains the list of groups metadata for CUSTOM security
     */
    private List<String> groups = new ArrayList<String>();

    /**
     * Contains the list of users metadata for CUSTOM security
     */
    private List<String> users = new ArrayList<String>();

    @Override
    public CrisSecurity getSecurity() {
        return security;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public List<String> getUsers() {
        return users;
    }

    public void setSecurity(CrisSecurity security) {
        this.security = security;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

}
