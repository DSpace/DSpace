/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.dspace.AbstractUnitTest;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.junit.Test;

/**
 * Unit Tests for class MetadataFieldTest
 *
 * @author milanmajchrak
 */
public class LocalMetadataTest extends AbstractUnitTest {

    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    /**
     * Test of existing custom metadata field `local.approximateDate.issued`
     */
    @Test
    public void existApproximateData() throws Exception {
        MetadataField field = metadataFieldService.findByString(context, "local.approximateDate.issued",
                '.');

        assertThat("existApproximateData 0", field, notNullValue());
    }

}
