/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

public class BundleBuilder extends AbstractDSpaceObjectBuilder<Bundle>  {

    private Bundle bundle;
    private Item item;
    private String name;
    private final List<Bitstream> bitstreams = new ArrayList<>();

    protected BundleBuilder(Context context) {
        super(context);
    }

    public static BundleBuilder createBundle(final Context context, final Item item) {
        BundleBuilder builder = new BundleBuilder(context);
        return builder.create(context, item);
    }

    private BundleBuilder create(Context context, Item item) {
        this.context = context;
        this.item = item;
        return this;
    }

    public BundleBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BundleBuilder withBitstream(Bitstream bitstream) {
        this.bitstreams.add(bitstream);
        return this;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            bundle = c.reloadEntity(bundle);
            if (bundle != null) {
                delete(c, bundle);
                c.complete();
            }
        }
    }

    @Override
    protected DSpaceObjectService<Bundle> getService() {
        return bundleService;
    }

    @Override
    public Bundle build() throws SQLException, AuthorizeException {
        bundle = bundleService.create(context, item, name);

        for (Bitstream bitstream: bitstreams) {
            bundleService.addBitstream(context, bundle, bitstream);
        }

        return bundle;
    }

    public static void deleteBundle(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Bundle bundle = bundleService.find(c, uuid);
            if (bundle != null) {
                try {
                    bundleService.delete(c, bundle);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e);
                }
            }
            c.complete();
        }
    }
}
