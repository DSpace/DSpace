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

import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.statistics.util.SpiderDetector;

/**
 * Processor that handles Bitstream events from the ExportUsageEventListener
 */
public class BitstreamEventProcessor extends ExportEventProcessor {

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
        this.item = getItem();
    }

    /**
     * Returns the parent item of the bitsream
     *
     * @return parent item of the bitstream
     * @throws SQLException
     */
    private Item getItem() throws SQLException {
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
            String fullParam = addObjectSpecificData(baseParam, item, bitstream);
            processObject(fullParam);
        }
    }

    /**
     * Adds additional item and bitstream data to the url
     *
     * @param string    to which the additional data needs to be added
     * @param item
     * @param bitstream
     * @return the string with additional data
     * @throws UnsupportedEncodingException
     */
    protected String addObjectSpecificData(final String string, Item item, Bitstream bitstream)
            throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder(string);

        String bitstreamInfo = getBitstreamInfo(item, bitstream);
        data.append("&").append(URLEncoder.encode("svc_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(bitstreamInfo, UTF_8));
        data.append("&").append(URLEncoder.encode("rft_dat", UTF_8)).append("=")
            .append(URLEncoder.encode(BITSTREAM_DOWNLOAD, UTF_8));

        return data.toString();
    }

    /**
     * Get Bitstream info used for the url
     *
     * @param item
     * @param bitstream
     * @return bitstream info
     */
    private String getBitstreamInfo(final Item item, final Bitstream bitstream) {

        StringBuilder sb = new StringBuilder(configurationService.getProperty("dspace.ui.url"));

        String identifier;
        if (item != null && item.getHandle() != null) {
            identifier = "handle/" + item.getHandle();
        } else if (item != null) {
            identifier = "item/" + item.getID();
        } else {
            identifier = "id/" + bitstream.getID();
        }


        sb.append("/bitstream/").append(identifier).append("/");

        // If we can, append the pretty name of the bitstream to the URL
        try {
            if (bitstream.getName() != null) {
                sb.append(Util.encodeBitstreamName(bitstream.getName(), UTF_8));
            }
        } catch (UnsupportedEncodingException uee) {
            // just ignore it, we don't have to have a pretty
            // name at the end of the URL because the sequence id will
            // locate it. However it means that links in this file might
            // not work....
        }

        sb.append("?sequence=").append(bitstream.getSequenceID());

        return sb.toString();
    }
}
