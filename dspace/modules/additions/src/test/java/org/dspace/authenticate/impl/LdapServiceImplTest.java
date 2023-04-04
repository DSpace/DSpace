package org.dspace.authenticate.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.naming.NamingException;

import edu.umd.lib.dspace.authenticate.impl.Ldap;
import edu.umd.lib.dspace.authenticate.impl.LdapServiceImpl;
import edu.umd.lib.dspace.authenticate.impl.LdapServiceImpl.LdapClient;
import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LdapServiceImplTest extends AbstractUnitTest {
    private TestableLdapServiceImpl ldapService;
    private LdapClient client;
    private Map<String, Ldap> ldapQueryCache;

    @Before
    public void setUp() throws Exception {
        this.ldapService = new TestableLdapServiceImpl(context);
        this.client = ldapService.getClient();
        this.ldapQueryCache = ldapService.replaceCacheWithSpy();
        when(client.queryLdapService(any())).thenReturn(new Ldap("testUser", null));
    }

    @Test
    public void ldapCacheMissesOnFirstUse() throws Exception {
        Ldap ldap = ldapService.queryLdap("testUser");

        assertNotNull(ldap);
        verify(ldapQueryCache, times(1)).containsKey(any());
        verify(client, times(1)).queryLdapService(any());
    }

    @Test
    public void ldapCacheUsedOnSecondCall() throws Exception {
        Ldap ldap1 = ldapService.queryLdap("testUser");
        Ldap ldap2 = ldapService.queryLdap("testUser");

        assertNotNull(ldap1);
        assertNotNull(ldap2);
        verify(ldapQueryCache, times(2)).containsKey(any());
        verify(ldapQueryCache, times(1)).get(any());
        verify(client, times(1)).queryLdapService(any());
    }

    @After
    public void tearDown() {
        // Reset the ldapQueryCache because it is a static variable that
        // otherwise will be persistent (as a Mockito spy) across tests
        ldapService.resetLdapQueryCache();
    }

    /**
     * Testable implementation of the LdapServiceImpl class that replaces
     * the LdapServerDelegate with a mock implementation, and allows the
     * ldapQueryCache to be accessed.
     */
    class TestableLdapServiceImpl extends LdapServiceImpl {
        public TestableLdapServiceImpl(org.dspace.core.Context context) throws NamingException {
            super(context);
        }

        @Override
        protected LdapClient createLdapClient(org.dspace.core.Context context) throws NamingException {
            return mock(LdapServiceImpl.LdapClient.class);
        }

        public LdapClient getClient() {
            return this.client;
        }

        public Map<String, Ldap> replaceCacheWithSpy() {
            ldapQueryCache = spy(LdapServiceImpl.ldapQueryCache);
            return ldapQueryCache;
        }

        public void resetLdapQueryCache() {
            ldapQueryCache = null;
        }
    }
}
