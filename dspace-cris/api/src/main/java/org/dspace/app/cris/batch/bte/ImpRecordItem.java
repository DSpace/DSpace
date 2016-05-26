package org.dspace.app.cris.batch.bte;


import java.util.HashMap;
import java.util.Set;

public class ImpRecordItem {

	public ImpRecordItem(){
		metadata = new HashMap<String,Set<String>>();
	}
	
	private String sourceId;
	
	private String sourceRef;
	
	private HashMap<String,Set<String>> metadata;
	
	public HashMap<String, Set<String>> getMetadata() {
		return metadata;
	}

	public void setMetadata(HashMap<String, Set<String>> metadata) {
		this.metadata = metadata;
	}
	
	public void addMetadata(String metadataName,Set<String> values){
		this.metadata.put(metadataName, values);
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
