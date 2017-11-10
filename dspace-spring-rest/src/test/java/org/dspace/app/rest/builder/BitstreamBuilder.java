/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

/**
 * Builder class to build bitstreams in test cases
 */
public class BitstreamBuilder extends AbstractBuilder<Bitstream>{

    public static final String ORIGINAL = "ORIGINAL";

    private Bitstream bitstream;

    public BitstreamBuilder createBitstream(Context context, Item item, InputStream is) throws SQLException, AuthorizeException, IOException {
        this.context = context;

        Bundle originalBundle = getOriginalBundle(item);

        bitstream = bitstreamService.create(context, originalBundle, is);

        return this;
    }

    public BitstreamBuilder withName(String name) throws SQLException {
        bitstream.setName(context, name);
        return this;
    }

    public BitstreamBuilder withDescription(String description) throws SQLException {
        bitstream.setDescription(context, description);
        return this;
    }

    public BitstreamBuilder withMimeType(String mimeType) throws SQLException {
        BitstreamFormat bf = bitstreamFormatService
                .findByMIMEType(context, mimeType);

        if (bf != null) {
            bitstream.setFormat(context, bf);
        }

        return this;
    }

    private Bundle getOriginalBundle(Item item) throws SQLException, AuthorizeException {
        List<Bundle> bundles = itemService.getBundles(item, ORIGINAL);
        Bundle targetBundle = null;

        if( bundles.size() < 1 )
        {
            // not found, create a new one
            targetBundle = bundleService.create(context, item, ORIGINAL);
        }
        else
        {
            // put bitstreams into first bundle
            targetBundle = bundles.iterator().next();
        }

        return targetBundle;
    }

    protected DSpaceObjectService<Bitstream> getDsoService() {
        return bitstreamService;
    }

    public Bitstream build() {
        try {
            bitstreamService.update(context, bitstream);
        } catch (Exception e) {
           return null;
        }

        return bitstream;
    }
}
