/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Test;

/**
 * Tests how {@link ImageMagickImageThumbnailFilter} chooses the ImageMagick decoder for a bitstream.
 */
public class ImageMagickImageThumbnailFilterTest extends AbstractUnitTest {

    private final ConfigurationService configurationService
        = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final ImageMagickImageThumbnailFilter filter = new ImageMagickImageThumbnailFilter();

    @After
    public void resetConfiguration() {
        configurationService.setProperty(ImageMagickImageThumbnailFilter.FORCE_INPUT_FORMAT, null);
    }

    @Test
    public void usesDecoderForRegisteredFormat() throws Exception {
        assertEquals("JPEG", filter.getInputFormat(context, bitstreamWithMimeType("image/jpeg")));
        assertEquals("PNG", filter.getInputFormat(context, bitstreamWithMimeType("image/png")));
        assertEquals("GIF", filter.getInputFormat(context, bitstreamWithMimeType("image/gif")));
        assertEquals("TIFF", filter.getInputFormat(context, bitstreamWithMimeType("image/tiff")));
        assertEquals("BMP", filter.getInputFormat(context, bitstreamWithMimeType("image/x-ms-bmp")));
        assertEquals("JP2", filter.getInputFormat(context, bitstreamWithMimeType("image/jp2")));
    }

    @Test
    public void mimeTypeIsMatchedCaseInsensitively() throws Exception {
        assertEquals("PNG", filter.getInputFormat(context, bitstreamWithMimeType("IMAGE/PNG")));
    }

    @Test
    public void unknownFormatLetsImageMagickChoose() throws Exception {
        assertNull(filter.getInputFormat(context, bitstreamWithMimeType("image/webp")));
        assertNull(filter.getInputFormat(context, bitstreamWithMimeType(null)));
    }

    @Test
    public void disabledByConfigurationLetsImageMagickChoose() throws Exception {
        configurationService.setProperty(ImageMagickImageThumbnailFilter.FORCE_INPUT_FORMAT, false);
        assertNull(filter.getInputFormat(context, mock(Bitstream.class)));
    }

    @Test
    public void noBitstreamLetsImageMagickChoose() throws Exception {
        assertNull(filter.getInputFormat(context, null));
    }

    @Test
    public void inputArgumentHasFormatPrefixAndFirstFrame() {
        File f = new File("/tmp/imthumb123.tmp");
        assertEquals("JPEG:" + f.getAbsolutePath() + "[0]", ImageMagickThumbnailFilter.inputImage(f, "JPEG"));
        assertEquals(f.getAbsolutePath() + "[0]", ImageMagickThumbnailFilter.inputImage(f, null));
    }

    private Bitstream bitstreamWithMimeType(String mimeType) throws SQLException {
        BitstreamFormat format = mock(BitstreamFormat.class);
        when(format.getMIMEType()).thenReturn(mimeType);
        Bitstream bitstream = mock(Bitstream.class);
        when(bitstream.getFormat(context)).thenReturn(format);
        return bitstream;
    }
}
