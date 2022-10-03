package edu.umd.lib.dspace.authenticate;

import edu.umd.lib.dspace.authenticate.impl.Ldap;

/**
 * Interface used with Ldap to provide authorizations for CAS authentication.
 */
public interface LdapService extends AutoCloseable {
    /**
     * Queries LDAP for the given username, returning an Ldap object if found,
     * otherwise null is returned.
     *
     * @param strUid the LDAP user id to retrieve
     */
    public Ldap queryLdap(String strUid);

    /**
     * Close the ldap connection
     */
    @Override
    public void close();
}
