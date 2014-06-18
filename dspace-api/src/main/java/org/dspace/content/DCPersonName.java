/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * DSpace person name utility class
 * <P>
 * Person names in the Dublin Core value table in the DSpace database are stored
 * in the following simple format:
 * <P>
 * <code>Lastname, First name(s)</code>
 * <P>
 * <em>FIXME:  No policy for dealing with "van"/"van der" and "Jr."</em>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class DCPersonName
{
    /** The person's last name */
    private String lastName;

    /** The person's first name(s) */
    private String firstNames;

    /** Construct a blank name */
    public DCPersonName()
    {
        lastName = null;
        firstNames = null;
    }

    /**
     * Construct a name from a raw DC value
     * 
     * @param rawValue
     *            the value entry from the database
     */
    public DCPersonName(String rawValue)
    {
        // Null by default (representing noone)
        lastName = null;
        firstNames = null;

        // Check we've actually been passed a name
        if ((rawValue != null) && !rawValue.equals(""))
        {
            // Extract the last name and first name components
            int commaIndex = rawValue.indexOf(',');

            // Just in case there's no comma, assume whole thing is
            // last name
            if (commaIndex == -1)
            {
                commaIndex = rawValue.length();
            }

            lastName = rawValue.substring(0, commaIndex).trim();

            // Just in case the first name is blank
            if (rawValue.length() > (commaIndex + 1))
            {
                firstNames = rawValue.substring(commaIndex + 1).trim();
            }
            else
            {
                // Since we have a name, we don't want to
                // leave the first name as null
                firstNames = "";
            }
        }
    }

    /**
     * Construct a name from a last name and first name
     * 
     * @param lastNameIn
     *            the last name
     * @param firstNamesIn
     *            the first names
     */
    public DCPersonName(String lastNameIn, String firstNamesIn)
    {
        lastName = lastNameIn;
        firstNames = firstNamesIn;
    }

    /**
     * Return a string for writing the name to the database
     * 
     * @return the name, suitable for putting in the database
     */
    public String toString()
    {
        StringBuffer out = new StringBuffer();

        if (lastName != null)
        {
            out.append(lastName);

            if ((firstNames != null) && !firstNames.equals(""))
            {
                out.append(", ").append(firstNames);
            }
        }

        return (out.toString());
    }

    /**
     * Get the first name(s). Guaranteed non-null.
     * 
     * @return the first name(s), or an empty string if none
     */
    public String getFirstNames()
    {
        return ((firstNames == null) ? "" : firstNames);
    }

    /**
     * Get the last name. Guaranteed non-null.
     * 
     * @return the last name, or an empty string if none
     */
    public String getLastName()
    {
        return ((lastName == null) ? "" : lastName);
    }
}
