package org.dspace.app.importer;

import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;

/**
 * <code>
 * plugin.selfnamed.org.dspace.importer.EnhanceImportedMetadata = org.dspace.app.importer.DefaultValueEnhanceMetadata
 * 
 * importer.DefaultValueEnhanceMetadata.plugin-names = <plugin-alias1>[,<plugin-alias2>,<plugin-aliasN>]
 * importer.DefaultValueEnhanceMetadata.<plugin-alias1>.value = YOUR-VALUE
 * </code>
 * 
 */
public class DefaultValueEnhanceMetadata extends SelfNamedPlugin implements
        EnhanceImportedMetadata
{

    public static String[] getPluginNames()
    {
        String cfg = ConfigurationManager
                .getProperty("importer.DefaultValueEnhanceMetadata.plugin-names");
        String[] names = cfg.split(",");
        for (int i = 0; i < names.length; i++)
        {
            names[i] = names[i].trim();
        }
        return names;
    }

    public void enhance(Context context, Item item, String schema,
            String element, String qualifier)
    {
        String value = ConfigurationManager
                .getProperty("importer.DefaultValueEnhanceMetadata."
                        + getPluginInstanceName() + ".value");
        item.addMetadata(schema, element, qualifier, null, value);
    }
}
