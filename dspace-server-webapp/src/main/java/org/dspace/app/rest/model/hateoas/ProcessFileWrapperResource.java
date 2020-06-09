/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.ProcessFileWrapperRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.scripts.Process;

/**
 * This is the Resource object for the {@link ProcessFileWrapperRest}
 * It'll create a Resource object to return and include the associated bitstreams in an embed that's properly
 * made for the type of file that that particular bitstream is
 */
public class ProcessFileWrapperResource extends HALResource<ProcessFileWrapperRest> {

    /**
     * Constructor for this object. Calls on super and creates separate embedded lists
     * @param content   The {@link ProcessFileWrapperRest} object associated with this resource
     * @param utils     Utils class
     */
    public ProcessFileWrapperResource(ProcessFileWrapperRest content, Utils utils) {
        super(content);

        if (content != null) {
            HashMap<String, List<BitstreamResource>> bitstreamResourceMap = new HashMap<>();
            for (BitstreamRest bitstreamRest : content.getBitstreams()) {
                List<MetadataValueRest> fileType = bitstreamRest.getMetadata().getMap()
                                                                .get(Process.BITSTREAM_TYPE_METADATAFIELD);
                if (fileType != null && !fileType.isEmpty()) {
                    bitstreamResourceMap
                        .computeIfAbsent(fileType.get(0).getValue(), k -> new ArrayList<>())
                        .add(new BitstreamResource(bitstreamRest, utils));
                }
            }

            for (Map.Entry<String, List<BitstreamResource>> entry : bitstreamResourceMap.entrySet()) {
                embedResource(entry.getKey(), entry.getValue());
            }
        }
    }
}
