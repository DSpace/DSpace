/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.dspace.util.PersonNameUtil.getAllNameVariants;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for {@link PersonNameUtil}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class PersonNameUtilTest {

    @Test
    public void testWithAllNames() {

        Set<String> variants = getAllNameVariants("Luca", "Giamminonni", List.of("Giamminonni, Luca",
                        "Luke Giammo"), "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca", "Luca Giamminonni",
            "Giamminonni L.", "L. Giamminonni", "Giamminonni L", "L Giamminonni", "Luke Giammo", "Giammo Luke"));
    }

    @Test
    public void testWithFirstNameComposedByTwoNames() {

        Set<String> variants = getAllNameVariants("Luca Paolo", "Giamminonni",
            List.of("Giamminonni, Luca", "Luke Giammo"), "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca Paolo", "Luca Paolo Giamminonni",
            "Giamminonni Luca", "Luca Giamminonni", "Giamminonni Paolo", "Paolo Giamminonni",
            "Giamminonni L. P.", "L. P. Giamminonni", "Giamminonni L P", "L P Giamminonni", "Luke Giammo",
            "Giamminonni L.", "Giamminonni L", "L. Giamminonni", "L Giamminonni",
            "Giamminonni P.", "Giamminonni P", "P. Giamminonni", "P Giamminonni",
            "Giammo Luke"));
    }

    @Test
    public void testWithFirstNameComposedByThreeNames() {

        Set<String> variants = getAllNameVariants("Luca Paolo Claudio", "Giamminonni",
            List.of("Giamminonni, Luca", "Luke Giammo"), "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca Paolo Claudio", "Luca Paolo Claudio Giamminonni",
            "Giamminonni Luca Claudio", "Luca Claudio Giamminonni", "Giamminonni Paolo Claudio",
            "Paolo Claudio Giamminonni", "Giamminonni Luca Paolo", "Luca Paolo Giamminonni", "Giamminonni Luca",
            "Luca Giamminonni", "Giamminonni Paolo", "Paolo Giamminonni", "Giamminonni Claudio", "Claudio Giamminonni",
            "Giamminonni L. P.", "L. P. Giamminonni", "Giamminonni P. C.", "P. C. Giamminonni", "Giamminonni P C",
            "P C Giamminonni", "Giamminonni L P", "L P Giamminonni", "L. C. Giamminonni", "Giamminonni L. C.",
            "Giamminonni L.", "Giamminonni L", "L. Giamminonni", "L Giamminonni",
            "Giamminonni P.", "Giamminonni P", "P. Giamminonni", "P Giamminonni",
            "Giamminonni C.", "Giamminonni C", "C. Giamminonni", "C Giamminonni",
            "L C Giamminonni", "Giamminonni L C", "Luke Giammo", "Giammo Luke"));

    }

    @Test
    public void testWithoutFirstAndLastName() {

        Set<String> variants = getAllNameVariants(null, null, List.of("Giamminonni, Luca Fabio", "Luke Giammo"),
                "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca Fabio", "Fabio Luca Giamminonni",
            "Giamminonni Fabio Luca", "Luca Fabio Giamminonni", "Luca Giamminonni Fabio",
            "Fabio Giamminonni Luca", "Luke Giammo", "Giammo Luke"));

    }

    @Test
    public void testWithAlreadyTruncatedName() {

        Set<String> variants = getAllNameVariants("L.", "Giamminonni", List.of("Giamminonni, Luca"),
                "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca", "Luca Giamminonni",
            "Giamminonni L.", "L. Giamminonni", "Giamminonni L", "L Giamminonni"));

        variants = getAllNameVariants("L. P.", "Giamminonni", List.of("Giamminonni, Luca"), "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca", "Luca Giamminonni", "L. Giamminonni",
            "Giamminonni L.", "P. Giamminonni", "Giamminonni P.", "Giamminonni L. P.", "L. P. Giamminonni",
            "Giamminonni L P", "L P Giamminonni", "Giamminonni L", "L Giamminonni",
            "Giamminonni P", "P Giamminonni"));

    }

    @Test
    public void testWithAlreadyTruncatedNameOnFullName() {

        Set<String> variants = getAllNameVariants("Luca", "Giamminonni", List.of("Giamminonni, L."),
                "uuid");

        assertThat(variants, containsInAnyOrder("Giamminonni Luca", "Luca Giamminonni",
            "Giamminonni L.", "L. Giamminonni", "Giamminonni L", "L Giamminonni"));

    }

}
