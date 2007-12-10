/*
 * AccountManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.eperson;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.dspace.eperson.dao.RegistrationDataDAOFactory;

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
public class AccountManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(AccountManager.class);

    /** Protected Constructor */
    protected AccountManager()
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
    public static void sendRegistrationInfo(Context context, String email)
            throws IOException, MessagingException
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
    public static void sendForgotPasswordInfo(Context context, String email)
            throws IOException, MessagingException
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
     * @exception SQLException
     *                If the token or eperson cannot be retrieved from the
     *                database.
     */
    public static EPerson getEPerson(Context context, String token)
    {
        String email = getEmail(context, token);

        if (email == null)
        {
            return null;
        }

        EPerson ep = EPerson.findByEmail(context, email);

        return ep;
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
    public static String getEmail(Context context, String token)
    {
        RegistrationDataDAO dao =
            RegistrationDataDAOFactory.getInstance(context);

        RegistrationData rd = dao.retrieveByToken(token);

        if (rd == null)
        {
            return null;
        }

        return rd.getEmail();
    }

    /**
     * Delete token.
     * 
     * @param context
     *            DSpace context
     * @param token
     *            The token to delete
     * @exception SQLException
     *                If a database error occurs
     */
    public static void deleteToken(Context context, String token)
    {
        RegistrationDataDAOFactory.getInstance(context).delete(token);
    }

    /*
     * THIS IS AN INTERNAL METHOD. THE SEND PARAMETER ALLOWS IT TO BE USED FOR
     * TESTING PURPOSES.
     * 
     * Send an info to the EPerson with the given email address. If isRegister
     * is TRUE, this is registration email; otherwise, it is forgot-password
     * email. If send is TRUE, the email is sent; otherwise it is skipped.
     * 
     * Potential error conditions: No EPerson with that email (returns null)
     * Cannot create registration data in database (throws SQLException) Error
     * sending email (throws MessagingException) Error reading email template
     * (throws IOException) Authorization error (throws AuthorizeException)
     * 
     * @param context DSpace context @param email Email address to send the
     * forgot-password email to @param isRegister If true, this is for
     * registration; otherwise, it is for forgot-password @param send If true,
     * send email; otherwise do not send any email
     */
    protected static void sendInfo(Context context, String email,
            boolean isRegister, boolean send)
        throws IOException, MessagingException
    {
        // See if a registration token already exists for this user
        RegistrationDataDAO dao =
            RegistrationDataDAOFactory.getInstance(context);

        RegistrationData rd = dao.retrieveByEmail(email);

        // If it already exists, just re-issue it
        if (rd == null)
        {
            rd = dao.create();
            rd.setToken(Utils.generateHexKey());

            // don't set expiration date any more
            // rd.setExpiryDate(getDefaultExpirationDate());
            rd.setEmail(email);
            dao.update(rd);

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
    }

    /**
     * Send a DSpace message to the given email address.
     * 
     * If isRegister is <code>true</code>, this is registration email;
     * otherwise, it is a forgot-password email.
     * 
     * @param email
     *            The email address to mail to
     * @param isRegister
     *            If true, this is registration email; otherwise it is
     *            forgot-password email.
     * @param rd
     *            The RDBMS row representing the registration data.
     * @exception MessagingException
     *                If an error occurs while sending email
     * @exception IOException
     *                If an error occurs while reading the email template.
     */
    private static void sendEmail(Context context, String email,
            boolean isRegister, RegistrationData rd)
        throws IOException, MessagingException
    {
        String base = ConfigurationManager.getProperty("dspace.url");

        //  Note change from "key=" to "token="
        String specialLink = new StringBuffer().append(base).append(
                base.endsWith("/") ? "" : "/").append(
                isRegister ? "register" : "forgot").append("?")
                .append("token=").append(rd.getToken())
                .toString();
        Locale locale = context.getCurrentLocale();
        Email bean = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(locale, isRegister ? "register"
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

    /**
     * Return the date on which registrations expire.
     * 
     * @return - The date on which registrations expire
     */
    private static Timestamp getDefaultExpirationDate()
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new java.util.Date());

        // Add 1 year from today
        calendar.add(Calendar.WEEK_OF_YEAR, 52);

        return new java.sql.Timestamp(calendar.getTime().getTime());
    }
}
