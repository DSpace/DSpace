/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.im4java.core.IMOperation;
import org.junit.Test;

/**
 * Tests for {@link ImageMagickThumbnailFilter}. ImageMagick does not need to
 * be installed: the tests only inspect the arguments of the generated
 * operation.
 */
public class ImageMagickThumbnailFilterTest extends AbstractUnitTest {

    private final ImageMagickThumbnailFilter filter = new ImageMagickImageThumbnailFilter();

    /**
     * ImageMagick 7 fails with "no images found for operation `-auto-orient'"
     * when the operator comes before the input image, so the input image must
     * be the first argument and the output image the last.
     */
    @Test
    public void testAutoOrientComesAfterInputImage() {
        File source = new File("/tmp/source.tif");
        File thumbnail = new File("/tmp/source.tif.jpg");

        IMOperation op = filter.buildThumbnailOperation(source, thumbnail);

        List<String> args = op.getCmdArgs();
        int input = args.indexOf(source.getAbsolutePath() + "[0]");
        int autoOrient = args.indexOf("-auto-orient");
        int resize = args.indexOf("-thumbnail");
        int output = args.indexOf(thumbnail.getAbsolutePath());

        assertEquals("the input image must be the first argument", 0, input);
        assertTrue("-auto-orient must come after the input image", autoOrient > input);
        assertTrue("-thumbnail must come after -auto-orient", resize > autoOrient);
        assertEquals("the output image must be the last argument", args.size() - 1, output);
    }
}
