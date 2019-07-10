package org.dspace.app.rest.model;

public class PropertiesRest {

    private String name;
    private String bundleName;
    private String sequenceId;
    private MetadataRest metadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public MetadataRest getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }
}
