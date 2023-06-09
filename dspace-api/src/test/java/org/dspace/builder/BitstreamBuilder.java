/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

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
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Builder class to build bitstreams in test cases
 */
public class BitstreamBuilder extends AbstractDSpaceObjectBuilder<Bitstream> {

    private Bitstream bitstream;
    private Item item;
    private Group readerGroup;

    protected BitstreamBuilder(Context context) {
        super(context);

    }

    public static BitstreamBuilder createBitstream(Context context, Item item, InputStream is)
        throws SQLException, AuthorizeException, IOException {
        BitstreamBuilder builder = new BitstreamBuilder(context);
        return builder.create(context, item, is);
    }

    public static BitstreamBuilder createBitstream(Context context, Bundle bundle, InputStream is)
            throws SQLException, AuthorizeException, IOException {
        BitstreamBuilder builder = new BitstreamBuilder(context);
        return builder.create(context, bundle, is);
    }

    public static BitstreamBuilder createBitstream(Context context, Item item, InputStream is, String bundleName)
            throws SQLException, AuthorizeException, IOException {
        BitstreamBuilder builder = new BitstreamBuilder(context);
        return builder.createInRequestedBundle(context, item, is, bundleName);
    }

    private BitstreamBuilder create(Context context, Item item, InputStream is)
        throws SQLException, AuthorizeException, IOException {
        this.context = context;
        this.item = item;

        Bundle originalBundle = getOriginalBundle(item);

        bitstream = bitstreamService.create(context, originalBundle, is);

        return this;
    }

    private BitstreamBuilder create(Context context, Bundle bundle, InputStream is)
            throws SQLException, AuthorizeException, IOException {
        this.context = context;
        this.item = bundle.getItems().get(0);
        bitstream = bitstreamService.create(context, bundle, is);

        return this;
    }

    private BitstreamBuilder createInRequestedBundle(Context context, Item item, InputStream is, String bundleName)
            throws SQLException, AuthorizeException, IOException {
        this.context = context;
        this.item = item;

        Bundle bundle = getBundleByName(item, bundleName);

        bitstream = bitstreamService.create(context, bundle, is);

        return this;
    }

    private Bundle getBundleByName(Item item, String bundleName) throws SQLException, AuthorizeException {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        Bundle targetBundle = null;

        if (bundles.size() < 1) {
            // not found, create a new one
            targetBundle = bundleService.create(context, item, bundleName);
        } else {
            // put bitstreams into first bundle
            targetBundle = bundles.iterator().next();
        }
        return targetBundle;
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
        BitstreamFormat bf = bitstreamFormatService.findByMIMEType(context, mimeType);

        if (bf != null) {
            bitstream.setFormat(context, bf);
        }

        return this;
    }

    public BitstreamBuilder withFormat(String format) throws SQLException {

        bitstreamService.addMetadata(context, bitstream, "dc", "format", null, null, format);

        return this;
    }

    public BitstreamBuilder withProvenance(String provenance) throws SQLException {

        bitstreamService.addMetadata(context, bitstream, "dc", "description", "provenance", null, provenance);

        return this;
    }


    public BitstreamBuilder withIIIFLabel(String label) throws SQLException {
        bitstreamService.addMetadata(context, bitstream, "iiif", "label", null, null, label);
        return this;
    }

    public BitstreamBuilder withIIIFCanvasWidth(int i) throws SQLException {
        bitstreamService.addMetadata(context, bitstream, "iiif", "image", "width", null, String.valueOf(i));
        return this;
    }

    public BitstreamBuilder withIIIFCanvasHeight(int i) throws SQLException {
        bitstreamService.addMetadata(context, bitstream, "iiif", "image", "height", null, String.valueOf(i));
        return this;
    }

    public BitstreamBuilder withIIIFToC(String toc) throws SQLException {
        bitstreamService.addMetadata(context, bitstream, "iiif", "toc", null, null, toc);
        return this;
    }

    private Bundle getOriginalBundle(Item item) throws SQLException, AuthorizeException {
        List<Bundle> bundles = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        Bundle targetBundle = null;

        if (bundles.size() < 1) {
            // not found, create a new one
            targetBundle = bundleService.create(context, item, Constants.CONTENT_BUNDLE_NAME);
        } else {
            // put bitstreams into first bundle
            targetBundle = bundles.iterator().next();
        }

        return targetBundle;
    }

    public BitstreamBuilder withEmbargoPeriod(String embargoPeriod) {
        return setEmbargo(embargoPeriod, bitstream);
    }

    public BitstreamBuilder withReaderGroup(Group group) {
        readerGroup = group;
        return this;
    }

    @Override
    public Bitstream build() {
        try {
            bitstreamService.update(context, bitstream);
            itemService.update(context, item);

            //Check if we need to make this bitstream private.
            if (readerGroup != null) {
                setOnlyReadPermission(bitstream, readerGroup, null);
            }

            context.dispatchEvents();

            indexingService.commit();

        } catch (Exception e) {
            return null;
        }

        return bitstream;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            bitstream = c.reloadEntity(bitstream);
            if (bitstream != null) {
                delete(c, bitstream);
                c.complete();
            }
        }
    }

    @Override
    protected DSpaceObjectService<Bitstream> getService() {
        return bitstreamService;
    }

}
