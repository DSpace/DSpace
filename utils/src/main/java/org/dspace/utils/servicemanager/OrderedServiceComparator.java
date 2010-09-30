/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.utils.servicemanager;

import java.util.Comparator;
import java.io.Serializable;

import org.dspace.kernel.mixins.OrderedService;


/**
 * A comparator for provider beans, filters, and plugins which will
 * take ordering into account if {@link OrderedService} is implemented.
 * Small numbers are ordered first in priority (i.e. are before large
 * ones).  NOTE that 0 means "not a priority" so it gets placed at the 
 * end.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class OrderedServiceComparator implements Comparator<Object>, Serializable {
    public final static long serialVersionUID = 1l;
    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object arg0, Object arg1) {
        /* a negative integer, zero, or a positive integer as the first argument 
         * is less than, equal to, or greater than the second.
         */
        int comparison = 0;
        if (arg0 instanceof OrderedService &&
                arg1 instanceof OrderedService) {
            int p0 = ((OrderedService)arg0).getOrder();
            int p1 = ((OrderedService)arg1).getOrder();
            if (p0 <= 0 && p1 <= 0) {
                comparison = 0; // both zero or less so equal
            } else if (p0 <= 0) {
                comparison = 2; // zero or less should always be after
            } else if (p1 <= 0) {
                comparison = -2; // zero or less should always be after
            } else {
                comparison = p0 - p1;
            }
        } else if (arg0 instanceof OrderedService) {
            comparison = -1; // ordered is always before unordered
        } else if (arg1 instanceof OrderedService) {
            comparison = 1; // ordered is always before unordered
        } else {
            comparison = 0; // unordered is equivalent
        }
        return comparison;
    }
}
