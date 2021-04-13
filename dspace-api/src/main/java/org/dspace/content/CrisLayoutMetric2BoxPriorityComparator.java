/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Comparator;

import org.dspace.layout.CrisLayoutMetric2Box;

/**
 * Compare the position of two {@link CrisLayoutMetric2Box}
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CrisLayoutMetric2BoxPriorityComparator implements Comparator<CrisLayoutMetric2Box> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CrisLayoutMetric2Box o1, CrisLayoutMetric2Box o2) {
        return o1.getPosition() <  o2.getPosition() ? -1 : o1.getPosition() == o2.getPosition() ? 0 : 1;
    }

}
