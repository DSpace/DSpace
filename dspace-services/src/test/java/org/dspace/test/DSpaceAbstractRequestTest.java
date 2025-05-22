/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


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

    @BeforeAll
    public static void initRequestService() {
        _initializeRequestService();
    }

    @BeforeEach
    public void startRequest() {
        _startRequest();
    }

    @AfterEach
    public void endRequest() {
        _endRequest();
    }

    @AfterAll
    public static void cleanupRequestService() {
        _destroyRequestService();
    }

}
