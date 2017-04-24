package org.dspace.app.util;

import org.apache.commons.lang.math.NumberUtils;
import org.dspace.content.Bitstream;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by frederic on 24/04/17.
 */
public class GoogleBitstreamComparator implements Comparator<Bitstream>{

    HashMap<String, Integer> priorityMap = new HashMap<>();

    public GoogleBitstreamComparator(Map<String, String> configuredFields){
        for(Map.Entry<String, String> entry : configuredFields.entrySet()){
            if(entry.getKey().startsWith("citation_priority_score")){
                priorityMap.put(entry.getKey().split("\\.", 2)[1], NumberUtils.createInteger(entry.getValue()));
            }
        }
    }

    public int compare(Bitstream b1, Bitstream b2) {
        int priority1 = 0;
        int priority2 = 0;
        try {
            priority1 = priorityMap.get(b1.getFormat().getMIMEType());
        } catch (NullPointerException e){
            priority1 = 999;
        }
        try {
            priority2 = priorityMap.get(b2.getFormat().getMIMEType());
        } catch (NullPointerException e){
            priority2 = 999;
        }
        if(priority1 > priority2){
            return 1;
        }
        else if(priority1 == priority2){
            if(b1.getSize() <= b2 .getSize()){
                return 1;
            }
            else {
                return -1;
            }
        }
        else {
            return -1;
        }
    }
}
