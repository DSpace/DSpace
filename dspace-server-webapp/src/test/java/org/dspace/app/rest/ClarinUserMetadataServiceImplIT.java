/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClarinUserMetadataBuilder;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ClarinUserMetadataServiceImplIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;

    @Test
    public void testFind() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserMetadata clarinUserMetadata = ClarinUserMetadataBuilder
                .createClarinUserMetadata(context).build();
        context.restoreAuthSystemState();
        Assert.assertEquals(clarinUserMetadata, clarinUserMetadataService
                .find(context, clarinUserMetadata.getID()));
    }
}
