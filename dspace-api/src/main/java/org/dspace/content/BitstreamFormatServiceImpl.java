/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.BitstreamFormatDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation for the BitstreamFormat object.
 * This class is responsible for all business logic calls for the BitstreamFormat object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamFormatServiceImpl implements BitstreamFormatService {

    /** log4j logger */
    private static Logger log = Logger.getLogger(BitstreamFormat.class);

    @Autowired(required = true)
    protected BitstreamFormatDAO bitstreamFormatDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected BitstreamFormatServiceImpl()
    {

    }

    /** translate support-level ID to string.  MUST keep this table in sync
     *  with support level definitions above.
     */
    protected final String supportLevelText[] =
        { "UNKNOWN", "KNOWN", "SUPPORTED" };


    /**
     * Get a bitstream format from the database.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the bitstream format
     *
     * @return the bitstream format, or null if the ID is invalid.
     * @throws SQLException if database error
     */
    @Override
    public BitstreamFormat find(Context context, int id)
            throws SQLException
    {
        BitstreamFormat bitstreamFormat = bitstreamFormatDAO.findByID(context, BitstreamFormat.class, id);

        if (bitstreamFormat == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                        "find_bitstream_format",
                        "not_found,bitstream_format_id=" + id));
            }

            return null;
        }

        // not null, return format object
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_bitstream_format",
                    "bitstream_format_id=" + id));
        }

        return bitstreamFormat;
    }

    @Override
    public BitstreamFormat findByMIMEType(Context context, String mimeType) throws SQLException {
        return bitstreamFormatDAO.findByMIMEType(context, mimeType, false);
    }

    @Override
    public BitstreamFormat findByShortDescription(Context context, String desc) throws SQLException{
        return bitstreamFormatDAO.findByShortDescription(context, desc);
    }

    @Override
    public BitstreamFormat findUnknown(Context context) throws SQLException {
        BitstreamFormat bf = findByShortDescription(context, "Unknown");

        if (bf == null)
        {
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
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can create bitstream formats");
        }

        // Create a table row
        BitstreamFormat bitstreamFormat = bitstreamFormatDAO.create(context, new BitstreamFormat());


        log.info(LogManager.getHeader(context, "create_bitstream_format",
                "bitstream_format_id="
                        + bitstreamFormat.getID()));

        return bitstreamFormat;
    }

    @Override
    public void setShortDescription(Context context, BitstreamFormat bitstreamFormat, String shortDescription) throws SQLException {
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
		if (unknown == null || unknown.getID() != bitstreamFormat.getID()) {
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
        if ((supportLevel < 0) || (supportLevel > 2))
        {
            throw new IllegalArgumentException("Invalid support level");
        }

        bitstreamFormat.setSupportLevelInternal(supportLevel);
    }

    @Override
    public void update(Context context, BitstreamFormat bitstreamFormat) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(bitstreamFormat));
    }

    @Override
    public void update(Context context, List<BitstreamFormat> bitstreamFormats) throws SQLException, AuthorizeException {
        if(CollectionUtils.isNotEmpty(bitstreamFormats)) {
            // Check authorisation - only administrators can change formats
            if (!authorizeService.isAdmin(context)) {
                throw new AuthorizeException(
                        "Only administrators can modify bitstream formats");
            }

            for (BitstreamFormat bitstreamFormat : bitstreamFormats) {
                log.info(LogManager.getHeader(context, "update_bitstream_format",
                        "bitstream_format_id=" + bitstreamFormat.getID()));

                bitstreamFormatDAO.save(context, bitstreamFormat);
            }
        }
    }

    @Override
    public void delete(Context context, BitstreamFormat bitstreamFormat) throws SQLException, AuthorizeException {
                // Check authorisation - only administrators can delete formats
        if (!authorizeService.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = findUnknown(context);

        if (unknown.getID() == bitstreamFormat.getID())
        {
            throw new IllegalArgumentException("The Unknown bitstream format may not be deleted.");
        }

        // Set bitstreams with this format to "unknown"
        int numberChanged = bitstreamFormatDAO.updateRemovedBitstreamFormat(context, bitstreamFormat, unknown);

        // Delete this format from database
        bitstreamFormatDAO.delete(context, bitstreamFormat);

        log.info(LogManager.getHeader(context, "delete_bitstream_format",
                "bitstream_format_id=" + bitstreamFormat.getID() + ",bitstreams_changed="
                        + numberChanged));
    }

    @Override
    public int getSupportLevelID(String supportLevel) {
        for (int i = 0; i < supportLevelText.length; i++)
        {
            if (supportLevelText[i].equals(supportLevel))
            {
                return i;
            }
        }

        return -1;
    }

    @Override
    public BitstreamFormat guessFormat(Context context, Bitstream bitstream) throws SQLException {
        String filename = bitstream.getName();
        // FIXME: Just setting format to first guess
        // For now just get the file name

        // Gracefully handle the null case
        if (filename == null) {
            return null;
        }

        filename = filename.toLowerCase();

        // This isn't rocket science. We just get the name of the
        // bitstream, get the extension, and see if we know the type.
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

        if(CollectionUtils.isNotEmpty(bitstreamFormats))
        {
            return bitstreamFormats.get(0);
        }
        return null;
    }
}
