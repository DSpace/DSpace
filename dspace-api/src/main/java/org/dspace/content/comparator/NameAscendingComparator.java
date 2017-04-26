package org.dspace.content.comparator;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;

import java.util.Comparator;

/**
 * Created by yana on 09/01/17.
 */
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
