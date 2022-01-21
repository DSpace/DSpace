/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import org.dspace.content.Bitstream;

/**
 * @author Michael Spalti mspalti@willamette.edu
 */
public interface IIIFApiQueryService {

    /**
     * Returns array with canvas height and width
     * @param bitstream
     * @return
     */
    int[] getImageDimensions(Bitstream bitstream);

}
