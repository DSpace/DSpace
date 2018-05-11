/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * Series and report number, as stored in relation.ispartofseries
 * 
 * @author Robert Tansley
 * @version $Id$
 */
public class DCSeriesNumber
{
    /** Series */
    private String series;

    /** Number */
    private String number;

    /** Construct clean series number */
    public DCSeriesNumber()
    {
        series = null;
        number = null;
    }

    /**
     * Construct from raw DC value
     * 
     * @param value
     *            value from database
     */
    public DCSeriesNumber(String value)
    {
        this();

        int semicolon = -1;

        if (value != null)
        {
            semicolon = value.indexOf(';');
        }

        if (semicolon >= 0)
        {
            series = value.substring(0, semicolon);
            number = value.substring(semicolon + 1);
        }
        else
        {
            series = value;
        }
    }

    /**
     * Construct from given values
     * 
     * @param s
     *            the series
     * @param n
     *            the number
     */
    public DCSeriesNumber(String s, String n)
    {
        series = s;
        number = n;
    }

    /**
     * Write as raw DC value
     * 
     * @return the series and number as they should be stored in the DB
     */
    public String toString()
    {
        if (series == null)
        {
            return (null);
        }
        else if (number == null)
        {
            return (series);
        }
        else
        {
            return (series + ";" + number);
        }
    }

    /**
     * Get the series name - guaranteed non-null
     * @return name
     */
    public String getSeries()
    {
        return ((series == null) ? "" : series);
    }

    /**
     * Get the number - guaranteed non-null
     * @return number
     */
    public String getNumber()
    {
        return ((number == null) ? "" : number);
    }
}
