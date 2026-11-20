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
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.im4java.core.IM4JavaException;


/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 * <p>
 * By default, ImageMagick is told which decoder to use based on the registered format (MIME type)
 * of the bitstream, so that it cannot choose another decoder based on the file contents. See
 * {@link #FORCE_INPUT_FORMAT}.
 */
public class ImageMagickImageThumbnailFilter extends ImageMagickThumbnailFilter {

    private static final Logger log = LogManager.getLogger();

    /**
     * Configuration property: if true (the default), decode each bitstream with the ImageMagick
     * decoder matching its registered format. If false, let ImageMagick choose a decoder based on
     * the file contents.
     */
    static final String FORCE_INPUT_FORMAT = ImageMagickImageThumbnailFilter.class.getName() + ".forceInputFormat";

    /**
     * ImageMagick decoders for the registered MIME types of the default input formats of this filter.
     */
    static final Map<String, String> IMAGEMAGICK_FORMATS = Map.of(
        "image/bmp", "BMP",
        "image/x-ms-bmp", "BMP",
        "image/gif", "GIF",
        "image/jpeg", "JPEG",
        "image/jp2", "JP2",
        "image/png", "PNG",
        "image/tiff", "TIFF"
    );

    /**
     * Create a thumbnail without knowing the registered format of the source bitstream.
     * ImageMagick chooses a decoder based on the file contents.
     *
     * @param currentItem item
     * @param source      source input stream
     * @param verbose     verbose mode
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
        throws Exception {
        return createThumbnail(source, null, verbose);
    }

    /**
     * Create a thumbnail, decoding the source with the ImageMagick decoder that matches the
     * registered format of the bitstream (unless disabled with {@link #FORCE_INPUT_FORMAT}).
     *
     * @param context     DSpace context
     * @param currentItem item
     * @param bitstream   source bitstream
     * @param source      source input stream
     * @param verbose     verbose mode
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Context context, Item currentItem, Bitstream bitstream,
                                            InputStream source, boolean verbose)
        throws Exception {
        return createThumbnail(source, getInputFormat(context, bitstream), verbose);
    }

    /**
     * Determine the ImageMagick decoder to use for a bitstream.
     *
     * @param context   DSpace context
     * @param bitstream source bitstream
     * @return the ImageMagick format, such as {@code JPEG}, or null to let ImageMagick choose a
     *         decoder based on the file contents (when disabled by configuration, or when the
     *         registered format has no known ImageMagick decoder)
     * @throws SQLException if the format cannot be read
     */
    String getInputFormat(Context context, Bitstream bitstream) throws SQLException {
        if (!configurationService.getBooleanProperty(FORCE_INPUT_FORMAT, true) || bitstream == null) {
            return null;
        }
        BitstreamFormat format = bitstream.getFormat(context);
        String mimeType = format == null || format.getMIMEType() == null
            ? null : format.getMIMEType().toLowerCase(Locale.ROOT);
        String inputFormat = mimeType == null ? null : IMAGEMAGICK_FORMATS.get(mimeType);
        if (inputFormat == null) {
            log.warn("No ImageMagick decoder known for bitstream {} with MIME type {}; letting ImageMagick "
                         + "choose a decoder based on the file contents", bitstream.getID(), mimeType);
        }
        return inputFormat;
    }

    private InputStream createThumbnail(InputStream source, String inputFormat, boolean verbose)
        throws Exception {
        File f = inputStreamToTempFile(source, "imthumb", ".tmp");
        File f2 = null;
        try {
            f2 = getThumbnailFile(f, inputFormat, verbose);
            byte[] bytes = Files.readAllBytes(f2.toPath());
            return new ByteArrayInputStream(bytes);
        } catch (IM4JavaException e) {
            if (inputFormat == null) {
                throw e;
            }
            throw new IM4JavaException("ImageMagick could not decode the bitstream as " + inputFormat
                                           + ", the decoder for its registered format. The file content may not "
                                           + "match its registered format.", e);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            if (f2 != null) {
                //noinspection ResultOfMethodCallIgnored
                f2.delete();
            }
        }
    }
}
