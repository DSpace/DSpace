/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Authorize the user if could download the Item's bitstream.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class AuthorizationBitstreamUtils {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationBitstreamUtils.class);

    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;
    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;
    @Autowired
    BitstreamService bitstreamService;

    /**
     * Check if the current user is authorized to download the bitstream in the three steps:
     * 1. If the current user is submitter of the item where the current bitstream is -> the user is authorized.
     * 2. If the request contains token which is verified -> the user is authorized.
     * 3. If the bitstream license requires confirmation every time or the user didn't fill in required
     *    metadata for the bitstream's license -> the user is not authorized.
     * @param context
     * @return
     * @throws SQLException
     */
    public boolean authorizeBitstream(Context context, Bitstream bitstream) throws SQLException,
            AuthorizeException {
        if (Objects.isNull(bitstream)) {
            return false;
        }
        if (Objects.isNull(context)) {
            return false;
        }

        // Load the current user
        EPerson currentUser = context.getCurrentUser();
        // Load the current user ID or if the user do not exist set ID to null
        UUID userID = null; // user not logged in
        if (Objects.nonNull(currentUser)) {
            userID = currentUser.getID();
        }

        UUID bitstreamUUID = bitstream.getID();
        // 1. If the current user is submitter of the item where the current bitstream is -> the user is authorized.
        if (userIsSubmitter(context, bitstream, currentUser, userID)) {
            return true;
        }

        // 2. If the request contains token which is verified -> the user is authorized.
        if (isTokenVerified(context, bitstreamUUID)) {
            return true;
        }

        // 3. If the bitstream license requires confirmation every time or the user didn't fill in required
        // metadata for the bitstream's license -> the user is not authorized.
        return isUserAllowedToAccessTheResource(context, userID, bitstreamUUID);
    }

    /**
     * If the bitstream has RES or ACA license and the user is Anonymous do not authorize that user.
     * The user will be redirected to the login.
     * @param context DSpace context object
     * @param bitstreamID downloading Bitstream UUID
     * @return if the current user is authorized
     */
    public boolean authorizeLicenseWithUser(Context context, UUID bitstreamID) throws SQLException {
        // If the current user is null that means that the user is not signed in and cannot download the bitstream
        // with RES or ACA license
        if (Objects.nonNull(context.getCurrentUser())) {
            // User is signed
            return true;
        }

        // Get ClarinLicenseResourceMapping where the bitstream is mapped with clarin license
        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                clarinLicenseResourceMappingService.findByBitstreamUUID(context, bitstreamID);

        // Bitstream does not have Clarin License
        if (CollectionUtils.isEmpty(clarinLicenseResourceMappings)) {
            return true;
        }

        // Bitstream should have only one type of the Clarin license, so we could get first record
        ClarinLicense clarinLicense = Objects.requireNonNull(clarinLicenseResourceMappings.get(0)).getLicense();
        // Get License Labels from clarin license and check if one of them is ACA or RES
        List<ClarinLicenseLabel> clarinLicenseLabels = clarinLicense.getLicenseLabels();
        for (ClarinLicenseLabel clarinLicenseLabel : clarinLicenseLabels) {
            if (StringUtils.equals(clarinLicenseLabel.getLabel(), "RES") ||
                StringUtils.equals(clarinLicenseLabel.getLabel(), "ACA")) {
                return false;
            }
        }

        return true;
    }

    private boolean userIsSubmitter(Context context, Bitstream bitstream, EPerson currentUser, UUID userID) {
        try {
            // Load Bitstream's Item, the Item contains the Bitstream
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);

            // If the Item is submitted by the current user, the submitter is always authorized to access his own
            // bitstream
            EPerson submitter = null;
            if (Objects.nonNull(item)) {
                submitter = item.getSubmitter();
            }

            if (Objects.nonNull(submitter) && Objects.nonNull(userID)) {
                if (Objects.nonNull(currentUser) &&
                        StringUtils.equals(submitter.getID().toString(), userID.toString())) {
                    return true;
                }
            }
        } catch (SQLException sqle) {
            log.error("Failed to get parent object for bitstream", sqle);
            return false;
        } catch (ClassCastException ex) {
            // parent object is not an Item
            // special bitstreams e.g. images of community/collection
            return false;
        }

        return false;
    }

    private boolean isTokenVerified(Context context, UUID bitstreamID) throws DownloadTokenExpiredException,
            SQLException {
        // Load the current request.
        Request currentRequest = new DSpace().getRequestService().getCurrentRequest();
        if (Objects.isNull(currentRequest)) {
            return false;
        }

        HttpServletRequest request = currentRequest.getHttpServletRequest();
        if (Objects.isNull(request)) {
            return false;
        }

        // Load the token from the request
        String dtoken = null;
        try {
            dtoken = request.getParameter("dtoken");
        } catch (IllegalStateException e) {
            //If the dspace kernel is null (eg. when we get here from OAI)
        } catch (Exception e) {
            //
        }

        if (StringUtils.isBlank(dtoken)) {
            return false;
        }

        boolean tokenFound = clarinLicenseResourceUserAllowanceService.verifyToken(context, bitstreamID, dtoken);
        // Check token
        if (tokenFound) { // database token match with url token
            return true;
        } else {
            throw new DownloadTokenExpiredException("The download token is invalid or expires.");
        }
    }

    /**
     * Check if the Clarin License attached to the downloading bitstream requires custom user information and
     * check if the current user has filled in that required info in the past.
     * @param context DSpace context object
     * @param userID UUID of the current user
     * @param bitstreamID UUID of the downloading bitstream
     */
    private boolean isUserAllowedToAccessTheResource(Context context, UUID userID, UUID bitstreamID)
            throws MissingLicenseAgreementException, SQLException {
        boolean allowed = clarinLicenseResourceUserAllowanceService
                .isUserAllowedToAccessTheResource(context, userID, bitstreamID);

        if (!allowed) {
            throw new MissingLicenseAgreementException("Missing license agreement!");
        }
        return true;
    }
}
