/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service.utils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Expose the Bitstream format mime type as a IIIF Metadata
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(BitstreamIIIFVirtualMetadata.IIIF_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX + "mimetype")
public class BitstreamMimetypeIIIFVirtualMetadata implements BitstreamIIIFVirtualMetadata {
    @Autowired
    BitstreamService bitstreamService;

    @Override
    public List<String> getValues(Context context, Bitstream bitstream) {
        try {
            return Collections.singletonList(bitstreamService.getFormat(context, bitstream).getMIMEType());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
