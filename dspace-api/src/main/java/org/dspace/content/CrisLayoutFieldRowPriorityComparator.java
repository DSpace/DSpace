/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Comparator;

import org.dspace.layout.CrisLayoutField;

/**
 * Compare the row and priority of two {@link CrisLayoutField}
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldRowPriorityComparator implements Comparator<CrisLayoutField> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CrisLayoutField o1, CrisLayoutField o2) {
        int result = o1.getRow() - o2.getRow();
        if (result == 0) {
            result =  o1.getPriority() <  o2.getPriority() ? -1 : o1.getPriority() == o2.getPriority() ? 0 : 1;
        }
        return result;
    }

}
