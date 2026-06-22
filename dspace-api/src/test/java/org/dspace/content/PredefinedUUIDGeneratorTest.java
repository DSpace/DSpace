/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.generator.EventType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PredefinedUUIDGenerator}, the Hibernate id generator that lets a
 * {@link DSpaceObject} supply a pre-determined UUID and otherwise falls back to a random one.
 */
public class PredefinedUUIDGeneratorTest {

    private final PredefinedUUIDGenerator generator = new PredefinedUUIDGenerator(null, null, null);

    @Test
    public void generateReturnsPredefinedUUIDWhenSet() {
        UUID predefined = UUID.randomUUID();
        DSpaceObject dso = mock(DSpaceObject.class);
        when(dso.getPredefinedUUID()).thenReturn(predefined);

        Object result = generator.generate(null, dso, null, EventType.INSERT);

        assertEquals(predefined, result);
    }

    @Test
    public void generateReturnsRandomUUIDWhenNoPredefinedUUID() {
        DSpaceObject dso = mock(DSpaceObject.class);
        when(dso.getPredefinedUUID()).thenReturn(null);

        Object result = generator.generate(null, dso, null, EventType.INSERT);

        assertInstanceOf(UUID.class, result);
    }

    @Test
    public void generateReturnsRandomUUIDForNonDSpaceObjectOwner() {
        Object result = generator.generate(null, "not-a-dspace-object", null, EventType.INSERT);

        assertInstanceOf(UUID.class, result);
    }

    @Test
    public void generateReturnsDistinctRandomUUIDsOnSuccessiveCalls() {
        Object first = generator.generate(null, "x", null, EventType.INSERT);
        Object second = generator.generate(null, "x", null, EventType.INSERT);

        assertNotEquals(first, second);
    }

    @Test
    public void getEventTypesReturnsInsertOnly() {
        assertEquals(EnumSet.of(EventType.INSERT), generator.getEventTypes());
    }
}
