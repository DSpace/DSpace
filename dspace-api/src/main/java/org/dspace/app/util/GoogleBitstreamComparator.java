/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

/**
 * This comparator is used to order files of an item, so that they are ordered in a way that the first one
 * is the most useful for use in the citation_pdf_url for Google Scholar
 */
public class GoogleBitstreamComparator implements Comparator<Bitstream> {

    private final static Logger log = org.apache.logging.log4j.LogManager.getLogger(GoogleBitstreamComparator.class);

    HashMap<String, Integer> priorityMap = new HashMap<>();

    private Context context;

    public GoogleBitstreamComparator(Context context, Map<String, String> googleScholarSettings) {
        this.context = context;
        String[] shortDescriptions;
        if (googleScholarSettings.containsKey("citation.prioritized_types")) {
            shortDescriptions = splitAndTrim(googleScholarSettings.get("citation.prioritized_types"));
        } else {
            log.warn("Please define citation.prioritized_types in google-metadata.properties");
            shortDescriptions = new String[0];
        }
        int priority = 1;
        for (String s : shortDescriptions) {
            try {
                BitstreamFormat format = ContentServiceFactory.getInstance().getBitstreamFormatService()
                                                              .findByShortDescription(context, s);
                if (format != null) {
                    priorityMap.put(format.getMIMEType(), priority);
                } else {
                    log.warn(s + " is not a valid short description, please add it to bitstream-formats.xml");
                }
                priority++;
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }

    }

    private String[] splitAndTrim(String toSplit) {
        if (toSplit != null) {
            String[] splittedArray = toSplit.split(",");
            for (int i = 0; i < splittedArray.length; i++) {
                splittedArray[i] = splittedArray[i].trim();
            }
            return splittedArray;
        } else {
            return new String[0];
        }
    }


    /**
     * Compares two bitstreams based on their mimetypes, if mimetypes are the same,then the largest bitstream comes
     * first
     * See google-metadata.properties to define the order
     *
     * @param b1 first bitstream
     * @param b2 second bitstream
     * @return
     */
    public int compare(Bitstream b1, Bitstream b2) {
        int priority1 = getPriorityFromBitstream(b1);
        int priority2 = getPriorityFromBitstream(b2);

        if (priority1 > priority2) {
            return 1;
        } else if (priority1 == priority2) {
            if (b1.getSizeBytes() < b2.getSizeBytes()) {
                return 1;
            } else if (b1.getSizeBytes() == b2.getSizeBytes()) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private int getPriorityFromBitstream(Bitstream bitstream) {
        try {
            String check = bitstream.getFormat(context).getMIMEType();
            if (priorityMap.containsKey(bitstream.getFormat(context).getMIMEType())) {
                return priorityMap.get(bitstream.getFormat(context).getMIMEType());
            } else {
                return Integer.MAX_VALUE;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }
}
