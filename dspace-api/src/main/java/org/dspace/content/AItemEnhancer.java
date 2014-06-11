package org.dspace.content;

import java.util.List;

public abstract class AItemEnhancer
{

    // the "element" of the virtual metadata (author)
    private String alias;

    // the metadata list to lookup for link to CRIS entities
    // (dc.contributor.author, dc.contributor.editor, etc.)
    private List<String> metadata;

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public String getAlias()
    {
        return alias;
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }
}
