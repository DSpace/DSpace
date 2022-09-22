package edu.umd.lib.dspace.authenticate;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.eperson.EPerson;
import edu.umd.lib.dspace.authenticate.impl.LdapInfo;

/**
 * Interface used with Ldap to provide authorizations for CAS authentication.
 */
public interface Ldap extends AutoCloseable {
    /**
     * Queries LDAP for the given username, returning an LdapInfo object if
     * found, otherwise null is returned.
     *
     * @param strUid the LDAP user id to retrieve
     */
    public LdapInfo queryLdap(String strUid) throws NamingException;

    /**
     * Close the ldap connection
     */
    @Override
    public void close();
}
