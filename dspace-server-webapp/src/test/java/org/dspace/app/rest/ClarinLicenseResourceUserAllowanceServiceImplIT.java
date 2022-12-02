/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClarinLicenseResourceUserAllowanceBuilder;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ClarinLicenseResourceUserAllowanceServiceImplIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;

    @Test
    public void testFind() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance =
                ClarinLicenseResourceUserAllowanceBuilder.
                        createClarinLicenseResourceUserAllowance(context).build();
        context.restoreAuthSystemState();
        Assert.assertEquals(clarinLicenseResourceUserAllowance,
                clarinLicenseResourceUserAllowanceService
                .find(context, clarinLicenseResourceUserAllowance.getID()));
    }
}

