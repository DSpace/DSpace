/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the BitstreamFormat object.
 * This class is responsible for all business logic calls for the BitstreamFormat object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamFormatServiceImpl implements BitstreamFormatService {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(BitstreamFormat.class);

    /**
     * Configuration property that enables content-based (Apache Tika) format
     * identification. When {@code false}, only the legacy filename-extension
     * identification is used. Defaults to {@code true}.
     */
    protected static final String CFG_IDENTIFY_BY_CONTENT = "bitstream.format.identification.by-content.enabled";

    /**
     * MIME type Apache Tika returns when it cannot recognise the content. It is
     * also the MIME type of the registry's "Unknown" format, so we treat it as
     * "not identified" and fall back to extension-based identification.
     */
    protected static final String UNKNOWN_MIME_TYPE = "application/octet-stream";

    @Autowired(required = true)
    protected BitstreamFormatDAO bitstreamFormatDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected BitstreamStorageService bitstreamStorageService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    /**
     * Apache Tika facade used for content-based (magic byte / container) format
     * identification. Tika is thread-safe, so a single instance is reused.
     */
    private final Tika tika = new Tika();

    protected BitstreamFormatServiceImpl() {

    }

    /**
     * translate support-level ID to string.  MUST keep this table in sync
     * with support level definitions above.
     */
    protected final String[] supportLevelText =
        {"UNKNOWN", "KNOWN", "SUPPORTED"};


    /**
     * Get a bitstream format from the database.
     *
     * @param context DSpace context object
     * @param id      ID of the bitstream format
     * @return the bitstream format, or null if the ID is invalid.
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat find(Context context, int id)
        throws SQLException {
        BitstreamFormat bitstreamFormat = bitstreamFormatDAO.findByID(context, BitstreamFormat.class, id);

        if (bitstreamFormat == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context,
                                               "find_bitstream_format",
                                               "not_found,bitstream_format_id=" + id));
            }

            return null;
        }

        // not null, return format object
        if (log.isDebugEnabled()) {
            log.debug(LogHelper.getHeader(context, "find_bitstream_format",
                                           "bitstream_format_id=" + id));
        }

        return bitstreamFormat;
    }

    @Override
    public BitstreamFormat findByMIMEType(Context context, String mimeType) throws SQLException {
        return bitstreamFormatDAO.findByMIMEType(context, mimeType, false);
    }

    @Override
    public BitstreamFormat findByShortDescription(Context context, String desc) throws SQLException {
        return bitstreamFormatDAO.findByShortDescription(context, desc);
    }

    @Override
    public BitstreamFormat findUnknown(Context context) throws SQLException {
        BitstreamFormat bf = findByShortDescription(context, "Unknown");

        if (bf == null) {
            throw new IllegalStateException(
                "No `Unknown' bitstream format in registry");
        }

        return bf;
    }

    @Override
    public List<BitstreamFormat> findAll(Context context) throws SQLException {
        return bitstreamFormatDAO.findAll(context, BitstreamFormat.class);
    }

    @Override
    public List<BitstreamFormat> findNonInternal(Context context) throws SQLException {
        return bitstreamFormatDAO.findNonInternal(context);
    }

    @Override
    public BitstreamFormat create(Context context) throws SQLException, AuthorizeException {
        // Check authorisation - only administrators can create new formats
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can create bitstream formats");
        }

        // Create a table row
        BitstreamFormat bitstreamFormat = bitstreamFormatDAO.create(context, new BitstreamFormat());


        log.info(LogHelper.getHeader(context, "create_bitstream_format",
                                      "bitstream_format_id="
                                          + bitstreamFormat.getID()));

        return bitstreamFormat;
    }

    @Override
    public void setShortDescription(Context context, BitstreamFormat bitstreamFormat, String shortDescription)
        throws SQLException {
        // You can not reset the unknown's registry's name
        BitstreamFormat unknown = null;
        try {
            unknown = findUnknown(context);
        } catch (IllegalStateException e) {
            // No short_description='Unknown' found in bitstreamformatregistry
            // table. On first load of registries this is expected because it
            // hasn't been inserted yet! So, catch but ignore this runtime
            // exception thrown by method findUnknown.
        }

        // If the exception was thrown, unknown will == null so goahead and
        // load s. If not, check that the unknown's registry's name is not
        // being reset.
        if (unknown == null || !unknown.getID().equals(bitstreamFormat.getID())) {
            bitstreamFormat.setShortDescriptionInternal(shortDescription);
        }
    }

    @Override
    public String getSupportLevelText(BitstreamFormat bitstreamFormat) {
        return supportLevelText[bitstreamFormat.getSupportLevel()];
    }

    @Override
    public void setSupportLevel(BitstreamFormat bitstreamFormat, int supportLevel) {
        // Sanity check
        if ((supportLevel < 0) || (supportLevel > 2)) {
            throw new IllegalArgumentException("Invalid support level");
        }

        bitstreamFormat.setSupportLevelInternal(supportLevel);
    }

    @Override
    public void update(Context context, BitstreamFormat bitstreamFormat) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(bitstreamFormat));
    }

    @Override
    public void update(Context context, List<BitstreamFormat> bitstreamFormats)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(bitstreamFormats)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                    "Only administrators can modify bitstream formats");
            }

            for (BitstreamFormat bitstreamFormat : bitstreamFormats) {
                log.info(LogHelper.getHeader(context, "update_bitstream_format",
                                              "bitstream_format_id=" + bitstreamFormat.getID()));

                bitstreamFormatDAO.save(context, bitstreamFormat);
            }
        }
    }

    @Override
    public void delete(Context context, BitstreamFormat bitstreamFormat) throws SQLException, AuthorizeException {
        // Check authorisation - only administrators can delete formats
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = findUnknown(context);

        if (unknown.getID().equals(bitstreamFormat.getID())) {
            throw new IllegalArgumentException("The Unknown bitstream format may not be deleted.");
        }

        // Set bitstreams with this format to "unknown"
        int numberChanged = bitstreamFormatDAO.updateRemovedBitstreamFormat(context, bitstreamFormat, unknown);

        // Delete this format from database
        bitstreamFormatDAO.delete(context, bitstreamFormat);

        log.info(LogHelper.getHeader(context, "delete_bitstream_format",
                                      "bitstream_format_id=" + bitstreamFormat.getID() + ",bitstreams_changed="
                                          + numberChanged));
    }

    @Override
    public int getSupportLevelID(String supportLevel) {
        for (int i = 0; i < supportLevelText.length; i++) {
            if (supportLevelText[i].equals(supportLevel)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public BitstreamFormat guessFormat(Context context, Bitstream bitstream) throws SQLException {
        // Content-based identification (Apache Tika) takes precedence: it inspects the
        // actual file content (magic bytes / container structure) instead of trusting the
        // filename extension, which may be missing, wrong, or deliberately misleading. The
        // filename is passed to Tika only as a hint. Can be disabled via configuration to
        // fall back to the legacy extension-only behaviour.
        if (configurationService.getBooleanProperty(CFG_IDENTIFY_BY_CONTENT, true)) {
            BitstreamFormat format = guessFormatByContent(context, bitstream);
            if (format != null) {
                return format;
            }
        }

        // Fall back to filename-extension identification when content detection is disabled
        // or inconclusive (e.g. the detected MIME type is not present in the registry).
        return guessFormatByExtension(context, bitstream);
    }

    /**
     * Identify a bitstream's format from its actual content, using Apache Tika. The
     * bitstream's filename (if any) is supplied to Tika only as a hint to disambiguate
     * content that magic-byte detection alone cannot separate. If the content cannot be
     * read, Tika cannot recognise it, or the detected MIME type is not present in the
     * bitstream format registry, {@code null} is returned so the caller can fall back to
     * extension-based identification.
     *
     * @param context   DSpace context object
     * @param bitstream the bitstream to identify
     * @return the matching {@link BitstreamFormat}, or {@code null} if it could not be
     *         determined from the content
     * @throws SQLException if a database error occurs
     */
    protected BitstreamFormat guessFormatByContent(Context context, Bitstream bitstream) throws SQLException {
        String mimeType;
        try (InputStream inputStream = bitstreamStorageService.retrieve(context, bitstream)) {
            if (inputStream == null) {
                return null;
            }
            Metadata metadata = new Metadata();
            String name = bitstream.getName();
            if (name != null) {
                // Provide the filename as a detection hint; it does not override the content.
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, name);
            }
            // Wrap in a TikaInputStream so container-aware detectors (e.g. OLE2/OOXML for
            // legacy and modern Office documents) can inspect the file properly.
            try (TikaInputStream tikaStream = TikaInputStream.get(inputStream)) {
                mimeType = tika.detect(tikaStream, metadata);
            }
        } catch (IOException e) {
            log.warn(LogHelper.getHeader(context, "guess_format_by_content",
                "Unable to read content of bitstream " + bitstream.getID()
                    + " for format identification; falling back to filename"), e);
            return null;
        }

        // Tika returns application/octet-stream when it cannot recognise the content.
        if (mimeType == null || mimeType.equalsIgnoreCase(UNKNOWN_MIME_TYPE)) {
            return null;
        }

        // Map the detected MIME type onto a (non-internal) registry format. Returns null
        // when the registry does not list this MIME type, letting the extension fallback try.
        return findByMIMEType(context, mimeType);
    }

    /**
     * Identify a bitstream's format solely from its filename extension (the legacy
     * behaviour). Used as a fallback when content-based identification is disabled or
     * inconclusive.
     *
     * @param context   DSpace context object
     * @param bitstream the bitstream to identify
     * @return the matching {@link BitstreamFormat}, or {@code null} if the extension is
     *         missing or unknown
     * @throws SQLException if a database error occurs
     */
    protected BitstreamFormat guessFormatByExtension(Context context, Bitstream bitstream) throws SQLException {
        String filename = bitstream.getName();

        // Gracefully handle the null case
        if (filename == null) {
            return null;
        }

        filename = filename.toLowerCase();

        // Get the name of the bitstream, get the extension, and see if we know the type.
        String extension = filename;
        int lastDot = filename.lastIndexOf('.');

        if (lastDot != -1) {
            extension = filename.substring(lastDot + 1);
        }

        // If the last character was a dot, then extension will now be
        // an empty string. If this is the case, we don't know what
        // file type it is.
        if (extension.equals("")) {
            return null;
        }

        List<BitstreamFormat> bitstreamFormats = bitstreamFormatDAO.findByFileExtension(context, extension);

        if (CollectionUtils.isNotEmpty(bitstreamFormats)) {
            return bitstreamFormats.get(0);
        }
        return null;
    }
}
