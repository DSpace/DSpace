/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Comparator;

import org.dspace.layout.CrisLayoutBox2Field;

/**
 * Compare the row and priority of two {@link CrisLayoutBox2Field}
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldRowPriorityComparator implements Comparator<CrisLayoutBox2Field> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CrisLayoutBox2Field o1, CrisLayoutBox2Field o2) {
        return o1.getPosition() <  o2.getPosition() ? -1 : o1.getPosition() == o2.getPosition() ? 0 : 1;
    }

}
