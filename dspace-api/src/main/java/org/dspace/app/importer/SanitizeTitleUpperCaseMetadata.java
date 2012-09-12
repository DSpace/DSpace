package org.dspace.app.importer;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class SanitizeTitleUpperCaseMetadata implements SanitizeImportedMetadata
{
    public void sanitize(Context context, Item item, String schema, String element,
            String qualifier)
    {
        String separatore = "-";

        DCValue[] metadata = item.getMetadata(schema, element, qualifier,
                Item.ANY);
        if (metadata != null && metadata.length > 0)
        {
            item.clearMetadata(schema, element, qualifier, Item.ANY);
            for (DCValue dcvalue : metadata)
            {
                String value = dcvalue.value;
                if (value.toUpperCase().equals(value))
                {
                    value = ImporterUtils.normalizeUpperCase("\\.\\s*", value);
                }

                item.addMetadata(schema, element, qualifier, null, value);
            }
        }
    }
}
