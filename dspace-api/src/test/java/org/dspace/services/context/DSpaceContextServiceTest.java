/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.context;

import static org.junit.Assert.*;

import org.dspace.core.Context;
import org.dspace.services.ContextService;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceContextServiceTest extends DSpaceAbstractKernelTest {
	private ContextService contextService;
	
	@Before
	public void init () {
		contextService = getService(ContextService.class);
	}
	
	@After
    public void tearDown() {
		contextService = null;
	}
	
	@Test
	public void testContextService () {
		assertNotNull(contextService);
		Context ctx = contextService.getContext();
		assertNotNull(ctx);
	}
}
