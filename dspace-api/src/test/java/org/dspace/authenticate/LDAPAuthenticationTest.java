/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.ldap.query.SearchScope;

/**
 * Unit tests for the translation of "authentication-ldap.search_scope" into the scope of
 * the Spring LDAP query that looks up the DN of a user.
 */
public class LDAPAuthenticationTest {

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
}
