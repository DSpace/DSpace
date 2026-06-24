/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.dspace.AbstractUnitTest;
import org.junit.Test;

/**
 * Unit tests for {@link DCInputAuthority}.
 */
public class DCInputAuthorityTest extends AbstractUnitTest {

    /**
     * When the authority has no value-pairs configured for the requested locale's
     * language, {@link DCInputAuthority#getBestMatch(String, String)} and
     * {@link DCInputAuthority#getMatches(String, int, int, String)} must return an
     * empty result instead of throwing a {@link NullPointerException}.
     */
    @Test
    public void noValuePairsForLocaleReturnsEmptyResult() throws Exception {
        DCInputAuthority authority = new DCInputAuthority();
        // Pre-populate the (empty) value/label maps so the once-only init() is a
        // no-op and lookups for any locale return null, reproducing a locale that
        // has no configured value-pairs.
        setField(authority, "values", new HashMap<String, String[]>());
        setField(authority, "labels", new HashMap<String, String[]>());

        Choices bestMatch = authority.getBestMatch("anything", "en");
        assertNotNull(bestMatch);
        assertEquals(Choices.CF_NOTFOUND, bestMatch.confidence);
        assertEquals(0, bestMatch.values.length);

        Choices matches = authority.getMatches("anything", 0, 10, "en");
        assertNotNull(matches);
        assertEquals(0, matches.values.length);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = DCInputAuthority.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
