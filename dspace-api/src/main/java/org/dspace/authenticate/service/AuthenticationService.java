/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.service;

import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Access point for the stackable authentication methods.
 * <p>
 * This class initializes the "stack" from the DSpace configuration,
 * and then invokes methods in the appropriate order on behalf of clients.
 * <p>
 * See the AuthenticationMethod interface for details about what each
 * function does.
 * <p>
 * <b>Configuration</b><br>
 * The stack of authentication methods is defined by one property in the DSpace configuration:
 * <pre>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = <em>a list of method class names</em>
 *     <em>e.g.</em>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = \
 *       org.dspace.eperson.X509Authentication, \
 *       org.dspace.eperson.PasswordAuthentication
 * </pre>
 * <p>
 * The "stack" is always traversed in order, with the methods
 * specified first (in the configuration) thus getting highest priority.
 *
 * @see AuthenticationMethod
 *
 * @author Larry Stone
 * @version $Revision$
 */
public interface AuthenticationService {


    /**
     * Test credentials for authenticity.
     * Apply the given credentials to each authenticate() method in
     * the stack.  Returns upon the first <code>SUCCESS</code>, or otherwise
     * returns the most favorable outcome from one of the methods.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param username
     *  Username (or email address) when method is explicit. Use null for
     *  implicit method.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but credentials (e.g. password) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/password not appropriate for this method
     */
    public int authenticate(Context context,
                                   String username,
                                   String password,
                                   String realm,
                                   HttpServletRequest request);

    /**
     * Test credentials for authenticity, using only Implicit methods.
     * Just like <code>authenticate()</code>, except it only invokes the
     * <em>implicit</em> authentication methods the stack.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param username
     *  Username (or email address) when method is explicit. Use null for
     *  implicit method.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but credentials (e.g. password) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/password not appropriate for this method
     */
    public int authenticateImplicit(Context context,
                                           String username,
                                           String password,
                                           String realm,
                                           HttpServletRequest request);


    /**
     * Predicate, can a new EPerson be created.
     * Invokes <code>canSelfRegister()</code> of every authentication
     * method in the stack, and returns true if any of them is true.
     *
     * @param context DSpace context
     * @param request HTTP request, in case it's needed. Can be null.
     * @param username Username, if available.  Can be null.
     * @return true if new ePerson should be created.
     * @throws SQLException if database error
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username) throws SQLException;

    /**
     * Predicate, can user set EPerson password.
     * Returns true if the <code>allowSetPassword()</code> method of any
     * member of the stack returns true.
     *
     * @param context DSpace context
     * @param request HTTP request, in case it's needed. Can be null.
     * @param username Username, if available.  Can be null.
     * @return true if this method allows user to change ePerson password.
     * @throws SQLException if database error
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username) throws SQLException;

    public void initEPerson(Context context,
                            HttpServletRequest request,
                            EPerson eperson)
            throws SQLException;


    /**
     * Get list of extra groups that user implicitly belongs to.
     * Returns accumulation of groups of all the <code>getSpecialGroups()</code>
     * methods in the stack.
     *
     * @param context A valid DSpace context.
     *
     * @param request The request that started this operation, or null if not applicable.
     *
     * @return Returns IDs of any groups the user authenticated by this
     * request is in implicitly -- checks for e.g. network-address dependent
     * groups.
     * @throws SQLException if database error
     */
    public List<Group> getSpecialGroups(Context context,
                                  HttpServletRequest request) throws SQLException;

    /**
     * Get stack of authentication methods.
     * Return an <code>Iterator</code> that steps through each configured
     * authentication method, in order of precedence.
     *
     * @return Iterator object.
     */
    public Iterator<AuthenticationMethod> authenticationMethodIterator();

}
