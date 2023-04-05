package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EtdUnitTest extends AbstractUnitTest {

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
    public void equals_UncommitedEtdUnitsNotEqual() {
        EtdUnit unit1 = new EtdUnit();
        EtdUnit unit2 = new EtdUnit();
        assertFalse(unit1.equals(unit2));
    }

    @Test
    public void equals_DifferentEtdUnitsNotEqual() throws Exception {
        EtdUnit unit1 = null;
        EtdUnit unit2 = null;
        try {
            unit1 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit One", true);
            unit2 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit Two", false);

            assertFalse(unit1.equals(unit2));
        } finally {
            EtdUnitTestUtils.deleteEtdUnit(context, unit1);
            EtdUnitTestUtils.deleteEtdUnit(context, unit2);
        }
    }

    @Test
    public void equals_sameEtdUnitIsEqual() throws Exception {
        EtdUnit unit1 = null;
        try {
            unit1 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit One", true);
            EtdUnit unit1FromDb = EtdUnitTestUtils.getEtdUnitFromDatabase(context, "EtdUnit One");

            assertTrue(unit1.equals(unit1FromDb));
        } finally {
            EtdUnitTestUtils.deleteEtdUnit(context, unit1);
        }
    }

    @Test
    public void hashCode_UncommitedEtdUnits() {
        EtdUnit unit1 = new EtdUnit();
        assertEquals(0, unit1.hashCode());
    }

    @Test
    public void hashCode_commitedEtdUnitsHaveDifferentHashCodes() throws Exception {
        EtdUnit unit1 = null;
        EtdUnit unit2 = null;
        try {
            unit1 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit One", true);
            unit2 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit Two", false);

            assertNotEquals(0, unit1.hashCode());
            assertNotEquals(0, unit2.hashCode());
            assertNotEquals(unit1.hashCode(), unit2.hashCode());
        } finally {
            EtdUnitTestUtils.deleteEtdUnit(context, unit1);
            EtdUnitTestUtils.deleteEtdUnit(context, unit2);
        }
    }
}
