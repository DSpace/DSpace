/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for interface InProgressSubmission. As it is an interface
 * (no implementation) no tests are added. This class is created for
 * coberture purposes and in case we want to use this class as parent
 * of the unit tests related to this interface
 * @author pvillega
 */
public class InProgressSubmissionTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(InProgressSubmissionTest.class);

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
     * Test of getID method, of class InProgressSubmission.
     */
    @Test
    public void testGetID()
    {
        
    }

    /**
     * Test of deleteWrapper method, of class InProgressSubmission.
     */
    @Test
    public void testDeleteWrapper() throws Exception
    {

    }

    /**
     * Test of update method, of class InProgressSubmission.
     */
    @Test
    public void testUpdate() throws Exception
    {

    }

    /**
     * Test of getItem method, of class InProgressSubmission.
     */
    @Test
    public void testGetItem()
    {

    }

    /**
     * Test of getCollection method, of class InProgressSubmission.
     */
    @Test
    public void testGetCollection() 
    {

    }

    /**
     * Test of getSubmitter method, of class InProgressSubmission.
     */
    @Test
    public void testGetSubmitter() throws Exception
    {

    }

    /**
     * Test of hasMultipleFiles method, of class InProgressSubmission.
     */
    @Test
    public void testHasMultipleFiles()
    {

    }

    /**
     * Test of setMultipleFiles method, of class InProgressSubmission.
     */
    @Test
    public void testSetMultipleFiles()
    {

    }

    /**
     * Test of hasMultipleTitles method, of class InProgressSubmission.
     */
    @Test
    public void testHasMultipleTitles()
    {

    }

    /**
     * Test of setMultipleTitles method, of class InProgressSubmission.
     */
    @Test
    public void testSetMultipleTitles()
    {

    }

    /**
     * Test of isPublishedBefore method, of class InProgressSubmission.
     */
    @Test
    public void testIsPublishedBefore()
    {

    }

    /**
     * Test of setPublishedBefore method, of class InProgressSubmission.
     */
    @Test
    public void testSetPublishedBefore()
    {

    }

}