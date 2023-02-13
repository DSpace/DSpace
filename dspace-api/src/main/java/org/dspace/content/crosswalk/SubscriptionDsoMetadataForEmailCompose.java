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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Creates a String to be sent as email body for subscriptions
 *
 * @author Alba Aliu
 */
public class SubscriptionDsoMetadataForEmailCompose implements StreamDisseminationCrosswalk {

    private List<String> metadata = new ArrayList<>();

    @Autowired
    private ItemService itemService;

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return Objects.nonNull(dso) && dso.getType() == Constants.ITEM;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out) throws SQLException {
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item) dso;
            PrintStream printStream = new PrintStream(out);
            for (String actualMetadata : metadata) {
                String[] splitted = actualMetadata.split("\\.");
                String qualifier = null;
                if (splitted.length == 1) {
                    qualifier = splitted[2];
                }
                var metadataValue = itemService.getMetadataFirstValue(item, splitted[0], splitted[1], qualifier, ANY);
                printStream.print(metadataValue + " ");
            }
            String itemURL = HandleServiceFactory.getInstance()
                                                 .getHandleService()
                                                 .resolveToURL(context, item.getHandle());
            printStream.print(itemURL);
            printStream.print("\n");
            printStream.close();
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
