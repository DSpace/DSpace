package org.dspace.app.rest;

import org.dspace.app.rest.matcher.ClarinLicenseLabelMatcher;
import org.dspace.app.rest.matcher.ClarinLicenseMatcher;
import org.dspace.app.rest.matcher.MetadataValueMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClarinLicenseRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;

    ClarinLicense firstCLicense;
    ClarinLicense secondCLicense;

    ClarinLicenseLabel firstCLicenseLabel;
    ClarinLicenseLabel secondCLicenseLabel;
    ClarinLicenseLabel thirdCLicenseLabel;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        // create LicenseLabels
        firstCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        firstCLicenseLabel.setDefinition("CC");
        firstCLicenseLabel.setExtended(false);
        firstCLicenseLabel.setTitle("CLL Title1");
        clarinLicenseLabelService.update(context, firstCLicenseLabel);

        secondCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        secondCLicenseLabel.setDefinition("CCC");
        secondCLicenseLabel.setExtended(true);
        secondCLicenseLabel.setTitle("CLL Title2");
        clarinLicenseLabelService.update(context, secondCLicenseLabel);

        thirdCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        thirdCLicenseLabel.setDefinition("DBC");
        thirdCLicenseLabel.setExtended(false);
        thirdCLicenseLabel.setTitle("CLL Title3");
        clarinLicenseLabelService.update(context, thirdCLicenseLabel);

        // create ClarinLicenses
        firstCLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        firstCLicense.setConfirmation(0);
        firstCLicense.setDefinition("CL Definition1");
        firstCLicense.setRequiredInfo("CL Req1");
        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> firstClarinLicenseLabels = new HashSet<>();
        firstClarinLicenseLabels.add(firstCLicenseLabel);
        firstClarinLicenseLabels.add(secondCLicenseLabel);
        firstCLicense.setLicenseLabels(firstClarinLicenseLabels);
        clarinLicenseService.update(context, firstCLicense);

        secondCLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        secondCLicense.setConfirmation(1);
        secondCLicense.setDefinition("CL Definition2");
        secondCLicense.setRequiredInfo("CL Req2");
        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> secondClarinLicenseLabels = new HashSet<>();
        secondClarinLicenseLabels.add(thirdCLicenseLabel);
        secondCLicense.setLicenseLabels(secondClarinLicenseLabels);
        clarinLicenseService.update(context, secondCLicense);

        context.restoreAuthSystemState();
    }

    @Test
    public void clarinLicensesAndLicenseLabelsAreInitialized() throws Exception {
        Assert.assertNotNull(firstCLicense);
        Assert.assertNotNull(secondCLicense);
        Assert.assertNotNull(firstCLicenseLabel);
        Assert.assertNotNull(secondCLicenseLabel);
        Assert.assertNotNull(thirdCLicenseLabel);
        Assert.assertNotNull(firstCLicense.getLicenseLabels());
        Assert.assertNotNull(secondCLicense.getLicenseLabels());
    }

    @Test
    public void findAll() throws Exception {
        firstCLicense.setDefinition("wrong");
        getClient().perform(get("/api/core/clarinlicenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.clarinlicenses", Matchers.hasItem(
                        ClarinLicenseMatcher.matchClarinLicense(firstCLicense))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses", Matchers.hasItem(
                        ClarinLicenseMatcher.matchClarinLicense(secondCLicense))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses[0]._embedded.clarinLicenseLabels", Matchers.hasItem(
                        ClarinLicenseLabelMatcher.matchClarinLicenseLabel(firstCLicense.getLicenseLabels().get(0)))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses[0]._embedded.clarinLicenseLabels", Matchers.hasItem(
                        ClarinLicenseLabelMatcher.matchClarinLicenseLabel(firstCLicense.getLicenseLabels().get(1)))
                ))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/clarinlicenses")))
        ;
    }


}
