/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.dspace.content.Item.ANY;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Creates a String to be sent as email body for subscriptions
 *
 * @author Alba Aliu
 */
public class SubscriptionDsoMetadataForEmailCompose implements StreamDisseminationCrosswalk {

    private List<String> metadata = new ArrayList<>();

    @Autowired
    ItemService itemService;

    @Autowired
    HandleService handleService;

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return Objects.nonNull(dso) && dso.getType() == Constants.ITEM;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out) {
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item) dso;
            try (PrintStream printStream = new PrintStream(out)) {
                for (String actualMetadata : metadata) {
                    String[] split = actualMetadata.split("\\.");
                    String qualifier = null;
                    if (split.length == 3) {
                        qualifier = split[2];
                    }
                    var metadataValue = itemService.getMetadataFirstValue(item, split[0], split[1], qualifier, ANY);
                    printStream.print(metadataValue + " ");
                }
                String itemURL = handleService.getCanonicalForm(item.getHandle());
                printStream.print(itemURL);
                printStream.print("\n");
            }
        }
    }

    @Override
    public String getMIMEType() {
        return "text/plain";
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<String> metadata) {
        this.metadata = metadata;
    }

}
