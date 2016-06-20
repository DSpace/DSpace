/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.commons.lang.ArrayUtils;

/**
 * Record class to hold a set of Choices returned by an authority in response
 * to a search.
 *
 * @author Larry Stone
 * @see Choice
 */
public class Choices
{

    /** -------------- Class fields ----------------- **/

    /** Canonical values of the confidence metric.  Higher is better. */

    /**  This authority value has been confirmed as accurate by an
         interactive user or authoritative policy */
    public static final int CF_ACCEPTED = 600;

    /**  Value is singular and valid but has not been seen and accepted
         by a human, so its provenance is uncertain.  */
    public static final int CF_UNCERTAIN = 500;

    /**  There are multiple matching authority values of equal validity. */
    public static final int CF_AMBIGUOUS = 400;

    /**  There are no matching answers from the authority. */
    public static final int CF_NOTFOUND = 300;

    /**  The authority encountered an internal failure - this preserves a
         record in the metadata of why there is no value. */
    public static final int CF_FAILED = 200;

    /**  The authority recommends this submission be rejected. */
    public static final int CF_REJECTED = 100;

    /**  No reasonable confidence value is available */
    public static final int CF_NOVALUE = 0;

    /**  Value has not been set (DB default). */
    public static final int CF_UNSET = -1;

    /** descriptive labels for confidence values */
    private static final int confidenceValue[] = {
        CF_UNSET,
        CF_NOVALUE,
        CF_REJECTED,
        CF_FAILED,
        CF_NOTFOUND,
        CF_AMBIGUOUS,
        CF_UNCERTAIN,
        CF_ACCEPTED,
    };
    private static final String confidenceText[] = {
        "UNSET",
        "NOVALUE",
        "REJECTED",
        "FAILED",
        "NOTFOUND",
        "AMBIGUOUS",
        "UNCERTAIN",
        "ACCEPTED"
    };

    /** -------------- Instance fields ----------------- **/

    /** The set of values returned by the authority */
    public Choice values[] = null;

    /** The confidence level that applies to all values in this result set */
    public int confidence = CF_NOVALUE;

    /** Index of start of this result wrt.  all results; 0 is start of
        complete result.  Note that length is implicit in values.length. */
    public int start = 0;

    /** Count of total results available */
    public int total = 0;

    /** Index of value to be selected by default, if any. -1 means none selected. */
    public int defaultSelected = -1;

    /** true when there are more values to be sent after this result. */
    public boolean more = false;

    /** -------------- Methods ----------------- **/

    /**
     * Constructor for general purpose
     * @param values values array
     * @param start start number
     * @param total total results
     * @param confidence confidence level
     * @param more whether more values
     */
    public Choices(Choice values[], int start, int total, int confidence, boolean more)
    {
        super();
        this.values = (Choice[]) ArrayUtils.clone(values);
        this.start = start;
        this.total = total;
        this.confidence = confidence;
        this.more = more;
    }

    /**
     * Constructor for general purpose
     * @param values values array
     * @param start start number
     * @param total total results
     * @param confidence confidence level
     * @param more whether more values
     * @param defaultSelected default selected value
     */
    public Choices(Choice values[], int start, int total, int confidence, boolean more, int defaultSelected)
    {
        super();
        this.values = (Choice[]) ArrayUtils.clone(values);
        this.start = start;
        this.total = total;
        this.confidence = confidence;
        this.more = more;
        this.defaultSelected = defaultSelected;
    }

    /**
     * Constructor for error results
     * @param confidence confidence level
     */
    public Choices(int confidence)
    {
        this.values = new Choice[0];
        this.confidence = confidence;
    }

    /**
     * Constructor for simple empty or error results
     * @param isError whether error
     */
    public Choices(boolean isError)
    {
        this.values = new Choice[0];
        this.confidence = isError ? CF_FAILED : CF_NOVALUE;
    }

    /**
     * Predicate,  did this result encounter an error?
     * @return true if this Choices result encountered an error
     */
    public boolean isError()
    {
        return confidence == CF_FAILED || confidence == CF_REJECTED;
    }

    /**
     * Get the symbolic name corresponding to a confidence value, or CF_NOVALUE's
     * name if the value is unknown.
     *
     * @param cv confidence value
     * @return String with symbolic name corresponding to value (never null)
     */
    public static String getConfidenceText(int cv)
    {
        String novalue = null;
        for (int i = 0; i < confidenceValue.length; ++i)
        {
            if (confidenceValue[i] == cv)
            {
                return confidenceText[i];
            }
            else if (confidenceValue[i] == CF_NOVALUE)
            {
                novalue = confidenceText[i];
            }
        }
        return novalue;
    }

    /**
     * Get the value corresponding to a symbolic name of a confidence
     * value, or CF_NOVALUE if the symbol is unknown.
     *
     * @param ct symbolic name in String
     * @return corresponding value or CF_NOVALUE if not found
     */
    public static int getConfidenceValue(String ct)
    {
        return getConfidenceValue(ct, CF_NOVALUE);
    }

    /**
     * Get the value corresponding to a symbolic name of a confidence
     * value, or the given default if the symbol is unknown.  This
     * lets an application detect invalid data, e.g. in a configuration file.
     *
     * @param ct symbolic name in String
     * @param dflt the default value to return
     * @return corresponding value or CF_NOVALUE if not found
     */
    public static int getConfidenceValue(String ct, int dflt)
    {
        for (int i = 0; i < confidenceText.length; ++i)
        {
            if (confidenceText[i].equalsIgnoreCase(ct))
            {
                return confidenceValue[i];
            }
        }
        return dflt;
    }
}
