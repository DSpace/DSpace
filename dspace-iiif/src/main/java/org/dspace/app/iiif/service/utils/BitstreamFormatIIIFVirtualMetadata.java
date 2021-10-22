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
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Expose the Bitstream Format as a IIIF Metadata
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component(BitstreamIIIFVirtualMetadata.IIIF_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX + "format")
public class BitstreamFormatIIIFVirtualMetadata implements BitstreamIIIFVirtualMetadata {

    @Override
    public List<String> getValues(Context context, Bitstream bitstream) {
        try {
            return Collections.singletonList(bitstream.getFormatDescription(context));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
