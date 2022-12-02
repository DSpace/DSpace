/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

/**
 * If the Clarin License which the bitstream is attached to needs the required info which the current user
 * hasn't filled in.
 */
public class MissingLicenseAgreementException extends AuthorizeException {
    public static String NAME = "MissingLicenseAgreementException";
    public MissingLicenseAgreementException(String message) {
        super(message);
    }
}
