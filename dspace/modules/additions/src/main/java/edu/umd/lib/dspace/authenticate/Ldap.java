package edu.umd.lib.dspace.authenticate;

import java.util.List;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Interface used with Ldap to provide authorizations for CAS authentication.
 */
public interface Ldap {
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    /**
     * Check if a user supplied uid is valid.
     */

    public boolean checkUid(String strUid) throws NamingException;

    /**
     * Check if a user supplied password is valid.
     */
    public boolean checkPassword(String strPassword) throws NamingException;

    /**
     * Check for an admin user override.
     */
    public boolean checkAdmin(String strLdapPassword);

    /**
     * Close the ldap connection
     */
    public void close();

    /**
     * Close the ldap connection
     */
    public void finalize();

    /**
     * get all instances of an attribute.
     */
    public List<String> getAttributeAll(String strName) throws NamingException;

    /**
     * get an attribute (first instance).
     */
    public String getAttribute(String strName) throws NamingException;

    /**
     * user's email address
     */
    public String getEmail() throws NamingException;

    /**
     * user's phone
     */
    public String getPhone() throws NamingException;

    /**
     * user's first name
     */
    public String getFirstName() throws NamingException;

    /**
     * user's last name
     */
    public String getLastName() throws NamingException;

    /**
     * organization units
     */
    public List<String> getUnits() throws NamingException;

    /**
     * Groups mapped by the Units for faculty.
     */
    public List<Group> getGroups() throws NamingException, java.sql.SQLException;

    /**
     * is the user CP faculty with an acceptable status?
     */
    public boolean isFaculty() throws NamingException;

    /**
     * Register this ldap user as an EPerson
     */
    public EPerson registerEPerson(String uid, HttpServletRequest request) throws Exception;

    /**
     * Reset the context. We lost it after every request.
     */
    public void setContext(org.dspace.core.Context context);
}
