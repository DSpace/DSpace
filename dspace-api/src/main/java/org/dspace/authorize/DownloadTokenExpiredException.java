/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

/**
 * If the token for downloading the bitstream with attached Clarin License is expired.
 */
public class DownloadTokenExpiredException extends AuthorizeException {
    public static String NAME = "DownloadTokenExpiredException";

    public DownloadTokenExpiredException(String message) {
        super(message);
    }
}
