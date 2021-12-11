/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.canvasdimension;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class ImageDimensionReader {

    private ImageDimensionReader() {}

    public static int[] getImageDimensions(InputStream image) throws Exception {
        int[] dims = new int[2];
        BufferedImage buf = ImageIO.read(image);
        int width = buf.getWidth(null);
        int height = buf.getHeight(null);
        if (width > 0 && height > 0) {
            dims[0] = width;
            dims[1] = height;
            return dims;
        }
        return null;
    }
}
