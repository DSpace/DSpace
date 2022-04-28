/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension;

import static org.dspace.iiif.canvasdimension.Util.checkDimensions;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Reads and return height and width dimensions for image bitstreams.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class ImageDimensionReader {

    private ImageDimensionReader() {}

    /**
     * Uses ImageIO to read height and width dimensions.
     * @param image inputstream for dspace image
     * @return image dimensions or null if the image format cannot be read.
     * @throws Exception
     */
    public static int[] getImageDimensions(InputStream image) throws IOException {
        int[] dims = new int[2];
        BufferedImage buf = ImageIO.read(image);
        if (buf != null) {
            int width = buf.getWidth(null);
            int height = buf.getHeight(null);
            if (width > 0 && height > 0) {
                dims[0] = width;
                dims[1] = height;
                return checkDimensions(dims);
            }
        }
        return null;
    }

}
