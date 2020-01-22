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


public class ProcessFileWrapperResource extends HALResource<ProcessFileWrapperRest> {

    public ProcessFileWrapperResource(ProcessFileWrapperRest content, Utils utils) {
        super(content);

        if (content != null) {
            HashMap<String, List<BitstreamResource>> bitstreamResourceMap = new HashMap<>();
            for (BitstreamRest bitstreamRest : content.getBitstreams()) {
                List<MetadataValueRest> fileType = bitstreamRest.getMetadata().getMap().get("process.type");
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
