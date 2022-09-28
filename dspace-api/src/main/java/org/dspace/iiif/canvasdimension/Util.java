/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension;

/**
 * Utilities for IIIF canvas dimension processing.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class Util {

    private Util() {}

    /**
     * IIIF Presentation API version 2.1.1:
     * If the largest image’s dimensions are less than 1200 pixels on either edge, then
     * the canvas’s dimensions SHOULD be double those of the image.
     * @param dims
     * @return
     */
    public static int[] checkDimensions(int[] dims) {
        if (dims[0] < 1200 || dims[1] < 1200) {
            dims[0] = dims[0] * 2;
            dims[1] = dims[1] * 2;
        }
        return dims;
    }

}
