/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;

public class NBSimpleMetadataAction implements NBAction {
    private String metadata;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public void applyCorrection(Context context, Item item, NBEvent event) {
        // TODO Auto-generated method stub

    }
}
