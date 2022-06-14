package org.dspace.app.rest.hdlresolver;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca@4science.com)
 *
 */
public class HdlResolverRestControllerIT extends AbstractControllerIntegrationTest {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HdlResolverRestControllerIT.class);

    // protected HdlResolverService hdlResolverService =
    // HdlResolverServiceFactory.getInstance().getHdlResolverService();

    /**
     * This method will be run before every test as per @Before. It will initialize
     * resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses but no
     * execution order is guaranteed
     */
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        context.restoreAuthSystemState();

    }

    /**
     * Verifies that any null hdlIdentifier returns a
     * <code>HttpStatus.BAD_REQUEST</code>
     * 
     */
    @Test
    public void givenNullHdlIdentifier_thenReturnsBadRequest() {
    }

    /**
     * Verifies that an empty hdlIdentifier returns a
     * <code>HttpStatus.BAD_REQUEST</code>
     * 
     */
    @Test
    public void givenEmptyHdlIdentifier_thenReturnsNull() {
    }

    /**
     * Verifies that any unmapped hdlIdentifier returns a null response
     * 
     */
    @Test
    public void givenHdlIdentifier_whenIdentifierIsNotMapped_thenReturnsNull() {
    }

    /**
     * 
     * Verifies that any mapped hdlIdentifier returns a non-empty array
     * 
     */
    @Test
    public void givenHdlIdentifier_whenIdentifierIsMapped_thenReturnsNonEmptyStringArray() {
    }

    /**
     * Verifies that any mapped hdlIdentifier returns that mapped value
     */
    @Test
    public void givenHdlIdentifier_whenIdentifierIsMapped_thenReturnsMappedURL() {
    }
}
