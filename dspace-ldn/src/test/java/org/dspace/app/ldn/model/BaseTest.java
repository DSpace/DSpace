/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 *
 */
public class BaseTest {

    @Test
    public void testBase() {
        // test constructor
        Base base = new Base();
        assertNotNull(base.getType());
        assertEquals(0, base.getType().size());

        // test id setter and getter
        base.setId("urn:uuid:a56881ef-b0ee-4ebb-ab08-04c0a1ee9ada");
        assertEquals("urn:uuid:a56881ef-b0ee-4ebb-ab08-04c0a1ee9ada", base.getId());

        // test type setter and getter and synthetic add
        Set<String> type = new HashSet<>();
        type.add("Announce");
        type.add("coar-notify:ReleaseAction");

        base.setType(type);

        assertEquals(2, base.getType().size());

        base.addType("Test");

        assertEquals(3, base.getType().size());

        List<String> types = base.getType().stream().collect(Collectors.toList());

        assertTrue(types.contains("Test"));
        assertTrue(types.contains("Announce"));
        assertTrue(types.contains("coar-notify:ReleaseAction"));

        // test base uniqueness
        Base anotherBase = new Base();
        anotherBase.setId("urn:uuid:f86a3c26-68fa-4ddd-ad59-5138f5d583fa");
        anotherBase.setType(type);

        assertFalse("Base should not equal another base with different id", base.equals(anotherBase));

        List<Base> nonUniqueBases = new ArrayList<>();

        nonUniqueBases.add(base);
        nonUniqueBases.add(anotherBase);

        assertEquals(2, nonUniqueBases.size());

        Set<Base> uniqueBases = new HashSet<>();

        uniqueBases.add(base);
        uniqueBases.add(anotherBase);

        assertEquals(2, uniqueBases.size());

        // hmmm... nice to have a set that reevaluated its uniqueness if any two entries
        // become duplicate through their own mutation
        anotherBase.setId("urn:uuid:a56881ef-b0ee-4ebb-ab08-04c0a1ee9ada");

        assertTrue("Base should equal another base with same id", base.equals(anotherBase));

        assertEquals(2, nonUniqueBases.size());
        assertEquals(2, uniqueBases.size());

        // clear and test uniqueness during insertion
        nonUniqueBases.clear();
        uniqueBases.clear();

        nonUniqueBases.add(base);
        nonUniqueBases.add(anotherBase);

        uniqueBases.add(base);
        uniqueBases.add(anotherBase);

        assertEquals(2, nonUniqueBases.size());
        assertEquals(1, uniqueBases.size());
    }

}
