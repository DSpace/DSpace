/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service.utils;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Interface to implement to expose additional information at the canvas level
 * for the bitstream
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface BitstreamIIIFVirtualMetadata {
    public final String IIIF_BITSTREAM_VIRTUAL_METADATA_BEAN_PREFIX = "iiif.bitstream.";

    List<String> getValues(Context context, Bitstream bitstream);
}
