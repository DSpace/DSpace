/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by frederic on 24/04/17.
 */
public class GoogleBitstreamComparator implements Comparator<Bitstream>{

    private final static Logger log = Logger.getLogger(GoogleBitstreamComparator.class);

    HashMap<String, Integer> priorityMap = new HashMap<>();

    public GoogleBitstreamComparator(Map<String, String> googleScholarSettings){
        String[] types = null;
        try {
            types = splitAndTrim(googleScholarSettings.get("citation.prioritized_types"));
        } catch (NullPointerException e){
            log.error("Please define citation.prioritized_types in google-metadata.properties");
        }
        int priority = 1;
        for(String s: types){
            String[] mimetypes = null;
            try {
                mimetypes = splitAndTrim(googleScholarSettings.get("citation.mimetypes." + s));
            } catch (NullPointerException e){
                log.error("Make sure you define all the mimetypes of the citation.prioritized_types");
            }
            for (String mimetype: mimetypes) {
                priorityMap.put(mimetype, priority);
            }
            priority++;
        }
    }

    private String[] splitAndTrim(String toSplit){
        return toSplit.replace(" ", "").split(",");
    }


    /**
     * Compares two bitstreams based on their mimetypes, if mimetypes are the same,then the largest bitstream comes first
     * See google-metadata.properties to define the order
     * @param b1 first bitstream
     * @param b2 second bitstream
     * @return
     */
    public int compare(Bitstream b1, Bitstream b2) {
        int priority1 = getPriorityFromBitstream(b1);
        int priority2 = getPriorityFromBitstream(b2);

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

    private int getPriorityFromBitstream(Bitstream bitstream) {
        if (priorityMap.containsKey(bitstream.getFormat().getMIMEType())){
            return priorityMap.get(bitstream.getFormat().getMIMEType());
        }
        else {
            return Integer.MAX_VALUE;
        }
    }
}
