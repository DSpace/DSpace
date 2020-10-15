package org.dspace.harvest.cristin;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestingException;

public interface MetadataRemover
{
    void clearMetadata(Context context, Item item) throws HarvestingException;
}
