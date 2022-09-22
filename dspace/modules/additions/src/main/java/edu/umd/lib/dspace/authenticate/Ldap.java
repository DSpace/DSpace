package edu.umd.lib.dspace.authenticate;

import java.util.List;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import edu.umd.lib.dspace.authenticate.impl.LdapInfo;

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

    public LdapInfo checkUid(String strUid) throws NamingException;

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
     * Register this ldap user as an EPerson
     */
    public EPerson registerEPerson(String uid, LdapInfo ldapInfo, HttpServletRequest request) throws Exception;

    /**
     * Reset the context. We lost it after every request.
     */
    public void setContext(org.dspace.core.Context context);
}
