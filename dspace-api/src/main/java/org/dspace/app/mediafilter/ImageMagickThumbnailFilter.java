/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.dspace.core.Constants.DEFAULT_BUNDLE_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.process.ProcessStarter;

/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public abstract class ImageMagickThumbnailFilter extends MediaFilter {
    private static final int DEFAULT_WIDTH = 180;
    private static final int DEFAULT_HEIGHT = 120;
    static final String DEFAULT_PATTERN = "Generated Thumbnail";
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected static final String PRE = ImageMagickThumbnailFilter.class.getName();

    static {
        String s = configurationService.getProperty(PRE + ".ProcessStarter");
        ProcessStarter.setGlobalSearchPath(s);
    }

    public ImageMagickThumbnailFilter() {
    }

    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     */
    @Override
    public String getBundleName() {
        return "THUMBNAIL";
    }

    /**
     * @return String bitstreamformat
     */
    @Override
    public String getFormatString() {
        return "JPEG";
    }

    /**
     * @return String bitstreamDescription
     */
    @Override
    public String getDescription() {
        return configurationService.getProperty(PRE + ".bitstreamDescription", "IM Thumbnail");
    }

    public File inputStreamToTempFile(InputStream source, String prefix, String suffix) throws IOException {
        File f = File.createTempFile(prefix, suffix);
        f.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(f);

        byte[] buffer = new byte[1024];
        int len = source.read(buffer);
        while (len != -1) {
            fos.write(buffer, 0, len);
            len = source.read(buffer);
        }
        fos.close();
        return f;
    }

    public File getThumbnailFile(File f, boolean verbose)
        throws IOException, InterruptedException, IM4JavaException {
        File f2 = new File(f.getParentFile(), f.getName() + ".jpg");
        f2.deleteOnExit();
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.autoOrient();
        op.addImage(f.getAbsolutePath());
        op.thumbnail(configurationService.getIntProperty("thumbnail.maxwidth", DEFAULT_WIDTH),
                        configurationService.getIntProperty("thumbnail.maxheight", DEFAULT_HEIGHT));
        op.addImage(f2.getAbsolutePath());
        if (verbose) {
            System.out.println("IM Thumbnail Param: " + op);
        }
        cmd.run(op);
        return f2;
    }

    /**
     * Return an image from a bitstream with specific processing options for
     * PDFs. This is only used by ImageMagickPdfThumbnailFilter in order to
     * generate an intermediate image file for use with getThumbnailFile.
     */
    public File getImageFile(File f, boolean verbose)
        throws IOException, InterruptedException, IM4JavaException {
        // Writing an intermediate file to disk is inefficient, but since we're
        // doing it anyway, we should use a lossless format. IM's internal MIFF
        // is lossless like PNG and TIFF, but much faster.
        File f2 = new File(f.getParentFile(), f.getName() + ".miff");
        f2.deleteOnExit();
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();

        // Optionally override ImageMagick's default density of 72 DPI to use a
        // "supersample" when creating the PDF thumbnail. Note that I prefer to
        // use the getProperty() method here instead of getIntPropert() because
        // the latter always returns an integer (0 in the case it's not set). I
        // would prefer to keep ImageMagick's default to itself rather than for
        // us to set one. Also note that the density option *must* come before
        // we open the input file.
        String density = configurationService.getProperty(PRE + ".density");
        if (density != null) {
            op.density(Integer.valueOf(density));
        }

        // Check the PDF's MediaBox and CropBox to see if they are the same.
        // If not, then tell ImageMagick to use the CropBox when generating
        // the thumbnail because the CropBox is generally used to define the
        // area displayed when a user opens the PDF on a screen, whereas the
        // MediaBox is used for print. Not all PDFs set these correctly, so
        // we can use ImageMagick's default behavior unless we see an explicit
        // CropBox. Note: we don't need to do anything special to detect if
        // the CropBox is missing or empty because pdfbox will set it to the
        // same size as the MediaBox if it doesn't exist. Also note that we
        // only need to check the first page, since that's what we use for
        // generating the thumbnail (PDDocument uses a zero-based index).
        PDPage pdfPage = PDDocument.load(f).getPage(0);
        PDRectangle pdfPageMediaBox = pdfPage.getMediaBox();
        PDRectangle pdfPageCropBox = pdfPage.getCropBox();

        // This option must come *before* we open the input file.
        if (pdfPageCropBox != pdfPageMediaBox) {
            op.define("pdf:use-cropbox=true");
        }

        String s = "[0]";
        op.addImage(f.getAbsolutePath() + s);
        if (configurationService.getBooleanProperty(PRE + ".flatten", true)) {
            op.flatten();
        }

        // PDFs using the CMYK color system can be handled specially if
        // profiles are defined
        String cmyk_profile = configurationService.getProperty(PRE + ".cmyk_profile");
        String srgb_profile = configurationService.getProperty(PRE + ".srgb_profile");
        if (cmyk_profile != null && srgb_profile != null) {
            Info imageInfo = new Info(f.getAbsolutePath() + s, true);
            String imageClass = imageInfo.getImageClass();
            if (imageClass.contains("CMYK")) {
                op.profile(cmyk_profile);
                op.profile(srgb_profile);
            }
        }
        op.addImage(f2.getAbsolutePath());
        if (verbose) {
            System.out.println("IM Image Param: " + op);
        }
        cmd.run(op);
        return f2;
    }

    /**
     * Helper method to check if a bitstream is of a supported format of the plugin
     */
    private boolean isSupportedFormat(Context c, Bitstream bitstream) {
        List<String> supportedFormats = List.of(configurationService.getArrayProperty(
                "filter." + this.getClass().getName() + ".inputFormats"));
        try {
            return supportedFormats.contains(bitstream.getFormatDescription(c));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean preProcessBitstream(Context c, Item item, Bitstream source, boolean verbose) throws Exception {
        boolean enforcePrimaryLogic = configurationService.getBooleanProperty("filter.imagemagick.thumbnails" +
                ".only-pimary", true);
        // Ensure only one thumbnail is generated for the ORIGINAL bundle.
        Bundle originalBundle = getOriginalBundle(source);
        if (originalBundle == null) {
            return false; // No ORIGINAL bundle found, so skip processing.
        }

        if (enforcePrimaryLogic && !shouldProcessBitstream(c, originalBundle, source, verbose, item)) {
            return false; // Skip processing for this bitstream.
        }

        String sourceName = source.getName();
        for (Bundle thumbnailBundle : itemService.getBundles(item, "THUMBNAIL")) {
            for (Bitstream bitstream : thumbnailBundle.getBitstreams()) {
                if (!isReplaceableThumbnail(bitstream, sourceName, verbose)) {
                    System.out.printf("Custom thumbnail exists for %s for item %s. Thumbnail will not be generated.%n",
                            sourceName, item.getHandle());
                    return false;
                }
            }
        }

        return true; // Assume the thumbnail is custom and replaceable.
    }

    private Bundle getOriginalBundle(Bitstream source) throws SQLException {
        return source.getBundles().stream()
                     .filter(bundle -> DEFAULT_BUNDLE_NAME.equals(bundle.getName()))
                     .findFirst()
                     .orElse(null);
    }

    private boolean shouldProcessBitstream(Context c, Bundle originalBundle, Bitstream source,
                                           boolean verbose, Item item) {
        Bitstream primaryBitstream = originalBundle.getPrimaryBitstream();

        if (primaryBitstream != null) {
            // Process only the primary bitstream if it exists.
            if (!source.equals(primaryBitstream)) {
                if (verbose) {
                    System.out.printf("Skipping non-primary bitstream %s for item %s.%n", source.getName(),
                            item.getHandle());
                }
                return false;
            }
            return true;
        }

        // Process the first supported bitstream if no primary exists.
        return isFirstSupportedBitstream(c, originalBundle, source);
    }

    private boolean isFirstSupportedBitstream(Context c, Bundle originalBundle, Bitstream source) {
        return originalBundle.getBitstreams().stream()
                             .filter(bitstream -> isSupportedFormat(c, bitstream))
                             .findFirst()
                             .map(source::equals)
                             .orElse(false);
    }

    private boolean isReplaceableThumbnail(Bitstream bitstream, String sourceName,
                                           boolean verbose) throws PatternSyntaxException {
        String bitstreamName = bitstream.getName();
        if (bitstreamName == null || sourceName == null || !bitstreamName.startsWith(sourceName)) {
            return true;
        }

        String description = bitstream.getDescription();
        Pattern replaceRegex = getReplacePattern();
        if (description != null) {
            if (replaceRegex.matcher(description).matches() || description.equals(getDescription())) {
                if (verbose) {
                    System.out.printf("%s %s is replaceable.%n", description, bitstreamName);
                }
                return true;
            }
        }

        return false; // A custom, non-replaceable thumbnail exists.
    }

    private Pattern getReplacePattern() throws PatternSyntaxException {
        String pattern = configurationService.getProperty(PRE + ".replaceRegex", DEFAULT_PATTERN);
        try {
            return Pattern.compile(pattern != null ? pattern : DEFAULT_PATTERN);
        } catch (PatternSyntaxException e) {
            System.err.printf("Invalid thumbnail replacement pattern: %s%n", e.getMessage());
            throw e;
        }
    }
}
