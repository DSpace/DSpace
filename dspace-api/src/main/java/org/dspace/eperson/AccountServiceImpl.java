/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.RegistrationDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

/**
 *
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
public class AccountServiceImpl implements AccountService
{
    /** log4j log */
    private static Logger log = Logger.getLogger(AccountServiceImpl.class);
    @Autowired(required = true)
    protected EPersonService ePersonService;
    @Autowired(required = true)
    protected RegistrationDataService registrationDataService;

    protected AccountServiceImpl()
    {

    }

    /**
     * Email registration info to the given email address.
     *
     * Potential error conditions: Cannot create registration data in database
     * (throws SQLException) Error sending email (throws MessagingException)
     * Error reading email template (throws IOException) Authorization error
     * (throws AuthorizeException)
     *
     * @param context
     *            DSpace context
     * @param email
     *            Email address to send the registration email to
     */
    @Override
    public void sendRegistrationInfo(Context context, String email)
            throws SQLException, IOException, MessagingException,
            AuthorizeException
    {
        sendInfo(context, email, true, true);
    }

    /**
     * Email forgot password info to the given email address.
     *
     * Potential error conditions: No EPerson with that email (returns null)
     * Cannot create registration data in database (throws SQLException) Error
     * sending email (throws MessagingException) Error reading email template
     * (throws IOException) Authorization error (throws AuthorizeException)
     *
     * @param context
     *            DSpace context
     * @param email
     *            Email address to send the forgot-password email to
     */
    @Override
    public void sendForgotPasswordInfo(Context context, String email)
            throws SQLException, IOException, MessagingException,
            AuthorizeException
    {
        sendInfo(context, email, false, true);
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
     * @param context
     *            DSpace context
     * @param token
     *            Account token
     * @return The EPerson corresponding to token, or null.
     * @throws SQLException
     *                If the token or eperson cannot be retrieved from the
     *                database.
     */
    @Override
    public EPerson getEPerson(Context context, String token)
            throws SQLException, AuthorizeException
    {
        String email = getEmail(context, token);

        if (email == null)
        {
            return null;
        }

        return ePersonService.findByEmail(context, email);
    }

    /**
     * Return the e-mail address referred to by a token, or null if email
     * address can't be found ignores expiration of token
     *
     * @param context
     *            DSpace context
     * @param token
     *            Account token
     * @return The email address corresponding to token, or null.
     */
    @Override
    public String getEmail(Context context, String token)
            throws SQLException
    {
        RegistrationData registrationData = registrationDataService.findByToken(context, token);

        if (registrationData == null)
        {
            return null;
        }

        /*
         * ignore the expiration date on tokens Date expires =
         * rd.getDateColumn("expires"); if (expires != null) { if ((new
         * java.util.Date()).after(expires)) return null; }
         */
        return registrationData.getEmail();
    }

    /**
     * Delete token.
     *
     * @param context
     *            DSpace context
     * @param token
     *            The token to delete
     * @throws SQLException
     *                If a database error occurs
     */
    @Override
    public void deleteToken(Context context, String token)
            throws SQLException
    {
        registrationDataService.deleteByToken(context, token);
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
     * @return null if no EPerson with that email found
     * @throws SQLException Cannot create registration data in database 
     * @throws MessagingException Error sending email
     * @throws IOException Error reading email template
     * @throws AuthorizeException Authorization error
     *
     * @param context DSpace context
     * @param email Email address to send the forgot-password email to
     * @param isRegister If true, this is for registration; otherwise, it is
     *     for forgot-password 
     * @param send If true, send email; otherwise do not send any email
     */
    protected RegistrationData sendInfo(Context context, String email,
            boolean isRegister, boolean send) throws SQLException, IOException,
            MessagingException, AuthorizeException
    {
        // See if a registration token already exists for this user
        RegistrationData rd = registrationDataService.findByEmail(context, email);


        // If it already exists, just re-issue it
        if (rd == null)
        {
            rd = registrationDataService.create(context);
            rd.setToken(Utils.generateHexKey());

            // don't set expiration date any more
            //            rd.setColumn("expires", getDefaultExpirationDate());
            rd.setEmail(email);
            registrationDataService.update(context, rd);

            // This is a potential problem -- if we create the callback
            // and then crash, registration will get SNAFU-ed.
            // So FIRST leave some breadcrumbs
            if (log.isDebugEnabled())
            {
                log.debug("Created callback "
                        + rd.getID()
                        + " with token " + rd.getToken()
                        + " with email \"" + email + "\"");
            }
        }

        if (send)
        {
            sendEmail(context, email, isRegister, rd);
        }

        return rd;
    }

    /**
     * Send a DSpace message to the given email address.
     *
     * If isRegister is <code>true</code>, this is registration email;
     * otherwise, it is a forgot-password email.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param email
     *     The email address to mail to
     * @param isRegister
     *     If true, this is registration email; otherwise it is
     *     forgot-password email.
     * @param rd
     *     The RDBMS row representing the registration data.
     * @throws MessagingException
     *     If an error occurs while sending email
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    protected void sendEmail(Context context, String email, boolean isRegister, RegistrationData rd)
            throws MessagingException, IOException, SQLException
    {
        String base = ConfigurationManager.getProperty("dspace.url");

        //  Note change from "key=" to "token="
        String specialLink = new StringBuffer().append(base).append(
                base.endsWith("/") ? "" : "/").append(
                isRegister ? "register" : "forgot").append("?")
                .append("token=").append(rd.getToken())
                .toString();
        Locale locale = context.getCurrentLocale();
        Email bean = Email.getEmail(I18nUtil.getEmailFilename(locale, isRegister ? "register"
                : "change_password"));
        bean.addRecipient(email);
        bean.addArgument(specialLink);
        bean.send();

        // Breadcrumbs
        if (log.isInfoEnabled())
        {
            log.info("Sent " + (isRegister ? "registration" : "account")
                    + " information to " + email);
        }
    }
}
