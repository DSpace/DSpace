/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif;

import org.dspace.content.Bitstream;

public class MockIIIFApiQueryServiceImpl extends IIIFApiQueryServiceImpl {
    public int[] getImageDimensions(Bitstream bitstream) {
        return new int[]{64, 64};
    }
}
