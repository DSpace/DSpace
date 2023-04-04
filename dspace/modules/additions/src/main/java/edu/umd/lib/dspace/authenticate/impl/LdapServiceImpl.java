/*
 * Copyright (c) 2004 The University of Maryland. All Rights Reserved.
 *
 */

package edu.umd.lib.dspace.authenticate.impl;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import edu.umd.lib.dspace.authenticate.LdapService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.LogHelper;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Implementation of the LdapService interface.
 */
public class LdapServiceImpl implements LdapService {

    /** log4j category */
    private static Logger log = LogManager.getLogger(LdapServiceImpl.class);

    private String strUid = null;
    private SearchResult entry = null;

    // The list of LDAP attributes to return as part of the SearchResult
    private static final String[] strRequestAttributes =
    new String[]{"givenname", "sn", "mail", "umfaculty", "telephonenumber",
                 "ou", "umappointment"};

    private final static ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * The client for calls to the LDAP server. Made protected for use in
     * tests
     */
    protected LdapClient client;

    /**
     * Configures the LDAP connection, based on configuration and environment
     * variables.
     */
    public LdapServiceImpl(org.dspace.core.Context context) throws NamingException {
        this.client = createLdapClient(context);
    }

    /**
     * Returns the LdapClient for use in querying the LDAP server
     *
     * This class it provided to enable tests to override calls to the LDAP
     * server.
     *
     * @return the LdapClient for use in querying the LDAP server
     */
    protected LdapClient createLdapClient(org.dspace.core.Context context)
            throws NamingException {
        return new LdapClient(context);
    }

    /**
     * Queries LDAP for the given username, returning an Ldap object if found,
     * otherwise null is returned.
     *
     * @param strUid the LDAP user id to retrieve
     */
    @Override
    public Ldap queryLdap(String strUid) {
        try {
            Ldap ldap = null;
            ldap = client.queryLdapService(strUid);
            return ldap;
        } catch (NamingException ne) {
            log.error("LDAP NamingException for '" + strUid + "'", ne);
        }
        return null;
    }

    /**
     * Close the ldap connection
     */
    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String toString() {
        if (entry == null) {
            return "null";
        }
        return strUid + " (" + entry.getName() + ")";
    }

    /**
     * Implementation that constructs an LDAP client and performs operations
     * against a real LDAP server.
     */
    public static class LdapClient {
        private SearchResult entry;
        private org.dspace.core.Context context;
        private DirContext ctx;

        public LdapClient(org.dspace.core.Context context)
                throws NamingException {
            String strUrl = configurationService.getProperty("drum.ldap.url");
            String strBindAuth = configurationService.getProperty("drum.ldap.bind.auth");
            String strBindPassword =  configurationService.getProperty("drum.ldap.bind.password");
            String strConnectTimeout =  configurationService.getProperty("drum.ldap.connect.timeout");
            String strReadTimeout =  configurationService.getProperty("drum.ldap.read.timeout");

            // Setup the JNDI environment
            Properties env = new Properties();

            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.REFERRAL, "follow");

            env.put(Context.PROVIDER_URL, strUrl);
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, strBindAuth);
            env.put(javax.naming.Context.SECURITY_CREDENTIALS, strBindPassword);
            env.put("com.sun.jndi.ldap.connect.timeout", strConnectTimeout);
            env.put("com.sun.jndi.ldap.read.timeout", strReadTimeout);


            log.debug("Initializing new LDAP context");
            this.ctx = new InitialDirContext(env);

            this.context = context;
        }

        /**
         * Returns an Ldap object returned by an LDAP server for the given user
         * id, or null, of the user id is not found.
         *
         * @param strUid the user id to search for in the LDAP server.
         * @return an Ldap object returned by an LDAP server for the given user
         * id, or null, of the user id is not found.
         */
        public Ldap queryLdapService(String strUid) throws NamingException {
            if (ctx == null) {
                return null;
            }

            String strFilter = "uid=" + strUid;

            // Setup the search controls
            SearchControls sc =  new SearchControls();
            sc.setReturningAttributes(strRequestAttributes);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // Search
            log.debug("Searching LDAP server for {}", strUid);
            NamingEnumeration<SearchResult> entries = ctx.search("", strFilter, sc);

            // Make sure we got something
            if (entries == null) {
                log.warn(LogHelper.getHeader(context,
                                                "null returned on ctx.search for " + strFilter,
                                                ""));
                return null;
            }

            // Check for a match
            if (!entries.hasMore()) {
                log.debug(LogHelper.getHeader(context,
                                                "no matching entries for " + strFilter,
                                                ""));
                return null;
            }


            // Get entry
            entry = (SearchResult)entries.next();
            log.debug(LogHelper.getHeader(context,
                                            "matching entry for " + strUid + ": " + entry.getName(),
                                            ""));
            Ldap ldap = new Ldap(strUid, entry);

            // Check for another match
            if (entries.hasMore()) {
                entry = null;
                log.warn(LogHelper.getHeader(context,
                                                "multiple matching entries for " + strFilter,
                                                ""));
                return null;
            }

            log.debug(LogHelper.getHeader(context,
                                            "ldap entry:\n" + entry,
                                            ""));
            return ldap;
        }

        /**
         * Close the ldap connection
         */
        public void close() {
            if (ctx != null) {
                try {
                    ctx.close();
                    ctx = null;
                } catch (NamingException e) {
                    // Do nothing
                }
            }
        }
    }
}


