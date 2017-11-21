package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

import java.util.Set;

public class ItemModificationConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(ItemModificationConsumer.class);

    public void initialize() throws Exception { }

    public void finish(Context ctx) throws Exception { }

    public void end(Context ctx) throws Exception { }

    public void update(Context ctx, Event event) throws Exception { }

    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();

        try {
            ctx.turnOffAuthorisationSystem();

            switch (st) {
                case Constants.BITSTREAM:
                {
                    log.debug("ItemModificationConsumer is consuming " + event.toString());
                    Bitstream bitstream = (Bitstream)event.getSubject(ctx);
                    Bundle bundle = (Bundle)bitstream.getParentObject();
                    Item item = (Item)bundle.getParentObject();
                    // update bitstream sizes.
                    item.clearMetadata(MetadataSchema.DC_SCHEMA, "format", "extent", null);
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "format", "extent", null, InstallItem.getBitstreamSizes(item));
                    item.update();
                }
                break;
                case Constants.ITEM:
                {
                    Item item = (Item) event.getSubject(ctx);
                    log.debug("ItemModificationConsumer is consuming " + event.toString());
                    // update bitstream sizes.
                    item.clearMetadata(MetadataSchema.DC_SCHEMA, "format", "extent", null);
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "format", "extent", null, InstallItem.getBitstreamSizes(item));
                    item.update();
                }
                break;
            }
            ctx.complete();
        } catch (Exception e) {
            ctx.abort();
        }
    }
}
