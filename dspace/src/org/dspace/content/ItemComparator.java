/*
 * ItemComparator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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


package org.dspace.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dspace.browse.Browse;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 * Compare two Items by their DCValues.
 *
 * The DCValues to be compared are specified by the element, qualifier
 * and language parameters to the constructor. If the Item has more
 * than one matching DCValue, then the max parameter to the constructor
 * specifies whether the maximum or minimum lexicographic value will be used.
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class ItemComparator implements Comparator
{
    private String element;
    private String qualifier;
    private String language;
    private boolean max;

    /**
     * Constructor.
     *
     * @param element The Dublin Core element
     * @param qualifier The Dublin Core qualifier
     * @param language The language for the DCValues
     * @param max If true, and there is more than one DCValue for
     * element, qualifier and language, then use the maximum value
     * lexicographically; otherwise use the minimum value.
     */
    public ItemComparator (String element,
                           String qualifier,
                           String language,
                           boolean max)
    {
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.max      = max;
    }

    /**
     * Compare two Items by checking their DCValues for element,
     * qualifier, and language.
     *
     * Return >= 1 if the first is lexicographically greater than
     * the second; <= -1 if the second is lexicographically greater
     * than the first, and 0 otherwise.
     */
    public int compare(Object first, Object second)
    {
        if ((! (first  instanceof Item)) ||
            (! (second instanceof Item)))
            throw new IllegalArgumentException("Arguments must be Items");

        Item firstItem = (Item) first;
        Item secondItem = (Item) second;

        // Retrieve a chosen value from the array for comparison
        String firstValue = getValue(firstItem.getDC(element, qualifier, language));
        String secondValue = getValue(secondItem.getDC(element, qualifier, language));

        // Normalize titles for comparison purposes
        if ("title".equals(element))
        {
            firstValue = Browse.getNormalizedTitle(firstValue, language);
            secondValue = Browse.getNormalizedTitle(secondValue, language);
        }

        if ((firstValue == null) && (secondValue == null))
            return 0;
        if ((firstValue != null) && (secondValue == null))
            return 1;
        if ((firstValue == null) && (secondValue != null))
            return -1;

        // See the javadoc for java.lang.String for an explanation
        // of the return value.
        return firstValue.compareTo(secondValue);
    }

    /**
     * Return true if the object is equal to this one, false
     * otherwise. Another object is equal to this one if it is also
     * an ItemComparator, and has the same values for element,
     * qualifier, language, and max.
     *
     * @param obj The object to compare to.
     * @return True if the other object is equal to this one, false otherwise.
     */
    public boolean equals(Object obj)
    {
        if (! (obj instanceof ItemComparator))
            return false;

        ItemComparator other = (ItemComparator) obj;

        return
            _equals(element,   other.element) &&
            _equals(qualifier, other.qualifier) &&
            _equals(language,  other.language) &&
            max == other.max
            ;
    }

    /**
     * Return true if the first string is equal to the second.
     * Either or both may be null.
     */
    private boolean _equals(String first, String second)
    {
        if ((first == null) && (second == null))
            return true;
        if ((first != null) && (second == null))
            return false;
        if ((first == null) && (second != null))
            return false;

        return first.equals(second);
    }

    /**
     * Choose the canonical value from values for comparison.
     * If there are no values, null is returned.
     * If there is exactly one value, then it is returned.
     * Otherwise, either the maximum or minimum lexicographical
     * value is returned; the parameter to the constructor says which.
     *
     * @param dcvalues The values to check
     * @return The chosen value, or null
     */
    private String getValue(DCValue[] dcvalues)
    {
        if ((dcvalues == null) || (dcvalues.length == 0))
            return null;
        if (dcvalues.length == 1)
            return dcvalues[0] == null ? null : dcvalues[0].value;

        List values = new ArrayList();
        for (int i = 0; i < dcvalues.length; i++ )
        {
            String value = dcvalues[i].value;
            if (value != null)
                values.add(value);
        }

        if (values.size() == 0)
            return null;

        return max ? (String) Collections.max(values) :
            (String) Collections.min(values);
    }
}
