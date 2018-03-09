/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import org.junit.Ignore;

/**
 * This is the base class for Integration Tests. It inherits from the class
 * AbstractUnitTest the structure (database, file system) required by DSpace to
 * run tests.
 *
 * It also contains some generic mocks and utilities that are needed by the
 * integration tests developed for DSpace
 *
 * @author pvillega
 */
@Ignore
public class AbstractIntegrationTest extends AbstractUnitTest {
    // This class intentionally left blank.
}
