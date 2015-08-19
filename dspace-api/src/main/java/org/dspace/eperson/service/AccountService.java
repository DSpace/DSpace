/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;

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
public interface AccountService {

    public void sendRegistrationInfo(Context context, String email) throws SQLException, IOException, MessagingException, AuthorizeException;

    public void sendForgotPasswordInfo(Context context, String email) throws SQLException, IOException, MessagingException, AuthorizeException;

    public EPerson getEPerson(Context context, String token)
                throws SQLException, AuthorizeException;


    public String getEmail(Context context, String token)
                throws SQLException;

    public void deleteToken(Context context, String token)
                throws SQLException;
}
