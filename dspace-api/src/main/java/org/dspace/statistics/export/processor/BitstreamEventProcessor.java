/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.processor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.util.SpiderDetector;

/**
 * Processor that handles Bitstream events from the IrusExportUsageEventListener
 */
public class BitstreamEventProcessor extends ExportEventProcessor {

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    private Item item;
    private Bitstream bitstream;

    /**
     * Creates a new BitstreamEventProcessor that will set the params and obtain the parent item of the bitstream
     *
     * @param context
     * @param request
     * @param bitstream
     * @throws SQLException
     */
    public BitstreamEventProcessor(Context context, HttpServletRequest request, Bitstream bitstream)
            throws SQLException {
        super(context, request);
        this.bitstream = bitstream;
        this.item = getItem(request);
    }

    /**
     * Returns the parent item of the bitsream
     *
     * @return parent item of the bitstream
     * @throws SQLException
     */
    private Item getItem(HttpServletRequest request) throws SQLException {
        if (0 < bitstream.getBundles().size()) {
            if (!SpiderDetector.isSpider(request)) {
                Bundle bundle = bitstream.getBundles().get(0);
                if (bundle.getName() == null || !bundle.getName().equals("ORIGINAL")) {
                    return null;
                }

                if (0 < bundle.getItems().size()) {
                    Item item = bundle.getItems().get(0);
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Process the event
     * Check if the item should be processed
     * Create the url to be transmitted based on item and bitstream data
     *
     * @throws SQLException
     * @throws IOException
     */
    public void processEvent() throws SQLException, IOException {
        if (shouldProcessItem(item)) {
            String baseParam = getBaseParameters(item);
            String fullParam = addObjectSpecificData(baseParam, bitstream);
            processObject(fullParam);
        }
    }

    /**
     * Adds additional item and bitstream data to the url
     *
     * @param string    to which the additional data needs to be added
     * @param bitstream
     * @return the string with additional data
     * @throws UnsupportedEncodingException
     */
    protected String addObjectSpecificData(final String string, Bitstream bitstream)
            throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder(string);

        String bitstreamInfo = getBitstreamInfo(bitstream);
        data.append("&").append(URLEncoder.encode("svc_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(bitstreamInfo, UTF_8));
        data.append("&").append(URLEncoder.encode("rft_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(BITSTREAM_DOWNLOAD, UTF_8));

        return data.toString();
    }

    /**
     * Get Bitstream info used for the url
     *
     * @param bitstream
     * @return bitstream info
     */
    private String getBitstreamInfo(final Bitstream bitstream) {

        String dspaceRestUrl = configurationService.getProperty("dspace.server.url");

        StringBuilder sb = new StringBuilder();

        sb.append(dspaceRestUrl);
        sb.append("/api/core/bitstreams/");
        sb.append(bitstream.getID());
        sb.append("/content");

        return sb.toString();
    }
}
