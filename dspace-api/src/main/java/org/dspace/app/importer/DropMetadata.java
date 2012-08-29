package org.dspace.app.importer;

import org.dspace.content.Item;
import org.dspace.core.Context;

public class DropMetadata implements SanitizeImportedMetadata
{
    public void sanitize(Context context, Item item, String schema, String element,
            String qualifier)
    {
        item.clearMetadata(schema, element, qualifier, Item.ANY);
    }
}
