/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import java.util.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.event.*;
import org.dspace.fileaccess.factory.*;
import org.dspace.fileaccess.service.*;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 02 Oct 2015
 */
public class FileAccessConsumer implements Consumer {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(FileAccessConsumer.class);

    private List<Item> items = new LinkedList<>();

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected FileAccessFromMetadataService fileAccessFromMetadataService = FileAccessServiceFactory.getInstance().getFileAccessFromMetadataService();

    public void initialize() throws Exception {

    }

    /**
     * Gather the DspaceObject IDs here.
     * DO NOT COMMIT THE CONTEXT
     */
    public void consume(Context context, Event event) throws Exception {


        int subjectType = event.getSubjectType();
        int eventType = event.getEventType();

        switch (subjectType) {
            case Constants.ITEM:
                if (eventType == Event.INSTALL) {
                    items.add((Item) event.getSubject(context));
                }
                break;
            default:
                log.debug("consume() got unrecognized event: " + event.toString());
        }

    }

    /**
     * Find the objects based on the IDS.
     * Process them here.
     * commit and clear the IDs
     */
    public void end(Context context) {

        try {
            // update objects
            if (!items.isEmpty()) {
                for (Item item : items) {
                    Iterator<Bitstream> bitstreams = bitstreamService.getItemBitstreams(context, item);

                    while (bitstreams.hasNext()) {
                        Bitstream bitstream = bitstreams.next();

                        List<MetadataValue> metadata = bitstreamService.getMetadataByMetadataString(bitstream, "workflow.fileaccess");
                        List<MetadataValue> endDate = bitstreamService.getMetadataByMetadataString(bitstream, "workflow.fileaccess.date");

                        String endDateString = null;

                        if(endDate.size()>0){
                            endDateString = endDate.get(0).getValue();
                        }

                        bitstreamService.clearMetadata(context, bitstream, "workflow", "fileaccess", null, Item.ANY);
                        bitstreamService.clearMetadata(context, bitstream, "workflow", "fileaccess", "date", Item.ANY);

                        if (metadata.size() > 0) {
                            fileAccessFromMetadataService.setFileAccess(context, bitstream, metadata.get(0).getValue(), endDateString);
                        }

                        bitstreamService.update(context, bitstream);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        finally {
            items.clear();
        }

        // commit context
        context.dispatchEvents();
    }

    public void finish(Context ctx) throws Exception {

    }
}
