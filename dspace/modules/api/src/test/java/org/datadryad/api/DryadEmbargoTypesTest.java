/*
 */
package org.datadryad.api;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadEmbargoTypesTest {
    private static Logger log = Logger.getLogger(DryadEmbargoTypesTest.class);

    /**
     * Test of validate method, of class DryadEmbargoTypes.
     */
    @Test
    public void testValidate() {
        log.info("validate");
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
