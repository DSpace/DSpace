/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.dspace.content.DSpaceObject;

import java.util.Comparator;

public class NameAscendingComparator implements Comparator<DSpaceObject>{

    @Override
    public int compare(DSpaceObject dso1, DSpaceObject dso2) {
        if (dso1 == dso2){
            return 0;
        }else if (dso1 == null){
            return -1;
        }else if (dso2 == null){
            return 1;
        }else {
            return ObjectUtils.compare(dso1.getName(),dso2.getName());
        }
    }
}