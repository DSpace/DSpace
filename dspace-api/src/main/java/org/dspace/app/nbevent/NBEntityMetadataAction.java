/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;

public class NBEntityMetadataAction implements NBAction {
    private String metadata;
    private String entityType;
    private Map<String, String> entityMetadata;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Map<String, String> getEntityMetadata() {
        return entityMetadata;
    }

    public void setEntityMetadata(Map<String, String> entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    @Override
    public void applyCorrection(Context context, Item item, NBEvent event) {
        // TODO Auto-generated method stub
    }
}
