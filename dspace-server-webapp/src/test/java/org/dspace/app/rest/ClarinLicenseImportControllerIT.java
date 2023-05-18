/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.ClarinLicenseConverter;
import org.dspace.app.rest.converter.ClarinLicenseLabelConverter;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Tests for import license controller.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinLicenseImportControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ClarinLicenseLabelService clarinLicenseLabelService;

    @Autowired
    private ClarinLicenseService clarinLicenseService;

    @Autowired
    private ClarinLicenseConverter clarinLicenseConverter;

    @Autowired
    private ClarinLicenseLabelConverter clarinLicenseLabelConverter;

    ClarinLicenseLabel firstCLicenseLabel;
    ClarinLicenseLabel secondCLicenseLabel;
    ClarinLicense firstCLicense;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        // create LicenseLabels
        firstCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        firstCLicenseLabel.setLabel("CC");
        firstCLicenseLabel.setExtended(true);
        firstCLicenseLabel.setTitle("CLL Title1");
        clarinLicenseLabelService.update(context, firstCLicenseLabel);

        secondCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        secondCLicenseLabel.setLabel("CCC");
        secondCLicenseLabel.setExtended(false);
        secondCLicenseLabel.setTitle("CLL Title2");
        clarinLicenseLabelService.update(context, secondCLicenseLabel);

        // create ClarinLicenses
        firstCLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        firstCLicense.setName("CL Name1");
        firstCLicense.setConfirmation(0);
        firstCLicense.setDefinition("CL Definition1");
        firstCLicense.setRequiredInfo("CL Req1");
        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> firstClarinLicenseLabels = new HashSet<>();
        firstClarinLicenseLabels.add(firstCLicenseLabel);
        firstClarinLicenseLabels.add(secondCLicenseLabel);
        firstCLicense.setLicenseLabels(firstClarinLicenseLabels);
        clarinLicenseService.update(context, firstCLicense);

        context.restoreAuthSystemState();

    }

    @Test
    public void importLicenseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com")
                .build();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder.createClarinUserRegistration(context)
                .withEPersonID(ePerson.getID())
                .build();
        context.restoreAuthSystemState();

        ClarinLicenseRest clarinLicenseRest = new ClarinLicenseRest();
        clarinLicenseRest.setName("name");
        clarinLicenseRest.setBitstreams(0);
        clarinLicenseRest.setConfirmation(4);
        clarinLicenseRest.setRequiredInfo("Not required");
        clarinLicenseRest.setDefinition("definition");
        clarinLicenseConverter.setExtendedClarinLicenseLabels(clarinLicenseRest, firstCLicense.getLicenseLabels(),
                Projection.DEFAULT);
        clarinLicenseConverter.setClarinLicenseLabel(clarinLicenseRest, firstCLicense.getLicenseLabels(),
                Projection.DEFAULT);

        // id of created clarin license
        AtomicReference<Integer> idRef = new AtomicReference<>();
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        try {
            getClient(authTokenAdmin).perform(post("/api/clarin/import/license")
                            .content(new ObjectMapper().writeValueAsBytes(clarinLicenseRest))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .param("eperson", ePerson.getID().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(clarinLicenseRest.getName())))
                    .andExpect(jsonPath("$.definition",
                            is(clarinLicenseRest.getDefinition())))
                    .andExpect(jsonPath("$.confirmation",
                            is(clarinLicenseRest.getConfirmation())))
                    .andExpect(jsonPath("$.requiredInfo",
                            is(clarinLicenseRest.getRequiredInfo())))
                    .andExpect(jsonPath("$.bitstreams",
                            is(clarinLicenseRest.getBitstreams())))
                    .andExpect(jsonPath("$.type",
                            is(ClarinLicenseRest.NAME)))

                    .andExpect(jsonPath("$.clarinLicenseLabel.label",
                            is(clarinLicenseRest.getClarinLicenseLabel().getLabel())))
                    .andExpect(jsonPath("$.clarinLicenseLabel.title",
                            is(clarinLicenseRest.getClarinLicenseLabel().getTitle())))
                    .andExpect(jsonPath("$.clarinLicenseLabel.extended",
                            is(clarinLicenseRest.getClarinLicenseLabel().isExtended())))
                    .andExpect(jsonPath("$.clarinLicenseLabel.type",
                            is(ClarinLicenseLabelRest.NAME)))

                    .andExpect(jsonPath("$.extendedClarinLicenseLabels[0].label",
                            is(clarinLicenseRest.getExtendedClarinLicenseLabels().get(0).getLabel())))
                    .andExpect(jsonPath("$.extendedClarinLicenseLabels[0].title",
                            is(clarinLicenseRest.getExtendedClarinLicenseLabels().get(0).getTitle())))
                    .andExpect(jsonPath("$.extendedClarinLicenseLabels[0].extended",
                            is(clarinLicenseRest.getExtendedClarinLicenseLabels().get(0).isExtended())))
                    .andExpect(jsonPath("$.extendedClarinLicenseLabels[0].type",
                            is(ClarinLicenseLabelRest.NAME)))
                    .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(),
                            "$.id")));

            //control of user registration
            ClarinLicense license = clarinLicenseService.find(context, idRef.get());
            assertEquals(license.getEperson().getID(), userRegistration.getID());
        } finally {
            if (Objects.nonNull(idRef.get())) {
                // remove created clarin license
                ClarinLicenseBuilder.deleteClarinLicense(idRef.get());
            }
        }
    }
}