/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * This is an abstract class which makes it easier to test things that use the DSpace Kernel
 * and includes an automatic request wrapper around every test method which will start and
 * end a request, the default behavior is to end the request with a failure which causes
 * a rollback and reverts the storage to the previous values
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public abstract class DSpaceAbstractRequestTest extends DSpaceAbstractKernelTest {

    /**
     * @return the current request ID for the current running request
     */
    public String getRequestId() {
        return requestId;
    }

    @BeforeClass
    public static void initRequestService() {
        _initializeRequestService();
    }

    @Before
    public void startRequest() {
        _startRequest();
    }

    @After
    public void endRequest() {
        _endRequest();
    }

    @AfterClass
    public static void cleanupRequestService() {
        _destroyRequestService();
    }

}
