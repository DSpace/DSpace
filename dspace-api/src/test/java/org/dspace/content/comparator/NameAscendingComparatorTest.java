/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.comparator;

import org.dspace.content.DSpaceObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NameAscendingComparatorTest {

    private NameAscendingComparator comparator = new NameAscendingComparator();

    @Mock
    private DSpaceObject dso1;

    @Mock
    private DSpaceObject dso2;


    @Test
    public void testCompareLessThan() throws Exception {
        when(dso1.getName()).thenReturn("a");
        when(dso2.getName()).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareGreaterThan() throws Exception {
        when(dso1.getName()).thenReturn("b");
        when(dso2.getName()).thenReturn("a");

        assertTrue(comparator.compare(dso1, dso2) > 0);
    }

    @Test
    public void testCompareEqual() throws Exception {
        when(dso1.getName()).thenReturn("b");
        when(dso2.getName()).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) == 0);
    }

    @Test
    public void testCompareFirstNull() throws Exception {
        when(dso2.getName()).thenReturn("b");

        assertTrue(comparator.compare(null, dso2) < 0);
    }

    @Test
    public void testCompareSecondNull() throws Exception {
        when(dso1.getName()).thenReturn("a");

        assertTrue(comparator.compare(dso1, null) > 0);
    }

    @Test
    public void testCompareBothNull() throws Exception {
        assertTrue(comparator.compare(null, null) == 0);
    }

    @Test
    public void testCompareNameNull() throws Exception {
        when(dso1.getName()).thenReturn(null);
        when(dso2.getName()).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareCaseInsensitive() throws Exception {
        when(dso1.getName()).thenReturn("a");
        when(dso2.getName()).thenReturn("B");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareCaseTrimmed() throws Exception {
        when(dso1.getName()).thenReturn("a");
        when(dso2.getName()).thenReturn(" b ");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }
}