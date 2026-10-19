/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.dspace.content.Item;

public class ImageMagickPdfThumbnailFilter extends ImageMagickThumbnailFilter {
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
        throws Exception {
        File f = inputStreamToTempFile(source, "impdfthumb", ".pdf");
        File f2 = null;
        File f3 = null;
        try {
            // Step 1: get an image from our PDF file, with PDF-specific processing options
            f2 = getImageFile(f, verbose);
            // Step 2: use the image above to create the final resized and rotated thumbnail
            f3 = getThumbnailFile(f2, verbose);
            byte[] bytes = Files.readAllBytes(f3.toPath());
            return new ByteArrayInputStream(bytes);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            if (f2 != null) {
                //noinspection ResultOfMethodCallIgnored
                f2.delete();
            }
            if (f3 != null) {
                //noinspection ResultOfMethodCallIgnored
                f3.delete();
            }
        }
    }

}
