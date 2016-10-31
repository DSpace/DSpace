/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * An exception that gets thrown when a metadata field cannot be created or
 * saved due to an existing field with an identical element and qualifier.
 * 
 * @author Martin Hald
 */
public class NonUniqueMetadataException extends Exception
{
    /**
     * Create an empty authorize exception
     */
    public NonUniqueMetadataException()
    {
        super();
    }

    /**
     * Create an exception with only a message
     * 
     * @param message
     *     message string
     */
    public NonUniqueMetadataException(String message)
    {
        super(message);
    }
}
