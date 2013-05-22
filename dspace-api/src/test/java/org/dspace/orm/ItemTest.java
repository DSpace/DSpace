/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm;

import static org.junit.Assert.*;

import org.dspace.orm.dao.api.IItemDao;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class ItemTest extends DSpaceAbstractKernelTest {
	private IItemDao itemDao;
	
	@Before
	public void init () {
		itemDao = getService(IItemDao.class);
	}
	
	@After
    public void tearDown() {
		itemDao = null;
	}
	
	@Test
	public void doTest () {
		assertNotNull(itemDao);
	}
}
