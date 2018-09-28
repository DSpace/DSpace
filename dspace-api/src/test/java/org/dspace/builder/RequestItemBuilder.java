/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Manage the creation and cleanup of {@link RequestItem}s for testing.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemBuilder
        extends AbstractBuilder<RequestItem, RequestItemService> {
    public static final String REQ_EMAIL = "jsmith@example.com";
    public static final String REQ_NAME = "John Smith";
    public static final String REQ_MESSAGE = "Please send me a copy of this.";
    public static final String REQ_PATH = "test/file";

    private Community community;
    private Collection collection;
    private Item item;
    private Bitstream bitstream;

    public RequestItemBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup()
            throws Exception {
        bitstreamService.delete(context, bitstream);
        itemService.delete(context, item);
        collectionService.delete(context, collection);
        communityService.delete(context, community);
    }

    @Override
    public RequestItem build() {
        // Build all the other stuff we need, to request an Item.
        CommunityBuilder communityBuilder = CommunityBuilder.createCommunity(context);
        community = communityBuilder.build();
        CollectionBuilder collectionBuilder = CollectionBuilder.createCollection(context,
                community);
        collection = collectionBuilder.build();
        ItemBuilder itemBuilder = ItemBuilder.createItem(context, collection);
        item = itemBuilder.build();

        // Request a copy of the Item that we just built.
        String token;
        try (InputStream is = new ByteArrayInputStream("".getBytes());) {
            BitstreamBuilder bitstreamBuilder
                    = BitstreamBuilder.createBitstream(context, item, is);
            bitstream = bitstreamBuilder.build();
            token = requestItemService.createRequest(context,
                    bitstream, item, true, REQ_EMAIL, REQ_NAME, REQ_MESSAGE);
        } catch (SQLException | AuthorizeException | IOException ex) {
            throw new RuntimeException(ex);
        }

        // Return the request.
        return requestItemService.findByToken(context, token);
    }

    @Override
    public void delete(Context context, RequestItem request)
            throws Exception {
        // N.B. RequestItem cannot be deleted.
    }

    @Override
    protected RequestItemService getService() {
        return requestItemService;
    }
}
