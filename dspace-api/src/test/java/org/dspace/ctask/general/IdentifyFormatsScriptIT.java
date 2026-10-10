/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.junit.Test;

/**
 * Integration test for the {@link IdentifyFormatsScript} process, which re-identifies
 * bitstream formats across the repository (or a given scope) and corrects those stored
 * as "Unknown".
 */
public class IdentifyFormatsScriptIT extends AbstractIntegrationTestWithDatabase {

    /**
     * With no target, the process scans the whole repository and corrects an Unknown PDF.
     */
    @Test
    public void testCorrectsUnknownFormatRepositoryWide() throws Exception {
        Bitstream bitstream = createUnknownPdfBitstream();

        assertEquals("Unknown", bitstream.getFormat(context).getShortDescription());

        int status = runDSpaceScript("identify-formats");
        assertEquals("Process should succeed", 0, status);

        Bitstream reloaded = context.reloadEntity(bitstream);
        assertEquals("Adobe PDF", reloaded.getFormat(context).getShortDescription());
    }

    /**
     * With a target identifier, only that scope is processed.
     */
    @Test
    public void testCorrectsUnknownFormatForScope() throws Exception {
        Bitstream bitstream = createUnknownPdfBitstream();
        Item item = bitstream.getBundles().get(0).getItems().get(0);

        assertEquals("Unknown", bitstream.getFormat(context).getShortDescription());

        int status = runDSpaceScript("identify-formats", "-i", item.getID().toString());
        assertEquals("Process should succeed", 0, status);

        Bitstream reloaded = context.reloadEntity(bitstream);
        assertEquals("Adobe PDF", reloaded.getFormat(context).getShortDescription());
    }

    private Bitstream createUnknownPdfBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        Item item = ItemBuilder.createItem(context, collection).build();

        Bitstream bitstream;
        File pdf = new File(testProps.get("test.bitstream").toString());
        try (InputStream is = new FileInputStream(pdf)) {
            // A PDF whose name has no usable extension is stored as "Unknown".
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                        .withName("mysteryfile")
                                        .build();
        }
        context.restoreAuthSystemState();
        return bitstream;
    }
}
