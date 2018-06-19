/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.comparator;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
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
            String name1 = StringUtils.trimToEmpty(dso1.getName());
            String name2 = StringUtils.trimToEmpty(dso2.getName());

            //When two DSO's have the same name, use their UUID to put them in an order
            if(name1.equals(name2)) {
                return ObjectUtils.compare(dso1.getID(), dso2.getID());
            } else {
                return name1.compareToIgnoreCase(name2);
            }
        }
    }

}