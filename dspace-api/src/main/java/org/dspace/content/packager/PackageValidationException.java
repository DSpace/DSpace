/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

/**
 * <p>
 * This represents a failure when importing or exporting a package
 * caused by invalid unacceptable package format or contents; for
 * example, missing files that were mentioned in the manifest, or
 * extra files not in manifest, or lack of a manifest.
 * </p>
 * <p>
 * When throwing a PackageValidationException, be sure the message
 * includes enough specific information to let the end user diagnose
 * the problem, i.e. what files appear to be missing from the manifest
 * or package, or the details of a checksum error on a file.
 * </p>
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class PackageValidationException extends PackageException
{
    /**
     * Create a new exception with the given message.
     * @param message - diagnostic message.
     */
    public PackageValidationException(String message)
    {
        super(message);
    }

    /**
     * Create a new exception wrapping it around another exception.
     * @param exception - exception specifying the cause of this failure.
     */
    public PackageValidationException(Exception exception)
    {
        super(exception);
    }

    public PackageValidationException(String message, Exception exception)
    {
        super(message, exception);
    }
}
