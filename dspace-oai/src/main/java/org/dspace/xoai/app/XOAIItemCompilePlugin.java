/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import org.dspace.content.Item;
import org.dspace.core.Context;

import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * Used to enrich item.compile field description
 *
 */
public interface XOAIItemCompilePlugin
{

    public Metadata additionalMetadata(Context context, Metadata metadata,
            Item item);

}
