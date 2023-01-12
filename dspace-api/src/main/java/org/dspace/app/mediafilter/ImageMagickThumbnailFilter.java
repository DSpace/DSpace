/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public File getImageFile(File f, int page, boolean verbose)
        throws IOException, InterruptedException, IM4JavaException {
        File f2 = new File(f.getParentFile(), f.getName() + ".jpg");
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
        // we can use ImageMagick's default behavior unless we see an explit
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

        String s = "[" + page + "]";
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

    @Override
    public boolean preProcessBitstream(Context c, Item item, Bitstream source, boolean verbose) throws Exception {
        String nsrc = source.getName();
        for (Bundle b : itemService.getBundles(item, "THUMBNAIL")) {
            for (Bitstream bit : b.getBitstreams()) {
                String n = bit.getName();
                if (n != null) {
                    if (nsrc != null) {
                        if (!n.startsWith(nsrc)) {
                            continue;
                        }
                    }
                }
                String description = bit.getDescription();
                // If anything other than a generated thumbnail
                // is found, halt processing
                Pattern replaceRegex;
                try {
                    String patt = configurationService.getProperty(PRE + ".replaceRegex", DEFAULT_PATTERN);
                    replaceRegex = Pattern.compile(patt == null ? DEFAULT_PATTERN : patt);
                } catch (PatternSyntaxException e) {
                    System.err.println("Invalid thumbnail replacement pattern: " + e.getMessage());
                    throw e;
                }
                if (description != null) {
                    if (replaceRegex.matcher(description).matches()) {
                        if (verbose) {
                            System.out.format("%s %s matches pattern and is replacable.%n",
                                    description, nsrc);
                        }
                        continue;
                    }
                    if (description.equals(getDescription())) {
                        if (verbose) {
                            System.out.format("%s %s is replaceable.%n",
                                    getDescription(), nsrc);
                        }
                        continue;
                    }
                }
                System.out.format("Custom Thumbnail exists for %s for item %s.  Thumbnail will not be generated.%n",
                        nsrc, item.getHandle());
                return false;
            }
        }

        return true; // assume that the thumbnail is a custom one
    }

}
