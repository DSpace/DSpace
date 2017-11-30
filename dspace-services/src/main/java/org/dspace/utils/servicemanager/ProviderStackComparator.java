/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;

import java.util.Comparator;
import java.io.Serializable;


/**
 * A Comparator for provider stacks.  This is specially designed for
 * sorting a list of ProviderHolders so it will unpack them and then do 
 * the typical sorting on them while properly handling the null cases.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class ProviderStackComparator implements Comparator<ProviderHolder<?>>, Serializable {
    public static final long serialVersionUID = 1l;
    public int compare(ProviderHolder<?> ph0, ProviderHolder<?> ph1) {
        /* a negative integer, zero, or a positive integer as the first argument 
         * is less than, equal to, or greater than the second.
         */
        int comparison = 0;
        Object arg0 = ph0.getProvider();
        Object arg1 = ph1.getProvider();
        if (arg0 == null && arg1 == null) {
            comparison = 0;
        } else if (arg0 == null) {
            comparison = 1; // null is last
        } else if (arg1 == null) {
            comparison = -1; // null is last
        } else {
            comparison = new OrderedServiceComparator().compare(arg0, arg1);
        }
        return comparison;
    }
}
