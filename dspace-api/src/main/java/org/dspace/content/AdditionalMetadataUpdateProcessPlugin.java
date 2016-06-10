package org.dspace.content;

import org.dspace.core.Context;

public interface AdditionalMetadataUpdateProcessPlugin
{

    public void process(Context context, Item item, String provider);

}
