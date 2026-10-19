/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.mail.MessagingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.dto.RegistrationDataPatch;

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
public interface AccountService {
    public void sendRegistrationInfo(Context context, String email)
        throws SQLException, IOException, MessagingException, AuthorizeException;

    public void sendForgotPasswordInfo(Context context, String email)
        throws SQLException, IOException, MessagingException, AuthorizeException;

    /**
     * Checks if exists an account related to the token provided
     *
     * @param context DSpace context
     * @param token Account token
     * @return true if exists, false otherwise
     * @throws SQLException
     * @throws AuthorizeException
     */
    boolean existsAccountFor(Context context, String token)
        throws SQLException, AuthorizeException;

    /**
     * Checks if exists an account related to the email provided
     *
     * @param context DSpace context
     * @param email String email to search for
     * @return true if exists, false otherwise
     * @throws SQLException
     */
    boolean existsAccountWithEmail(Context context, String email)
        throws SQLException;

    public EPerson getEPerson(Context context, String token)
        throws SQLException, AuthorizeException;

    public String getEmail(Context context, String token) throws SQLException;

    public void deleteToken(Context context, String token) throws SQLException;

    /**
     * Merge registration data with an existing EPerson or create a new one.
     *
     * @param context    DSpace context
     * @param userId   The ID of the EPerson to merge with or create
     * @param token      The token to use for registration data
     * @param overrides  List of fields to override in the EPerson
     * @return           The merged or created EPerson
     * @throws AuthorizeException If the user is not authorized to perform the action
     * @throws SQLException       If a database error occurs
     */
    EPerson mergeRegistration(
        Context context,
        UUID userId,
        String token,
        List<String> overrides
    ) throws AuthorizeException, SQLException;

    /**
     * This method creates a fresh new {@link RegistrationData} based on the {@link RegistrationDataPatch} requested
     * by a given user.
     *
     * @param context - The DSapce Context
     * @param registrationDataPatch - Details of the patch request coming from the Controller
     * @return a newly created {@link RegistrationData}
     * @throws AuthorizeException
     */
    RegistrationData renewRegistrationForEmail(
        Context context,
        RegistrationDataPatch registrationDataPatch
    ) throws AuthorizeException;

    /**
     * Checks if the {@link RegistrationData#token} is valid.
     *
     * @param registrationData that will be checked
     * @return true if valid, false otherwise
     */
    boolean isTokenValidForCreation(RegistrationData registrationData);
}
