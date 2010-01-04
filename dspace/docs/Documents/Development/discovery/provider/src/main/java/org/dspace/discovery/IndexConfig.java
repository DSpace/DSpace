package org.dspace.discovery;

import java.io.Serializable;

public class IndexConfig implements Serializable, Cloneable {// Class to hold the index configuration (one instance per config line)

    String indexName;
    String schema;
    String element;
    String qualifier = null;
    String type = "text";

    public IndexConfig(String indexName, String schema, String element,
                       String qualifier, String type) {
        this.indexName = indexName;
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.type = type;
    }


}