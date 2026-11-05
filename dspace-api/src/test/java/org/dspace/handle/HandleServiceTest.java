/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dspace.AbstractUnitTest;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

public class HandleServiceTest extends AbstractUnitTest {
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ConfigurationService configurationService = new DSpace().getConfigurationService();

    @Before
    @Override
    public void init() {
        super.init();

        configurationService.setProperty("handle.prefix", "123456789");
        configurationService.setProperty("handle.canonical.prefix", "https://fake.canonical.prefix");
        configurationService.setProperty("handle.additional.prefixes", "987654321, 654321987");
    }

    @Test
    public void testParseHandleInvalid() {
        assertNull(handleService.parseHandle(null));
        assertNull(handleService.parseHandle("123456789"));
        assertNull(handleService.parseHandle("/123456789"));
        assertNull(handleService.parseHandle("https://duraspace.org/dspace/"));
        assertNull(handleService.parseHandle("10.70131/test_doi_5d2be995d35b6"));
        assertNull(handleService.parseHandle("not a handle"));
    }

    @Test
    public void testParseHandleByPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("123456789/111", handleService.parseHandle("123456789/111"));
    }

    @Test
    public void testParseHandleByCanonicalPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("111222333/111", handleService.parseHandle("https://fake.canonical.prefix/111222333/111"));
    }

    /**
     * A URL that can never yield a handle has to be rejected promptly. The greedy ".+" in the
     * replaced pattern retried every "/handle/" segment in turn, needing time quadratic in the
     * length of the identifier, where rejecting it is now linear. The bound is therefore a very
     * wide margin and is not sensitive to a slow CI machine.
     */
    @Test(timeout = 10000)
    public void testParseHandleRejectsLongUrlWithoutBacktracking() {
        // a line terminator is what made the URL unmatchable, since "." never matched one
        assertNull(handleService.parseHandle("http://a/handle/" + "a/handle/a".repeat(40000) + "\n"));
    }

    @Test
    public void testParseHandleByAdditionalPrefix() {
        // note: handle pattern after prefix is not checked
        assertEquals("987654321/111", handleService.parseHandle("987654321/111"));
        assertEquals("654321987/111", handleService.parseHandle("654321987/111"));
    }

    @Test
    public void testParseHandleByPattern() {
        assertEquals("111222333/111", handleService.parseHandle("hdl:111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("info:hdl/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("https://hdl.handle.net/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("http://hdl.handle.net/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("https://whatever/handle/111222333/111"));
        assertEquals("111222333/111", handleService.parseHandle("http://whatever/handle/111222333/111"));
        // a repository deployed under a context path
        assertEquals("111222333/111", handleService.parseHandle("https://whatever/jspui/handle/111222333/111"));
        // the replaced pattern was greedy, so a nested "/handle/" resolved to the last one
        assertEquals("111222333/111",
                     handleService.parseHandle("https://whatever/handle/x/handle/111222333/111"));
    }
}
