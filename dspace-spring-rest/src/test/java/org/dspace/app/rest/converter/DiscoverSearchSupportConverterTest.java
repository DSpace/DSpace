/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchSupportRest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by raf on 26/09/2017.
 */
public class DiscoverSearchSupportConverterTest {


    DiscoverSearchSupportConverter discoverSearchSupportConverter;

    @Before
    public void setUp() throws Exception{
        discoverSearchSupportConverter = new DiscoverSearchSupportConverter();
    }

    @Test
    public void testReturnIsCorrectClass() throws Exception{
        assertEquals(discoverSearchSupportConverter.convert().getClass(), SearchSupportRest.class);
    }

    @Test
    public void testReturnIsNotNull() throws Exception{
        assertNotNull(discoverSearchSupportConverter.convert());
    }
}
