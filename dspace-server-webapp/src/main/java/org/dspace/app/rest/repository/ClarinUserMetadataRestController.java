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
import static org.dspace.content.clarin.ClarinLicense.EXTRA_EMAIL;
import static org.dspace.content.clarin.ClarinLicense.SEND_TOKEN;
import static org.dspace.content.clarin.ClarinUserRegistration.ANONYMOUS_USER_REGISTRATION;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/" + ClarinUserMetadataRest.CATEGORY + "/" + ClarinUserMetadataRest.NAME + "/" + "manage")
public class ClarinUserMetadataRestController {
    private static final Logger log = LoggerFactory.getLogger(ClarinUserMetadataRestController.class);

    public static final String CHECK_EMAIL_RESPONSE_CONTENT = "checkEmail";

    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;
    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;
    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;
    @Autowired
    ClarinUserRegistrationService clarinUserRegistrationService;
    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    ItemService itemService;

    @Autowired
    ConfigurationService configurationService;

    // Enum to distinguish between the two types of the email
    enum MailType { ALLZIP, BITSTREAM }

    @RequestMapping(value = "/zip", method = POST, consumes = APPLICATION_JSON)
    @PreAuthorize("permitAll()")
    public ResponseEntity manageUserMetadataForZIP(@RequestParam("itemUUID") UUID itemUUID,
                                             HttpServletRequest request)
            throws SQLException, ParseException, IOException, AuthorizeException, MessagingException {

        // Get context from the request
        Context context = obtainContext(request);
        // Validate parameters
        if (Objects.isNull(context)) {
            return null;
        }
        if (Objects.isNull(itemUUID)) {
            return null;
        }

        // Get current user from the context to find out if the user is signed in
        EPerson currentUser = context.getCurrentUser();
        String downloadToken = this.generateToken();

        Item item = itemService.find(context, itemUUID);
        if (Objects.isNull(item)) {
            log.error("Cannot find the item with ID: " + itemUUID);
            throw new NotFoundException("Cannot find the item with ID: " + itemUUID);
        }
        // Load the name because it is used in the email, and it must be loaded before the `sendEmailWithDownloadLink`
        // method otherwise it will throw an exception about Lazy loading.
        item.getName();

        boolean shouldEmailToken = false;
        ClarinLicense clarinLicense = null;
        // Get ClarinUserMetadataRest Array from the request body
        ClarinUserMetadataRest[] clarinUserMetadataRestArray =
                new ObjectMapper().readValue(request.getInputStream(), ClarinUserMetadataRest[].class);
        if (Objects.isNull(clarinUserMetadataRestArray)) {
            throw new RuntimeException("The clarinUserMetadataRestArray cannot be null. It could be empty, but" +
                    " not null");
        }

        // Convert Array to the List
        List<ClarinUserMetadataRest> clarinUserMetadataRestList = Arrays.asList(clarinUserMetadataRestArray);
        List<Bundle> bundles = item.getBundles("ORIGINAL");
        for (Bundle original : bundles) {
            List<Bitstream> bss = original.getBitstreams();
            for (Bitstream bitstream : bss) {
                UUID bitstreamUUID = bitstream.getID();
                // Get mapping between clarin license and the bitstream
                ClarinLicenseResourceMapping clarinLicenseResourceMapping =
                        this.getLicenseResourceMapping(context, bitstreamUUID);
                if (Objects.isNull(clarinLicenseResourceMapping)) {
                    throw new NotFoundException("Cannot find the license resource mapping between clarin license" +
                            " and the bitstream");
                }
                if (Objects.isNull(currentUser)) {
                    // The user is not signed in
                    this.processNonSignedInUser(context, clarinUserMetadataRestList, clarinLicenseResourceMapping,
                            downloadToken);

                } else {
                    // The user is signed in
                    this.processSignedInUser(context, currentUser, clarinUserMetadataRestList,
                            clarinLicenseResourceMapping, downloadToken);
                }

                // Check (only once) if the Clarin License contains the required information to SEND_TOKEN, which means
                // the item must be downloaded after the user confirms the download with the token sent by email.
                if (Objects.isNull(clarinLicense)) {
                    clarinLicense = this.getClarinLicense(clarinLicenseResourceMapping);
                    shouldEmailToken = this.shouldEmailToken(clarinLicenseResourceMapping);
                }
            }
        }

        context.commit();
        if (shouldEmailToken) {
            // If yes - send token to e-mail
            try {
                String email = getEmailFromUserMetadata(clarinUserMetadataRestList);
                this.sendEmailWithDownloadLink(context, item, clarinLicense,
                        email, downloadToken, MailType.ALLZIP, clarinUserMetadataRestList);
            } catch (MessagingException e) {
                log.error("Cannot send the download email because: " + e.getMessage());
                throw new RuntimeException("Cannot send the download email because: " + e.getMessage());
            }

            return ResponseEntity.ok().body(CHECK_EMAIL_RESPONSE_CONTENT);
        } else {
            // If no - send token in the response to download the bitstream immediately
            return ResponseEntity.ok().body(downloadToken);
        }
    }

    @RequestMapping(method = POST, consumes = APPLICATION_JSON)
    @PreAuthorize("permitAll()")
    public ResponseEntity manageUserMetadata(@RequestParam("bitstreamUUID") UUID bitstreamUUID,
                                                     HttpServletRequest request)
            throws SQLException, ParseException, IOException, AuthorizeException, MessagingException {

        // Get context from the request
        Context context = obtainContext(request);
        // Validate parameters
        if (Objects.isNull(context)) {
            return null;
        }


        Bitstream bitstream = bitstreamService.find(context, bitstreamUUID);
        if (Objects.isNull(bitstream)) {
            log.error("Cannot find the bitstream with ID: " + bitstreamUUID);
            return null;
        }
        // Load the name because it is used in the email, and it must be loaded before the `sendEmailWithDownloadLink`
        // method otherwise it will throw an exception about Lazy loading.
        bitstream.getName();

        // Get current user from the context to find out if the user is signed in
        EPerson currentUser = context.getCurrentUser();
        String downloadToken = this.generateToken();

        // Get mapping between clarin license and the bitstream
        ClarinLicenseResourceMapping clarinLicenseResourceMapping =
                this.getLicenseResourceMapping(context, bitstream.getID());
        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new NotFoundException("Cannot find the license resource mapping between clarin license" +
                    " and the bitstream");
        }

        // Get ClarinUserMetadataRest Array from the request body
        ClarinUserMetadataRest[] clarinUserMetadataRestArray =
                new ObjectMapper().readValue(request.getInputStream(), ClarinUserMetadataRest[].class);
        if (Objects.isNull(clarinUserMetadataRestArray)) {
            throw new RuntimeException("The clarinUserMetadataRestArray cannot be null. It could be empty, but" +
                    " not null");
        }

        // Convert Array to the List
        List<ClarinUserMetadataRest> clarinUserMetadataRestList = Arrays.asList(clarinUserMetadataRestArray);
        if (Objects.isNull(currentUser)) {
            // The user is not signed in
            this.processNonSignedInUser(context, clarinUserMetadataRestList, clarinLicenseResourceMapping,
                    downloadToken);

        } else {
            // The user is signed in
            this.processSignedInUser(context, currentUser, clarinUserMetadataRestList, clarinLicenseResourceMapping,
                    downloadToken);
        }

        boolean shouldEmailToken = this.shouldEmailToken(clarinLicenseResourceMapping);
        context.commit();
        if (shouldEmailToken) {
            // If yes - send token to e-mail
            try {
                String email = getEmailFromUserMetadata(clarinUserMetadataRestList);
                ClarinLicense clarinLicense = this.getClarinLicense(clarinLicenseResourceMapping);
                this.sendEmailWithDownloadLink(context, bitstream, clarinLicense,
                        email, downloadToken, MailType.BITSTREAM, clarinUserMetadataRestList);
            } catch (MessagingException e) {
                log.error("Cannot send the download email because: " + e.getMessage());
                throw new RuntimeException("Cannot send the download email because: " + e.getMessage());
            }

            return ResponseEntity.ok().body(CHECK_EMAIL_RESPONSE_CONTENT);
        } else {
            // If no - send token in the response to download the bitstream immediately
            return ResponseEntity.ok().body(downloadToken);
        }
    }

    private void sendEmailWithDownloadLink(Context context, DSpaceObject dso,
                                           ClarinLicense clarinLicense,
                                           String email,
                                           String downloadToken,
                                           MailType mailType,
                                           List<ClarinUserMetadataRest> clarinUserMetadataRestList)
            throws IOException, SQLException, MessagingException {
        if (StringUtils.isBlank(email)) {
            log.error("Cannot send email with download link because the email is empty.");
            throw new BadRequestException("Cannot send email with download link because the email is empty.");
        }

        if (Objects.isNull(dso)) {
            log.error("Cannot send email with download link because the DSpaceObject is null.");
            throw new BadRequestException("Cannot send email with download link because the DSpaceObject is null.");
        }

        // Fetch DSpace main cfg info and send it in the email
        String uiUrl = configurationService.getProperty("dspace.ui.url", "");
        String helpDeskEmail = configurationService.getProperty("lr.help.mail", "");
        String helpDeskPhoneNum = configurationService.getProperty("lr.help.phone", "");
        String dspaceName = configurationService.getProperty("dspace.name", "");
        String dspaceNameShort = configurationService.getProperty("dspace.name.short", "");

        if (StringUtils.isEmpty(uiUrl)) {
            log.error("Cannot load the `dspace.ui.url` property from the cfg.");
            throw new RuntimeException("Cannot load the `dspace.ui.url` property from the cfg.");
        }
        // Compose download link
        // Redirect to `/api/bitstreams/{bitstreamId}/download?dtoken={downloadToken}` or
        // `/api/items/{itemId}/download?dtoken={downloadToken}`
        String downloadLink = uiUrl + "/"  + (dso instanceof Item ? ItemRest.PLURAL_NAME : BitstreamRest.PLURAL_NAME) +
                "/" + dso.getID() + "/download";
        String downloadLinkWithToken = downloadLink + "?dtoken=" + downloadToken;
        try {
            Locale locale = context.getCurrentLocale();
            Email bean = Email.getEmail(I18nUtil.getEmailFilename(locale, "clarin_download_link"));
            bean.addArgument(dso.getName());
            bean.addArgument(downloadLinkWithToken);
            bean.addArgument(clarinLicense.getDefinition());
            bean.addArgument(helpDeskEmail);
            bean.addArgument(helpDeskPhoneNum);
            bean.addArgument(dspaceNameShort);
            bean.addArgument(dspaceName);
            bean.addArgument(uiUrl);
            bean.addRecipient(email);
            bean.send();
        } catch (MessagingException e) {
            log.error("Cannot send the email with download link, because: " + e.getMessage());
            throw new MessagingException(e.getMessage());
        }
        // If previous mail fails with exception, this block never executes = admin is NOT
        // notified, if the mail is not really sent (if it fails HERE, not later, e.g. due to mail server issue).
        sendAdminNotificationEmail(context, downloadLink, dso, clarinLicense, mailType, clarinUserMetadataRestList);

    }


    private List<String> getCCEmails(String ccAdmin, ClarinLicense clarinLicense) {
        List<String> ccEmails = new ArrayList<>();
        if (ccAdmin != null && !ccAdmin.isEmpty()) {
            ccEmails.add(ccAdmin);
        }

        String licenseName = clarinLicense.getName().trim().replace(" ", "_").toLowerCase();
        String[] licenseSpecialRecipients = configurationService.getArrayProperty("download.email.cc." + licenseName);

        if (licenseSpecialRecipients != null) {
            for (String cc : licenseSpecialRecipients) {
                if (!cc.isEmpty()) {
                    ccEmails.add(cc.trim());
                }
            }
        }

        return ccEmails;
    }

    private void addAdminEmailArguments(Email mail, MailType mailType, DSpaceObject dso, String downloadLink,
                                        ClarinLicense clarinLicense, Context context,
                                        List<ClarinUserMetadataRest> extraMetadata) {
        if (mailType == MailType.ALLZIP) {
            mail.addArgument("all files requested");
        } else if (mailType == MailType.BITSTREAM) {
            mail.addArgument(dso.getName());
        } else {
            throw new IllegalArgumentException("The mail type is not supported.");
        }
        mail.addArgument(downloadLink);
        mail.addArgument(clarinLicense.getDefinition());
        if (context.getCurrentUser() != null) {
            mail.addArgument(context.getCurrentUser().getFullName());
            mail.addArgument(context.getCurrentUser().getEmail());

        } else {
            // used in place of Full Name of user
            mail.addArgument("Anonymous user");
            // used in place of user's email
            mail.addArgument("Anonymous user");
        }
        StringBuilder exdata = new StringBuilder();
        for (ClarinUserMetadataRest data : extraMetadata) {
            exdata.append(data.getMetadataKey()).append(": ").append(data.getMetadataValue()).append(", ");
        }
        mail.addArgument(exdata.toString());
    }

    private void sendAdminNotificationEmail(Context context,
                                            String downloadLink,
                                            DSpaceObject dso,
                                            ClarinLicense clarinLicense,
                                            MailType mailType,
                                            List<ClarinUserMetadataRest> extraMetadata)
            throws MessagingException, IOException {
        try {
            Locale locale = context.getCurrentLocale();
            Email email2Admin = Email.getEmail(I18nUtil.getEmailFilename(locale, "clarin_download_link_admin"));
            String ccAdmin = configurationService.getProperty("download.email.cc");
            List<String> ccEmails = getCCEmails(ccAdmin, clarinLicense);

            if (!ccEmails.isEmpty()) {
                for (String cc : ccEmails) {
                    email2Admin.addRecipient(cc);
                }
                addAdminEmailArguments(email2Admin, mailType, dso, downloadLink, clarinLicense, context, extraMetadata);

            }
            email2Admin.send();
        } catch (MessagingException e) {
            log.error("Cannot send the notification email to admin because: " + e.getMessage());
            throw new MessagingException(e.getMessage());
        }
    }

    private String getEmailFromUserMetadata(List<ClarinUserMetadataRest> clarinUserMetadataRestList) {
        return getFieldFromUserMetadata(EXTRA_EMAIL, clarinUserMetadataRestList);
    }

    private String getFieldFromUserMetadata(String field, List<ClarinUserMetadataRest> clarinUserMetadataRestList) {
        String ret = "";
        for (ClarinUserMetadataRest clarinUserMetadataRest : clarinUserMetadataRestList) {
            if (StringUtils.equals(clarinUserMetadataRest.getMetadataKey(), field)) {
                ret = clarinUserMetadataRest.getMetadataValue();
            }
        }
        return ret;
    }

    public List<ClarinUserMetadata> processSignedInUser(Context context, EPerson currentUser,
                                              List<ClarinUserMetadataRest> clarinUserMetadataRestList,
                                              ClarinLicenseResourceMapping clarinLicenseResourceMapping,
                                              String downloadToken)
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

        // List of the new user metadata - passed from the request
        List<ClarinUserMetadata> clarinUserMetadataList = new ArrayList<>();

        // Create user metadata records from request
        for (ClarinUserMetadataRest clarinUserMetadataRest : clarinUserMetadataRestList) {
            ClarinUserMetadata clarinUserMetadata = this.clarinUserMetadataService.create(context);
            clarinUserMetadata.setMetadataKey(clarinUserMetadataRest.getMetadataKey());
            clarinUserMetadata.setMetadataValue(clarinUserMetadataRest.getMetadataValue());
            clarinUserMetadata.setEperson(clarinUserRegistration);
            clarinUserMetadataService.update(context, clarinUserMetadata);
            // Add userMetadata to the list of the new user metadata
            clarinUserMetadataList.add(clarinUserMetadata);
        }

        // Process clrua with the new clarin user metadata
        ClarinLicenseResourceUserAllowance clrua =
                this.createClrua(context, clarinLicenseResourceMapping, clarinUserMetadataList, downloadToken,
                clarinUserRegistration);

        // Add Clarin License Resource Allowance to the user metadata records
        for (ClarinUserMetadata clarinUserMetadata : clarinUserMetadataList) {
            clarinUserMetadata.setTransaction(clrua);
            clarinUserMetadataService.update(context, clarinUserMetadata);
        }
        return clarinUserMetadataList;
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

    public List<ClarinUserMetadata> processNonSignedInUser(Context context,
                                                  List<ClarinUserMetadataRest> clarinUserMetadataRestList,
                                                  ClarinLicenseResourceMapping clarinLicenseResourceMapping,
                                                  String downloadToken) throws SQLException {
        // Create ClarinUserMetadataRecord from the ClarinUserMetadataRest List.
        // Add created ClarinUserMetadata to the List.
        List<ClarinUserMetadata> clarinUserMetadataList = this.createUserMetadataFromRequest(context,
                clarinUserMetadataRestList);

        // Get anonymous user registration - user metadata should not have `user_registration_id = null`
        ClarinUserRegistration clarinUserRegistration = null;
        List<ClarinUserRegistration> clarinUserRegistrationList = clarinUserRegistrationService
                .findByEmail(context, ANONYMOUS_USER_REGISTRATION);
        for (ClarinUserRegistration fetchedClarinuserRegistration : clarinUserRegistrationList) {
            if (!StringUtils.equals(fetchedClarinuserRegistration.getOrganization(), ANONYMOUS_USER_REGISTRATION)) {
                continue;
            }
            clarinUserRegistration = fetchedClarinuserRegistration;
            break;
        }

        // Create ClarinResourceUserAllowance record to generate token.
        ClarinLicenseResourceUserAllowance clrua = this.createClrua(context, clarinLicenseResourceMapping,
                clarinUserMetadataList, downloadToken, clarinUserRegistration);
        // Add Clarin License Resource Allowance to the user metadata records
        for (ClarinUserMetadata clarinUserMetadata : clarinUserMetadataList) {
            clarinUserMetadata.setTransaction(clrua);
            clarinUserMetadata.setEperson(clarinUserRegistration);
            clarinUserMetadataService.update(context, clarinUserMetadata);
        }
        return clarinUserMetadataList;
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    public ClarinLicenseResourceMapping getLicenseResourceMapping(Context context, UUID bitstreamUUID)
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

    private ClarinLicense getClarinLicense(ClarinLicenseResourceMapping clarinLicenseResourceMapping) {
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

    private boolean shouldEmailToken(ClarinLicenseResourceMapping clarinLicenseResourceMapping) {
        ClarinLicense clarinLicense = this.getClarinLicense(clarinLicenseResourceMapping);
        if (Objects.isNull(clarinLicense)) {
            throw new NullPointerException("The ClarinLicense is null.");
        }

        // If the required info contains the key work `SEND_TOKEN` it should generate the token.
        if (StringUtils.isBlank(clarinLicense.getRequiredInfo())) {
            return false;
        }
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
