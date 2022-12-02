/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.ClarinLicenseRestRepository.OPERATION_PATH_LICENSE_RESOURCE;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.apache.http.entity.ContentType;
import org.dspace.authorize.DownloadTokenExpiredException;
import org.dspace.authorize.MissingLicenseAgreementException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClarinLicenseBuilder;
import org.dspace.builder.ClarinLicenseLabelBuilder;
import org.dspace.builder.ClarinLicenseResourceUserAllowanceBuilder;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class AuthorizationRestControllerIT extends AbstractControllerIntegrationTest {

    private static final String CLARIN_LICENSE_NAME = "Test Clarin License";
    private static final String TEXT_PLAIN_UTF_8 = "text/plain;charset=UTF-8";

    @Autowired
    ClarinLicenseService clarinLicenseService;
    @Autowired
    ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    Item item;
    WorkspaceItem witem;
    ClarinLicense clarinLicense;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .build();

        context.setCurrentUser(eperson);
        InputStream pdf = getClass().getResourceAsStream("simple-article.pdf");

        witem = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
                .withTitle("Test WorkspaceItem")
                .withIssueDate("2017-10-17")
                .withFulltext("simple-article.pdf", "/local/path/simple-article.pdf", pdf)
                .build();

        item = witem.getItem();

        // Create clarin license with clarin license label
        clarinLicense = createClarinLicense(CLARIN_LICENSE_NAME, "Test Def", "Test R Info", 1);
        context.restoreAuthSystemState();
    }

    // Submitter should be authorized to download th bitstream, 200
    @Test
    public void shouldAuthorizeUserAsSubmitter() throws Exception {
        String authTokenAdmin = getAuthToken(eperson.getEmail(), password);

        // Load bitstream from the item.
        Bitstream bitstream = item.getBundles().get(0).getBitstreams().get(0);
        getClient(authTokenAdmin).perform(get("/api/authrn/" + bitstream.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN_UTF_8))
                .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$.errorName")));
    }

    // DownloadTokenExpiredException, 401
    @Test
    public void shouldNotAuthorizeUserByWrongToken() throws Exception {
        // Admin is not the submitter.
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);

        // Load bitstream from the item.
        Bitstream bitstream = item.getBundles().get(0).getBitstreams().get(0);
        getClient(authTokenAdmin).perform(get("/api/authrn/" +
                        bitstream.getID().toString() + "?dtoken=wrongToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(is(Matchers.is(DownloadTokenExpiredException.NAME))));
    }

    // Download by token, 200
    @Test
    public void shouldAuthorizeUserByCorrectToken() throws Exception {
        // Prepare environment
        attachLicenseToBitstream();

        // Get clarin license resource mapping which was created by attaching license to Bitstream
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingService.findAllByLicenseId(context, clarinLicense.getID());

        // Token for authorization to download the bitstream
        String token = "amazingToken";

        context.turnOffAuthorisationSystem();
        // Create the ClarinLicenseResourceUserAllowance with token then the download with token should work
        ClarinLicenseResourceUserAllowanceBuilder.createClarinLicenseResourceUserAllowance(context)
                .withToken(token)
                .withCreatedOn(new Date())
                .withMapping(clarinLicenseResourceMappings.get(0))
                .build();
        context.restoreAuthSystemState();

        Bitstream bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        // Admin is not the submitter
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        // The admin should be authorized to download the bitstream with token
        getClient(authTokenAdmin).perform(get("/api/authrn/" +
                        bitstream.getID().toString() + "?dtoken=" + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN_UTF_8))
                .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$.errorName")));

    }

    // Token is expired, 401
    @Test
    public void shouldNotAuthorizeByExpiredToken() throws Exception {
        // Prepare environment
        attachLicenseToBitstream();

        // Get clarin license resource mapping which was created by attaching license to Bitstream
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingService.findAllByLicenseId(context, clarinLicense.getID());

        // Token for authorization to download the bitstream
        String token = "amazingToken";

        // Create date the user won't be authorized with because of expired token
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -45);
        Date expiredTokenDate = cal.getTime();

        context.turnOffAuthorisationSystem();
        // Create the ClarinLicenseResourceUserAllowance with token then the download with token should work
        ClarinLicenseResourceUserAllowanceBuilder.createClarinLicenseResourceUserAllowance(context)
                .withToken(token)
                .withCreatedOn(expiredTokenDate)
                .withMapping(clarinLicenseResourceMappings.get(0))
                .build();
        context.restoreAuthSystemState();

        Bitstream bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        // Admin is not the submitter
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        // The admin should be authorized to download the bitstream with token
        getClient(authTokenAdmin).perform(get("/api/authrn/" +
                        bitstream.getID().toString() + "?dtoken=" + token))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(is(Matchers.is(DownloadTokenExpiredException.NAME))));
    }

    // User metadata are filled in, 200
    @Test
    public void shouldAuthorizeWhenUserMetadataAreFilledIn() throws Exception {
        context.turnOffAuthorisationSystem();
        // Prepare environment
        attachLicenseToBitstream();

        ClarinUserRegistration clarinUserRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context)
                .withEPersonID(admin.getID())
                .build();

        // Get clarin license resource mapping which was created by attaching license to Bitstream
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingService.findAllByLicenseId(context, clarinLicense.getID());

        // Create the ClarinLicenseResourceUserAllowance with User Registration data that means
        // the user has filled in any information.
        ClarinLicenseResourceUserAllowanceBuilder.createClarinLicenseResourceUserAllowance(context)
                .withMapping(clarinLicenseResourceMappings.get(0))
                .withUser(clarinUserRegistration)
                .build();
        context.restoreAuthSystemState();

        Bitstream bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        // Admin is not the submitter
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/authrn/" + bitstream.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TEXT_PLAIN_UTF_8))
                .andExpect(jsonPath("$", JsonPathMatchers.hasNoJsonPath("$.errorName")));
    }

    // User metadata are NOT filled in, MissingLicenseAgreementException
    @Test
    public void shouldNotAuthorizeWhenUserMetadataAreNotFilledIn() throws Exception {
        // Prepare environment
        attachLicenseToBitstream();

        // Get clarin license resource mapping which was created by attaching license to Bitstream
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingService.findAllByLicenseId(context, clarinLicense.getID());

        context.turnOffAuthorisationSystem();
        // Create the ClarinLicenseResourceUserAllowance without User Registration data that means
        // the user hasn't filled in any information.
        ClarinLicenseResourceUserAllowanceBuilder.createClarinLicenseResourceUserAllowance(context)
                .withMapping(clarinLicenseResourceMappings.get(0))
                .build();
        context.restoreAuthSystemState();

        Bitstream bitstream = witem.getItem().getBundles().get(0).getBitstreams().get(0);
        // Admin is not the submitter
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/authrn/" + bitstream.getID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(is(Matchers.is(MissingLicenseAgreementException.NAME))));
    }

    // 400
    @Test
    public void shouldReturnNotFoundExceptionWhenIdIsNull() throws Exception {
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/authrn"))
                .andExpect(status().isNotFound());
    }

    // 404
    @Test
    public void shouldReturnBadRequestExceptionWhenIdIsWrong() throws Exception {
        String authTokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(authTokenAdmin).perform(get("/api/authrn/wrongID"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create Clarin License Label object for testing purposes.
     */
    private ClarinLicenseLabel createClarinLicenseLabel(String label, boolean extended, String title)
            throws SQLException, AuthorizeException {
        ClarinLicenseLabel clarinLicenseLabel = ClarinLicenseLabelBuilder.createClarinLicenseLabel(context).build();
        clarinLicenseLabel.setLabel(label);
        clarinLicenseLabel.setExtended(extended);
        clarinLicenseLabel.setTitle(title);

        clarinLicenseLabelService.update(context, clarinLicenseLabel);
        return clarinLicenseLabel;
    }

    /**
     * Create ClarinLicense object with ClarinLicenseLabel object for testing purposes.
     */
    private ClarinLicense createClarinLicense(String name, String definition, String requiredInfo, int confirmation)
            throws SQLException, AuthorizeException {
        ClarinLicense clarinLicense = ClarinLicenseBuilder.createClarinLicense(context).build();
        clarinLicense.setConfirmation(confirmation);
        clarinLicense.setDefinition(definition);
        clarinLicense.setRequiredInfo(requiredInfo);
        clarinLicense.setName(name);

        // add ClarinLicenseLabels to the ClarinLicense
        HashSet<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();
        ClarinLicenseLabel clarinLicenseLabel = createClarinLicenseLabel("lbl", false, "Test Title");
        clarinLicenseLabels.add(clarinLicenseLabel);
        clarinLicense.setLicenseLabels(clarinLicenseLabels);

        clarinLicenseService.update(context, clarinLicense);
        return clarinLicense;
    }

    private void attachLicenseToBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create WorkspaceItem with Bitstream and Clarin License
        List<Operation> replaceOperations = new ArrayList<Operation>();


        // Creating replace operation
        Map<String, String> licenseReplaceOpValue = new HashMap<String, String>();
        licenseReplaceOpValue.put("value", CLARIN_LICENSE_NAME);
        replaceOperations.add(new ReplaceOperation("/" + OPERATION_PATH_LICENSE_RESOURCE,
                licenseReplaceOpValue));

        context.restoreAuthSystemState();
        String updateBody = getPatchContent(replaceOperations);

        // 3. Send request to add Clarin License to the Workspace Item
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(patch("/api/submission/workspaceitems/" + witem.getID())
                        .content(updateBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // 4. Check if the Clarin License name was added to the Item's metadata `dc.rights`
        getClient(tokenAdmin).perform(get("/api/submission/workspaceitems/" + witem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.item.metadata['dc.rights'][0].value", is(CLARIN_LICENSE_NAME)));

        // 5. Check if the Clarin License was attached to the Bitstream
        getClient(tokenAdmin).perform(get("/api/core/clarinlicenses/" + clarinLicense.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bitstreams", is(1)));
    }
}
