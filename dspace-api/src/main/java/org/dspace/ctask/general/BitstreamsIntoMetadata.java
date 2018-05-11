/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.sql.SQLException;
import java.util.List;

/**
 * A curation job to take bitstream URLs and place them into metadata elements.
 *
 * @author Stuart Lewis
 */
public class BitstreamsIntoMetadata extends AbstractCurationTask
{

    // The status of this item
    protected int status = Curator.CURATE_UNSET;

    // The results of processing this
    protected List<String> results = null;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(BitstreamsIntoMetadata.class);


    /**
     * Perform the bitstream metadata creation.
     *
     * @param dso The DSpaceObject to be checked
     * @return The curation task status of the checking
     */
    @Override
    public int perform(DSpaceObject dso)
    {
        // The results that we'll return
        StringBuilder results = new StringBuilder();

        // Unless this is an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        boolean changed = false;
        logDebugMessage("The target dso is " + dso.getName());
        if (dso instanceof Item)
        {
            try {
                Item item = (Item)dso;
                itemService.clearMetadata(Curator.curationContext(), item, "dc", "format", Item.ANY, Item.ANY);
                for (Bundle bundle : item.getBundles()) {
                    if ("ORIGINAL".equals(bundle.getName())) {
                        for (Bitstream bitstream : bundle.getBitstreams()) {
                            // Add the metadata and update the item
                            addMetadata(item, bitstream, "original");
                            changed = true;
                        }
                    } else if ("THUMBNAIL".equals(bundle.getName())) {
                        for (Bitstream bitstream : bundle.getBitstreams()) {
                            // Add the metadata and update the item
                            addMetadata(item, bitstream, "thumbnail");
                            changed = true;
                        }
                    }

                    if (changed) {
                        itemService.update(Curator.curationContext(), item);
                        status = Curator.CURATE_SUCCESS;
                    }
                }
            } catch (AuthorizeException ae) {
                // Something went wrong
                logDebugMessage(ae.getMessage());
                status = Curator.CURATE_ERROR;
            } catch (SQLException sqle) {
                // Something went wrong
                logDebugMessage(sqle.getMessage());
                status = Curator.CURATE_ERROR;
            }

        }

        logDebugMessage("About to report: " + results.toString());
        setResult(results.toString());
        report(results.toString());

        return status;
    }

    /**
     * Debugging logging if required
     *
     * @param message The message to log
     */
    protected void logDebugMessage(String message)
    {
        if (log.isDebugEnabled())
        {
            log.debug(message);
        }
    }

    /**
     * Add the bitstream metadata to the item
     *
     * @param item The item
     * @param bitstream The bitstream
     * @param type The type of bitstream
     */
    protected void addMetadata(Item item, Bitstream bitstream, String type) throws SQLException {
        String value = bitstream.getFormat(Curator.curationContext()).getMIMEType() + "##";
        value += bitstream.getName() + "##";
        value += bitstream.getSize() + "##";
        value += item.getHandle() + "##";
        value += bitstream.getSequenceID() + "##";
        value += bitstream.getChecksum() + "##";
        if (bitstream.getDescription() != null) {
            value += bitstream.getDescription();
        }
        itemService.addMetadata(Curator.curationContext(), item, "dc", "format", type, "en", value);
    }
}
