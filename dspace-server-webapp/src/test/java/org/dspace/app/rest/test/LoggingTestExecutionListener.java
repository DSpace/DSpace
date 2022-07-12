/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import org.apache.logging.log4j.Logger;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Custom DSpace TestExecutionListener which logs messages whenever a specific Test Case (i.e. test method) has
 * started or ended execution. This makes Test environment logs easier to read/understand as you know which method has
 * caused errors, etc.
 */
public class LoggingTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LoggingTestExecutionListener.class);

    /**
     * Before each test method is run
     * @param testContext
     */
    @Override
    public void beforeTestMethod(TestContext testContext) {
        // Log the test method being executed. Put lines around it to make it stand out.
        log.info("---");
        log.info("Starting execution of test method: {}()", testContext.getTestMethod().getName());
        log.info("---");
    }

    /**
     * After each test method is run
     * @param testContext
     */
    @Override
    public void afterTestMethod(TestContext testContext) {
        // Log the test method just completed.
        log.info("Finished execution of test method: {}()", testContext.getTestMethod().getName());
    }
}
