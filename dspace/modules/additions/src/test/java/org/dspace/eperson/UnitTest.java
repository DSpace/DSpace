package org.dspace.eperson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnitTest extends AbstractUnitTest {

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
    }

    @After
    @Override
    public void destroy() {
        super.destroy();
    }

    @Test
    public void equals_UncommitedUnitsNotEqual() {
        Unit unit1 = new Unit();
        Unit unit2 = new Unit();
        assertFalse(unit1.equals(unit2));
    }

    @Test
    public void equals_DifferentUnitsNotEqual() throws Exception {
        Unit unit1 = null;
        Unit unit2 = null;
        try {
            unit1 = UnitTestUtils.createUnit(context, "Unit One", true);
            unit2 = UnitTestUtils.createUnit(context, "Unit Two", false);

            assertFalse(unit1.equals(unit2));
        } finally {
            UnitTestUtils.deleteUnit(context, unit1);
            UnitTestUtils.deleteUnit(context, unit2);
        }
    }

    @Test
    public void equals_sameUnitIsEqual() throws Exception {
        Unit unit1 = null;
        try {
            unit1 = UnitTestUtils.createUnit(context, "Unit One", true);
            Unit unit1FromDb = UnitTestUtils.getUnitFromDatabase(context, "Unit One");

            assertTrue(unit1.equals(unit1FromDb));
        } finally {
            UnitTestUtils.deleteUnit(context, unit1);
        }
    }

    @Test
    public void hashCode_UncommitedUnits() {
        Unit unit1 = new Unit();
        assertEquals(0, unit1.hashCode());
    }

    @Test
    public void hashCode_commitedUnitsHaveDifferentHashCodes() throws Exception {
        Unit unit1 = null;
        Unit unit2 = null;
        try {
            unit1 = UnitTestUtils.createUnit(context, "Unit One", true);
            unit2 = UnitTestUtils.createUnit(context, "Unit Two", false);

            assertNotEquals(0, unit1.hashCode());
            assertNotEquals(0, unit2.hashCode());
            assertNotEquals(unit1.hashCode(), unit2.hashCode());
        } finally {
            UnitTestUtils.deleteUnit(context, unit1);
            UnitTestUtils.deleteUnit(context, unit2);
        }
    }
}
