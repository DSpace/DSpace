package org.dspace.app.importer;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface EnhanceImportedMetadata
{

    public void enhance(Context context, Item item, String schema,
            String element, String qualifier);
}
