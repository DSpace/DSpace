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

public class NBMetadataMapAction implements NBAction {
    private Map<String, String> types;

    public Map<String, String> getTypes() {
        return types;
    }

    public void setTypes(Map<String, String> types) {
        this.types = types;
    }

    @Override
    public void applyCorrection(Context context, Item item, NBEvent event) {
        // TODO Auto-generated method stub

    }
}
