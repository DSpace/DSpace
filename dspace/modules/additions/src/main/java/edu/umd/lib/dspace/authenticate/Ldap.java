/*
 * Copyright (c) 2004 The University of Maryland. All Rights Reserved.
 *
 */

package edu.umd.lib.dspace.authenticate;

import java.util.List;

import javax.naming.NamingException;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/*********************************************************************
 * Use Ldap to provide authorizations for CAS authentication.
 *
 * @author Ben Wallberg
 *
 *********************************************************************/

public interface Ldap {
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    /*************************************************************** checkUid */
    /**
     * Check if a user supplied uid is valid.
     */

    public boolean checkUid(String strUid) throws NamingException;

    /********************************************************** checkPassword */
    /**
     * Check if a user supplied password is valid.
     */

    public boolean checkPassword(String strPassword) throws NamingException;

    /*********************************************************** checkAdmin */
    /**
     * Check for an admin user override.
     */

    public boolean checkAdmin(String strLdapPassword);

    /***************************************************************** close */
    /**
     * Close the ldap connection
     */

    public void close();

    /************************************************************** finalize */
    /**
     * Close the ldap connection
     */

    public void finalize();

    /****************************************************** getAttributeAll */
    /**
     * get all instances of an attribute.
     */

    public List<String> getAttributeAll(String strName) throws NamingException;

    /********************************************************* getAttribute */
    /**
     * get an attribute (first instance).
     */

    public String getAttribute(String strName) throws NamingException;

    /************************************************************* getEmail */
    /**
     * user's email address
     */

    public String getEmail() throws NamingException;

    /************************************************************* getPhone */
    /**
     * user's phone
     */

    public String getPhone() throws NamingException;

    /********************************************************* getFirstName */
    /**
     * user's first name
     */

    public String getFirstName() throws NamingException;

    /********************************************************** getLastName */
    /**
     * user's last name
     */

    public String getLastName() throws NamingException;

    /************************************************************** getUnits */
    /**
     * organization units
     */

    public List<String> getUnits() throws NamingException;

    /************************************************************* getGroups */
    /**
     * Groups mapped by the Units for faculty.
     */

    public List<Group> getGroups() throws NamingException, java.sql.SQLException;

    /************************************************************ isFaculty */
    /**
     * is the user CP faculty with an acceptable status?
     */

    public boolean isFaculty() throws NamingException;

    /****************************************************** registerEPerson */
    /**
     * Register this ldap user as an EPerson
     */

    public EPerson registerEPerson(String uid) throws Exception;

    /*********************************************************** setContext */
    /**
     * Reset the context. We lost it after every request.
     */

    public void setContext(org.dspace.core.Context context);
}
