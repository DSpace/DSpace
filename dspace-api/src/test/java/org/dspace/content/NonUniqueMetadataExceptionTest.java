/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import static org.junit.Assert.*;

import org.junit.*;

/**
 * Unit Tests for class NonUniqueMetadataException. Being an exception
 * no tests have to be done, the class is created for coberture purposes
 * @author pvillega
 */
public class NonUniqueMetadataExceptionTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(NonUniqueMetadataExceptionTest.class);

    /**
     * Dummy test to avoid initialization errors
     */
    @Test
    public void testDummy()
    {
        assertTrue(true);
    }

}