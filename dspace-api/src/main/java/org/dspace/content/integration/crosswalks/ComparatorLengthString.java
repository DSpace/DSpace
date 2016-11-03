/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.Comparator;

public class ComparatorLengthString implements Comparator<String>
{

    @Override
    public int compare(String o1, String o2)
    {
        if (o1.length() > o2.length()) {
            return -1;
         } else if (o1.length() < o2.length()) {
            return 1;
         } else { 
            return o1.compareTo(o2);
         }

    }

}
