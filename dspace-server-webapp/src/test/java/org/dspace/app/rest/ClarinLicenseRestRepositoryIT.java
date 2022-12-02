/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.ClarinLicenseConverter;
import org.dspace.app.rest.matcher.ClarinLicenseLabelMatcher;
import org.dspace.app.rest.matcher.ClarinLicenseMatcher;
import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the Clarin License Rest Repository
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ClarinLicenseService clarinLicenseService;

    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;

    @Autowired
    ClarinLicenseConverter clarinLicenseConverter;

    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;
    ClarinLicense firstCLicense;
    ClarinLicense secondCLicense;

    ClarinLicenseLabel firstCLicenseLabel;
    ClarinLicenseLabel secondCLicenseLabel;
    ClarinLicenseLabel thirdCLicenseLabel;

    Item publicItem1;

    Item publicItem2;
    Item publicItem3;

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
        secondCLicenseLabel.setExtended(true);
        secondCLicenseLabel.setTitle("CLL Title2");
        clarinLicenseLabelService.update(context, secondCLicenseLabel);

        thirdCLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        thirdCLicenseLabel.setLabel("DBC");
        thirdCLicenseLabel.setExtended(false);
        thirdCLicenseLabel.setTitle("CLL Title3");
        clarinLicenseLabelService.update(context, thirdCLicenseLabel);

        // create ClarinLicenses
        firstCLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        firstCLicense.setName("CL Name1");
        firstCLicense.setConfirmation(0);
        firstCLicense.setDefinition("CL Definition1");
        firstCLicense.setRequiredInfo("CL Req1");
        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> firstClarinLicenseLabels = new HashSet<>();
        firstClarinLicenseLabels.add(firstCLicenseLabel);
//        firstClarinLicenseLabels.add(secondCLicenseLabel);
        firstClarinLicenseLabels.add(thirdCLicenseLabel);
        firstCLicense.setLicenseLabels(firstClarinLicenseLabels);
        clarinLicenseService.update(context, firstCLicense);

        secondCLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        secondCLicense.setName("CL Name2");
        secondCLicense.setConfirmation(1);
        secondCLicense.setDefinition("CL Definition2");
        secondCLicense.setRequiredInfo("CL Req2");
        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> secondClarinLicenseLabels = new HashSet<>();
        secondClarinLicenseLabels.add(thirdCLicenseLabel);
        secondClarinLicenseLabels.add(firstCLicenseLabel);
        secondCLicense.setLicenseLabels(secondClarinLicenseLabels);
        clarinLicenseService.update(context, secondCLicense);

        //create collection for items
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection2").build();
        Collection col3 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection3").build();

        // create two items with the first license
        // the publicItem1 has license information added to the metadata
        publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2022-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .withMetadata("dc", "rights", null, firstCLicense.getName())
                .withMetadata("dc", "rights", "uri", firstCLicense.getDefinition())
                .withMetadata("dc", "rights", "label",
                        Objects.requireNonNull(firstCLicense.getNonExtendedClarinLicenseLabel()).getLabel())
                .build();

        publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        // create item with the second license
        publicItem3 = ItemBuilder.createItem(context, col3)
                .withTitle("Public item 3")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

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
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/core/clarinlicenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.clarinlicenses", Matchers.hasItem(
                        ClarinLicenseMatcher.matchClarinLicense(firstCLicense))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses", Matchers.hasItem(
                        ClarinLicenseMatcher.matchClarinLicense(secondCLicense))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses[0].clarinLicenseLabel", Matchers.is(
                        ClarinLicenseLabelMatcher.matchClarinLicenseLabel(
                                Objects.requireNonNull(getNonExtendedLicenseLabel(firstCLicense.getLicenseLabels()))))
                ))
                .andExpect(jsonPath("$._embedded.clarinlicenses[0].extendedClarinLicenseLabels",
                        Matchers.hasItem(
                                ClarinLicenseLabelMatcher.matchClarinLicenseLabel(
                                        Objects.requireNonNull(getExtendedLicenseLabels(
                                                firstCLicense.getLicenseLabels())))
                                )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/core/clarinlicenses")));
    }

    @Test
    public void create() throws Exception {
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
            getClient(authTokenAdmin).perform(post("/api/core/clarinlicenses")
                            .content(new ObjectMapper().writeValueAsBytes(clarinLicenseRest))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
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
        } finally {
            if (Objects.nonNull(idRef.get())) {
                // remove created clarin license
                ClarinLicenseBuilder.deleteClarinLicense(idRef.get());
            }
        }
    }

    // Edit
    @Test
    public void update() throws Exception {
        context.turnOffAuthorisationSystem();
        // clarin license to update
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        clarinLicense.setName("default name");
        clarinLicense.setDefinition("default definition");
        clarinLicense.setConfirmation(0);
        clarinLicense.setRequiredInfo("default info");

        Set<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        clarinLicenseLabels.add(firstCLicenseLabel);
        clarinLicenseLabels.add(secondCLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);

        // clarin license with updated values
        ClarinLicense clarinLicenseUpdated = ClarinLicenseBuilder.createClarinLicense(context).build();
        clarinLicenseUpdated.setName("updated name");
        clarinLicenseUpdated.setDefinition("updated definition");
        clarinLicenseUpdated.setConfirmation(4);
        clarinLicenseUpdated.setRequiredInfo("updated info");

        Set<ClarinLicenseLabel> clarinLicenseLabelUpdated = new HashSet<>();
        clarinLicenseLabelUpdated.add(firstCLicenseLabel);
        clarinLicenseLabelUpdated.add(thirdCLicenseLabel);
        clarinLicenseUpdated.setLicenseLabels(clarinLicenseLabelUpdated);
        context.restoreAuthSystemState();

        ClarinLicenseRest clarinLicenseRest = clarinLicenseConverter.convert(clarinLicenseUpdated, Projection.DEFAULT);

        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk());

        getClient(authTokenAdmin).perform(put("/api/core/clarinlicenses/" + clarinLicense.getID())
                        .content(new ObjectMapper().writeValueAsBytes(clarinLicenseRest))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        getClient(authTokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        ClarinLicenseMatcher.matchClarinLicenseWithoutId(clarinLicenseUpdated))
                ));
    }

    // 403
    @Test
    public void forbiddenUpdateClarinLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        // clarin license to update
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();

        clarinLicense.setName("default name");
        clarinLicense.setDefinition("default definition");
        clarinLicense.setConfirmation(0);
        clarinLicense.setRequiredInfo("default info");

        Set<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        clarinLicenseLabels.add(firstCLicenseLabel);
        clarinLicenseLabels.add(thirdCLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);
        context.restoreAuthSystemState();

        ClarinLicenseRest clarinLicenseRest = clarinLicenseConverter.convert(clarinLicense, Projection.DEFAULT);
        String authTokenUser = getAuthToken(eperson.getEmail(), password);
        getClient(authTokenUser).perform(delete("/api/core/clarinlicenses/" + clarinLicense.getID())
                .content(new ObjectMapper().writeValueAsBytes(clarinLicenseRest))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
        ;
    }

    // 404
    @Test
    public void notFoundUpdateClarinLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        // clarin license to update
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();

        clarinLicense.setName("default name");
        clarinLicense.setDefinition("default definition");
        clarinLicense.setConfirmation(0);
        clarinLicense.setRequiredInfo("default info");

        Set<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        clarinLicenseLabels.add(firstCLicenseLabel);
        clarinLicenseLabels.add(thirdCLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);
        context.restoreAuthSystemState();

        ClarinLicenseRest clarinLicenseRest = clarinLicenseConverter.convert(clarinLicense, Projection.DEFAULT);

        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(put("/api/core/clarinlicenses/" + clarinLicense.getID() + "124679")
                .content(new ObjectMapper().writeValueAsBytes(clarinLicenseRest))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
        ;
    }

    // 204
    @Test
    public void deleteClarinLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        context.restoreAuthSystemState();

        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk());

        getClient(authTokenAdmin).perform(delete("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isNoContent());

        getClient(authTokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isNotFound());
    }

    // 401
    @Test
    public void unauthorizedDeleteClarinLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isUnauthorized())
        ;
    }

    // 403
    @Test
    public void forbiddenDeleteClarinLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        context.restoreAuthSystemState();

        String authTokenUser = getAuthToken(eperson.getEmail(), password);
        getClient(authTokenUser).perform(delete("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isForbidden())
        ;
    }

    // 404
    @Test
    public void notFoundDeleteClarinLicense() throws Exception {
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(delete("/api/core/clarinlicenses/" + 1239990))
                .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void findAllBitstreamsAttachedToLicense() throws Exception {
        context.turnOffAuthorisationSystem();
        // create bitstreams and add them with licenses to the clarin license resource mapping
        BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test 1", UTF_8))
                .withFormat("test format")
                .build();

        BitstreamBuilder.createBitstream(context, publicItem1, toInputStream("test 2", UTF_8))
                .withFormat("test format")
                .build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // Check if the Clarin License was attached to the Bitstreams
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + firstCLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(2)));
    }

    private ClarinLicenseLabel getNonExtendedLicenseLabel(List<ClarinLicenseLabel> clarinLicenseLabelList) {
        for (ClarinLicenseLabel clarinLicenseLabel : clarinLicenseLabelList) {
            if (clarinLicenseLabel.isExtended()) {
                continue;
            }
            return clarinLicenseLabel;
        }
        return null;
    }

    private ClarinLicenseLabel getExtendedLicenseLabels(List<ClarinLicenseLabel> clarinLicenseLabelList) {
        for (ClarinLicenseLabel clarinLicenseLabel : clarinLicenseLabelList) {
            if (!clarinLicenseLabel.isExtended()) {
                continue;
            }
            return clarinLicenseLabel;
        }
        return null;
    }


}
