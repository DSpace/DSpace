/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.content.Item;
import org.dspace.core.Context;


/**
 * This interface can be implemented by plugins that aims to contribute to the
 * generation of the xoai document stored in the item.compile solr OAI core
 * field
 *
 */
public interface XOAIExtensionItemCompilePlugin {

    /**
     * This method allows plugins to add content to the xoai document generated for
     * the item.
     * 
     * @param context  the DSpace Context
     * @param metadata the basic xoai representation of the item that can be
     *                 manipulated by the plugin
     * @param item     the dspace item to index
     * @return the altered xoai metadata
     */
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item);

}
