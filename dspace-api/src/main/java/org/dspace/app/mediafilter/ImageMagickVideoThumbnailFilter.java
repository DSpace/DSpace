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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.dspace.content.Item;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;


/**
 * Filter video bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public class ImageMagickVideoThumbnailFilter extends ImageMagickThumbnailFilter {
    private static final int DEFAULT_WIDTH = 180;
    private static final int DEFAULT_HEIGHT = 120;
    private static final int FRAME_NUMBER = 100;

    /**
     * @param currentItem item
     * @param source      source input stream
     * @param verbose     verbose mode
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
        throws Exception {
        File f = inputStreamToTempFile(source, "imthumb", ".tmp");
        File f2 = null;
        try {
            f2 = getThumbnailFile(f, verbose);
            byte[] bytes = Files.readAllBytes(f2.toPath());
            return new ByteArrayInputStream(bytes);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            if (f2 != null) {
                //noinspection ResultOfMethodCallIgnored
                f2.delete();
            }
        }
    }

    @Override
    public File getThumbnailFile(File f, boolean verbose)
        throws IOException, InterruptedException, IM4JavaException {
        File f2 = new File(f.getParentFile(), f.getName() + ".jpg");
        f2.deleteOnExit();
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.autoOrient();
        op.addImage("VIDEO:" + f.getAbsolutePath() + "[" + FRAME_NUMBER + "]");
        op.thumbnail(configurationService.getIntProperty("thumbnail.maxwidth", DEFAULT_WIDTH),
                        configurationService.getIntProperty("thumbnail.maxheight", DEFAULT_HEIGHT));
        op.addImage(f2.getAbsolutePath());
        if (verbose) {
            System.out.println("IM Thumbnail Param: " + op);
        }
        cmd.run(op);
        return f2;
    }
}
