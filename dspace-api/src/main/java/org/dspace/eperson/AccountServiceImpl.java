/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.mail.MessagingException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.eperson.dto.RegistrationDataPatch;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.log.LogMessage;

/**
 * Methods for handling registration by email and forgotten passwords. When
 * someone registers as a user, or forgets their password, the
 * sendRegistrationInfo or sendForgotPasswordInfo methods can be used to send an
 * email to the user. The email contains a special token, a long string which is
 * randomly generated and thus hard to guess. When the user presents the token
 * back to the system, the AccountManager can use the token to determine the
 * identity of the eperson.
 *
 * *NEW* now ignores expiration dates so that tokens never expire
 *
 * @author Peter Breton
 * @version $Revision$
 */
public class AccountServiceImpl implements AccountService {
    /**
     * log4j log
     */
    private static final Logger log = LogManager.getLogger(AccountServiceImpl.class);

    private static final Map<String, BiConsumer<RegistrationData, EPerson>> allowedMergeArguments =
        Map.of(
            "email",
            (RegistrationData registrationData, EPerson eperson) -> eperson.setEmail(registrationData.getEmail())
        );

    @Autowired(required = true)
    protected EPersonService ePersonService;

    @Autowired(required = true)
    protected RegistrationDataService registrationDataService;
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private MetadataValueService metadataValueService;

    protected AccountServiceImpl() {

    }

    /**
     * Email registration info to the given email address.
     * Potential error conditions:
     * <ul>
     *   <li>Cannot create registration data in database (throws SQLException).</li>
     *   <li>Error sending email (throws MessagingException).</li>
     *   <li>Error reading email template (throws IOException).</li>
     *   <li>Authorization error (throws AuthorizeException).</li>
     * </ul>
     *
     * @param context DSpace context
     * @param email   Email address to send the registration email to
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws jakarta.mail.MessagingException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void sendRegistrationInfo(Context context, String email)
        throws SQLException, IOException, MessagingException,
        AuthorizeException {
        if (!configurationService.getBooleanProperty("user.registration", true)) {
            throw new IllegalStateException("The user.registration parameter was set to false");
        }
        if (!authenticationService.canSelfRegister(context, null, email)) {
            throw new IllegalStateException("self registration is not allowed with this email address");
        }
        sendInfo(context, email, RegistrationTypeEnum.REGISTER, true);
    }

    /**
     * Email forgot password info to the given email address.
     * Potential error conditions:
     * <ul>
     *   <li>No EPerson with that email (returns null).</li>
     *   <li>Cannot create registration data in database (throws SQLException).</li>
     *   <li>Error sending email (throws MessagingException).</li>
     *   <li>Error reading email template (throws IOException).</li>
     *   <li>Authorization error (throws AuthorizeException).</li>
     * </ul>
     *
     *
     * @param context DSpace context
     * @param email   Email address to send the forgot-password email to
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws jakarta.mail.MessagingException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void sendForgotPasswordInfo(Context context, String email)
        throws SQLException, IOException, MessagingException, AuthorizeException {
        sendInfo(context, email, RegistrationTypeEnum.FORGOT, true);
    }

    /**
     * Checks if exists an account related to the token provided
     *
     * @param context DSpace context
     * @param token Account token
     * @return true if exists, false otherwise
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    public boolean existsAccountFor(Context context, String token) throws SQLException, AuthorizeException {
        return getEPerson(context, token) != null;
    }

    @Override
    public boolean existsAccountWithEmail(Context context, String email) throws SQLException {
        return ePersonService.findByEmail(context, email) != null;
    }

    /**
     * <p>
     * Return the EPerson corresponding to token, where token was emailed to the
     * person by either the sendRegistrationInfo or sendForgotPasswordInfo
     * methods.
     * </p>
     *
     * <p>
     * If the token is not found return null.
     * </p>
     *
     * @param context DSpace context
     * @param token   Account token
     * @return The EPerson corresponding to token, or null.
     * @throws SQLException If the token or eperson cannot be retrieved from the
     *                      database.
     * @throws AuthorizeException passed through.
     */
    @Override
    public EPerson getEPerson(Context context, String token)
        throws SQLException, AuthorizeException {
        String email = getEmail(context, token);

        if (email == null) {
            return null;
        }

        return ePersonService.findByEmail(context, email);
    }

    /**
     * Return the e-mail address referred to by a token, or null if email
     * address can't be found ignores expiration of token
     *
     * @param context DSpace context
     * @param token   Account token
     * @return The email address corresponding to token, or null.
     * @throws java.sql.SQLException passed through.
     */
    @Override
    public String getEmail(Context context, String token)
        throws SQLException {
        RegistrationData registrationData = registrationDataService.findByToken(context, token);

        if (registrationData == null) {
            return null;
        }

        return registrationData.getEmail();
    }

    /**
     * Delete token.
     *
     * @param context DSpace context
     * @param token   The token to delete
     * @throws SQLException If a database error occurs
     */
    @Override
    public void deleteToken(Context context, String token)
        throws SQLException {
        registrationDataService.deleteByToken(context, token);
    }

    @Override
    public EPerson mergeRegistration(Context context, UUID personId, String token, List<String> overrides)
        throws AuthorizeException, SQLException {

        RegistrationData registrationData = getRegistrationData(context, token);
        EPerson eperson = null;
        if (personId != null) {
            eperson = ePersonService.findByIdOrLegacyId(context, personId.toString());
        }

        if (!canCreateUserBy(context, registrationData.getRegistrationType())) {
            throw new AuthorizeException("Token type invalid for the current user.");
        }

        if (hasLoggedEPerson(context) && !isSameContextEPerson(context, eperson)) {
            throw new AuthorizeException("Only the user with id: " + personId + " can make this action.");
        }

        context.turnOffAuthorisationSystem();

        eperson = Optional.ofNullable(eperson).orElseGet(() -> createEPerson(context, registrationData));
        updateValuesFromRegistration(context, eperson, registrationData, overrides);
        deleteToken(context, token);
        ePersonService.update(context, eperson);

        context.commit();
        context.restoreAuthSystemState();

        return eperson;
    }

    private EPerson createEPerson(Context context, RegistrationData registrationData) {
        EPerson eperson;
        try {
            eperson = ePersonService.create(context);

            eperson.setNetid(registrationData.getNetId());
            eperson.setEmail(registrationData.getEmail());

            RegistrationDataMetadata firstName =
                registrationDataService.getMetadataByMetadataString(
                    registrationData,
                    "eperson.firstname"
                );
            if (firstName != null) {
                eperson.setFirstName(context, firstName.getValue());
            }

            RegistrationDataMetadata lastName =
                registrationDataService.getMetadataByMetadataString(
                    registrationData,
                    "eperson.lastname"
                );
            if (lastName != null) {
                eperson.setLastName(context, lastName.getValue());
            }
            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(
                "Cannote create the eperson linked to the token: " + registrationData.getToken(),
                e
            );
        }
        return eperson;
    }

    private boolean hasLoggedEPerson(Context context) {
        return context.getCurrentUser() != null;
    }

    private boolean isSameContextEPerson(Context context, EPerson eperson) {
        return context.getCurrentUser().equals(eperson);
    }

    @Override
    public RegistrationData renewRegistrationForEmail(
        Context context, RegistrationDataPatch registrationDataPatch
    ) throws AuthorizeException {
        try {
            RegistrationData newRegistration = registrationDataService.clone(context, registrationDataPatch);
            registrationDataService.delete(context, registrationDataPatch.getOldRegistration());
            sendRegistationLinkByEmail(context, newRegistration);
            return newRegistration;
        } catch (SQLException | MessagingException | IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private boolean isEmailConfirmed(RegistrationData oldRegistration, String email) {
        return email.equals(oldRegistration.getEmail());
    }

    @Override
    public boolean isTokenValidForCreation(RegistrationData registrationData) {
        return (
                isExternalRegistrationToken(registrationData.getRegistrationType()) ||
                isValidationToken(registrationData.getRegistrationType())
            ) &&
            StringUtils.isNotBlank(registrationData.getNetId());
    }

    private boolean canCreateUserBy(Context context, RegistrationTypeEnum registrationTypeEnum) {
        return isValidationToken(registrationTypeEnum) ||
            canCreateUserFromExternalRegistrationToken(context, registrationTypeEnum);
    }

    private static boolean canCreateUserFromExternalRegistrationToken(
        Context context, RegistrationTypeEnum registrationTypeEnum
    ) {
        return context.getCurrentUser() != null && isExternalRegistrationToken(registrationTypeEnum);
    }

    private static boolean isExternalRegistrationToken(RegistrationTypeEnum registrationTypeEnum) {
        return RegistrationTypeEnum.ORCID.equals(registrationTypeEnum);
    }

    private static boolean isValidationToken(RegistrationTypeEnum registrationTypeEnum) {
        return RegistrationTypeEnum.VALIDATION_ORCID.equals(registrationTypeEnum);
    }


    /**
     * Updates Eperson using the provided {@link RegistrationData}.<br/>
     * Tries to replace {@code metadata} already set inside the {@link EPerson} with the ones
     * listed inside the {@code overrides} field by taking the value from the {@link RegistrationData}. <br/>
     * Updates the empty values inside the {@link EPerson} by taking them directly from the {@link RegistrationData},
     * according to the method {@link AccountServiceImpl#getUpdateActions(Context, EPerson, RegistrationData)}
     *
     * @param context The DSpace context
     * @param eperson The EPerson that should be updated
     * @param registrationData The RegistrationData related to that EPerson
     * @param overrides List of metadata that will be overwritten inside the EPerson
     */
    protected void updateValuesFromRegistration(
        Context context, EPerson eperson, RegistrationData registrationData, List<String> overrides
    ) {
        Stream.concat(
            getMergeActions(registrationData, overrides),
            getUpdateActions(context, eperson, registrationData)
        ).forEach(c -> c.accept(eperson));
    }

    private Stream<Consumer<EPerson>> getMergeActions(RegistrationData registrationData, List<String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return Stream.empty();
        }
        return overrides.stream().map(f -> mergeField(f, registrationData));
    }

    /**
     * This methods tries to fullfill missing values inside the {@link EPerson} by taking them directly from the
     * {@link RegistrationData}. <br/>
     * Returns a {@link Stream} of consumers that will be evaluated on an {@link EPerson}, this stream contains
     * the following actions:
     * <ul>
     *     <li>Copies {@code netId} and {@code email} to the {@link EPerson} <br/></li>
     *     <li>Copies any {@link RegistrationData#metadata} inside {@link EPerson#metadata} if isn't already set.</li>
     * </ul>
     *
     * @param context DSpace context
     * @param eperson EPerson that will be evaluated
     * @param registrationData RegistrationData used as a base to copy value from.
     * @return a stream of consumers to be evaluated on EPerson.
     */
    protected Stream<Consumer<EPerson>> getUpdateActions(
        Context context, EPerson eperson, RegistrationData registrationData
    ) {
        Stream.Builder<Consumer<EPerson>> actions = Stream.builder();
        if (eperson.getNetid() == null) {
            actions.add(p -> p.setNetid(registrationData.getNetId()));
        }
        if (eperson.getEmail() == null) {
            actions.add(p -> p.setEmail(registrationData.getEmail()));
        }
        for (RegistrationDataMetadata metadatum : registrationData.getMetadata()) {
            Optional<List<MetadataValue>> epersonMetadata =
                Optional.ofNullable(
                    ePersonService.getMetadataByMetadataString(
                        eperson, metadatum.getMetadataField().toString('.')
                    )
                ).filter(l -> !l.isEmpty());
            if (epersonMetadata.isEmpty()) {
                actions.add(p -> addMetadataValue(context, metadatum, p));
            }
        }
        return actions.build();
    }

    private List<MetadataValue> addMetadataValue(Context context, RegistrationDataMetadata metadatum, EPerson p) {
        try {
            return ePersonService.addMetadata(
                context, p, metadatum.getMetadataField(), Item.ANY, List.of(metadatum.getValue())
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                "Could not add metadata" + metadatum.getMetadataField() + " to eperson with uuid: " + p.getID(), e);
        }
    }

    /**
     * This method returns a Consumer that will override a given {@link MetadataValue} of the {@link EPerson} by taking
     * that directly from the {@link RegistrationData}.
     *
     * @param field             The metadatafield
     * @param registrationData  The RegistrationData where the metadata wil be taken
     * @return a Consumer of the person that will replace that field
     */
    protected Consumer<EPerson> mergeField(String field, RegistrationData registrationData) {
        return person ->
            allowedMergeArguments.getOrDefault(
                    field,
                    mergeRegistrationMetadata(field)
            ).accept(registrationData, person);
    }

    /**
     * This method returns a {@link BiConsumer} that can be evaluated on any {@link RegistrationData} and
     * {@link EPerson} in order to replace the value of the metadata {@code field} placed on the {@link EPerson}
     * by taking the value directly from the {@link RegistrationData}.
     *
     * @param field The metadata that will be overwritten inside the {@link EPerson}
     * @return a BiConsumer
     */
    protected BiConsumer<RegistrationData, EPerson> mergeRegistrationMetadata(String field) {
        return (registrationData, person) -> {
            RegistrationDataMetadata registrationMetadata = getMetadataOrThrow(registrationData, field);
            MetadataValue metadata = getMetadataOrThrow(person, field);
            metadata.setValue(registrationMetadata.getValue());
            ePersonService.setMetadataModified(person);
        };
    }

    private RegistrationDataMetadata getMetadataOrThrow(RegistrationData registrationData, String field) {
        return registrationDataService.getMetadataByMetadataString(registrationData, field);
    }

    private MetadataValue getMetadataOrThrow(EPerson eperson, String field) {
        return ePersonService.getMetadataByMetadataString(eperson, field).stream().findFirst()
                             .orElseThrow(
                                 () -> new IllegalArgumentException(
                                     "Could not find the metadata field: " + field + " for eperson: " + eperson.getID())
                             );
    }

    private RegistrationData getRegistrationData(Context context, String token)
        throws SQLException, AuthorizeException {
        return Optional.ofNullable(registrationDataService.findByToken(context, token))
                       .filter(rd ->
                                   isValid(rd) ||
                                   !isValidationToken(rd.getRegistrationType())
                       )
                       .orElseThrow(
                           () -> new AuthorizeException(
                               "The registration token: " + token + " is not valid!"
                           )
                       );
    }

    private boolean isValid(RegistrationData rd) {
        return registrationDataService.isValid(rd);
    }


    /**
     * THIS IS AN INTERNAL METHOD. THE SEND PARAMETER ALLOWS IT TO BE USED FOR
     * TESTING PURPOSES.
     *
     * Send an info to the EPerson with the given email address. If isRegister
     * is TRUE, this is registration email; otherwise, it is forgot-password
     * email. If send is TRUE, the email is sent; otherwise it is skipped.
     *
     * Potential error conditions:
     *
     * @param context    DSpace context
     * @param email      Email address to send the forgot-password email to
     * @param type       Type of registration {@link RegistrationTypeEnum}
     * @param send       If true, send email; otherwise do not send any email
     * @return null if no EPerson with that email found
     * @throws SQLException       Cannot create registration data in database
     * @throws MessagingException Error sending email
     * @throws IOException        Error reading email template
     * @throws AuthorizeException Authorization error
     */
    protected RegistrationData sendInfo(
        Context context, String email, RegistrationTypeEnum type, boolean send
    ) throws SQLException, IOException, MessagingException, AuthorizeException {
        // See if a registration token already exists for this user
        RegistrationData rd = registrationDataService.findBy(context, email, type);
        boolean isRegister = RegistrationTypeEnum.REGISTER.equals(type);

        // If it already exists, just re-issue it
        if (rd == null) {
            rd = registrationDataService.create(context);
            rd.setRegistrationType(type);
            rd.setToken(Utils.generateHexKey());

            // don't set expiration date any more
            //            rd.setColumn("expires", getDefaultExpirationDate());
            rd.setEmail(email);
            registrationDataService.update(context, rd);

            // This is a potential problem -- if we create the callback
            // and then crash, registration will get SNAFU-ed.
            // So FIRST leave some breadcrumbs
            if (log.isDebugEnabled()) {
                log.debug("Created callback "
                              + rd.getID()
                              + " with token " + rd.getToken()
                              + " with email \"" + email + "\"");
            }
        }

        if (send) {
            fillAndSendEmail(context, email, isRegister, rd);
        }

        return rd;
    }

    /**
     * Send a DSpace message to the given email address.
     *
     * If isRegister is <code>true</code>, this is registration email;
     * otherwise, it is a forgot-password email.
     *
     * @param context    The relevant DSpace Context.
     * @param email      The email address to mail to
     * @param isRegister If true, this is registration email; otherwise it is
     *                   forgot-password email.
     * @param rd         The RDBMS row representing the registration data.
     * @throws MessagingException If an error occurs while sending email
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     */
    protected void fillAndSendEmail(Context context, String email, boolean isRegister, RegistrationData rd)
        throws MessagingException, IOException, SQLException {
        String base = configurationService.getProperty("dspace.ui.url");

        //  Note change from "key=" to "token="
        String specialLink = new StringBuffer().append(base).append(
            base.endsWith("/") ? "" : "/").append(
            isRegister ? "register" : "forgot").append("/")
                                               .append(rd.getToken())
                                               .toString();
        Locale locale = context.getCurrentLocale();
        String emailFilename = I18nUtil.getEmailFilename(locale, isRegister ? "register" : "change_password");

        fillAndSendEmail(email, emailFilename, specialLink);

        // Breadcrumbs
        if (log.isInfoEnabled()) {
            log.info("Sent " + (isRegister ? "registration" : "account")
                         + " information to " + email);
        }
    }

    /**
     * This method returns a link that will point to the Angular UI that will be used by the user to complete the
     * registration process.
     *
     * @param base is the UI url of DSpace
     * @param rd is the RegistrationData related to the user
     * @param subPath is the specific page that will be loaded on the FE
     * @return String that represents that link
     */
    private static String getSpecialLink(String base, RegistrationData rd, String subPath) {
        return new StringBuffer(base)
            .append(base.endsWith("/") ? "" : "/")
            .append(subPath)
            .append("/")
            .append(rd.getToken())
            .toString();
    }

    /**
     * Fills out a given email template obtained starting from the {@link RegistrationTypeEnum}.
     *
     * @param context The DSpace Context
     * @param rd The RegistrationData that will be used as a registration.
     * @throws MessagingException
     * @throws IOException
     */
    protected void sendRegistationLinkByEmail(
        Context context, RegistrationData rd
    ) throws MessagingException, IOException {
        String base = configurationService.getProperty("dspace.ui.url");

        //  Note change from "key=" to "token="
        String specialLink = getSpecialLink(base, rd, rd.getRegistrationType().getLink());

        String emailFilename = I18nUtil.getEmailFilename(
            context.getCurrentLocale(), rd.getRegistrationType().toString().toLowerCase()
        );

        fillAndSendEmail(rd.getEmail(), emailFilename, specialLink);

        log.info(LogMessage.of(() -> "Sent " + rd.getRegistrationType().getLink() + " link to " + rd.getEmail()));
    }

    /**
     * This method fills out the given email with all the fields and sends out the email.
     *
     * @param email - The recipient
     * @param emailFilename The name of the email
     * @param specialLink - The link that will be set inside the email
     * @throws IOException
     * @throws MessagingException
     */
    protected void fillAndSendEmail(String email, String emailFilename, String specialLink)
        throws IOException, MessagingException {
        Email bean = Email.getEmail(emailFilename);
        bean.addRecipient(email);
        bean.addArgument(specialLink);
        bean.send();
    }
}
