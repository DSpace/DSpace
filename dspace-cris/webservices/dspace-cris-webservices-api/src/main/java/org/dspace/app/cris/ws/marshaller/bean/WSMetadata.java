/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.marshaller.bean;

import java.util.ArrayList;
import java.util.List;

public class WSMetadata
{

    private String name;
    private List<WSMetadataValue> values = new ArrayList<WSMetadataValue>();
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<WSMetadataValue> getValues() {
        return values;
    }
    public void setValues(List<WSMetadataValue> values) {
        this.values = values;
    }

}
