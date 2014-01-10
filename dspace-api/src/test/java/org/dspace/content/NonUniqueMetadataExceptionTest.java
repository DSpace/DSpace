/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.AbstractUnitTest;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * Unit Tests for class NonUniqueMetadataException. Being an exception
 * no tests have to be done, the class is created for coberture purposes
 * @author pvillega
 */
public class NonUniqueMetadataExceptionTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(NonUniqueMetadataExceptionTest.class);

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Dummy test to avoid initialization errors
     */
    @Test
    public void testDummy()
    {
        assertTrue(true);
    }

}