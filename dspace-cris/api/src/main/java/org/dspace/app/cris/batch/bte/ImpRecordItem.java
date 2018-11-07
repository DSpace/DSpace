/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.bte;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ImpRecordItem
{

    public ImpRecordItem()
    {
        metadata = new HashMap<String, Set<ImpRecordMetadata>>();
    }

    private String sourceId;

    private String sourceRef;

    private HashMap<String, Set<ImpRecordMetadata>> metadata;

    public HashMap<String, Set<ImpRecordMetadata>> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(HashMap<String, Set<ImpRecordMetadata>> metadata)
    {
        this.metadata = metadata;
    }

    public void addMetadata(String metadataName, Set<ImpRecordMetadata> values)
    {
        this.metadata.put(metadataName, values);
    }

    public void addMetadata(String metadataName, ImpRecordMetadata value)
    {
        Set<ImpRecordMetadata> metadataMap = null;
        if(this.metadata.containsKey(metadataName)) {
            metadataMap = this.metadata.get(metadataName);
        }
        else {
            metadataMap = new HashSet<ImpRecordMetadata>();            
        }
        metadataMap.add(value);
        this.metadata.put(metadataName, metadataMap);
    }
    
    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(String sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getSourceRef()
    {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef)
    {
        this.sourceRef = sourceRef;
    }

}
