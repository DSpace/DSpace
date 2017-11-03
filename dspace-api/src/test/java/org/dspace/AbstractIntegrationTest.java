/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import org.databene.contiperf.junit.ContiPerfRule;
import org.databene.contiperf.junit.ContiPerfRuleExt;
import org.junit.Ignore;
import org.junit.Rule;

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
public class AbstractIntegrationTest extends AbstractUnitTest
{

    //We only enable contiperf in the integration tests, as it doesn't
    //seem so useful to run them in isolated unit tests
    @Rule
    public ContiPerfRule contiperfRules = new ContiPerfRuleExt();
}
