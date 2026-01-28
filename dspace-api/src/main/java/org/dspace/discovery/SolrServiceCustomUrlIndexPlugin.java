/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} to fill the custom url
 * field.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class SolrServiceCustomUrlIndexPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private CustomUrlService customUrlService;

    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso instanceof IndexableItem)) {
            return;
        }

        Item item = ((IndexableItem) dso).getIndexedObject();
        document.addField("customurl", customUrlService.getAllCustomUrls(item));
    }

}
