/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * Implements virtual field processing for citation information (based on
 * Grahamt version).
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class VirtualFieldCitation implements VirtualField {

    private static Logger log = Logger.getLogger(VirtualFieldCitation.class);

    public String[] getMetadata(Context context, Item item, String fieldName) {
        StreamDisseminationCrosswalk crosswalk = getStreamDisseminationCrosswalk(fieldName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] result = null;
        try {
            crosswalk.disseminate(null, item, out);
            result = new String[] { out.toString() };
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    private StreamDisseminationCrosswalk getStreamDisseminationCrosswalk(String fieldName) {
        return (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
            .getNamedPlugin(StreamDisseminationCrosswalk.class, fieldName);
    }
}