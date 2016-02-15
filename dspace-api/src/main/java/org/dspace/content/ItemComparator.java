/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.dspace.sort.OrderFormat;

/**
 * Compare two Items by their DCValues.
 * 
 * The DCValues to be compared are specified by the element, qualifier and
 language parameters to the constructor. If the Item has more than one
 matching Metadatum, then the max parameter to the constructor specifies whether
 the maximum or minimum lexicographic value will be used.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class ItemComparator implements Comparator, Serializable
{
    /** Dublin Core element */
    private String element;

    /** Dublin Core qualifier */
    private String qualifier;

    /** Language */
    private String language;

    /** Whether maximum or minimum value will be used */
    private boolean max;

    /**
     * Constructor.
     * 
     * @param element
     *            The Dublin Core element
     * @param qualifier
     *            The Dublin Core qualifier
     * @param language
     *            The language for the DCValues
     * @param max
     *            If true, and there is more than one Metadatum for element,
            qualifier and language, then use the maximum value
            lexicographically; otherwise use the minimum value.
     */
    public ItemComparator(String element, String qualifier, String language,
            boolean max)
    {
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.max = max;
    }

    /**
     * Compare two Items by checking their DCValues for element, qualifier, and
     * language.
     * 
     * <p>
     * Return >= 1 if the first is lexicographically greater than the second; <=
     * -1 if the second is lexicographically greater than the first, and 0
     * otherwise.
     * </p>
     * 
     * @param first
     *            The first object to compare. Must be an object of type
     *            org.dspace.content.Item.
     * @param second
     *            The second object to compare. Must be an object of type
     *            org.dspace.content.Item.
     * @return >= 1 if the first is lexicographically greater than the second; <=
     *         -1 if the second is lexicographically greater than the first, and
     *         0 otherwise.
     */
    public int compare(Object first, Object second)
    {
        if ((!(first instanceof Item)) || (!(second instanceof Item)))
        {
            throw new IllegalArgumentException("Arguments must be Items");
        }

        // Retrieve a chosen value from the array for comparison
        String firstValue = getValue((Item) first);
        String secondValue = getValue((Item) second);

        if (firstValue == null && secondValue == null)
        {
            return 0;
        }

        if (firstValue == null)
        {
            return -1;
        }

        if (secondValue == null)
        {
            return 1;
        }

        // See the javadoc for java.lang.String for an explanation
        // of the return value.
        return firstValue.compareTo(secondValue);
    }

    /**
     * Return true if the object is equal to this one, false otherwise. Another
     * object is equal to this one if it is also an ItemComparator, and has the
     * same values for element, qualifier, language, and max.
     * 
     * @param obj
     *            The object to compare to.
     * @return True if the other object is equal to this one, false otherwise.
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ItemComparator))
        {
            return false;
        }

        ItemComparator other = (ItemComparator) obj;

        return equalsWithNull(element, other.element)
                && equalsWithNull(qualifier, other.qualifier)
                && equalsWithNull(language, other.language) && (max == other.max);
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(element).append(qualifier).append(language).append(max).toHashCode();
    }

    /**
     * Return true if the first string is equal to the second. Either or both
     * may be null.
     */
    private boolean equalsWithNull(String first, String second)
    {
        if (first == null && second == null)
        {
            return true;
        }

        if (first == null || second == null)
        {
            return false;
        }

        return first.equals(second);
    }

    /**
     * Choose the canonical value from an item for comparison. If there are no
     * values, null is returned. If there is exactly one value, then it is
     * returned. Otherwise, either the maximum or minimum lexicographical value
     * is returned; the parameter to the constructor says which.
     * 
     * @param item
     *            The item to check
     * @return The chosen value, or null
     */
    private String getValue(Item item)
    {
        // The overall array and each element are guaranteed non-null
        Metadatum[] dcvalues = item.getDC(element, qualifier, language);

        if (dcvalues.length == 0)
        {
            return null;
        }

        if (dcvalues.length == 1)
        {
            return normalizeTitle(dcvalues[0]);
        }

        // We want to sort using Strings, but also keep track of
        // which Metadatum the value came from.
        Map<String, Integer> values = new HashMap<String, Integer>();

        for (int i = 0; i < dcvalues.length; i++)
        {
            String value = dcvalues[i].value;

            if (value != null)
            {
                values.put(value, Integer.valueOf(i));
            }
        }

        if (values.size() == 0)
        {
            return null;
        }

        Set<String> valueSet = values.keySet();
        String chosen = max ? Collections.max(valueSet)
                : Collections.min(valueSet);

        int index = (values.get(chosen)).intValue();

        return normalizeTitle(dcvalues[index]);
    }

    /**
     * Normalize the title of a Metadatum.
     */
    private String normalizeTitle(Metadatum value)
    {
        if (!"title".equals(element))
        {
            return value.value;
        }

        return OrderFormat.makeSortString(value.value, value.language, OrderFormat.TITLE);
    }
}
