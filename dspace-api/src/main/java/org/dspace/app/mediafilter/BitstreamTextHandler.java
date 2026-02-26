/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;

/**
 * Callback class to receive the filtered data into a new Bitstream.
 * Call {@link getBitstream()} to retrieve the new Bitstream.
 */
class BitstreamTextHandler implements ExtractedTextHandler {

    private final Context context;
    private final Item item;
    private final List<Bundle> bundles;
    private final FormatFilter formatFilter;

    private final BundleService bundleService;
    private final BitstreamService bitstreamService;

    private Bitstream b;

    public BitstreamTextHandler(Context context, Item item, List<Bundle> bundles, FormatFilter filter) {
        this.context = context;
        this.item = item;
        this.bundles = bundles;
        this.formatFilter = filter;

        this.bundleService = ContentServiceFactory.getInstance().getBundleService();
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    }

    @Override
    public void handleStream(InputStream stream)
            throws SQLException, AuthorizeException, IOException {
        Bundle targetBundle; // bundle we're modifying
        if (bundles.isEmpty()) {
            // create new bundle if needed
            targetBundle = bundleService.create(context, item, formatFilter.getBundleName());
        } else {
            // take the first match as we already looked out for the correct bundle name
            targetBundle = bundles.get(0);
        }
        // create bitstream to store the filter result
        b = bitstreamService.create(context, targetBundle, stream);
    }

    /**
     * Expose the Bitstream that was created to absorb the text.
     * @return new Bitstream installed in a Bundle of {@link item}.
     */
    public Bitstream getBitstream() {
        return b;
    }

}
