/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    private List<Bitstream> bitstreams = new ArrayList<>();

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

    public void cleanup() throws Exception {
        delete(bundle);
    }

    protected DSpaceObjectService<Bundle> getService() {
        return bundleService;
    }

    public Bundle build() throws SQLException, AuthorizeException {
        bundle = bundleService.create(context, item, name);

        for (Bitstream bitstream: bitstreams) {
            bundleService.addBitstream(context, bundle, bitstream);
        }

        return bundle;
    }
}
