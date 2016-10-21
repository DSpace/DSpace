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

    /** The column number */
    private int column;

    /** Error with the schema */
    public static final int SCHEMA = 0;

    /** Error with the element */
    public static final int ELEMENT = 1;

    /** Error with a missing header */
    public static final int MISSING = 98;

    /** Error with the whole entry */
    public static final int ENTRY = 99;


    /**
     * Instantiate a new MetadataImportInvalidHeadingException
     *
     * @param message the error message
     * @param theType the type of the error
     */
    public MetadataImportInvalidHeadingException(String message, int theType, int theColumn)
    {
        super(message);
        badHeading = message;
        type = theType;
        column = theColumn;
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
     * Get the column number that was invalid
     *
     * @return the invalid column number
     */
    public int getColumn()
    {
        return column;
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
            return "Unknown metadata schema in column " + column + ": " + badHeading;
        } else if (type == ELEMENT)
        {
            return "Unknown metadata element in column " + column + ": " + badHeading;
        } else if (type == MISSING)
        {
            return "Row with missing header: column " + column;
        } else
        {
            return "Bad metadata declaration in column" + column + ": " + badHeading;
        }
    }
}
