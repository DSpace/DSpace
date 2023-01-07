/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.content.clarin.ClarinLicense.SEND_TOKEN;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + ClarinUserMetadataRest.CATEGORY + "/" + ClarinUserMetadataRest.NAME + "/" + "manage")
public class ClarinUserMetadataRestController {

    public static final String CHECK_EMAIL_RESPONSE_CONTENT = "checkEmail";

    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;
    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;
    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;
    @Autowired
    ClarinUserRegistrationService clarinUserRegistrationService;

    @RequestMapping(method = POST, consumes = APPLICATION_JSON)
    @PreAuthorize("permitAll()")
    public ResponseEntity manageUserMetadata(@RequestParam("bitstreamUUID") UUID bitstreamUUID,
                                                     HttpServletRequest request)
            throws SQLException, ParseException, IOException, AuthorizeException, ParserConfigurationException {

        // Get context from the request
        Context context = obtainContext(request);
        // Validate parameters
        if (Objects.isNull(context)) {
            return null;
        }
        if (Objects.isNull(bitstreamUUID)) {
            return null;
        }

        // Get ClarinUserMetadataRest Array from the request body
        ClarinUserMetadataRest[] clarinUserMetadataRestArray =
                new ObjectMapper().readValue(request.getInputStream(), ClarinUserMetadataRest[].class);
        if (ArrayUtils.isEmpty(clarinUserMetadataRestArray)) {
            return null;
        }

        // Convert Array to the List
        List<ClarinUserMetadataRest> clarinUserMetadataRestList = Arrays.asList(clarinUserMetadataRestArray);
        if (CollectionUtils.isEmpty(clarinUserMetadataRestList)) {
            return null;
        }

        // Get mapping between clarin license and the bitstream
        ClarinLicenseResourceMapping clarinLicenseResourceMapping =
                this.getLicenseResourceMapping(context, bitstreamUUID);
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new NotFoundException("Cannot find the license resource mapping between clarin license" +
                    " and the bitstream");
        }

        // Get current user from the context to find out if the user is signed in
        EPerson currentUser = context.getCurrentUser();
        String downloadToken = this.generateToken();
        if (Objects.isNull(currentUser)) {
            // The user is not signed in
            this.processNonSignedInUser(context, clarinUserMetadataRestList, clarinLicenseResourceMapping,
                    bitstreamUUID, downloadToken);

        } else {
            // The user is signed in
            this.processSignedInUser(context, currentUser, clarinUserMetadataRestList, clarinLicenseResourceMapping,
                    bitstreamUUID, downloadToken);
        }

        boolean shouldEmailToken = this.shouldEmailToken(context, bitstreamUUID, clarinLicenseResourceMapping);
        context.commit();
        if (shouldEmailToken) {
            // If yes - send token to e-mail
            // @TODO send download token to e-mail
            return ResponseEntity.ok().body(CHECK_EMAIL_RESPONSE_CONTENT);
        } else {
            // If no - send token in the response to download the bitstream immediately
            return ResponseEntity.ok().body(downloadToken);
        }
    }

    public void processSignedInUser(Context context, EPerson currentUser,
                                              List<ClarinUserMetadataRest> clarinUserMetadataRestList,
                                              ClarinLicenseResourceMapping clarinLicenseResourceMapping,
                                              UUID bitstreamUUID, String downloadToken)
            throws SQLException {
        // If exists userMetadata records in the table update them or create them in other case
        // Get UserRegistration which has the UserMetadata list
        List<ClarinUserRegistration> clarinUserRegistrationList =
                clarinUserRegistrationService.findByEPersonUUID(context, currentUser.getID());
        if (CollectionUtils.isEmpty(clarinUserRegistrationList)) {
            throw new NotFoundException("Cannot find user registration object associated to the user with" +
                    "id: " + currentUser.getID());
        }

        // The userUUID is unique so the list has only one record
        ClarinUserRegistration clarinUserRegistration = clarinUserRegistrationList.get(0);
        if (Objects.isNull(clarinUserRegistration)) {
            throw new NullPointerException("The user registration object associated to the user with id:" +
                    currentUser.getID() + " is null.");
        }

        List<ClarinUserMetadata> currentClarinUserMetadataList = clarinUserRegistration.getUserMetadata();
        List<ClarinUserMetadata> newClarinUserMetadataList;
        // If exists ClarinResourceUserAllowance - Clrua record in the table, create a new clrua with current
        // resource mapping, user metadata, user registration
        if (CollectionUtils.isEmpty(currentClarinUserMetadataList)) {
            // The current user doesn't fill in any user metadata, create a new UserMetadata objects
            newClarinUserMetadataList = this.createUserMetadataFromRequest(context,
                    clarinUserMetadataRestList);
        } else {
            // The current user does fill in any user metadata, update actual UserMetadata objects and create
            // the new ones is some are missing.
            // Compare the old metadata value with the new one and if the value is changed or missing, create/update
            // the metadata value.
            newClarinUserMetadataList = new ArrayList<>();
            for (ClarinUserMetadataRest clarinUserMetadataRest : clarinUserMetadataRestList) {
                boolean shouldCreate = true;
                for (ClarinUserMetadata clarinUserMetadata: currentClarinUserMetadataList) {
                    if (StringUtils.equals(clarinUserMetadataRest.getMetadataKey(),
                            clarinUserMetadata.getMetadataKey())) {
                        shouldCreate = false;
                        // Set metadata value
                        clarinUserMetadata.setMetadataValue(clarinUserMetadataRest.getMetadataValue());
                        // Update the user metadata record
                        clarinUserMetadataService.update(context, clarinUserMetadata);
                        // Add userMetadata to the list of the new user metadata
                        newClarinUserMetadataList.add(clarinUserMetadata);
                    }
                }
                if (shouldCreate) {
                    ClarinUserMetadata clarinUserMetadata = this.clarinUserMetadataService.create(context);
                    clarinUserMetadata.setMetadataKey(clarinUserMetadataRest.getMetadataKey());
                    clarinUserMetadata.setMetadataValue(clarinUserMetadataRest.getMetadataValue());
                    clarinUserMetadata.setEperson(clarinUserRegistration);
                    clarinUserMetadataService.update(context, clarinUserMetadata);
                    // Add userMetadata to the list of the new user metadata
                    newClarinUserMetadataList.add(clarinUserMetadata);
                }
            }
        }

        // Process clrua with the new clarin user metadata
        ClarinLicenseResourceUserAllowance clrua =
                this.createClrua(context, clarinLicenseResourceMapping, newClarinUserMetadataList, downloadToken,
                clarinUserRegistration);

        // Add Clarin License Resource Allowance to the user metadata records
        for (ClarinUserMetadata clarinUserMetadata : newClarinUserMetadataList) {
            clarinUserMetadata.setTransaction(clrua);
            clarinUserMetadataService.update(context, clarinUserMetadata);
        }
    }

    private ClarinLicenseResourceUserAllowance createClrua(Context context,
                                                           ClarinLicenseResourceMapping clarinLicenseResourceMapping,
                                                           List<ClarinUserMetadata> clarinUserMetadataList,
                                                           String downloadToken,
                                                           ClarinUserRegistration clarinUserRegistration)
            throws SQLException {
        // Create ClarinResourceUserAllowance record to generate token.
        ClarinLicenseResourceUserAllowance clrua =
                clarinLicenseResourceUserAllowanceService.create(context);
        clrua.setLicenseResourceMapping(clarinLicenseResourceMapping);
        clrua.setUserMetadata(clarinUserMetadataList);
        clrua.setCreatedOn(Calendar.getInstance().getTime());
        // Generate token to download the bitstream. The token is sent by the response or by the e-mail.
        clrua.setToken(downloadToken);
        if (Objects.nonNull(clarinUserRegistration)) {
            clrua.setUserRegistration(clarinUserRegistration);
        }
        clarinLicenseResourceUserAllowanceService.update(context, clrua);
        return clrua;
    }

    private void processNonSignedInUser(Context context,
                                                  List<ClarinUserMetadataRest> clarinUserMetadataRestList,
                                                  ClarinLicenseResourceMapping clarinLicenseResourceMapping,
                                                  UUID bitstreamUUID,
                                                  String downloadToken) throws SQLException {
        // Create ClarinUserMetadataRecord from the ClarinUserMetadataRest List.
        // Add created ClarinUserMetadata to the List.
        List<ClarinUserMetadata> clarinUserMetadataList = this.createUserMetadataFromRequest(context,
                clarinUserMetadataRestList);

        // Create ClarinResourceUserAllowance record to generate token.
        this.createClrua(context, clarinLicenseResourceMapping, clarinUserMetadataList, downloadToken, null);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private ClarinLicenseResourceMapping getLicenseResourceMapping(Context context, UUID bitstreamUUID)
            throws SQLException {
        // Get ClarinLicense to check if it needs to generate the token
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappingList =
                clarinLicenseResourceMappingService.findByBitstreamUUID(context, bitstreamUUID);
        if (CollectionUtils.isEmpty(clarinLicenseResourceMappingList)) {
            // Cannot find the license resource mapping between clarin license and the bitstream
            throw new NotFoundException("Cannot find the license resource mapping between clarin license" +
                    " and the bitstream");
        }
        // Every resource mapping between license and the bitstream has only one record,
        // because the bitstream has unique UUID, so get the first record from the List
        ClarinLicenseResourceMapping clarinLicenseResourceMapping = clarinLicenseResourceMappingList.get(0);
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new NullPointerException("The ClarinLicenseResourceMapping object from the list is null.");
        }
        return clarinLicenseResourceMapping;
    }

    private ClarinLicense getClarinLicense(Context context, UUID bitstreamUUID,
                                           ClarinLicenseResourceMapping clarinLicenseResourceMapping)
            throws SQLException {
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new NullPointerException("The clarinLicenseResourceMapping object is null.");
        }

        // Get ClarinLicense from the ClarinLicenseResourceMapping
        ClarinLicense clarinLicense = clarinLicenseResourceMapping.getLicense();
        if (Objects.isNull(clarinLicense)) {
            throw new NullPointerException("The ClarinLicense of the ClarinLicenseResourceMapping with id: "
                    + clarinLicenseResourceMapping.getID() + "is null.");
        }
        return clarinLicense;
    }

    private boolean shouldEmailToken(Context context, UUID bitstreamUUID,
                                     ClarinLicenseResourceMapping clarinLicenseResourceMapping) throws SQLException {
        ClarinLicense clarinLicense = this.getClarinLicense(context, bitstreamUUID, clarinLicenseResourceMapping);
        if (Objects.isNull(clarinLicense)) {
            throw new NullPointerException("The ClarinLicense is null.");
        }

        // If the required info contains the key work `SEND_TOKEN` it should generate the token.
        return clarinLicense.getRequiredInfo().contains(SEND_TOKEN);
    }


    private List<ClarinUserMetadata> createUserMetadataFromRequest(Context context,
                                                                   List<ClarinUserMetadataRest>
                                                                           clarinUserMetadataRestList)
            throws SQLException {
        List<ClarinUserMetadata> clarinUserMetadataList = new ArrayList<>();
        for (ClarinUserMetadataRest clarinUserMetadataRest : clarinUserMetadataRestList) {
            ClarinUserMetadata clarinUserMetadata = clarinUserMetadataService.create(context);
            clarinUserMetadata.setMetadataValue(clarinUserMetadataRest.getMetadataValue());
            clarinUserMetadata.setMetadataKey(clarinUserMetadataRest.getMetadataKey());
            clarinUserMetadataService.update(context, clarinUserMetadata);

            // Add created ClarinUserMetadata to the list
            clarinUserMetadataList.add(clarinUserMetadata);
        }
        return clarinUserMetadataList;
    }
}
