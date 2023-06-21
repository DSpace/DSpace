/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.repository.ClarinUserMetadataRestController;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Specialized controller created for Clarin-Dspace import user metadata.
 * It creates ClarinLicenseResourceUserAllowance too.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/clarin/import")
public class ClarinUserMetadataImportController {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinUserMetadataImportController.class);

    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;
    @Autowired
    private ClarinUserRegistrationService clarinUserRegistrationService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private Utils utils;
    @Autowired
    private ClarinUserMetadataRestController clarinUserMetadataRestController;

    /**
     * Endpoint for import user_metadata for eperson and bitstream.
     * Endpoint creates ClarinLicenseResourceUserAllowance too, because we use the method in which it is doing.
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/import/usermetadata
     * }
     * </pre>
     * @param request request
     * @return created user metadata converted to rest object
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method =  RequestMethod.POST, value = "/usermetadata")
    public ClarinUserMetadataRest importUserMetadata(HttpServletRequest request) throws SQLException, IOException,
            java.text.ParseException {
        //controlling of the input parameters
        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }

        String userRegistrationIdString = request.getParameter("userRegistrationId");
        if (StringUtils.isBlank(userRegistrationIdString)) {
            log.error("Required parameter userRegistrationId is null!");
            throw new RuntimeException("UserRegistrationId is null!");
        }
        Integer userRegistrationId = Integer.parseInt(userRegistrationIdString);

        String bitstreamUUIDString = request.getParameter("bitstreamUUID");
        if (StringUtils.isBlank(bitstreamUUIDString)) {
            log.error("Required parameter bitstreamUUID is null!");
            throw new RuntimeException("BitstreamUUID is null!");
        }
        UUID bitstreamUUID = UUID.fromString(bitstreamUUIDString);

        log.info("Processing user registration id: " + userRegistrationId + " and bitstream UUID: " + bitstreamUUID);

        String createdOnString = request.getParameter("createdOn");
        if (StringUtils.isBlank(createdOnString)) {
            log.error("Required parameter created_on is null!");
            throw new RuntimeException("Created_on is null!");
        }
        Date createdOn = getDateFromString(createdOnString);

        //we don't control token, because it can be null
        String token = request.getParameter("token");

        ClarinUserRegistration userRegistration = clarinUserRegistrationService.find(context,
                userRegistrationId);
        if (Objects.isNull(userRegistration)) {
            log.error("User registration with id: " + userRegistrationId + " doesn't exist!");
            throw new RuntimeException("User registration with id: " + userRegistrationId + " doesn't exist!");
        }

        //eperson can be null, we don't control, if it exists
        EPerson ePerson = null;
        if (Objects.nonNull(userRegistration.getPersonID())) {
            ePerson = ePersonService.find(context, userRegistration.getPersonID());
        }

        // Get ClarinUserMetadataRest Array from the request body
        ClarinUserMetadataRest[] clarinUserMetadataRestArray =
                new ObjectMapper().readValue(request.getInputStream(), ClarinUserMetadataRest[].class);
        if (ArrayUtils.isEmpty(clarinUserMetadataRestArray)) {
            log.error("Cannot get clarinUserMetadataRestArray from request for user registration with id: "
                    + userRegistrationId +
                    " and bitstream with id: " + bitstreamUUID);
            throw new RuntimeException("Cannot get clarinUserMetadataRestArray from request " +
                    "for user registration with id: "
                    + userRegistrationId + " and bitstream with id: " + bitstreamUUID);
        }
        // Convert Array to the List
        List<ClarinUserMetadataRest> clarinUserMetadataRestList = Arrays.asList(clarinUserMetadataRestArray);
        if (CollectionUtils.isEmpty(clarinUserMetadataRestList)) {
            log.error("Cannot convert clarinUserMetadataRestArray to array for user registration with id: "
                    + userRegistrationId +
                    " and bitstream id: " + bitstreamUUID);
            throw new RuntimeException("Cannot get clarinUserMetadataRestArray from " +
                    "request for user registration with id: "
                    + userRegistrationId + " and bitstream with id: " + bitstreamUUID);
        }

        try {
            // Get mapping between clarin license and the bitstream
            ClarinLicenseResourceMapping clarinLicenseResourceMapping =
                    clarinUserMetadataRestController.getLicenseResourceMapping(context, bitstreamUUID);
            if (Objects.isNull(clarinLicenseResourceMapping)) {
                log.error("Cannot find the license resource mapping between clarin license" +
                        " and the bitstream with id: " + bitstreamUUID);
                throw new NotFoundException("Cannot find the license resource mapping between clarin license" +
                        " and the bitstream with id: " + bitstreamUUID);
            }
            List<ClarinUserMetadata> newClarinUserMetadataList;
            if (Objects.nonNull(ePerson)) {
                // The user is signed in
                //create user metadata and license resource user allowance
                newClarinUserMetadataList = clarinUserMetadataRestController.processSignedInUser(
                        context, ePerson, clarinUserMetadataRestList,
                        clarinLicenseResourceMapping, bitstreamUUID, token);
            } else {
                // The user not is signed in
                //create user metadata and license resource user allowance
                newClarinUserMetadataList = clarinUserMetadataRestController.processNonSignedInUser(
                        context, clarinUserMetadataRestList, clarinLicenseResourceMapping, bitstreamUUID, token);
            }
            //set eperson_id (user registration) in user_metadata
            newClarinUserMetadataList.get(0).setEperson(userRegistration);
            //set created_on for created license_resource_user_allowance
            //created list has to contain minimally one record
            ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance =
                    newClarinUserMetadataList.get(0).getTransaction();
            clarinLicenseResourceUserAllowance.setCreatedOn(createdOn);
            clarinLicenseResourceUserAllowanceService.update(context, clarinLicenseResourceUserAllowance);
            //return created object as rest object
            ClarinUserMetadataRest clarinUserMetadataRest = converter.toRest(newClarinUserMetadataList.get(0),
                    utils.obtainProjection());
            context.commit();

            return clarinUserMetadataRest;
        } catch (Exception e) {
            log.error("Something is very very very wrong with user registration: " + userRegistration.getID()
                    + " and bitstream: "
                    + bitstreamUUID + ". Excemption: " + e.getMessage());
            throw new RuntimeException("Something is very very very wrong with user registration: "
                    + userRegistration.getID()
                    + " and bitstream: " + bitstreamUUID + ". Excemption: " + e.getMessage());
        }
    }

    /**
     * Convert String value to Date.
     * Expects two possible date formats, but more can be added.
     * @param value
     * @return converted input value to Date
     * @throws java.text.ParseException if parse error
     */
    private Date getDateFromString(String value) throws java.text.ParseException {
        Date output = null;
        if (StringUtils.isBlank(value)) {
            return null;
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        try {
            output = sdf.parse(value);
        } catch (java.text.ParseException e) {
            try {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSS");
                output = sdf.parse(value);
            } catch (java.text.ParseException e1) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS");
                output = sdf.parse(value);
            }
        }
        return output;
    }
}
