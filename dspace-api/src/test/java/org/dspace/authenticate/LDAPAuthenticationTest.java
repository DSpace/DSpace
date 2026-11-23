/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.SimpleDirContextAuthenticationStrategy;
import org.springframework.ldap.query.SearchScope;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the translation of "authentication-ldap.search_scope" into the scope of
 * the Spring LDAP query that looks up the DN of a user.
 */
public class LDAPAuthenticationTest {

    private static final String URL = "ldap://ldap.example.org:389";
    private static final String SEARCH_DN = "cn=search,dc=example,dc=org";

    @Test
    public void testParseSearchScopeHonoursConfiguredValue() {
        assertEquals(SearchScope.OBJECT, LDAPAuthentication.parseSearchScope("0"));
        assertEquals(SearchScope.ONELEVEL, LDAPAuthentication.parseSearchScope("1"));
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope("2"));
    }

    @Test
    public void testParseSearchScopeIgnoresSurroundingWhitespace() {
        assertEquals(SearchScope.ONELEVEL, LDAPAuthentication.parseSearchScope(" 1 "));
    }

    @Test
    public void testParseSearchScopeDefaultsToSubtreeWhenUnset() {
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope(null));
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope(""));
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope("   "));
    }

    @Test
    public void testParseSearchScopeDefaultsToSubtreeWhenInvalid() {
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope("one"));
        assertEquals(SearchScope.SUBTREE, LDAPAuthentication.parseSearchScope("3"));
    }

    @Test
    public void testContextSourceWithoutStartTlsIsPooled() {
        LdapContextSource contextSource = LDAPAuthentication.createContextSource(URL, SEARCH_DN, "secret", false);
        assertTrue(contextSource.isPooled());
        assertFalse(contextSource.isAnonymousReadOnly());
        assertTrue(authenticationStrategy(contextSource) instanceof SimpleDirContextAuthenticationStrategy);
    }

    @Test
    public void testContextSourceWithoutStartTlsOrSearchAccountIsAnonymousReadOnly() {
        LdapContextSource contextSource = LDAPAuthentication.createContextSource(URL, null, null, false);
        assertTrue(contextSource.isPooled());
        assertTrue(contextSource.isAnonymousReadOnly());
    }

    @Test
    public void testContextSourceWithStartTlsNegotiatesTlsWithoutPooling() {
        LdapContextSource contextSource = LDAPAuthentication.createContextSource(URL, SEARCH_DN, "secret", true);
        assertFalse(contextSource.isPooled());
        assertFalse(contextSource.isAnonymousReadOnly());
        assertTrue(authenticationStrategy(contextSource) instanceof DefaultTlsDirContextAuthenticationStrategy);
    }

    @Test
    public void testContextSourceWithStartTlsAndNoSearchAccountStillNegotiatesTls() {
        // An anonymous read-only context would be created without StartTLS.
        LdapContextSource contextSource = LDAPAuthentication.createContextSource(URL, "", "", true);
        assertFalse(contextSource.isPooled());
        assertFalse(contextSource.isAnonymousReadOnly());
        assertTrue(authenticationStrategy(contextSource) instanceof DefaultTlsDirContextAuthenticationStrategy);
    }

    private static Object authenticationStrategy(LdapContextSource contextSource) {
        return ReflectionTestUtils.getField(contextSource, "authenticationStrategy");
    }
}
