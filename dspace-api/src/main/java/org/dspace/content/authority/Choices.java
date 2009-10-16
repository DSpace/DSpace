/*
 * Choices.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.authority;

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
     */
    public Choices(Choice values[], int start, int total, int confidence, boolean more)
    {
        super();
        this.values = values;
        this.start = start;
        this.total = total;
        this.confidence = confidence;
        this.more = more;
    }

    /**
     * Constructor for general purpose
     */
    public Choices(Choice values[], int start, int total, int confidence, boolean more, int defaultSelected)
    {
        super();
        this.values = values;
        this.start = start;
        this.total = total;
        this.confidence = confidence;
        this.more = more;
        this.defaultSelected = defaultSelected;
    }

    /**
     * Constructor for error results
     */
    public Choices(int confidence)
    {
        this.values = new Choice[0];
        this.confidence = confidence;
    }

    /**
     * Constructor for simple empty or error results
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
                return confidenceText[i];
            else if (confidenceValue[i] == CF_NOVALUE)
                novalue = confidenceText[i];
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
                return confidenceValue[i];
        }
        return dflt;
    }
}
