/**
 * $Id: ProviderStackComparator.java 3678 2009-04-06 12:45:44Z grahamtriggs $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/utils/src/main/java/org/dspace/utils/servicemanager/ProviderStackComparator.java $
 * OrderedBeanComparator.java - DSpace2 - Oct 24, 2008 5:01:44 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.utils.servicemanager;

import java.util.Comparator;
import java.io.Serializable;


/**
 * A comparator for provider stacks, this is specially designed for sorting a list
 * of ProviderHolders so it will unpack them and then do the typical sorting on them
 * while properly handling the null cases
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class ProviderStackComparator implements Comparator<ProviderHolder<?>>, Serializable {
    public final static long serialVersionUID = 1l;
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
