/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

/**
 * Metadata importer exception
 *
 * @author Stuart Lewis
 */
public class MetadataImportException extends Exception
{
    /**
     * Instantiate a new MetadataImportException
     *
     * @param message the error message
     */
    public MetadataImportException(String message)
    {
       super(message);
    }

    /**
     * Instantiate a new MetadataImportException
     *
     * @param message the error message
     * @param exception the root cause
     */
    public MetadataImportException(String message, Exception exception)
    {
       super(message, exception);
    }
}