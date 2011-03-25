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
public class MetadataImportInvalidHeadingException extends Exception
{
    /** The type of error (schema or element) */
    private int type;

    /** The bad heading */
    private String badHeading;

    /** Error with the schema */
    public static final int SCHEMA = 0;

    /** Error with the element */
    public static final int ELEMENT = 1;


    /**
     * Instantiate a new MetadataImportInvalidHeadingException
     *
     * @param message the error message
     * @param theType the type of the error
     */
    public MetadataImportInvalidHeadingException(String message, int theType)
    {
        super(message);
        badHeading = message;
        type = theType;
    }

    /**
     * Get the type of the exception
     *
     *  @return the type of the exception
     */
    public String getType()
    {
        return "" + type;
    }

    /**
     * Get the heading that was invalid
     *
     * @return the invalid heading
     */
    public String getBadHeader()
    {
        return badHeading;
    }

    /**
     * Get the exception message
     *
     * @return The exception message
     */
    public String getMessage()
    {
        if (type == SCHEMA)
        {
            return "Unknown metadata schema in heading: " + badHeading;
        }
        else
        {
            return "Unknown metadata element in heading: " + badHeading;
        }
    }
}