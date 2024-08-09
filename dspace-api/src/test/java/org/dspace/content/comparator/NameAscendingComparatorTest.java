/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.comparator;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NameAscendingComparatorTest {

    private NameAscendingComparator comparator;

    @Mock
    private DSpaceObject dso1;

    @Mock
    private DSpaceObject dso2;

    @Mock
    private ContentServiceFactory contentServiceFactory;

    @Mock
    private DSpaceObjectService<DSpaceObject> dsoService;

    @Before
    public void setup() {
        when(contentServiceFactory.getDSpaceObjectService(dso1)).thenReturn(dsoService);
        when(contentServiceFactory.getDSpaceObjectService(dso2)).thenReturn(dsoService);

        comparator = new NameAscendingComparator(contentServiceFactory);
    }

    @Test
    public void testCompareLessThan() throws Exception {
        when(dsoService.getName(dso1)).thenReturn("a");
        when(dsoService.getName(dso2)).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareGreaterThan() throws Exception {
        when(dsoService.getName(dso1)).thenReturn("a");
        when(dsoService.getName(dso2)).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) > 0);
    }

    @Test
    public void testCompareEqual() throws Exception {
        when(dsoService.getName(dso1)).thenReturn("b");
        when(dsoService.getName(dso2)).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) == 0);
    }

    @Test
    public void testCompareFirstNull() throws Exception {

        assertTrue(comparator.compare(null, dso2) < 0);
    }

    @Test
    public void testCompareSecondNull() throws Exception {

        assertTrue(comparator.compare(dso1, null) > 0);
    }

    @Test
    public void testCompareBothNull() throws Exception {
        assertTrue(comparator.compare(null, null) == 0);
    }

    @Test
    public void testCompareNameNull() throws Exception {
        when(dsoService.getName(dso1)).thenReturn(null);
        when(dsoService.getName(dso2)).thenReturn("b");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareCaseInsensitive() throws Exception {
        when(dsoService.getName(dso1)).thenReturn("a");
        when(dsoService.getName(dso2)).thenReturn("B");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }

    @Test
    public void testCompareCaseTrimmed() throws Exception {
        when(dsoService.getName(dso1)).thenReturn("a");
        when(dsoService.getName(dso2)).thenReturn(" b ");

        assertTrue(comparator.compare(dso1, dso2) < 0);
    }
}
