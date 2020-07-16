/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 27/01/16
 * Time: 17:11
 */
public class ClearEmptyItemMetadata extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(ClearEmptyItemMetadata.class);

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso instanceof Item) {
            Item item = (Item) dso;

            try {
                List<MetadataValue> metadata = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                List<MetadataValue> metadataValuesToRemove = new ArrayList<>();

                for (MetadataValue metadataValue : metadata) {
                    if (StringUtils.isBlank(metadataValue.getValue())) {
                        metadataValuesToRemove.add(metadataValue);
                    }
                }

                if (CollectionUtils.isNotEmpty(metadataValuesToRemove)) {
                    itemService.removeMetadataValues(Curator.curationContext(), item, metadataValuesToRemove);
                    itemService.update(Curator.curationContext(), item);
                }

                setResult(metadataValuesToRemove.size() + " empty metadata values removed");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Curator.CURATE_ERROR;
            }
        }

        return Curator.CURATE_SUCCESS;
    }
}
