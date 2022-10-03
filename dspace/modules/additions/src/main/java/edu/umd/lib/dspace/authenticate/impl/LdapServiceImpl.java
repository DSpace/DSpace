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

    private org.dspace.core.Context context = null;
    private DirContext ctx = null;
    private String strUid = null;
    private SearchResult entry = null;

    private static final String[] strRequestAttributes =
    new String[]{"givenname", "sn", "mail", "umfaculty", "telephonenumber",
                 "ou", "umappointment"};

    private final static ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Configures the LDAP connection, based on configuration and environment
     * variables.
     */
    public LdapServiceImpl(org.dspace.core.Context context) throws NamingException {
        this.context = context;

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

        // Create the directory context
        log.debug("Initailizing new LDAP context");
        ctx = new InitialDirContext(env);
    }


    /**
     * Queries LDAP for the given username, returning an LdapInfo object if
     * found, otherwise null is returned.
     *
     * @param strUid the LDAP user id to retrieve
     */
    @Override
    public LdapInfo queryLdap(String strUid) {
        try {
            if (ctx == null) {
                return null;
            }

            this.strUid = strUid;
            String strFilter = "uid=" + strUid;

            // Setup the search controls
            SearchControls sc =  new SearchControls();
            sc.setReturningAttributes(strRequestAttributes);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // Search
            NamingEnumeration entries = ctx.search("", strFilter, sc);

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
            LdapInfo ldapInfo = new LdapInfo(strUid, entry);

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
            return ldapInfo;
        } catch(NamingException ne) {
            log.error("LDAP NamingException for '" + strUid + "'", ne);
        }
        return null;
    }

    /**
     * Close the ldap connection
     */
    @Override
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

    @Override
    public String toString() {
        if (entry == null) {
            return "null";
        }
        return strUid + " (" + entry.getName() + ")";
    }
}


