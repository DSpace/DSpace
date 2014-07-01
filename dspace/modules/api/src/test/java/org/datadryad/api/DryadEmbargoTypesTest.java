/*
 */
package org.datadryad.api;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadEmbargoTypesTest {

    /**
     * Test of validate method, of class DryadEmbargoTypes.
     */
    @Test
    public void testValidate() {
        String type = "oneyear";
        Boolean result = DryadEmbargoTypes.validate(type);
        assertTrue(result);

        type = "custom";
        result = DryadEmbargoTypes.validate(type);
        assertTrue(result);

        type = "untilArticleAppears";
        result = DryadEmbargoTypes.validate(type);
        assertTrue(result);

        type = "some other string";
        result = DryadEmbargoTypes.validate(type);
        assertFalse(result);

        type = null;
        result = DryadEmbargoTypes.validate(type);
        assertFalse(result);
    }
}
